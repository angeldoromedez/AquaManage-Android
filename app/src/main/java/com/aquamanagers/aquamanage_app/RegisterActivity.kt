package com.aquamanagers.aquamanage_app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aquamanagers.aquamanage_app.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.Random

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.signupButton.setOnClickListener {
            val firstName = binding.firstname.text.toString().trim()
            val middleInitial = binding.middleInitial.text.toString().trim()
            val lastName = binding.lastname.text.toString().trim()
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString()
            val confirmPassword = binding.confirmPassword.text.toString()

            if (validateInput(firstName, lastName, email, password, confirmPassword)) {
                registerUser(firstName, middleInitial, lastName, email, password)
            }
        }
    }

    private fun validateInput(firstName: String, lastName: String, email: String, password: String, confirmPassword: String): Boolean {
        if (firstName.isEmpty()) {
            binding.firstname.error = "First Name is required"
            return false
        }
        if (lastName.isEmpty()) {
            binding.lastname.error = "Last Name is required"
            return false
        }
        if (email.isEmpty()) {
            binding.email.error = "Email is required"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.email.error = "Enter a valid email address"
            return false
        }
        if (password.isEmpty()) {
            binding.password.error = "Password is required"
            return false
        }
        if (password.length < 6) {
            binding.password.error = "Password must be at least 6 characters"
            return false
        }
        if (!password.matches(".*\\d.*".toRegex())) {
            binding.password.error = "Password must contain at least one digit"
            return false
        }
        if (!password.matches(".*[A-Z].*".toRegex())) {
            binding.password.error = "Password must contain at least one uppercase letter"
            return false
        }
        if (password != confirmPassword) {
            binding.confirmPassword.error = "Passwords do not match"
            return false
        }
        return true
    }

    private fun registerUser(firstName: String, middleInitial: String, lastName: String, email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val customUserId = generateCustomUserId()
                    val firebaseUser = firebaseAuth.currentUser
                    val authUid = firebaseUser?.uid

                    if (authUid != null) {
                        database = FirebaseDatabase.getInstance().getReference("Users")

                        // Generate the current timestamp (in milliseconds)
                        val createdAt = System.currentTimeMillis()

                        // Create a new Users object including the createdAt timestamp
                        val user = model.Users(
                            firstName = firstName,
                            middleInitial = middleInitial,
                            lastName = lastName,
                            email = email,
                            password = password,
                            customUID = customUserId,
                            createdAt = createdAt
                        )

                        database.child(authUid).setValue(user).addOnSuccessListener {
                            Toast.makeText(this, "Registration successful. Please log in.", Toast.LENGTH_SHORT).show()

                            firebaseAuth.signOut()

                            val intent = Intent(this, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }.addOnFailureListener {
                            Toast.makeText(this, "Error saving user data", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Failed to get Auth UID", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, task.exception?.message ?: "Registration failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun generateCustomUserId(): String {
        val allowedChars = "0123456789"
        val random = Random()
        val length = 8
        return (1..length)
            .map { allowedChars[random.nextInt(allowedChars.length)] }
            .joinToString("")
    }
}
