package com.aquamanagers.aquamanage_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aquamanagers.aquamanage_app.adapters.NotificationAdapter
import com.aquamanagers.aquamanage_app.databinding.ActivityNotificationsBinding
import com.aquamanagers.aquamanage_app.models.NotificationItem

class NotificationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.notificationContainer.layoutManager = LinearLayoutManager(null)

        val sampleNotifications = mutableListOf(
            NotificationItem("1",R.drawable.carmela_hi,"Treatment Complete","Device 1", "#4ea8e188")
        )
        adapter = NotificationAdapter(sampleNotifications)
        binding.notificationContainer.adapter = adapter
    }
}