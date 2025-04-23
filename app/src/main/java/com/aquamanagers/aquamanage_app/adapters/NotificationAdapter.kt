package com.aquamanagers.aquamanage_app.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aquamanagers.aquamanage_app.R
import com.aquamanagers.aquamanage_app.models.NotificationItem

class NotificationAdapter(
    private val items: MutableList<NotificationItem>
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.notificationImage)
        val messageView: TextView = view.findViewById(R.id.notificationMessage)
        val deviceNameView: TextView = view.findViewById(R.id.deviceName)
        val container: RelativeLayout = view.findViewById(R.id.notificationCard) // optional if you want to apply background color dynamically
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.imageView.setImageResource(item.notificationImage)
        holder.messageView.text = item.notificationName
        holder.deviceNameView.text = item.deviceName

        try {
            holder.view.setBackgroundColor(Color.parseColor(item.colorHex))
        } catch (e: IllegalArgumentException) {
            holder.view.setBackgroundColor(Color.parseColor("#584ea8e1"))
        }
    }

    fun addNotification(notification: NotificationItem){
        items.add(0,notification)
        notifyItemInserted(0)
    }

    override fun getItemCount(): Int = items.size
}
