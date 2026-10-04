package com.example.resistiq.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.resistiq.data.PreferencesManager
import com.example.resistiq.engine.ESeries
import com.example.resistiq.engine.OhmsLawCalculator
import com.example.resistiq.engine.PreferredValueChecker
import com.example.resistiq.engine.PreferredValueMatch
import com.example.resistiq.engine.ResistorColorCodeCalculator
import com.example.resistiq.model.AppSettings
import com.example.resistiq.model.BandCount
import com.example.resistiq.model.CalculationType
import com.example.resistiq.model.CalculatorTab
import com.example.resistiq.model.CurrentUnit
import com.example.resistiq.model.ElectricalParameter
import com.example.resistiq.model.OhmsLawResult
import com.example.resistiq.model.PowerUnit
import com.example.resistiq.model.ResistanceUnit
import com.example.resistiq.model.ResistorColor
import com.example.resistiq.model.ResistorHistoryItem
import com.example.resistiq.model.ResistorValueResult
import com.example.resistiq.model.VoltageUnit
import com.example.resistiq.model.parseDecimalInput
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

private val DEFAULT_BANDS = ResistorColorCodeCalculator.getDefaultBands(BandCount.FOUR)

/** Tolerances offered in reverse mode. ±20% means "no tolerance band", i.e. a 3-band resistor. */
val REVERSE_TOLERANCES: List<BigDecimal> = listOf("1", "2", "5", "10", "20").map { BigDecimal(it) }

