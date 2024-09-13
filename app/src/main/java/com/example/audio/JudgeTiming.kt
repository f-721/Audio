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
import java.util.LinkedList
import android.media.MediaPlayer
import androidx.fragment.app.Fragment
import java.util.Queue

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

    fun recordHitTime(hitTime: Long) {
        if (!hasReceivedHitTime) {
            Log.d("JudgeTiming", "受信したヒット時刻: $hitTime")
            this.hitTime = hitTime
            hasReceivedHitTime = true // データ受信フラグを立てる
        } else {
            Log.d("JudgeTiming", "データは既に受信されました")
        }
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

    private fun playSound(resId: Int) {
        mediaPlayer?.release()  // 既存の MediaPlayer を解放
        mediaPlayer = MediaPlayer.create(context, resId)
        mediaPlayer?.setOnCompletionListener {
            it.release()  // 再生が完了したらリソースを解放
        }
        mediaPlayer?.start()
    }

    fun startJudging(clientId: String) {
        job = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                //delay(1500)
                delay(1000)
                Log.d("JudgeTiming","⭐︎⭐︎⭐︎⭐︎⭐︎⭐︎⭐︎⭐︎")
                Log.d("JudgeTiming","音ゲー判定してるで！！")
                Log.d("JudgeTiming","⭐︎⭐︎⭐︎⭐︎⭐︎⭐︎⭐︎⭐︎")
//                mediaPlayer = MediaPlayer.create(context, R.raw.timing) //判定の音源
//                mediaPlayer?.start()
                playSound(R.raw.timing)
                hasReceivedHitTime = false // フラグをリセットする
                hasReceivedId = false      // ID受信フラグもリセットする
                triggerJudging(clientId)
            }
        }
    }

    fun triggerJudging(clientId: String) {

        if (!hasReceivedId) {
            Log.d("JudgeTiming", "IDが受信されていないため、判定をスキップします")
            return
        }

        val nowtime = System.currentTimeMillis()

        // 1秒ごとの判定タイミングを生成
        val lastJudgementTime = (nowtime / 1000) * 1000 // 最後の1秒間隔のタイムスタンプ
        val timeDiff = (nowtime - hitTime) // 判定タイミングとヒットタイムとの差

        Log.d("JudgeTiming", "-------------------")
        Log.d("JudgeTiming", "ゲーム内判定時刻: $nowtime ms")
        Log.d("JudgeTiming", "ヒット時刻: $hitTime ms")
        Log.d("JudgeTiming", "Time difference(ヒット時刻との差): $timeDiff ms")

        // 判定の範囲を更新した -250ms 〜 +250ms, -375ms 〜 -251ms および 251ms 〜 375ms に設定
//        val judgement = when {
//            timeDiff in -250..250 -> {
//                tvgreat.text = "GREAT"
//                Log.d("JudgeTiming", "GREATです")
//                mediaPlayer = MediaPlayer.create(context, R.raw.greatsounds) //判定の音源
//                mediaPlayer?.start()
//                "GREAT"
//            }
//            timeDiff in -375..-251 || timeDiff in 251..375 -> {
//                tvgreat.text = "GOOD"
//                Log.d("JudgeTiming", "GOODです")
//                mediaPlayer = MediaPlayer.create(context, R.raw.goodsounds)
//                mediaPlayer?.start()
//                "GOOD"
//            }
//            timeDiff in -499..-376 || timeDiff in 376..499 -> {
//                tvgreat.text = "BAD"
//                Log.d("JudgeTiming", "BADです")
//                mediaPlayer = MediaPlayer.create(context, R.raw.badsounds)
//                mediaPlayer?.start()
//                "BAD"
//            }
//            else -> {
//                tvgreat.text = "MISS"
//                Log.d("JudgeTiming", "失敗(MISS)")
//                mediaPlayer = MediaPlayer.create(context, R.raw.misssounds)
//                mediaPlayer?.start()
//                "MISS"
//            }
        // これ音ゲーとして不可能だ、難易度高スンギ

        val judgement = when {
            timeDiff in -500..500 -> {
                tvgreat.text = "GOOD"
                Log.d("JudgeTiming", "GOODです")
                mediaPlayer = MediaPlayer.create(context, R.raw.greatsounds) //判定の音源
                mediaPlayer?.start()
                "GOOD"
            }
            else -> {
                tvgreat.text = "MISS"
                Log.d("JudgeTiming", "失敗(MISS)")
                mediaPlayer = MediaPlayer.create(context, R.raw.misssounds)
                mediaPlayer?.start()
                "MISS"
            }
        }
        nearBy?.enableReceiving()  // 受信を再度有効化

        clientId?.let {
            saveJudgement(it, judgement) // clientId を使って保存
        }

        postJudgement(judgement)
        Log.d("Judgement","一つの判定を終了")
        Log.d("JudgeTiming", "-------------------")
    }


    private fun saveJudgement(clientId: String, judgement: String) {
        if (clientId.contains("atuo_")) {
            val count = judgementCounts.getOrPut(clientId) { JudgementCount(id = clientId) }

            when (judgement) {
                "GREAT" -> count.greatCount++
                "GOOD" -> count.goodCount++
                "BAD" -> count.badCount++
                "MISS" -> count.missCount++
            }

            Log.d("JudgeTiming", "クライアントID: $clientId の判定結果を保存しました: $judgement")
        }
//         else {
//            Log.d("JudgeTiming", "クライアントID: $clientId に「atuo_」が含まれていないため、判定はカウントされませんでした")
//        }
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
        stopJudging()
    }
}