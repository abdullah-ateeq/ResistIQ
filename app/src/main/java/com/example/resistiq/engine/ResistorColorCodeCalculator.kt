package com.example.resistiq.engine

import com.example.resistiq.model.BandBreakdownItem
import com.example.resistiq.model.BandCount
import com.example.resistiq.model.ResistorColor
import com.example.resistiq.model.ResistorValueResult
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale

/**
 * Central resistor color-code engine (IEC 60062). All color ↔ value mappings live here;
 * UI code must not hardcode them.
 */
object ResistorColorCodeCalculator {

    /** Tolerance assumed when a resistor has no tolerance band (3-band). */
    val NO_BAND_TOLERANCE: BigDecimal = BigDecimal("20")

    val digitColors = listOf(
        ResistorColor.BLACK,
        ResistorColor.BROWN,
        ResistorColor.RED,
        ResistorColor.ORANGE,
        ResistorColor.YELLOW,
        ResistorColor.GREEN,
        ResistorColor.BLUE,
        ResistorColor.VIOLET,
        ResistorColor.GRAY,
        ResistorColor.WHITE
    )

    // Standard first digit excludes Black (starts at 1)
    val firstDigitColors = listOf(
        ResistorColor.BROWN,
        ResistorColor.RED,
        ResistorColor.ORANGE,
        ResistorColor.YELLOW,
        ResistorColor.GREEN,
        ResistorColor.BLUE,
        ResistorColor.VIOLET,
        ResistorColor.GRAY,
        ResistorColor.WHITE
    )

    val multiplierColors = listOf(
        ResistorColor.BLACK,
        ResistorColor.BROWN,
        ResistorColor.RED,
        ResistorColor.ORANGE,
        ResistorColor.YELLOW,
        ResistorColor.GREEN,
        ResistorColor.BLUE,
        ResistorColor.VIOLET,
        ResistorColor.GRAY,
        ResistorColor.WHITE,
        ResistorColor.GOLD,
        ResistorColor.SILVER
    )

    val toleranceColors = listOf(
        ResistorColor.BROWN,
        ResistorColor.RED,
        ResistorColor.GREEN,
        ResistorColor.BLUE,
        ResistorColor.VIOLET,
        ResistorColor.GRAY,
        ResistorColor.GOLD,
        ResistorColor.SILVER
    )

    val tempCoColors = listOf(
        ResistorColor.BROWN,
        ResistorColor.RED,
        ResistorColor.ORANGE,
        ResistorColor.YELLOW,
        ResistorColor.BLUE,
        ResistorColor.VIOLET,
        ResistorColor.GRAY
    )

    fun colorToDigit(color: ResistorColor): Int? {
        return when (color) {
            ResistorColor.BLACK -> 0
            ResistorColor.BROWN -> 1
            ResistorColor.RED -> 2
            ResistorColor.ORANGE -> 3
            ResistorColor.YELLOW -> 4
            ResistorColor.GREEN -> 5
            ResistorColor.BLUE -> 6
            ResistorColor.VIOLET -> 7
            ResistorColor.GRAY -> 8
            ResistorColor.WHITE -> 9
            ResistorColor.GOLD, ResistorColor.SILVER -> null
        }
    }

    fun digitToColor(digit: Int): ResistorColor? {
        return when (digit) {
            0 -> ResistorColor.BLACK
            1 -> ResistorColor.BROWN
            2 -> ResistorColor.RED
            3 -> ResistorColor.ORANGE
            4 -> ResistorColor.YELLOW
            5 -> ResistorColor.GREEN
            6 -> ResistorColor.BLUE
            7 -> ResistorColor.VIOLET
            8 -> ResistorColor.GRAY
            9 -> ResistorColor.WHITE
            else -> null
        }
    }

