package com.example.audio

import android.util.Log
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class JudgeTiming(
    private val accEstimation: AccEstimation,
    private val tvgreat: TextView,
    nearBy: NearBy?,
) : ViewModel() {
    lateinit var nearBy: NearBy

    private val _judgement = MutableLiveData<String>()
    val judgement: LiveData<String> get() = _judgement

    private var job: Job? = null
    private var hitTime = 0L

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

    fun startJudging() {
        job = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                // delay(1000) を削除
                triggerJudging()
            }
        }
    }

    fun stopJudging() {
        job?.cancel()
        accEstimation.isHit.removeObserver(hitObserver)
        accEstimation.lastHitTime.removeObserver(hitTimeObserver)
    }

    fun recordHitTime(hitTime: Long) {
        Log.d("JudgeTiming", "受信したヒット時刻: $hitTime")
        this.hitTime = hitTime
        triggerJudging() // ヒット時刻を受信したら triggerJudging を実行
    }

    fun triggerJudging() {
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
                Log.d("JudgeTiming", "ばーか")
                "MISS"
            }
        }

        postJudgement(judgement)

        if (hitTime != 0L && (timeDiff < -1000 || timeDiff > 1000)) {
            hitTime = 0L
            Log.d("JudgeTiming", "判定リセットします！")
        }
        Log.d("JudgeTiming", "-------------------")
    }

    private fun postJudgement(judgement: String) {
        _judgement.postValue(judgement)
        Log.d("JudgeTiming", "Judgement: $judgement")
        Log.d("JudgeTiming", "-------------------")
    }

    override fun onCleared() {
        super.onCleared()
        stopJudging()
    }
}
