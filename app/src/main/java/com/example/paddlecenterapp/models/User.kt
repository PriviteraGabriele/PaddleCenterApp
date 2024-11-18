package com.example.paddlecenterapp.models

data class User(
    val birthDate: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val gender: String = "",
    val profileImageUrl: String? = null,
    val friends: Map<String, Boolean>? = null,
    val reports: Map<String, Report>? = null
)