    fun colorToMultiplier(color: ResistorColor): BigDecimal? {
        return when (color) {
            ResistorColor.BLACK -> BigDecimal("1")
            ResistorColor.BROWN -> BigDecimal("10")
            ResistorColor.RED -> BigDecimal("100")
            ResistorColor.ORANGE -> BigDecimal("1000")
            ResistorColor.YELLOW -> BigDecimal("10000")
            ResistorColor.GREEN -> BigDecimal("100000")
            ResistorColor.BLUE -> BigDecimal("1000000")
            ResistorColor.VIOLET -> BigDecimal("10000000")
            ResistorColor.GRAY -> BigDecimal("100000000")
            ResistorColor.WHITE -> BigDecimal("1000000000")
            ResistorColor.GOLD -> BigDecimal("0.1")
            ResistorColor.SILVER -> BigDecimal("0.01")
        }
    }

    fun multiplierToColor(multiplier: BigDecimal): ResistorColor? {
        val normalized = multiplier.stripTrailingZeros()
        return multiplierColors.firstOrNull { color ->
            colorToMultiplier(color)?.stripTrailingZeros()?.compareTo(normalized) == 0
        }
    }

    fun colorToTolerance(color: ResistorColor): BigDecimal? {
        return when (color) {
            ResistorColor.BROWN -> BigDecimal("1")
            ResistorColor.RED -> BigDecimal("2")
            ResistorColor.GREEN -> BigDecimal("0.5")
            ResistorColor.BLUE -> BigDecimal("0.25")
            ResistorColor.VIOLET -> BigDecimal("0.1")
            ResistorColor.GRAY -> BigDecimal("0.05")
            ResistorColor.GOLD -> BigDecimal("5")
            ResistorColor.SILVER -> BigDecimal("10")
            else -> null
        }
    }

    fun toleranceToColor(tolerance: BigDecimal): ResistorColor? {
        val normalized = tolerance.stripTrailingZeros()
        return toleranceColors.firstOrNull { color ->
            colorToTolerance(color)?.stripTrailingZeros()?.compareTo(normalized) == 0
        }
    }

    fun colorToTempCo(color: ResistorColor): Int? {
        return when (color) {
            ResistorColor.BROWN -> 100
            ResistorColor.RED -> 50
            ResistorColor.ORANGE -> 15
            ResistorColor.YELLOW -> 25
            ResistorColor.BLUE -> 10
            ResistorColor.VIOLET -> 5
            ResistorColor.GRAY -> 1
            else -> null
        }
    }

    fun tempCoToColor(ppm: Int): ResistorColor? {
        return tempCoColors.firstOrNull { color ->
            colorToTempCo(color) == ppm
        }
    }

    fun getValidColorsForBand(bandCount: BandCount, bandIndex: Int): List<ResistorColor> {
        return when (bandCount) {
            BandCount.THREE -> when (bandIndex) {
                0 -> firstDigitColors
                1 -> digitColors
                2 -> multiplierColors
                else -> emptyList()
            }
            BandCount.FOUR -> when (bandIndex) {
                0 -> firstDigitColors
                1 -> digitColors
                2 -> multiplierColors
                3 -> toleranceColors
                else -> emptyList()
            }
            BandCount.FIVE -> when (bandIndex) {
                0 -> firstDigitColors
                1 -> digitColors
                2 -> digitColors
                3 -> multiplierColors
                4 -> toleranceColors
                else -> emptyList()
            }
            BandCount.SIX -> when (bandIndex) {
                0 -> firstDigitColors
                1 -> digitColors
                2 -> digitColors
                3 -> multiplierColors
                4 -> toleranceColors
                5 -> tempCoColors
                else -> emptyList()
            }
        }
    }

    fun getDefaultBands(bandCount: BandCount): List<ResistorColor> {
        return when (bandCount) {
            BandCount.THREE -> listOf(ResistorColor.BROWN, ResistorColor.BLACK, ResistorColor.RED)
            BandCount.FOUR -> listOf(ResistorColor.BROWN, ResistorColor.BLACK, ResistorColor.RED, ResistorColor.GOLD)
            BandCount.FIVE -> listOf(ResistorColor.BROWN, ResistorColor.BLACK, ResistorColor.BLACK, ResistorColor.BROWN, ResistorColor.BROWN)
            BandCount.SIX -> listOf(ResistorColor.BROWN, ResistorColor.BLACK, ResistorColor.BLACK, ResistorColor.BROWN, ResistorColor.BROWN, ResistorColor.RED)
        }
    }

