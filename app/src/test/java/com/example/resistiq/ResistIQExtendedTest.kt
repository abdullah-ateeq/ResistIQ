package com.example.resistiq

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.example.resistiq.data.HistorySerializer
import com.example.resistiq.data.SettingsMapper
import com.example.resistiq.engine.ESeries
import com.example.resistiq.engine.OhmsLawCalculator
import com.example.resistiq.engine.PreferredValueChecker
import com.example.resistiq.engine.ResistorColorCodeCalculator
import com.example.resistiq.model.AppSettings
import com.example.resistiq.model.BandCount
import com.example.resistiq.model.CalculationType
import com.example.resistiq.model.CalculatorTab
import com.example.resistiq.model.ElectricalParameter
import com.example.resistiq.model.ResistorColor
import com.example.resistiq.model.ResistorColor.BLACK
import com.example.resistiq.model.ResistorColor.BROWN
import com.example.resistiq.model.ResistorColor.GOLD
import com.example.resistiq.model.ResistorColor.RED
import com.example.resistiq.model.ResistorColor.VIOLET
import com.example.resistiq.model.ResistorColor.YELLOW
import com.example.resistiq.model.ResistorHistoryItem
import com.example.resistiq.model.ThemeMode
import com.example.resistiq.model.parseDecimalInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigDecimal

class ResistIQExtendedTest {

    private fun bd(v: String) = BigDecimal(v)

    private fun assertBd(expected: String, actual: BigDecimal) =
        assertEquals("expected $expected but was $actual", 0, bd(expected).compareTo(actual))

    private fun expectError(message: String, block: () -> Unit) {
        try {
            block()
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals(message, e.message)
        }
    }

    // --- Color code ---

    @Test
    fun displayedRangeKeepsToleranceLimitPrecision() {
        val r = ResistorColorCodeCalculator.decodeBands(listOf(YELLOW, VIOLET, RED, GOLD), BandCount.FOUR, 2)
        assertEquals("4.70 kΩ", r.formattedResistance)
        assertEquals("4.465 kΩ – 4.935 kΩ", r.formattedRange)
        assertEquals("47 × 100 = 4,700 Ω", r.calculationFormula)

        val oneK = ResistorColorCodeCalculator.decodeBands(listOf(BROWN, BLACK, RED, GOLD), BandCount.FOUR, 2)
        assertEquals("950 Ω – 1.05 kΩ", oneK.formattedRange)
    }

    @Test
    fun threeBandHasNoFakeToleranceBand() {
        val r = ResistorColorCodeCalculator.decodeBands(listOf(BROWN, BLACK, RED), BandCount.THREE)
        assertBd("20", r.tolerancePercent)
        assertNull(r.breakdown.last().color)
        assertEquals(3, r.bands.size)
    }

    @Test
    fun goldAndSilverMultipliersUseDecimals() {
        val r = ResistorColorCodeCalculator.decodeBands(listOf(YELLOW, VIOLET, GOLD, GOLD), BandCount.FOUR)
        assertBd("4.7", r.resistanceOhms)
        assertEquals("4.70 Ω", r.formattedResistance)
    }

    @Test
    fun bandConversionPreservesValue() {
        val five = ResistorColorCodeCalculator.convertBands(listOf(BROWN, BLACK, RED, GOLD), BandCount.FOUR, BandCount.FIVE)
        assertEquals(listOf(BROWN, BLACK, BLACK, BROWN, GOLD), five)
        assertBd("1000", ResistorColorCodeCalculator.decodeBands(five, BandCount.FIVE).resistanceOhms)

        val four = ResistorColorCodeCalculator.convertBands(listOf(BROWN, BLACK, BLACK, BROWN, BROWN), BandCount.FIVE, BandCount.FOUR)
        assertEquals(listOf(BROWN, BLACK, RED, BROWN), four)

        val six = ResistorColorCodeCalculator.convertBands(listOf(BROWN, BLACK, RED), BandCount.THREE, BandCount.SIX)
        assertEquals(6, six.size)
        six.forEachIndexed { i, c -> assertTrue(c in ResistorColorCodeCalculator.getValidColorsForBand(BandCount.SIX, i)) }

        val three = ResistorColorCodeCalculator.convertBands(six, BandCount.SIX, BandCount.THREE)
        assertEquals(listOf(BROWN, BLACK, RED), three)
    }

    @Test
    fun firstBandNeverOffersBlackGoldOrSilver() {
        BandCount.entries.forEach { count ->
            val first = ResistorColorCodeCalculator.getValidColorsForBand(count, 0)
            assertFalse(BLACK in first)
            assertFalse(GOLD in first)
            assertFalse(ResistorColor.SILVER in first)
        }
    }

    // --- Reverse lookup ---

    @Test
    fun reverseSixBandIncludesTempCo() {
        val r = ResistorColorCodeCalculator.encodeResistanceToBands(bd("4700"), BandCount.SIX, bd("1"), 50)
        assertTrue(r.isExact)
        assertEquals(listOf(YELLOW, VIOLET, BLACK, BROWN, BROWN, RED), r.bands)
    }

