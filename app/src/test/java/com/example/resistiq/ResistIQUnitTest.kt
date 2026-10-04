package com.example.resistiq

import com.example.resistiq.data.HistorySerializer
import com.example.resistiq.engine.ESeries
import com.example.resistiq.engine.OhmsLawCalculator
import com.example.resistiq.engine.PreferredValueChecker
import com.example.resistiq.engine.ResistorColorCodeCalculator
import com.example.resistiq.model.AppSettings
import com.example.resistiq.model.BandCount
import com.example.resistiq.model.CalculationType
import com.example.resistiq.model.CurrentUnit
import com.example.resistiq.model.ElectricalParameter
import com.example.resistiq.model.PowerUnit
import com.example.resistiq.model.ResistanceUnit
import com.example.resistiq.model.ResistorColor
import com.example.resistiq.model.ResistorHistoryItem
import com.example.resistiq.model.ThemeMode
import com.example.resistiq.model.VoltageUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigDecimal

class ResistIQUnitTest {

    // 1. Color -> digit
    @Test
    fun test1_colorToDigit() {
        assertEquals(0, ResistorColorCodeCalculator.colorToDigit(ResistorColor.BLACK))
        assertEquals(1, ResistorColorCodeCalculator.colorToDigit(ResistorColor.BROWN))
        assertEquals(2, ResistorColorCodeCalculator.colorToDigit(ResistorColor.RED))
        assertEquals(3, ResistorColorCodeCalculator.colorToDigit(ResistorColor.ORANGE))
        assertEquals(4, ResistorColorCodeCalculator.colorToDigit(ResistorColor.YELLOW))
        assertEquals(5, ResistorColorCodeCalculator.colorToDigit(ResistorColor.GREEN))
        assertEquals(6, ResistorColorCodeCalculator.colorToDigit(ResistorColor.BLUE))
        assertEquals(7, ResistorColorCodeCalculator.colorToDigit(ResistorColor.VIOLET))
        assertEquals(8, ResistorColorCodeCalculator.colorToDigit(ResistorColor.GRAY))
        assertEquals(9, ResistorColorCodeCalculator.colorToDigit(ResistorColor.WHITE))
        assertNull(ResistorColorCodeCalculator.colorToDigit(ResistorColor.GOLD))
        assertNull(ResistorColorCodeCalculator.colorToDigit(ResistorColor.SILVER))
    }

    // 2. Color -> multiplier
    @Test
    fun test2_colorToMultiplier() {
        assertEquals(BigDecimal("1"), ResistorColorCodeCalculator.colorToMultiplier(ResistorColor.BLACK))
        assertEquals(BigDecimal("10"), ResistorColorCodeCalculator.colorToMultiplier(ResistorColor.BROWN))
        assertEquals(BigDecimal("100"), ResistorColorCodeCalculator.colorToMultiplier(ResistorColor.RED))
        assertEquals(BigDecimal("1000"), ResistorColorCodeCalculator.colorToMultiplier(ResistorColor.ORANGE))
        assertEquals(BigDecimal("10000"), ResistorColorCodeCalculator.colorToMultiplier(ResistorColor.YELLOW))
        assertEquals(BigDecimal("100000"), ResistorColorCodeCalculator.colorToMultiplier(ResistorColor.GREEN))
        assertEquals(BigDecimal("1000000"), ResistorColorCodeCalculator.colorToMultiplier(ResistorColor.BLUE))
        assertEquals(BigDecimal("10000000"), ResistorColorCodeCalculator.colorToMultiplier(ResistorColor.VIOLET))
        assertEquals(BigDecimal("100000000"), ResistorColorCodeCalculator.colorToMultiplier(ResistorColor.GRAY))
        assertEquals(BigDecimal("1000000000"), ResistorColorCodeCalculator.colorToMultiplier(ResistorColor.WHITE))
        assertEquals(BigDecimal("0.1"), ResistorColorCodeCalculator.colorToMultiplier(ResistorColor.GOLD))
        assertEquals(BigDecimal("0.01"), ResistorColorCodeCalculator.colorToMultiplier(ResistorColor.SILVER))
    }

