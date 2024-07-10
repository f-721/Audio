package com.example.audio.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.audio.NearBy
import com.example.audio.R
import com.example.auido.ui.main.MainViewModel

class MainFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var tvjudge: TextView
    private lateinit var btnadvertise1: Button
    private lateinit var btnadvertise2: Button
    private lateinit var btndiscovery1: Button
    private lateinit var btndiscovery2: Button

    private lateinit var nearBy: NearBy

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.activity_main, container, false)

        btnadvertise1 = rootView.findViewById(R.id.btnadvertise)
        btnadvertise2 = rootView.findViewById(R.id.btnadvertise2)
        btndiscovery1 = rootView.findViewById(R.id.btndiscovery)
        btndiscovery2 = rootView.findViewById(R.id.btndiscovery2)
        tvjudge = rootView.findViewById(R.id.tvgreat)

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize NearBy
        nearBy = NearBy(requireContext())

        btnadvertise1.setOnClickListener {
            nearBy.advertise()
        }

        btnadvertise2.setOnClickListener {
            nearBy.discovery()
        }

        btndiscovery1.setOnClickListener {
            nearBy.discovery()
        }

        btndiscovery2.setOnClickListener {
            nearBy.discovery()
        }
    }
}
