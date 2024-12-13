package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.*

class NearBy(private val context: Context, private var judgeTiming: JudgeTiming?) {
    var SERVICE_ID = "atuo.nearby"
    var nickname: String
    val TAG = "Nearby"
    var startcount = 0
    private var connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private var startSignalReceived = mutableSetOf<String>()
    private lateinit var JudgeTiming: JudgeTiming
    private lateinit var playAudio: PlayAudio
    private var isConnected: Boolean = false
    private lateinit var endpointId: String
    private var canReceive = true

    private lateinit var mediaPlayer: MediaPlayer
    private val connectedEndpoints = mutableListOf<String>()
    private val maxConnections = 2

    init {
        connectionsClient = Nearby.getConnectionsClient(context)
        nickname = generateUniqueNickname(context)
    }

    // PlayAudioを外部からセットするメソッド
    fun setPlayAudio(playAudio: PlayAudio) {
        this.playAudio = playAudio
    }

    fun isConnected(): Boolean {
        return connectedEndpoints.size >= maxConnections
    }

    fun sendCurrentTimeToClients() {
        val currentTimeMillis = System.currentTimeMillis()
        val message = "TIME:$currentTimeMillis"

        connectedEndpoints.forEach { endpointId ->
            connectionsClient.sendPayload(endpointId, Payload.fromBytes(message.toByteArray()))
            Log.d("Nearby", "時刻を送信しました: $message to $endpointId")
        }
    }


    interface ConnectionCountListener {
        fun onConnectionCountChanged(count: Int)
    }

    private var connectionCountListener: ConnectionCountListener? = null

