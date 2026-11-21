package com.mytrolley.app.model

import androidx.annotation.DrawableRes

data class Category(
    val id: Int,
    val name: String,
    val productCount: Int,
    @DrawableRes val imageRes: Int?,
    val bgColorHex: String
)