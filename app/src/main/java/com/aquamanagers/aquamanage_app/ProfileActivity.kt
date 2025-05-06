package com.aquamanagers.aquamanage_app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.aquamanagers.aquamanage_app.adapters.AvatarAdapter
import com.aquamanagers.aquamanage_app.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val savedAvatarResId = prefs.getInt("selectedAvatar", -1)
        if(savedAvatarResId != -1)
            binding.profileImage.setImageResource(savedAvatarResId)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")

        // Fetch user data from Realtime Database
        fetchUserData()

        // Logout Button
        binding.logoutButton.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        // Change Password Button
        binding.changePassButton.setOnClickListener {
            startActivity(Intent(this, ChangePassword::class.java))
        }

        // Back Arrow Button
        binding.backArrow.setOnClickListener {
            finish()
        }

        // Tech Support Button
        binding.TechSupport.setOnClickListener {
            startActivity(Intent(this, ChatSupportActivity::class.java))
        }

        // Avatar Edit Icon Click
        binding.editProfileIcon.setOnClickListener {
            showAvatarSelectionDialog()
        }
    }

    private fun fetchUserData() {
        val userId = firebaseAuth.currentUser?.uid

        if (userId != null) {
            database.child(userId).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val firstName = snapshot.child("firstName").value?.toString()?.trim() ?: ""
                    val middleInitial = snapshot.child("middleInitial").value?.toString()?.trim()
                    val lastName = snapshot.child("lastName").value?.toString()?.trim() ?: ""
                    val userEmail = snapshot.child("email").value?.toString() ?: ""

                    val userName = if (!middleInitial.isNullOrEmpty()) {
                        "$firstName ${middleInitial.first().uppercaseChar()}. $lastName"
                    } else {
                        "$firstName $lastName"
                    }

                    // Display user data
                    binding.userName.text = userName
                    binding.userEmail.text = userEmail
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    // Avatar selection dialog with GridView
    private fun showAvatarSelectionDialog() {
        val avatars = listOf(
            R.drawable.ava_a, R.drawable.ava_b, R.drawable.ava_c, R.drawable.ava_d, R.drawable.ava_e,
            R.drawable.ava_f, R.drawable.ava_g, R.drawable.ava_h, R.drawable.ava_i, R.drawable.ava_j
        )

        val dialogView = layoutInflater.inflate(R.layout.dialog_avatar_picker, null)
        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.avatarRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        val alertDialog = android.app.AlertDialog.Builder(this)
            .setTitle("Choose Your Avatar")
            .setView(dialogView)
            .create()

        val adapter = AvatarAdapter(avatars) { selectedAvatarResId ->
            binding.profileImage.setImageResource(selectedAvatarResId)

            val prefs = getSharedPreferences("ProfileAvatar", MODE_PRIVATE)
            prefs.edit().putInt("selectedAvatar", selectedAvatarResId).apply()
            alertDialog.dismiss()
        }

        recyclerView.adapter = adapter

        alertDialog.show()
    }
}

