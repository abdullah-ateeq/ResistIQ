package com.example.resistiq.model

enum class CalculationType {
    COLOR_TO_VALUE,
    VALUE_TO_COLOR,
    OHMS_LAW
}

data class ResistorHistoryItem(
    val id: Long,
    val type: CalculationType,
    val title: String,
    val details: String,
    val timestamp: Long,
    val payloadJson: String = ""
)
