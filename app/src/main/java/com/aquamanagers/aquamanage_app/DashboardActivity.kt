package com.aquamanagers.aquamanage_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.aquamanagers.aquamanage_app.databinding.ActivityDashboardBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference

class DashboardActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {

        firebaseAuth = Firebase.auth

        super.onCreate(savedInstanceState)

        val currentUser = firebaseAuth.currentUser
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setContentView(R.layout.activity_dashboard)

        binding.profileIcon.setOnClickListener{
            firebaseAuth.signOut()
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.addDeviceButton.setOnClickListener{
            startActivity(Intent(this,ProfileActivity::class.java))
        }

        binding.addDeviceCardView.setOnClickListener{
            startActivity(Intent(this,ProfileActivity::class.java))
        }

        binding.profileName.setOnClickListener{
            startActivity(Intent(this,ProfileActivity::class.java))
        }
    }
}