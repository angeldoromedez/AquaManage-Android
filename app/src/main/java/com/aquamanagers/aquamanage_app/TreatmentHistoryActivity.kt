package com.aquamanagers.aquamanage_app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aquamanagers.aquamanage_app.adapters.HistoryAdapter
import com.aquamanagers.aquamanage_app.databinding.ActivityTreatmentHistoryBinding
import com.aquamanagers.aquamanage_app.models.HistoryItem
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
        historyRef = FirebaseDatabase.getInstance().getReference("history").child(userId)

        historyRef.get().addOnSuccessListener { snapshot ->
            val deviceList = mutableListOf<Pair<String, String>>()
            for (deviceSnapshot in snapshot.children) {
                val deviceId = deviceSnapshot.key ?: continue
                val deviceName = deviceSnapshot.child("deviceName").getValue(String::class.java)
                    ?: "Unknown Device"
                deviceList.add(Pair(deviceId, deviceName))
            }

            setupRecyclerView(deviceList)
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
            val phEntry = ArrayList<Entry>()
            val tdsEntry = ArrayList<Entry>()
            val turbidityEntry = ArrayList<Entry>()

            for (historySnapshot in snapshot.children) {
                val history = historySnapshot.getValue(HistoryItem::class.java)
                if (history != null) {
                    val timeInMillis = history.timeStamp.toFloat()
                    val phValue = history.phValue.toFloat()
                    val tdsValue = history.tdsValue.toFloat()
                    val turbidityValue = history.turbidityValue.toFloat()
                    phEntry.add(Entry(timeInMillis, phValue))
                    tdsEntry.add(Entry(timeInMillis, tdsValue))
                    turbidityEntry.add(Entry(timeInMillis, turbidityValue))
                }
            }

            setupChart(phEntry,tdsEntry, turbidityEntry)
        }
    }

    private fun setupChart(phEntries: ArrayList<Entry>, tdsEntries: ArrayList<Entry>, turbidityEntries: ArrayList<Entry>) {
        val phDataSet = LineDataSet(phEntries, "pH Level").apply{
            color = resources.getColor(R.color.red)
        }

        val tdsDataSet = LineDataSet(tdsEntries, "TDS Level").apply{
            color = resources.getColor(R.color.green)
        }

        val turbidityDataSet = LineDataSet(turbidityEntries, "Turbidity Level").apply{
            color = resources.getColor(R.color.yellow)
        }

        val lineDataSet = LineData(phDataSet, tdsDataSet, turbidityDataSet)
        binding.historyChart.apply{
            data = lineData
            description.text = "Treatment History"
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = DateValueFormatter()
            axisRight.isEnabled = false
            legend.isEnabled = true
            invalidate()
        }
    }
}