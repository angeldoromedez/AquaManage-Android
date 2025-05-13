package com.aquamanagers.aquamanage_app.models

data class HistoryItem(
    val id: String,
    val phValue: Double,
    val tdsValue: Double,
    val turbidityValue: Double,
    val timestamp: Long,
    val status: String
)
