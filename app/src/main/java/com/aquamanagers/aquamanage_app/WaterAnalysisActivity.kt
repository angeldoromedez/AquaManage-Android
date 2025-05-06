package com.aquamanagers.aquamanage_app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aquamanagers.aquamanage_app.databinding.ActivityWaterAnalysisBinding
import com.aquamanagers.aquamanage_app.models.DeviceItem
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase

@Suppress("DEPRECATION")
class WaterAnalysisActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivityWaterAnalysisBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        firebaseAuth = Firebase.auth
        super.onCreate(savedInstanceState)

        binding = ActivityWaterAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId = firebaseAuth.currentUser?.uid ?: return

        val deviceItem = intent.getParcelableExtra<DeviceItem>("deviceItem")
        val deviceId = deviceItem!!.id
        val phDeviceItem = deviceItem.phValue
        val tdsDeviceItem = deviceItem.tdsValue
        val turDeviceItem = deviceItem.turbidityValue

        val deviceRegistry =
            FirebaseDatabase.getInstance().getReference("registry").child(userId).child(deviceId).child("deviceName")

        deviceRegistry.get().addOnSuccessListener { snapshot->
            val deviceName = snapshot.getValue(String::class.java)
            binding.deviceTitle.text = deviceName ?: "Device 1"
        }.addOnFailureListener{ e->
            Toast.makeText(this,"Failure to fetch name: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        binding.phValue.text = phDeviceItem
        binding.tdsValue.text = tdsDeviceItem
        binding.turbidityValue.text = turDeviceItem

        binding.btnStart.setOnClickListener {
            FirebaseDatabase.getInstance().getReference("esp32").child(deviceId).child("controls")
                .setValue(1)
        }

        binding.btnStop.setOnClickListener {
            FirebaseDatabase.getInstance().getReference("esp32").child(deviceId).child("controls")
                .setValue(0)
                .addOnSuccessListener {
                    NotificationsActivity.sendStopNotification(this,userId, deviceId)
                }
        }
    }
}