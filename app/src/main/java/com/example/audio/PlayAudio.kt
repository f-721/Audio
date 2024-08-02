package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

class PlayAudio() {
    private var mediaPlayer: MediaPlayer? = null
    private val TAG = "PlayAudio"

    fun playAudio(context: Context) {
        val resourceName = "rhythmrally1"
        val resId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        mediaPlayer = MediaPlayer.create(context, resId)

        // 再生完了時に呼び出されるリスナーを設定
        mediaPlayer?.setOnCompletionListener {
            Log.d(TAG, "曲が終了しました")
            onAudioComplete()
        }

        mediaPlayer?.start()
    }

//    fun stopAudio() {
//        mediaPlayer?.stop()
//        mediaPlayer?.release()
//        mediaPlayer = null
//    }

    // 曲が終了したときの処理
    private fun onAudioComplete() {
        // ここに曲終了時の追加処理があれば書く
        Log.d(TAG, "曲の再生が完全に終了しました")
        // NearByの信号受信を終了する

    }
}
