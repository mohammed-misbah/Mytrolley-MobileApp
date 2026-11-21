package com.mytrolley.app.data.repository

import com.google.gson.Gson
import com.mytrolley.app.data.remote.ApiClient
import com.mytrolley.app.data.remote.ApiErrorEnvelope
import com.mytrolley.app.data.remote.ApiProductDetail
import com.mytrolley.app.data.remote.CatalogPricing
import com.mytrolley.app.model.Category
import com.mytrolley.app.model.CategoryPanel
import com.mytrolley.app.model.Product
import com.mytrolley.app.model.Store
import com.mytrolley.app.model.Subgroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

class CatalogRepository {
    private val api = ApiClient.service

    suspend fun loadStores(): List<Store> = catalogRequest {
        api.stores().requireBody()
            .filter { it.isActive }
            .map { Store(it.id, it.name, it.code) }
    }

    suspend fun loadCategories(): List<Category> = catalogRequest {
        api.categories().requireBody()
            .filter { it.isActive }
            .map { Category(it.id, it.name, it.productCount, null, "#EAF4FB") }
    }

    suspend fun loadProducts(
        storeId: Int,
        search: String? = null,
        categoryId: Int? = null
    ): List<Product> = catalogRequest {
        val normalizedSearch = search?.trim()?.takeIf(String::isNotEmpty)
        val first = api.products(
            storeId = storeId,
            search = normalizedSearch,
            categoryId = categoryId
        ).requireBody()
        val all = first.results.toMutableList()
        val totalPages = first.totalPages.coerceAtLeast(first.page)
        for (page in (first.page + 1)..totalPages) {
            all += api.products(
                storeId = storeId,
                search = normalizedSearch,
                categoryId = categoryId,
                page = page,
                pageSize = first.pageSize
            ).requireBody().results
        }
        all.mapNotNull { item ->
            val association = CatalogPricing.offerFor(item, storeId) ?: return@mapNotNull null
            val price = association.price.toDoubleOrNull() ?: return@mapNotNull null
            Product(
                id = item.id,
                name = item.name,
                brand = "",
                price = price,
                unit = "",
                imageRes = null,
                availableQty = association.quantity,
                description = item.description,
                category = item.category?.name.orEmpty(),
                imageUrl = item.primaryImage?.imageUrl?.takeIf(String::isNotBlank)
            )
        }
    }

    suspend fun loadProductDetails(productId: Int, storeId: Int?): ApiProductDetail =
        catalogRequest { api.productDetails(productId, storeId).requireBody() }

    /**
     * Builds the category browser hierarchy from active categories and the
     * store's real products. Results are cached per store/category set so
     * revisiting the browser does not repeat every category request.
     */
    suspend fun loadCategoryPanels(
        storeId: Int,
        categories: List<Category>,
        forceRefresh: Boolean = false
    ): List<CategoryPanel> {
        val unique = categories.distinctBy { it.id }
        if (unique.isEmpty()) return emptyList()

        val cacheKey = "$storeId|${unique.joinToString(",") { it.id.toString() }}"
        if (!forceRefresh && CategoryPanelCache.key == cacheKey) {
            CategoryPanelCache.panels?.let { return it }
        }

        return catalogRequest {
            coroutineScope {
                val remote = api.categories().requireBody()
                val categoryIds = unique.map { it.id }.toSet()

                val parents = remote.associate { category ->
                    val parentId = category.parent?.id?.takeIf {
                        it != category.id && it in categoryIds
                    }
                    category.id to parentId
                }
                val children = unique.groupBy { parents[it.id] }
                val permits = Semaphore(3)

                val byCategory = unique.map { category ->
                    async {
                        val products = permits.withPermit {
                            loadProducts(storeId = storeId, categoryId = category.id)
                                .distinctBy { it.id }
                        }
                        category.id to products
                    }
                }.awaitAll().toMap()

                fun subtreeProducts(id: Int, seen: MutableSet<Int>): List<Product> {
                    if (!seen.add(id)) return emptyList()
                    val direct = byCategory[id].orEmpty()
                    val nested = children[id].orEmpty().flatMap { child ->
                        subtreeProducts(child.id, seen)
                    }
                    return (direct + nested).distinctBy { it.id }
                }

                val panels = children[null].orEmpty().mapNotNull { root ->
                    val groups = children[root.id].orEmpty().mapNotNull { child ->
                        val products = subtreeProducts(child.id, mutableSetOf())
                        if (products.isEmpty()) {
                            null
                        } else {
                            Subgroup(child.id, child.name, products)
                        }
                    }

                    val groupedIds = groups.flatMap { group -> group.products.map { it.id } }.toSet()
                    val direct = byCategory[root.id].orEmpty().filter { it.id !in groupedIds }

                    if (direct.isEmpty() && groups.isEmpty()) {
                        null
                    } else {
                        CategoryPanel(root.id, root.name, direct, groups)
                    }
                }

                CategoryPanelCache.key = cacheKey
                CategoryPanelCache.panels = panels
                panels
            }
        }
    }

    private suspend fun <T> catalogRequest(request: suspend () -> T): T =
        withContext(Dispatchers.IO) { request() }

    private fun <T : Any> Response<T>.requireBody(): T {
        if (isSuccessful) return body() ?: throw ApiException("The server returned an empty response.")
        val apiError = runCatching {
            errorBody()?.string()?.let { Gson().fromJson(it, ApiErrorEnvelope::class.java)?.error }
        }.getOrNull()
        val request = raw().request
        val endpoint = "${request.method} ${request.url}"
        val message = apiError?.message?.takeIf(String::isNotBlank)
            ?: "Request failed (HTTP ${code()}) for $endpoint."
        throw ApiException(message, code())
    }
}

private object CategoryPanelCache {
    var key: String? = null
    var panels: List<CategoryPanel>? = null
}

class ApiException(message: String, val statusCode: Int? = null) : Exception(message)

fun Throwable.userMessage(): String = when (this) {
    is ApiException -> message ?: "The API request failed."
    is IOException -> "Could not reach the MyTrolley server. Check that Django is running and the device can access it."
    else -> message?.takeIf(String::isNotBlank) ?: "Something went wrong while loading the catalog."
}
