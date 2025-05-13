@file:Suppress("DEPRECATION")

package com.aquamanagers.aquamanage_app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.aquamanagers.aquamanage_app.databinding.ActivityWaterAnalysisBinding
import com.aquamanagers.aquamanage_app.models.DeviceItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.DecimalFormat

class WaterAnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWaterAnalysisBinding
    private lateinit var deviceRef: DatabaseReference
    private lateinit var registryRef: DatabaseReference
    private lateinit var progressBar: ProgressBar
    private var userId: String? = null
    private var deviceId: String? = null

    private val decimalFormat = DecimalFormat("#.##")
    private var initialPhValue = 0.0
    private var initialTdsValue = 0.0
    private var initialTurbidityValue = 0.0
    private var isChecking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWaterAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeFirebase()
        setupDeviceFromIntent()
        setupButtons()
        setupRealtimeUpdates()

        progressBar = binding.progressBar
        progressBar.visibility = View.GONE
    }

    private fun initializeFirebase() {
        userId = FirebaseAuth.getInstance().currentUser?.uid
    }

    private fun setupDeviceFromIntent() {
        val deviceItem = intent.getParcelableExtra<DeviceItem>("deviceItem")
        deviceId = intent.getStringExtra("deviceId")
        if (deviceId == null) {
            showToast("Invalid device")
            finish()
            return
        }

        deviceRef = FirebaseDatabase.getInstance().getReference("esp32").child(deviceId!!)
        registryRef = FirebaseDatabase.getInstance().getReference("registry").child(userId!!)
            .child(deviceId!!)

        deviceItem?.let { updateDeviceInfo(it) }
        fetchDeviceName()
    }

    private fun setupButtons() {
        binding.btnStart.setOnClickListener { startWaterAnalysis() }
        binding.btnStop.setOnClickListener { stopWaterAnalysis() }
        binding.checkWaterQuality.setOnClickListener { showWaterQualityDialog() }
    }

    private fun setupRealtimeUpdates() {
        clearRealtimeListeners()
        deviceRef.child("ph").addValueEventListener(createRealtimeListener(binding.phValue))
        deviceRef.child("tds").addValueEventListener(createRealtimeListener(binding.tdsValue))
        deviceRef.child("turbidity")
            .addValueEventListener(createRealtimeListener(binding.turbidityValue))
    }

    private fun createRealtimeListener(textView: TextView): ValueEventListener {
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value =
                    snapshot.getValue(Double::class.java)?.let { decimalFormat.format(it) } ?: "0.0"
                textView.text = value
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Realtime update error: ${error.message}")
            }
        }
    }

    private fun startWaterAnalysis() {
        isChecking = true
        progressBar.visibility = View.VISIBLE
        deviceRef.child("controls").setValue(1).addOnSuccessListener {
            showToast("Analysis started")
            setupAnalysisMonitoring()
        }.addOnFailureListener { showToast("Failed to start analysis: ${it.message}")
        progressBar.visibility = View.GONE}
    }

    private fun setupAnalysisMonitoring() {
        deviceRef.child("ph").get().addOnSuccessListener {
            initialPhValue = it.getValue(Double::class.java) ?: 0.0
        }
        deviceRef.child("tds").get().addOnSuccessListener {
            initialTdsValue = it.getValue(Double::class.java) ?: 0.0
        }

        deviceRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ph = snapshot.child("ph").getValue(Double::class.java) ?: 0.0
                val tds = snapshot.child("tds").getValue(Double::class.java) ?: 0.0
                val turbidity = snapshot.child("turbidity").getValue(Double::class.java) ?: 0.0

                if (ph != initialPhValue || tds != initialTdsValue || turbidity != initialTurbidityValue) {
                    completeAnalysisSuccessfully()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun completeAnalysisSuccessfully() {
        isChecking = false
        progressBar.visibility = View.GONE
        deviceRef.child("controls").setValue(0)
        NotificationsActivity.sendCompleteNotification(this, userId!!, deviceId!!)
        showWaterAnalysisDialog()
        showToast("Analysis completed successfully")
    }

    private fun stopWaterAnalysis() {
        isChecking = false
        progressBar.visibility = View.GONE
        deviceRef.child("controls").setValue(0).addOnSuccessListener {
            NotificationsActivity.sendStopNotification(this, userId!!, deviceId!!)
            showToast("Analysis stopped")
        }
    }

    private fun fetchDeviceName() {
        registryRef.child("deviceName").get().addOnSuccessListener { snapshot ->
            binding.deviceTitle.text = snapshot.getValue(String::class.java) ?: "Unknown Device"
        }
    }

    private fun updateDeviceInfo(deviceItem: DeviceItem) {
        binding.apply {
            phValue.text = deviceItem.phValue
            tdsValue.text = deviceItem.tdsValue
            turbidityValue.text = deviceItem.turbidityValue
        }
    }

    @SuppressLint("InflateParams", "SetTextI18n", "DefaultLocale")
    private fun showWaterAnalysisDialog() {
        val dialog = AlertDialog.Builder(this).create()
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_treatment_complete, null)

        val viewFlipper:ViewFlipper = dialogView.findViewById(R.id.dialogViewFlipper)
        val btnShowAnalysis: Button = dialogView.findViewById(R.id.btnShowAnalysis)

        btnShowAnalysis.setOnClickListener{
            viewFlipper.showNext()
        }

        deviceRef = FirebaseDatabase.getInstance().getReference("esp32").child(deviceId!!)
        deviceRef.get().addOnSuccessListener{ snapshot ->
            val ph = snapshot.child("ph").getValue(Double::class.java) ?: 0.0
            val tds = snapshot.child("tds").getValue(Double::class.java) ?: 0.0
            val turbidity = snapshot.child("turbidity").getValue(Double::class.java) ?: 0.0

            val phTextView:TextView = dialogView.findViewById(R.id.phValueAnalysis)
            val tdsTextView:TextView = dialogView.findViewById(R.id.tdsValueAnalysis)
            val turbidityTextView:TextView = dialogView.findViewById(R.id.turbidityValueAnalysis)

            phTextView.text = String.format("%.2f", ph)
            tdsTextView.text = String.format("%.2f", tds)
            turbidityTextView.text = String.format("%.2f", turbidity)
        }.addOnFailureListener{e ->
            Toast.makeText(this, "Failed to get water quality: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        dialog.setView(dialogView)
        dialog.setCancelable(true)
        dialog.show()
    }

    @SuppressLint("DefaultLocale")
    private fun showWaterQualityDialog() {
        deviceRef = FirebaseDatabase.getInstance().getReference("esp32").child(deviceId!!)
        deviceRef.get().addOnSuccessListener{ snapshot ->
            val ph = snapshot.child("ph").getValue(Double::class.java) ?: 0.0
            val tds = snapshot.child("tds").getValue(Double::class.java) ?: 0.0
            val turbidity = snapshot.child("turbidity").getValue(Double::class.java) ?: 0.0

            val dialog = AlertDialog.Builder(this).create()
            val inflater = LayoutInflater.from(this)
            val dialogView = inflater.inflate(R.layout.dialog_water_quality, null)

            val phTextView:TextView = dialogView.findViewById(R.id.phValueAnalysis)
            val tdsTextView:TextView = dialogView.findViewById(R.id.tdsValueAnalysis)
            val turbidityTextView:TextView = dialogView.findViewById(R.id.turbidityValueAnalysis)
            val okButton: Button = dialogView.findViewById(R.id.okButton)

            dialog.setView(dialogView)
            dialog.setCancelable(true)
            dialog.show()

            okButton.setOnClickListener{
                dialog.dismiss()
            }

            phTextView.text = String.format("%.2f", ph)
            tdsTextView.text = String.format("%.2f", tds)
            turbidityTextView.text = String.format("%.2f", turbidity)


        }.addOnFailureListener{e ->
            Toast.makeText(this, "Failed to get water quality: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearRealtimeListeners() {
        deviceRef.removeEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearRealtimeListeners()
    }
}
