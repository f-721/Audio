package com.example.audio

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import uk.me.berndporr.iirj.Butterworth
import kotlin.math.sqrt

class AccEstimation {

    val _isHit = MutableLiveData<Boolean>(false)
    val isHit: LiveData<Boolean> = _isHit

    val _isSwing = MutableLiveData<Boolean>(false)
    val isSwing: LiveData<Boolean> = _isSwing

    private val _accTest = MutableLiveData<String>("")
    val accTest: LiveData<String> = _accTest

    private val _lastHitTime = MutableLiveData<Long>(0L)
    val lastHitTime: LiveData<Long> = _lastHitTime

    private var lastHitTimeMillis = 0L
    private val butterworth = Butterworth()

    var timeflag = true
    var difftime: Long = 1
    var starttime = System.currentTimeMillis()
    var startendtime = System.currentTimeMillis()
    var max_acc: Double = 0.0
    var min_acc: Double = 0.0

    var bl_hit_updown = true
    var bl_swing_updown = true
    var hit_hantei = false
    var swing_hantei = false
    var bl_onhit = false
    var bl_onswing = false
    var hit_keep_thirty = 0

    var hit = 0.2
    var swing = 1.0

    fun filter(x: Float, y: Float, z: Float): Triple<Double, Double, Long> {
        val (butterworth_hx, butterworth_hy, butterworth_hz) = listOf(Butterworth(), Butterworth(), Butterworth())
        val (butterworth_lx, butterworth_ly, butterworth_lz) = listOf(Butterworth(), Butterworth(), Butterworth())

        butterworth_hx.highPass(10, 50.0, 15.0)
        butterworth_hy.highPass(10, 50.0, 15.0)
        butterworth_hz.highPass(10, 50.0, 15.0)
        butterworth_lx.lowPass(10, 50.0, 3.0)
        butterworth_ly.lowPass(10, 50.0, 3.0)
        butterworth_lz.lowPass(10, 50.0, 3.0)

        val (hx, hy, hz) = listOf(butterworth_hx.filter(x.toDouble()), butterworth_hy.filter(y.toDouble()), butterworth_hz.filter(z.toDouble()))
        val (lx, ly, lz) = listOf(butterworth_lx.filter(x.toDouble()), butterworth_ly.filter(y.toDouble()), butterworth_lz.filter(z.toDouble()))

        val highPassNorm = sqrt((hx * hx) + (hy * hy) + (hz * hz)) * 100
        val lowPassNorm = sqrt((lx * lx) + (ly * ly) + (lz * lz)) * 10000000

        val nowtime = System.currentTimeMillis()
        difftime = if (timeflag) {
            (startendtime - starttime).also { timeflag = false }
        } else {
            (nowtime - starttime)
        }

        return Triple(highPassNorm, lowPassNorm, difftime)
    }

    fun estimationIsHit(acc_num: Double, nowtime: Long) {
        if (nowtime > 4000) {
            if (bl_hit_updown) {
                if (acc_num > hit) {
                    hit_hantei = true
                    bl_hit_updown = false
                    bl_onhit = true
                    lastHitTimeMillis = System.currentTimeMillis()
                    _lastHitTime.postValue(lastHitTimeMillis)
//                    Log.d("AccEstimation", "lastHitTime LiveData updated: ${_lastHitTime.value}")
                    Log.d("Estimation", "ヒットしました")
//                    Log.d("Estimation", "Hit detected at time: $lastHitTimeMillis with acc_num = $acc_num")
                    _isHit.postValue(true)
//                    Log.d("Estimation", "${isHit.value} の中身")
                }
            } else {
                hit_keep_thirty += 1
                //_isHit.postValue(false)
                //Log.d("Estimation", "${isHit.value} の中身")
                if (hit_keep_thirty >= 30) {
                    bl_hit_updown = true
                    hit_keep_thirty = 0
                }
            }
        }
    }

    fun estimationIsSwing(acc_num: Double) {
        if (acc_num > max_acc) {
            max_acc = acc_num
        } else if (acc_num < max_acc) {
            if (bl_swing_updown) {
                val diffnum = max_acc - min_acc
                if (diffnum > swing) {
                    swing_hantei = true
                    bl_onswing = true
                    _isSwing.postValue(true)
//                    Log.d("Estimation", "スイング判定")
//                    Log.d("Estimation", "diff_num = $diffnum")
                }
                min_acc = max_acc
                bl_swing_updown = false
            }
        }
        if (acc_num < min_acc) {
            min_acc = acc_num
        } else if (acc_num > min_acc) {
            if (!bl_swing_updown) {
                max_acc = min_acc
                bl_swing_updown = true
            }
        }
    }
}
