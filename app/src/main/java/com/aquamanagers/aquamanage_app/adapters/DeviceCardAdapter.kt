package com.aquamanagers.aquamanage_app.adapters

import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        fun onItemLongClick(position: Int, item:DeviceItem)
    }

    class ViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView){
        val phValue: TextView = itemView.findViewById(R.id.phValueHolder)
        val tdsValue: TextView = itemView.findViewById(R.id.tdsValueHolder)
        val turbidityValue: TextView = itemView.findViewById(R.id.turbidityValueHolder)
        val deviceName: TextView = itemView.findViewById(R.id.deviceTitleHolder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.phValue.text = item.phValue
        holder.tdsValue.text = item.tdsValue
        holder.turbidityValue.text = item.turbidityValue
        holder.deviceName.text = item.deviceName

        try{
            holder.itemView.setBackgroundColor(android.graphics.Color.parseColor(item.colorHex))
        }catch(e: IllegalArgumentException){
            holder.itemView.setBackgroundColor(android.graphics.Color.WHITE)
        }

        holder.itemView.setOnClickListener{
            listener.onItemClick(item)
        }
        holder.itemView.setOnLongClickListener{
            listener.onItemLongClick(holder.adapterPosition, item)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    fun addItem(item: DeviceItem){
        items.add(item)
        notifyItemInserted(items.size-1)
    }

    fun updateItem(position: Int, newItem:DeviceItem){
        items[position] = newItem
        notifyItemChanged(position)
    }
}