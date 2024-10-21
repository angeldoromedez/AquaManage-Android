package com.aquamanagers.aquamanage_app
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.FirebaseApp

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        FirebaseApp.initializeApp(this)

        val signUpButton: Button = findViewById(R.id.signup_button)
        val loginLink: TextView = findViewById(R.id.login_link)
        val lastName: EditText = findViewById(R.id.lastname)
        val middleInitial: EditText =  findViewById(R.id.middle_initial)
        val firstName: EditText = findViewById(R.id.firstname)
        val email: EditText = findViewById(R.id.email)
        val password: EditText = findViewById(R.id.password)
        val confirmPassword: EditText = findViewById(R.id.confirm_password)

        loginLink.setOnClickListener{
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }


        signUpButton.setOnClickListener{
            val firstNameInput = firstName.text.toString().trim()
            middleInitial.text.toString().trim()
            val lastNameInput = lastName.text.toString().trim()
            val emailInput = email.text.toString().trim()
            val passwordInput = password.text.toString().trim()
            val confirmPasswordInput = confirmPassword.text.toString().trim()

            if (firstNameInput.isEmpty() || lastNameInput.isEmpty() || emailInput.isEmpty() || passwordInput.isEmpty() || confirmPasswordInput.isEmpty()) {

                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
            if (passwordInput != confirmPasswordInput) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "Sign-up complete!", Toast.LENGTH_SHORT).show()
                lastName.setText("")
                firstName.setText("")
                middleInitial.setText("")
                email.setText("")
                password.setText("")
                confirmPassword.setText("")
            }
        }
    }
}