    private fun digitCount(bandCount: BandCount) =
        if (bandCount == BandCount.FIVE || bandCount == BandCount.SIX) 3 else 2

    /**
     * Converts a band selection to another band count by role (digits, multiplier, tolerance,
     * temp. coefficient). The nominal value is preserved whenever the color code allows it;
     * missing roles get standard defaults so no invalid selection survives.
     */
    fun convertBands(bands: List<ResistorColor>, from: BandCount, to: BandCount): List<ResistorColor> {
        if (from == to) return bands.take(to.count)
        val fromDigits = digitCount(from)
        val toDigits = digitCount(to)
        var digits = bands.take(fromDigits)
        var multiplier = bands.getOrElse(fromDigits) { ResistorColor.RED }
        val tolerance = if (from != BandCount.THREE) bands.getOrNull(fromDigits + 1) else null
        val tempCo = if (from == BandCount.SIX) bands.getOrNull(5) else null

        val multValue = colorToMultiplier(multiplier) ?: BigDecimal.ONE
        if (toDigits > fromDigits) {
            // e.g. 47 × 100 → 470 × 10
            val shifted = multiplierToColor(multValue.divide(BigDecimal.TEN))
            if (shifted != null) {
                digits = digits + ResistorColor.BLACK
                multiplier = shifted
            } else {
                digits = digits + ResistorColor.BLACK
            }
        } else if (toDigits < fromDigits) {
            // e.g. 100 × 10 → 10 × 100 when the dropped digit is zero
            val shifted = multiplierToColor(multValue.multiply(BigDecimal.TEN))
            if (digits.getOrNull(2) == ResistorColor.BLACK && shifted != null) {
                multiplier = shifted
            }
            digits = digits.take(2)
        }

        val result = mutableListOf<ResistorColor>()
        result += digits
        result += multiplier
        if (to != BandCount.THREE) {
            result += tolerance ?: if (to == BandCount.FOUR) ResistorColor.GOLD else ResistorColor.BROWN
        }
        if (to == BandCount.SIX) {
            result += tempCo ?: ResistorColor.RED
        }
        return result.mapIndexed { index, color ->
            val valid = getValidColorsForBand(to, index)
            if (color in valid) color else valid.first()
        }
    }

