package com.ren42377.bimalaunch.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val SlateDark = Color(0xFF141413)
val IvoryLight = Color(0xFFFAF9F5)
val IvoryMedium = Color(0xFFF0EEE6)
val IvoryDark = Color(0xFFE8E6DC)
val Oat = Color(0xFFE3DACC)
val CloudMedium = Color(0xFFB0AEA5)
val CloudLight = Color(0xFFD1CFC5)
val CloudDark = Color(0xFF87867F)
val SlateLight = Color(0xFF5E5D59)
val Clay = Color(0xFFD97757)
val AccentEmber = Color(0xFFC6613F)
val Olive = Color(0xFF788C5D)
val WhatsAppGreen = Color(0xFF25D366)

internal val BimalaunchColorScheme = lightColorScheme(
    background = IvoryLight,
    onBackground = SlateDark,
    surface = IvoryLight,
    onSurface = SlateDark,
    surfaceVariant = IvoryMedium,
    onSurfaceVariant = SlateLight,
    surfaceContainerLowest = IvoryLight,
    surfaceContainerLow = IvoryMedium,
    surfaceContainer = Oat,
    surfaceContainerHigh = IvoryDark,
    surfaceContainerHighest = CloudLight,
    surfaceDim = IvoryDark,
    surfaceBright = IvoryLight,
    primary = AccentEmber,
    onPrimary = IvoryLight,
    primaryContainer = IvoryMedium,
    onPrimaryContainer = SlateDark,
    secondary = SlateDark,
    onSecondary = IvoryLight,
    secondaryContainer = IvoryMedium,
    onSecondaryContainer = SlateDark,
    tertiary = Olive,
    onTertiary = SlateDark,
    tertiaryContainer = Oat,
    onTertiaryContainer = SlateDark,
    error = AccentEmber,
    onError = SlateDark,
    errorContainer = IvoryDark,
    onErrorContainer = SlateDark,
    outline = SlateDark,
    outlineVariant = CloudLight,
    inverseSurface = SlateDark,
    inverseOnSurface = IvoryLight,
    inversePrimary = Clay,
    scrim = SlateDark
)
