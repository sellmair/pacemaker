package io.sellmair.pacemaker.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.sellmair.evas.compose.composeFlow
import io.sellmair.pacemaker.MeColorState
import io.sellmair.pacemaker.UserColors
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import pacemaker.app.generated.resources.Res
import pacemaker.app.generated.resources.BarlowCondensed_SemiBold
import pacemaker.app.generated.resources.Inter_Variable
import org.jetbrains.compose.resources.Font as ComposeFont

/**
 * Participant identity color is user data. It deliberately stays separate from brand and status colors.
 */
interface PacemakerTheme {
    val meColor: Color
    val meColorLight: Color
    val meColorWhite: Color
}

private val PacemakerThemeCompositionLocal =
    compositionLocalOf<PacemakerTheme> { MissingPacemakerTheme }
private val SharedRhythmColorsLocal = compositionLocalOf { SharedRhythmColors.Dark }

@Composable
fun PacemakerTheme(): PacemakerTheme = PacemakerThemeCompositionLocal.current

@Composable
fun MeColor() = PacemakerTheme().meColor

@Composable
fun MeColorLight() = PacemakerTheme().meColorLight

@Composable
fun MeColorWhite() = PacemakerTheme().meColorWhite

@Composable
fun SharedRhythmColors(): SharedRhythmColors = SharedRhythmColorsLocal.current

@Composable
fun PacemakerTheme(content: @Composable () -> Unit) {
    val identityTheme by MeColorState.composeFlow()
        .filterNotNull()
        .map { state ->
            PacemakerThemeImpl(
                meColor = state.color.toColor(),
                meColorLight = UserColors.fromHueLight(state.color.hue).toColor(),
                meColorWhite = state.color.copy(lightness = .95f).toColor()
            )
        }
        .distinctUntilChanged()
        .collectAsState(MissingPacemakerTheme)

    CompositionLocalProvider(PacemakerThemeCompositionLocal provides identityTheme) {
        SharedRhythmTheme(content = content)
    }
}

@Composable
fun SharedRhythmTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (dark) SharedRhythmColors.Dark else SharedRhythmColors.Light
    val colorScheme = if (dark) {
        darkColorScheme(
            primary = SharedRhythmColors.Acid,
            onPrimary = SharedRhythmColors.Charcoal,
            primaryContainer = SharedRhythmColors.Acid,
            onPrimaryContainer = SharedRhythmColors.Charcoal,
            secondary = colors.accentText,
            onSecondary = SharedRhythmColors.Charcoal,
            secondaryContainer = colors.raised,
            onSecondaryContainer = colors.text,
            tertiary = colors.info.ink,
            onTertiary = SharedRhythmColors.Charcoal,
            tertiaryContainer = colors.info.surface,
            onTertiaryContainer = colors.info.ink,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.raised,
            onSurfaceVariant = colors.muted,
            inverseSurface = colors.text,
            inverseOnSurface = colors.background,
            inversePrimary = SharedRhythmColors.Light.accentText,
            outline = colors.border,
            outlineVariant = colors.border,
            error = colors.danger.ink,
            onError = SharedRhythmColors.Charcoal,
            errorContainer = colors.danger.surface,
            onErrorContainer = colors.text,
            surfaceTint = colors.surface,
            surfaceDim = colors.background,
            surfaceBright = colors.raised,
            surfaceContainerLowest = colors.background,
            surfaceContainerLow = colors.background,
            surfaceContainer = colors.surface,
            surfaceContainerHigh = colors.raised,
            surfaceContainerHighest = colors.raised
        )
    } else {
        lightColorScheme(
            primary = SharedRhythmColors.Acid,
            onPrimary = SharedRhythmColors.Charcoal,
            primaryContainer = SharedRhythmColors.Acid,
            onPrimaryContainer = SharedRhythmColors.Charcoal,
            secondary = colors.accentText,
            onSecondary = Color.White,
            secondaryContainer = colors.raised,
            onSecondaryContainer = colors.text,
            tertiary = colors.info.ink,
            onTertiary = Color.White,
            tertiaryContainer = colors.info.surface,
            onTertiaryContainer = colors.info.ink,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.raised,
            onSurfaceVariant = colors.muted,
            inverseSurface = colors.text,
            inverseOnSurface = colors.background,
            inversePrimary = SharedRhythmColors.Acid,
            outline = colors.border,
            outlineVariant = colors.border,
            error = colors.danger.ink,
            onError = Color.White,
            errorContainer = colors.danger.surface,
            onErrorContainer = colors.danger.ink,
            surfaceTint = colors.surface,
            surfaceDim = colors.raised,
            surfaceBright = colors.surface,
            surfaceContainerLowest = colors.surface,
            surfaceContainerLow = colors.surface,
            surfaceContainer = colors.surface,
            surfaceContainerHigh = colors.raised,
            surfaceContainerHighest = colors.raised
        )
    }

    val inter = FontFamily(
        ComposeFont(Res.font.Inter_Variable, FontWeight.Normal),
        ComposeFont(Res.font.Inter_Variable, FontWeight.Medium),
        ComposeFont(Res.font.Inter_Variable, FontWeight.SemiBold),
        ComposeFont(Res.font.Inter_Variable, FontWeight.Bold)
    )
    val display = FontFamily(ComposeFont(Res.font.BarlowCondensed_SemiBold, FontWeight.SemiBold))
    val typography = Typography(
        displayLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 88.sp,
            lineHeight = 84.sp
        ),
        displayMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 64.sp,
            lineHeight = 62.sp
        ),
        displaySmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 48.sp,
            lineHeight = 48.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 40.sp,
            lineHeight = 40.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 35.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 32.sp
        ),
        titleLarge = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp
        ),
        titleMedium = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        titleSmall = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodySmall = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 18.sp
        ),
        labelLarge = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelMedium = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelSmall = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    )
    val shapes = Shapes(
        small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
    )

    CompositionLocalProvider(SharedRhythmColorsLocal provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}