    fun decodeBands(
        bands: List<ResistorColor>,
        bandCount: BandCount,
        decimalPlaces: Int = 2
    ): ResistorValueResult {
        val breakdown = mutableListOf<BandBreakdownItem>()
        val significantDigits: BigDecimal
        val multiplier: BigDecimal
        val tolerancePercent: BigDecimal
        var tempCo: Int? = null

        when (bandCount) {
            BandCount.THREE -> {
                val d1 = colorToDigit(bands.getOrElse(0) { ResistorColor.BROWN }) ?: 1
                val d2 = colorToDigit(bands.getOrElse(1) { ResistorColor.BLACK }) ?: 0
                val multColor = bands.getOrElse(2) { ResistorColor.RED }
                multiplier = colorToMultiplier(multColor) ?: BigDecimal.ONE
                tolerancePercent = NO_BAND_TOLERANCE

                significantDigits = BigDecimal((d1 * 10 + d2).toString())
                breakdown.add(BandBreakdownItem("Band 1", bands[0], "$d1"))
                breakdown.add(BandBreakdownItem("Band 2", bands[1], "$d2"))
                breakdown.add(BandBreakdownItem("Multiplier", multColor, "×${formatMultiplier(multiplier)}"))
                breakdown.add(BandBreakdownItem("Tolerance", null, "±20% (no band)"))
            }
            BandCount.FOUR -> {
                val d1 = colorToDigit(bands.getOrElse(0) { ResistorColor.BROWN }) ?: 1
                val d2 = colorToDigit(bands.getOrElse(1) { ResistorColor.BLACK }) ?: 0
                val multColor = bands.getOrElse(2) { ResistorColor.RED }
                multiplier = colorToMultiplier(multColor) ?: BigDecimal.ONE
                val tolColor = bands.getOrElse(3) { ResistorColor.GOLD }
                tolerancePercent = colorToTolerance(tolColor) ?: BigDecimal("5")

                significantDigits = BigDecimal((d1 * 10 + d2).toString())
                breakdown.add(BandBreakdownItem("Band 1", bands[0], "$d1"))
                breakdown.add(BandBreakdownItem("Band 2", bands[1], "$d2"))
                breakdown.add(BandBreakdownItem("Multiplier", multColor, "×${formatMultiplier(multiplier)}"))
                breakdown.add(BandBreakdownItem("Tolerance", tolColor, "±${formatDecimal(tolerancePercent)}%"))
            }
            BandCount.FIVE -> {
                val d1 = colorToDigit(bands.getOrElse(0) { ResistorColor.BROWN }) ?: 1
                val d2 = colorToDigit(bands.getOrElse(1) { ResistorColor.BLACK }) ?: 0
                val d3 = colorToDigit(bands.getOrElse(2) { ResistorColor.BLACK }) ?: 0
                val multColor = bands.getOrElse(3) { ResistorColor.BROWN }
                multiplier = colorToMultiplier(multColor) ?: BigDecimal.ONE
                val tolColor = bands.getOrElse(4) { ResistorColor.BROWN }
                tolerancePercent = colorToTolerance(tolColor) ?: BigDecimal("1")

                significantDigits = BigDecimal((d1 * 100 + d2 * 10 + d3).toString())
                breakdown.add(BandBreakdownItem("Band 1", bands[0], "$d1"))
                breakdown.add(BandBreakdownItem("Band 2", bands[1], "$d2"))
                breakdown.add(BandBreakdownItem("Band 3", bands[2], "$d3"))
                breakdown.add(BandBreakdownItem("Multiplier", multColor, "×${formatMultiplier(multiplier)}"))
                breakdown.add(BandBreakdownItem("Tolerance", tolColor, "±${formatDecimal(tolerancePercent)}%"))
            }
            BandCount.SIX -> {
                val d1 = colorToDigit(bands.getOrElse(0) { ResistorColor.BROWN }) ?: 1
                val d2 = colorToDigit(bands.getOrElse(1) { ResistorColor.BLACK }) ?: 0
                val d3 = colorToDigit(bands.getOrElse(2) { ResistorColor.BLACK }) ?: 0
                val multColor = bands.getOrElse(3) { ResistorColor.BROWN }
                multiplier = colorToMultiplier(multColor) ?: BigDecimal.ONE
                val tolColor = bands.getOrElse(4) { ResistorColor.BROWN }
                tolerancePercent = colorToTolerance(tolColor) ?: BigDecimal("1")
                val tcColor = bands.getOrElse(5) { ResistorColor.RED }
                tempCo = colorToTempCo(tcColor) ?: 50

                significantDigits = BigDecimal((d1 * 100 + d2 * 10 + d3).toString())
                breakdown.add(BandBreakdownItem("Band 1", bands[0], "$d1"))
                breakdown.add(BandBreakdownItem("Band 2", bands[1], "$d2"))
                breakdown.add(BandBreakdownItem("Band 3", bands[2], "$d3"))
                breakdown.add(BandBreakdownItem("Multiplier", multColor, "×${formatMultiplier(multiplier)}"))
                breakdown.add(BandBreakdownItem("Tolerance", tolColor, "±${formatDecimal(tolerancePercent)}%"))
                breakdown.add(BandBreakdownItem("Temp. Coeff.", tcColor, "$tempCo ppm/°C"))
            }
        }

        val nominalResistance = significantDigits.multiply(multiplier).stripTrailingZeros()
        val toleranceFactor = tolerancePercent.divide(BigDecimal("100"), MathContext.DECIMAL128)
        val delta = nominalResistance.multiply(toleranceFactor)
        val minimum = nominalResistance.subtract(delta).stripTrailingZeros()
        val maximum = nominalResistance.add(delta).stripTrailingZeros()

        val formula = "$significantDigits × ${formatMultiplier(multiplier)} = ${formatOhmsPlain(nominalResistance)} Ω"
        val formattedRes = formatResistance(nominalResistance, decimalPlaces)
        val formattedTol = "±${formatDecimal(tolerancePercent)}%"
        val formattedRange = "${formatResistancePrecise(minimum, decimalPlaces)} – ${formatResistancePrecise(maximum, decimalPlaces)}"
        val formattedTempCo = tempCo?.let { "$it ppm/°C" }

        return ResistorValueResult(
            resistanceOhms = nominalResistance,
            tolerancePercent = tolerancePercent,
            minimumOhms = minimum,
            maximumOhms = maximum,
            temperatureCoefficient = tempCo,
            bands = bands.take(bandCount.count),
            breakdown = breakdown,
            calculationFormula = formula,
            formattedResistance = formattedRes,
            formattedTolerance = formattedTol,
            formattedRange = formattedRange,
            formattedTempCo = formattedTempCo
        )
    }

