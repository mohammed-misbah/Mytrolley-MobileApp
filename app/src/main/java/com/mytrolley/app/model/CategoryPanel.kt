package com.mytrolley.app.model

/**
 * Presentation-ready category hierarchy for the category browser.
 *
 * Built by the repository from the public catalogue so the UI layer does not
 * make API calls or re-implement category parent/child logic.
 */
data class Subgroup(
    val id: Int,
    val name: String,
    val products: List<Product>
)

data class CategoryPanel(
    val id: Int,
    val name: String,
    val products: List<Product>,
    val groups: List<Subgroup>
)
