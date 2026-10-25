package io.sellmair.pacemaker.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.text.selection.TextSelectionColors
import io.sellmair.pacemaker.ui.PacemakerIcons
import io.sellmair.pacemaker.ui.PacemakerSpacing
import io.sellmair.pacemaker.ui.SharedRhythmColors

enum class PacemakerNoticeTone { Success, Attention, Danger, Information }

@Composable
fun PacemakerPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick,
        modifier.heightIn(min = 48.dp),
        enabled,
        shape = MaterialTheme.shapes.small
    ) { Text(text) }
}

@Composable
fun PacemakerSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick,
        modifier.heightIn(min = 48.dp),
        enabled,
        shape = MaterialTheme.shapes.small
    ) { Text(text) }
}

@Composable
fun PacemakerStatusNotice(
    title: String,
    message: String,
    tone: PacemakerNoticeTone,
    modifier: Modifier = Modifier
) {
    val colors = SharedRhythmColors()
    val semantic = when (tone) {
        PacemakerNoticeTone.Success -> colors.success
        PacemakerNoticeTone.Attention -> colors.warning
        PacemakerNoticeTone.Danger -> colors.danger
        PacemakerNoticeTone.Information -> colors.info
    }
    val icon = when (tone) {
        PacemakerNoticeTone.Success -> PacemakerIcons.Check
        PacemakerNoticeTone.Attention, PacemakerNoticeTone.Danger -> PacemakerIcons.Warning
        PacemakerNoticeTone.Information -> PacemakerIcons.Info
    }
    Row(
        modifier.fillMaxWidth()
            .background(semantic.surface)
            .drawBehind {
                drawRect(
                    color = semantic.ink,
                    size = Size(width = 4.dp.toPx(), height = size.height)
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier.fillMaxWidth().padding(
                start = PacemakerSpacing.x5,
                end = PacemakerSpacing.x4,
                top = PacemakerSpacing.x4,
                bottom = PacemakerSpacing.x4
            ),
            horizontalArrangement = Arrangement.spacedBy(PacemakerSpacing.x3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = semantic.ink,
                modifier = Modifier.size(24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(PacemakerSpacing.x1)) {
                Text(title, color = semantic.ink, style = MaterialTheme.typography.titleSmall)
                Text(message, color = semantic.ink, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun PacemakerMemberRow(
    name: String,
    initials: String,
    heartRateBpm: Int?,
    limitBpm: Int?,
    identityColor: Color,
    modifier: Modifier = Modifier,
    limitLabel: String? = null,
    readingStatus: String? = null,
    limitLabelColor: Color? = null
) {
    val colors = SharedRhythmColors()
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(vertical = PacemakerSpacing.x2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).background(colors.raised),
                contentAlignment = Alignment.Center
            ) {
                Text(initials.uppercase(), color = colors.text, style = MaterialTheme.typography.labelLarge)
                Box(
                    Modifier.align(Alignment.CenterStart)
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(identityColor)
                )
            }
            Spacer(Modifier.width(PacemakerSpacing.x3))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    limitLabel ?: limitBpm?.let { "Limit $it bpm" } ?: "Limit not set",
                    style = MaterialTheme.typography.bodySmall,
                    color = limitLabelColor ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
                readingStatus?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    heartRateBpm?.toString() ?: "—",
                    style = PacemakerMetricStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text("bpm", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
    }
}

@Composable
fun PacemakerDeviceCard(
    title: String,
    detail: String,
    icon: ImageVector,
    selected: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    secondaryActionEnabled: Boolean = true
) {
    val colors = SharedRhythmColors()
    Card(
        modifier.fillMaxWidth()
            .semantics { this.selected = selected }
            .border(1.dp, if (selected) colors.accentText else colors.border, MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val secondaryActionBelow =
                LocalDensity.current.fontScale >= 1.2f || maxWidth < 340.dp
            Column(
                Modifier.padding(PacemakerSpacing.x4),
                verticalArrangement = Arrangement.spacedBy(PacemakerSpacing.x3)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PacemakerSpacing.x3)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(PacemakerSpacing.x1)
                    ) {
                        Text(title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (selected) {
                        Icon(
                            PacemakerIcons.Check,
                            contentDescription = null,
                            tint = colors.accentText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (!secondaryActionBelow && secondaryActionLabel != null && onSecondaryAction != null) {
                        TextButton(
                            onClick = onSecondaryAction,
                            enabled = secondaryActionEnabled,
                            modifier = Modifier.heightIn(min = 40.dp),
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = colors.text,
                                disabledContentColor = colors.muted
                            )
                        ) {
                            Text(secondaryActionLabel)
                        }
                    }
                }
                if (secondaryActionBelow && secondaryActionLabel != null && onSecondaryAction != null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = onSecondaryAction,
                            enabled = secondaryActionEnabled,
                            modifier = Modifier.heightIn(min = 40.dp),
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = colors.text,
                                disabledContentColor = colors.muted
                            )
                        ) {
                            Text(secondaryActionLabel)
                        }
                    }
                }
                PacemakerSecondaryButton(actionLabel, onAction, Modifier.fillMaxWidth(), enabled)
            }
        }
    }
}

@Composable
fun PacemakerLabeledInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true
) {
    val colors = SharedRhythmColors()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PacemakerSpacing.x1)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = colors.text
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            enabled = enabled,
            isError = isError,
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                disabledContainerColor = colors.surface,
                errorContainerColor = colors.surface,
                cursorColor = colors.text,
                selectionColors = TextSelectionColors(
                    handleColor = colors.accentText,
                    backgroundColor = colors.accentText.copy(alpha = 0.34f)
                ),
                focusedBorderColor = colors.border,
                unfocusedBorderColor = colors.border,
                errorBorderColor = colors.danger.ink,
                focusedLabelColor = colors.accentText,
                unfocusedLabelColor = colors.muted,
                errorLabelColor = colors.danger.ink,
                focusedSupportingTextColor = colors.muted,
                unfocusedSupportingTextColor = colors.muted,
                errorSupportingTextColor = colors.danger.ink
            ),
            supportingText = supportingText?.let { note -> ({ Text(note) }) },
            singleLine = true,
            shape = MaterialTheme.shapes.small
        )
    }
}

