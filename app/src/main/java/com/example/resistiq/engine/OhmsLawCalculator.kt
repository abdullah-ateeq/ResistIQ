package com.example.resistiq.engine

import com.example.resistiq.model.ElectricalParameter
import com.example.resistiq.model.OhmsLawResult
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

object OhmsLawCalculator {

    private val MC = MathContext(34, RoundingMode.HALF_UP)

    fun bigDecimalSqrt(value: BigDecimal): BigDecimal {
        if (value < BigDecimal.ZERO) {
            throw IllegalArgumentException("Cannot compute square root of negative number: $value")
        }
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO
        }
        
        // Initial estimate from Double
        val doubleVal = value.toDouble()
        if (doubleVal.isInfinite()) {
            throw ArithmeticException("Value too large for square root calculation")
        }
        var x = BigDecimal(kotlin.math.sqrt(doubleVal), MC)
        val two = BigDecimal("2")

        // 10 Newton-Raphson iterations for 34 decimal places
        for (i in 0 until 12) {
            val nextX = x.add(value.divide(x, MC)).divide(two, MC)
            if (nextX.compareTo(x) == 0) break
            x = nextX
        }
        return x.stripTrailingZeros()
    }

    const val ERROR_NEGATIVE = "Value cannot be negative."
    const val ERROR_NOT_POSITIVE = "Value must be greater than zero for this calculation."

    private fun validateInputs(p1: BigDecimal, p2: BigDecimal) {
        if (p1.signum() < 0 || p2.signum() < 0) {
            throw IllegalArgumentException(ERROR_NEGATIVE)
        }
    }

    private fun requirePositive(value: BigDecimal) {
        if (value.signum() == 0) {
            throw IllegalArgumentException(ERROR_NOT_POSITIVE)
        }
    }

    fun calculateFromVoltageCurrent(voltage: BigDecimal, current: BigDecimal): OhmsLawResult {
        validateInputs(voltage, current)
        // R = V ÷ I is undefined when I = 0 (including the indeterminate 0 ÷ 0 case)
        requirePositive(current)

        val resistance = voltage.divide(current, MC).stripTrailingZeros()
        val power = voltage.multiply(current, MC).stripTrailingZeros()

        return OhmsLawResult(
            voltage = voltage.stripTrailingZeros(),
            current = current.stripTrailingZeros(),
            resistance = resistance,
            power = power,
            inputParameters = setOf(ElectricalParameter.VOLTAGE, ElectricalParameter.CURRENT),
            formulas = listOf("R = V ÷ I", "P = V × I")
        )
    }

    fun calculateFromVoltageResistance(voltage: BigDecimal, resistance: BigDecimal): OhmsLawResult {
        validateInputs(voltage, resistance)
        requirePositive(resistance)

        val current = voltage.divide(resistance, MC).stripTrailingZeros()
        val power = voltage.multiply(voltage, MC).divide(resistance, MC).stripTrailingZeros()

        return OhmsLawResult(
            voltage = voltage.stripTrailingZeros(),
            current = current,
            resistance = resistance.stripTrailingZeros(),
            power = power,
            inputParameters = setOf(ElectricalParameter.VOLTAGE, ElectricalParameter.RESISTANCE),
            formulas = listOf("I = V ÷ R", "P = V² ÷ R")
        )
    }

    fun calculateFromVoltagePower(voltage: BigDecimal, power: BigDecimal): OhmsLawResult {
        validateInputs(voltage, power)
        // I = P ÷ V needs V > 0; R = V² ÷ P needs P > 0
        requirePositive(voltage)
        requirePositive(power)

        val current = power.divide(voltage, MC).stripTrailingZeros()
        val resistance = voltage.multiply(voltage, MC).divide(power, MC).stripTrailingZeros()

        return OhmsLawResult(
            voltage = voltage.stripTrailingZeros(),
            current = current,
            resistance = resistance,
            power = power.stripTrailingZeros(),
            inputParameters = setOf(ElectricalParameter.VOLTAGE, ElectricalParameter.POWER),
            formulas = listOf("I = P ÷ V", "R = V² ÷ P")
        )
    }

    fun calculateFromCurrentResistance(current: BigDecimal, resistance: BigDecimal): OhmsLawResult {
        validateInputs(current, resistance)

        val voltage = current.multiply(resistance, MC).stripTrailingZeros()
        val power = current.multiply(current, MC).multiply(resistance, MC).stripTrailingZeros()

        return OhmsLawResult(
            voltage = voltage,
            current = current.stripTrailingZeros(),
            resistance = resistance.stripTrailingZeros(),
            power = power,
            inputParameters = setOf(ElectricalParameter.CURRENT, ElectricalParameter.RESISTANCE),
            formulas = listOf("V = I × R", "P = I² × R")
        )
    }

    fun calculateFromCurrentPower(current: BigDecimal, power: BigDecimal): OhmsLawResult {
        validateInputs(current, power)
        // V = P ÷ I and R = P ÷ I² need I > 0; P = 0 is valid (V = 0, R = 0)
        requirePositive(current)

        val voltage = power.divide(current, MC).stripTrailingZeros()
        val iSquared = current.multiply(current, MC)
        val resistance = power.divide(iSquared, MC).stripTrailingZeros()

        return OhmsLawResult(
            voltage = voltage,
            current = current.stripTrailingZeros(),
            resistance = resistance,
            power = power.stripTrailingZeros(),
            inputParameters = setOf(ElectricalParameter.CURRENT, ElectricalParameter.POWER),
            formulas = listOf("V = P ÷ I", "R = P ÷ I²")
        )
    }

    fun calculateFromResistancePower(resistance: BigDecimal, power: BigDecimal): OhmsLawResult {
        validateInputs(resistance, power)
        requirePositive(resistance)

        val pTimesR = power.multiply(resistance, MC)
        val voltage = bigDecimalSqrt(pTimesR).stripTrailingZeros()

        val pDivR = power.divide(resistance, MC)
        val current = bigDecimalSqrt(pDivR).stripTrailingZeros()

        return OhmsLawResult(
            voltage = voltage,
            current = current,
            resistance = resistance.stripTrailingZeros(),
            power = power.stripTrailingZeros(),
            inputParameters = setOf(ElectricalParameter.RESISTANCE, ElectricalParameter.POWER),
            formulas = listOf("V = √(P × R)", "I = √(P ÷ R)")
        )
    }

    fun calculate(
        p1: ElectricalParameter,
        v1: BigDecimal,
        p2: ElectricalParameter,
        v2: BigDecimal
    ): OhmsLawResult {
        return when {
            (p1 == ElectricalParameter.VOLTAGE && p2 == ElectricalParameter.CURRENT) ->
                calculateFromVoltageCurrent(v1, v2)
            (p1 == ElectricalParameter.CURRENT && p2 == ElectricalParameter.VOLTAGE) ->
                calculateFromVoltageCurrent(v2, v1)

            (p1 == ElectricalParameter.VOLTAGE && p2 == ElectricalParameter.RESISTANCE) ->
                calculateFromVoltageResistance(v1, v2)
            (p1 == ElectricalParameter.RESISTANCE && p2 == ElectricalParameter.VOLTAGE) ->
                calculateFromVoltageResistance(v2, v1)

            (p1 == ElectricalParameter.VOLTAGE && p2 == ElectricalParameter.POWER) ->
                calculateFromVoltagePower(v1, v2)
            (p1 == ElectricalParameter.POWER && p2 == ElectricalParameter.VOLTAGE) ->
                calculateFromVoltagePower(v2, v1)

            (p1 == ElectricalParameter.CURRENT && p2 == ElectricalParameter.RESISTANCE) ->
                calculateFromCurrentResistance(v1, v2)
            (p1 == ElectricalParameter.RESISTANCE && p2 == ElectricalParameter.CURRENT) ->
                calculateFromCurrentResistance(v2, v1)

            (p1 == ElectricalParameter.CURRENT && p2 == ElectricalParameter.POWER) ->
                calculateFromCurrentPower(v1, v2)
            (p1 == ElectricalParameter.POWER && p2 == ElectricalParameter.CURRENT) ->
                calculateFromCurrentPower(v2, v1)

            (p1 == ElectricalParameter.RESISTANCE && p2 == ElectricalParameter.POWER) ->
                calculateFromResistancePower(v1, v2)
            (p1 == ElectricalParameter.POWER && p2 == ElectricalParameter.RESISTANCE) ->
                calculateFromResistancePower(v2, v1)

            else -> throw IllegalArgumentException("Select two distinct parameters for Ohm's Law.")
        }
    }

    /** Fixed-decimal display in base units, e.g. "12.00 V". Rounding happens here only. */
    fun formatValue(value: BigDecimal, parameter: ElectricalParameter, decimalPlaces: Int = 2): String {
        val scaled = value.setScale(decimalPlaces, RoundingMode.HALF_UP)
        return "${scaled.toPlainString()} ${parameter.standardUnit}"
    }
}
