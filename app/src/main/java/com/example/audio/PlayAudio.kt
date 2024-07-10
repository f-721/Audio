package com.example.audio

import android.content.Context
import android.media.MediaPlayer

class PlayAudio {
    private var mediaPlayer: MediaPlayer? = null

    fun playAudio(resourceName: String, context: Context) {
        val resId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        mediaPlayer = MediaPlayer.create(context, resId)
        mediaPlayer?.start()
    }

    fun stopAudio() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
