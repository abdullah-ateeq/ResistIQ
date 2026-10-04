package com.example.resistiq.model

enum class ElectricalParameter(
    val displayName: String,
    val symbol: String,
    val standardUnit: String
) {
    VOLTAGE("Voltage", "V", "V"),
    CURRENT("Current", "I", "A"),
    RESISTANCE("Resistance", "R", "Ω"),
    POWER("Power", "P", "W")
}
