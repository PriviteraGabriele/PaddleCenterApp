package com.example.paddlecenterapp.models

data class Field(
    val id: String = "",
    val name: String = "",
    val availability: Map<String, Slot> = emptyMap()
)