data class CalculatorUiState(
    val currentTab: CalculatorTab = CalculatorTab.COLOR_CODE,
    val decimalPlaces: Int = 2,
    val showFormula: Boolean = true,
    val showBandBreakdown: Boolean = true,
    val showESeries: Boolean = true,
    val hapticFeedback: Boolean = true,

    // Tool 1: Color Code -> Resistance
    val bandCount: BandCount = BandCount.FOUR,
    val selectedBands: List<ResistorColor> = DEFAULT_BANDS,
    /** Index of the most recently changed band, used to animate it in the resistor visual. */
    val lastChangedBand: Int? = null,
    val colorResult: ResistorValueResult = ResistorColorCodeCalculator.decodeBands(DEFAULT_BANDS, BandCount.FOUR, 2),

    // Tool 2: Resistance -> Color
    val reverseInput: String = "4.7",
    val reverseUnit: ResistanceUnit = ResistanceUnit.KILOHM,
    val reverseBandCount: BandCount = BandCount.FOUR,
    val reverseTolerance: BigDecimal = BigDecimal("5"),
    val reverseTempCo: Int = 50,
    val reverseInputError: String? = null,
    val reverseResult: ResistorColorCodeCalculator.ReverseLookupResult? = null,
    val reversePreferredMatches: List<PreferredValueMatch> = emptyList(),

    // Tool 3: Ohm's Law
    val ohmsSelectedParams: List<ElectricalParameter> = listOf(ElectricalParameter.VOLTAGE, ElectricalParameter.RESISTANCE),
    val ohmsVoltageInput: String = "12",
    val ohmsVoltageUnit: VoltageUnit = VoltageUnit.VOLT,
    val ohmsCurrentInput: String = "",
    val ohmsCurrentUnit: CurrentUnit = CurrentUnit.AMPERE,
    val ohmsResistanceInput: String = "6",
    val ohmsResistanceUnit: ResistanceUnit = ResistanceUnit.OHM,
    val ohmsPowerInput: String = "",
    val ohmsPowerUnit: PowerUnit = PowerUnit.WATT,
    val ohmsResult: OhmsLawResult? = null,
    val ohmsError: String? = null
) {
    /** Primary E-series shown as "E24 STANDARD" / "NOT AN E24 VALUE". */
    val reversePrimaryMatch: PreferredValueMatch?
        get() = reversePreferredMatches.firstOrNull { it.series == ESeries.E24 }
}

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private var defaultsApplied = false
    private var latestSettings = AppSettings()

    init {
        recalculateAll()
        viewModelScope.launch {
            preferencesManager.appSettingsFlow.collect { settings ->
                latestSettings = settings
                _uiState.update {
                    it.copy(
                        decimalPlaces = settings.decimalPlaces,
                        showFormula = settings.showFormula,
                        showBandBreakdown = settings.showBandBreakdown,
                        showESeries = settings.showESeries,
                        hapticFeedback = settings.hapticFeedback
                    )
                }
                if (!defaultsApplied) {
                    defaultsApplied = true
                    applyDefaults(settings)
                }
                recalculateAll()
            }
        }
    }

    /** Applies the user's default tool, band count and Ohm's Law pair once per launch. */
    private fun applyDefaults(settings: AppSettings) {
        val count = settings.defaultBandCount
        _uiState.update {
            it.copy(
                currentTab = settings.defaultTab,
                bandCount = count,
                selectedBands = ResistorColorCodeCalculator.getDefaultBands(count),
                reverseBandCount = count,
                reverseTolerance = if (count == BandCount.THREE) ResistorColorCodeCalculator.NO_BAND_TOLERANCE else it.reverseTolerance,
                ohmsSelectedParams = listOf(settings.defaultOhmsFirst, settings.defaultOhmsSecond)
            )
        }
    }

    private fun recalculateAll() {
        recalculateColorCode()
        recalculateReverse()
        recalculateOhmsLaw()
    }

    fun selectTab(tab: CalculatorTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    private fun emit(message: String) {
        _snackbarMessage.tryEmit(message)
    }

    // --- TOOL 1: COLOR CODE -> RESISTANCE ---

    fun setBandCount(newCount: BandCount) {
        val state = _uiState.value
        if (state.bandCount == newCount) return
        val converted = ResistorColorCodeCalculator.convertBands(state.selectedBands, state.bandCount, newCount)
        _uiState.update { it.copy(bandCount = newCount, selectedBands = converted, lastChangedBand = null) }
        recalculateColorCode()
    }

    fun setBandColor(bandIndex: Int, color: ResistorColor) {
        val state = _uiState.value
        if (bandIndex !in state.selectedBands.indices) return
        if (color !in ResistorColorCodeCalculator.getValidColorsForBand(state.bandCount, bandIndex)) return
        val updated = state.selectedBands.toMutableList().also { it[bandIndex] = color }
        _uiState.update { it.copy(selectedBands = updated, lastChangedBand = bandIndex) }
        recalculateColorCode()
    }

    /** Reset returns to the reference 4-band 1 kΩ ±5% resistor (Brown-Black-Red-Gold). */
    fun resetColorCode() {
        _uiState.update {
            it.copy(bandCount = BandCount.FOUR, selectedBands = DEFAULT_BANDS, lastChangedBand = null)
        }
        recalculateColorCode()
    }

    private fun recalculateColorCode() {
        val state = _uiState.value
        val result = ResistorColorCodeCalculator.decodeBands(
            bands = state.selectedBands,
            bandCount = state.bandCount,
            decimalPlaces = state.decimalPlaces
        )
        _uiState.update { it.copy(colorResult = result) }
    }

    fun colorCodeValueText(): String {
        val res = _uiState.value.colorResult
        return "${res.formattedResistance} ${res.formattedTolerance}"
    }

    fun colorCodeBandsText(): String =
        _uiState.value.colorResult.bands.joinToString(" - ") { it.displayName }

    fun colorCodeFullText(): String {
        val res = _uiState.value.colorResult
        return buildString {
            appendLine("Bands: ${colorCodeBandsText()}")
            appendLine("Resistance: ${res.formattedResistance}")
            appendLine("Tolerance: ${res.formattedTolerance}")
            append("Range: ${res.formattedRange}")
            res.formattedTempCo?.let { append("\nTemperature coefficient: $it") }
        }
    }

    fun saveColorCodeCalculation() {
        val state = _uiState.value
        val res = state.colorResult
        val details = buildString {
            append(res.bands.joinToString(" · ") { it.displayName })
            res.formattedTempCo?.let { append(" · $it") }
        }
        save(
            type = CalculationType.COLOR_TO_VALUE,
            title = "${res.formattedResistance} ${res.formattedTolerance}",
            details = details,
            payload = "${state.bandCount.name};${res.bands.joinToString(",") { it.name }}"
        )
    }

    // --- TOOL 2: RESISTANCE -> COLOR ---

    fun setReverseInput(input: String) {
        _uiState.update { it.copy(reverseInput = input) }
        recalculateReverse()
    }

    fun setReverseUnit(unit: ResistanceUnit) {
        _uiState.update { it.copy(reverseUnit = unit) }
        recalculateReverse()
    }

    fun setReverseBandCount(count: BandCount) {
        _uiState.update {
            val tolerance = when {
                count == BandCount.THREE -> ResistorColorCodeCalculator.NO_BAND_TOLERANCE
                it.reverseTolerance.compareTo(ResistorColorCodeCalculator.NO_BAND_TOLERANCE) == 0 -> BigDecimal("5")
                else -> it.reverseTolerance
            }
            it.copy(reverseBandCount = count, reverseTolerance = tolerance)
        }
        recalculateReverse()
    }

    fun setReverseTolerance(tolerance: BigDecimal) {
        _uiState.update {
            val isNoBand = tolerance.compareTo(ResistorColorCodeCalculator.NO_BAND_TOLERANCE) == 0
            val count = when {
                isNoBand -> BandCount.THREE
                it.reverseBandCount == BandCount.THREE -> BandCount.FOUR
                else -> it.reverseBandCount
            }
            it.copy(reverseTolerance = tolerance, reverseBandCount = count)
        }
        recalculateReverse()
    }

    fun setReverseTempCo(ppm: Int) {
        _uiState.update { it.copy(reverseTempCo = ppm) }
        recalculateReverse()
    }

    private fun reverseTargetOhms(state: CalculatorUiState): BigDecimal? =
        parseDecimalInput(state.reverseInput)?.let { state.reverseUnit.toBase(it) }

    private fun recalculateReverse() {
        val state = _uiState.value
        if (state.reverseInput.isBlank()) {
            _uiState.update { it.copy(reverseResult = null, reversePreferredMatches = emptyList(), reverseInputError = null) }
            return
        }
        val parsed = parseDecimalInput(state.reverseInput)
        val error = when {
            parsed == null -> "Enter a valid number."
            parsed.signum() < 0 -> OhmsLawCalculator.ERROR_NEGATIVE
            parsed.signum() == 0 -> "Resistance must be greater than zero."
            else -> null
        }
        if (error != null || parsed == null) {
            _uiState.update { it.copy(reverseResult = null, reversePreferredMatches = emptyList(), reverseInputError = error) }
            return
        }

        val ohms = state.reverseUnit.toBase(parsed)
        val result = ResistorColorCodeCalculator.encodeResistanceToBands(
            resistanceOhms = ohms,
            bandCount = state.reverseBandCount,
            tolerancePercent = state.reverseTolerance,
            tempCoPpm = state.reverseTempCo
        )
        val matches = ESeries.entries.map { PreferredValueChecker.findNearestPreferredValue(ohms, it) }

        _uiState.update {
            it.copy(reverseResult = result, reversePreferredMatches = matches, reverseInputError = null)
        }
    }

    fun reverseBandsText(): String? =
        _uiState.value.reverseResult?.bands?.takeIf { it.isNotEmpty() }?.joinToString(" - ") { it.displayName }

    fun saveReverseCalculation() {
        val state = _uiState.value
        val rev = state.reverseResult?.takeIf { it.bands.isNotEmpty() } ?: return
        val ohms = reverseTargetOhms(state) ?: return
        val target = ResistorColorCodeCalculator.formatResistance(ohms, state.decimalPlaces)
        val tolerance = "±${state.reverseTolerance.stripTrailingZeros().toPlainString()}%"
        val details = buildString {
            append(rev.bands.joinToString(" · ") { it.displayName })
            append(" (${state.reverseBandCount.count}-band)")
            if (!rev.isExact) {
                append(" · nearest ${ResistorColorCodeCalculator.formatResistance(rev.exactResistance, state.decimalPlaces)}")
            }
        }
        save(
            type = CalculationType.VALUE_TO_COLOR,
            title = "$target $tolerance",
            details = details,
            payload = listOf(
                state.reverseInput.trim(),
                state.reverseUnit.name,
                state.reverseBandCount.name,
                state.reverseTolerance.toPlainString(),
                state.reverseTempCo.toString(),
                rev.bands.joinToString(",") { it.name }
            ).joinToString(";")
        )
    }

    // --- TOOL 3: OHM'S LAW ---

    /** Keeps exactly two known values selected; choosing a third replaces the oldest choice. */
    fun toggleOhmsParameter(param: ElectricalParameter) {
        val current = _uiState.value.ohmsSelectedParams
        if (param in current) return
        _uiState.update { it.copy(ohmsSelectedParams = current.drop(1) + param) }
        recalculateOhmsLaw()
    }

    fun setOhmsVoltageInput(value: String) {
        _uiState.update { it.copy(ohmsVoltageInput = value) }
        recalculateOhmsLaw()
    }

    fun setOhmsVoltageUnit(unit: VoltageUnit) {
        _uiState.update { it.copy(ohmsVoltageUnit = unit) }
        recalculateOhmsLaw()
    }

    fun setOhmsCurrentInput(value: String) {
        _uiState.update { it.copy(ohmsCurrentInput = value) }
        recalculateOhmsLaw()
    }

    fun setOhmsCurrentUnit(unit: CurrentUnit) {
        _uiState.update { it.copy(ohmsCurrentUnit = unit) }
        recalculateOhmsLaw()
    }

    fun setOhmsResistanceInput(value: String) {
        _uiState.update { it.copy(ohmsResistanceInput = value) }
        recalculateOhmsLaw()
    }

    fun setOhmsResistanceUnit(unit: ResistanceUnit) {
        _uiState.update { it.copy(ohmsResistanceUnit = unit) }
        recalculateOhmsLaw()
    }

    fun setOhmsPowerInput(value: String) {
        _uiState.update { it.copy(ohmsPowerInput = value) }
        recalculateOhmsLaw()
    }

    fun setOhmsPowerUnit(unit: PowerUnit) {
        _uiState.update { it.copy(ohmsPowerUnit = unit) }
        recalculateOhmsLaw()
    }

    fun resetOhmsLaw() {
        _uiState.update {
            it.copy(
                ohmsSelectedParams = listOf(latestSettings.defaultOhmsFirst, latestSettings.defaultOhmsSecond),
                ohmsVoltageInput = "12",
                ohmsVoltageUnit = VoltageUnit.VOLT,
                ohmsCurrentInput = "",
                ohmsCurrentUnit = CurrentUnit.AMPERE,
                ohmsResistanceInput = "6",
                ohmsResistanceUnit = ResistanceUnit.OHM,
                ohmsPowerInput = "",
                ohmsPowerUnit = PowerUnit.WATT
            )
        }
        recalculateOhmsLaw()
    }

    private fun CalculatorUiState.rawInput(param: ElectricalParameter): String = when (param) {
        ElectricalParameter.VOLTAGE -> ohmsVoltageInput
        ElectricalParameter.CURRENT -> ohmsCurrentInput
        ElectricalParameter.RESISTANCE -> ohmsResistanceInput
        ElectricalParameter.POWER -> ohmsPowerInput
    }

    private fun CalculatorUiState.unitName(param: ElectricalParameter): String = when (param) {
        ElectricalParameter.VOLTAGE -> ohmsVoltageUnit.name
        ElectricalParameter.CURRENT -> ohmsCurrentUnit.name
        ElectricalParameter.RESISTANCE -> ohmsResistanceUnit.name
        ElectricalParameter.POWER -> ohmsPowerUnit.name
    }

    private fun CalculatorUiState.toBase(param: ElectricalParameter, value: BigDecimal): BigDecimal = when (param) {
        ElectricalParameter.VOLTAGE -> ohmsVoltageUnit.toBase(value)
        ElectricalParameter.CURRENT -> ohmsCurrentUnit.toBase(value)
        ElectricalParameter.RESISTANCE -> ohmsResistanceUnit.toBase(value)
        ElectricalParameter.POWER -> ohmsPowerUnit.toBase(value)
    }

    private fun recalculateOhmsLaw() {
        val state = _uiState.value
        val selected = state.ohmsSelectedParams
        if (selected.size != 2 || selected[0] == selected[1]) {
            _uiState.update { it.copy(ohmsResult = null, ohmsError = "Select exactly two known values.") }
            return
        }

        val values = mutableListOf<BigDecimal>()
        for (param in selected) {
            val raw = state.rawInput(param)
            if (raw.isBlank()) {
                _uiState.update { it.copy(ohmsResult = null, ohmsError = null) }
                return
            }
            val parsed = parseDecimalInput(raw)
            val error = when {
                parsed == null -> "Enter a valid number for ${param.displayName.lowercase()}."
                parsed.signum() < 0 -> OhmsLawCalculator.ERROR_NEGATIVE
                else -> null
            }
            if (error != null || parsed == null) {
                _uiState.update { it.copy(ohmsResult = null, ohmsError = error) }
                return
            }
            values += state.toBase(param, parsed)
        }

        try {
            val result = OhmsLawCalculator.calculate(selected[0], values[0], selected[1], values[1])
            _uiState.update { it.copy(ohmsResult = result, ohmsError = null) }
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(ohmsResult = null, ohmsError = e.message ?: "Calculation not possible.") }
        } catch (e: ArithmeticException) {
            _uiState.update { it.copy(ohmsResult = null, ohmsError = "Value is out of range for this calculation.") }
        }
    }

    private fun formatOhms(res: OhmsLawResult, param: ElectricalParameter, decimals: Int): String {
        val value = when (param) {
            ElectricalParameter.VOLTAGE -> res.voltage
            ElectricalParameter.CURRENT -> res.current
            ElectricalParameter.RESISTANCE -> res.resistance
            ElectricalParameter.POWER -> res.power
        }
        return OhmsLawCalculator.formatValue(value, param, decimals)
    }

    fun ohmsLawCopyText(): String? {
        val state = _uiState.value
        val res = state.ohmsResult ?: return null
        return buildString {
            ElectricalParameter.entries.forEach { param ->
                val tag = if (param in res.inputParameters) "input" else "calculated"
                appendLine("${param.displayName}: ${formatOhms(res, param, state.decimalPlaces)} ($tag)")
            }
            append(res.formulas.joinToString(", "))
        }
    }

    fun saveOhmsLawCalculation() {
        val state = _uiState.value
        val res = state.ohmsResult ?: return
        val inputs = state.ohmsSelectedParams
        val calculated = ElectricalParameter.entries.filter { it !in inputs }
        val payload = buildList {
            add("v2")
            inputs.forEach { param ->
                add("${param.name}=${state.rawInput(param).trim()}:${state.unitName(param)}")
            }
            add("formula=${res.formulas.joinToString(", ")}")
        }.joinToString(";")
        save(
            type = CalculationType.OHMS_LAW,
            title = inputs.joinToString(" · ") { formatOhms(res, it, state.decimalPlaces) },
            details = calculated.joinToString(" · ") { formatOhms(res, it, state.decimalPlaces) } +
                "  (${res.formulas.joinToString(", ")})",
            payload = payload
        )
    }

    private fun save(type: CalculationType, title: String, details: String, payload: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            preferencesManager.saveHistoryItem(
                ResistorHistoryItem(
                    id = now,
                    type = type,
                    title = title,
                    details = details,
                    timestamp = now,
                    payloadJson = payload
                )
            )
            emit("Calculation saved")
        }
    }

    // --- RESTORE FROM HISTORY ---

    fun restoreFromHistory(item: ResistorHistoryItem) {
        defaultsApplied = true
        val restored = runCatching {
            when (item.type) {
                CalculationType.COLOR_TO_VALUE -> restoreColorCode(item.payloadJson)
                CalculationType.VALUE_TO_COLOR -> restoreReverse(item.payloadJson)
                CalculationType.OHMS_LAW -> restoreOhmsLaw(item.payloadJson)
            }
        }.isSuccess
        val tab = when (item.type) {
            CalculationType.COLOR_TO_VALUE -> CalculatorTab.COLOR_CODE
            CalculationType.VALUE_TO_COLOR -> CalculatorTab.VALUE_TO_COLOR
            CalculationType.OHMS_LAW -> CalculatorTab.OHMS_LAW
        }
        _uiState.update { it.copy(currentTab = tab) }
        recalculateAll()
        if (!restored) emit("Could not restore this record")
    }

    private fun restoreColorCode(payload: String) {
        val (countName, colorNames) = payload.split(";")
        val count = BandCount.valueOf(countName)
        val colors = colorNames.split(",").map { ResistorColor.valueOf(it) }
        require(colors.size == count.count)
        colors.forEachIndexed { index, color ->
            require(color in ResistorColorCodeCalculator.getValidColorsForBand(count, index))
        }
        _uiState.update { it.copy(bandCount = count, selectedBands = colors, lastChangedBand = null) }
    }

    private fun restoreReverse(payload: String) {
        val parts = payload.split(";")
        val count = BandCount.valueOf(parts[2])
        _uiState.update {
            it.copy(
                reverseInput = parts[0],
                reverseUnit = ResistanceUnit.valueOf(parts[1]),
                reverseBandCount = count,
                reverseTolerance = BigDecimal(parts[3]),
                reverseTempCo = parts.getOrNull(4)?.toIntOrNull() ?: it.reverseTempCo
            )
        }
    }

    private fun restoreOhmsLaw(payload: String) {
        val parts = payload.split(";")
        if (parts.firstOrNull() != "v2") {
            // Legacy records only stored the selected pair
            _uiState.update {
                it.copy(ohmsSelectedParams = listOf(ElectricalParameter.valueOf(parts[0]), ElectricalParameter.valueOf(parts[1])))
            }
            return
        }
        val inputs = parts.drop(1).filter { !it.startsWith("formula=") }.map { entry ->
            val (name, rest) = entry.split("=", limit = 2)
            val (value, unit) = rest.split(":", limit = 2)
            Triple(ElectricalParameter.valueOf(name), value, unit)
        }
        require(inputs.size == 2)
        var state = _uiState.value.copy(ohmsSelectedParams = inputs.map { it.first })
        inputs.forEach { (param, value, unit) ->
            state = when (param) {
                ElectricalParameter.VOLTAGE -> state.copy(ohmsVoltageInput = value, ohmsVoltageUnit = VoltageUnit.valueOf(unit))
                ElectricalParameter.CURRENT -> state.copy(ohmsCurrentInput = value, ohmsCurrentUnit = CurrentUnit.valueOf(unit))
                ElectricalParameter.RESISTANCE -> state.copy(ohmsResistanceInput = value, ohmsResistanceUnit = ResistanceUnit.valueOf(unit))
                ElectricalParameter.POWER -> state.copy(ohmsPowerInput = value, ohmsPowerUnit = PowerUnit.valueOf(unit))
            }
        }
        _uiState.value = state
    }
}
