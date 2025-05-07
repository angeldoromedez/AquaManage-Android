package com.aquamanagers.aquamanage_app

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquamanagers.aquamanage_app.adapters.UsesAdapter
import com.aquamanagers.aquamanage_app.databinding.ActivityWaterAnalysisBinding
import com.aquamanagers.aquamanage_app.models.DeviceItem
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@Suppress("DEPRECATION")
class WaterAnalysisActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivityWaterAnalysisBinding
    private lateinit var deviceRef: DatabaseReference
    private var userId: String? = null
    private var deviceId: String? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWaterAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = Firebase.auth
        userId = firebaseAuth.currentUser?.uid ?: return

        val deviceItem = intent.getParcelableExtra<DeviceItem>("deviceItem")
        deviceId = deviceItem?.id ?: return

        setupDeviceInfo(deviceItem)
        setupButtons()
    }

    private fun setupDeviceInfo(deviceItem: DeviceItem) {
        val deviceRegistry = FirebaseDatabase.getInstance().getReference("registry").child(userId!!)
            .child(deviceId!!).child("deviceName")

        deviceRegistry.get().addOnSuccessListener { snapshot ->
            binding.deviceTitle.text = snapshot.getValue(String::class.java) ?: "Device 1"
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to fetch device data: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }

        binding.phValue.text = deviceItem.phValue
        binding.tdsValue.text = deviceItem.tdsValue
        binding.turbidityValue.text = deviceItem.turbidityValue
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        val images = listOf(
            R.drawable.housekeeping,
            R.drawable.wateringplants,
            R.drawable.laundry
        )
        recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = UsesAdapter(images)
    }

    private fun setupButtons() {
        binding.btnStart.setOnClickListener {
            startWaterAnalysis()
        }

        binding.btnStop.setOnClickListener {
            stopWaterAnalysis()
        }

        binding.checkWaterQuality.setOnClickListener {
            showWaterQualityDialog()
        }
    }

    private fun startWaterAnalysis() {
        deviceRef = FirebaseDatabase.getInstance().getReference("esp32").child(deviceId!!)
        deviceRef.child("controls").setValue(1).addOnSuccessListener {
            deviceRef.addValueEventListener(object:ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot){
                    val ph = snapshot.child("ph").getValue(Double::class.java)?: 0.0
                    val turbidity = snapshot.child("turbidity").getValue(Double::class.java)?: 0.0
                    val tds = snapshot.child("tds").getValue(Double::class.java)?: 0.0

                    if (ph > 0 || turbidity > 0 || tds > 0) {
                        NotificationsActivity.sendCompleteNotification(this@WaterAnalysisActivity, userId!!, deviceId!!)
                        deviceRef.removeEventListener(this)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@WaterAnalysisActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }


    private fun stopWaterAnalysis() {
        FirebaseDatabase.getInstance().getReference("esp32").child(deviceId!!).child("controls")
            .setValue(0)
            .addOnSuccessListener {
                NotificationsActivity.sendStopNotification(this, userId!!, deviceId!!)
            }
    }

    @SuppressLint("InflateParams", "SetTextI18n")
    private fun showWaterQualityDialog() {
        deviceRef = FirebaseDatabase.getInstance().getReference("esp32").child(deviceId!!)
        deviceRef.get().addOnSuccessListener { snapshot ->
            val ph = snapshot.child("ph").getValue(Double::class.java)?: 0.0
            val tds = snapshot.child("tds").getValue(Double::class.java)?: 0.0
            val turbidity = snapshot.child("turbidity").getValue(Double::class.java)?: 0.0

            val dialogView = layoutInflater.inflate(R.layout.dialog_water_analysis,null)
            val phTextView: TextView = dialogView.findViewById(R.id.phValueAnalysis)
            val tdsTextView: TextView = dialogView.findViewById(R.id.tdsValueAnalysis)
            val turbidityTextView: TextView = dialogView.findViewById(R.id.turbidityValueAnalysis)
            val okButton: Button = dialogView.findViewById(R.id.okButton)
            val recyclerView: RecyclerView = dialogView.findViewById(R.id.uses_holder)

            phTextView.text = ph.toString()
            tdsTextView.text = tds.toString()
            turbidityTextView.text = turbidity.toString()

            setupRecyclerView(recyclerView)

            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            okButton.setOnClickListener{
                dialog.dismiss()
            }
            dialog.show()
        }.addOnFailureListener{ e->
            Toast.makeText(this, "Failed to get water quality: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}