package com.aquamanagers.aquamanage_app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquamanagers.aquamanage_app.adapters.DeviceCardAdapter
import com.aquamanagers.aquamanage_app.databinding.ActivityDashboardBinding
import com.aquamanagers.aquamanage_app.models.DeviceItem
import com.aquamanagers.aquamanage_app.models.DeviceRegistry
import com.aquamanagers.aquamanage_app.models.Users
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import java.util.concurrent.CompletableFuture

class DashboardActivity : AppCompatActivity(), DeviceCardAdapter.OnItemClickListener {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var devicesRef: DatabaseReference
    private lateinit var database: DatabaseReference
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DeviceCardAdapter
    private val items = mutableListOf<DeviceItem>()

    @SuppressLint("NotifyDataSetChanged")
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                showCamera()
            } else {
                //TODO
            }
        }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {

        firebaseAuth = Firebase.auth
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recyclerView = findViewById(R.id.newDevice)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = DeviceCardAdapter(items, this)
        recyclerView.adapter = adapter

        binding.addDevice.setOnClickListener {
            showAddDeviceDialog()
        }

        reloadDevices()

        binding.profileIcon.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.profileName.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun reloadDevices() {
        val currentUser = firebaseAuth.currentUser ?: return
        val userId = currentUser.uid
        devicesRef = FirebaseDatabase.getInstance().getReference("registry")

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val additionalInfo = snapshot.getValue(Users::class.java)
                    val userName = additionalInfo?.firstName.toString()
                    binding.profileName.text = userName
                } else {
                    Toast.makeText(
                        this@DashboardActivity,
                        "User data not found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        devicesRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                items.clear()

                if (!snapshot.exists() || snapshot.childrenCount.toInt() == 0) {
                    adapter.notifyDataSetChanged()
                    return
                }

                val deviceLoadPromises = mutableListOf<CompletableFuture<Unit>>()

                for (deviceSnapshot in snapshot.children) {
                    val deviceId = deviceSnapshot.key ?: continue
                    val future = CompletableFuture<Unit>()
                    deviceLoadPromises.add(future)

                    FirebaseDatabase.getInstance().getReference("esp32").child(deviceId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(espSnapshot: DataSnapshot) {
                                val ph = espSnapshot.child("ph").getValue(Double::class.java) ?: 0.0
                                val tds = espSnapshot.child("tds").getValue(Double::class.java) ?: 0.0
                                val turbidity = espSnapshot.child("turbidity").getValue(Double::class.java) ?: 0.0
                                items.add(DeviceItem(deviceId, ph.toString(), tds.toString(), turbidity.toString()))
                                future.complete(Unit)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                future.complete(Unit)
                            }
                        })
                }

                CompletableFuture.allOf(*deviceLoadPromises.toTypedArray())
                    .thenRun{
                        runOnUiThread{
                            adapter.notifyDataSetChanged()
                        }
                    }
            }

            override fun onCancelled(error: DatabaseError){
                Toast.makeText(this@DashboardActivity, "Failed to load devices", Toast.LENGTH_SHORT).show()
            }
    })

}

private fun showAddDeviceDialog() {
    val dialogView = layoutInflater.inflate(R.layout.dialog_add_device, null)
    val dialog = AlertDialog.Builder(this)
        .setView(dialogView)
        .setCancelable(true)
        .create()

    val closeButton: ImageView = dialogView.findViewById(R.id.close_button)
    val qrScannerButton: Button = dialogView.findViewById(R.id.open_qr_scanner_button)

    closeButton.setOnClickListener {
        dialog.dismiss()
    }

    qrScannerButton.setOnClickListener {
        Toast.makeText(this, "Opening camera...", Toast.LENGTH_SHORT).show()
        dialog.dismiss()
        scanQR()
    }
    dialog.show()
}

private fun scanQR() {
    checkPermissionCamera(this)
}

private fun checkPermissionCamera(context: Context) {
    if (ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        showCamera()
    } else if (shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)) {
        Toast.makeText(context, "Camera Permission Required", Toast.LENGTH_LONG).show()
    } else
        requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
}

private fun showCamera() {
    val options = ScanOptions()
    options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
    options.setPrompt("Scan QR Code")
    options.setCameraId(0)
    options.setBeepEnabled(true)
    options.setBarcodeImageEnabled(true)
    options.setOrientationLocked(false)

    scanLauncher.launch(options)

}

private val scanLauncher =
    registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents == null) {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            setResult(RESULT_CANCELED)
            finish()
        } else {
            checkResult(result.contents)
        }
    }

private fun checkResult(deviceId: String) {
    val userId = firebaseAuth.currentUser?.uid ?: return
    addDeviceRegistry(userId, deviceId)

    database = FirebaseDatabase.getInstance().getReference("esp32")
    database.child(deviceId).get().addOnSuccessListener { snapshot ->
        if (snapshot.exists()) {
            fetchDeviceData(deviceId)
        } else {
            Toast.makeText(this, "Device QR unrecognized.", Toast.LENGTH_SHORT).show()
        }
    }.addOnFailureListener { e ->
        Toast.makeText(this, e.message ?: "An error occurred", Toast.LENGTH_SHORT).show()
    }

}

private fun fetchDeviceData(deviceId: String) {
    val deviceReading = FirebaseDatabase.getInstance().getReference("esp32").child(deviceId)
    deviceReading.get().addOnSuccessListener { snapshot ->
        val phValue = snapshot.child("ph").getValue(String::class.java) ?: "N/A"
        val tdsValue = snapshot.child("tds").getValue(String::class.java) ?: "N/A"
        val turbidityValue = snapshot.child("turbidity").getValue(String::class.java) ?: "N/A"

        val newItem = DeviceItem(deviceId, phValue, tdsValue, turbidityValue)
        adapter.addItem(newItem)
        Toast.makeText(this, "Device added successfully", Toast.LENGTH_SHORT).show()
    }.addOnFailureListener { e ->
        Toast.makeText(this, "Error fetching device data: ${e.message}", Toast.LENGTH_SHORT)
            .show()
    }
}

private fun addDeviceRegistry(userId: String, deviceId: String) {
    DeviceRegistry(true)
    FirebaseDatabase.getInstance().getReference("registry")
    val deviceIndexRef = FirebaseDatabase.getInstance().getReference("deviceIndex")

    deviceIndexRef.child(deviceId).get().addOnSuccessListener { snapshot ->
        when(snapshot.getValue(String::class.java)) {
            null -> {
                val updates = hashMapOf<String,Any>(
                    "registry/$userId/$deviceId/connected" to true,
                    "deviceIndex/$deviceId" to userId
                )

                FirebaseDatabase.getInstance().reference.updateChildren(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Device added successfully", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener{ e->
                        Toast.makeText(this, "Failed to add device: ${e.message}",Toast.LENGTH_SHORT).show()
                    }
            } userId -> {
                Toast.makeText(this, "Device already registered to your account", Toast.LENGTH_SHORT).show()
            } else -> {
                Toast.makeText(this, "Device already linked to another account", Toast.LENGTH_SHORT).show()
            }
        }
    }.addOnFailureListener{
        Toast.makeText(this, "Failed to check device registration", Toast.LENGTH_SHORT).show()
    }
}

override fun onItemClick(item: DeviceItem) {
    val intent = Intent(this, WaterAnalysisActivity::class.java)
    intent.putExtra("deviceItem", item)
    startActivity(intent)
}
}