    // 3. Color -> tolerance
    @Test
    fun test3_colorToTolerance() {
        assertEquals(BigDecimal("1"), ResistorColorCodeCalculator.colorToTolerance(ResistorColor.BROWN))
        assertEquals(BigDecimal("2"), ResistorColorCodeCalculator.colorToTolerance(ResistorColor.RED))
        assertEquals(BigDecimal("0.5"), ResistorColorCodeCalculator.colorToTolerance(ResistorColor.GREEN))
        assertEquals(BigDecimal("0.25"), ResistorColorCodeCalculator.colorToTolerance(ResistorColor.BLUE))
        assertEquals(BigDecimal("0.1"), ResistorColorCodeCalculator.colorToTolerance(ResistorColor.VIOLET))
        assertEquals(BigDecimal("0.05"), ResistorColorCodeCalculator.colorToTolerance(ResistorColor.GRAY))
        assertEquals(BigDecimal("5"), ResistorColorCodeCalculator.colorToTolerance(ResistorColor.GOLD))
        assertEquals(BigDecimal("10"), ResistorColorCodeCalculator.colorToTolerance(ResistorColor.SILVER))
        assertNull(ResistorColorCodeCalculator.colorToTolerance(ResistorColor.BLACK))
    }

    // 4. 3-band decoding
    @Test
    fun test4_threeBandDecoding() {
        val bands = listOf(ResistorColor.BROWN, ResistorColor.BLACK, ResistorColor.RED)
        val result = ResistorColorCodeCalculator.decodeBands(bands, BandCount.THREE)
        // Brown (1), Black (0), Red (×100) = 1000 Ω
        assertEquals(0, BigDecimal("1000").compareTo(result.resistanceOhms))
        assertEquals(0, BigDecimal("20").compareTo(result.tolerancePercent))
        assertEquals("1.00 kΩ", result.formattedResistance)
    }

    // 5. 4-band decoding
    @Test
    fun test5_fourBandDecoding() {
        // Brown Black Red Gold -> 1 kΩ ±5%
        val bands1 = listOf(ResistorColor.BROWN, ResistorColor.BLACK, ResistorColor.RED, ResistorColor.GOLD)
        val res1 = ResistorColorCodeCalculator.decodeBands(bands1, BandCount.FOUR)
        assertEquals(0, BigDecimal("1000").compareTo(res1.resistanceOhms))
        assertEquals(0, BigDecimal("5").compareTo(res1.tolerancePercent))
        assertEquals("1.00 kΩ", res1.formattedResistance)
        assertEquals("±5%", res1.formattedTolerance)

        // Yellow Violet Red Gold -> 4.7 kΩ ±5%
        val bands2 = listOf(ResistorColor.YELLOW, ResistorColor.VIOLET, ResistorColor.RED, ResistorColor.GOLD)
        val res2 = ResistorColorCodeCalculator.decodeBands(bands2, BandCount.FOUR)
        assertEquals(0, BigDecimal("4700").compareTo(res2.resistanceOhms))
        assertEquals(0, BigDecimal("5").compareTo(res2.tolerancePercent))
        assertEquals("4.70 kΩ", res2.formattedResistance)
    }

    // 6. 5-band decoding
    @Test
    fun test6_fiveBandDecoding() {
        // Brown Black Black Brown Brown -> 100 × 10 = 1000 Ω = 1 kΩ ±1%
        val bands = listOf(
            ResistorColor.BROWN,
            ResistorColor.BLACK,
            ResistorColor.BLACK,
            ResistorColor.BROWN,
            ResistorColor.BROWN
        )
        val res = ResistorColorCodeCalculator.decodeBands(bands, BandCount.FIVE)
        assertEquals(0, BigDecimal("1000").compareTo(res.resistanceOhms))
        assertEquals(0, BigDecimal("1").compareTo(res.tolerancePercent))
        assertEquals("1.00 kΩ", res.formattedResistance)
        assertEquals("±1%", res.formattedTolerance)
    }

