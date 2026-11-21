package com.mytrolley.app.ui.home

import com.mytrolley.app.ui.theme.MH
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.mytrolley.app.R
import com.mytrolley.app.ui.heroEntrance
import com.mytrolley.app.ui.rememberMotionEnabled
import com.mytrolley.app.model.Category
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import java.util.Locale

private val ivory = MH.Ivory
private val forest = MH.ForestDeep
private val nameSeparators = Regex("[\\s_‐‑–—-]+")

/**
 * Normalizes both banner aliases and API category names to the same shape.
 * The catalogue uses names such as "Fruits & Vegetables" and "Dairy & Eggs",
 * so ampersands must be expanded before alias matching.
 */
private fun normalize(value: String): String =
    value.trim()
        .lowercase(Locale.ROOT)
        .replace("&", " and ")
        .replace(nameSeparators, " ")
        .replace(Regex("\\s+"), " ")
        .trim()

internal enum class BannerArt(
    val image: Int,
    val kicker: Int,
    val title: Int,
    val tagline: Int,
    val action: Int,
    vararg val aliases: String
) {
    FRUITS(
        R.drawable.banner_fruits,
        R.string.hero_fruits_kicker,
        R.string.hero_fruits_title,
        R.string.hero_fruits_tagline,
        R.string.hero_fruits_action,
        "fruits", "fruit", "fruits and vegetables", "vegetables and fruits"
    ),
    VEGETABLES(
        R.drawable.banner_vegetables,
        R.string.hero_vegetables_kicker,
        R.string.hero_vegetables_title,
        R.string.hero_vegetables_tagline,
        R.string.hero_vegetables_action,
        "vegetables", "vegetable", "fruits and vegetables", "vegetables and fruits"
    ),
    NONVEG(
        R.drawable.banner_nonveg,
        R.string.hero_nonveg_kicker,
        R.string.hero_nonveg_title,
        R.string.hero_nonveg_tagline,
        R.string.hero_nonveg_action,
        "non veg", "nonveg", "non vegetarian", "meat"
    ),
    ELECTRONICS(
        R.drawable.banner_electronics,
        R.string.hero_electronics_kicker,
        R.string.hero_electronics_title,
        R.string.hero_electronics_tagline,
        R.string.hero_electronics_action,
        "electronics", "electronic"
    ),
    DAIRY(
        R.drawable.banner_dairy,
        R.string.hero_dairy_kicker,
        R.string.hero_dairy_title,
        R.string.hero_dairy_tagline,
        R.string.hero_dairy_action,
        "dairy", "dairy and eggs", "eggs and dairy"
    ),
    BAKERY(
        R.drawable.banner_bakery,
        R.string.hero_bakery_kicker,
        R.string.hero_bakery_title,
        R.string.hero_bakery_tagline,
        R.string.hero_bakery_action,
        "bakery", "bakeries"
    ),
    GROCERIES(
        R.drawable.banner_groceries,
        R.string.hero_groceries_kicker,
        R.string.hero_groceries_title,
        R.string.hero_groceries_tagline,
        R.string.hero_groceries_action,
        "groceries", "grocery"
    )
}

// Enum declaration order is the display order. This list has no store dependency.
internal val homeBannerSlides: List<BannerArt> = BannerArt.values().toList()

// Called only when the shopper taps Explore.
internal fun BannerArt.categoryId(categories: List<Category>): Int? {
    for (alias in aliases) {
        val match = categories.firstOrNull { normalize(it.name) == alias }
            ?: categories.firstOrNull { category ->
                normalize(category.name).split(" ").any { token -> token == alias }
            }
        if (match != null) return match.id
    }
    return null
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HomeCategoryCarousel(
    slides: List<BannerArt>,
    visible: Boolean,
    onExplore: (BannerArt) -> Unit
) {
    val count = slides.size

    // Seven distinct slides. Virtual positions let both swipe directions wrap
    // without animating backwards across six slides at the end.
    val middle = Int.MAX_VALUE / 2
    val pager = rememberPagerState(
        initialPage = middle - middle % count
    ) { Int.MAX_VALUE }

    val view = LocalView.current
    val lifecycle = view.findViewTreeLifecycleOwner()?.lifecycle
    val context = LocalContext.current
    val manager = remember(context) {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }
    val motionEnabled = rememberMotionEnabled()

    var exploring by remember { mutableStateOf(manager.isTouchExplorationEnabled) }
    DisposableEffect(manager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener {
            exploring = it
        }
        manager.addTouchExplorationStateChangeListener(listener)
        exploring = manager.isTouchExplorationEnabled
        onDispose {
            manager.removeTouchExplorationStateChangeListener(listener)
        }
    }

    var down by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    var touchSequence by remember { mutableIntStateOf(0) }

    if (visible && !down && !focused && !exploring && motionEnabled && lifecycle != null) {
        LaunchedEffect(pager, lifecycle, touchSequence) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    snapshotFlow { pager.isScrollInProgress }.first { moving -> !moving }
                    val page = pager.settledPage
                    delay(2_000L)

                    if (!down && !focused && !exploring &&
                        !pager.isScrollInProgress && pager.settledPage == page
                    ) {
                        try {
                            pager.animateScrollToPage(
                                page + 1,
                                animationSpec = tween(260)
                            )
                        } catch (cancelled: CancellationException) {
                            // A user swipe can interrupt the animation. Propagate
                            // cancellation if Home itself has left the foreground.
                            currentCoroutineContext().ensureActive()
                        }
                    }
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.hasFocus }
            .focusGroup()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )
                    touchSequence += 1
                    down = true
                    try {
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                        } while (event.changes.any { it.pressed })
                    } finally {
                        down = false
                    }
                }
            }
    ) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxWidth()
        ) { position ->
            BannerSlide(slides[position % count], onExplore)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(
                4.dp,
                Alignment.CenterHorizontally
            )
        ) {
            repeat(count) { index ->
                val active = index == pager.settledPage % count
                Spacer(
                    Modifier
                        .size(width = if (active) 18.dp else 6.dp, height = 6.dp)
                        .background(
                            if (active) MH.Forest else MH.Divider,
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun BannerSlide(
    slide: BannerArt,
    onExplore: (BannerArt) -> Unit
) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(MH.BannerShape)
            .background(MH.Forest)
    ) {
        val width = maxWidth

        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = width * (558f / 1200f))
        ) {
            Image(
                painter = painterResource(slide.image),
                contentDescription = null,
                modifier = Modifier.matchParentSize().heroEntrance(),
                contentScale = ContentScale.FillBounds
            )

            Column(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(width * .54f)
                    .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp)
            ) {
                Text(
                    text = stringResource(slide.kicker),
                    color = MH.Lime,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    maxLines = 1
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = stringResource(slide.title),
                    color = ivory,
                    fontFamily = FontFamily.Serif,
                    fontSize = 24.sp,
                    lineHeight = 26.sp
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = stringResource(slide.tagline),
                    color = ivory.copy(alpha = 0.78f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 2
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { onExplore(slide) },
                    modifier = Modifier.heightIn(min = 40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MH.PrimaryButton,
                        contentColor = MH.OnPrimaryButton,
                        disabledContainerColor = MH.PrimaryButtonDisabled,
                        disabledContentColor = MH.OnPrimaryButtonDisabled
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(slide.action),
                        modifier = Modifier.weight(1f, fill = false),
                        color = MH.OnPrimaryButton,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MH.OnPrimaryButton
                    )
                }
            }
        }
    }
}