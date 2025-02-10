package com.aquamanagers.aquamanage_app.Models

data class Users(
    val firstName:String? = null,
    val middleInitial:String? = null,
    val lastName:String? = null,
    val email:String? = null,
    val username: String?,
    val password: String? = null){

}

