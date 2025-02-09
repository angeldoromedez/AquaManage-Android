package com.aquamanagers.aquamanage_app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aquamanagers.aquamanage_app.databinding.ActivityLoginBinding
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class LoginActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        FirebaseApp.initializeApp(this)
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = Firebase.auth

        val currentUser = firebaseAuth.currentUser
        if(currentUser!=null){
            reload()
        }

        binding.signupLink.setOnClickListener{
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        binding.loginButton.setOnClickListener{
            val email = binding.email.text.toString()
            val password = binding.password.text.toString()

            if(email.isNotBlank() && password.isNotBlank()){
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener{
                    if(it.isSuccessful){
                        Toast.makeText(this, "Welcome back, $email", Toast.LENGTH_SHORT).show()
                    } else{
                        Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
            } else{
                Toast.makeText(this,"Fields cannot be left blank", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun reload() {
        TODO("Not yet implemented")
    }
}