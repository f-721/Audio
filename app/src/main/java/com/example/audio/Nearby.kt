package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*

class NearBy(private val context: Context, private var judgeTiming: JudgeTiming) {
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
//        playAudio = PlayAudio()
        connectionsClient = Nearby.getConnectionsClient(context)
        nickname = generateUniqueNickname(context)
    }

    // PlayAudioを外部からセットするメソッド
    fun setPlayAudio(playAudio: PlayAudio) {
        this.playAudio = playAudio
    }


    interface ConnectionCountListener {
        fun onConnectionCountChanged(count: Int)
    }

    private var connectionCountListener: ConnectionCountListener? = null

    private fun generateUniqueNickname(context: Context): String {
        return "atuo_${Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)}"
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

    fun advertise2() {
        if (connectedEndpoints.size >= maxConnections) {
            Log.d(TAG, "既に最大接続数に達しています")
            return
        }
        Log.d(TAG, "advertise2をタップ!?!?!?")
        connectionsClient
            .startAdvertising(
                nickname,
                SERVICE_ID,
                mConnectionLifecycleCallback,
                AdvertisingOptions(Strategy.P2P_STAR)
            )
            .addOnSuccessListener {
                Log.d(TAG, "Advertise2開始したwww")
            }
            .addOnFailureListener {
                Log.d(TAG, "Advertise2できなかった!!!!")
            }
    }

    fun discovery() {
        if (connectedEndpoints.size >= maxConnections) {
            Log.d(TAG, "既に最大接続数に達しています")
            return
        }
        Log.d(TAG, "Discoveryをタップ")
        connectionsClient
            .startDiscovery(
                SERVICE_ID,
                mEndpointDiscoveryCallback,
                DiscoveryOptions(Strategy.P2P_STAR)
            )
            .addOnSuccessListener {
                Log.d(TAG, "Discovery開始した")
            }
            .addOnFailureListener {
                Log.d(TAG, "Discovery開始できなかった")
            }
    }

    fun discovery2() {
        if (connectedEndpoints.size >= maxConnections) {
            Log.d(TAG, "既に最大接続数に達しています")
            return
        }
        Log.d(TAG, "Discovery2をタップ")
        connectionsClient
            .startDiscovery(
                SERVICE_ID,
                mEndpointDiscoveryCallback,
                DiscoveryOptions(Strategy.P2P_STAR)
            )
            .addOnSuccessListener {
                Log.d(TAG, "Discovery2開始した")
            }
            .addOnFailureListener {
                Log.d(TAG, "Discovery2開始できなかった")
            }
    }

    private val mEndpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
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
            isConnected = false
            connectionCountListener?.onConnectionCountChanged(connectedEndpoints.size)
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
                        judgeTiming.recordHitTime(hitTime)
                    } else {
                        Log.d(TAG, "無効なヒット時刻形式: $hitTimeString")
                    }
                } else if (message.startsWith("ID:")) {
                    val id = message.removePrefix("ID:")
                    Log.d(TAG, "受信したID: $id")
                    judgeTiming.recordid(id) //これはIDの数数えるやつ
                    // IDの処理
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

    fun disconnect(){
        connectionsClient.stopAllEndpoints()
        // Stop judging
        if (::JudgeTiming.isInitialized) {
            JudgeTiming.stopJudging()
            Log.d("NearBy", "JudgeTiming stopped.")
        } else {
            Log.d("NearBy", "JudgeTiming was not initialized.")
        }

        Log.d("NearBy", "悪りぃけど接続、勝手に切らせてもらうぜBaby...")
    }
    fun setJudgeTiming(judgeTiming: JudgeTiming) {
        this.JudgeTiming = judgeTiming
    }

    private fun checkStartSignals() {
        if (!::JudgeTiming.isInitialized) {
            Log.e(TAG, "JudgeTiming が初期化されていません")
            return
        }
        if (startcount == maxConnections) {
            Log.d(TAG, "5秒後に曲流すよ")
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                if (::playAudio.isInitialized) {
                    playAudio.playAudio(context)
                    val clientID = endpointId
                    JudgeTiming.startJudging(clientID)
                } else {
                    Log.e(TAG, "playAudioが初期化されていません")
                }
            }, 5000)
        }
    }
}