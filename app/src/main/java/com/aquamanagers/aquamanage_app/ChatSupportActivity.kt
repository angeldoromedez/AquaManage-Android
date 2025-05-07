package com.aquamanagers.aquamanage_app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aquamanagers.aquamanage_app.adapters.ChatAdapter
import com.aquamanagers.aquamanage_app.databinding.ActivityChatSupportBinding
import com.aquamanagers.aquamanage_app.models.AdminNotification
import com.aquamanagers.aquamanage_app.models.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

class ChatSupportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatSupportBinding
    private lateinit var databaseRef: DatabaseReference
    private lateinit var userId: String
    private var senderDisplayName: String = "Unknown"
    private var customUID: String = "N/A"

    private lateinit var chatAdapter: ChatAdapter
    private val messageList = mutableListOf<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChatSupportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        databaseRef = FirebaseDatabase.getInstance().getReference("conversation").child(userId)

        fetchUserDetails()

        binding.sendButton.setOnClickListener {
            sendMessage()
        }

        binding.backArrow.setOnClickListener {
            finish()
        }
    }

    private fun fetchUserDetails() {
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                val middleInitial =
                    snapshot.child("middleInitial").getValue(String::class.java) ?: ""
                val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                senderDisplayName = "$firstName $middleInitial. $lastName"
                customUID = snapshot.child("customUID").getValue(String::class.java) ?: "N/A"

                // Initialize chat adapter after getting customUID
                chatAdapter = ChatAdapter(messageList, customUID)
                binding.messagesRecyclerView.layoutManager =
                    LinearLayoutManager(this@ChatSupportActivity)
                binding.messagesRecyclerView.adapter = chatAdapter

                listenForMessages()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@ChatSupportActivity,
                    "Failed to fetch user details",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun sendMessage() {
        val messageText = binding.editMessage.text.toString().trim()
        if (messageText.isEmpty()) return

        val messageId = databaseRef.push().key ?: return
        val message = Message(
            sender = senderDisplayName,
            customUID = customUID,
            message = messageText,
            timestamp = System.currentTimeMillis()
        )

        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isFirstMessage = !snapshot.exists()
                databaseRef.child(messageId).setValue(message)
                    .addOnSuccessListener {
                        binding.editMessage.setText("")
                        if (isFirstMessage) {
                            val customAdminNotificationId = generateAdminNotificationId()
                            val currentDate = Date()
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val formattedDate = dateFormat.format(currentDate)
                            val createdAt = System.currentTimeMillis()
                            val adminDatabase =
                                FirebaseDatabase.getInstance().getReference("notificationAdmin")
                                    .child("newChats")
                            val userReference =
                                FirebaseDatabase.getInstance().getReference("Users").child(userId)

                            userReference.get().addOnSuccessListener { snapshot ->
                                val userId =
                                    snapshot.child("customUID").getValue(String::class.java)
                                        ?: "N/A"
                                val chatId = generateChatId()

                                val adminNotification = AdminNotification(
                                    id = customAdminNotificationId,
                                    customId = userId,
                                    date = formattedDate,
                                    time = createdAt,
                                    description = "User $userId chatted"
                                )
                                adminDatabase.child(chatId).setValue(adminNotification)
                                    .addOnSuccessListener {
                                        //TODO
                                    }.addOnFailureListener { e ->
                                    Toast.makeText(
                                        this@ChatSupportActivity,
                                        "Failed to send admin notification: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this@ChatSupportActivity,
                            "Failed: ${it.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@ChatSupportActivity,
                    "Error checking Conversation History",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun listenForMessages() {
        databaseRef.orderByChild("timestamp").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                message?.let {
                    chatAdapter.addMessage(it)
                    binding.messagesRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@ChatSupportActivity,
                    "Failed to load messages.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun generateAdminNotificationId(): String {
        val allowedChars = "12345abcdeABCDE"
        val random = Random()
        val length = 8
        return (1..length)
            .map { allowedChars[random.nextInt(allowedChars.length)] }
            .joinToString("")
    }

    private fun generateChatId(): String {
        val allowedChars = "12345abcdeABCDE"
        val random = Random()
        val length = 10
        return (1..length)
            .map { allowedChars[random.nextInt(allowedChars.length)] }
            .joinToString("")
    }
}
