package com.mytrolley.app.ui.home

import com.mytrolley.app.ui.theme.MH
import com.mytrolley.app.ui.theme.MarketHallSerif
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.mytrolley.app.R
import com.mytrolley.app.data.remote.CatalogPricing
import com.mytrolley.app.ui.MarketHallEasing
import com.mytrolley.app.ui.rememberMotionEnabled
import com.mytrolley.app.ui.riseEntrance
import com.mytrolley.app.model.Category
import com.mytrolley.app.model.Product

private val HeaderGreen = MH.Forest
private val FooterGreen = MH.ForestDeep
private val ButtonGreen = MH.ForestButton
private val Ivory = MH.Ivory
private val Sage = MH.Sage
private val Ink = MH.Ink
private val Muted = MH.Muted
private val Olive = MH.Olive

internal val ForestPageBrush = Brush.linearGradient(
    listOf(Sage, MH.Sage, MH.Sage, Sage)
)

private val HeaderBrush = Brush.linearGradient(
    listOf(MH.ForestDeep, HeaderGreen, MH.ForestDeep)
)

@Composable
internal fun ForestHeaderBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    storeName: String?,
    loadingStores: Boolean,
    storeLoadFailed: Boolean,
    canChangeStore: Boolean,
    onChangeStore: () -> Unit,
    onFilter: () -> Unit,
    onCatalogue: () -> Unit,
    searchLabel: String = "Search groceries",
    actionLabel: String = "Browse categories"
) {
    val focus = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(HeaderBrush)
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painterResource(R.drawable.mytrolley_logo),
                contentDescription = "MY Trolley",
                modifier = Modifier
                    .weight(1f, fill = false)
                    .widthIn(max = 120.dp)
                    .height(26.dp),
                alignment = Alignment.CenterStart,
                contentScale = ContentScale.Fit
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MH.ForestSoft)
                    .clickable(role = Role.Button, onClick = onCatalogue)
                    .semantics { contentDescription = "Open catalogue" },
                contentAlignment = Alignment.Center
            ) {
                CatalogueBagIcon(Ivory, Modifier.size(20.dp))
            }
        }

        val location = storeName ?: when {
            loadingStores -> "Loading stores…"
            storeLoadFailed -> "Stores unavailable"
            else -> "No active store"
        }

        Row(
            modifier = Modifier
                .clickable(
                    enabled = canChangeStore,
                    role = Role.Button,
                    onClick = onChangeStore
                )
                .padding(top = 14.dp)
                .heightIn(min = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            CataloguePinIcon(Ivory, Modifier.size(16.dp))

            Text(
                text = location,
                modifier = Modifier.weight(1f, fill = false),
                color = Ivory,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (canChangeStore) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = Ivory
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 38.dp)
                    .background(Ivory, CircleShape)
                    .semantics { contentDescription = searchLabel },
                textStyle = TextStyle(color = Ink, fontSize = 13.sp),
                cursorBrush = SolidColor(ButtonGreen),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { focus.clearFocus() }
                ),
                decorationBox = { field ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 13.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = HeaderGreen
                        )

                        Box(Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = searchLabel,
                                    color = Muted,
                                    fontSize = 13.sp
                                )
                            }
                            field()
                        }
                    }
                }
            )

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Ivory)
                    .clickable(role = Role.Button) {
                        focus.clearFocus()
                        onFilter()
                    }
                    .semantics { contentDescription = actionLabel },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Category,
                    contentDescription = null,
                    modifier = Modifier.size(21.dp),
                    tint = HeaderGreen
                )
            }
        }
    }
}




@Composable
internal fun ForestAisleRail(
    categories: List<Category>,
    selectedCategoryId: Int?,
    onSelect: (Int?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AisleTile(
            label = "All",
            count = categories.sumOf { it.productCount },
            iconRes = R.drawable.ic_category_grid,
            photoRes = null,
            selected = selectedCategoryId == null,
            onClick = { onSelect(null) }
        )

        categories.filter { it.productCount > 0 }.forEach { category ->
            AisleTile(
                label = category.name,
                count = category.productCount,
                iconRes = categoryIcon(category.name),
                photoRes = categoryPhoto(category.name),
                selected = selectedCategoryId == category.id,
                onClick = { onSelect(category.id) }
            )
        }
    }
}

