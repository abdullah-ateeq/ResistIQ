package com.example.resistiq.model

import java.math.BigDecimal

interface ElectricalUnit {
    val symbol: String
    val multiplier: BigDecimal
    
    fun toBase(value: BigDecimal): BigDecimal = value.multiply(multiplier)
    fun fromBase(baseValue: BigDecimal): BigDecimal = baseValue.divide(multiplier, 12, java.math.RoundingMode.HALF_UP).stripTrailingZeros()
}

enum class VoltageUnit(override val symbol: String, override val multiplier: BigDecimal) : ElectricalUnit {
    MILLIVOLT("mV", BigDecimal("0.001")),
    VOLT("V", BigDecimal("1")),
    KILOVOLT("kV", BigDecimal("1000"))
}

enum class CurrentUnit(override val symbol: String, override val multiplier: BigDecimal) : ElectricalUnit {
    MILLIAMPERE("mA", BigDecimal("0.001")),
    AMPERE("A", BigDecimal("1")),
    KILOAMPERE("kA", BigDecimal("1000"))
}

enum class ResistanceUnit(override val symbol: String, override val multiplier: BigDecimal) : ElectricalUnit {
    MILLIOHM("mΩ", BigDecimal("0.001")),
    OHM("Ω", BigDecimal("1")),
    KILOHM("kΩ", BigDecimal("1000")),
    MEGAOHM("MΩ", BigDecimal("1000000")),
    GIGAOHM("GΩ", BigDecimal("1000000000"))
}

enum class PowerUnit(override val symbol: String, override val multiplier: BigDecimal) : ElectricalUnit {
    MILLIWATT("mW", BigDecimal("0.001")),
    WATT("W", BigDecimal("1")),
    KILOWATT("kW", BigDecimal("1000"))
}

/**
 * Safe numeric parsing for user input. Accepts a comma as decimal separator and returns null
 * for anything that is not a plain finite number (no exponents, no NaN/Infinity).
 */
fun parseDecimalInput(input: String): BigDecimal? {
    val trimmed = input.trim().replace(" ", "")
    if (trimmed.isEmpty()) return null
    val normalized = if (!trimmed.contains('.') && trimmed.count { it == ',' } == 1) trimmed.replace(',', '.') else trimmed
    if (!normalized.matches(Regex("""[+-]?(\d+\.?\d*|\.\d+)"""))) return null
    return normalized.toBigDecimalOrNull()
}
