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
import android.media.MediaPlayer
import android.view.Gravity
import com.example.audio.NearBy
import com.example.audio.PlayAudio
import com.example.audio.R

class MainFragment(private val nearBy: NearBy,private val judgeTiming: JudgeTiming) : Fragment() {

    private lateinit var tvjudge: TextView
    private lateinit var btnadvertise1: Button
    private lateinit var btndouki: Button
    private lateinit var btnresult: Button
    private lateinit var CountTextview: TextView
    private lateinit var accSensor: AccSensor
    private lateinit var accEstimation: AccEstimation
    private lateinit var judgeTime: JudgeTiming
    private var mediaPlayer: MediaPlayer? = null
    private  lateinit var btnresultdelete: Button

    // ここでは `judgeTiming` というオブジェクトが結果を持っていると仮定します
    private var client1Results = judgeTiming.getResultsForClient("atuo_2b77e0851dd47474")
    private var client2Results = judgeTiming.getResultsForClient("atuo_264ac95f5a0c0fbc")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.activity_main, container, false)
        return inflater.inflate(R.layout.fragment_main, container, false)

        btnadvertise1 = rootView.findViewById(R.id.btnadvertise)
        btndouki = rootView.findViewById(R.id.btndouki)
        btnresult = rootView.findViewById(R.id.btnresult)
        tvjudge = rootView.findViewById(R.id.tvgreat)
        CountTextview = rootView.findViewById(R.id.CountTextview)
        btnresultdelete = rootView.findViewById(R.id.btnresultdelete)

        return rootView
    }

    private fun onBtndoukiClicked() {
        if (nearBy.isConnected()) { // 接続状態を確認
            Log.d("MainFragment", "接続されています。時刻同期を開始します。")
            Toast.makeText(requireContext(), "時刻同期を開始", Toast.LENGTH_SHORT).show()
            synchronizeTime() // 時刻同期の実行
        } else {
            Log.d("MainFragment", "接続されていません。接続を確認してください。")
            Toast.makeText(requireContext(), "接続を確認してください", Toast.LENGTH_SHORT).show()
        }
    }

    private fun synchronizeTime() {
        // NearBy から現在時刻をクライアントに送信
        try {
            nearBy.sendCurrentTimeToClients()
            Toast.makeText(requireContext(), "時刻同期が完了しました", Toast.LENGTH_SHORT).show()
            Log.d("MainFragment", "時刻同期が完了しました")
        } catch (e: Exception) {
            // エラーが発生した場合の処理
            Toast.makeText(requireContext(), "時刻同期に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("MainFragment", "時刻同期に失敗しました", e)
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        accEstimation = AccEstimation() // AccEstimation の初期化

        // JudgeTiming の初期化
        judgeTime = JudgeTiming(
            accEstimation,
            tvjudge,
            nearBy,
            requireContext(),
            PlayAudio(nearBy),
            tvjudge
        )

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


        // ここでは新たに初期化せず、`judgeTiming`を直接使用する
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

        btndouki.setOnClickListener{
            onBtndoukiClicked()
        }

        btnresult.setOnClickListener {
            val client1ResultsText = judgeTiming.getResultsForClient("atuo_2b77e0851dd47474")
            val client2ResultsText = judgeTiming.getResultsForClient("atuo_264ac95f5a0c0fbc")


            if (client1ResultsText == null && client2ResultsText == null) {
                Toast.makeText(requireContext(), "データがありません!", Toast.LENGTH_SHORT).apply {
                    setGravity(Gravity.CENTER, 0, 0)
                    show()
                }
                mediaPlayer = MediaPlayer.create(requireContext(), R.raw.result).apply { start() }
            } else {
                mediaPlayer = MediaPlayer.create(requireContext(), R.raw.levelup).apply { start() }
                val resultsMessage = buildString {
                    client1ResultsText?.let { append(it).append("\n\n") }
                    client2ResultsText?.let { append(it) }
                }
                AlertDialog.Builder(requireContext())
                    .setTitle("リズムゲーム結果(貢献度)")
                    .setMessage(resultsMessage)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

        btnresultdelete.setOnClickListener{
            if(client1Results == null && client2Results == null){
                val toast = Toast.makeText(requireContext(), "データがありません!", Toast.LENGTH_SHORT)
                toast.setGravity(Gravity.CENTER, 0, 0)  // 中央に表示する
                toast.show()
            }else{
                // データが存在する場合
                AlertDialog.Builder(requireContext())
                    .setTitle("データ削除の確認")
                    .setMessage("データを削除しますか？")
                    .setPositiveButton("削除") { _, _ ->
                        // JudgeTiming のデータを削除
                        judgeTiming.clearResultsForClient("atuo_2b77e0851dd47474") // クライアント1のデータを削除
                        judgeTiming.clearResultsForClient("atuo_264ac95f5a0c0fbc") // クライアント2のデータを削除

                        // 確認メッセージを表示
                        val toast = Toast.makeText(requireContext(), "データを削除しました!", Toast.LENGTH_SHORT)
                        toast.setGravity(Gravity.CENTER, 0, 0) // 中央に表示する
                        toast.show()
                        Log.d("MainFragment", "データを削除しました")
                    }
                    .setNegativeButton("キャンセル", null)
                    .show()
            }
        }

    }

    companion object {
        fun newInstance(nearBy: NearBy,judgeTiming: JudgeTiming): MainFragment {
            return MainFragment(nearBy,judgeTiming)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // MediaPlayerをリリース
        mediaPlayer?.release()
        mediaPlayer = null
    }

}
