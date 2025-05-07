package com.aquamanagers.aquamanage_app.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.aquamanagers.aquamanage_app.NotificationsActivity
import com.aquamanagers.aquamanage_app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d("FCM", "Refreshed token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let { notification ->
            sendNotification(notification.title ?: "New Notification",
                notification.body ?: "",
                remoteMessage.data
            )
        }

        if(remoteMessage.data.isNotEmpty()){
            sendNotification(
                remoteMessage.data["title"]?: "New Notification",
                remoteMessage.data["message"]?: "",
                remoteMessage.data
            )
        }
    }

    fun sendNotification(title: String,  message: String, data:Map<String, String>) {
        val intent = Intent(this, NotificationsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notificationData", data.toString())
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val channelId = "aquamanager_notifications"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.app)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                "AquaManager Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "AquaManager treatment notifications"
                enableVibration(true)
                vibrationPattern = longArrayOf(100,200,300,400,500)
            }
            notificationManager.createNotificationChannel(notificationChannel)
        }

        notificationManager.notify(Random.nextInt(), builder.build())
    }
}