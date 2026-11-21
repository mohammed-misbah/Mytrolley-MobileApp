package com.mytrolley.app.ui

import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Market Hall prototype motion language (v1).
 *
 * Timings and easing are exact values from the reference CSS:
 * - rise:       260ms, 35ms per-item stagger, opacity 0 -> 1, translateY 10px -> 0
 * - heroIn:     300ms, opacity .4 -> 1, scale 1.025 -> 1
 * - sheetUp:    280ms, translateY 102% -> 0
 * - detailIn:   240ms, opacity 0 -> 1, translateY 14px -> 0
 * - pressScale: 140ms, scale 1 -> .92
 *
 * The reference disables all of this under `prefers-reduced-motion`, which is
 * honoured here through Android's animator duration scale.
 */
internal val MarketHallEasing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

@Composable
internal fun rememberMotionEnabled(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) > 0f
        }.getOrDefault(true)
    }
}

@Composable
private fun entranceProgress(durationMs: Int, delayMs: Int = 0): Float {
    val enabled = rememberMotionEnabled()
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val progress by animateFloatAsState(
        targetValue = if (entered || !enabled) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (enabled) durationMs else 0,
            delayMillis = if (enabled) delayMs else 0,
            easing = MarketHallEasing
        ),
        label = "marketHallEntrance"
    )
    return progress
}

/** Staggered card entrance: fade up from 10px below. */
@Composable
internal fun Modifier.riseEntrance(index: Int = 0): Modifier {
    val progress = entranceProgress(260, index.coerceIn(0, 8) * 35)
    return graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 10.dp.toPx()
    }
}

/** Hero entrance: fade in from .4 alpha while settling a 2.5% overscale. */
@Composable
internal fun Modifier.heroEntrance(): Modifier {
    val progress = entranceProgress(300)
    return graphicsLayer {
        alpha = 0.4f + 0.6f * progress
        val scale = 1.025f - 0.025f * progress
        scaleX = scale
        scaleY = scale
    }
}

/** Bottom-sheet entrance: slide up from just below its own height. */
@Composable
internal fun Modifier.sheetEntrance(): Modifier {
    val progress = entranceProgress(280)
    return graphicsLayer {
        translationY = (1f - progress) * size.height
    }
}

/** Detail entrance: fade up from 14px below. */
@Composable
internal fun Modifier.detailEntrance(): Modifier {
    val progress = entranceProgress(240)
    return graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 14.dp.toPx()
    }
}
