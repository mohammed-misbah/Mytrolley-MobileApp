package com.mytrolley.app.data.remote

import java.util.Locale

/** Store-specific pricing helpers shared by the repository and the UI. */
object CatalogPricing {
    const val CURRENCY: String = "AED"

    /** The UAE catalogue uses AED; format consistently across all screens. */
    fun format(price: Double): String = String.format(Locale.US, "%s %.2f", CURRENCY, price)

    /** Decimal prices arrive as strings from the API; keep them exact until display. */
    fun format(price: String): String = price.toDoubleOrNull()?.let(::format) ?: price

    /** Price for one product in exactly one selected store, never another store's. */
    fun offerFor(item: ApiProductListItem, storeId: Int): ApiStoreAssociation? =
        item.selectedStorePrice
            ?.takeIf { it.store.id == storeId && it.isAvailable }
            ?: item.availableStores.firstOrNull { it.store.id == storeId && it.isAvailable }

    /** Selected-store price for the product detail response. */
    fun offerFor(detail: ApiProductDetail, storeId: Int): ApiStoreAssociation? =
        detail.selectedStore
            ?.takeIf { it.store.id == storeId && it.isAvailable }
            ?: detail.stores.firstOrNull { it.store.id == storeId && it.isAvailable }
}