@Composable
private fun AisleTile(
    label: String,
    count: Int,
    @androidx.annotation.DrawableRes iconRes: Int,
    @androidx.annotation.DrawableRes photoRes: Int? = null,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(62.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .shadow(2.dp, CircleShape, ambientColor = MH.Hairline, spotColor = MH.Hairline)
                .clip(CircleShape)
                .background(if (photoRes == null) MH.Forest else MH.PhotoWell)
                .then(
                    if (selected) {
                        Modifier.border(4.dp, MH.Forest, CircleShape)
                    } else {
                        Modifier.border(1.dp, MH.Hairline, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (photoRes != null) {
                Image(
                    painter = painterResource(photoRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MH.Lime,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = label,
            color = MH.Ink,
            fontSize = 10.5.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = count.toString(),
            color = MH.Muted,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}


@Composable
internal fun ForestHomeProductCard(
    product: Product,
    onProductClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    entranceIndex: Int = 0,
    weight: String = ""
) {
    MarketHallProductCard(
        product = product,
        weight = weight,
        onOpen = onProductClick,
        modifier = modifier,
        entranceIndex = entranceIndex
    )
}

@Composable
private fun ProductCardPhoto(product: Product, modifier: Modifier) {
    val url = product.imageUrl?.takeIf { it.isNotBlank() }
    val drawable = product.imageRes

    when {
        url != null -> SubcomposeAsyncImage(
            model = url,
            contentDescription = product.name,
            modifier = modifier,
            contentScale = ContentScale.Fit,
            error = { ProductPhotoFallback(product) }
        )

        drawable != null -> Image(
            painter = painterResource(drawable),
            contentDescription = product.name,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )

        else -> ProductPhotoFallback(product, modifier)
    }
}

@Composable
private fun ProductPhotoFallback(product: Product, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text = product.name.trim().take(1).uppercase(),
            color = MH.ForestMuted,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Shared Market Hall product card used by Home, Catalogue, Search and Categories. */
@Composable
internal fun MarketHallProductCard(
    product: Product,
    weight: String = "",
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier,
    entranceIndex: Int = 0
) {
    val shape = MH.CardShape
    val inStock = product.availableQty > 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(238.dp)
            .clip(shape)
            .background(MH.Ivory)
            .border(1.dp, MH.Hairline, shape)
            .clickable(
                role = Role.Button,
                onClickLabel = "View ${product.name}"
            ) { onOpen(product.id) }
            .riseEntrance(entranceIndex)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(116.dp)
                .background(MH.PhotoWell)
        ) {
            // Keep the whole product visible, matching the product detail gallery.
            ProductCardPhoto(
                product,
                Modifier.matchParentSize().padding(MH.ProductImagePadding)
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .height(122.dp)
                .padding(start = 11.dp, end = 11.dp, top = 9.dp, bottom = 8.dp)
        ) {
            Column(Modifier.height(62.dp)) {
                if (product.category.isNotBlank()) {
                    Text(
                        text = product.category.uppercase(),
                        color = MH.Olive,
                        fontSize = 8.5.sp,
                        lineHeight = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.height(14.dp)
                    )
                } else {
                    Spacer(Modifier.height(14.dp))
                }

                Spacer(Modifier.height(3.dp))

                Text(
                    text = product.name,
                    color = MH.Ink,
                    fontSize = 13.5.sp,
                    lineHeight = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(29.dp)
                )

                Text(
                    text = weight.ifBlank { product.unit },
                    color = MH.Muted,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp).height(14.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Text(
                        text = CatalogPricing.format(product.price),
                        color = MH.Forest,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Spacer(
                            Modifier
                                .size(4.dp)
                                .background(
                                    if (inStock) MH.StockIn else MH.StockOut,
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (inStock) "${product.availableQty} in stock" else "Out of stock",
                            color = MH.Muted,
                            fontSize = 9.sp,
                            lineHeight = 12.sp
                        )
                    }
                }

                val pressInteraction = remember { MutableInteractionSource() }
                val pressed by pressInteraction.collectIsPressedAsState()
                val motionEnabled = rememberMotionEnabled()
                val pressScale by animateFloatAsState(
                    targetValue = if (pressed && motionEnabled) 0.92f else 1f,
                    animationSpec = tween(140, easing = MarketHallEasing),
                    label = "addButtonPress"
                )

                Box(
                    modifier = Modifier
                        .size(27.dp)
                        .clip(CircleShape)
                        .border(1.dp, MH.ActionOutline, CircleShape)
                        .clickable(
                            interactionSource = pressInteraction,
                            indication = null,
                            role = Role.Button,
                            onClickLabel = "View ${product.name} details"
                        ) { onOpen(product.id) }
                        .graphicsLayer {
                            scaleX = pressScale
                            scaleY = pressScale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (inStock) MH.Forest else MH.Muted
                    )
                }
            }
        }
    }
}

@Composable
internal fun ForestBottomBar(
    selected: CatalogueTab,
    onSelect: (CatalogueTab) -> Unit
) {
    val barShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

    Box(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MH.Divider, barShape)
                .background(MH.Ivory, barShape)
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ForestTab(CatalogueTab.HOME, selected, Modifier.weight(1f), onSelect)
            ForestTab(CatalogueTab.CATEGORIES, selected, Modifier.weight(1f), onSelect)
            Spacer(Modifier.width(54.dp))
            ForestTab(CatalogueTab.PRODUCTS, selected, Modifier.weight(1f), onSelect)
            ForestTab(CatalogueTab.STORES, selected, Modifier.weight(1f), onSelect)
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-20).dp)
                .size(54.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(MH.Forest)
                .border(3.dp, MH.Ivory, CircleShape)
                .clickable(role = Role.Button) { onSelect(CatalogueTab.PRODUCTS) }
                .semantics { contentDescription = "Browse all products" },
            contentAlignment = Alignment.Center
        ) {
            CatalogueBagIcon(Color.White, Modifier.size(24.dp))
        }
    }
}

@Composable
private fun ForestTab(
    tab: CatalogueTab,
    selected: CatalogueTab,
    modifier: Modifier,
    onSelect: (CatalogueTab) -> Unit
) {
    val active = tab == selected
    val iconColor = if (active) MH.Forest else Color(0xFF9AA39B)
    val labelColor = if (active) MH.Forest else Color(0xFF869087)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Tab) { onSelect(tab) }
            .heightIn(min = 56.dp)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CatalogueTabIcon(tab, iconColor, Modifier.size(20.dp), filled = active)

        Spacer(Modifier.height(3.dp))

        Text(
            text = tab.label,
            color = labelColor,
            fontSize = 10.5.sp,
            lineHeight = 13.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )

        Spacer(Modifier.height(3.dp))

        Spacer(
            Modifier
                .size(4.dp)
                .background(
                    if (active) MH.Lime else Color.Transparent,
                    CircleShape
                )
        )
    }
}