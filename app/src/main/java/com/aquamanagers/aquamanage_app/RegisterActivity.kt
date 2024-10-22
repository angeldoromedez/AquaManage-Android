package com.aquamanagers.aquamanage_app
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var rootDatabaseref: DatabaseReference
    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val signUpButton: Button = findViewById(R.id.signup_button)
        val loginLink: TextView = findViewById(R.id.login_link)
        val lastName: EditText = findViewById(R.id.lastname)
        val middleInitial: EditText =  findViewById(R.id.middle_initial)
        val firstName: EditText = findViewById(R.id.firstname)
        val email: EditText = findViewById(R.id.email)
        val password: EditText = findViewById(R.id.password)
        val confirmPassword: EditText = findViewById(R.id.confirm_password)

        var firstNameInput: String?
        var middleInitialInput: String?
        var lastNameInput: String?
        var emailInput: String?
        var passwordInput: String?
        var confirmPasswordInput: String?

        loginLink.setOnClickListener{
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        rootDatabaseref= FirebaseDatabase.getInstance().reference.child("Users")

        signUpButton.setOnClickListener{
            firstNameInput = firstName.text.toString()
            middleInitialInput = middleInitial.text.toString()
            lastNameInput = lastName.text.toString()
            emailInput = email.text.toString()
            passwordInput = password.text.toString()
            confirmPasswordInput = confirmPassword.text.toString()

            val userHash = hashMapOf("First Name" to firstNameInput, "Last Name " to lastNameInput,
                "Middle Initial" to middleInitialInput, "Email" to emailInput, "Password" to passwordInput)

            rootDatabaseref.push().setValue(userHash).addOnSuccessListener { _ ->
                Toast.makeText(this, "Sign-up complete!", Toast.LENGTH_LONG).show()
                lastName.setText("")
                firstName.setText("")
                middleInitial.setText("")
                email.setText("")
                password.setText("")
                confirmPassword.setText("")
            }.addOnFailureListener{_ ->
                Toast.makeText(this, "Something went wrong :(", Toast.LENGTH_SHORT).show()
            }
        }
    }
}