    // 7. 6-band decoding
    @Test
    fun test7_sixBandDecoding() {
        // Brown Black Black Brown Brown Red -> 1 kΩ ±1%, 50 ppm/°C
        val bands = listOf(
            ResistorColor.BROWN,
            ResistorColor.BLACK,
            ResistorColor.BLACK,
            ResistorColor.BROWN,
            ResistorColor.BROWN,
            ResistorColor.RED
        )
        val res = ResistorColorCodeCalculator.decodeBands(bands, BandCount.SIX)
        assertEquals(0, BigDecimal("1000").compareTo(res.resistanceOhms))
        assertEquals(0, BigDecimal("1").compareTo(res.tolerancePercent))
        assertEquals(50, res.temperatureCoefficient)
        assertEquals("1.00 kΩ", res.formattedResistance)
        assertEquals("50 ppm/°C", res.formattedTempCo)
    }

    // 8. Tolerance range
    @Test
    fun test8_toleranceRange() {
        // 1 kΩ ±5% -> min: 950 Ω, max: 1050 Ω
        val bands1 = listOf(ResistorColor.BROWN, ResistorColor.BLACK, ResistorColor.RED, ResistorColor.GOLD)
        val res1 = ResistorColorCodeCalculator.decodeBands(bands1, BandCount.FOUR)
        assertEquals(0, BigDecimal("950").compareTo(res1.minimumOhms))
        assertEquals(0, BigDecimal("1050").compareTo(res1.maximumOhms))

        // 4.7 kΩ ±5% -> min: 4465 Ω (4.465 kΩ), max: 4935 Ω (4.935 kΩ)
        val bands2 = listOf(ResistorColor.YELLOW, ResistorColor.VIOLET, ResistorColor.RED, ResistorColor.GOLD)
        val res2 = ResistorColorCodeCalculator.decodeBands(bands2, BandCount.FOUR)
        assertEquals(0, BigDecimal("4465").compareTo(res2.minimumOhms))
        assertEquals(0, BigDecimal("4935").compareTo(res2.maximumOhms))
    }

    // 9. Resistance formatting
    @Test
    fun test9_resistanceFormatting() {
        assertEquals("470 Ω", ResistorColorCodeCalculator.formatResistance(BigDecimal("470")))
        assertEquals("4.7 kΩ", ResistorColorCodeCalculator.formatResistance(BigDecimal("4700"), 1))
        assertEquals("4.70 kΩ", ResistorColorCodeCalculator.formatResistance(BigDecimal("4700"), 2))
        assertEquals("1.0 MΩ", ResistorColorCodeCalculator.formatResistance(BigDecimal("1000000"), 1))
        assertEquals("1.00 MΩ", ResistorColorCodeCalculator.formatResistance(BigDecimal("1000000"), 2))
        assertEquals("2.20 GΩ", ResistorColorCodeCalculator.formatResistance(BigDecimal("2200000000"), 2))
    }

    // 10. Resistance -> bands
    @Test
    fun test10_resistanceToBands() {
        // 4.7 kΩ, 4-band, ±5% -> Yellow, Violet, Red, Gold
        val res = ResistorColorCodeCalculator.encodeResistanceToBands(
            resistanceOhms = BigDecimal("4700"),
            bandCount = BandCount.FOUR,
            tolerancePercent = BigDecimal("5")
        )
        assertTrue(res.isExact)
        assertEquals(
            listOf(ResistorColor.YELLOW, ResistorColor.VIOLET, ResistorColor.RED, ResistorColor.GOLD),
            res.bands
        )
    }

    // 11. E12 detection
    @Test
    fun test11_e12Detection() {
        assertTrue(PreferredValueChecker.isPreferredValue(BigDecimal("4700"), ESeries.E12))
        assertTrue(PreferredValueChecker.isPreferredValue(BigDecimal("1000"), ESeries.E12))
        assertFalse(PreferredValueChecker.isPreferredValue(BigDecimal("4300"), ESeries.E12))
    }

