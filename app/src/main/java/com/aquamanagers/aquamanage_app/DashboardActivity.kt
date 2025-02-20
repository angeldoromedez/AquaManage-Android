package com.aquamanagers.aquamanage_app

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.aquamanagers.aquamanage_app.databinding.ActivityDashboardBinding
import com.aquamanagers.aquamanage_app.models.Users
import com.google.firebase.Firebase
import com.google.firebase.auth.*
import com.google.firebase.database.*

class DashboardActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        firebaseAuth = Firebase.auth

        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUser = firebaseAuth.currentUser
        if(currentUser!=null){
            val userId = currentUser.uid

            val database = FirebaseDatabase.getInstance().getReference("Users").child(userId)
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
                    Log.w("TAG", "Failed to read value.", error.toException())
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

        binding.addDevice.setOnClickListener{
            startActivity(Intent(this,ProfileActivity::class.java))
        }

        binding.profileName.setOnClickListener{
            startActivity(Intent(this,ProfileActivity::class.java))
        }
    }

    private fun reload() {
        startActivity(Intent(this, LoginActivity::class.java))
    }
}