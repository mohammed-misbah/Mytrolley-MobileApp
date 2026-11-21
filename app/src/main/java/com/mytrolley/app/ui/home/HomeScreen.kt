package com.mytrolley.app.ui.home

import com.mytrolley.app.ui.theme.MH
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mytrolley.app.ui.detail.ProductDetailScreen
import com.mytrolley.app.ui.theme.MarketHallSerif
import com.mytrolley.app.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import java.util.Locale

internal val CatalogueBackground = MH.Cream
internal val CatalogueCream = MH.Cream
internal val CatalogueForest = MH.ForestButton
internal val CatalogueGreen = MH.ForestMuted
internal val CatalogueLime = MH.Lime
internal val CatalogueInk = MH.Ink
internal val CatalogueMuted = MH.Muted

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val attributes: CatalogueAttributesViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()

    val grid = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val density = LocalDensity.current

    var page by rememberSaveable { mutableStateOf(CatalogueTab.HOME) }
    var choosingStore by rememberSaveable { mutableStateOf(false) }
    var categorySearch by rememberSaveable { mutableStateOf("") }
    var diet by rememberSaveable { mutableStateOf(DietFilter.ALL) }
    var sort by rememberSaveable { mutableStateOf(ProductSort.ORIGINAL) }
    var retryAttributes by remember { mutableIntStateOf(0) }

    val storeId = state.selectedStore?.id
    val categoriesPage = page == CatalogueTab.CATEGORIES
    val productsPage = page == CatalogueTab.PRODUCTS ||
            (!categoriesPage && (state.searchQuery.isNotBlank() || state.selectedCategoryId != null))
    val home = !categoriesPage && !productsPage
    val keyboard = WindowInsets.ime.getBottom(density) > 0

    fun top() {
        focus.clearFocus()
        if (!categoriesPage && state.detailProductId == null) {
            scope.launch { grid.scrollToItem(0) }
        }
    }

    fun closePicker() {
        focus.clearFocus()
        choosingStore = false
    }

    fun openPicker() {
        focus.clearFocus()
        choosingStore = true
        top()
    }

    fun navigate(tab: CatalogueTab) {
        if (tab == CatalogueTab.STORES) {
            openPicker()
            return
        }

        closePicker()
        if (state.detailProductId != null) viewModel.closeProduct()
        if (tab == CatalogueTab.CATEGORIES && page != tab) categorySearch = ""

        page = tab
        if (tab != CatalogueTab.CATEGORIES) {
            viewModel.clearFilters()
            diet = DietFilter.ALL
            sort = ProductSort.ORIGINAL
            scope.launch { grid.scrollToItem(0) }
        }
    }

    // Both category chips and Explore use this route.
    fun openProducts(category: Int?, dietary: DietFilter = DietFilter.ALL) {
        focus.clearFocus()
        page = CatalogueTab.PRODUCTS
        diet = dietary
        sort = ProductSort.ORIGINAL
        viewModel.showCategory(category)
        scope.launch { grid.scrollToItem(0) }
    }

    if (state.detailProductId != null) {
        ProductDetailScreen(
            detail = state.detail,
            entryTab = when {
                categoriesPage -> CatalogueTab.CATEGORIES
                productsPage -> CatalogueTab.PRODUCTS
                else -> CatalogueTab.HOME
            },
            selectedStoreId = storeId,
            loading = state.detailLoading,
            errorMessage = state.detailError,
            onBack = viewModel::closeProduct,
            onRetry = viewModel::retryDetail,
            stores = state.stores,
            onSelectStore = viewModel::switchStore,
            onHome = { navigate(CatalogueTab.HOME) },
            onCategories = { navigate(CatalogueTab.CATEGORIES) },
            onCatalogue = { navigate(CatalogueTab.PRODUCTS) }
        )
        return
    }

    val products = state.products
    val chips = state.categories
    val ids = remember(products) { products.map { it.id } }
    val loading = state.isLoading || state.isProductsLoading
    val error = state.errorMessage

    // A tap is associated with the selected store. Switching stores or
    // leaving Home discards a pending tap from the previous screen.
    var request by remember(storeId, home) { mutableStateOf<BannerArt?>(null) }
    var notice by remember(storeId, home) { mutableStateOf<String?>(null) }
    val tapped = request

    LaunchedEffect(tapped, state.isLoading, error, chips, storeId) {
        if (tapped != null && home && !state.isLoading) {
            val live = viewModel.uiState.value
            if (live.selectedStore?.id == storeId && !live.isLoading) {
                request = null
                val store = live.selectedStore
                val id = tapped.categoryId(live.categories)
                when {
                    store == null && live.errorMessage != null ->
                        notice = live.errorMessage
                    store == null -> openPicker()
                    id != null -> openProducts(id)
                    live.errorMessage != null -> notice = live.errorMessage
                    tapped == BannerArt.NONVEG -> openProducts(null, DietFilter.NON_VEG)
                    else -> {
                        val name = tapped.name.lowercase(Locale.ROOT)
                            .replaceFirstChar { it.uppercase() }
                        notice = "$name unavailable at ${store.name}."
                    }
                }
            }
        }
    }

    LaunchedEffect(productsPage, storeId, ids, loading, error, retryAttributes) {
        if (storeId != null && !loading && error == null) {
            attributes.load(storeId, ids, retryFailures = retryAttributes > 0)
        }
    }

    val ordered = if (productsPage) {
        val matching = products.filter {
            diet == DietFilter.ALL || (storeId != null &&
                    attributes.values[AttributeKey(storeId, it.id)]?.diet == diet)
        }
        when (sort) {
            ProductSort.ORIGINAL -> matching
            ProductSort.PRICE_LOW -> matching.sortedBy { it.price }
            ProductSort.PRICE_HIGH -> matching.sortedByDescending { it.price }
            ProductSort.NAME -> matching.sortedBy { it.name.lowercase(Locale.ROOT) }
        }
    } else {
        products
    }

    val attrError = productsPage && storeId != null && ids.any {
        AttributeKey(storeId, it) in attributes.failures
    }
    val reading = productsPage && storeId != null && ids.any {
        val key = AttributeKey(storeId, it)
        key !in attributes.values && key !in attributes.failures
    }

    val bannerInView by remember(grid) {
        derivedStateOf {
            grid.layoutInfo.visibleItemsInfo.any { it.key == "category_banner" }
        }
    }

    // While the IME is open, let the system consume Back to dismiss the keyboard first.
    BackHandler(enabled = choosingStore && !keyboard) { closePicker() }
    BackHandler(enabled = !choosingStore && !home && !keyboard) { navigate(CatalogueTab.HOME) }

    if (request != null && state.isLoading) {
        AlertDialog(
            onDismissRequest = { request = null },
            containerColor = MH.Ivory,
            shape = MH.SectionShape,
            title = { Text("Loading categories…") },
            text = { LinearProgressIndicator() },
            confirmButton = {
                TextButton(onClick = { request = null }) { Text("Cancel") }
            }
        )
    }

    notice?.let { message ->
        AlertDialog(
            onDismissRequest = { notice = null },
            containerColor = MH.Ivory,
            shape = MH.SectionShape,
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    notice = null
                    openPicker()
                }) {
                    Text("Change store")
                }
            },
            dismissButton = {
                TextButton(onClick = { notice = null }) { Text("Close") }
            }
        )
    }

    val background = when {
        productsPage -> Modifier.background(MH.SageSoft)
        else -> Modifier.background(ForestPageBrush)
    }

    Column(
        Modifier
            .fillMaxSize()
            .then(if (choosingStore) Modifier.background(CatalogueCream) else background)
            .systemBarsPadding()
            .imePadding()
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val columns = when {
                maxWidth < 330.dp || density.fontScale >= 1.6f -> 1
                maxWidth >= 720.dp -> 3
                else -> 2
            }
            val behind = if (choosingStore) Modifier.clearAndSetSemantics {} else Modifier

            if (categoriesPage) {
                Column(Modifier.fillMaxSize().background(CatalogueBackground).then(behind)) {
                    ForestHeaderBar(
                        searchQuery = categorySearch,
                        onSearchQueryChanged = { categorySearch = it },
                        searchLabel = "Search categories",
                        actionLabel = "Browse products",
                        storeName = state.selectedStore?.name,
                        loadingStores = state.isLoading,
                        storeLoadFailed = error != null && storeId == null,
                        canChangeStore = state.stores.isNotEmpty(),
                        onChangeStore = ::openPicker,
                        onFilter = { navigate(CatalogueTab.PRODUCTS) },
                        onCatalogue = { navigate(CatalogueTab.PRODUCTS) }
                    )
                    CategoryBrowser(
                        storeId = storeId,
                        categories = state.categories,
                        catalogLoading = state.isLoading,
                        catalogError = error?.takeIf { storeId == null || state.categories.isEmpty() },
                        search = categorySearch,
                        preferredCategoryId = state.selectedCategoryId,
                        onOpenProduct = viewModel::openProduct,
                        onSeeAll = { openProducts(it) },
                        onChooseStore = ::openPicker,
                        onRetryCatalog = viewModel::retry,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    state = grid,
                    userScrollEnabled = !choosingStore,
                    modifier = Modifier.fillMaxSize()
                        .then(if (choosingStore) background else Modifier)
                        .then(behind),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                    contentPadding = PaddingValues(bottom = 14.dp)
                ) {
                    item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                        ForestHeaderBar(
                            searchQuery = state.searchQuery,
                            onSearchQueryChanged = viewModel::onSearchQueryChanged,
                            storeName = state.selectedStore?.name,
                            loadingStores = state.isLoading,
                            storeLoadFailed = error != null && storeId == null,
                            canChangeStore = state.stores.isNotEmpty(),
                            onChangeStore = ::openPicker,
                            onFilter = { navigate(CatalogueTab.CATEGORIES) },
                            onCatalogue = { navigate(CatalogueTab.PRODUCTS) }
                        )
                    }

                    // Home: Search groceries -> seven banners -> Recommended.
                    if (home) {
                        item(key = "category_banner", span = { GridItemSpan(maxLineSpan) }) {
                            HomeCategoryCarousel(
                                slides = homeBannerSlides,
                                visible = bannerInView && !choosingStore && !keyboard &&
                                        request == null && notice == null
                            ) { art ->
                                request = art
                            }
                        }
                    }

                    if (productsPage) {
                        item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
                            FilteredCategoryTabs(chips, state.selectedCategoryId) {
                                page = CatalogueTab.PRODUCTS
                                viewModel.showCategory(it)
                            }
                        }

                        item(key = "diet", span = { GridItemSpan(maxLineSpan) }) {
                            CatalogueFilterRow(
                                diet = diet,
                                sort = sort,
                                onDiet = { diet = it },
                                onSort = { sort = it }
                            )
                        }

                        item(key = "product_heading", span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    chips.firstOrNull { it.id == state.selectedCategoryId }?.name
                                        ?: "Products",
                                    Modifier.weight(1f),
                                    color = MH.Forest,
                                    fontFamily = MarketHallSerif,
                                    fontSize = 22.sp,
                                    lineHeight = 26.sp
                                )
                                if (!loading && error == null && storeId != null &&
                                    (diet == DietFilter.ALL || (!reading && !attrError))
                                ) {
                                    Text(
                                        "${ordered.size} ${if (ordered.size == 1) "item" else "items"}",
                                        color = CatalogueForest,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    } else {
                        item(key = "home_aisle_heading", span = { GridItemSpan(maxLineSpan) }) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                Text(
                                    "Shop by aisle",
                                    color = MH.Ink,
                                    fontFamily = MarketHallSerif,
                                    fontSize = 22.sp,
                                    lineHeight = 26.sp
                                )
                            }
                        }

                        item(key = "home_chips", span = { GridItemSpan(maxLineSpan) }) {
                            ForestAisleRail(chips, state.selectedCategoryId) {
                                if (it == null) viewModel.showCategory(null) else openProducts(it)
                            }
                        }

                        item(key = "home_heading", span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Recommended",
                                    Modifier.weight(1f),
                                    color = MH.Ink,
                                    fontFamily = MarketHallSerif,
                                    fontSize = 22.sp,
                                    lineHeight = 26.sp
                                )
                                TextButton(onClick = { navigate(CatalogueTab.PRODUCTS) }) {
                                    Text(
                                        "See all",
                                        color = MH.Olive,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (!loading && error == null && attrError) {
                        item(key = "attribute_error", span = { GridItemSpan(maxLineSpan) }) {
                            HomeNotice(
                                "Some product attributes could not be loaded, so they may be " +
                                        "missing from filtered results.",
                                "Retry attributes"
                            ) {
                                retryAttributes += 1
                            }
                        }
                    }

                    when {
                        error != null -> item(span = { GridItemSpan(maxLineSpan) }) {
                            HomeNotice(error, "Retry", viewModel::retry)
                        }

                        loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                            HomeLoading()
                        }

                        storeId == null -> item(span = { GridItemSpan(maxLineSpan) }) {
                            HomeNotice("No active stores are available.", "Refresh", viewModel::retry)
                        }

                        products.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                            when {
                                state.selectedCategoryId != null && state.searchQuery.isBlank() ->
                                    HomeNotice(
                                        "No products in this category at ${state.selectedStore?.name.orEmpty()}.",
                                        "Clear category",
                                        viewModel::clearFilters
                                    )

                                state.searchQuery.isNotBlank() || state.selectedCategoryId != null ->
                                    HomeNotice("No products match these filters.", "Clear filters") {
                                        navigate(CatalogueTab.PRODUCTS)
                                    }

                                state.stores.size > 1 ->
                                    HomeNotice(
                                        "No products are available at this store.",
                                        "Change store",
                                        ::openPicker
                                    )

                                else -> HomeNotice(
                                    "No products are available at this store.",
                                    "Refresh",
                                    viewModel::retry
                                )
                            }
                        }

                        ordered.isEmpty() && reading && diet != DietFilter.ALL ->
                            item(span = { GridItemSpan(maxLineSpan) }) { HomeLoading() }

                        ordered.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                            HomeNotice(
                                "No products are marked ${diet.label} at this store.",
                                "Show all dietary types"
                            ) {
                                diet = DietFilter.ALL
                            }
                        }

                        else -> items(ordered.size, key = { ordered[it].id }) { index ->
                            val product = ordered[index]
                            val column = index % columns
                            val margin = Modifier.padding(
                                start = if (column == 0) 16.dp else 4.dp,
                                end = if (column == columns - 1) 16.dp else 4.dp
                            )

                            if (productsPage) {
                                FilteredProductCard(
                                    product = product,
                                    attributes = attributes.values[AttributeKey(storeId, product.id)],
                                    onOpen = viewModel::openProduct,
                                    modifier = margin,
                                    entranceIndex = index
                                )
                            } else {
                                ForestHomeProductCard(
                                    product = product,
                                    onProductClick = viewModel::openProduct,
                                    modifier = margin,
                                    entranceIndex = index,
                                    weight = attributes.values[AttributeKey(storeId, product.id)]
                                        ?.weight.orEmpty()
                                )
                            }
                        }
                    }
                }
            }

            if (choosingStore) {
                val height = if (keyboard || maxHeight < 480.dp) {
                    maxHeight
                } else {
                    (maxHeight * .67f).coerceIn(386.dp, maxHeight)
                }

                Box(
                    Modifier.matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                .22f to Color.Black.copy(alpha = .06f),
                                .33f to Color.Black.copy(alpha = .45f),
                                1f to Color.Black.copy(alpha = .45f)
                            )
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClickLabel = "Close store picker",
                            onClick = ::closePicker
                        )
                )

                val choices = remember(state.stores) {
                    state.stores.map {
                        StoreChoice(
                            id = it.id,
                            name = it.name,
                            detail = if (it.code.isBlank()) "" else "Store code · ${it.code}"
                        )
                    }
                }

                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(height),
                    color = MH.Ivory,
                    shape = MH.SheetShape
                ) {
                    StoreSelectionSheet(
                        stores = choices,
                        currentStoreId = storeId,
                        loading = state.isLoading,
                        error = error,
                        onClose = ::closePicker,
                        onConfirm = { id ->
                            state.stores.firstOrNull { it.id == id }?.let {
                                if (it.id != storeId) categorySearch = ""
                                viewModel.switchStore(it)
                                closePicker()
                                top()
                            }
                        },
                        onRetry = viewModel::retry,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (!keyboard && !choosingStore) {
            ForestBottomBar(
                selected = when {
                    choosingStore -> CatalogueTab.STORES
                    categoriesPage -> CatalogueTab.CATEGORIES
                    productsPage -> CatalogueTab.PRODUCTS
                    else -> CatalogueTab.HOME
                },
                onSelect = ::navigate
            )
        }
    }
}

@Composable
private fun HomeLoading() {
    Box(
        Modifier.fillMaxWidth().padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = CatalogueGreen)
    }
}

@Composable
private fun HomeNotice(message: String, label: String, action: () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(CatalogueCream, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text(message, color = CatalogueInk)
        TextButton(onClick = action) {
            Text(label, color = CatalogueForest)
        }
    }
}

