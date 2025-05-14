package com.aquamanagers.aquamanage_app.adapters

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.aquamanagers.aquamanage_app.R
import com.aquamanagers.aquamanage_app.models.NotificationItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class NotificationAdapter(
    private val items: MutableList<NotificationItem>
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.notificationImage)
        val messageView: TextView = view.findViewById(R.id.notificationMessage)
        val deviceNameView: TextView = view.findViewById(R.id.deviceName)
        val menuView: LinearLayout = view.findViewById(R.id.notificationMenu)
        val timestampView: TextView = view.findViewById(R.id.notificationTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.imageView.setImageResource(item.notificationImage)
        holder.messageView.text = item.notificationName
        holder.deviceNameView.text = item.deviceName
        holder.timestampView.text = System.currentTimeMillis().toString()

        if (item.timeStamp > 0)
            holder.timestampView.text = formatTimeStamp(item.timeStamp)
        else
            holder.timestampView.text = "No timestamp"

            try {
                holder.view.setBackgroundColor(Color.parseColor(item.colorHex))
            } catch (e: IllegalArgumentException) {
                holder.view.setBackgroundColor(Color.parseColor("#584ea8e1"))
            }

        holder.menuView.setOnClickListener {
            showFloatingMenu(item, it, holder.adapterPosition)
        }

        holder.view.setOnClickListener {
            val newColor = "#D3D3D3"
            holder.view.setBackgroundColor(Color.parseColor(newColor))
            items[position].colorHex = newColor

            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(userId)
                .child(item.id)
                .child("colorHex")
                .setValue(newColor)
        }
    }

    private fun showFloatingMenu(item: NotificationItem, anchorView: View?, position: Int) {
        val popupMenu = PopupMenu(anchorView!!.context, anchorView)
        popupMenu.menuInflater.inflate(R.menu.notification_popup_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_view_details -> {
                    Toast.makeText(
                        anchorView.context,
                        "View details of $position",
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }

                R.id.menu_delete -> {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                        ?: return@setOnMenuItemClickListener true
                    FirebaseDatabase.getInstance()
                        .getReference("notifications")
                        .child(userId)
                        .child(item.id)
                        .removeValue()
                        .addOnSuccessListener {
                            if (position in items.indices) {
                                items.removeAt(position)
                                notifyItemRemoved(position)
                            }
                        }
                    true
                }

                else -> false
            }
        }
        popupMenu.show()
    }

    private fun formatTimeStamp(timeStamp: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timeStamp))
    }

    override fun getItemCount(): Int = items.size
}
