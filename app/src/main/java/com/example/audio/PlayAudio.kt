package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.MutableLiveData
import android.util.Log
import android.media.AudioTrack
import android.os.Build

class PlayAudio(private val nearBy: NearBy) {
    private var mediaPlayer: MediaPlayer? = null
    private val TAG = "PlayAudio"
    private var isPlaying = false
    private var audioTrack: AudioTrack? = null

    // 曲終了のイベントを通知するためのLiveData
    val isAudioComplete = MutableLiveData<Boolean>()

    fun playAudio(context: Context, judgeTiming: JudgeTiming) {
        val resourceName = "rhythmrally1"
        val resId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        // MediaPlayerインスタンスを新たに作成
        mediaPlayer = MediaPlayer.create(context, resId)

        if (mediaPlayer == null) {
            Log.e(TAG, "MediaPlayerの作成に失敗しました")
            return
        }

        isPlaying = true
        isAudioComplete.value = false // 曲再生開始時は未完了

        // 再生準備が整ったタイミングでstart()を呼ぶ
        mediaPlayer?.setOnPreparedListener {
            it.start()  // 準備が整ったら再生を開始
        }

        // 再生完了時に呼び出されるリスナーを設定
        mediaPlayer?.setOnCompletionListener {
            Log.d(TAG, "曲が終了しました")
            stopTimingSound(judgeTiming)
            isPlaying = false
            isAudioComplete.postValue(true)
        }

    }

    // ピッチを変更するメソッド
    fun changePitch(pitch: Float) {
        mediaPlayer?.let {
            try {
                val params = it.playbackParams
                params.pitch = pitch
                it.playbackParams = params
                Log.d("PlayAudio", "ピッチを変更しました: $pitch")
            } catch (e: Exception) {
                Log.e("PlayAudio", "ピッチ変更中にエラーが発生しました", e)
            }
        }
    }

    // ピッチを変更するメソッド
//    fun changePitch(pitch: Float) {
//        if (audioTrack != null && isPlaying) {
//            // ピッチを変更するには、再生速度を調整する
//            val playbackRate = (44100 * pitch).toInt()  // 標準サンプリングレート（44100Hz）
//            audioTrack?.setPlaybackRate(playbackRate)
//        }
//    }



    private fun stopTimingSound(judgeTiming: JudgeTiming) {
        // 曲が終了した際にTIMING音声も停止する処理を追加
        judgeTiming.stopTimingSound()
        audioTrack?.stop()
        audioTrack?.release()
        isPlaying = false
    }

    // 音源が再生中かどうかを確認するメソッド
    fun isPlaying(): Boolean {
        return isPlaying
    }
}