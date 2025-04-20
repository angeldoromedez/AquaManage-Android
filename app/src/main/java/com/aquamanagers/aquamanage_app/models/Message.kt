package com.aquamanagers.aquamanage_app.models

data class Message(
    val sender: String = "",
    val customUID: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val messageId: String? = null
)
