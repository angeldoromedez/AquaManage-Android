package com.aquamanagers.aquamanage_app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquamanagers.aquamanage_app.adapters.UsesAdapter
import com.aquamanagers.aquamanage_app.databinding.ActivityWaterAnalysisBinding
import com.aquamanagers.aquamanage_app.models.DeviceData
import com.aquamanagers.aquamanage_app.models.DeviceItem
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@Suppress("DEPRECATION")
class WaterAnalysisActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivityWaterAnalysisBinding

    @SuppressLint("MissingInflatedId")
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

        val recyclerView = findViewById<RecyclerView>(R.id.uses_holder)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val images = listOf(R.drawable.housekeeping, R.drawable.laundry, R.drawable.wateringplants)
        recyclerView.adapter = UsesAdapter(images)

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
            val deviceRef = FirebaseDatabase.getInstance().getReference("esp32").child(deviceId)
            FirebaseDatabase.getInstance().getReference("esp32").child(deviceId).child("controls")
                .setValue(1)
                .addOnSuccessListener{
                    deviceRef.addValueEventListener(object: ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val ph = snapshot.child("ph").getValue(Double::class.java)
                            val turbidity = snapshot.child("turbidity").getValue(Double::class.java)
                            val tds = snapshot.child("tds").getValue(Double::class.java)

                            if ((ph ?: 0.0) > 0 || (turbidity ?: 0.0) > 0 || (tds ?: 0.0) > 0) {
                                NotificationsActivity.sendCompleteNotification(this@WaterAnalysisActivity, userId, deviceId)
                                deviceRef.removeEventListener(this)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            TODO("Not yet implemented")
                        }

                    })
                }
        }

        binding.btnStop.setOnClickListener {
            FirebaseDatabase.getInstance().getReference("esp32").child(deviceId).child("controls")
                .setValue(0)
                .addOnSuccessListener {
                    NotificationsActivity.sendStopNotification(this,userId, deviceId)
                }
        }

        binding.checkWaterQuality.setOnClickListener{
            FirebaseDatabase.getInstance().getReference("esp32").child(deviceId).get().addOnSuccessListener { snapshot->
                val ph = snapshot.child("ph").getValue(Double::class.java)
                val tds = snapshot.child("tds").getValue(Double::class.java)
                val turbidity = snapshot.child("turbidity").getValue(Double::class.java)

                val dialogView = layoutInflater.inflate(R.layout.dialog_water_analysis, null)
                val dialog = AlertDialog.Builder(this).setView(dialogView).create()

                val phTextView: TextView = dialogView.findViewById(R.id.phValueAnalysis)
                val tdsTextView: TextView = dialogView.findViewById(R.id.tdsValueAnalysis)
                val turbidityTextView: TextView = dialogView.findViewById(R.id.turbidityValueAnalysis)
                val okButton: Button = dialogView.findViewById(R.id.okButton)

                phTextView.text = "$ph"
                tdsTextView.text = "$tds"
                turbidityTextView.text = "$turbidity"

                okButton.setOnClickListener {
                    dialog.dismiss()
                }

                dialog.show()
            }
        }
    }
}