package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy

class NearBy(private val context: Context,private val judgeTiming: JudgeTiming) {
    var SERVICE_ID = "atuo.nearby"
    var nickname: String
    val TAG = "myapp"
    var startcount = 0
//    val accEstimation = AccEstimation()
//    val nearbyManager = NearBy(context)
    private var connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private var startSignalReceived = mutableSetOf<String>()
    private lateinit var JudgeTiming : JudgeTiming
    private lateinit var playAudio: PlayAudio
   // private var connectionsClient: ConnectionsClient
    private var isConnected: Boolean = false
    private lateinit var endpointId: String
    private lateinit var mediaPlayer: MediaPlayer

    fun setJudgeTiming(judgeTiming: JudgeTiming) {
        this.JudgeTiming = judgeTiming
    }

    init {
        playAudio = PlayAudio()
//        JudgeTiming = JudgeTiming(accEstimation, tvgreat, nearbyManager)
    }
    interface ConnectionCountListener {
        fun onConnectionCountChanged(count: Int)
    }
    private var connectionCountListener: ConnectionCountListener? = null

    init {
        connectionsClient = Nearby.getConnectionsClient(context)
        nickname = generateUniqueNickname(context)
    }

    private fun generateUniqueNickname(context: Context): String {
        // Android IDを利用してユニークなニックネームを生成する
        return "atuo_${Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)}"
    }

    fun setConnectionCountListener(listener: ConnectionCountListener) {
        connectionCountListener = listener
    }

    fun initializeNearby() {
        connectionsClient = Nearby.getConnectionsClient(context)
    }

    // ID保存
    private val connectedEndpoints = mutableListOf<String>()

    fun sendTimeDiff(timeDiff: Long) {
        if (::endpointId.isInitialized) {
            val payload = Payload.fromBytes(timeDiff.toString().toByteArray())
            Nearby.getConnectionsClient(context).sendPayload(endpointId, payload)
        } else {
            Log.d(TAG, "Endpoint ID is not initialized")
        }
    }

    fun sendJudgement(judgement: String) {
        if (::endpointId.isInitialized) {
            val payload = Payload.fromBytes(judgement.toByteArray())
            Nearby.getConnectionsClient(context).sendPayload(endpointId, payload)
        } else {
            Log.d(TAG, "Endpoint ID is not initialized")
        }
    }

    fun advertise() {
        if (isConnected) {
            Log.d(TAG, "既に接続済みです")
            return
        }
        Log.d(TAG, "advertiseをタップ!!!!!")
        Nearby.getConnectionsClient(context)
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
        if (isConnected) {
            Log.d(TAG, "既に接続済みです")
            return
        }
        Log.d(TAG, "advertise2をタップ!?!?!?")
        Nearby.getConnectionsClient(context)
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
        if (isConnected) {
            Log.d(TAG, "既に接続済みです")
            return
        }
        Log.d(TAG, "Discoveryをタップ")
        Nearby.getConnectionsClient(context)
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
        if (isConnected) {
            Log.d(TAG, "既に接続済みです")
            return
        }
        Log.d(TAG, "Discovery2をタップ")
        Nearby.getConnectionsClient(context)
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
            Nearby.getConnectionsClient(context)
                .requestConnection(nickname, endpointId, mConnectionLifecycleCallback)
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "見つけたエンドポイントを見失った")
        }
    }

    private val mConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d(TAG, "他の端末からコネクションのリクエストを受け取った")
            this@NearBy.endpointId = endpointId
            Nearby.getConnectionsClient(context)
                .acceptConnection(endpointId, mPayloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            Log.d(TAG, "コネクションリクエストの結果を受け取った時")
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "コネクションが確立した。今後通信が可能。")
                    connectedEndpoints.add(endpointId)
                    Log.d(TAG, "通信成功")
                    Toast.makeText(context, "接続成功", Toast.LENGTH_SHORT).show()
                    isConnected = true
                    // 接続数の変更をリスナーに通知
                    connectionCountListener?.onConnectionCountChanged(connectedEndpoints.size)
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
            // 接続数の変更をリスナーに通知
            connectionCountListener?.onConnectionCountChanged(connectedEndpoints.size)
        }
    }

    private val mPayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val data = payload.asBytes() ?: return
                val message = String(data)
                Log.d(TAG, "メッセージを受信: $message")

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

    fun initializeJudgeTiming(accEstimation: AccEstimation, tvjudge: TextView) {
        JudgeTiming = JudgeTiming(accEstimation, tvjudge, this)
    }
    private fun checkStartSignals() {
        if (!::JudgeTiming.isInitialized) {
            Log.e(TAG, "JudgeTiming が初期化されていません")
            return
        }
        if (startcount == 2) {
            Log.d(TAG, "5秒後に曲流すよ")
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                if (::playAudio.isInitialized) {
                    playAudio.playAudio(context)
                    JudgeTiming.startJudging()
                } else {
                    Log.e(TAG, "playAudioが初期化されていません")
                }
            }, 5000)
        }
    }

    private fun handleHitTime(hitTime: String) {
        Log.d(TAG, "Received hit time: $hitTime")
        // Convert hitTime to Long and process it as needed
        val hitTimeLong = hitTime.toLongOrNull()
        if (hitTimeLong != null) {
            // Process the hit time
        } else {
            Log.d(TAG, "Invalid hit time format: $hitTime")
        }
    }

    private fun handleId(id: String) {
        Log.d(TAG, "Received ID: $id")
        // Process the ID
    }

}
