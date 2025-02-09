package com.aquamanagers.aquamanage_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.aquamanagers.aquamanage_app.Models.Users
import com.aquamanagers.aquamanage_app.databinding.ActivityRegisterBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding:ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = Firebase.auth

        binding.loginLink.setOnClickListener{
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.signupButton.setOnClickListener{
            val firstName = binding.firstname.text.toString()
            val middleInitial = binding.middleInitial.text.toString()
            val lastName = binding.lastname.text.toString()
            val email = binding.email.text.toString()
            val password = binding.password.text.toString()
            val confirmPassword = binding.confirmPassword.text.toString()
            val index = email.lastIndexOf("@")
            val username = if(index != 1) email.substring(0, index) else email

            database = FirebaseDatabase.getInstance().getReference("Users")
            val user = Users(firstName, middleInitial, lastName, email, username, password)

            if(firstName.isNotBlank() && middleInitial.isNotBlank() && lastName.isNotBlank() && email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()){
                if(password == confirmPassword){
                    firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener{
                        if(it.isSuccessful){
                            database.child(username).setValue(user).addOnSuccessListener {
                                Toast.makeText(this, "Registration complete", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, LoginActivity::class.java)
                                startActivity(intent)
                            }.addOnFailureListener{
                                Toast.makeText(this,"Something went wrong, please retry", Toast.LENGTH_SHORT).show()
                            }
                        } else{
                            Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
                } else{
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else{
                Toast.makeText(this, "Fields cannot be left blank", Toast.LENGTH_SHORT).show()
            }
        }
    }
}