    // 12. E24 detection
    @Test
    fun test12_e24Detection() {
        assertTrue(PreferredValueChecker.isPreferredValue(BigDecimal("4300"), ESeries.E24))
        assertTrue(PreferredValueChecker.isPreferredValue(BigDecimal("4700"), ESeries.E24))
        assertTrue(PreferredValueChecker.isPreferredValue(BigDecimal("91000"), ESeries.E24))
    }

    // 13. Nearest preferred value
    @Test
    fun test13_nearestPreferredValue() {
        val exactMatch = PreferredValueChecker.findNearestPreferredValue(BigDecimal("4300"), ESeries.E24)
        assertTrue(exactMatch.isExactMatch)
        assertEquals(0, BigDecimal("4300").compareTo(exactMatch.nearestValue))
        assertEquals(0, BigDecimal.ZERO.compareTo(exactMatch.percentageDifference))

        val nonExactMatch = PreferredValueChecker.findNearestPreferredValue(BigDecimal("4300"), ESeries.E12)
        assertFalse(nonExactMatch.isExactMatch)
        assertTrue(nonExactMatch.percentageDifference > BigDecimal.ZERO)
    }

    // 14. V + I -> P & R
    @Test
    fun test14_voltageCurrentToPower() {
        // V = 24 V, I = 5 A -> R = 4.8 Ω, P = 120 W
        val res = OhmsLawCalculator.calculateFromVoltageCurrent(BigDecimal("24"), BigDecimal("5"))
        assertEquals(0, BigDecimal("4.8").compareTo(res.resistance))
        assertEquals(0, BigDecimal("120").compareTo(res.power))
    }

    // 15. V + R -> I/P
    @Test
    fun test15_voltageResistanceToCurrentPower() {
        // V = 12 V, R = 6 Ω -> I = 2 A, P = 24 W
        val res = OhmsLawCalculator.calculateFromVoltageResistance(BigDecimal("12"), BigDecimal("6"))
        assertEquals(0, BigDecimal("2").compareTo(res.current))
        assertEquals(0, BigDecimal("24").compareTo(res.power))
    }

    // 16. V + P -> I/R
    @Test
    fun test16_voltagePowerToCurrentResistance() {
        // V = 20 V, P = 100 W -> I = 5 A, R = 4 Ω
        val res = OhmsLawCalculator.calculateFromVoltagePower(BigDecimal("20"), BigDecimal("100"))
        assertEquals(0, BigDecimal("5").compareTo(res.current))
        assertEquals(0, BigDecimal("4").compareTo(res.resistance))
    }

    // 17. I + R -> V/P
    @Test
    fun test17_currentResistanceToVoltagePower() {
        // I = 2 A, R = 6 Ω -> V = 12 V, P = 24 W
        val res = OhmsLawCalculator.calculateFromCurrentResistance(BigDecimal("2"), BigDecimal("6"))
        assertEquals(0, BigDecimal("12").compareTo(res.voltage))
        assertEquals(0, BigDecimal("24").compareTo(res.power))
    }

    // 18. I + P -> V/R
    @Test
    fun test18_currentPowerToVoltageResistance() {
        // I = 5 A, P = 100 W -> V = 20 V, R = 4 Ω
        val res = OhmsLawCalculator.calculateFromCurrentPower(BigDecimal("5"), BigDecimal("100"))
        assertEquals(0, BigDecimal("20").compareTo(res.voltage))
        assertEquals(0, BigDecimal("4").compareTo(res.resistance))
    }

    // 19. R + P -> V/I
    @Test
    fun test19_resistancePowerToVoltageCurrent() {
        // R = 4 Ω, P = 100 W -> V = 20 V, I = 5 A
        val res = OhmsLawCalculator.calculateFromResistancePower(BigDecimal("4"), BigDecimal("100"))
        assertEquals(0, BigDecimal("20").compareTo(res.voltage))
        assertEquals(0, BigDecimal("5").compareTo(res.current))
    }

