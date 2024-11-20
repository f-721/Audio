package com.example.audio

import android.content.Context
import android.security.identity.ResultData
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
    private val context: Context
) : ViewModel() {

    // 追加: IDがすでに受信されたかどうかを管理するフラグ
    private var hasReceivedId = false

    private var clientId: String? = null // 追加: clientId を保持するフィールド

    // クライアントデバイスごとの結果を保存するためのマップ
    private val resultsMap = mutableMapOf<String, ResultData>()

    private val _judgement = MutableLiveData<String>()
    private var job: Job? = null
    private var hitTime = 0L
    private var hasReceivedHitTime = false // データ受信フラグ

    private val judgementCounts = mutableMapOf<String, JudgementCount>()

    // クライアントIDをヒットタイムとペアで保存
    private val hitTimeWithClientIDMap = mutableMapOf<Long, String>()

    private var mediaPlayer: MediaPlayer? = null

    private var nowtime: Long = 0L // nowtime をクラスフィールドとして定義
    private var judgementTiming: Long = 0L
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
        var greatCount: Int = 0,
        var goodCount: Int = 0,
        var badCount: Int = 0,
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

    fun recordid(clientId: String) {
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
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val player = when (soundType) {
                    "GOOD" -> MediaPlayer.create(context, R.raw.greatsounds)
                    "MISS" -> MediaPlayer.create(context, R.raw.misssounds)
                    "TIMING" -> MediaPlayer.create(context, R.raw.timing)
                    else -> null
                }

                player?.let {
                    it.start()
                    Log.d("JudgeTiming", "サウンド再生が即時に開始されました: $soundType")
                    it.setOnCompletionListener {
                        it.release()
                    }
                } ?: run {
                    Log.e("JudgeTiming", "音声プレイヤーが準備されていません: $soundType")
                }
            } catch (e: Exception) {
                Log.e("JudgeTiming", "サウンド再生中にエラーが発生しました", e)
            }
        }
    }




    fun startJudging(clientId: String) {
        job = viewModelScope.launch(Dispatchers.Main) {
            val bpm = 72.75

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
                Log.d("JudgeTiming","ぴっ")
                nearBy?.enableReceiving()  // 受信を再度有効化
                hasReceivedHitTime = false // Reset flag
                hasReceivedId = false      // Reset ID flag

                // 判定タイミングの0.5 * delayMillis秒前かどうかを確認
                if (System.currentTimeMillis() < (nowtime + judgingThreshold)) {
                    Log.d("JudgeTiming", "まだ判定タイミング前なので、triggerJudgingをスキップします")
                    continue
                }
                triggerJudging(clientId)
            }
        }
    }


    fun triggerJudging(clientId: String) {

        // IDが受信されていない場合
        if (!hasReceivedId) {
            Log.d("JudgeTiming", "IDが受信されていないため、判定をスキップします")
            misscount += 1
            Log.d("JudgeTiming","うおおおおお $misscount")
            nowtime = System.currentTimeMillis() // nowtimeをリセット
            Log.d("JudgeTiming", "nowtimeをリセットしました: $nowtime")
            return
        }

//        val Judgementtiming = nowtime + 0.5 * (delayMillis)
        var timeDiff: Long = (nowtime - hitTime) // 判定タイミングとヒットタイムとの差

//         特定のクライアントIDなら +1500ms を timeDiff に加算
//        if (clientId == "atuo_2b77e0851dd47474") {
//            timeDiff += 1400
//        }
//
//        if (clientId == "atuo_264ac95f5a0c0fbc") {
//            timeDiff += 55
//        }



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
                tvgreat.text = "GOOD"
                Log.d("JudgeTiming", "GOODです")
                playSoundAsync("GOOD")
                Log.d("JudgeTiming","シャン")
                "GOOD"
            }
            else -> {
                tvgreat.text = "MISS"
                Log.d("JudgeTiming", "失敗(MISS)")
                playSoundAsync("MISS")
                Log.d("JudgeTiming","パフ")
                "MISS"
            }
        }

        clientId?.let {
            saveJudgement(it, judgement) // clientId を使って保存
        }

        postJudgement(judgement)
        Log.d("Judgement","一つの判定を終了")
        Log.d("JudgeTiming", "-------------------⭐︎")
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