package com.aquamanagers.aquamanage_app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquamanagers.aquamanage_app.adapters.DeviceCardAdapter
import com.aquamanagers.aquamanage_app.databinding.ActivityDashboardBinding
import com.aquamanagers.aquamanage_app.models.DeviceItem
import com.aquamanagers.aquamanage_app.models.Users
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@Suppress("DEPRECATION")
class DashboardActivity : AppCompatActivity(), DeviceCardAdapter.OnItemClickListener {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter:DeviceCardAdapter
    private val items = mutableListOf<DeviceItem>()
    @SuppressLint("NotifyDataSetChanged")
    private val scanResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
        if(result.resultCode == RESULT_OK){
            val deviceItem = result.data?.getParcelableExtra<DeviceItem>("scannedData")
            deviceItem?.let {
                items.add(it)
                adapter.notifyDataSetChanged()
            }
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

        binding.addDevice.setOnClickListener{
            showAddDeviceDialog()
        }

        items.add(DeviceItem("test1", "7.0", "500", "10"))
        items.add(DeviceItem("test2", "6.5", "600", "15"))
        adapter.notifyDataSetChanged()

        val currentUser = firebaseAuth.currentUser
        if(currentUser!=null){
            val userId = currentUser.uid
            val devicesRef = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("connectedDevices")
            val database = FirebaseDatabase.getInstance().getReference("Users").child(userId)

            devicesRef.addListenerForSingleValueEvent(object:ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    items.clear()
                    for(deviceSnapshot in snapshot.children) {
                        val deviceId = deviceSnapshot.key
                        deviceId?.let{
                            FirebaseDatabase.getInstance().getReference("esp32").child(it)
                                .addListenerForSingleValueEvent(object:ValueEventListener{
                                    override fun onDataChange(espSnapshot:DataSnapshot){
                                        val ph = espSnapshot.child("ph").getValue(String::class.java)?:"0"
                                        val tds = espSnapshot.child("tds").getValue(String::class.java)?:"0"
                                        val turbidity = espSnapshot.child("turbidity").getValue(String::class.java)?:"0"
                                        items.add(DeviceItem(it,ph,tds,turbidity))
                                        adapter.notifyDataSetChanged()
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        TODO("Not yet implemented")
                                    }
                                })
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
            database.addListenerForSingleValueEvent(object: ValueEventListener{
                @SuppressLint("SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()){
                        val additionalInfo = snapshot.getValue(Users::class.java)
                        val userName = additionalInfo?.firstName.toString()
                        binding.profileName.text = userName
                    } else {
                        binding.profileName.text = "User data not found"
                    }
                }
                override fun onCancelled(error: DatabaseError){
                }
            })
        } else {
            reload()
        }

        binding.profileIcon.setOnClickListener{
            firebaseAuth.signOut()
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.profileName.setOnClickListener{
            startActivity(Intent(this,ProfileActivity::class.java))
        }
    }

    private fun reload() {
        startActivity(Intent(this, LoginActivity::class.java))
    }

    private fun showAddDeviceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_device, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val closeButton: ImageView = dialogView.findViewById(R.id.close_button)
        val qrScannerButton: Button = dialogView.findViewById(R.id.open_qr_scanner_button)

        closeButton.setOnClickListener{
            dialog.dismiss()
        }

        qrScannerButton.setOnClickListener{
            Toast.makeText(this, "Opening camera...", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            val intent = Intent(this, ScanQRActivity::class.java)
            scanResultLauncher.launch(intent)
       }
        dialog.show()
    }

    override fun onItemClick(item: DeviceItem) {
        val intent = Intent(this,WaterAnalysisActivity::class.java)
        intent.putExtra("deviceItem", item)
        startActivity(intent)
    }
}