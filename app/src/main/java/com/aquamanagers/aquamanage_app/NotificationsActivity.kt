package com.aquamanagers.aquamanage_app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aquamanagers.aquamanage_app.adapters.NotificationAdapter
import com.aquamanagers.aquamanage_app.databinding.ActivityNotificationsBinding
import com.aquamanagers.aquamanage_app.models.NotificationItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.Random
import java.util.UUID
import android.content.Context


class NotificationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var adapter: NotificationAdapter
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId = FirebaseAuth.getInstance().currentUser?.uid?:return

        binding.notificationContainer.layoutManager = LinearLayoutManager(this)

        val sampleNotifications = mutableListOf<NotificationItem>()
        adapter = NotificationAdapter(sampleNotifications)
        binding.notificationContainer.adapter = adapter

        listenForNotifications(userId)
    }

    private fun listenForNotifications(userId: String) {
        val notificationRef = FirebaseDatabase
            .getInstance()
            .getReference("notifications")
            .child(userId)

        notificationRef.addChildEventListener(object: ChildEventListener {
            override fun onChildAdded(snapshot:DataSnapshot, previousChildName: String?){
                val notification = snapshot.getValue(NotificationItem::class.java)
                if(notification!=null){
                    adapter.addNotification(notification)
                }
            }

            override fun onChildChanged(snapshot:DataSnapshot, previousChildName: String?){}
            override fun onChildRemoved(snapshot:DataSnapshot){}
            override fun onChildMoved(snapshot:DataSnapshot, previousChildName: String?){}
            override fun onCancelled(error: DatabaseError){}
        })
    }

    private fun generateCustomNotificationId(): String {
        val allowedChars = "abcdefghijklmnopqrstuvwxyz"
        val random = Random()
        val length = 10
        return (1..length)
            .map { allowedChars[random.nextInt(allowedChars.length)] }
            .joinToString("")
    }

    companion object {
        fun sendStopNotification(context: Context, userId: String, deviceId: String) {
            val customNotificationId = UUID.randomUUID().toString()
            val notificationRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(userId)
                .child(customNotificationId)

            val deviceRef = FirebaseDatabase
                .getInstance()
                .getReference("registry")
                .child(userId).child(deviceId)
                .child("deviceName")

            deviceRef.get().addOnSuccessListener { deviceSnapshot ->
                val deviceName = deviceSnapshot.getValue(String::class.java) ?: "Unknown device"

                val stopNotification = NotificationItem(
                    id = customNotificationId,
                    deviceId = deviceId,
                    notificationImage = R.drawable.treatmenterror,
                    notificationName = "Treatment Stopped",
                    deviceName = deviceName,
                    colorHex = R.color.notification_blue.toString()
                )

                notificationRef.setValue(stopNotification)
                Toast.makeText(context,"Notification sent",Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Failed to notify: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        fun sendCompleteNotification(context: Context, userId: String, deviceId: String) {
            val customNotificationId = UUID.randomUUID().toString()
            val notificationRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(userId)
                .child(customNotificationId)

            val deviceRef = FirebaseDatabase
                .getInstance()
                .getReference("registry")
                .child(userId).child(deviceId)
                .child("deviceName")

            deviceRef.get().addOnSuccessListener { deviceSnapshot ->
                val deviceName = deviceSnapshot.getValue(String::class.java) ?: "Unknown device"
                val color = R.color.notification_blue.toString()

                val stopNotification = NotificationItem(
                    id = customNotificationId,
                    deviceId = deviceId,
                    notificationImage = R.drawable.treatmentsuccess,
                    notificationName = "Treatment Completed",
                    deviceName = deviceName,
                    colorHex = color
                )

                notificationRef.setValue(stopNotification)
                Toast.makeText(context,"Notification sent",Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Failed to notify: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

    }
}