package com.mytrolley.app.model

import androidx.annotation.DrawableRes

data class Product(
    val id: Int,
    val name: String,
    val brand: String,
    val price: Double,
    val unit: String,
    @DrawableRes val imageRes: Int?,
    val availableQty: Int,
    val description: String = "",
    val category: String = "",
    val imageUrl: String? = null
)