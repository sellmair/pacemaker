package io.sellmair.pacemaker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import io.sellmair.pacemaker.HSLColor
import io.sellmair.pacemaker.MeColorState
import io.sellmair.pacemaker.UserColors
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

interface PacemakerTheme {


    val meColor: Color
    val meColorLight: Color
    val meColorWhite: Color
}

private val PacemakerThemeCompositionLocal = compositionLocalOf<PacemakerTheme> { MissingPacemakerTheme }

@Composable
fun PacemakerTheme(): PacemakerTheme = PacemakerThemeCompositionLocal.current

@Composable
fun MeColor() = PacemakerTheme().meColor

@Composable
fun MeColorLight() = PacemakerTheme().meColorLight

@Composable
fun MeColorWhite() = PacemakerTheme().meColorWhite

@Composable
fun PacemakerTheme(content: @Composable () -> Unit) {
    val pacemakerTheme by MeColorState.get().filterNotNull()
        .map { state ->
            PacemakerThemeImpl(
                meColor = state.color.toColor(),
                meColorLight = UserColors.fromHueLight(state.color.hue).toColor(),
                meColorWhite = state.color.copy(lightness = .95f).toColor()
            )
        }
        .distinctUntilChanged()
        .collectAsState(MissingPacemakerTheme)

    val meColor = pacemakerTheme.meColor


    CompositionLocalProvider(
        PacemakerThemeCompositionLocal provides pacemakerTheme
    ) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = meColor,
                primaryContainer = meColor,
                secondaryContainer = meColor,
                onSecondaryContainer = Color.White,
                onPrimaryContainer = meColor,
                onTertiaryContainer = meColor,
                onSurface = meColor,
                onSurfaceVariant = meColor,
            )
        ) {
            content()
        }
    }
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
