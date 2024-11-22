package com.example.paddlecenterapp.models

data class Reservation(
    val id: String = "",  // Impostiamo valori di default
    val type: String = "",
    val slotDate: String = "",
    val participants: List<String> = emptyList(),
    val coachId: String? = null,
    val fieldId: String? = null,
    var fieldName: String? = null,
    var coachName: String? = null
)

