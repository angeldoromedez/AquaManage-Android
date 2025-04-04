package model

data class Users(
    val firstName: String = "",
    val middleInitial: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val customUID: String = "",
    val createdAt: Long = 0L
)