    data class ReverseLookupResult(
        val isExact: Boolean,
        val bands: List<ResistorColor>,
        val exactResistance: BigDecimal,
        val targetResistance: BigDecimal,
        val percentageDifference: BigDecimal,
        val errorMessage: String? = null
    )

    fun encodeResistanceToBands(
        resistanceOhms: BigDecimal,
        bandCount: BandCount,
        tolerancePercent: BigDecimal = BigDecimal("5"),
        tempCoPpm: Int = 50
    ): ReverseLookupResult {
        if (resistanceOhms <= BigDecimal.ZERO) {
            return ReverseLookupResult(
                isExact = false,
                bands = emptyList(),
                exactResistance = BigDecimal.ZERO,
                targetResistance = resistanceOhms,
                percentageDifference = BigDecimal.ZERO,
                errorMessage = "Resistance must be greater than zero."
            )
        }

        val tolBand: ResistorColor? = if (bandCount == BandCount.THREE) null else toleranceToColor(tolerancePercent)
        if (bandCount != BandCount.THREE && tolBand == null) {
            return ReverseLookupResult(
                isExact = false,
                bands = emptyList(),
                exactResistance = BigDecimal.ZERO,
                targetResistance = resistanceOhms,
                percentageDifference = BigDecimal.ZERO,
                errorMessage = "±${formatDecimal(tolerancePercent)}% has no tolerance band color. Use a 3-band resistor for ±20%."
            )
        }
        val tcBand: ResistorColor? = if (bandCount == BandCount.SIX) {
            tempCoToColor(tempCoPpm) ?: return ReverseLookupResult(
                isExact = false,
                bands = emptyList(),
                exactResistance = BigDecimal.ZERO,
                targetResistance = resistanceOhms,
                percentageDifference = BigDecimal.ZERO,
                errorMessage = "$tempCoPpm ppm/°C has no temperature-coefficient color."
            )
        } else null

        val numDigits = if (bandCount == BandCount.FIVE || bandCount == BandCount.SIX) 3 else 2
        val minRepresentable = if (numDigits == 2) BigDecimal("0.10") else BigDecimal("1.00")
        val maxRepresentable = if (numDigits == 2) BigDecimal("99000000000") else BigDecimal("999000000000")

        if (resistanceOhms < minRepresentable || resistanceOhms > maxRepresentable) {
            return ReverseLookupResult(
                isExact = false,
                bands = emptyList(),
                exactResistance = BigDecimal.ZERO,
                targetResistance = resistanceOhms,
                percentageDifference = BigDecimal.ZERO,
                errorMessage = "Value is outside the range a ${bandCount.count}-band color code can represent " +
                    "(${formatResistance(minRepresentable)} – ${formatResistance(maxRepresentable)})."
            )
        }

        // Search through valid multiplier colors to find the best match
        var bestBands: List<ResistorColor>? = null
        var bestResistance: BigDecimal? = null
        var minDiff = BigDecimal(Double.MAX_VALUE.toString())

        for (multColor in multiplierColors) {
            val mult = colorToMultiplier(multColor) ?: continue
            val rawDigits = resistanceOhms.divide(mult, 8, RoundingMode.HALF_UP)
            val roundedDigits = rawDigits.setScale(0, RoundingMode.HALF_UP)

            val minDigitsVal = if (numDigits == 2) BigDecimal("10") else BigDecimal("100")
            val maxDigitsVal = if (numDigits == 2) BigDecimal("99") else BigDecimal("999")

            if (roundedDigits in minDigitsVal..maxDigitsVal) {
                val candidateResistance = roundedDigits.multiply(mult).stripTrailingZeros()
                val diff = candidateResistance.subtract(resistanceOhms).abs()

                if (diff < minDiff) {
                    minDiff = diff
                    bestResistance = candidateResistance

                    val digitsInt = roundedDigits.toInt()
                    val digitsStr = digitsInt.toString()
                    val digitBands = digitsStr.mapNotNull { ch -> digitToColor(ch.digitToInt()) }

                    val resultList = mutableListOf<ResistorColor>()
                    resultList.addAll(digitBands)
                    resultList.add(multColor)
                    tolBand?.let { resultList.add(it) }
                    tcBand?.let { resultList.add(it) }
                    bestBands = resultList

                    if (diff.compareTo(BigDecimal.ZERO) == 0) {
                        break
                    }
                }
            }
        }

        if (bestBands != null && bestResistance != null) {
            val isExact = minDiff.compareTo(BigDecimal.ZERO) == 0
            val pctDiff = if (isExact) BigDecimal.ZERO else {
                minDiff.multiply(BigDecimal("100"))
                    .divide(resistanceOhms, 2, RoundingMode.HALF_UP)
            }

            return ReverseLookupResult(
                isExact = isExact,
                bands = bestBands,
                exactResistance = bestResistance,
                targetResistance = resistanceOhms,
                percentageDifference = pctDiff,
                errorMessage = if (isExact) null else "Exact color-code representation unavailable."
            )
        }

        return ReverseLookupResult(
            isExact = false,
            bands = emptyList(),
            exactResistance = BigDecimal.ZERO,
            targetResistance = resistanceOhms,
            percentageDifference = BigDecimal.ZERO,
            errorMessage = "Exact color-code representation unavailable."
        )
    }

