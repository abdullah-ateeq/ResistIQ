package com.example.resistiq.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

enum class ESeries(val seriesName: String, val count: Int) {
    E12("E12", 12),
    E24("E24", 24),
    E96("E96", 96)
}

data class PreferredValueMatch(
    val isExactMatch: Boolean,
    val series: ESeries,
    val nearestValue: BigDecimal,
    val percentageDifference: BigDecimal,
    val formattedStatus: String
)

/**
 * Preferred-value (IEC 60063 E-series) checks.
 *
 * Values are stored once per decade as normalized mantissas in [1, 10) and compared exactly
 * using BigDecimal, so 1040 Ω is *not* reported as an E24 value.
 */
object PreferredValueChecker {

    private fun decade(vararg values: String) = values.map { BigDecimal(it) }

    val e12Values = decade(
        "1.0", "1.2", "1.5", "1.8", "2.2", "2.7", "3.3", "3.9", "4.7", "5.6", "6.8", "8.2"
    )

    val e24Values = decade(
        "1.0", "1.1", "1.2", "1.3", "1.5", "1.6", "1.8", "2.0", "2.2", "2.4", "2.7", "3.0",
        "3.3", "3.6", "3.9", "4.3", "4.7", "5.1", "5.6", "6.2", "6.8", "7.5", "8.2", "9.1"
    )

    val e96Values = decade(
        "1.00", "1.02", "1.05", "1.07", "1.10", "1.13", "1.15", "1.18", "1.21", "1.24", "1.27", "1.30",
        "1.33", "1.37", "1.40", "1.43", "1.47", "1.50", "1.54", "1.58", "1.62", "1.65", "1.69", "1.74",
        "1.78", "1.82", "1.87", "1.91", "1.96", "2.00", "2.05", "2.10", "2.15", "2.21", "2.26", "2.32",
        "2.37", "2.43", "2.49", "2.55", "2.61", "2.67", "2.74", "2.80", "2.87", "2.94", "3.01", "3.09",
        "3.16", "3.24", "3.32", "3.40", "3.48", "3.57", "3.65", "3.74", "3.83", "3.92", "4.02", "4.12",
        "4.22", "4.32", "4.42", "4.53", "4.64", "4.75", "4.87", "4.99", "5.11", "5.23", "5.36", "5.49",
        "5.62", "5.76", "5.90", "6.04", "6.19", "6.34", "6.49", "6.65", "6.81", "6.98", "7.15", "7.32",
        "7.50", "7.68", "7.87", "8.06", "8.25", "8.45", "8.66", "8.87", "9.09", "9.31", "9.53", "9.76"
    )

    fun valuesFor(series: ESeries): List<BigDecimal> = when (series) {
        ESeries.E12 -> e12Values
        ESeries.E24 -> e24Values
        ESeries.E96 -> e96Values
    }

    /** Power-of-ten exponent so that value / 10^exp lies in [1, 10). */
    private fun decadeExponent(value: BigDecimal): Int {
        val stripped = value.stripTrailingZeros()
        return stripped.precision() - stripped.scale() - 1
    }

    fun isPreferredValue(resistanceOhms: BigDecimal, series: ESeries): Boolean {
        if (resistanceOhms.signum() <= 0) return false
        val mantissa = resistanceOhms.movePointLeft(decadeExponent(resistanceOhms))
        return valuesFor(series).any { it.compareTo(mantissa) == 0 }
    }

    fun findNearestPreferredValue(resistanceOhms: BigDecimal, series: ESeries = ESeries.E24): PreferredValueMatch {
        if (resistanceOhms.signum() <= 0) {
            return PreferredValueMatch(
                isExactMatch = false,
                series = series,
                nearestValue = BigDecimal.ZERO,
                percentageDifference = BigDecimal.ZERO,
                formattedStatus = "INVALID RESISTANCE"
            )
        }

        val exponent = decadeExponent(resistanceOhms)
        val values = valuesFor(series)
        // Candidates in this decade, plus the neighbouring decade edges (e.g. 9.76 → 10.0)
        val candidates = values.map { it.movePointRight(exponent) } +
            values.first().movePointRight(exponent + 1) +
            values.last().movePointRight(exponent - 1)

        val nearest = candidates.minBy { it.subtract(resistanceOhms).abs() }.stripTrailingZeros()
        val pctDiff = percentageDifference(nearest, resistanceOhms)
        val isExact = nearest.compareTo(resistanceOhms) == 0

        return PreferredValueMatch(
            isExactMatch = isExact,
            series = series,
            nearestValue = nearest,
            percentageDifference = pctDiff,
            formattedStatus = if (isExact) "${series.seriesName} STANDARD" else "NOT AN ${series.seriesName} VALUE"
        )
    }

    /** |actual − target| ÷ target × 100, rounded to two decimals for display. */
    fun percentageDifference(actual: BigDecimal, target: BigDecimal): BigDecimal {
        if (target.signum() == 0) return BigDecimal.ZERO
        return actual.subtract(target).abs()
            .multiply(BigDecimal("100"))
            .divide(target, MathContext.DECIMAL64)
            .setScale(2, RoundingMode.HALF_UP)
    }
}
