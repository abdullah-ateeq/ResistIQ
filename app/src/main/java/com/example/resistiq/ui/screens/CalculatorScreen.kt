package com.example.resistiq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resistiq.engine.OhmsLawCalculator
import com.example.resistiq.engine.PreferredValueMatch
import com.example.resistiq.engine.ResistorColorCodeCalculator
import com.example.resistiq.model.BandCount
import com.example.resistiq.model.CalculatorTab
import com.example.resistiq.model.CurrentUnit
import com.example.resistiq.model.ElectricalParameter
import com.example.resistiq.model.OhmsLawResult
import com.example.resistiq.model.PowerUnit
import com.example.resistiq.model.ResistanceUnit
import com.example.resistiq.model.ResistorColor
import com.example.resistiq.model.VoltageUnit
import com.example.resistiq.ui.components.BandSelectorDropdown
import com.example.resistiq.ui.components.ResistorVisual
import com.example.resistiq.ui.components.SafetyCard
import com.example.resistiq.viewmodel.CalculatorUiState
import com.example.resistiq.viewmodel.CalculatorViewModel
import com.example.resistiq.viewmodel.REVERSE_TOLERANCES
import java.math.BigDecimal

private val WideLayoutBreakpoint = 600.dp

/** Shared UI actions so each tool stays a plain function of state. */
private class CalculatorActions(
    val tick: () -> Unit,
    val copy: (text: String, message: String) -> Unit
)

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    onMessage: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    val actions = CalculatorActions(
        tick = {
            if (uiState.hapticFeedback) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        },
        copy = { text, message ->
            clipboardManager.setText(AnnotatedString(text))
            onMessage(message)
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "RESISTIQ",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = "Decode resistors and solve electrical values.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        PrimaryTabRow(
            selectedTabIndex = uiState.currentTab.ordinal,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            CalculatorTab.entries.forEach { tab ->
                Tab(
                    selected = uiState.currentTab == tab,
                    onClick = {
                        actions.tick()
                        viewModel.selectTab(tab)
                    },
                    text = {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (uiState.currentTab == tab) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val wide = maxWidth >= WideLayoutBreakpoint
            when (uiState.currentTab) {
                CalculatorTab.COLOR_CODE -> ColorCodeTool(uiState, viewModel, actions, wide)
                CalculatorTab.VALUE_TO_COLOR -> ValueToColorTool(uiState, viewModel, actions, wide)
                CalculatorTab.OHMS_LAW -> OhmsLawTool(uiState, viewModel, actions, wide)
            }
        }
    }
}

/** Single scrolling column on phones; two independently scrolling panes on tablets/landscape. */
@Composable
private fun ToolLayout(
    wide: Boolean,
    primary: @Composable ColumnScope.() -> Unit,
    secondary: @Composable ColumnScope.() -> Unit
) {
    val spacing = Arrangement.spacedBy(14.dp)
    if (wide) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp),
                verticalArrangement = spacing,
                content = primary
            )
            Column(
                modifier = Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp),
                verticalArrangement = spacing,
                content = secondary
            )
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = spacing
        ) {
            primary()
            secondary()
        }
    }
}

