package com.example.audio

import android.Manifest
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.example.audio.ui.main.MainFragment
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity() {

    private lateinit var nearBy: NearBy
    private lateinit var playAudio: PlayAudio
    private lateinit var judgeTiming: JudgeTiming

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nearBy = NearBy(this, null)  // ここで一旦nullを渡す

        playAudio = PlayAudio(nearBy)

        judgeTiming = JudgeTiming(
            accEstimation = AccEstimation(),
            tvgreat = findViewById(R.id.tvgreat),
            nearBy = nearBy,
            context = this,
            playAudio = playAudio,
            tvjudge = TextView(this)
        )

        nearBy.setJudgeTiming(judgeTiming)  // 後からjudgeTimingをセット
        nearBy.setPlayAudio(playAudio)

        nearBy.initializeJudgeTiming(
            accEstimation = AccEstimation(),
            tvjudge = findViewById(R.id.tvgreat),
            playAudio = playAudio
        )

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.container, MainFragment.newInstance(nearBy, judgeTiming))
            }

            val permissions = arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )

            if (!EasyPermissions.hasPermissions(this, *permissions)) {
                EasyPermissions.requestPermissions(this, "パーミッションに関する説明", 1, *permissions)
            }
        }
    }
}