//package com.example.audio.ui.main
//
//import android.os.Bundle
//import android.util.Log
//import android.widget.Button
//import androidx.appcompat.app.AppCompatActivity
//import androidx.fragment.app.Fragment
//import com.example.audio.JudgeTiming
//import com.example.audio.NearBy
//import com.example.audio.R
//
//class Main : AppCompatActivity() {
//
//    private lateinit var btnokura: Button
//    private lateinit var btnmaimu: Button
//    private lateinit var nearBy: NearBy
//    private lateinit var judgeTiming: JudgeTiming
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.fragment_main) // レイアウトファイル名に合わせて設定
//
//        btnokura = findViewById(R.id.btnokura) // Okuraボタン
//        btnmaimu = findViewById(R.id.btnmaimu) // Maimuボタン
//
//
//        btnokura.setOnClickListener {
//            Log.d("Main", "btnokura clicked")
//            replaceFragment(MainFragment.newInstance(nearBy, judgeTiming))
//        }
//
//        btnmaimu.setOnClickListener {
//            Log.d("Main", "btnmaimu clicked")
//            replaceFragment(MainFragment2.newInstance(nearBy, judgeTiming))
//        }
//    }
//
//    // フラグメントを切り替える共通メソッド
//    private fun replaceFragment(fragment: Fragment) {
//        supportFragmentManager.beginTransaction()
//            .commit()
//    }
//}
