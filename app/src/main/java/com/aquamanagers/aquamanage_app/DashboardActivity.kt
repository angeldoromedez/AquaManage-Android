package com.aquamanagers.aquamanage_app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
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
import com.aquamanagers.aquamanage_app.models.AdminNotification
import com.aquamanagers.aquamanage_app.models.DeviceItem
import com.aquamanagers.aquamanage_app.models.DeviceRegistry
import com.aquamanagers.aquamanage_app.models.Users
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.concurrent.CompletableFuture


class DashboardActivity : AppCompatActivity(), DeviceCardAdapter.OnItemClickListener {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var registryRef: DatabaseReference
    private lateinit var database: DatabaseReference
    private lateinit var adminDatabase: DatabaseReference
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DeviceCardAdapter
    private val items = mutableListOf<DeviceItem>()

    @SuppressLint("NotifyDataSetChanged")
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) showCamera()
            else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
        }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {

        firebaseAuth = Firebase.auth
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val savedAvatarResId = prefs.getInt("selectedAvatar", -1)
        if (savedAvatarResId != -1)
            binding.profileIcon.setImageResource(savedAvatarResId)

        recyclerView = findViewById(R.id.newDevice)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = DeviceCardAdapter(items, this)
        recyclerView.adapter = adapter

        binding.addDevice.setOnClickListener {
            showAddDeviceDialog()
        }

        reloadDevices()

        binding.bellIcon.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            startActivity(intent)
        }

        binding.profileIcon.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.profileName.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.historyIcon.setOnClickListener{
            val intent = Intent(this, TreatmentHistoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun reloadDevices() {
        val currentUser = firebaseAuth.currentUser ?: return
        val userId = currentUser.uid
        registryRef = FirebaseDatabase.getInstance().getReference("registry")
        database = FirebaseDatabase.getInstance().getReference("Users").child(userId)

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val additionalInfo = snapshot.getValue(Users::class.java)
                    val userName = additionalInfo?.firstName.toString()
                    binding.profileName.text = userName
                } else {
                    Toast.makeText(
                        this@DashboardActivity, "User data not found", Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        registryRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
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

                    val deviceMetaRef = registryRef.child(userId).child(deviceId)
                    val deviceReadingRef =
                        FirebaseDatabase.getInstance().getReference("esp32").child(deviceId)

                    deviceMetaRef.get().addOnSuccessListener { metaSnapshot ->
                        val deviceName =
                            metaSnapshot.child("deviceName").getValue(String::class.java)
                                ?: "Device 1"
                        val colorHex =
                            metaSnapshot.child("deviceColor").getValue(String::class.java)
                                ?: "#FFFFFF"

                        deviceReadingRef.get().addOnSuccessListener { readingSnapshot ->
                            val ph =
                                readingSnapshot.child("ph").getValue(Double::class.java).toString()
                            val tds =
                                readingSnapshot.child("tds").getValue(Double::class.java).toString()
                            val turbidity =
                                readingSnapshot.child("turbidity").getValue(Double::class.java)
                                    .toString()

                            items.add(
                                DeviceItem(
                                    deviceId,
                                    ph,
                                    tds,
                                    turbidity,
                                    deviceName,
                                    colorHex
                                )
                            )
                            binding.newDevice.visibility = View.VISIBLE
                            future.complete(Unit)
                        }
                    }
                }
                CompletableFuture.allOf(*deviceLoadPromises.toTypedArray()).thenRun {
                    runOnUiThread {
                        adapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DashboardActivity, "Failed to load devices", Toast.LENGTH_SHORT)
                    .show()
            }
        })

    }

    @SuppressLint("NewApi")
    private fun showAddDeviceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_device, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(true).create()

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

    @SuppressLint("MissingInflatedId")
    private fun showEditDeviceDialog(position: Int, item: DeviceItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_device, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.editTextDeviceName)
        val colorButton = dialogView.findViewById<Button>(R.id.chooseColorButton)
        val colorPreview = dialogView.findViewById<View>(R.id.viewColorPreview)
        val saveButton = dialogView.findViewById<Button>(R.id.saveButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        colorPreview.setBackgroundColor(Color.parseColor(item.colorHex))

        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(true).create()

        nameInput.setText(item.deviceName)

        var selectedColor = item.colorHex

        colorButton.setOnClickListener {
            ColorPickerDialogBuilder
                .with(this)
                .setTitle("Choose color")
                .initialColor(Color.parseColor(item.colorHex))
                .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
                .density(12)
                .setPositiveButton("OK") { _, color, _ ->
                    selectedColor = String.format("#%06X", 0xFFFFFF and color)
                    colorPreview.setBackgroundColor(color)
                }
                .setNegativeButton("Cancel", null)
                .build()
                .show()
        }

        saveButton.setOnClickListener {
            item.deviceName = nameInput.text.toString()
            item.colorHex = selectedColor
            adapter.updateItem(position, item)

            val userId = firebaseAuth.currentUser?.uid ?: return@setOnClickListener
            val deviceId = item.id
            val registryRef = FirebaseDatabase.getInstance()
                .getReference("registry")
                .child(userId)
                .child(deviceId)

            registryRef.child("deviceColor").setValue(item.colorHex)
            registryRef.child("deviceName").setValue(item.deviceName)
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun scanQR() {
        checkPermissionCamera(this)
    }

    private fun checkPermissionCamera(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            showCamera()
        } else if (shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)) {
            Toast.makeText(context, "Camera Permission Required", Toast.LENGTH_LONG).show()
        } else requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
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
            } else {
                checkResult(result.contents)
            }
        }

    private fun checkResult(deviceId: String) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        addDeviceRegistry(userId, deviceId)
    }

    private fun addDeviceRegistry(userId: String, deviceId: String) {
        val customDeviceId = generateCustomDeviceId()
        val deviceName = "Device 1"
        val regItem = DeviceRegistry(true, customDeviceId, deviceName)
        registryRef = FirebaseDatabase.getInstance().getReference("registry")
        database = FirebaseDatabase.getInstance().getReference("esp32")
        adminDatabase =
            FirebaseDatabase.getInstance().getReference("notificationAdmin").child("newDevices")
        val thisDeviceRegistryRef =
            FirebaseDatabase.getInstance().getReference("registry").child(userId).child(deviceId)

        thisDeviceRegistryRef.get().addOnSuccessListener { thisRegSnapshot ->
            registryRef.get().addOnSuccessListener { regSnapshot ->
                var deviceAlreadyLinked = false
                var deviceLinkedHere = false

                for (userSnapshot in regSnapshot.children) {
                    val userDevices = userSnapshot.children
                    for (deviceSnapshot in userDevices) {
                        if ((deviceSnapshot.key == deviceId) && (deviceSnapshot.key == thisRegSnapshot.toString())) {
                            deviceLinkedHere = true
                            break
                        } else if (deviceSnapshot.key == deviceId) {
                            deviceAlreadyLinked = true
                            break
                        }
                    }
                    if (deviceAlreadyLinked || deviceLinkedHere) break
                }


                if (deviceAlreadyLinked)
                    Toast.makeText(
                        this,
                        "This device is already linked to another user",
                        Toast.LENGTH_SHORT
                    ).show()
                else if (deviceLinkedHere) Toast.makeText(
                    this,
                    "This device is already linked to this user",
                    Toast.LENGTH_SHORT
                ).show()
                else {
                    registryRef.child(userId).child(deviceId).get()
                        .addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) Toast.makeText(
                                this,
                                "Device is already registered under this user",
                                Toast.LENGTH_SHORT
                            ).show()
                            else {
                                database.child(deviceId).get()
                                    .addOnSuccessListener { deviceSnapshot ->
                                        if (deviceSnapshot.exists()) {
                                            registryRef.child(userId).child(deviceId)
                                                .setValue(regItem)
                                                .addOnSuccessListener {
                                                    fetchDeviceData(deviceId)
                                                    binding.newDevice.visibility = View.VISIBLE
                                                    Toast.makeText(
                                                        this,
                                                        "Device added to registry",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                    val currentUser =
                                                        firebaseAuth.currentUser?.uid ?: "N/A"
                                                    val customAdminNotification =
                                                        generateAdminNotificationId()
                                                    val currentDate = Date()
                                                    val dateFormat = SimpleDateFormat(
                                                        "yyyy-MM-dd",
                                                        Locale.getDefault()
                                                    )
                                                    val formattedDate =
                                                        dateFormat.format(currentDate)
                                                    val registeredAt = System.currentTimeMillis()
                                                    val userReference =
                                                        FirebaseDatabase.getInstance()
                                                            .getReference("Users")
                                                            .child(currentUser)
                                                    userReference.get()
                                                        .addOnSuccessListener { snapshot ->
                                                            val customUserId =
                                                                snapshot.child("customUID")
                                                                    .getValue(String::class.java)
                                                                    ?: "N/A"

                                                            val notificationAdmin =
                                                                AdminNotification(
                                                                    id = customAdminNotification,
                                                                    customId = deviceId,
                                                                    date = formattedDate,
                                                                    time = registeredAt,
                                                                    description = ("Device $deviceId added to user $customUserId")
                                                                )
                                                            adminDatabase.child(deviceId)
                                                                .setValue(notificationAdmin)
                                                                .addOnSuccessListener {
                                                                    //TODO
                                                                }.addOnFailureListener { e ->
                                                                    Toast.makeText(
                                                                        this,
                                                                        "Error sending admin notification: ${e.message}",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                        }.addOnFailureListener { e ->
                                                        Toast.makeText(
                                                            this,
                                                            "Error in registering device to registry: ${e.message}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                        } else if (!deviceSnapshot.exists())
                                            Toast.makeText(
                                                this,
                                                "Device QR unrecognized.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        else {
                                            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                        }.addOnFailureListener { e ->
                            Toast.makeText(this, "Fetch failure: ${e.message}", Toast.LENGTH_SHORT)
                                .show()
                        }
                }
            }
        }
    }

    private fun fetchDeviceData(deviceId: String) {
        val userId = firebaseAuth.currentUser?.uid ?: return

        val deviceReading = FirebaseDatabase.getInstance().getReference("esp32").child(deviceId)
        val deviceData =
            FirebaseDatabase.getInstance().getReference("registry").child(userId).child(deviceId)
        deviceReading.get().addOnSuccessListener { snapshot ->
            deviceData.get().addOnSuccessListener { dataSnapshot ->
                val deviceName =
                    dataSnapshot.child("deviceName").getValue(String::class.java) ?: "Device 1"
                val deviceColor =
                    dataSnapshot.child("deviceColor").getValue(String::class.java) ?: "#FFFFFF"
                val phValue = snapshot.child("ph").getValue(Float::class.java) ?: "N/A"
                val tdsValue = snapshot.child("tds").getValue(Float::class.java) ?: "N/A"
                val turbidityValue =
                    snapshot.child("turbidity").getValue(Float::class.java) ?: "N/A"

                val newItem = DeviceItem(
                    deviceId,
                    phValue.toString(),
                    tdsValue.toString(),
                    turbidityValue.toString(),
                    deviceName,
                    deviceColor
                )
                adapter.addItem(newItem)
                Toast.makeText(this, "Device added successfully", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { _ ->

            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error fetching device data: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun generateCustomDeviceId(): String {
        val allowedChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val random = Random()
        val length = 8
        return (1..length)
            .map { allowedChars[random.nextInt(allowedChars.length)] }
            .joinToString("")
    }

    private fun generateAdminNotificationId(): String {
        val allowedChars = "12345abcdeABCDE"
        val random = Random()
        val length = 8
        return (1..length)
            .map { allowedChars[random.nextInt(allowedChars.length)] }
            .joinToString("")
    }

    override fun onItemClick(item: DeviceItem) {
        val intent = Intent(this, WaterAnalysisActivity::class.java)
        intent.putExtra("deviceItem", item)
        startActivity(intent)
    }

    override fun onItemLongClick(position: Int, item: DeviceItem) {
        showEditDeviceDialog(position, item)
    }
}