    fun formatResistance(ohms: BigDecimal, decimalPlaces: Int = 2): String {
        val strip = ohms.stripTrailingZeros()
        val giga = BigDecimal("1000000000")
        val mega = BigDecimal("1000000")
        val kilo = BigDecimal("1000")

        return when {
            strip >= giga -> {
                val scaled = strip.divide(giga, decimalPlaces, RoundingMode.HALF_UP)
                "${scaled.setScale(decimalPlaces, RoundingMode.HALF_UP).toPlainString()} GΩ"
            }
            strip >= mega -> {
                val scaled = strip.divide(mega, decimalPlaces, RoundingMode.HALF_UP)
                "${scaled.setScale(decimalPlaces, RoundingMode.HALF_UP).toPlainString()} MΩ"
            }
            strip >= kilo -> {
                val scaled = strip.divide(kilo, decimalPlaces, RoundingMode.HALF_UP)
                "${scaled.setScale(decimalPlaces, RoundingMode.HALF_UP).toPlainString()} kΩ"
            }
            else -> {
                if (strip.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
                    "${strip.toBigInteger()} Ω"
                } else {
                    val scaled = strip.setScale(decimalPlaces, RoundingMode.HALF_UP)
                    "${scaled.toPlainString()} Ω"
                }
            }
        }
    }

