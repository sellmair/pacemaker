package io.sellmair.pacemaker.wear

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography

private val WearColors = ColorScheme(
    primary = Color(0xFFDBFF00),
    primaryDim = Color(0xFFDBFF00),
    primaryContainer = Color(0xFF222624),
    onPrimary = Color(0xFF171A19),
    onPrimaryContainer = Color(0xFFDBFF00),
    secondary = Color(0xFFA3E8BC),
    secondaryDim = Color(0xFFA3E8BC),
    secondaryContainer = Color(0xFF173424),
    onSecondary = Color(0xFF171A19),
    onSecondaryContainer = Color(0xFFA3E8BC),
    tertiary = Color(0xFFFFB86B),
    tertiaryDim = Color(0xFFFFB86B),
    tertiaryContainer = Color(0xFF382B1D),
    onTertiary = Color(0xFF171A19),
    onTertiaryContainer = Color(0xFFFFB86B),
    surfaceContainerLow = Color(0xFF222624),
    surfaceContainer = Color(0xFF222624),
    surfaceContainerHigh = Color(0xFF2C312E),
    onSurface = Color(0xFFF3F5ED),
    onSurfaceVariant = Color(0xFFA9B3AA),
    outline = Color(0xFF748178),
    outlineVariant = Color(0xFF748178),
    background = Color(0xFF171A19),
    onBackground = Color(0xFFF3F5ED),
    error = Color(0xFFFF8E84),
    errorDim = Color(0xFFFF8E84),
    errorContainer = Color(0xFF402622),
    onError = Color(0xFF171A19),
    onErrorContainer = Color(0xFFFF8E84),
)

private val InterFontFamily = FontFamily(
    Font(R.font.inter_variable, weight = FontWeight.Normal)
)

private val BarlowCondensedFontFamily = FontFamily(
    Font(R.font.barlow_condensed_semibold, weight = FontWeight.SemiBold)
)

private val WearBaseTypography = Typography(defaultFontFamily = InterFontFamily)

private val WearTypography = WearBaseTypography.copy(
    arcLarge = WearBaseTypography.arcLarge.copy(fontFamily = InterFontFamily),
    arcMedium = WearBaseTypography.arcMedium.copy(fontFamily = InterFontFamily),
    arcSmall = WearBaseTypography.arcSmall.copy(fontFamily = InterFontFamily),
    displayLarge = WearBaseTypography.displayLarge.copy(
        fontFamily = BarlowCondensedFontFamily,
        fontWeight = FontWeight.SemiBold,
    ),
    displayMedium = WearBaseTypography.displayMedium.copy(
        fontFamily = BarlowCondensedFontFamily,
        fontWeight = FontWeight.SemiBold,
    ),
    displaySmall = WearBaseTypography.displaySmall.copy(
        fontFamily = BarlowCondensedFontFamily,
        fontWeight = FontWeight.SemiBold,
    ),
    titleLarge = WearBaseTypography.titleLarge.copy(fontFamily = InterFontFamily),
    titleMedium = WearBaseTypography.titleMedium.copy(fontFamily = InterFontFamily),
    titleSmall = WearBaseTypography.titleSmall.copy(fontFamily = InterFontFamily),
    labelLarge = WearBaseTypography.labelLarge.copy(fontFamily = InterFontFamily),
    labelMedium = WearBaseTypography.labelMedium.copy(fontFamily = InterFontFamily),
    labelSmall = WearBaseTypography.labelSmall.copy(fontFamily = InterFontFamily),
    bodyLarge = WearBaseTypography.bodyLarge.copy(fontFamily = InterFontFamily),
    bodyMedium = WearBaseTypography.bodyMedium.copy(fontFamily = InterFontFamily),
    bodySmall = WearBaseTypography.bodySmall.copy(fontFamily = InterFontFamily),
    bodyExtraSmall = WearBaseTypography.bodyExtraSmall.copy(fontFamily = InterFontFamily),
    numeralExtraLarge = WearBaseTypography.numeralExtraLarge.copy(
        fontFamily = BarlowCondensedFontFamily,
        fontWeight = FontWeight.SemiBold,
    ),
    numeralLarge = WearBaseTypography.numeralLarge.copy(
        fontFamily = BarlowCondensedFontFamily,
        fontWeight = FontWeight.SemiBold,
    ),
    numeralMedium = WearBaseTypography.numeralMedium.copy(
        fontFamily = BarlowCondensedFontFamily,
        fontWeight = FontWeight.SemiBold,
    ),
    numeralSmall = WearBaseTypography.numeralSmall.copy(
        fontFamily = BarlowCondensedFontFamily,
        fontWeight = FontWeight.SemiBold,
    ),
    numeralExtraSmall = WearBaseTypography.numeralExtraSmall.copy(
        fontFamily = BarlowCondensedFontFamily,
        fontWeight = FontWeight.SemiBold,
    ),
)

/** Wear-local brand palette and typography for the shared rhythm experience. */
@Composable
fun WearSharedRhythmTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = WearColors, typography = WearTypography, content = content)
}
