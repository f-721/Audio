package com.example.audio.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import com.example.audio.AccEstimation
import com.example.audio.AccSensor
import com.example.audio.JudgeTiming
import com.example.audio.NearBy
import com.example.audio.R

class MainFragment(private val nearBy: NearBy,private val judgeTiming: JudgeTiming) : Fragment() {

    private lateinit var tvjudge: TextView
    private lateinit var btnadvertise1: Button
    private lateinit var btnadvertise2: Button
    private lateinit var btndiscovery1: Button
    private lateinit var btndiscovery2: Button
    private lateinit var btnresult: Button
    private lateinit var CountTextview: TextView
    private lateinit var accSensor: AccSensor
    private lateinit var accEstimation: AccEstimation
    private lateinit var judgeTime: JudgeTiming

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.activity_main, container, false)

        btnadvertise1 = rootView.findViewById(R.id.btnadvertise)
        btnadvertise2 = rootView.findViewById(R.id.btnadvertise2)
        btndiscovery1 = rootView.findViewById(R.id.btndiscovery)
        btndiscovery2 = rootView.findViewById(R.id.btndiscovery2)
        btnresult = rootView.findViewById(R.id.btnresult)
        tvjudge = rootView.findViewById(R.id.tvgreat)
        CountTextview = rootView.findViewById(R.id.CountTextview)

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        accEstimation = AccEstimation() // AccEstimation の初期化

        // JudgeTiming の初期化
        judgeTime = JudgeTiming(accEstimation, tvjudge, nearBy)

        // NearBy の初期化
        nearBy.initializeNearby()

        // AccSensor の初期化
        accSensor = AccSensor(
            requireContext(),
            tvjudge,
            accEstimation,
            nearBy,
            judgeTime
        )

        nearBy.setConnectionCountListener(object : NearBy.ConnectionCountListener {
            override fun onConnectionCountChanged(count: Int) {
                requireActivity().runOnUiThread {
                    CountTextview.isInvisible = count == 0
                    CountTextview.text = "接続中: $count 台"
                }
            }
        })

        btnadvertise1.setOnClickListener {
            Log.d("MainFragment", "advertise button 1 clicked")
            nearBy.advertise()
        }

        btnadvertise2.setOnClickListener {
            Log.d("MainFragment", "advertise button 2 clicked")
            nearBy.advertise2()
        }

        btndiscovery1.setOnClickListener {
            Log.d("MainFragment", "discovery button 1 clicked")
            nearBy.discovery()
        }

        btndiscovery2.setOnClickListener {
            Log.d("MainFragment", "discovery button 2 clicked")
            nearBy.discovery2()
        }

        btnresult.setOnClickListener {
            Log.d("MainFragment", "btnresult button clicked")

            // ここでは `judgeTiming` というオブジェクトが結果を持っていると仮定します
            val client1Results = judgeTiming.getResultsForClient("client1_id")
            val client2Results = judgeTiming.getResultsForClient("client2_id")

            // 両方のデータがない場合
            if (client1Results == null && client2Results == null) {
                // 「データがありません」とメッセージを表示する
                Toast.makeText(context, "データがありません", Toast.LENGTH_SHORT).show()
            } else {
                // 結果を表示するための文字列を作成
                val results = StringBuilder()
                client1Results?.let {
                    results.append("クライアントデバイス1 (ID: ${it.id}):\n")
                    results.append("GREAT: ${it.greatCount}\n")
                    results.append("GOOD: ${it.goodCount}\n")
                    results.append("BAD: ${it.badCount}\n")
                    results.append("MISS: ${it.missCount}\n\n")
                }
                client2Results?.let {
                    results.append("クライアントデバイス2 (ID: ${it.id}):\n")
                    results.append("GREAT: ${it.greatCount}\n")
                    results.append("GOOD: ${it.goodCount}\n")
                    results.append("BAD: ${it.badCount}\n")
                    results.append("MISS: ${it.missCount}\n")
                }

                // 結果を画面に表示する
                AlertDialog.Builder(requireContext())
                    .setTitle("リズムゲーム結果")
                    .setMessage(results.toString())
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

    }

    companion object {
        fun newInstance(nearBy: NearBy,judgeTiming: JudgeTiming): MainFragment {
            return MainFragment(nearBy,judgeTiming)
        }
    }
}
