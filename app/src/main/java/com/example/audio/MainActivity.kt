package com.example.audio

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.example.audio.ui.main.MainFragment
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity() {

    private lateinit var nearBy: NearBy
    private lateinit var playAudio: PlayAudio

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace<MainFragment>(R.id.container)
            }
        }

        nearBy = NearBy(this)
        playAudio = PlayAudio()

        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )

        if (!EasyPermissions.hasPermissions(this, *permissions)) {
            EasyPermissions.requestPermissions(this, "パーミッションに関する説明", 1, *permissions)
        }

        nearBy.advertise()
    }

    private fun handleReceivedData(data: String) {
        when (data) {
            "GREAT" -> playAudio.playAudio("great_sound", this)
            "GOOD" -> playAudio.playAudio("good_sound", this)
            "BAD" -> playAudio.playAudio("bad_sound", this)
            "MISS" -> playAudio.playAudio("miss_sound", this)
        }
        Log.d("MainActivity", "Playing sound for: $data")
    }
}
