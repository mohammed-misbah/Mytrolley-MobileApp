package com.mytrolley.app.data.remote

import com.google.gson.annotations.SerializedName

/** Nested category representation returned by the public catalogue. */
data class ApiCategorySummary(
    val id: Int,
    val name: String,
    val slug: String = ""
)

data class ApiStore(
    val id: Int,
    val name: String,
    val code: String,
    val description: String = "",
    val address: String = "",
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("product_count") val productCount: Int = 0
)

data class ApiCategory(
    val id: Int,
    val name: String,
    val slug: String = "",
    val description: String = "",
    val parent: ApiCategorySummary? = null,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("product_count") val productCount: Int = 0
)

data class ApiImage(
    val id: Int,
    val image: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("alt_text") val altText: String = "",
    @SerializedName("display_order") val displayOrder: Int = 0,
    @SerializedName("is_primary") val isPrimary: Boolean = false
)

/** A store-specific offer. Never treat this as a global product price. */
data class ApiStoreAssociation(
    val id: Int = 0,
    val store: ApiStore,
    val price: String,
    val quantity: Int = 0,
    @SerializedName("is_available") val isAvailable: Boolean = true
)

data class ApiProductListItem(
    val id: Int,
    val name: String,
    val slug: String = "",
    val description: String = "",
    val category: ApiCategorySummary? = null,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("primary_image") val primaryImage: ApiImage? = null,
    @SerializedName("starting_price") val startingPrice: String? = null,
    @SerializedName("selected_store_price") val selectedStorePrice: ApiStoreAssociation? = null,
    @SerializedName("available_stores") val availableStores: List<ApiStoreAssociation> = emptyList()
)

data class PagedProductsResponse(
    val count: Int,
    val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("total_pages") val totalPages: Int,
    val results: List<ApiProductListItem> = emptyList()
)

data class ApiProductAttribute(
    val id: Int,
    val name: String,
    val value: String
)

data class ApiProductDetail(
    val id: Int,
    val name: String,
    val slug: String = "",
    val description: String = "",
    val category: ApiCategorySummary? = null,
    val attributes: List<ApiProductAttribute> = emptyList(),
    val images: List<ApiImage> = emptyList(),
    val stores: List<ApiStoreAssociation> = emptyList(),
    @SerializedName("selected_store") val selectedStore: ApiStoreAssociation? = null
)

data class ApiErrorEnvelope(val error: ApiError? = null)

data class ApiError(
    val code: String = "API_ERROR",
    val message: String = "The request could not be completed.",
    @SerializedName("request_id") val requestId: String? = null
)
