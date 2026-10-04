package com.example.resistiq.model

import java.math.BigDecimal

data class OhmsLawResult(
    val voltage: BigDecimal,
    val current: BigDecimal,
    val resistance: BigDecimal,
    val power: BigDecimal,
    val inputParameters: Set<ElectricalParameter>,
    val formulas: List<String>
)
