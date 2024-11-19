package com.example.paddlecenterapp.models

data class Field(
    val id: String = "",            // ID univoco del campo
    val name: String = "",          // Nome del campo
    val availability: Map<String, Slot> = emptyMap() // Disponibilità dei slot
)
