package com.aquamanagers.aquamanage_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.aquamanagers.aquamanage_app.databinding.ActivityDashboardBinding
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = firebaseAuth.currentUser
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setContentView(R.layout.activity_dashboard)

        binding.profileIcon.setOnClickListener{
            startActivity(Intent(this,ProfileActivity::class.java))
        }
    }
}