@Composable
private fun SectionLabel(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
private fun ResultSurface(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun ResistorCard(bands: List<ResistorColor>, bandCount: BandCount, selectedBandIndex: Int? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        ResistorVisual(
            bands = bands,
            bandCount = bandCount,
            selectedBandIndex = selectedBandIndex,
            height = 100.dp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun BandCountSelector(
    options: List<BandCount>,
    selected: BandCount,
    onSelect: (BandCount) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { count ->
            FilterChip(
                selected = selected == count,
                onClick = { onSelect(count) },
                label = {
                    Text(
                        count.displayName,
                        fontSize = 12.sp,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    a11yLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    enabled: Boolean = true,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.ContentCopy
) {
    val padding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    val content: @Composable () -> Unit = {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    val semanticsModifier = modifier.semantics { contentDescription = a11yLabel }
    if (filled) {
        Button(onClick = onClick, enabled = enabled, modifier = semanticsModifier, contentPadding = padding) { content() }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = semanticsModifier, contentPadding = padding) { content() }
    }
}

@Composable
private fun SaveResetRow(
    onSave: () -> Unit,
    onReset: () -> Unit,
    saveEnabled: Boolean = true
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionButton(
            label = "Save calculation",
            a11yLabel = "Save calculation",
            onClick = onSave,
            filled = true,
            enabled = saveEnabled,
            icon = Icons.Default.BookmarkBorder,
            modifier = Modifier.weight(1f)
        )
        ActionButton(
            label = "Reset",
            a11yLabel = "Reset calculator",
            onClick = onReset,
            icon = Icons.Default.Refresh
        )
    }
}

private fun spokenTolerance(percent: BigDecimal) =
    "plus or minus ${percent.stripTrailingZeros().toPlainString()} percent"

// ==========================================
// TOOL 1: COLOR CODE -> RESISTANCE
// ==========================================
@Composable
private fun ColorCodeTool(
    uiState: CalculatorUiState,
    viewModel: CalculatorViewModel,
    actions: CalculatorActions,
    wide: Boolean
) {
    val result = uiState.colorResult
    ToolLayout(
        wide = wide,
        primary = {
            BandCountSelector(BandCount.entries, uiState.bandCount) {
                actions.tick()
                viewModel.setBandCount(it)
            }

            ResistorCard(uiState.selectedBands, uiState.bandCount, uiState.lastChangedBand)

            ResultSurface {
                SectionLabel("RESISTANCE")
                Text(
                    text = result.formattedResistance,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = "Calculated resistance ${ResistorColorCodeCalculator.spokenResistance(result.resistanceOhms)}"
                    }
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(
                        modifier = Modifier.clearAndSetSemantics {
                            contentDescription = "Tolerance ${spokenTolerance(result.tolerancePercent)}"
                        }
                    ) {
                        SectionLabel("TOLERANCE")
                        Text(
                            text = result.formattedTolerance,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f).clearAndSetSemantics {
                            contentDescription = "Range ${ResistorColorCodeCalculator.spokenResistance(result.minimumOhms)} to " +
                                ResistorColorCodeCalculator.spokenResistance(result.maximumOhms)
                        },
                        horizontalAlignment = Alignment.End
                    ) {
                        SectionLabel("RANGE")
                        Text(
                            text = result.formattedRange,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                result.formattedTempCo?.let { tempCo ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionLabel("TEMPERATURE COEFFICIENT")
                        Text(
                            text = tempCo,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        },
        secondary = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                uiState.selectedBands.forEachIndexed { index, color ->
                    val role = ResistorColorCodeCalculator.bandRole(uiState.bandCount, index)
                    val roleLabel = when (role) {
                        ResistorColorCodeCalculator.BandRole.DIGIT -> "Digit ${index + 1}"
                        else -> role.label
                    }
                    BandSelectorDropdown(
                        label = "Band ${index + 1} · $roleLabel",
                        bandCount = uiState.bandCount,
                        bandIndex = index,
                        currentColor = color,
                        validColors = ResistorColorCodeCalculator.getValidColorsForBand(uiState.bandCount, index),
                        onColorSelected = {
                            actions.tick()
                            viewModel.setBandColor(index, it)
                        }
                    )
                }
            }

            if (uiState.showBandBreakdown || uiState.showFormula) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionLabel("BAND BREAKDOWN", MaterialTheme.colorScheme.primary)
                        if (uiState.showBandBreakdown) {
                            result.breakdown.forEach { item ->
                                BreakdownRow(item.bandPosition, item.color, item.meaning)
                            }
                        }
                        if (uiState.showFormula && result.calculationFormula.isNotEmpty()) {
                            Text(
                                text = result.calculationFormula,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("Value", "Copy resistance", {
                    actions.copy(viewModel.colorCodeValueText(), "Resistance copied")
                }, Modifier.weight(1f))
                ActionButton("Bands", "Copy bands", {
                    actions.copy(viewModel.colorCodeBandsText(), "Bands copied")
                }, Modifier.weight(1f))
                ActionButton("All", "Copy full result", {
                    actions.copy(viewModel.colorCodeFullText(), "Result copied")
                }, Modifier.weight(1f))
            }
            SaveResetRow(
                onSave = {
                    actions.tick()
                    viewModel.saveColorCodeCalculation()
                },
                onReset = {
                    actions.tick()
                    viewModel.resetColorCode()
                }
            )
            SafetyCard()
        }
    )
}

@Composable
private fun ColorSwatch(color: ResistorColor, size: androidx.compose.ui.unit.Dp = 14.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(color.color, RoundedCornerShape(3.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
    )
}

@Composable
private fun BreakdownRow(position: String, color: ResistorColor?, meaning: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = position,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp)
        )
        if (color != null) {
            ColorSwatch(color)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = color?.displayName ?: "None",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "→ $meaning",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

// ==========================================
// TOOL 2: RESISTANCE -> COLOR
// ==========================================
@Composable
private fun ValueToColorTool(
    uiState: CalculatorUiState,
    viewModel: CalculatorViewModel,
    actions: CalculatorActions,
    wide: Boolean
) {
    val focusManager = LocalFocusManager.current
    val rev = uiState.reverseResult
    val hasBands = rev != null && rev.bands.isNotEmpty()

    ToolLayout(
        wide = wide,
        primary = {
            SectionLabel("TARGET RESISTANCE")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = uiState.reverseInput,
                    onValueChange = viewModel::setReverseInput,
                    label = { Text("Resistance") },
                    isError = uiState.reverseInputError != null,
                    supportingText = uiState.reverseInputError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                UnitDropdown(
                    current = uiState.reverseUnit.symbol,
                    options = listOf(ResistanceUnit.OHM, ResistanceUnit.KILOHM, ResistanceUnit.MEGAOHM, ResistanceUnit.GIGAOHM),
                    label = { it.symbol },
                    onSelect = viewModel::setReverseUnit,
                    a11yLabel = "Resistance unit"
                )
            }

            SectionLabel("BAND COUNT")
            BandCountSelector(BandCount.entries, uiState.reverseBandCount) {
                actions.tick()
                viewModel.setReverseBandCount(it)
            }

            SectionLabel("TOLERANCE")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                REVERSE_TOLERANCES.forEach { tol ->
                    FilterChip(
                        selected = uiState.reverseTolerance.compareTo(tol) == 0,
                        onClick = {
                            actions.tick()
                            viewModel.setReverseTolerance(tol)
                        },
                        label = {
                            Text(
                                "±${tol.toPlainString()}%",
                                fontSize = 11.sp,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (uiState.reverseBandCount == BandCount.THREE) {
                Text(
                    "±20% is a 3-band resistor with no tolerance band.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.reverseBandCount == BandCount.SIX) {
                SectionLabel("TEMPERATURE COEFFICIENT")
                UnitDropdown(
                    current = "${uiState.reverseTempCo} ppm/°C",
                    options = ResistorColorCodeCalculator.tempCoColors.mapNotNull { ResistorColorCodeCalculator.colorToTempCo(it) }.sortedDescending(),
                    label = { "$it ppm/°C" },
                    onSelect = viewModel::setReverseTempCo,
                    a11yLabel = "Temperature coefficient",
                    fillWidth = true
                )
            }

            if (hasBands && rev != null) {
                ResistorCard(rev.bands, uiState.reverseBandCount)
            }
        },
        secondary = {
            if (rev != null) {
                ResultSurface {
                    SectionLabel("TARGET")
                    Text(
                        text = ResistorColorCodeCalculator.formatResistance(rev.targetResistance, uiState.decimalPlaces),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clearAndSetSemantics {
                            contentDescription = "Target resistance ${ResistorColorCodeCalculator.spokenResistance(rev.targetResistance)}"
                        }
                    )

                    rev.errorMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (hasBands) {
                        if (!rev.isExact) {
                            SectionLabel("NEAREST REPRESENTATION")
                            Text(
                                text = "${ResistorColorCodeCalculator.formatResistance(rev.exactResistance, uiState.decimalPlaces)}  " +
                                    "(${rev.percentageDifference.toPlainString()}% difference)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        SectionLabel("BANDS")
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            rev.bands.forEachIndexed { index, color ->
                                val role = ResistorColorCodeCalculator.bandRole(uiState.reverseBandCount, index)
                                BreakdownRow(
                                    position = if (role == ResistorColorCodeCalculator.BandRole.DIGIT) "Band ${index + 1}" else role.label,
                                    color = color,
                                    meaning = ResistorColorCodeCalculator.describeBand(uiState.reverseBandCount, index, color)
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.showESeries && uiState.reversePreferredMatches.isNotEmpty() && rev != null) {
                PreferredValueCard(uiState.reversePreferredMatches, uiState.decimalPlaces)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(
                    label = "Copy bands",
                    a11yLabel = "Copy bands",
                    onClick = { viewModel.reverseBandsText()?.let { actions.copy(it, "Bands copied") } },
                    enabled = hasBands,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    label = "Save",
                    a11yLabel = "Save calculation",
                    onClick = {
                        actions.tick()
                        viewModel.saveReverseCalculation()
                    },
                    filled = true,
                    enabled = hasBands,
                    icon = Icons.Default.BookmarkBorder,
                    modifier = Modifier.weight(1f)
                )
            }
            SafetyCard()
        }
    )
}

@Composable
private fun PreferredValueCard(matches: List<PreferredValueMatch>, decimals: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("STANDARD VALUE", MaterialTheme.colorScheme.primary)
            matches.forEach { match ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = match.formattedStatus,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (match.isExactMatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (!match.isExactMatch) {
                        Text(
                            text = "Nearest ${match.series.seriesName}: " +
                                "${ResistorColorCodeCalculator.formatResistance(match.nearestValue, decimals)} " +
                                "(${match.percentageDifference.toPlainString()}%)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Text(
                text = "Nearest values are shown for reference only and are not necessarily interchangeable.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==========================================
// TOOL 3: OHM'S LAW
// ==========================================
private val ElectricalParameter.spokenUnit: String
    get() = when (this) {
        ElectricalParameter.VOLTAGE -> "volts"
        ElectricalParameter.CURRENT -> "amperes"
        ElectricalParameter.RESISTANCE -> "ohms"
        ElectricalParameter.POWER -> "watts"
    }

private fun OhmsLawResult.valueOf(param: ElectricalParameter): BigDecimal = when (param) {
    ElectricalParameter.VOLTAGE -> voltage
    ElectricalParameter.CURRENT -> current
    ElectricalParameter.RESISTANCE -> resistance
    ElectricalParameter.POWER -> power
}

@Composable
private fun OhmsLawTool(
    uiState: CalculatorUiState,
    viewModel: CalculatorViewModel,
    actions: CalculatorActions,
    wide: Boolean
) {
    val res = uiState.ohmsResult
    ToolLayout(
        wide = wide,
        primary = {
            SectionLabel("SELECT TWO KNOWN VALUES")
            ElectricalParameter.entries.chunked(2).forEach { rowParams ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowParams.forEach { param ->
                        val isSelected = param in uiState.ohmsSelectedParams
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                actions.tick()
                                viewModel.toggleOhmsParameter(param)
                            },
                            label = {
                                Text(
                                    text = "${if (isSelected) "✓ " else ""}${param.displayName.uppercase()}",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            ElectricalParameter.entries.filter { it in uiState.ohmsSelectedParams }.forEach { param ->
                when (param) {
                    ElectricalParameter.VOLTAGE -> ParameterInputField(
                        label = "Voltage",
                        value = uiState.ohmsVoltageInput,
                        onValueChange = viewModel::setOhmsVoltageInput,
                        currentUnit = uiState.ohmsVoltageUnit,
                        units = VoltageUnit.entries,
                        unitLabel = { it.symbol },
                        onUnitSelected = viewModel::setOhmsVoltageUnit
                    )
                    ElectricalParameter.CURRENT -> ParameterInputField(
                        label = "Current",
                        value = uiState.ohmsCurrentInput,
                        onValueChange = viewModel::setOhmsCurrentInput,
                        currentUnit = uiState.ohmsCurrentUnit,
                        units = CurrentUnit.entries,
                        unitLabel = { it.symbol },
                        onUnitSelected = viewModel::setOhmsCurrentUnit
                    )
                    ElectricalParameter.RESISTANCE -> ParameterInputField(
                        label = "Resistance",
                        value = uiState.ohmsResistanceInput,
                        onValueChange = viewModel::setOhmsResistanceInput,
                        currentUnit = uiState.ohmsResistanceUnit,
                        units = ResistanceUnit.entries,
                        unitLabel = { it.symbol },
                        onUnitSelected = viewModel::setOhmsResistanceUnit
                    )
                    ElectricalParameter.POWER -> ParameterInputField(
                        label = "Power",
                        value = uiState.ohmsPowerInput,
                        onValueChange = viewModel::setOhmsPowerInput,
                        currentUnit = uiState.ohmsPowerUnit,
                        units = PowerUnit.entries,
                        unitLabel = { it.symbol },
                        onUnitSelected = viewModel::setOhmsPowerUnit
                    )
                }
            }

            uiState.ohmsError?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        secondary = {
            if (res != null) {
                ResultSurface {
                    SectionLabel("RESULTS")
                    ElectricalParameter.entries.chunked(2).forEach { rowParams ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowParams.forEach { param ->
                                OhmsResultBox(
                                    param = param,
                                    value = res.valueOf(param),
                                    decimals = uiState.decimalPlaces,
                                    isInput = param in res.inputParameters,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    if (uiState.showFormula && res.formulas.isNotEmpty()) {
                        SectionLabel("FORMULAS", MaterialTheme.colorScheme.primary)
                        res.formulas.forEach { formula ->
                            Text(
                                text = formula,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else if (uiState.ohmsError == null) {
                Text(
                    text = "Enter both known values to calculate the rest.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(
                    label = "Copy values",
                    a11yLabel = "Copy calculated values",
                    onClick = { viewModel.ohmsLawCopyText()?.let { actions.copy(it, "Values copied") } },
                    enabled = res != null,
                    modifier = Modifier.weight(1f)
                )
            }
            SaveResetRow(
                onSave = {
                    actions.tick()
                    viewModel.saveOhmsLawCalculation()
                },
                onReset = {
                    actions.tick()
                    viewModel.resetOhmsLaw()
                },
                saveEnabled = res != null
            )
            SafetyCard()
        }
    )
}

@Composable
private fun <T> UnitDropdown(
    current: String,
    options: List<T>,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    a11yLabel: String,
    fillWidth: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .height(56.dp)
                .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .semantics { contentDescription = "$a11yLabel: $current" },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 14.dp)
        ) {
            Text(text = current, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option), fontWeight = FontWeight.Medium) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun <T> ParameterInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    currentUnit: T,
    units: List<T>,
    unitLabel: (T) -> String,
    onUnitSelected: (T) -> Unit
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        UnitDropdown(
            current = unitLabel(currentUnit),
            options = units,
            label = unitLabel,
            onSelect = onUnitSelected,
            a11yLabel = "$label unit"
        )
    }
}

@Composable
private fun OhmsResultBox(
    param: ElectricalParameter,
    value: BigDecimal,
    decimals: Int,
    isInput: Boolean,
    modifier: Modifier = Modifier
) {
    val formatted = OhmsLawCalculator.formatValue(value, param, decimals)
    val spokenNumber = value.setScale(decimals, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    Card(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "${if (isInput) "Input" else "Calculated"} ${param.displayName.lowercase()} $spokenNumber ${param.spokenUnit}"
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isInput) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = param.displayName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatted,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isInput) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isInput) "INPUT" else "CALCULATED",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isInput) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
            )
        }
    }
}
