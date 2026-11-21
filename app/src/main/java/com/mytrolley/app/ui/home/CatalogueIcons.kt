package com.mytrolley.app.ui.home

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shared Material Icons used across the catalogue screens.
 * Replaces the previously hand-drawn Canvas icons so the whole app uses one
 * consistent, package-provided icon language.
 */

@Composable
internal fun CatalogueBagIcon(
    color: Color,
    modifier: Modifier = Modifier.size(23.dp)
) {
    Icon(
        imageVector = Icons.Outlined.ShoppingBag,
        contentDescription = null,
        modifier = modifier,
        tint = color
    )
}

@Composable
internal fun CataloguePinIcon(
    color: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    Icon(
        imageVector = if (filled) Icons.Filled.LocationOn else Icons.Outlined.LocationOn,
        contentDescription = null,
        modifier = modifier,
        tint = color
    )
}

@Composable
internal fun CatalogueTabIcon(
    tab: CatalogueTab,
    color: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    val imageVector = when (tab) {
        CatalogueTab.HOME -> if (filled) Icons.Filled.Home else Icons.Outlined.Home
        CatalogueTab.CATEGORIES -> Icons.Outlined.GridView
        CatalogueTab.PRODUCTS -> Icons.Outlined.Inventory2
        CatalogueTab.STORES -> Icons.Filled.LocationOn
    }
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = modifier,
        tint = color
    )
}
