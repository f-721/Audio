package com.example.audio

import android.content.Context
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.media.MediaPlayer

class JudgeTiming(
    private val accEstimation: AccEstimation,
    private val tvgreat: TextView,
    var nearBy: NearBy? = null,
    private val context: Context,
    private val playAudio: PlayAudio,
    tvjudge: TextView
) : ViewModel() {

    // 追加: IDがすでに受信されたかどうかを管理するフラグ
    private var hasReceivedId = false

    private var clientId: String? = null // 追加: clientId を保持するフィールド

    private val _judgement = MutableLiveData<String>()
    private var job: Job? = null
    private var hitTime = 0L
    private var hasReceivedHitTime = false // データ受信フラグ

    private val judgementCounts = mutableMapOf<String, JudgementCount>()

    // クライアントIDをヒットタイムとペアで保存
    private val hitTimeWithClientIDMap = mutableMapOf<Long, String>()

    private var nowtime: Long = 0L // nowtime をクラスフィールドとして定義
    private var delayMillis: Long = 0L
    var misscount = 0

    // 事前にロードしておく MediaPlayer インスタンス
    private var goodSoundPlayer: MediaPlayer? = null
    private var missSoundPlayer: MediaPlayer? = null
    private var timingSoundPlayer: MediaPlayer? = null

    init {
        // 音声ファイルを事前にロードして準備
        goodSoundPlayer = MediaPlayer.create(context, R.raw.greatsounds)
        missSoundPlayer = MediaPlayer.create(context, R.raw.misssounds)
        timingSoundPlayer = MediaPlayer.create(context, R.raw.timing)

        // 再生が完了したらリソースを解放しないようにリスナーを設定
        goodSoundPlayer?.setOnCompletionListener {
            it.seekTo(0) // 再生位置を最初に戻す
        }
        missSoundPlayer?.setOnCompletionListener {
            it.seekTo(0)
        }
        timingSoundPlayer?.setOnCompletionListener {
            it.seekTo(0)
        }
    }

    // クライアントごとの判定結果を保存するデータクラス
    data class JudgementCount(
        val id:String,
        var goodCount: Int = 0,
        var missCount: Int = 0
    )


    private val hitObserver = Observer<Boolean> { isHit ->
        if (isHit) {
        }
        Log.d("JudgeTiming", "isHit changed: $isHit")
    }

    private val hitTimeObserver = Observer<Long> { newHitTime ->
        hitTime = newHitTime
        Log.d("JudgeTiming", "Observed hitTime: $newHitTime")
    }

    init {
        Log.d("JudgeTiming", "JudgeTiming initialized")
        accEstimation.isHit.observeForever(hitObserver)
        accEstimation.lastHitTime.observeForever(hitTimeObserver)
    }

    fun stopJudging() {
        job?.cancel()
        accEstimation.isHit.removeObserver(hitObserver)
        accEstimation.lastHitTime.removeObserver(hitTimeObserver)
    }

    fun recordid(clientId: String,) {
        if (!hasReceivedId) {
            Log.d("JudgeTiming:recordid", "IDを受信: $clientId")
            this.clientId = clientId
            hasReceivedId = true // ID受信フラグを立てる
            hitTimeWithClientIDMap[hitTime] = clientId
            // 受信したIDがまだ判定リストにない場合、初期化して保存
            judgementCounts.getOrPut(clientId) { JudgementCount(id = clientId) }
            Log.d("JudgeTiming", "判定するクライアントID: $clientId")
            triggerJudging(clientId)
            nearBy?.disableReceiving() // 受信を無効化
        } else {
            Log.d("JudgeTiming", "IDは既に受信されました")
        }
//
//        fun stoprecord(){
//            job?.cancel()
//            Log.d("JudgeTiming","ID受信終了")
//        }
    }

    fun clearResultsForClient(clientId: String) {
        // clientId のデータを削除する
        judgementCounts.remove(clientId)
        Log.d("JudgeTiming", "クライアント $clientId のデータを削除しました")
    }

    fun recordHitTime(hitTime: Long) {
        if (!hasReceivedHitTime) {
            Log.d("JudgeTiming", "受信したヒット時刻: $hitTime")
            this.hitTime = hitTime
            this.nowtime = System.currentTimeMillis()
            hasReceivedHitTime = true // データ受信フラグを立てる
        } else {
            Log.d("JudgeTiming", "データは既に受信されました")
        }
    }

    private fun playSoundAsync(soundType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // PlayAudioのisPlaying()メソッドで音源が再生中かをチェック
                if (playAudio.isPlaying()) {
                    val player = when (soundType) {
                        "GOOD" -> goodSoundPlayer
                        "MISS" -> missSoundPlayer
                        "TIMING" -> timingSoundPlayer
                        else -> null
                    }

                    player?.let {
                        it.seekTo(0) // 再生位置をリセット
                        if (soundType == "MISS") {
                            // MISS判定時はピッチを0.5ずつ下げる
                            val currentPitch = it.playbackParams.speed // 現在のピッチを取得
                            val newPitch = currentPitch - 0.5f
                            it.playbackParams = it.playbackParams.setPitch(newPitch)
                        } else if (soundType == "GOOD") {
                            // GOOD判定時はピッチを元に戻す
                            it.playbackParams = it.playbackParams.setPitch(1.0f)
                        }
                        it.start()
                        Log.d("JudgeTiming", "サウンド再生が即時に開始されました: $soundType")
                    } ?: run {
                        Log.e("JudgeTiming", "音声プレイヤーが準備されていません: $soundType")
                    }
                } else {
                    Log.d("JudgeTiming", "音源が再生中でないため、TIMING音声は再生しません")
                }
            } catch (e: Exception) {
                Log.e("JudgeTiming", "サウンド再生中にエラーが発生しました", e)
            }
        }
    }



    fun startJudging(clientId: String) {
        job = viewModelScope.launch(Dispatchers.Default) {
            val bpm = 69

            while (isActive) {
                delayMillis = (60_000 / bpm).toLong() //判定タイミングはここいじって変えましょう
                // 0.5 * delayMillis 前に判定を行うための基準時間を計算
                val judgingThreshold = 0.5 * delayMillis
                delay(delayMillis)
                Log.d("Judgement","delayMills(判定周期) = $delayMillis")
                Log.d("JudgeTiming", "⭐︎⭐︎⭐︎⭐︎⭐︎⭐︎⭐︎⭐︎")
                Log.d("JudgeTiming", "音ゲー判定中")
                Log.d("JudgeTiming", "⭐︎⭐︎⭐︎⭐︎⭐︎⭐︎⭐︎⭐︎")
                nowtime = System.currentTimeMillis()
                Log.d("JudgeTiming", "nowtime(判定時の現在時刻)= $nowtime ms")
                playSoundAsync("TIMING")


                nearBy?.enableReceiving()  // 受信を再度有効化
                hasReceivedHitTime = false // Reset flag
                hasReceivedId = false      // Reset ID flag
            }
        }
    }


    fun triggerJudging(clientId: String) {
        // IDが受信されていない場合
        if (!hasReceivedId) {
            Log.d("JudgeTiming", "IDが受信されていないため、判定をスキップします")
            misscount += 1
            Log.d("JudgeTiming", "うおおおおお $misscount")
            return
        }

        var timeDiff: Long = (nowtime - hitTime) // 判定タイミングとヒットタイムとの差

        // 特定のクライアントIDなら時間差を調整
        if (clientId == "atuo_2b77e0851dd47474") {
            timeDiff -= 1000
        }

        Log.d("JudgeTiming", "-------------------")
        Log.d("JudgeTiming", "ゲーム内判定時刻(nowtime): $nowtime ms")
        Log.d("JudgeTiming", "ヒット時刻: $hitTime ms")
        Log.d("JudgeTiming", "Time difference(ヒット時刻との差): $timeDiff ms")

        val lowerBound = (-0.4 * delayMillis).toLong()
        val upperBound = (0.4 * delayMillis).toLong()

        Log.d("JudgeTiming", "判定範囲: $lowerBound ～ $upperBound")
        Log.d("JudgeTiming", "現在の timeDiff: $timeDiff")

        val judgement = when {
            timeDiff in lowerBound..upperBound -> {
                viewModelScope.launch(Dispatchers.Main) {
                    tvgreat.text = "GOOD"
                }
                Log.d("JudgeTiming", "GOODです")

                // ピッチを元に戻す（例: 標準ピッチ=1.0）
                playAudio.changePitch(1.0f)
                "GOOD"
            }
            else -> {
                viewModelScope.launch(Dispatchers.Main) {
                    tvgreat.text = "MISS"
                }
                Log.d("JudgeTiming", "失敗(MISS)")

                // ピッチを下げる（例: MISS時は0.9）
                playAudio.changePitch(0.9f)
                "MISS"
            }
        }

        fun stopJudging() {
            job?.cancel()  // jobをキャンセルして判定処理を停止
            Log.d("JudgeTiming", "Judging process has been stopped")
        }

        clientId?.let {
            saveJudgement(it, judgement) // clientId を使って保存
        }

        postJudgement(judgement)
        Log.d("Judgement", "一つの判定を終了")
        Log.d("JudgeTiming", "-------------------⭐︎")
    }


    init {
        Log.d("JudgeTiming", "JudgeTiming initialized")
        accEstimation.isHit.observeForever(hitObserver)
        accEstimation.lastHitTime.observeForever(hitTimeObserver)

        // 曲の終了イベントを監視
        playAudio.isAudioComplete.observeForever { isComplete ->
            if (isComplete) {
                stopTimingSound() // 曲終了時にTIMING音声を止める
            }
        }
    }

    fun stopTimingSound() {
        try {
            if (timingSoundPlayer?.isPlaying == true) {
                timingSoundPlayer?.pause() // 再生を一時停止
                timingSoundPlayer?.seekTo(0) // 再生位置を先頭に戻す
                Log.d("JudgeTiming", "TIMING音声の再生を停止しました")
            }
        } catch (e: Exception) {
            Log.e("JudgeTiming", "TIMING音声の停止中にエラーが発生しました", e)
        }
    }

    private fun saveJudgement(clientId: String, judgement: String) {
        if (clientId.contains("atuo_")) {
            val count = judgementCounts.getOrPut(clientId) { JudgementCount(id = clientId) }

            when (judgement) {
                "GOOD" -> count.goodCount++
                "MISS" -> count.missCount++
            }

            Log.d("JudgeTiming", "クライアントID: $clientId の判定結果を保存しました: $judgement")
        }
    }


    fun getResultsForClient(clientId: String): JudgementCount? {
        return judgementCounts[clientId]
    }

    private fun postJudgement(judgement: String) {
        _judgement.postValue(judgement)
        Log.d("JudgeTiming", "Judgement: $judgement")
        Log.d("JudgeTiming", "-------------------")

        hasReceivedHitTime = false
    }

    override fun onCleared() {
        super.onCleared()
        //リソース解放
        goodSoundPlayer?.release()
        missSoundPlayer?.release()
        timingSoundPlayer?.release()
        stopJudging()
    }
}