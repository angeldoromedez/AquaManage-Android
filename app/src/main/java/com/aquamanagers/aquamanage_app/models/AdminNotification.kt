package com.aquamanagers.aquamanage_app.models

data class AdminNotification(
    val id: String = "",
    val customId: String = "",
    val date: String = "",
    val time: Long = 0L,
    val description: String = ""
)
