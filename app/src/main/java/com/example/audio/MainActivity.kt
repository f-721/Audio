package com.example.audio

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.example.audio.ui.main.MainFragment
import pub.devrel.easypermissions.EasyPermissions
import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var nearBy: NearBy
    private lateinit var playAudio: PlayAudio
    private lateinit var judgeTiming: JudgeTiming

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playAudio = PlayAudio()

        judgeTiming = JudgeTiming(
            accEstimation = AccEstimation(),
            tvgreat = findViewById(R.id.tvgreat)
        )

        nearBy = NearBy(this, judgeTiming)

        // nearByにjudgeTimingをセット
        judgeTiming.setNearBy(nearBy)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.container, MainFragment.newInstance(nearBy))
            }

            //xmlで定義したパーミッションの権限を付与
            val permissions = arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
            //パーミッションの許可
            if (!EasyPermissions.hasPermissions(this, *permissions)) {
                EasyPermissions.requestPermissions(this, "パーミッションに関する説明", 1, *permissions)
            }
        }
    }
}