data class SemanticColor(val ink: Color, val surface: Color)

data class SharedRhythmColors(
    val background: Color,
    val surface: Color,
    val raised: Color,
    val text: Color,
    val muted: Color,
    val border: Color,
    val accentText: Color,
    val success: SemanticColor,
    val warning: SemanticColor,
    val danger: SemanticColor,
    val info: SemanticColor
) {
    companion object {
        val Acid = Color(0xFFDBFF00)
        val Charcoal = Color(0xFF171A19)
        val Chalk = Color(0xFFF3F5ED)
        val Ice = Color(0xFF8BCAFF)
        val Lilac = Color(0xFFD3BEFF)

        val Dark = SharedRhythmColors(
            background = Color(0xFF171A19),
            surface = Color(0xFF222624),
            raised = Color(0xFF2C312E),
            text = Color(0xFFF3F5ED),
            muted = Color(0xFFA9B3AA),
            border = Color(0xFF748178),
            accentText = Acid,
            success = SemanticColor(Color(0xFFA3E8BC), Color(0xFF173424)),
            warning = SemanticColor(Color(0xFFFFB86B), Color(0xFF382B1D)),
            danger = SemanticColor(Color(0xFFFF8E84), Color(0xFF402622)),
            info = SemanticColor(Ice, Color(0xFF1E3040))
        )

        val Light = SharedRhythmColors(
            background = Chalk,
            surface = Color.White,
            raised = Color(0xFFE6EADF),
            text = Charcoal,
            muted = Color(0xFF546055),
            border = Color(0xFF7A8578),
            accentText = Color(0xFF4D6000),
            success = SemanticColor(Color(0xFF176B3B), Color(0xFFE1F2E5)),
            warning = SemanticColor(Color(0xFF7A4600), Color(0xFFFFF0D5)),
            danger = SemanticColor(Color(0xFFB32B26), Color(0xFFFFE5E0)),
            info = SemanticColor(Color(0xFF245C89), Color(0xFFE1EFFB))
        )
    }
}

object PacemakerSpacing {
    val x1 = 4.dp
    val x2 = 8.dp
    val x3 = 12.dp
    val x4 = 16.dp
    val x5 = 20.dp
    val x6 = 24.dp
    val x8 = 32.dp
    val x10 = 40.dp
    val x12 = 48.dp
}

object PacemakerMotion {
    const val fastMillis = 120
    const val standardMillis = 180
    const val slowMillis = 240
}

object PacemakerIdentityColors {
    val participants = listOf(
        SharedRhythmColors.Acid,
        SharedRhythmColors.Ice,
        SharedRhythmColors.Lilac,
        SharedRhythmColors.Chalk
    )
}

private data class PacemakerThemeImpl(
    override val meColor: Color,
    override val meColorLight: Color,
    override val meColorWhite: Color
) : PacemakerTheme

private object MissingPacemakerTheme : PacemakerTheme {
    override val meColor: Color = Color.Gray
    override val meColorLight: Color = Color.LightGray
    override val meColorWhite: Color = Color.White
}