    @Test
    fun reverseTwentyPercentRequiresThreeBand() {
        val four = ResistorColorCodeCalculator.encodeResistanceToBands(bd("4700"), BandCount.FOUR, bd("20"))
        assertTrue(four.bands.isEmpty())
        assertTrue(four.errorMessage!!.contains("3-band"))

        val three = ResistorColorCodeCalculator.encodeResistanceToBands(bd("4700"), BandCount.THREE, bd("20"))
        assertEquals(listOf(YELLOW, VIOLET, RED), three.bands)
    }

    @Test
    fun reverseInexactValueReportsNearest() {
        val r = ResistorColorCodeCalculator.encodeResistanceToBands(bd("4750"), BandCount.FOUR, bd("5"))
        assertFalse(r.isExact)
        assertEquals("Exact color-code representation unavailable.", r.errorMessage)
        assertBd("4800", r.exactResistance)
        assertTrue(r.percentageDifference.signum() > 0)

        val five = ResistorColorCodeCalculator.encodeResistanceToBands(bd("4750"), BandCount.FIVE, bd("1"))
        assertTrue(five.isExact)
    }

    @Test
    fun reverseOutOfRangeReturnsNoBands() {
        val r = ResistorColorCodeCalculator.encodeResistanceToBands(bd("0.01"), BandCount.FOUR, bd("5"))
        assertTrue(r.bands.isEmpty())
        assertFalse(r.isExact)
    }

    // --- E-series ---

    @Test
    fun preferredValuesAreExact() {
        assertFalse(PreferredValueChecker.isPreferredValue(bd("1040"), ESeries.E24))
        assertTrue(PreferredValueChecker.isPreferredValue(bd("0.47"), ESeries.E12))
        assertTrue(PreferredValueChecker.isPreferredValue(bd("4990"), ESeries.E96))
        assertFalse(PreferredValueChecker.isPreferredValue(bd("4990"), ESeries.E24))
        assertTrue(PreferredValueChecker.isPreferredValue(bd("10000000"), ESeries.E12))
    }

    @Test
    fun nearestPreferredCrossesDecade() {
        val m = PreferredValueChecker.findNearestPreferredValue(bd("9900"), ESeries.E24)
        assertBd("10000", m.nearestValue)
        assertEquals("NOT AN E24 VALUE", m.formattedStatus)
        assertBd("1.01", m.percentageDifference)

        val exact = PreferredValueChecker.findNearestPreferredValue(bd("4300"), ESeries.E24)
        assertEquals("E24 STANDARD", exact.formattedStatus)
    }

    // --- Ohm's Law ---

    @Test
    fun calculatePicksFormulaRegardlessOfOrder() {
        val r = OhmsLawCalculator.calculate(ElectricalParameter.RESISTANCE, bd("6"), ElectricalParameter.VOLTAGE, bd("12"))
        assertBd("2", r.current)
        assertBd("24", r.power)
        assertEquals(listOf("I = V ÷ R", "P = V² ÷ R"), r.formulas)
    }

    @Test
    fun displayUsesFixedDecimals() {
        assertEquals("12.00 V", OhmsLawCalculator.formatValue(bd("12"), ElectricalParameter.VOLTAGE, 2))
        assertEquals("4.8 Ω", OhmsLawCalculator.formatValue(bd("4.8"), ElectricalParameter.RESISTANCE, 1))
        assertEquals("0.333 A", OhmsLawCalculator.formatValue(bd("1").divide(bd("3"), java.math.MathContext.DECIMAL64), ElectricalParameter.CURRENT, 3))
    }

    @Test
    fun zeroPowerHandling() {
        // V + P with P = 0 → R = V² ÷ 0 is undefined
        expectError(OhmsLawCalculator.ERROR_NOT_POSITIVE) {
            OhmsLawCalculator.calculateFromVoltagePower(bd("12"), BigDecimal.ZERO)
        }
        // I + P with P = 0 → V = 0, R = 0 are valid
        val ip = OhmsLawCalculator.calculateFromCurrentPower(bd("2"), BigDecimal.ZERO)
        assertBd("0", ip.voltage)
        assertBd("0", ip.resistance)
        // R + P with P = 0 → V = 0, I = 0
        val rp = OhmsLawCalculator.calculateFromResistancePower(bd("10"), BigDecimal.ZERO)
        assertBd("0", rp.voltage)
        assertBd("0", rp.current)
        // V + R with V = 0 → I = 0, P = 0
        val vr = OhmsLawCalculator.calculateFromVoltageResistance(BigDecimal.ZERO, bd("10"))
        assertBd("0", vr.current)
        // I + R with R = 0 → V = 0, P = 0
        val ir = OhmsLawCalculator.calculateFromCurrentResistance(bd("3"), BigDecimal.ZERO)
        assertBd("0", ir.voltage)
    }

