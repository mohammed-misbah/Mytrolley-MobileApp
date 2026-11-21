package com.mytrolley.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytrolley.app.data.remote.ApiProductDetail
import com.mytrolley.app.data.repository.CatalogRepository
import com.mytrolley.app.data.repository.userMessage
import com.mytrolley.app.model.Category
import com.mytrolley.app.model.Product
import com.mytrolley.app.model.Store
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlin.time.Duration.Companion.milliseconds

data class HomeUiState(
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
    val stores: List<Store> = emptyList(),
    val selectedStore: Store? = null,
    val selectedCategoryId: Int? = null,
    val detail: ApiProductDetail? = null,
    val detailProductId: Int? = null,
    val detailLoading: Boolean = false,
    val detailError: String? = null,
    val isLoading: Boolean = false,
    val isProductsLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = ""
)

class HomeViewModel(
    private val repository: CatalogRepository = CatalogRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var productsJob: Job? = null
    private var detailJob: Job? = null
    private var latestProductsRequest = 0
    private var latestDetailRequest = 0

    init {
        loadCatalog()
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val (stores, categories) = supervisorScope {
                    val storesDeferred = async { repository.loadStores() }
                    val categoriesDeferred = async { repository.loadCategories() }
                    storesDeferred.await() to categoriesDeferred.await()
                }

                val previousSelection = _uiState.value.selectedStore
                val selected = previousSelection?.let { previous ->
                    stores.firstOrNull { it.id == previous.id }
                } ?: stores.firstOrNull()

                _uiState.update {
                    it.copy(
                        stores = stores,
                        categories = categories,
                        selectedStore = selected
                    )
                }

                if (selected == null) {
                    productsJob?.cancel()
                    _uiState.update {
                        it.copy(
                            products = emptyList(),
                            isLoading = false,
                            isProductsLoading = false
                        )
                    }
                } else {
                    requestProducts(
                        selected.id,
                        _uiState.value.searchQuery,
                        _uiState.value.selectedCategoryId
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                Log.e("MyTrolleyCatalog", "Catalog load failed", error)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isProductsLoading = false,
                        errorMessage = error.userMessage()
                    )
                }
            }
        }
    }

    fun selectStore(store: Store) {
        if (_uiState.value.selectedStore?.id == store.id) return

        _uiState.update { it.copy(selectedStore = store, errorMessage = null) }
        requestProducts(
            store.id,
            _uiState.value.searchQuery,
            _uiState.value.selectedCategoryId
        )
    }

    /** Store switch from the picker resets stale search/category filters in one request. */
    fun switchStore(store: Store) {
        if (_uiState.value.selectedStore?.id == store.id) return
        _uiState.update {
            it.copy(
                selectedStore = store,
                searchQuery = "",
                selectedCategoryId = null,
                errorMessage = null
            )
        }
        requestProducts(store.id, "", null)
    }

    fun selectCategory(categoryId: Int?) {
        if (_uiState.value.selectedCategoryId == categoryId) return

        _uiState.update { it.copy(selectedCategoryId = categoryId) }
        val storeId = _uiState.value.selectedStore?.id ?: return
        requestProducts(storeId, _uiState.value.searchQuery, categoryId)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        val storeId = _uiState.value.selectedStore?.id ?: return
        val categoryId = _uiState.value.selectedCategoryId
        requestProducts(storeId, query, categoryId, debounceMs = 350)
    }

    /** Clears search and category in a single request instead of issuing two. */
    fun clearFilters() = showCategory(null)

    /** Opens a category while clearing any active search in one request. */
    fun showCategory(categoryId: Int?) {
        val state = _uiState.value
        if (state.searchQuery.isEmpty() && state.selectedCategoryId == categoryId) return
        _uiState.update {
            it.copy(searchQuery = "", selectedCategoryId = categoryId)
        }
        val storeId = state.selectedStore?.id ?: return
        requestProducts(storeId, "", categoryId)
    }

    private fun requestProducts(
        storeId: Int,
        query: String,
        categoryId: Int?,
        debounceMs: Long = 0
    ) {
        val requestId = ++latestProductsRequest
        productsJob?.cancel()
        productsJob = viewModelScope.launch {
            if (debounceMs > 0) delay(debounceMs.milliseconds)
            loadProductsFor(requestId, storeId, query, categoryId)
        }
    }

    private suspend fun loadProductsFor(
        requestId: Int,
        storeId: Int,
        query: String,
        categoryId: Int?
    ) {
        _uiState.update {
            it.copy(isLoading = false, isProductsLoading = true, errorMessage = null)
        }

        try {
            val products = repository.loadProducts(storeId, query, categoryId)

            if (isCurrentProductsRequest(requestId, storeId)) {
                _uiState.update {
                    it.copy(products = products, isProductsLoading = false)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            if (isCurrentProductsRequest(requestId, storeId)) {
                _uiState.update {
                    it.copy(
                        products = emptyList(),
                        isProductsLoading = false,
                        errorMessage = error.userMessage()
                    )
                }
            }
        }
    }

    private fun isCurrentProductsRequest(requestId: Int, storeId: Int): Boolean =
        requestId == latestProductsRequest &&
                _uiState.value.selectedStore?.id == storeId

    fun openProduct(productId: Int) {
        val requestId = ++latestDetailRequest
        detailJob?.cancel()

        _uiState.update {
            it.copy(
                detail = null,
                detailProductId = productId,
                detailLoading = true,
                detailError = null
            )
        }

        detailJob = viewModelScope.launch {
            try {
                val storeId = _uiState.value.selectedStore?.id
                val detail = repository.loadProductDetails(productId, storeId)

                if (requestId == latestDetailRequest) {
                    _uiState.update {
                        it.copy(detail = detail, detailLoading = false)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (requestId == latestDetailRequest) {
                    _uiState.update {
                        it.copy(
                            detail = null,
                            detailLoading = false,
                            detailError = error.userMessage()
                        )
                    }
                }
            }
        }
    }

    fun closeProduct() {
        ++latestDetailRequest
        detailJob?.cancel()
        _uiState.update {
            it.copy(
                detail = null,
                detailProductId = null,
                detailLoading = false,
                detailError = null
            )
        }
    }

    fun retryDetail() {
        _uiState.value.detailProductId?.let(::openProduct)
    }

    fun retry() {
        loadCatalog()
    }
}