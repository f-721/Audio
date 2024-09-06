package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

class PlayAudio(private val nearBy: NearBy) {
    private var mediaPlayer: MediaPlayer? = null
    private val TAG = "PlayAudio"
    private var isPlaying = false

    fun playAudio(context: Context) {
        val resourceName = "rhythmrally1"
        val resId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        mediaPlayer = MediaPlayer.create(context, resId)
        isPlaying = true

        // 再生完了時に呼び出されるリスナーを設定
        mediaPlayer?.setOnCompletionListener {
            Log.d(TAG, "曲が終了しました")
            onAudioComplete()
            isPlaying = false
        }

        mediaPlayer?.start()
    }

    // 曲が終了したときの処理
    private fun onAudioComplete() {
        Log.d(TAG, "曲の再生が完全に終了しました")
        // NearByの信号受信を終了する
        nearBy.disconnect()
    }

    fun isPlaying():Boolean{
        return isPlaying
    }
}