    @Test
    fun indeterminateZeroCasesAreRejected() {
        expectError(OhmsLawCalculator.ERROR_NOT_POSITIVE) {
            OhmsLawCalculator.calculateFromVoltageCurrent(BigDecimal.ZERO, BigDecimal.ZERO)
        }
        expectError(OhmsLawCalculator.ERROR_NOT_POSITIVE) {
            OhmsLawCalculator.calculateFromCurrentPower(BigDecimal.ZERO, BigDecimal.ZERO)
        }
        expectError(OhmsLawCalculator.ERROR_NOT_POSITIVE) {
            OhmsLawCalculator.calculateFromVoltagePower(BigDecimal.ZERO, bd("5"))
        }
        expectError(OhmsLawCalculator.ERROR_NOT_POSITIVE) {
            OhmsLawCalculator.calculateFromResistancePower(BigDecimal.ZERO, bd("5"))
        }
    }

    @Test
    fun negativeRejectedForEveryCombination() {
        val params = ElectricalParameter.entries
        for (i in params.indices) for (j in params.indices) if (i != j) {
            expectError(OhmsLawCalculator.ERROR_NEGATIVE) {
                OhmsLawCalculator.calculate(params[i], bd("-1"), params[j], bd("5"))
            }
        }
    }

    @Test
    fun squareRootIsPrecise() {
        val r = OhmsLawCalculator.calculateFromResistancePower(bd("2"), bd("1"))
        // V = √2
        assertEquals("1.41421356237309504880168872420970", r.voltage.setScale(32, java.math.RoundingMode.HALF_UP).toPlainString())
    }

    // --- Input parsing ---

    @Test
    fun inputParsing() {
        assertBd("4.7", parseDecimalInput(" 4.7 ")!!)
        assertBd("4.7", parseDecimalInput("4,7")!!)
        assertBd("0.5", parseDecimalInput(".5")!!)
        assertBd("-3", parseDecimalInput("-3")!!)
        assertNull(parseDecimalInput(""))
        assertNull(parseDecimalInput("abc"))
        assertNull(parseDecimalInput("1e3"))
        assertNull(parseDecimalInput("NaN"))
        assertNull(parseDecimalInput("1.2.3"))
    }

    // --- History ---

    private fun item(id: Long, title: String = "t", details: String = "d", payload: String = "p") =
        ResistorHistoryItem(id, CalculationType.OHMS_LAW, title, details, id, payload)

    @Test
    fun historySerializationEscapesSpecialCharacters() {
        val tricky = item(
            id = 7,
            title = "a|b\\c \\p literal",
            details = "line1\nline2\r\\n",
            payload = "v2;VOLTAGE=12:VOLT;formula=I = V ÷ R"
        )
        val restored = HistorySerializer.deserialize(HistorySerializer.serialize(tricky))
        assertEquals(tricky, restored)
        assertFalse(HistorySerializer.serialize(tricky).contains('\n'))

        val list = listOf(tricky, item(8), item(9, title = "4.70 kΩ ±5%"))
        assertEquals(list, HistorySerializer.deserializeList(HistorySerializer.serializeList(list)))
    }

    @Test
    fun corruptHistoryLinesAreSkipped() {
        val good = HistorySerializer.serialize(item(1))
        val data = listOf("garbage", "x|UNKNOWN|a|b|1|p", good).joinToString("\n")
        assertEquals(listOf(item(1)), HistorySerializer.deserializeList(data))
    }

    @Test
    fun historyKeepsNewestFiftyRecords() {
        var list = emptyList<ResistorHistoryItem>()
        for (id in 1L..51L) list = HistorySerializer.prepend(list, item(id))
        assertEquals(50, list.size)
        assertEquals(51L, list.first().id)
        assertEquals(2L, list.last().id) // record #1 (oldest) removed
    }

    // --- Settings persistence ---

    @Test
    fun settingsRoundTripThroughPreferences() {
        val settings = AppSettings(
            themeMode = ThemeMode.DARK,
            dynamicColor = false,
            defaultTab = CalculatorTab.OHMS_LAW,
            defaultBandCount = BandCount.SIX,
            defaultOhmsFirst = ElectricalParameter.CURRENT,
            defaultOhmsSecond = ElectricalParameter.POWER,
            decimalPlaces = 3,
            hapticFeedback = false,
            showFormula = false,
            showBandBreakdown = false,
            showESeries = false
        )
        val prefs = mutablePreferencesOf()
        SettingsMapper.write(prefs, settings)
        assertEquals(settings, SettingsMapper.read(prefs))
    }

    @Test
    fun settingsDefaultsAndInvalidValuesAreSanitized() {
        assertEquals(AppSettings(), SettingsMapper.read(mutablePreferencesOf()))

        val prefs = mutablePreferencesOf(
            SettingsMapper.THEME_MODE to "PURPLE",
            SettingsMapper.DECIMAL_PLACES to 9,
            SettingsMapper.DEFAULT_OHMS_FIRST to "POWER",
            SettingsMapper.DEFAULT_OHMS_SECOND to "POWER"
        )
        val read = SettingsMapper.read(prefs)
        assertEquals(ThemeMode.SYSTEM, read.themeMode)
        assertEquals(3, read.decimalPlaces)
        assertEquals(ElectricalParameter.POWER, read.defaultOhmsFirst)
        assertTrue(read.defaultOhmsSecond != ElectricalParameter.POWER)
    }
}
