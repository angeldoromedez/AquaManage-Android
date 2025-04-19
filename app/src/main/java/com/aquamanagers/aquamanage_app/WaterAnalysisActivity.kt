package com.aquamanagers.aquamanage_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aquamanagers.aquamanage_app.databinding.ActivityWaterAnalysisBinding
import com.aquamanagers.aquamanage_app.models.DeviceItem
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase

class WaterAnalysisActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivityWaterAnalysisBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        firebaseAuth = Firebase.auth

        super.onCreate(savedInstanceState)

        binding = ActivityWaterAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStart.setOnClickListener{
            FirebaseDatabase.getInstance().getReference("esp32").child("ESP32-FD49F8").child("controls").setValue(1)
        }

        val deviceItem = intent.getParcelableExtra<DeviceItem>("deviceItem")
        if(deviceItem!=null) {
            //TODO
        }
    }
}