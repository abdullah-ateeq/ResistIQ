package com.example.resistiq.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class CalculatorTab(val title: String) {
    COLOR_CODE("COLOR CODE"),
    VALUE_TO_COLOR("VALUE → COLOR"),
    OHMS_LAW("OHM'S LAW")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val defaultTab: CalculatorTab = CalculatorTab.COLOR_CODE,
    val defaultBandCount: BandCount = BandCount.FOUR,
    val defaultOhmsFirst: ElectricalParameter = ElectricalParameter.VOLTAGE,
    val defaultOhmsSecond: ElectricalParameter = ElectricalParameter.RESISTANCE,
    val decimalPlaces: Int = 2,
    val hapticFeedback: Boolean = true,
    val showFormula: Boolean = true,
    val showBandBreakdown: Boolean = true,
    val showESeries: Boolean = true
)
