package com.example.paddlecenterapp.models

data class Coach(
    val id: String,
    val name: String,
    val availability: Map<String, Slot>
)