    // 20. Division by zero
    @Test
    fun test20_divisionByZero() {
        try {
            OhmsLawCalculator.calculateFromVoltageResistance(BigDecimal("12"), BigDecimal.ZERO)
            fail("Expected IllegalArgumentException for R = 0")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("greater than zero") == true)
        }

        try {
            OhmsLawCalculator.calculateFromVoltageCurrent(BigDecimal("12"), BigDecimal.ZERO)
            fail("Expected IllegalArgumentException for I = 0")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("greater than zero") == true)
        }
    }

    // 21. Negative input validation
    @Test
    fun test21_negativeInputValidation() {
        try {
            OhmsLawCalculator.calculateFromVoltageResistance(BigDecimal("-12"), BigDecimal("6"))
            fail("Expected IllegalArgumentException for negative voltage")
        } catch (e: IllegalArgumentException) {
            assertEquals("Value cannot be negative.", e.message)
        }

        try {
            OhmsLawCalculator.calculateFromCurrentResistance(BigDecimal("2"), BigDecimal("-6"))
            fail("Expected IllegalArgumentException for negative resistance")
        } catch (e: IllegalArgumentException) {
            assertEquals("Value cannot be negative.", e.message)
        }
    }

    // 22. Unit conversion
    @Test
    fun test22_unitConversion() {
        assertEquals(0, BigDecimal("1000").compareTo(VoltageUnit.KILOVOLT.toBase(BigDecimal("1"))))
        assertEquals(0, BigDecimal("0.5").compareTo(CurrentUnit.MILLIAMPERE.toBase(BigDecimal("500"))))
        assertEquals(0, BigDecimal("4700").compareTo(ResistanceUnit.KILOHM.toBase(BigDecimal("4.7"))))
        assertEquals(0, BigDecimal("2500").compareTo(PowerUnit.KILOWATT.toBase(BigDecimal("2.5"))))
    }

    // 23. History serialization
    @Test
    fun test23_historySerialization() {
        val originalItem = ResistorHistoryItem(
            id = 12345L,
            type = CalculationType.COLOR_TO_VALUE,
            title = "4.70 kΩ ±5%",
            details = "Yellow · Violet · Red · Gold (4 BAND)",
            timestamp = 1680000000L,
            payloadJson = "FOUR;YELLOW,VIOLET,RED,GOLD"
        )

        val serialized = HistorySerializer.serialize(originalItem)
        val deserialized = HistorySerializer.deserialize(serialized)

        assertNotNull(deserialized)
        assertEquals(originalItem.id, deserialized?.id)
        assertEquals(originalItem.type, deserialized?.type)
        assertEquals(originalItem.title, deserialized?.title)
        assertEquals(originalItem.details, deserialized?.details)
        assertEquals(originalItem.timestamp, deserialized?.timestamp)
        assertEquals(originalItem.payloadJson, deserialized?.payloadJson)

        val list = listOf(originalItem)
        val listSerialized = HistorySerializer.serializeList(list)
        val listDeserialized = HistorySerializer.deserializeList(listSerialized)
        assertEquals(1, listDeserialized.size)
        assertEquals(originalItem.id, listDeserialized[0].id)
    }

    // 24. Settings persistence model
    @Test
    fun test24_settingsPersistence() {
        val defaultSettings = AppSettings()
        assertEquals(ThemeMode.SYSTEM, defaultSettings.themeMode)
        assertEquals(BandCount.FOUR, defaultSettings.defaultBandCount)
        assertEquals(2, defaultSettings.decimalPlaces)
        assertTrue(defaultSettings.showFormula)

        val modified = defaultSettings.copy(
            themeMode = ThemeMode.DARK,
            decimalPlaces = 3,
            defaultBandCount = BandCount.FIVE
        )
        assertEquals(ThemeMode.DARK, modified.themeMode)
        assertEquals(3, modified.decimalPlaces)
        assertEquals(BandCount.FIVE, modified.defaultBandCount)
    }
}
