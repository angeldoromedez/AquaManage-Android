package com.aquamanagers.aquamanage_app.models

data class Users(
    val firstName: String = "",
    val middleInitial: String = "",
    val lastName: String = "",
    val email: String = "",
    val customUID: String = "",
    val createdAt: Long = 0L)