    /**
     * Like [formatResistance] but keeps up to [maxDecimals] significant decimals when needed,
     * so tolerance limits such as 4.465 kΩ are not hidden by display rounding.
     */
    fun formatResistancePrecise(ohms: BigDecimal, minDecimals: Int = 2, maxDecimals: Int = 3): String {
        val (scaled, unit) = scaleToUnit(ohms)
        var value = scaled.setScale(maxOf(minDecimals, maxDecimals), RoundingMode.HALF_UP).stripTrailingZeros()
        if (unit == "Ω" && value.scale() <= 0) return "${value.toBigInteger()} Ω"
        if (value.scale() < minDecimals) value = value.setScale(minDecimals)
        return "${value.toPlainString()} $unit"
    }

    /** Spoken form for accessibility, e.g. "4.7 kilohms". */
    fun spokenResistance(ohms: BigDecimal): String {
        val (scaled, unit) = scaleToUnit(ohms)
        val word = when (unit) {
            "GΩ" -> "gigaohms"
            "MΩ" -> "megaohms"
            "kΩ" -> "kilohms"
            else -> "ohms"
        }
        return "${scaled.setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()} $word"
    }

    private fun scaleToUnit(ohms: BigDecimal): Pair<BigDecimal, String> {
        val strip = ohms.stripTrailingZeros()
        return when {
            strip >= BigDecimal("1000000000") -> strip.movePointLeft(9) to "GΩ"
            strip >= BigDecimal("1000000") -> strip.movePointLeft(6) to "MΩ"
            strip >= BigDecimal("1000") -> strip.movePointLeft(3) to "kΩ"
            else -> strip to "Ω"
        }
    }

    private fun formatOhmsPlain(ohms: BigDecimal): String {
        val strip = ohms.stripTrailingZeros()
        return if (strip.scale() <= 0) String.format(Locale.US, "%,d", strip.toBigInteger()) else strip.toPlainString()
    }

    fun formatMultiplier(multiplier: BigDecimal): String {
        val strip = multiplier.stripTrailingZeros()
        return if (strip.scale() > 0) strip.toPlainString() else String.format(Locale.US, "%,d", strip.toBigInteger())
    }

    /** Human-readable meaning of a color in a given band position, e.g. "4", "×100", "±5%", "50 ppm/°C". */
    fun describeBand(bandCount: BandCount, bandIndex: Int, color: ResistorColor): String {
        return when (bandRole(bandCount, bandIndex)) {
            BandRole.DIGIT -> colorToDigit(color)?.toString().orEmpty()
            BandRole.MULTIPLIER -> colorToMultiplier(color)?.let { "×${formatMultiplier(it)}" }.orEmpty()
            BandRole.TOLERANCE -> colorToTolerance(color)?.let { "±${formatDecimal(it)}%" }.orEmpty()
            BandRole.TEMP_COEFFICIENT -> colorToTempCo(color)?.let { "$it ppm/°C" }.orEmpty()
        }
    }

    enum class BandRole(val label: String) {
        DIGIT("Digit"),
        MULTIPLIER("Multiplier"),
        TOLERANCE("Tolerance"),
        TEMP_COEFFICIENT("Temp. Coefficient")
    }

    fun bandRole(bandCount: BandCount, bandIndex: Int): BandRole {
        val digits = if (bandCount == BandCount.FIVE || bandCount == BandCount.SIX) 3 else 2
        return when {
            bandIndex < digits -> BandRole.DIGIT
            bandIndex == digits -> BandRole.MULTIPLIER
            bandIndex == digits + 1 -> BandRole.TOLERANCE
            else -> BandRole.TEMP_COEFFICIENT
        }
    }

    private fun formatDecimal(value: BigDecimal): String {
        return value.stripTrailingZeros().toPlainString()
    }
}