@Composable
fun PacemakerLimitControl(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: IntRange = 40..200,
    enabled: Boolean = true,
    limitLabel: String = "Your limit",
    unitLabel: String = "bpm",
    decreaseLabel: String = "Decrease limit",
    increaseLabel: String = "Increase limit",
    accessibilityLabel: String = "Your heart-rate limit",
    valueStateDescription: String? = null,
    outOfRangeMessage: String? = null,
    storedValue: Float = value.toFloat()
) {
    require(valueRange.first < valueRange.last) { "valueRange must contain at least two values" }
    val sliderValue = value.coerceIn(valueRange.first, valueRange.last)
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PacemakerSpacing.x2)
    ) {
        Text(limitLabel, style = MaterialTheme.typography.labelLarge, color = SharedRhythmColors().muted)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PacemakerSpacing.x2)
        ) {
            OutlinedButton(
                onClick = { onValueChange((value - 1).coerceIn(valueRange.first, valueRange.last)) },
                modifier = Modifier.size(48.dp),
                enabled = enabled && value > valueRange.first,
                shape = MaterialTheme.shapes.small,
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = SharedRhythmColors().surface,
                    contentColor = SharedRhythmColors().text
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Icon(PacemakerIcons.Minus, contentDescription = decreaseLabel, tint = SharedRhythmColors().text)
            }
            BoxWithConstraints(Modifier.weight(1f)) {
                val fontScale = LocalDensity.current.fontScale
                val metricSize = if (fontScale >= 1.3f || maxWidth < 220.dp) 56.sp else 72.sp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        value.toString(),
                        textAlign = TextAlign.Center,
                        style = PacemakerMetricStyle.copy(fontSize = metricSize, lineHeight = metricSize),
                        color = SharedRhythmColors().text
                    )
                    Spacer(Modifier.width(PacemakerSpacing.x1))
                    Text(
                        unitLabel,
                        modifier = Modifier.padding(bottom = if (metricSize == 56.sp) 6.dp else 10.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = SharedRhythmColors().muted,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            OutlinedButton(
                onClick = { onValueChange((value + 1).coerceIn(valueRange.first, valueRange.last)) },
                modifier = Modifier.size(48.dp),
                enabled = enabled && value < valueRange.last,
                shape = MaterialTheme.shapes.small,
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = SharedRhythmColors().surface,
                    contentColor = SharedRhythmColors().text
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Icon(PacemakerIcons.Plus, contentDescription = increaseLabel, tint = SharedRhythmColors().text)
            }
        }
        PacemakerSlider(
            modifier = Modifier.semantics {
                contentDescription = accessibilityLabel
                stateDescription = valueStateDescription ?: "$value bpm"
            },
            value = sliderValue.toFloat(),
            onValueChange = {
                onValueChange(
                    it.toInt().coerceIn(valueRange.first, valueRange.last)
                )
            },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = valueRange.last - valueRange.first - 1,
            enabled = enabled,
            thumbColor = SharedRhythmColors().accentText,
            activeTrackColor = SharedRhythmColors().accentText,
            inactiveTrackColor = SharedRhythmColors().border
        )
        if (storedValue < valueRange.first || storedValue > valueRange.last) {
            Text(
                outOfRangeMessage
                    ?: "Stored value is outside the supported edit range (${valueRange.first}–${valueRange.last} bpm).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun PacemakerSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    enabled: Boolean = true,
    thumbColor: Color,
    activeTrackColor: Color,
    inactiveTrackColor: Color
) {
    val sliderColors = SliderDefaults.colors(
        thumbColor = thumbColor,
        activeTrackColor = activeTrackColor,
        inactiveTrackColor = inactiveTrackColor
    )
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        steps = steps,
        enabled = enabled,
        colors = sliderColors,
        thumb = {
            Box(Modifier.size(20.dp).background(thumbColor, CircleShape))
        },
        track = { sliderState ->
            Canvas(Modifier.fillMaxWidth().height(20.dp)) {
                val centerY = size.height / 2f
                val start = Offset(0f, centerY)
                val end = Offset(size.width, centerY)
                val fraction = sliderState.coercedValueAsFraction
                val activeEnd = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl) {
                    Offset(size.width * (1f - fraction), centerY)
                } else {
                    Offset(size.width * fraction, centerY)
                }
                drawLine(inactiveTrackColor, start, end, 6.dp.toPx(), StrokeCap.Round)
                drawLine(
                    activeTrackColor,
                    if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl) end else start,
                    activeEnd,
                    6.dp.toPx(),
                    StrokeCap.Round
                )
            }
        }
    )
}

private val PacemakerMetricStyle
    @Composable get() = MaterialTheme.typography.displaySmall.copy(
        fontSize = 32.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = "tnum"
    )
