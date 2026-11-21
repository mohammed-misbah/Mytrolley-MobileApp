package com.mytrolley.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Single source of truth for the approved Market Hall visual language.
 * Presentation only — no data, navigation or business state lives here.
 */
object MarketHall {
    val Forest = Color(0xFF0A2417)
    val ForestDeep = Color(0xFF071811)
    val ForestButton = Color(0xFF125135)
    val ForestSoft = Color(0xFF15533A)
    val ForestMuted = Color(0xFF396044)

    val Ivory = Color(0xFFFDF9EE)
    val Cream = Color(0xFFFAF8EF)
    val Paper = Color(0xFFFBF7F0)
    val Sage = Color(0xFFE6EADA)
    val SageSoft = Color(0xFFEFF3E7)

    val Lime = Color(0xFFB6DF0A)
    val LimeBright = Color(0xFFC2E500)

    val Ink = Color(0xFF0C1711)
    val Muted = Color(0xFF5C6B60)
    val Olive = Color(0xFF40591D)
    val Divider = Color(0xFFDCE2D2)

    val OnDark = Color(0xFFFFFFFF)

    // Market Hall prototype (v1) reference tokens.
    val PhotoWell = Color(0xFFFBE7D5)
    val Hairline = Color(0x140A2417)      // rgba(10, 36, 23, 0.08)
    val ActionOutline = Color(0x400A2417) // rgba(10, 36, 23, 0.25)
    val StockIn = Color(0xFF6C9A3E)
    val StockOut = Color(0xFFC25A4A)

    // Shared primary action button colors — used by every filled primary action.
    val PrimaryButton = Lime
    val OnPrimaryButton = Forest
    val PrimaryButtonDisabled = Sage
    val OnPrimaryButtonDisabled = Muted
    val Destructive = Color(0xFFB3261E)

    // Shared inset for product photography. Keeps every product fully visible
    // inside a consistent, padded image container with ContentScale.Fit.
    val ProductImagePadding = 10.dp

    val CardShape = RoundedCornerShape(18.dp)
    val BannerShape = RoundedCornerShape(18.dp)
    val SectionShape = RoundedCornerShape(24.dp)
    val SheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val PillShape = RoundedCornerShape(50)
}

/** Short alias used by composables. */
val MH = MarketHall

val MarketHallShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = MH.CardShape,
    large = MH.SectionShape,
    extraLarge = RoundedCornerShape(32.dp)
)

/** Elegant serif used for major headings and hero titles. */
val MarketHallSerif = FontFamily.Serif

val MarketHallDisplay = TextStyle(
    fontFamily = MarketHallSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 24.sp,
    lineHeight = 28.sp
)

val MarketHallSectionTitle = TextStyle(
    fontFamily = MarketHallSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 22.sp,
    lineHeight = 26.sp
)
