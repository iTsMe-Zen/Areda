package app.areada.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.areada.R
import app.areada.data.ReaderFontChoice
import app.areada.data.ReaderLanguageMode
import app.areada.data.ReaderOrientationMode
import app.areada.data.ReaderPreferences
import app.areada.data.ReaderRulerPositionMax
import app.areada.data.ReaderRulerPositionMin
import app.areada.data.ReaderThemeMode
import app.areada.data.readingRulerPositionLabel
import app.areada.data.sanitizeReadingRulerPosition
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderSettingsSheet(
    preferences: ReaderPreferences,
    showPdfNote: Boolean,
    showReadingControls: Boolean = true,
    showLanguageSelector: Boolean = false,
    onBookNoteClick: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onPreferencesChange: (ReaderPreferences) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var fontSizeDraft by rememberSaveable {
        mutableFloatStateOf(preferences.fontSizeSp.toFloat())
    }
    var lineSpacingDraft by rememberSaveable {
        mutableFloatStateOf(preferences.lineSpacing)
    }
    var rulerPositionDraft by rememberSaveable {
        mutableFloatStateOf(preferences.readingRulerPosition)
    }
    var languageExpanded by rememberSaveable {
        androidx.compose.runtime.mutableStateOf(false)
    }
    val maxSheetHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.50f).coerceAtLeast(300.dp)

    LaunchedEffect(preferences.fontSizeSp) {
        fontSizeDraft = preferences.fontSizeSp.toFloat()
    }
    LaunchedEffect(preferences.lineSpacing) {
        lineSpacingDraft = preferences.lineSpacing
    }
    LaunchedEffect(preferences.readingRulerPosition) {
        rulerPositionDraft = preferences.readingRulerPosition
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = SettingsSheetHorizontalPadding, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                onBookNoteClick?.let { openBookNote ->
                    Text(
                        text = stringResource(R.string.book_note),
                        modifier = Modifier
                            .clickable(onClick = openBookNote)
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            SettingsSection(title = stringResource(R.string.theme)) {
                SegmentedSettingGrid(
                    items = ReaderThemeMode.entries,
                    selected = preferences.themeMode,
                    label = { mode -> mode.displayLabel() },
                    onSelect = { mode -> onPreferencesChange(preferences.copy(themeMode = mode)) },
                )
            }
            if (showReadingControls) {
                SettingsSectionSpacer()
                SettingsSection(title = stringResource(R.string.orientation)) {
                    SegmentedSettingGrid(
                        items = ReaderOrientationMode.entries,
                        selected = preferences.orientationMode,
                        label = { mode -> mode.displayLabel() },
                        onSelect = { mode -> onPreferencesChange(preferences.copy(orientationMode = mode)) },
                    )
                }
            }
            if (showReadingControls) {
                SettingsSectionSpacer()
                SettingsSection(title = stringResource(R.string.font)) {
                    SegmentedSettingGrid(
                        items = ReaderFontChoice.entries,
                        selected = preferences.fontChoice,
                        label = { choice -> choice.displayLabel() },
                        onSelect = { choice -> onPreferencesChange(preferences.copy(fontChoice = choice)) },
                    )
                }
                SettingsSectionSpacer()
                SettingsSlider(
                    label = stringResource(R.string.font_size),
                    valueLabel = "${fontSizeDraft.roundToInt().coerceIn(14, 30)}sp",
                    value = fontSizeDraft,
                    onValueChange = { value -> fontSizeDraft = value },
                    onValueChangeFinished = {
                        onPreferencesChange(
                            preferences.copy(fontSizeSp = fontSizeDraft.roundToInt().coerceIn(14, 30)),
                        )
                    },
                    valueRange = 14f..30f,
                    steps = 15,
                )
                SettingsControlSpacer()
                SettingsSlider(
                    label = stringResource(R.string.line_spacing),
                    valueLabel = "${(lineSpacingDraft * 10f).roundToInt() / 10f}x",
                    value = lineSpacingDraft,
                    onValueChange = { value -> lineSpacingDraft = value },
                    onValueChangeFinished = {
                        onPreferencesChange(preferences.copy(lineSpacing = lineSpacingDraft.coerceIn(1.2f, 2.4f)))
                    },
                    valueRange = 1.2f..2.4f,
                    steps = 11,
                )
                if (showPdfNote) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.pdf_font_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SettingsControlSpacer()
                SettingsBinaryRow(
                    label = stringResource(R.string.reading_ruler),
                    checked = preferences.readingRuler,
                    onCheckedChange = { checked ->
                        onPreferencesChange(
                            preferences.copy(
                                readingRuler = checked,
                                readingRulerPosition = sanitizeReadingRulerPosition(rulerPositionDraft),
                            ),
                        )
                    },
                )
                if (preferences.readingRuler) {
                    SettingsControlSpacer()
                    SettingsSlider(
                        label = stringResource(R.string.ruler_position),
                        valueLabel = readingRulerPositionLabel(rulerPositionDraft),
                        value = rulerPositionDraft,
                        onValueChange = { value -> rulerPositionDraft = value },
                        onValueChangeFinished = {
                            val sanitized = sanitizeReadingRulerPosition(rulerPositionDraft)
                            rulerPositionDraft = sanitized
                            onPreferencesChange(preferences.copy(readingRulerPosition = sanitized))
                        },
                        valueRange = ReaderRulerPositionMin..ReaderRulerPositionMax,
                        steps = 13,
                    )
                }
                SettingsBinaryRow(
                    label = stringResource(R.string.keep_screen_on),
                    checked = preferences.keepScreenOn,
                    onCheckedChange = { checked -> onPreferencesChange(preferences.copy(keepScreenOn = checked)) },
                )
                SettingsBinaryRow(
                    label = stringResource(R.string.volume_buttons_turn_pages),
                    checked = preferences.volumeButtonsTurnPages,
                    onCheckedChange = { checked ->
                        onPreferencesChange(preferences.copy(volumeButtonsTurnPages = checked))
                    },
                )
                if (preferences.volumeButtonsTurnPages) {
                    SettingsBinaryRow(
                        label = stringResource(R.string.invert_volume_buttons),
                        checked = preferences.invertVolumeButtons,
                        onCheckedChange = { checked ->
                            onPreferencesChange(preferences.copy(invertVolumeButtons = checked))
                        },
                    )
                }
            }
            if (showLanguageSelector) {
                SettingsSectionSpacer()
                LanguageSelector(
                    selected = preferences.languageMode,
                    expanded = languageExpanded,
                    onToggleExpanded = { languageExpanded = !languageExpanded },
                    onSelect = { mode ->
                        onPreferencesChange(preferences.copy(languageMode = mode))
                    },
                )
            }
            Spacer(modifier = Modifier.height(22.dp))
            VersionLine()
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun SettingsSectionSpacer() {
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
private fun SettingsControlSpacer() {
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun <T> SegmentedSettingGrid(
    items: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SettingsButtonGap),
    ) {
        items.chunked(SettingsGridColumns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SettingsButtonGap),
            ) {
                rowItems.forEach { item ->
                    SettingChip(
                        label = label(item),
                        selected = item == selected,
                        onClick = { onSelect(item) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(SettingsGridColumns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LanguageSelector(
    selected: ReaderLanguageMode,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onSelect: (ReaderLanguageMode) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clickable(onClick = onToggleExpanded),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.language),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.graphicsLayer {
                    rotationZ = if (expanded) 180f else 0f
                },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            Spacer(modifier = Modifier.height(10.dp))
            SegmentedSettingGrid(
                items = ReaderLanguageMode.entries,
                selected = selected,
                label = { mode -> mode.displayLabel() },
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun SettingsSlider(
    label: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsLabelValueRow(label = label, value = valueLabel)
        Spacer(modifier = Modifier.height(6.dp))
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
        )
    }
}

@Composable
private fun SettingsBinaryRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val contentAlpha = if (enabled) 1f else 0.45f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            maxLines = 1,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SettingsBinaryButton(
                label = stringResource(R.string.on),
                selected = checked,
                enabled = enabled,
                onClick = { onCheckedChange(true) },
            )
            SettingsBinaryButton(
                label = stringResource(R.string.off),
                selected = !checked,
                enabled = enabled,
                onClick = { onCheckedChange(false) },
            )
        }
    }
}

@Composable
private fun SettingsBinaryButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentAlpha = if (enabled) 1f else 0.45f
    Surface(
        modifier = Modifier
            .width(52.dp)
            .height(34.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RectangleShape,
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = contentAlpha)
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = contentAlpha)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                },
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SettingsLabelValueRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun VersionLine() {
    val context = LocalContext.current
    val versionName = remember(context) {
        @Suppress("DEPRECATION")
        runCatching {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName
                .orEmpty()
        }.getOrDefault("")
    }.ifBlank { "1.1.0" }

    Text(
        text = stringResource(R.string.version_areada, versionName),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private val SettingsSheetHorizontalPadding = 24.dp
private val SettingsButtonGap = 8.dp
private const val SettingsGridColumns = 3

@Composable
private fun ReaderThemeMode.displayLabel(): String =
    when (this) {
        ReaderThemeMode.LIGHT -> stringResource(R.string.theme_day)
        ReaderThemeMode.SEPIA -> stringResource(R.string.theme_sepia)
        ReaderThemeMode.DARK -> stringResource(R.string.theme_dark)
        ReaderThemeMode.SAGE -> stringResource(R.string.theme_sage)
        ReaderThemeMode.BLUSH -> stringResource(R.string.theme_blush)
        ReaderThemeMode.ANDROID -> stringResource(R.string.theme_system)
    }

@Composable
private fun ReaderFontChoice.displayLabel(): String =
    when (this) {
        ReaderFontChoice.SERIF -> stringResource(R.string.font_serif)
        ReaderFontChoice.SANS -> stringResource(R.string.font_sans)
        ReaderFontChoice.MONO -> stringResource(R.string.font_mono)
    }

@Composable
private fun ReaderOrientationMode.displayLabel(): String =
    when (this) {
        ReaderOrientationMode.Portrait -> stringResource(R.string.orientation_portrait)
        ReaderOrientationMode.Landscape -> stringResource(R.string.orientation_landscape)
        ReaderOrientationMode.FollowSystem -> stringResource(R.string.orientation_follow_system)
    }

@Composable
private fun ReaderLanguageMode.displayLabel(): String =
    when (this) {
        ReaderLanguageMode.System -> stringResource(R.string.language_system)
        ReaderLanguageMode.English -> stringResource(R.string.language_english)
        ReaderLanguageMode.Nepali -> stringResource(R.string.language_nepali)
    }
