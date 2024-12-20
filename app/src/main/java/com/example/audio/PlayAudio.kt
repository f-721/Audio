package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.MutableLiveData
import android.util.Log
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

class PlayAudio(private val nearBy: NearBy) {
    private var mediaPlayer: MediaPlayer? = null
    private val TAG = "PlayAudio"
    private var isPlaying = false
    private var audioTrack: AudioTrack? = null
    private var audioData: ByteArray? = null  // 音源データ（バイナリデータ）

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

    // 音源のバイナリデータをセット
    fun setAudioData(audioData: ByteArray) {
        this.audioData = audioData
    }

    // ピッチを変更するメソッド
    fun changePitch(pitch: Float) {
        if (audioTrack != null && isPlaying) {
            // ピッチを変更するには、再生速度を調整する
            val playbackRate = (44100 * pitch).toInt()  // 標準サンプリングレート（44100Hz）
            audioTrack?.setPlaybackRate(playbackRate)
        }
    }

    // 音源の再生
    fun playAudio2(context: Context, judgeTiming: JudgeTiming) {
        if (audioData == null || audioData!!.isEmpty()) {
            Log.e(TAG, "音源データがセットされていません")
            return
        }

        // AudioTrackのインスタンスを作成
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            44100,  // サンプリングレート（44100Hz）
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            audioData!!.size,
            AudioTrack.MODE_STREAM
        )

        // 再生開始
        audioTrack?.play()
        isPlaying = true

        // 音源データをストリームに送信
        audioTrack?.write(audioData!!, 0, audioData!!.size)

        // 曲終了のための処理（stopTimingSound）
        stopTimingSound(judgeTiming)
    }

    // 音源データをセットする例
    fun loadAudioData(context: Context) {
        val resourceName = "your_audio_file_name"
        val resId = context.resources.getIdentifier(resourceName, "raw", context.packageName)

        // バイナリデータを取得
        val inputStream = context.resources.openRawResource(resId)
        val audioData = inputStream.readBytes()
        setAudioData(audioData)
    }




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