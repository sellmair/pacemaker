package io.sellmair.pacemaker.ui.settingsPage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.sellmair.pacemaker.MeState
import io.sellmair.pacemaker.UtteranceState
import io.sellmair.pacemaker.UserColors
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.ui.PacemakerSpacing
import io.sellmair.pacemaker.ui.SharedRhythmColors
import io.sellmair.pacemaker.ui.toColor
import io.sellmair.pacemaker.ui.widget.PacemakerLabeledInput
import io.sellmair.pacemaker.ui.widget.PacemakerLimitControl
import io.sellmair.pacemaker.ui.widget.PacemakerSlider
import org.jetbrains.compose.resources.stringResource
import pacemaker.app.generated.resources.Res
import pacemaker.app.generated.resources.live_bpm
import pacemaker.app.generated.resources.live_increase_limit
import pacemaker.app.generated.resources.live_decrease_limit
import pacemaker.app.generated.resources.live_voice_all
import pacemaker.app.generated.resources.live_voice_off
import pacemaker.app.generated.resources.live_voice_title
import pacemaker.app.generated.resources.live_voice_warnings
import pacemaker.app.generated.resources.settings_display_name
import pacemaker.app.generated.resources.settings_hue_accessibility
import pacemaker.app.generated.resources.settings_hue_label
import pacemaker.app.generated.resources.settings_limit_accessibility
import pacemaker.app.generated.resources.settings_limit_description
import pacemaker.app.generated.resources.settings_limit_label
import pacemaker.app.generated.resources.settings_limit_range
import pacemaker.app.generated.resources.settings_name_blank
import pacemaker.app.generated.resources.settings_profile_description
import pacemaker.app.generated.resources.settings_profile_eyebrow
import pacemaker.app.generated.resources.settings_profile_title
import kotlin.math.roundToInt

data class ProfileScreenContentState(
    val me: MeState,
    val displayNameDraft: String,
    val hue: Float,
    val utteranceState: UtteranceState
)

/** The name field accepts drafts locally, but blank drafts never reach persistent user state. */
fun validProfileNameDraft(draft: String): String? = draft.takeUnless(String::isBlank)

@Composable
fun ProfilePageContent(
    state: ProfileScreenContentState,
    onDisplayNameDraftChanged: (String) -> Unit,
    onNameCommit: (String) -> Unit,
    onHeartRateLimitChanged: (HeartRate) -> Unit,
    onHueChanged: (Float) -> Unit,
    onUtteranceStateChanged: (UtteranceState) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SharedRhythmColors()
    val draft = state.displayNameDraft
    val validatedName = validProfileNameDraft(draft)
    val voiceStatus = stringResource(state.utteranceState.voiceLabel)
    var voiceMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(PacemakerSpacing.x5)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PacemakerSpacing.x2)) {
            Text(
                stringResource(Res.string.settings_profile_eyebrow),
                style = MaterialTheme.typography.labelMedium,
                color = colors.muted
            )
            Text(
                stringResource(Res.string.settings_profile_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 40.sp, lineHeight = 42.sp),
                color = colors.text
            )
            Text(
                stringResource(Res.string.settings_profile_description),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted
            )
        }

        PacemakerLabeledInput(
            label = stringResource(Res.string.settings_display_name),
            value = draft,
            onValueChange = { newDraft ->
                onDisplayNameDraftChanged(newDraft)
                validProfileNameDraft(newDraft)?.let(onNameCommit)
            },
            supportingText = if (validatedName == null) stringResource(Res.string.settings_name_blank) else null,
            isError = validatedName == null
        )

        Column(verticalArrangement = Arrangement.spacedBy(PacemakerSpacing.x2)) {
            PacemakerLimitControl(
                value = state.me.heartRateLimit.value.roundToInt(),
                onValueChange = { onHeartRateLimitChanged(HeartRate(it)) },
                valueRange = 40..200,
                limitLabel = stringResource(Res.string.settings_limit_label),
                unitLabel = stringResource(Res.string.live_bpm),
                decreaseLabel = stringResource(Res.string.live_decrease_limit),
                increaseLabel = stringResource(Res.string.live_increase_limit),
                accessibilityLabel = stringResource(Res.string.settings_limit_accessibility),
                valueStateDescription = "${state.me.heartRateLimit.value} ${stringResource(Res.string.live_bpm)}",
                outOfRangeMessage = stringResource(Res.string.settings_limit_range, 40, 200),
                storedValue = state.me.heartRateLimit.value
            )
            Text(
                stringResource(Res.string.settings_limit_description),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(PacemakerSpacing.x1)) {
            Text(
                stringResource(Res.string.settings_hue_label),
                style = MaterialTheme.typography.labelLarge,
                color = colors.text
            )
            val hue = state.hue.coerceIn(0f, 360f)
            val identityColor = UserColors.fromHue(hue).toColor()
            val hueLabel = stringResource(Res.string.settings_hue_label)
            val hueStateDescription = stringResource(Res.string.settings_hue_accessibility, hue.roundToInt())
            PacemakerSlider(
                value = hue,
                onValueChange = onHueChanged,
                valueRange = 0f..360f,
                steps = 359,
                modifier = Modifier.semantics {
                    contentDescription = hueLabel
                    stateDescription = hueStateDescription
                },
                thumbColor = identityColor,
                activeTrackColor = identityColor,
                inactiveTrackColor = colors.border
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(PacemakerSpacing.x2)) {
            BoxWithConstraints {
                val compactRow = maxWidth < 340.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.3f
                Column {
                    androidx.compose.foundation.layout.Box(
                        Modifier.fillMaxWidth().height(1.dp).background(colors.border)
                    )
                    if (compactRow) {
                        Column(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .clickable(role = Role.Button) { voiceMenuExpanded = true }
                                .semantics {
                                    role = Role.Button
                                    stateDescription = voiceStatus
                                }
                                .padding(vertical = PacemakerSpacing.x2),
                            verticalArrangement = Arrangement.spacedBy(PacemakerSpacing.x1)
                        ) {
                            Text(stringResource(Res.string.live_voice_title), color = colors.text)
                            Text(voiceStatus, color = colors.accentText)
                        }
                    } else {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .clickable(role = Role.Button) { voiceMenuExpanded = true }
                                .semantics {
                                    role = Role.Button
                                    stateDescription = voiceStatus
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(Res.string.live_voice_title), color = colors.text)
                            Text(voiceStatus, color = colors.accentText)
                        }
                    }
                    androidx.compose.foundation.layout.Box(
                        Modifier.fillMaxWidth().height(1.dp).background(colors.border)
                    )
                }
                DropdownMenu(
                    expanded = voiceMenuExpanded,
                    onDismissRequest = { voiceMenuExpanded = false }
                ) {
                    UtteranceState.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(stringResource(mode.voiceLabel)) },
                            onClick = {
                                voiceMenuExpanded = false
                                onUtteranceStateChanged(mode)
                            }
                        )
                    }
                }
            }
        }
    }
}

private val UtteranceState.voiceLabel
    get() = when (this) {
        UtteranceState.Silence -> Res.string.live_voice_off
        UtteranceState.Warnings -> Res.string.live_voice_warnings
        UtteranceState.All -> Res.string.live_voice_all
    }
