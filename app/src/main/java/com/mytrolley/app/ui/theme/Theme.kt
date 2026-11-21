package com.mytrolley.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MyTrolleyColors = lightColorScheme(
    primary = MH.Forest,
    onPrimary = MH.Ivory,
    primaryContainer = MH.Sage,
    onPrimaryContainer = MH.Forest,
    secondary = MH.PrimaryButton,
    onSecondary = MH.OnPrimaryButton,
    secondaryContainer = MH.SageSoft,
    onSecondaryContainer = MH.Forest,
    background = MH.Cream,
    onBackground = MH.Ink,
    surface = MH.Ivory,
    onSurface = MH.Ink,
    surfaceVariant = MH.SageSoft,
    onSurfaceVariant = MH.Muted,
    surfaceContainerLowest = MH.Ivory,
    surfaceContainerLow = MH.Ivory,
    surfaceContainer = MH.Ivory,
    surfaceContainerHigh = MH.Cream,
    surfaceContainerHighest = MH.SageSoft,
    outline = MH.Divider,
    error = MH.Destructive
)

@Composable
fun MyTrolleyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MyTrolleyColors,
        typography = Typography,
        shapes = MarketHallShapes,
        content = content
    )
}
