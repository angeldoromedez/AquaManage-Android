package com.aquamanagers.aquamanage_app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
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
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import java.util.Random
import java.util.UUID


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

                notificationRef.setValue(stopNotification).addOnSuccessListener {
                    showLocalNotification(
                        context,
                        "Treatment Stopped",
                        "Device $deviceName has stopped treatment"
                    )
                    Toast.makeText(context, "Notification stopped", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to notify: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                    Toast.makeText(context, "Notification sent", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to notify: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        private fun showLocalNotification(context: Context, title: String, message: String) {
            val intent = Intent(context, NotificationsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val channelId = "aquamanager_local_notifications"
            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.app)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Local Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            notificationManager.notify(0, notificationBuilder.build())
        }

        fun sendCompleteNotification(context: Context, userId: String, deviceId: String) {
            val customNotificationId = UUID.randomUUID().toString()
            val notificationRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(userId)
                .child(customNotificationId)

            FirebaseMessaging.getInstance().token.addOnSuccessListener { deviceToken ->
                val message = RemoteMessage.Builder("$deviceToken@fcm.googleapis.com")
                    .setMessageId(customNotificationId)
                    .addData("title", "Treatment Stopped")
                    .addData("body", "Your device $deviceId has stopped treatment.")
                    .build()

                FirebaseMessaging.getInstance().send(message)
            }

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
                    notificationImage = R.drawable.treatmentsuccess,
                    notificationName = "Treatment Completed",
                    deviceName = deviceName,
                    colorHex = R.color.notification_blue.toString()
                )
                notificationRef.setValue(stopNotification).addOnSuccessListener {
                    showLocalNotification(
                        context,
                        "Treatment Completed",
                        "Device $deviceName has completed treatment"
                    )
                    Toast.makeText(context, "Notification stopped", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to notify: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                    Toast.makeText(context, "Notification sent", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to notify: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    }
}