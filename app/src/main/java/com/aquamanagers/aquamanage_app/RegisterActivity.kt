package com.aquamanagers.aquamanage_app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.aquamanagers.aquamanage_app.databinding.ActivityRegisterBinding
import com.aquamanagers.aquamanage_app.models.AdminNotification
import com.aquamanagers.aquamanage_app.models.Users
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var adminDatabase: DatabaseReference
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
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

        // Toggle password visibility
        binding.password.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2
                val drawable = binding.password.compoundDrawables[drawableEnd]

                if (drawable != null && event.rawX >= (binding.password.right - drawable.bounds.width() - binding.password.paddingEnd)) {
                    binding.password.performClick()
                    isPasswordVisible = !isPasswordVisible
                    togglePasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
        }

        // Toggle confirm password visibility
        binding.confirmPassword.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2
                val drawable = binding.confirmPassword.compoundDrawables[drawableEnd]

                if (drawable != null && event.rawX >= (binding.confirmPassword.right - drawable.bounds.width() - binding.confirmPassword.paddingEnd)) {
                    binding.confirmPassword.performClick()
                    isConfirmPasswordVisible = !isConfirmPasswordVisible
                    toggleConfirmPasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun togglePasswordVisibility() {
        binding.password.inputType = if (isPasswordVisible)
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        else
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        binding.password.typeface = binding.email.typeface
        binding.password.setSelection(binding.password.text.length)
        val icon = if (isPasswordVisible) R.drawable.ic_eye_open else R.drawable.ic_eye_closed
        binding.password.setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0)
    }

    private fun toggleConfirmPasswordVisibility() {
        binding.confirmPassword.inputType = if (isConfirmPasswordVisible)
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        else
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        binding.confirmPassword.typeface = binding.email.typeface
        binding.confirmPassword.setSelection(binding.confirmPassword.text.length)
        val icon =
            if (isConfirmPasswordVisible) R.drawable.ic_eye_open else R.drawable.ic_eye_closed
        binding.confirmPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0)
    }

    private fun validateInput(
        firstName: String, lastName: String, email: String,
        password: String, confirmPassword: String
    ): Boolean {
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun registerUser(
        firstName: String, middleInitial: String, lastName: String,
        email: String, password: String
    ) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val customUserId = generateCustomUserId()
                    val customAdminNotificationId = generateAdminNotificationId()
                    val firebaseUser = firebaseAuth.currentUser
                    val authUid = firebaseUser?.uid

                    if (authUid != null) {
                        database = FirebaseDatabase.getInstance().getReference("Users")
                        adminDatabase =
                            FirebaseDatabase.getInstance().getReference("notificationAdmin")
                                .child("newUsers")
                        val createdAt = System.currentTimeMillis()
                        val currentDate = Date()
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val formattedDate = dateFormat.format(currentDate)

                        val user = Users(
                            firstName = firstName,
                            middleInitial = middleInitial,
                            lastName = lastName,
                            email = email,
                            customUID = customUserId,
                            createdAt = createdAt
                        )

                        val adminNotification = AdminNotification(
                            id = customAdminNotificationId,
                            date = formattedDate,
                            time = createdAt,
                            description = "User $customUserId created"
                        )

                        database.child(authUid).setValue(user)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Registration successful. Please log in.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                adminDatabase.child(authUid).setValue(adminNotification)
                                    .addOnSuccessListener {
                                        //TODO
                                    }.addOnFailureListener { e ->
                                        Toast.makeText(
                                            this,
                                            "Error sending admin notification: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                firebaseAuth.signOut()

                                val intent = Intent(this, LoginActivity::class.java)
                                intent.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error saving user data", Toast.LENGTH_SHORT)
                                    .show()
                            }
                    } else {
                        Toast.makeText(this, "Failed to get Auth UID", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Registration failed",
                        Toast.LENGTH_SHORT
                    ).show()
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

    private fun generateAdminNotificationId(): String {
        val allowedChars = "12345abcdeABCDE"
        val random = Random()
        val length = 8
        return (1..length)
            .map { allowedChars[random.nextInt(allowedChars.length)] }
            .joinToString("")
    }
}
