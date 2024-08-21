package com.example.audio

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
import java.util.Queue

class JudgeTiming(
    private val accEstimation: AccEstimation,
    private val tvgreat: TextView
) : ViewModel() {
    lateinit var nearBy: NearBy

    // クライアントデバイスごとの結果を保存するためのマップ
    private val resultsMap = mutableMapOf<String, ResultData>()

    private val _judgement = MutableLiveData<String>()

    private val idQueue: Queue<String> = LinkedList()

    private var job: Job? = null
    private var hitTime = 0L
    private var hasReceivedHitTime = false // データ受信フラグ

    private val judgementCounts = mutableMapOf<String, JudgementCount>()

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
        // Process the received ID as needed
        Log.d("JudgeTiming", "IDを受信: $clientId")
        // Example: You might want to queue this ID or associate it with a hit time
        idQueue.add(clientId)
    }

    fun startJudging(clientId: String) {
        job = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(2000)
                hasReceivedHitTime = false // フラグをリセットする
                triggerJudging(clientId)
            }
        }
    }

    fun triggerJudging(clientId: String) {
        val nowtime = System.currentTimeMillis()
        val timeDiff =
            if (hitTime != 0L)
                (nowtime - hitTime) - 1000
            else {
                Long.MAX_VALUE //ヒット時刻がまだ設定されていない場合
            }

        Log.d("JudgeTiming", "-------------------")
        Log.d("JudgeTiming", "ゲーム内判定時刻: $nowtime ms")
        Log.d("JudgeTiming", "ヒット時刻: $hitTime ms")
        Log.d("JudgeTiming", "Time difference(ゲーム内判定時刻-ヒット時刻): $timeDiff ms")

        val judgement = when {
            timeDiff in -500..500 -> {
                tvgreat.text = "GREAT"
                Log.d("JudgeTiming", "GREATです")
                "GREAT"
            }
            timeDiff in -750..-501 || timeDiff in 501..750 -> {
                tvgreat.text = "GOOD"
                Log.d("JudgeTiming", "GOODです")
                "GOOD"
            }
            timeDiff in -999..-751 || timeDiff in 751..999 -> {
                tvgreat.text = "BAD"
                Log.d("JudgeTiming", "BADです")
                "BAD"
            }
            else -> {
                tvgreat.text = "MISS"
                Log.d("JudgeTiming", "失敗(MISS)")
                "MISS"
            }
        }

        saveJudgement(clientId,judgement)

        postJudgement(judgement)

        if (hitTime != 0L && (timeDiff < -1000 || timeDiff > 1000)) {
            hitTime = 0L
            Log.d("JudgeTiming", "判定リセットします！")
        }
        Log.d("JudgeTiming", "-------------------")
    }

    private fun saveJudgement(clientId: String, judgement: String) {
        val count = judgementCounts.getOrPut(clientId) { JudgementCount(id = clientId) }

        when (judgement) {
            "GREAT" -> count.greatCount++
            "GOOD" -> count.goodCount++
            "BAD" -> count.badCount++
            "MISS" -> count.missCount++
        }

        //Log.d("JudgeTiming", "クライアントID: $clientId の判定結果を保存しました: $judgement")
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