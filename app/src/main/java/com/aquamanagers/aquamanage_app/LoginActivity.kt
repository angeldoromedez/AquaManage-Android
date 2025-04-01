package com.aquamanagers.aquamanage_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aquamanagers.aquamanage_app.databinding.ActivityLoginBinding
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        FirebaseApp.initializeApp(this)
        super.onCreate(savedInstanceState)

        firebaseAuth = Firebase.auth
        Thread.sleep(1500)
        installSplashScreen()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId = firebaseAuth.currentUser?.uid
        if(userId!=null){
            val database = FirebaseDatabase.getInstance().getReference("Users").child(userId)
            database.addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot){
                    val userName = snapshot.child("firstName").getValue(String::class.java)?:"user"
                    Toast.makeText(applicationContext,"Welcome back, $userName", Toast.LENGTH_SHORT).show()
                    reload()
                }
                override fun onCancelled(error: DatabaseError){
                    Log.w("FirebaseDB", "Failed to read value", error.toException())
                }
            })
        }else{
            Toast.makeText(this,"Welcome",Toast.LENGTH_SHORT).show()
        }

        binding.signupLink.setOnClickListener{
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        binding.loginButton.setOnClickListener{
            val email = binding.email.text.toString()
            val password = binding.password.text.toString()

            if(email.isNotBlank() && password.isNotBlank()){
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnSuccessListener{
                        val currentUser = firebaseAuth.currentUser!!.uid
                        val database = FirebaseDatabase.getInstance().getReference("Users").child(currentUser)
                        database.addListenerForSingleValueEvent(object : ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot){
                                val userName = snapshot.child("firstName").getValue(String::class.java)?:"user"
                                Toast.makeText(applicationContext,"Welcome back, $userName", Toast.LENGTH_SHORT).show()
                                reload()
                            }
                            override fun onCancelled(error: DatabaseError){
                                Log.w("FirebaseDB", "Failed to read value", error.toException())
                            }
                        })
                        val intent = Intent(this, DashboardActivity::class.java)
                        startActivity(intent)
                }.addOnFailureListener{ e ->
                    Toast.makeText(this, "Error: $e", Toast.LENGTH_SHORT).show()
                }
            } else{
                Toast.makeText(this,"Fields cannot be left blank", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun reload(){
        startActivity(Intent(this, DashboardActivity::class.java))
    }
}
