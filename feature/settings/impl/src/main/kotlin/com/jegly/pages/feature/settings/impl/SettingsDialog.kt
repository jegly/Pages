package com.jegly.pages.feature.settings.impl

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jegly.pages.core.model.AccentColor
import com.jegly.pages.core.model.FontFamily
import com.jegly.pages.core.model.PageTurnMode
import com.jegly.pages.core.model.ReadingTheme
import com.jegly.pages.core.model.SliderTimeout

private val accentColors = mapOf(
    AccentColor.Emerald to Color(0xFF34D399),
    AccentColor.Amber to Color(0xFFFBBF24),
    AccentColor.Sapphire to Color(0xFF3B82F6),
    AccentColor.Ruby to Color(0xFFEF4444),
    AccentColor.Amethyst to Color(0xFFA855F7),
    AccentColor.Rose to Color(0xFFF43F5E),
)

private val readingThemeOptions = ReadingTheme.entries.filter { it != ReadingTheme.Custom }

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()

    val scanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) viewModel.scanAndImport(uri)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {

                // ── Appearance ────────────────────────────────────────────
                SectionLabel("Appearance", Color(0xFFb4befe))
                SettingRow(
                    label = "Material You",
                    checked = prefs.useDynamicColor,
                    onToggle = viewModel::setUseDynamicColor,
                    description = "Use your wallpaper colours as the app theme",
                )
                if (!prefs.useDynamicColor) {
                    SubLabel("Accent colour")
                    Row(
                        modifier = Modifier.fillMaxWidth().selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        accentColors.forEach { (accent, color) ->
                            IconToggleButton(
                                checked = prefs.accentColor == accent,
                                onCheckedChange = { viewModel.setAccentColor(accent) },
                            ) {
                                Surface(
                                    color = color,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.size(32.dp),
                                ) {}
                            }
                        }
                    }
                }
                SubLabel("Reading background")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    readingThemeOptions.forEachIndexed { index, theme ->
                        SegmentedButton(
                            selected = prefs.readingTheme == theme,
                            onClick = { viewModel.setReadingTheme(theme) },
                            shape = SegmentedButtonDefaults.itemShape(index, readingThemeOptions.size),
                            icon = {},
                            label = { Text(theme.name, maxLines = 1) },
                        )
                    }
                }

                // ── Typography ─────────────────────────────────────────────
                SectionLabel("Typography", Color(0xFFf5c2e7))
                SliderRow(
                    label = "Font size",
                    valueLabel = "${(prefs.fontScaleFactor * 100).toInt()}%",
                    value = prefs.fontScaleFactor,
                    range = 0.7f..2.5f,
                    onValueChange = viewModel::setFontScale,
                )
                SubLabel("Font family")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    FontFamily.entries.forEachIndexed { index, family ->
                        SegmentedButton(
                            selected = prefs.fontFamily == family,
                            onClick = { viewModel.setFontFamily(family) },
                            shape = SegmentedButtonDefaults.itemShape(index, FontFamily.entries.size),
                            icon = {},
                            label = { Text(family.name, maxLines = 1) },
                        )
                    }
                }
                SliderRow(
                    label = "Line spacing",
                    valueLabel = "${"%.1f".format(prefs.lineSpacingMultiplier)}×",
                    value = prefs.lineSpacingMultiplier,
                    range = 1.0f..2.5f,
                    onValueChange = viewModel::setLineSpacing,
                )
                SliderRow(
                    label = "Side margins",
                    valueLabel = "${prefs.horizontalMarginDp} dp",
                    value = prefs.horizontalMarginDp.toFloat(),
                    range = 4f..48f,
                    steps = 10,
                    onValueChange = { viewModel.setHorizontalMargin(it.toInt()) },
                )

                // ── Reading ────────────────────────────────────────────────
                SectionLabel("Reading", Color(0xFFa6e3a1))
                SubLabel("Page turn")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    PageTurnMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = prefs.pageTurnMode == mode,
                            onClick = { viewModel.setPageTurnMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, PageTurnMode.entries.size),
                            label = { Text(mode.name) },
                        )
                    }
                }
                SettingRow(
                    label = "Blue light filter",
                    checked = prefs.blueLightFilter,
                    onToggle = viewModel::setBlueLightFilter,
                    description = "Warm amber overlay to reduce eye strain",
                )
                SliderRow(
                    label = "Auto-scroll speed",
                    valueLabel = "${"%.1f".format(prefs.autoScrollSpeed)}×",
                    value = prefs.autoScrollSpeed,
                    range = 0.5f..5.0f,
                    onValueChange = viewModel::setAutoScrollSpeed,
                )
                SettingRow(
                    label = "Show page number",
                    checked = prefs.showPageNumber,
                    onToggle = viewModel::setShowPageNumber,
                    description = "Display current page over the progress bar",
                )
                SettingRow(
                    label = "Navigation buttons",
                    checked = prefs.showNavigationButtons,
                    onToggle = viewModel::setShowNavigationButtons,
                    description = "Show on-screen prev / next buttons while reading",
                )
                SubLabel("Page slider timeout")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = SliderTimeout.entries
                    options.forEachIndexed { index, timeout ->
                        SegmentedButton(
                            selected = prefs.sliderTimeout == timeout,
                            onClick = { viewModel.setSliderTimeout(timeout) },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                            icon = {},
                            label = {
                                Text(
                                    when (timeout) {
                                        SliderTimeout.Off -> "Off"
                                        SliderTimeout.Short -> "2s"
                                        SliderTimeout.Medium -> "3s"
                                        SliderTimeout.Long -> "5s"
                                        SliderTimeout.AlwaysOn -> "Always"
                                    },
                                    maxLines = 1,
                                )
                            },
                        )
                    }
                }

                // ── PDF ───────────────────────────────────────────────────
                SectionLabel("PDF", Color(0xFFf38ba8))
                SettingRow("High contrast (grayscale)", prefs.pdfHighContrast, viewModel::setPdfHighContrast)
                SettingRow("Crop margins", prefs.pdfCropMargins, viewModel::setPdfCropMargins)

                // ── E-ink ─────────────────────────────────────────────────
                SectionLabel("E-ink / E-paper", Color(0xFF89dceb))
                SettingRow(
                    label = "E-ink mode",
                    checked = prefs.eInkMode,
                    onToggle = viewModel::setEInkMode,
                    description = "White/black only, no animations — optimised for e-paper displays",
                )

                // ── Security ──────────────────────────────────────────────
                SectionLabel("Security", Color(0xFFfab387))
                SettingRow(
                    label = "Biometric lock",
                    checked = prefs.biometricLockEnabled,
                    onToggle = viewModel::setBiometricLockEnabled,
                    description = "Require fingerprint or PIN to open the app. When enabled, all imported files are encrypted with AES-256-GCM and stored in private app storage.",
                )
                if (prefs.biometricLockEnabled) {
                    Text(
                        text = "Encryption active — imports are stored as AES-256-GCM encrypted copies. You can safely delete the originals from your files.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                SettingRow(
                    label = "Protect PDFs",
                    checked = prefs.pdfRequireAuth,
                    onToggle = viewModel::setPdfRequireAuth,
                    description = "Prompt for biometric or PIN before opening any PDF",
                )
                SettingRow(
                    label = "Allow screenshots",
                    checked = prefs.allowScreenshots,
                    onToggle = viewModel::setAllowScreenshots,
                    description = "Permit screen capture and app switcher previews",
                )

                // ── Storage ───────────────────────────────────────────────
                SectionLabel("Storage", Color(0xFFf9e2af))
                SettingRow(
                    label = "Auto-clear cache on exit",
                    checked = prefs.autoClearCache,
                    onToggle = viewModel::setAutoClearCache,
                    description = "Delete temporary files each time the app closes",
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Cache", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Temporary render data and thumbnails",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(onClick = viewModel::clearCache) {
                        Text("Clear now")
                    }
                }

                // ── Library ───────────────────────────────────────────────
                SectionLabel("Library", Color(0xFF89b4fa))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Scan folder for books", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            when (val s = scanState) {
                                is ScanState.Idle -> "Import all PDF, EPUB, TXT, MOBI, CBZ & CBR files from a folder"
                                is ScanState.Scanning -> if (s.total > 0)
                                    "Importing ${s.done} of ${s.total}…"
                                else
                                    "Scanning folder…"
                                is ScanState.Done -> "Imported ${s.count} book(s)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (scanState is ScanState.Done)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (scanState is ScanState.Done) {
                        OutlinedButton(onClick = viewModel::dismissScanResult) {
                            Text("OK")
                        }
                    } else {
                        Button(
                            onClick = { scanLauncher.launch(null) },
                            enabled = scanState !is ScanState.Scanning,
                        ) {
                            Text("Scan")
                        }
                    }
                }

                // ── About ─────────────────────────────────────────────────
                HorizontalDivider()
                val uriHandler = LocalUriHandler.current
                SectionLabel("About", Color(0xFFcba6f7))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = buildAnnotatedString {
                            append("Developer: ")
                            withStyle(SpanStyle(color = Color(0xFF50FA7B))) { append("Jegly") }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "github.com/jegly",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFBD93F9),
                        modifier = Modifier.clickable { uriHandler.openUri("https://github.com/jegly") },
                    )
                    Text(
                        text = "www.jegly.xyz",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFBD93F9),
                        modifier = Modifier.clickable { uriHandler.openUri("https://www.jegly.xyz") },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

@Composable
private fun SectionLabel(text: String, color: Color = Color.Unspecified) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = color)
}

@Composable
private fun SubLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    steps: Int = 0,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SettingRow(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    description: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}
