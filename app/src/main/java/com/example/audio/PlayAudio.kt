package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.MutableLiveData
import android.util.Log

class PlayAudio(private val nearBy: NearBy) {
    private var mediaPlayer: MediaPlayer? = null
    private val TAG = "PlayAudio"
    private var isPlaying = false

    // 曲終了のイベントを通知するためのLiveData
    val isAudioComplete = MutableLiveData<Boolean>()

    fun playAudio(context: Context, judgeTiming: JudgeTiming) {
        val resourceName = "rhythmrally1"
        val resId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        mediaPlayer = MediaPlayer.create(context, resId)
        isPlaying = true
        isAudioComplete.value = false // 曲再生開始時は未完了

        // 再生完了時に呼び出されるリスナーを設定
        mediaPlayer?.setOnCompletionListener {
            Log.d(TAG, "曲が終了しました")
            stopTimingSound(judgeTiming)
            isPlaying = false
            isAudioComplete.postValue(true)
        }

        mediaPlayer?.start()
    }

    private fun stopTimingSound(judgeTiming: JudgeTiming) {
        // 曲が終了した際にTIMING音声も停止する処理を追加
        judgeTiming.stopTimingSound()
    }

    // 音源が再生中かどうかを確認するメソッド
    fun isPlaying(): Boolean {
        return isPlaying
    }
}