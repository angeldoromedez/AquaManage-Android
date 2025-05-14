package com.aquamanagers.aquamanage_app.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aquamanagers.aquamanage_app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HistoryAdapter(
    private val deviceList: List<Pair<String, String>>,
    private val onDeviceClick: (String) -> Unit
): RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val deviceName: TextView = itemView.findViewById(R.id.deviceTitleHolder)
        val timeStamp: TextView = itemView.findViewById(R.id.deviceHistoryTimestamp)
        var currentListener: ValueEventListener? = null
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
        holder.deviceName.text = deviceName
        holder.timeStamp.text = "Loading..."

        holder.currentListener?.let{
            FirebaseDatabase.getInstance().reference.removeEventListener(it)
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid?: return
        val historyRef = FirebaseDatabase.getInstance().getReference("history").child(userId).child(deviceId)

        val valueListener = object:ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot){
                var latestTimeStamp:Long = 0

                snapshot.children.forEach{ historyEntry ->
                    val timeStamp = historyEntry.child("timeStamp").getValue(Long::class.java) ?: 0L
                    if(timeStamp > latestTimeStamp){
                        latestTimeStamp = timeStamp
                    }

                    if(latestTimeStamp > 0 )
                        holder.timeStamp.text = formatTimeStamp(latestTimeStamp)
                    else
                        holder.timeStamp.text = "No history found"
                }
            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        }

        historyRef.addValueEventListener(valueListener)
        holder.currentListener = valueListener

        holder.itemView.setOnClickListener{
            onDeviceClick(deviceId)
        }
    }

    private fun formatTimeStamp(timeStamp: Long):String{
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timeStamp))
    }

    override fun getItemCount(): Int = deviceList.size
}