    private fun generateUniqueNickname(context: Context): String {
        return "atuo_${
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        }"
    }

    fun enableReceiving() {
        canReceive = true
        Log.d("Nearby", "受信が有効になりました")
    }

    fun disableReceiving() {
        canReceive = false
        Log.d("Nearby", "受信が無効になりました")
    }

    fun setConnectionCountListener(listener: ConnectionCountListener) {
        connectionCountListener = listener
    }

    fun initializeNearby() {
        connectionsClient = Nearby.getConnectionsClient(context)
    }

    fun advertise() {
        if (connectedEndpoints.size >= maxConnections) {
            Log.d(TAG, "既に最大接続数に達しています")
            return
        }
        Log.d(TAG, "advertiseをタップ!!!!!")
        connectionsClient
            .startAdvertising(
                nickname,
                SERVICE_ID,
                mConnectionLifecycleCallback,
                AdvertisingOptions(Strategy.P2P_STAR)
            )
            .addOnSuccessListener {
                Log.d(TAG, "Advertise開始した!!!")
            }
            .addOnFailureListener {
                Log.d(TAG, "Advertiseできなかった...")
            }
    }


    private val mEndpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(
            endpointId: String,
            discoveredEndpointInfo: DiscoveredEndpointInfo
        ) {
            Log.d(TAG, "Advertise側を発見した")
            connectionsClient.requestConnection(nickname, endpointId, mConnectionLifecycleCallback)
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "見つけたエンドポイントを見失った")
        }
    }

    private val mConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d(TAG, "他の端末からコネクションのリクエストを受け取った")
            this@NearBy.endpointId = endpointId
            connectionsClient.acceptConnection(endpointId, mPayloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            Log.d(TAG, "コネクションリクエストの結果を受け取った時")
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "コネクションが確立した。今後通信が可能。")
                    connectedEndpoints.add(endpointId)
                    Log.d(TAG, "通信成功")
                    Toast.makeText(context, "接続成功", Toast.LENGTH_SHORT).show()
                    isConnected = connectedEndpoints.size >= maxConnections
                    connectionCountListener?.onConnectionCountChanged(connectedEndpoints.size)
                    if (connectedEndpoints.size >= maxConnections) {
                        connectionsClient.stopAdvertising()
                        connectionsClient.stopDiscovery()
                    }
                }

                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d(TAG, "コネクションが拒否された時。通信はできない。")
                }

                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.d(TAG, "エラーでコネクションが確立できない時。通信はできない。")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "コネクションが切断された")
            connectedEndpoints.remove(endpointId)
            startSignalReceived.clear()
            startcount = 0  // Reset the start count
            isConnected = false
            connectionCountListener?.onConnectionCountChanged(connectedEndpoints.size)

            Log.d(TAG, "スタート信号がリセットされました")
        }
    }

    private val mPayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val data = payload.asBytes() ?: return
                val message = String(data)
                //Log.d(TAG, "メッセージを受信: $message")

                if (message.startsWith("TIME:")) {
                    val hitTimeString = message.removePrefix("TIME:")
                    val hitTime = hitTimeString.toLongOrNull()
                    if (hitTime != null) {
                        judgeTiming?.recordHitTime(hitTime)
                    } else {
                        Log.d(TAG, "無効なヒット時刻形式: $hitTimeString")
                    }
                } else if (message.startsWith("ID:")) {
                    val id = message.removePrefix("ID:")
                    Log.d(TAG, "受信したID: $id")
                    // 曲が再生されているか確認する
                    if (::playAudio.isInitialized && playAudio.isPlaying()) {
                        judgeTiming?.recordid(id) // IDをカウントする
                    } else {
                        Log.d(TAG, "曲が再生されていないため、IDを無視しました: $id")
                    }
                } else if (message == "start" && !startSignalReceived.contains(endpointId)) {
                    startSignalReceived.add(endpointId)
                    Log.d(TAG, "受け取ったスタート信号 = ${startSignalReceived.size}")
                    startcount += 1
                    checkStartSignals()
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // 転送状態が更新された時の詳細は省略
        }
    }

    fun setJudgeTiming(judgeTiming: JudgeTiming) {
        this.judgeTiming = judgeTiming
    }

    fun initializeJudgeTiming(
        accEstimation: AccEstimation,
        tvjudge: TextView,
        playAudio: PlayAudio
    ) {
        if (judgeTiming == null) {
            judgeTiming = JudgeTiming(
                accEstimation = accEstimation,
                tvjudge = tvjudge,
                nearBy = this,
                tvgreat = TextView(context),
                context = context,
                playAudio = playAudio
            )
            Log.d(TAG, "JudgeTimingを初期化しました")
        } else {
            Log.d(TAG, "既存のJudgeTimingを利用します")
        }
    }

    private fun checkStartSignals() {
        if (judgeTiming == null) {
            Log.e(TAG, "JudgeTiming が初期化されていません")
            return
        }


        if (startcount == maxConnections) {
            Log.d(TAG, "このテンポでラリーしてね")
            Toast.makeText(context, "このテンポでラリーしてください", Toast.LENGTH_SHORT).show()

            val countdownSounds = listOf(
                R.raw.countdown,
                R.raw.countdown,
                R.raw.countdown
            )

            val handler = android.os.Handler(Looper.getMainLooper())
            val bpm = 69 // 使用する音楽のBPM
            val delayMillis = (60_000 / bpm).toLong()

            for (i in countdownSounds.indices) {
                handler.postDelayed({
                    playSound(countdownSounds[i]) // delayMillisごとにカウントダウン音を再生
                }, (i * delayMillis))
            }

            handler.postDelayed({
                playAudio?.let {
                    it.playAudio(context, judgeTiming = judgeTiming!!)
                    judgeTiming!!.startJudging(endpointId)
                } ?: Log.e(TAG, "playAudioが初期化されていません")
            }, delayMillis * countdownSounds.size)
        }
    }


    // 音を再生するメソッド
    private fun playSound(soundResId: Int) {
        val mediaPlayer = MediaPlayer.create(context, soundResId)
        mediaPlayer.setOnCompletionListener {
            it.release() // 音が再生し終わったらMediaPlayerを解放
            maxConnections == 0
        }
        mediaPlayer.start() // 音を再生
    }

}