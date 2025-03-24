package com.aquamanagers.aquamanage_app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.aquamanagers.aquamanage_app.models.DeviceItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions

class ScanQRActivity: AppCompatActivity() {
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){
            isGranted: Boolean ->
        if(isGranted){
            showCamera()
        }
        else{
            //TODO
        }
    }

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        scanQR()
    }

    private fun scanQR(){
        checkPermissionCamera(this)
    }

    private fun checkPermissionCamera(context: Context) {
        if(ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showCamera()
        } else if(shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)){
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

    private val scanLauncher = registerForActivityResult(ScanContract()){ result: ScanIntentResult ->
        run {
            if (result.contents == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                checkResult(result.contents)
            }
        }
    }

    private fun setResult(string:String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if(currentUser != null) {
            val userId = currentUser.uid
            val userDevicesRef = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("connectedDevices")
            val deviceReading = FirebaseDatabase.getInstance().getReference("esp32").child(string)

            userDevicesRef.child(string).setValue(true)
            deviceReading.get().addOnSuccessListener{snapshot ->
                if(snapshot.exists()){
                    val phValue = snapshot.child("ph").getValue(String::class.java) ?: "N/A"
                    val tdsValue = snapshot.child("tds").getValue(String::class.java) ?: "N/A"
                    val turbidityValue = snapshot.child("turbidity").getValue(String::class.java) ?: "N/A"

                    val newItem = DeviceItem(string, phValue, tdsValue, turbidityValue)
                    val resultIntent = Intent().apply{
                        putExtra("scannedData", newItem)
                    }
                    setResult(RESULT_OK , resultIntent)
                } else {
                    val newItem = DeviceItem(string, "0", "0", "0")
                    val resultIntent = Intent().apply{
                        putExtra("scannedData", newItem)
                    }
                    setResult(RESULT_OK , resultIntent)
                }
                finish()
            }.addOnFailureListener{ e ->
                Toast.makeText(this, "Error fetching device data: ${e.message}", Toast.LENGTH_SHORT).show()
                setResult(RESULT_CANCELED)
                finish()
            }
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun checkResult(string:String){
        val database = FirebaseDatabase.getInstance().getReference("esp32")
        database.child(string).get().addOnSuccessListener{ snapshot ->
            if(snapshot.exists()){
                val usersRef = FirebaseDatabase.getInstance().getReference("Users")
                usersRef.orderByChild("connectedDevices/$string").equalTo(true)
                    .get().addOnSuccessListener{ userSnapshot ->
                        if(userSnapshot.exists()){
                            Toast.makeText(this, "This device is linked to a different account", Toast.LENGTH_SHORT).show()
                            finish()
                        } else
                            setResult(string)
                    }
            } else{
                Toast.makeText(this, "Device QR unrecognized.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }.addOnFailureListener{ e ->
            Toast.makeText(this, e.message?: "An error occurred", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}