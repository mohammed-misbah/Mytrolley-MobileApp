package com.mytrolley.app.ui.home

import com.mytrolley.app.ui.theme.MH
import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.mytrolley.app.R
import java.util.Locale

@DrawableRes
internal fun categoryIcon(name: String?): Int {
    val key = name.orEmpty().trim().lowercase(Locale.ROOT)
        .replace("&", "and")
        .replace(Regex("[\\s\\-_]+"), " ")

    return when (key) {
        "vegetable", "vegetables", "fresh vegetables" -> R.drawable.ic_category_leaf
        "fruit", "fruits", "fresh fruits" -> R.drawable.ic_category_apple
        "fruits and vegetables", "vegetables and fruits" -> R.drawable.ic_category_apple
        "bakery", "bakery products", "bread" -> R.drawable.ic_category_bread
        "drink", "drinks", "beverage", "beverages" -> R.drawable.ic_category_bottle
        "dairy", "dairy and eggs", "eggs", "egg", "milk" -> R.drawable.ic_category_milk
        "meat", "chicken", "poultry", "non veg", "non vegetarian" -> R.drawable.ic_category_steak
        "electronics", "electronic" -> R.drawable.ic_category_electronics
        "tech accessories", "accessories" -> R.drawable.ic_category_accessories
        "groceries", "grocery", "pantry" -> R.drawable.ic_category_grid
        else -> R.drawable.ic_category_grid
    }
}

/** Studio product photo used for the Market Hall circular aisle tiles. */
@DrawableRes
internal fun categoryPhoto(name: String?): Int? {
    val key = name.orEmpty().trim().lowercase(Locale.ROOT)
        .replace("&", "and")
        .replace(Regex("[\\s\\-_]+"), " ")

    return when (key) {
        "fruit", "fruits", "fresh fruits" -> R.drawable.cat_oranges
        "vegetable", "vegetables", "fresh vegetables",
        "fruits and vegetables", "vegetables and fruits" -> R.drawable.cat_broccoli
        "bakery", "bakery products", "bread" -> R.drawable.cat_croissant
        "drink", "drinks", "beverage", "beverages" -> R.drawable.cat_milk
        "dairy", "dairy and eggs", "eggs", "egg", "milk" -> R.drawable.cat_cheese
        "meat", "chicken", "poultry", "non veg", "non vegetarian" -> R.drawable.cat_chicken
        "electronics", "electronic", "tech accessories", "accessories" -> R.drawable.cat_headphones
        "groceries", "grocery", "pantry" -> R.drawable.cat_rice
        else -> null
    }
}

@Composable
internal fun CategoryNameIcon(
    name: String?,
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(categoryIcon(name)),
        contentDescription = null,
        modifier = modifier,
        tint = MH.Ink
    )
}