package com.aquamanagers.aquamanage_app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aquamanagers.aquamanage_app.R
import com.aquamanagers.aquamanage_app.models.DeviceItem

class DeviceCardAdapter(
    private val items: MutableList<DeviceItem>,
    private val listener:OnItemClickListener)
    : RecyclerView.Adapter<DeviceCardAdapter.ViewHolder>() {

    interface OnItemClickListener{
        fun onItemClick(item:DeviceItem)
    }

    class ViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView){
        val id: TextView = itemView.findViewById(R.id.deviceName)
        val phValue: TextView = itemView.findViewById(R.id.phValueHolder)
        val tdsValue: TextView = itemView.findViewById(R.id.tdsValueHolder)
        val turbidityValue: TextView = itemView.findViewById(R.id.turbidityValueHolder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.id.text = item.id
        holder.phValue.text = item.phValue
        holder.tdsValue.text = item.tdsValue
        holder.turbidityValue.text = item.turbidityValue
        holder.itemView.setOnClickListener{
            listener.onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun addItem(item: DeviceItem){
        items.add(item)
        notifyItemInserted(items.size-1)
    }
}