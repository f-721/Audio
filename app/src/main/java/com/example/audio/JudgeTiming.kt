package com.example.audio

import android.content.Context
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class JudgeTiming(
    private val accEstimation: AccEstimation,
    private val tvgreat: TextView,
    private val nearBy: NearBy
) : ViewModel() {

    private val _judgement = MutableLiveData<String>()
    val judgement: LiveData<String> get() = _judgement

    private var job: Job? = null
    private var lastHitTime = 0L

    private val hitObserver = Observer<Boolean> { isHit ->
        if (isHit) {
        }
        Log.d("JudgeTiming", "isHit changed: $isHit")
    }

    private val lastHitTimeObserver = Observer<Long> { newLastHitTime ->
        lastHitTime = newLastHitTime
        Log.d("JudgeTiming", "Observed lastHitTime: $newLastHitTime")
    }

    init {
        Log.d("JudgeTiming", "JudgeTiming initialized")
        accEstimation.isHit.observeForever(hitObserver)
        accEstimation.lastHitTime.observeForever(lastHitTimeObserver)
    }

    fun startJudging() {
        job = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(2000)
                triggerJudging()
            }
        }
    }

    fun stopJudging() {
        job?.cancel()
        accEstimation.isHit.removeObserver(hitObserver)
        accEstimation.lastHitTime.removeObserver(lastHitTimeObserver)
    }

    fun recordHitTime(hitTime: Long) {
        // hitTime を利用した処理
        Log.d("JudgeTiming", "受信したヒット時刻: $hitTime")
        // 例: ヒット時刻を利用した判定処理など
    }

    fun triggerJudging() {
        val nowtime = System.currentTimeMillis()
        val timeDiff =
            if (lastHitTime != 0L)
                (nowtime - lastHitTime) - 1000
            else 2001L

        Log.d("JudgeTiming", "-------------------")
        Log.d("JudgeTiming", "ゲーム内判定時刻: $nowtime ms")
        Log.d("JudgeTiming", "ヒット時刻: $lastHitTime ms")
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

        if (lastHitTime != 0L && (timeDiff < -1000 || timeDiff > 1000)) {
            lastHitTime = 0L
            Log.d("JudgeTiming", "判定リセットします！")
        }
        Log.d("JudgeTiming", "-------------------")
    }

    private fun postJudgement(judgement: String) {
        _judgement.postValue(judgement)
        Log.d("JudgeTiming", "Judgement: $judgement")
        Log.d("JudgeTiming", "-------------------")
    }

    fun processReceivedHitTime(hitTime: String) {
        Log.d("JudgeTiming", "Received hit time: $hitTime")
        val hitTimeLong = hitTime.toLongOrNull()
        if (hitTimeLong != null) {
            lastHitTime = hitTimeLong
            Log.d("JudgeTiming", "Updated lastHitTime: $lastHitTime")
        } else {
            Log.d("JudgeTiming", "Invalid hit time format: $hitTime")
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopJudging()
    }
}
