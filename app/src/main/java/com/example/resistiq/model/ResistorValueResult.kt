package com.example.resistiq.model

import java.math.BigDecimal

data class BandBreakdownItem(
    val bandPosition: String,
    /** Null when the position has no physical band (e.g. the implied ±20% of a 3-band resistor). */
    val color: ResistorColor?,
    val meaning: String
)

data class ResistorValueResult(
    val resistanceOhms: BigDecimal,
    val tolerancePercent: BigDecimal,
    val minimumOhms: BigDecimal,
    val maximumOhms: BigDecimal,
    val temperatureCoefficient: Int?,
    val bands: List<ResistorColor>,
    val breakdown: List<BandBreakdownItem> = emptyList(),
    val calculationFormula: String = "",
    val formattedResistance: String = "",
    val formattedTolerance: String = "",
    val formattedRange: String = "",
    val formattedTempCo: String? = null
)
