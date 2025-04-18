package com.aquamanagers.aquamanage_app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aquamanagers.aquamanage_app.databinding.ActivityChangePasswordBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePassword : AppCompatActivity() {

    private lateinit var currentPasswordField: EditText
    private lateinit var newPasswordField: EditText
    private lateinit var confirmNewPasswordField: EditText
    private lateinit var updatePasswordButton: Button
    private lateinit var backArrow: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityChangePasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentPasswordField = findViewById(R.id.currentPassword)
        newPasswordField = findViewById(R.id.newPassword)
        confirmNewPasswordField = findViewById(R.id.confirmNewPassword)
        updatePasswordButton = findViewById(R.id.btnUpdatePassword)
        backArrow = findViewById(R.id.back_arrow)

        auth = FirebaseAuth.getInstance()

        backArrow.setOnClickListener {
            finish()
        }

        updatePasswordButton.setOnClickListener {
            val currentPassword = currentPasswordField.text.toString().trim()
            val newPassword = newPasswordField.text.toString().trim()
            val confirmPassword = confirmNewPasswordField.text.toString().trim()

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = auth.currentUser
            if (user != null && user.email != null) {
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

                user.reauthenticate(credential).addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        if (validatePassword(newPassword)) {
                            user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    Toast.makeText(
                                        this,
                                        "Password updated successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    finish()
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Failed to update password: ${updateTask.exception?.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } else {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validatePassword(
        newPassword: String
    ): Boolean {
        if (newPassword.length < 6) {
            binding.newPassword.error = "Password must be at least 6 characters"
            return false
        }
        if (!newPassword.matches(".*\\d.*".toRegex())) {
            binding.newPassword.error = "Password must contain at least one digit"
            return false
        }
        if (!newPassword.matches(".*[A-Z].*".toRegex())) {
            binding.newPassword.error = "Password must contain at least one uppercase letter"
            return false
        }
        return true
    }
}