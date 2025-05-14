package com.aquamanagers.aquamanage_app.models

data class HistoryItem(
    val phValue: Double = 0.0,
    val tdsValue: Double = 0.0,
    val turbidityValue: Double = 0.0,
    val timeStamp: Long = 0L,
    val date: String = "",
    val status: String = ""
)
