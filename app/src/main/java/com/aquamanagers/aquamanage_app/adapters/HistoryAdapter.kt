package com.aquamanagers.aquamanage_app.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aquamanagers.aquamanage_app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class HistoryAdapter(
    private val deviceList: List<Pair<String, String>>,
    private val onDeviceClick: (String) -> Unit
): RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val deviceName: TextView = itemView.findViewById(R.id.deviceTitleHolder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: HistoryViewHolder,
        position: Int
    ) {
        val (deviceId, deviceName) = deviceList[position]
        holder.deviceName.text = "Device: $deviceName"
        holder.itemView.setOnClickListener{
            onDeviceClick(deviceId)
        }
    }

    override fun getItemCount(): Int = deviceList.size
}