@file:Suppress("DEPRECATION")

package com.aquamanagers.aquamanage_app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aquamanagers.aquamanage_app.adapters.HistoryAdapter
import com.aquamanagers.aquamanage_app.databinding.ActivityTreatmentHistoryBinding
import com.aquamanagers.aquamanage_app.services.DateValueFormatter
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class TreatmentHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTreatmentHistoryBinding
    private lateinit var historyRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTreatmentHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadDeviceList()
    }

    private fun loadDeviceList() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val registryRef = FirebaseDatabase.getInstance().getReference("registry").child(userId)

        registryRef.get().addOnSuccessListener { snapshot ->
            val deviceList = mutableListOf<Pair<String, String>>()
            snapshot.children.forEach { deviceSnapshot ->
                val deviceId = deviceSnapshot.key ?: return@forEach
                val deviceName =
                    deviceSnapshot.child("deviceName").getValue(String::class.java)
                        ?: "Unknown Device"
                deviceList.add(Pair(deviceId, deviceName))
            }
            if (deviceList.isEmpty()) {
                binding.historyRecycler.visibility = View.GONE
            } else {
                setupRecyclerView(deviceList)
            }
        }
    }

    private fun setupRecyclerView(deviceList: MutableList<Pair<String, String>>) {
        val adapter = HistoryAdapter(deviceList) { selectedDeviceId ->
            loadDeviceHistory(selectedDeviceId)
        }
        binding.historyRecycler.layoutManager = LinearLayoutManager(this)
        binding.historyRecycler.adapter = adapter
    }

    private fun loadDeviceHistory(deviceId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        historyRef = FirebaseDatabase.getInstance().getReference("history")
            .child(userId)
            .child(deviceId)

        historyRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                binding.historyChart.visibility = View.GONE
                return@addOnSuccessListener
            }

            val phEntry = ArrayList<Entry>()
            val tdsEntry = ArrayList<Entry>()
            val turbidityEntry = ArrayList<Entry>()

            snapshot.children.forEach { historySnapshot ->
                val timeInMillis =
                    historySnapshot.child("timeStamp").getValue(Long::class.java) ?: return@forEach
                val phValue = historySnapshot.child("phValue").getValue(Double::class.java) ?: 0.0
                val tdsValue = historySnapshot.child("tdsValue").getValue(Double::class.java) ?: 0.0
                val turbidityValue =
                    historySnapshot.child("turbidityValue").getValue(Double::class.java) ?: 0.0

                phEntry.add(Entry(timeInMillis.toFloat(), phValue.toFloat()))
                tdsEntry.add(Entry(timeInMillis.toFloat(), tdsValue.toFloat()))
                turbidityEntry.add(Entry(timeInMillis.toFloat(), turbidityValue.toFloat()))
            }

            if (phEntry.isNotEmpty())
                setupChart(phEntry, tdsEntry, turbidityEntry)
            else
                binding.historyChart.visibility = View.GONE
        }
    }

    private fun setupChart(
        phEntries: ArrayList<Entry>,
        tdsEntries: ArrayList<Entry>,
        turbidityEntries: ArrayList<Entry>
    ) {
        binding.historyChart.apply {
            clear()
            val phDataSet = LineDataSet(phEntries, "pH Level").apply {
                color = resources.getColor(R.color.red)
                lineWidth = 2f
                setDrawCircles(true)
                setCircleColor(color)
            }

            val tdsDataSet = LineDataSet(tdsEntries, "TDS Level").apply {
                color = resources.getColor(R.color.green)
                lineWidth = 2f
                setDrawCircles(true)
                setCircleColor(color)
            }

            val turbidityDataSet = LineDataSet(turbidityEntries, "Turbidity Level").apply {
                color = resources.getColor(R.color.yellow)
                lineWidth = 2f
                setDrawCircles(true)
                setCircleColor(color)
            }

            data = LineData(phDataSet, tdsDataSet, turbidityDataSet)
            description.text = "Water Quality History"
            description.textSize = 12f

            xAxis.apply{
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = DateValueFormatter()
                granularity = 1f
                labelCount = 5
                setDrawGridLines(false)
            }

            axisLeft.apply{
                setDrawGridLines(true)
                axisMinimum = 0f
                granularity = 1f
            }

            axisRight.isEnabled = false
            legend.isEnabled = true
            setTouchEnabled(true)
            setPinchZoom(true)

            animateX(1000)
            invalidate()
        }
    }
}