@file:Suppress("DEPRECATION")

package com.aquamanagers.aquamanage_app

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.aquamanagers.aquamanage_app.databinding.ActivityWaterAnalysisBinding
import com.aquamanagers.aquamanage_app.models.DeviceItem
import com.aquamanagers.aquamanage_app.models.HistoryItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WaterAnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWaterAnalysisBinding
    private lateinit var deviceRef: DatabaseReference
    private lateinit var registryRef: DatabaseReference
    private lateinit var historyRef: DatabaseReference
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private var userId: String? = null
    private var deviceId: String? = null
    private var progressTimer: CountDownTimer? = null

    private val realtimeListeners = mutableListOf<ValueEventListener>()
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

        progressBar = binding.progressBar
        progressText = binding.progressText
        progressBar.visibility = View.GONE
        progressText.visibility = View.GONE

        binding.backButton.setOnClickListener{
            finish()
        }

        binding.infoIcon.setOnClickListener{
            showInformation()
        }
    }

    private fun initializeFirebase() {
        userId = FirebaseAuth.getInstance().currentUser?.uid
    }

    private fun showInformation() {
        val dialog = AlertDialog.Builder(this).create()
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_information, null)

        val btnDone:Button = dialogView.findViewById(R.id.btnDone)

        btnDone.setOnClickListener{
            dialog.dismiss()
        }

        dialog.setView(dialogView)
        dialog.setCancelable(true)
        dialog.show()
    }

    private fun setupDeviceFromIntent() {
        val deviceItem = intent.getParcelableExtra<DeviceItem>("deviceItem")
        if (deviceItem != null) {
            deviceId = deviceItem.id
        }

        if (deviceId == null) {
            showToast("Invalid device")
            finish()
            return
        } else {

            deviceRef = FirebaseDatabase.getInstance().getReference("esp32").child(deviceId!!)
            registryRef = FirebaseDatabase.getInstance().getReference("registry").child(userId!!)
                .child(deviceId!!)
            historyRef = FirebaseDatabase.getInstance().getReference("history").child(userId!!)
                .child(deviceId!!)

            deviceItem?.let { updateDeviceInfo(it) }
            fetchDeviceName()
            setupRealtimeUpdates()
        }
    }

    private fun setupHistoryData(userId: String, deviceId: String, status: String) {

        generateIncrementingId { customHistoryId ->
            val timeStart = System.currentTimeMillis()
            val currentDate = Date()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formattedDate = dateFormat.format(currentDate)

            deviceRef = FirebaseDatabase.getInstance().getReference("esp32").child(deviceId)
            deviceRef.get().addOnSuccessListener { snapshot ->
                val phValue = snapshot.child("ph").getValue(Double::class.java) ?: 0.0
                val tdsValue = snapshot.child("tds").getValue(Double::class.java) ?: 0.0
                val turbidityValue = snapshot.child("turbidity").getValue(Double::class.java) ?: 0.0

                val history = HistoryItem(
                    phValue = phValue,
                    tdsValue = tdsValue,
                    turbidityValue = turbidityValue,
                    timeStamp = timeStart,
                    date = formattedDate,
                    status = status
                )

                historyRef = FirebaseDatabase.getInstance().getReference("history")
                    .child(userId).child(deviceId).child(customHistoryId)
                historyRef.setValue(history).addOnSuccessListener {
                    //
                }.addOnFailureListener{ e->
                    Toast.makeText(this, "Failed to add history data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun generateIncrementingId(callback: (String) -> Unit) {
        historyRef = FirebaseDatabase.getInstance().getReference("history").child(userId!!).child(deviceId!!)
        historyRef.limitToLast(1).get().addOnSuccessListener { snapshot ->
            val lastId = snapshot.children.lastOrNull()?.key?.toIntOrNull() ?: 0
            val nextId = (lastId+ 1).toString().padStart(8, '0')
            callback(nextId)
        }.addOnFailureListener {
            callback("00000001")
        }
    }

    private fun setupButtons() {
        binding.btnStart.setOnClickListener { startWaterAnalysis() }
        binding.btnStop.setOnClickListener { stopWaterAnalysis() }
        binding.checkWaterQuality.setOnClickListener { showWaterQualityDialog() }
    }

    private fun setupRealtimeUpdates() {
        clearRealtimeListeners()

        val phListener = createRealtimeListener(binding.phValue)
        val tdsListener = createRealtimeListener(binding.tdsValue)
        val turbidityListener = createRealtimeListener(binding.turbidityValue)

        deviceRef.child("ph").addValueEventListener(phListener)
        deviceRef.child("tds").addValueEventListener(tdsListener)
        deviceRef.child("turbidity").addValueEventListener(turbidityListener)

        realtimeListeners.apply {
            add(phListener)
            add(tdsListener)
            add(turbidityListener)
        }
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
        progressText.visibility = View.VISIBLE
        deviceRef.child("controls").setValue(1).addOnSuccessListener {
            showToast("Analysis started")
            setupAnalysisMonitoring()
            setupHistoryData(userId!!, deviceId!!, "Treatment started")
            registryRef.child("progress").setValue(0)
            startProgressTimer()
        }.addOnFailureListener {
            showToast("Failed to start analysis: ${it.message}")
            progressBar.visibility = View.GONE
            progressText.visibility = View.GONE
        }
    }

    private fun startProgressTimer() {
        val totalDuration = 20 * 60 * 1000L
        progressBar.max = 100
        progressTimer = object: CountDownTimer(totalDuration, 1000){
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long){
                val progress = ((totalDuration - millisUntilFinished) * 100/totalDuration).toInt()
                updateProgress(progress)

                FirebaseDatabase.getInstance()
                    .getReference("registry")
                    .child(FirebaseAuth.getInstance().currentUser?.uid?:return)
                    .child(deviceId!!)
                    .child("progress")
                    .setValue(progress)
            }

            @SuppressLint("SetTextI18n")
            override fun onFinish() {
                updateProgress(100)
                FirebaseDatabase.getInstance()
                    .getReference("registry")
                    .child(FirebaseAuth.getInstance().currentUser?.uid?:return)
                    .child(deviceId!!)
                    .child("progress")
                    .setValue(100)
            }
        }
        progressTimer?.start()
    }

    private fun stopProgressTimer(){
        progressTimer?.cancel()
        progressTimer = null

        FirebaseDatabase.getInstance()
            .getReference("registry")
            .child(FirebaseAuth.getInstance().currentUser?.uid?:return)
            .child(deviceId!!)
            .child("progress")
            .setValue(0)
    }

    @SuppressLint("SetTextI18n")
    private fun updateProgress(progress: Int) {
        progressBar.setProgress(progress, true)
        progressText.text = "$progress%"

        if(progress>=100){
            progressBar.visibility = View.GONE
            progressText.visibility = View.GONE
        } else {
            progressBar.visibility = View.VISIBLE
            progressText.visibility = View.VISIBLE
        }

        FirebaseDatabase.getInstance()
            .getReference("registry")
            .child(FirebaseAuth.getInstance().currentUser?.uid ?: return)
            .child(deviceId!!)
            .child("progress")
            .get().addOnSuccessListener { snapshot ->
                val progressValue = snapshot.getValue(Int::class.java) ?: 0
                updateRecyclerViewProgress(progressValue)
            }
    }

    private fun updateRecyclerViewProgress(progress: Int) {
        registryRef.child("progress").setValue(progress)
    }

    private fun setupAnalysisMonitoring() {
        deviceRef.child("ph").get().addOnSuccessListener {
            initialPhValue = it.getValue(Double::class.java) ?: 0.0
        }
        deviceRef.child("tds").get().addOnSuccessListener {
            initialTdsValue = it.getValue(Double::class.java) ?: 0.0
        }

        deviceRef.child("turbidity").get().addOnSuccessListener {
            initialTurbidityValue = it.getValue(Double::class.java) ?: 0.0
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
        progressText.visibility = View.GONE
        deviceRef.child("controls").setValue(0).addOnSuccessListener {
            NotificationsActivity.sendCompleteNotification(this, userId!!, deviceId!!)
            showWaterAnalysisDialog(R.drawable.treatmentsucess, "TREATMENT SUCCESS")
            setupHistoryData(userId!!, deviceId!!, "Treatment completed")
        }
        stopProgressTimer()
    }

    private fun stopWaterAnalysis() {
        isChecking = false
        progressBar.visibility = View.GONE
        progressText.visibility = View.GONE
        deviceRef.child("controls").setValue(0).addOnSuccessListener {
            NotificationsActivity.sendStopNotification(this, userId!!, deviceId!!)
            showWaterAnalysisDialog(R.drawable.treatmentstop, "TREATMENT STOPPED")
            setupHistoryData(userId!!, deviceId!!, "Treatment stopped")
        }
        stopProgressTimer()
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
    private fun showWaterAnalysisDialog(doneTreatment: Int, analysis: String) {
        val dialog = AlertDialog.Builder(this).create()
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_treatment_complete, null)

        val viewFlipper: ViewFlipper = dialogView.findViewById(R.id.dialogViewFlipper)
        val btnShowAnalysis: Button = dialogView.findViewById(R.id.btnShowAnalysis)
        val treatmentDoneImage: ImageView = dialogView.findViewById(R.id.treatmentSuccessImage)
        val analysisText: TextView = dialogView.findViewById(R.id.treatmentSuccessText)

        treatmentDoneImage.setImageResource(doneTreatment)
        analysisText.text = analysis

        btnShowAnalysis.setOnClickListener {
            viewFlipper.showNext()
        }

        deviceRef = FirebaseDatabase.getInstance().getReference("esp32").child(deviceId!!)
        deviceRef.get().addOnSuccessListener { snapshot ->
            val ph = snapshot.child("ph").getValue(Double::class.java) ?: 0.0
            val tds = snapshot.child("tds").getValue(Double::class.java) ?: 0.0
            val turbidity = snapshot.child("turbidity").getValue(Double::class.java) ?: 0.0

            val phTextView: TextView = dialogView.findViewById(R.id.phValueAnalysis)
            val tdsTextView: TextView = dialogView.findViewById(R.id.tdsValueAnalysis)
            val turbidityTextView: TextView = dialogView.findViewById(R.id.turbidityValueAnalysis)
            val usagesTextView: TextView = dialogView.findViewById(R.id.usagesText)
            val usages: String

            if(ph in 7.0..10.0 && turbidity<5 && tds<1500) {
                usages = getString(R.string.useful_water_uses)
                showRandomizedImage(dialogView)
            } else{
                usages = getString(R.string.harmful_water)
                showWarningImage(dialogView)
            }

            usagesTextView.text = usages
            phTextView.text = String.format("%.2f", ph)
            tdsTextView.text = String.format("%.2f", tds)
            turbidityTextView.text = String.format("%.2f", turbidity)
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to get water quality: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }

        dialog.setView(dialogView)
        dialog.setCancelable(true)
        dialog.show()
    }

    private fun showRandomizedImage(dialogView: View){
        val imageView:ImageView = dialogView.findViewById(R.id.uses_holder)
        val images = arrayOf(
            R.drawable.flushing,
            R.drawable.laundry,
            R.drawable.carwash,
            R.drawable.mopping,
            R.drawable.watering
        )

        val randomImage = images.random()
        imageView.setImageResource(randomImage)
    }

    private fun showWarningImage(dialogView: View){
        val imageView:ImageView = dialogView.findViewById(R.id.uses_holder)
        imageView.setImageResource(R.drawable.maintenancealert)
    }

    @SuppressLint("DefaultLocale")
    private fun showWaterQualityDialog() {
        deviceRef = FirebaseDatabase.getInstance().getReference("esp32").child(deviceId!!)
        deviceRef.get().addOnSuccessListener { snapshot ->
            val ph = snapshot.child("ph").getValue(Double::class.java) ?: 0.0
            val tds = snapshot.child("tds").getValue(Double::class.java) ?: 0.0
            val turbidity = snapshot.child("turbidity").getValue(Double::class.java) ?: 0.0

            val dialog = AlertDialog.Builder(this).create()
            val inflater = LayoutInflater.from(this)
            val dialogView = inflater.inflate(R.layout.dialog_water_quality, null)

            val phTextView: TextView = dialogView.findViewById(R.id.phValueAnalysis)
            val tdsTextView: TextView = dialogView.findViewById(R.id.tdsValueAnalysis)
            val turbidityTextView: TextView = dialogView.findViewById(R.id.turbidityValueAnalysis)
            val okButton: Button = dialogView.findViewById(R.id.okButton)

            dialog.setView(dialogView)
            dialog.setCancelable(true)
            dialog.show()

            okButton.setOnClickListener {
                dialog.dismiss()
            }

            phTextView.text = String.format("%.2f", ph)
            tdsTextView.text = String.format("%.2f", tds)
            turbidityTextView.text = String.format("%.2f", turbidity)


        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to get water quality: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun clearRealtimeListeners() {
        if (::deviceRef.isInitialized) {
            for (listener in realtimeListeners) {
                deviceRef.removeEventListener(listener)
            }
            realtimeListeners.clear()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearRealtimeListeners()
    }
}
