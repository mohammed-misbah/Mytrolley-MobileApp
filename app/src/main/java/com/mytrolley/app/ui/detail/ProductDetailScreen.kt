
package com.mytrolley.app.ui.detail

import com.mytrolley.app.ui.theme.MH
import com.mytrolley.app.ui.theme.MarketHallSerif
import com.mytrolley.app.ui.detailEntrance
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.mytrolley.app.R
import com.mytrolley.app.data.remote.ApiImage
import com.mytrolley.app.data.remote.ApiProductAttribute
import com.mytrolley.app.data.remote.ApiProductDetail
import com.mytrolley.app.data.remote.ApiStoreAssociation
import com.mytrolley.app.data.remote.CatalogPricing
import com.mytrolley.app.model.Store
import com.mytrolley.app.ui.home.CatalogueBagIcon
import com.mytrolley.app.ui.home.CataloguePinIcon
import com.mytrolley.app.ui.home.CatalogueTab
import com.mytrolley.app.ui.home.ForestBottomBar
import com.mytrolley.app.ui.home.StoreChoice
import com.mytrolley.app.ui.home.StoreSelectionSheet
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale

// HOME-MATCHING COLORS
private val HeaderGreen = MH.Forest
private val FooterGreen = MH.ForestDeep
private val DarkGreen = MH.Forest
private val ButtonGreen = MH.ForestButton
private val WarmIvory = MH.Cream
private val PaleSage = MH.Sage
private val MutedGreen = MH.Muted
private val TrolleyLime = MH.LimeBright

// Exact gradient from ForestHome.kt
private val DetailHeaderBrush = Brush.linearGradient(
    listOf(
        MH.ForestDeep,
        HeaderGreen,
        MH.ForestDeep
    )
)

private data class DetailFact(
    val label: String,
    val value: String
)

private data class DetailDisplay(
    val name: String,
    val category: String,
    val diet: String?,
    val price: String?,
    val priceBasis: String?,
    val stockLabel: String,
    val inStock: Boolean,
    val facts: List<DetailFact>,
    val description: String,
    val storeLabel: String,
    val images: List<ApiImage>
)

@Composable
internal fun ProductDetailScreen(
    detail: ApiProductDetail?,
    selectedStoreId: Int?,
    loading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    stores: List<Store> = emptyList(),
    onSelectStore: ((Store) -> Unit)? = null,
    onHome: (() -> Unit)? = null,
    onCategories: (() -> Unit)? = null,
    onCatalogue: (() -> Unit)? = null,
    entryTab: CatalogueTab = CatalogueTab.PRODUCTS
) {
    var showStores by rememberSaveable {
        mutableStateOf(false)
    }

    var showComparison by rememberSaveable {
        mutableStateOf(false)
    }

    var showCategoryInfo by rememberSaveable {
        mutableStateOf(false)
    }

    var inspectedStoreId by rememberSaveable(
        detail?.id,
        selectedStoreId
    ) {
        mutableStateOf(selectedStoreId)
    }

    val integratedStorePicker =
        onSelectStore != null && stores.isNotEmpty()

    val activeStoreId =
        if (integratedStorePicker) {
            selectedStoreId
        } else {
            inspectedStoreId
        }

    val density = LocalDensity.current
    val keyboardOpen =
        WindowInsets.ime.getBottom(density) > 0

    val offers = remember(detail) {
        if (detail == null) {
            emptyList()
        } else {
            (listOfNotNull(detail.selectedStore) + detail.stores)
                .distinctBy { it.store.id }
        }
    }

    val storeChoices = remember(
        stores,
        offers,
        integratedStorePicker
    ) {
        if (integratedStorePicker) {
            // Only offer stores that actually carry this product; otherwise fall
            // back to all stores for catalogues without store associations.
            val offerIds = offers.map { it.store.id }.toSet()
            val eligible = stores.filter { it.id in offerIds }
            (eligible.ifEmpty { stores }).map {
                StoreChoice(
                    id = it.id,
                    name = it.name,
                    detail = if (it.code.isBlank()) {
                        ""
                    } else {
                        "Store code · ${it.code}"
                    }
                )
            }
        } else {
            offers.map {
                StoreChoice(
                    id = it.store.id,
                    name = it.store.name,
                    detail = "${CatalogPricing.format(it.price)} · " +
                            stockDescription(it)
                )
            }
        }
    }

    val activeOffer = offers.firstOrNull {
        it.store.id == activeStoreId
    }

    val activeStoreName = stores.firstOrNull {
        it.id == activeStoreId
    }?.name ?: activeOffer?.store?.name

    fun goToCatalogue() {
        showStores = false
        if (onCatalogue != null) {
            onCatalogue()
        } else {
            onBack()
        }
    }

    // Let the system consume Back first while the store search keyboard is open.
    BackHandler(enabled = showStores && !keyboardOpen) {
        showStores = false
    }
    BackHandler(
        enabled = !showStores && !showComparison && !showCategoryInfo && !keyboardOpen
    ) {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HeaderGreen)
            .systemBarsPadding()
            .imePadding()
            .detailEntrance()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(Modifier.fillMaxSize()) {
                DetailHeader(
                    showReferenceBadge = false,
                    openCatalogue = { goToCatalogue() }
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = MH.SheetShape,
                    color = MH.Ivory
                ) {
                    when {
                        loading -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = DarkGreen
                            )
                        }

                        errorMessage != null -> StatusContent(
                            message = errorMessage,
                            onBack = onBack,
                            onRetry = onRetry
                        )

                        detail == null -> StatusContent(
                            message = "This product is no longer available.",
                            onBack = onBack,
                            onRetry = onRetry
                        )

                        else -> key(detail.id) {
                            val display = makeDisplay(
                                product = detail,
                                offer = activeOffer,
                                storeId = activeStoreId,
                                storeName = activeStoreName
                            )

                            DetailBody(
                                data = display,
                                reference = false,
                                previewPhoto = null,
                                onClose = onBack,
                                onChange = {
                                    showStores = true
                                },
                                onFirstAction = {
                                    showComparison = true
                                },
                                onSecondAction = {
                                    goToCatalogue()
                                }
                            )
                        }
                    }
                }
            }

            if (showStores) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Color.Black.copy(alpha = 0.35f)
                        )
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Close store picker"
                        ) {
                            showStores = false
                        }
                )

                val sheetHeight =
                    if (keyboardOpen || maxHeight < 480.dp) {
                        maxHeight
                    } else {
                        (maxHeight * 0.67f)
                            .coerceAtLeast(386.dp)
                            .coerceAtMost(maxHeight)
                    }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(sheetHeight),
                    shape = RoundedCornerShape(
                        topStart = 32.dp,
                        topEnd = 32.dp
                    ),
                    color = WarmIvory
                ) {
                    StoreSelectionSheet(
                        stores = storeChoices,
                        currentStoreId = activeStoreId,
                        loading = loading,
                        error = errorMessage,
                        onClose = {
                            showStores = false
                        },
                        onConfirm = { id ->
                            if (integratedStorePicker) {
                                stores.firstOrNull {
                                    it.id == id
                                }?.let { store ->
                                    onSelectStore?.invoke(store)
                                }
                            } else {
                                inspectedStoreId = id
                            }

                            showStores = false
                        },
                        onRetry = onRetry,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Hide the bar while the modal store sheet is open: its FAB overlapped
        // the sheet's confirmation button.
        if (!keyboardOpen && !showStores) {
            Box(
                Modifier.background(WarmIvory)
            ) {
                ForestBottomBar(
                    selected = entryTab,
                    onSelect = { tab ->
                        showStores = false

                        when (tab) {
                            CatalogueTab.HOME -> {
                                if (onHome != null) {
                                    onHome()
                                } else {
                                    onBack()
                                }
                            }

                            CatalogueTab.CATEGORIES -> {
                                if (onCategories != null) {
                                    onCategories()
                                } else {
                                    showCategoryInfo = true
                                }
                            }

                            CatalogueTab.PRODUCTS -> {
                                goToCatalogue()
                            }

                            CatalogueTab.STORES -> {
                                showStores = true
                            }
                        }
                    }
                )
            }
        }
    }

    if (showComparison) {
        OfferDialog(
            offers = offers,
            activeStoreId = activeStoreId,
            onClose = {
                showComparison = false
            },
            onChoose = {
                showComparison = false
                showStores = true
            }
        )
    }

    if (showCategoryInfo) {
        AlertDialog(
            onDismissRequest = {
                showCategoryInfo = false
            },
            containerColor = WarmIvory,
            shape = MH.SectionShape,
            title = {
                Text(
                    "Product category",
                    color = DarkGreen
                )
            },
            text = {
                Text(
                    detail?.category?.name
                        ?.takeIf { it.isNotBlank() }
                        ?: "No category available.",
                    color = MutedGreen
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCategoryInfo = false
                        goToCatalogue()
                    }
                ) {
                    Text(
                        "Back to catalogue",
                        color = DarkGreen
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCategoryInfo = false
                    }
                ) {
                    Text("Close", color = DarkGreen)
                }
            }
        )
    }
}

private fun normalize(value: String): String =
    value.trim()
        .lowercase(Locale.ROOT)
        .replace('_', ' ')
        .replace('-', ' ')
        .replace(Regex("\\s+"), " ")

private fun stockDescription(
    offer: ApiStoreAssociation
): String = when {
    !offer.isAvailable -> "Unavailable"
    offer.quantity <= 0 -> "Out of stock"
    else -> "${offer.quantity} in stock"
}

private fun readDiet(
    attributes: List<ApiProductAttribute>
): String? {
    val labels = mutableSetOf<String>()

    attributes.forEach { attribute ->
        val value = normalize(attribute.value)

        when (normalize(attribute.name)) {
            "diet",
            "diet type",
            "dietary type",
            "food type",
            "dietary preference" -> {
                when (value) {
                    "vegan", "veg", "vegetarian" ->
                        labels.add("Veg")

                    "non veg", "non vegetarian" ->
                        labels.add("Non-veg")
                }
            }

            "vegetarian", "is vegetarian" -> {
                when (value) {
                    "yes", "true" -> labels.add("Veg")
                    "no", "false" -> labels.add("Non-veg")
                }
            }
        }
    }

    return labels.singleOrNull()
}

private fun factPosition(label: String): Int =
    when (normalize(label)) {
        "origin", "country of origin" -> 0
        "shelf life" -> 1
        "storage", "storage instructions" -> 2
        "pack size", "weight", "net weight", "pack weight", "volume", "net quantity", "size" -> 3
        else -> 4
    }

private fun makeDisplay(
    product: ApiProductDetail,
    offer: ApiStoreAssociation?,
    storeId: Int?,
    storeName: String?
): DetailDisplay {
    val attributes = product.attributes.filter {
        it.name.isNotBlank() && it.value.isNotBlank()
    }

    val priceBasis = attributes.firstOrNull {
        normalize(it.name) in setOf(
            "price basis",
            "pricing basis",
            "price unit"
        )
    }?.value

    return DetailDisplay(
        name = product.name,
        category = product.category?.name.orEmpty(),
        diet = readDiet(attributes),
        price = offer?.takeIf {
            it.isAvailable
        }?.let {
            CatalogPricing.format(it.price)
        },
        priceBasis = priceBasis,
        stockLabel = when {
            storeId == null -> "Select a store"
            offer == null -> "Unavailable here"
            else -> stockDescription(offer)
        },
        inStock = offer != null &&
                offer.isAvailable &&
                offer.quantity > 0,
        facts = attributes
            .sortedBy { factPosition(it.name) }
            .map { DetailFact(it.name, it.value) },
        description = product.description.ifBlank {
            "No description available."
        },
        storeLabel = storeName ?: "Choose a store",
        images = product.images
            .filter { !it.imageUrl.isNullOrBlank() }
            .sortedWith(
                compareByDescending<ApiImage> {
                    it.isPrimary
                }.thenBy {
                    it.displayOrder
                }
            )
            .distinctBy { it.imageUrl }
    )
}

@Composable
private fun DetailHeader(
    showReferenceBadge: Boolean,
    openCatalogue: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DetailHeaderBrush)
            .padding(
                start = 17.dp,
                end = 16.dp,
                top = 6.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(
                R.drawable.mytrolley_logo
            ),
            contentDescription = "MyTrolley",
            modifier = Modifier
                .width(120.dp)
                .height(26.dp),
            alignment = Alignment.CenterStart,
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.weight(1f))

        Box {
            IconButton(
                onClick = openCatalogue,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        MH.ForestSoft,
                        CircleShape
                    )
                    .semantics {
                        contentDescription = "Open catalogue"
                    }
            ) {
                CatalogueBagIcon(
                    Color.White,
                    Modifier.size(20.dp)
                )
            }

            if (showReferenceBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(17.dp)
                        .background(
                            TrolleyLime,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "2",
                        color = DarkGreen,
                        fontSize = 11.sp,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailBody(
    data: DetailDisplay,
    reference: Boolean,
    previewPhoto: ImageBitmap?,
    onClose: () -> Unit,
    onChange: () -> Unit,
    onFirstAction: () -> Unit,
    onSecondAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 9.dp,
                end = 9.dp,
                top = 9.dp,
                bottom = 10.dp
            )
    ) {
        DetailGallery(
            data = data,
            reference = reference,
            previewPhoto = previewPhoto,
            onClose = onClose
        )

        Column(
            modifier = Modifier.padding(horizontal = 9.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            val categoryLine = listOfNotNull(
                data.category.takeIf {
                    it.isNotBlank()
                },
                data.diet
            ).joinToString(" · ")

            if (categoryLine.isNotBlank()) {
                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(5.dp)
                ) {
                    if (data.diet != null) {
                        val symbolColor =
                            if (data.diet == "Veg") {
                                Color(0xFF258C42)
                            } else {
                                Color(0xFFAA3B35)
                            }

                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .border(
                                    1.dp,
                                    symbolColor,
                                    RoundedCornerShape(2.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Spacer(
                                Modifier
                                    .size(6.dp)
                                    .background(
                                        symbolColor,
                                        CircleShape
                                    )
                            )
                        }
                    }

                    Text(
                        categoryLine,
                        color = MutedGreen,
                        fontSize = 13.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.name,
                    modifier = Modifier.weight(1f),
                    color = MH.Forest,
                    fontFamily = MarketHallSerif,
                    fontSize = 24.sp,
                    lineHeight = 28.sp
                )

                if (reference) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .background(
                                PaleSage,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = DarkGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                if (data.price != null) {
                    Text(
                        data.price,
                        color = DarkGreen,
                        fontSize = 23.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (!data.priceBasis.isNullOrBlank()) {
                        Text(
                            data.priceBasis,
                            color = MutedGreen,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Text(
                        "Price unavailable at this store",
                        color = MutedGreen,
                        fontSize = 14.sp
                    )
                }

                if (reference) {
                    Spacer(Modifier.weight(1f))
                    ReferenceQuantity()
                }
            }

            if (data.facts.isNotEmpty()) {
                FactCards(data.facts)
            }

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    "Description",
                    color = DarkGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    data.description,
                    color = MutedGreen,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MH.CardShape)
                    .background(MH.Ivory)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Change store",
                        onClick = onChange
                    )
                    .padding(
                        horizontal = 11.dp,
                        vertical = 7.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(9.dp)
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .background(
                            DarkGreen,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CataloguePinIcon(
                        Color.White,
                        Modifier.size(17.dp)
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        if (data.inStock) {
                            "Available at"
                        } else {
                            "Selected store"
                        },
                        color = MutedGreen,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )

                    Text(
                        data.storeLabel,
                        color = DarkGreen,
                        fontSize = 14.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    "Change",
                    modifier = Modifier
                        .background(
                            PaleSage,
                            CircleShape
                        )
                        .padding(
                            horizontal = 10.dp,
                            vertical = 5.dp
                        ),
                    color = DarkGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(9.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MH.Forest, MH.CardShape)
                .padding(8.dp),
            horizontalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {
            PanelButton(
                label = if (reference) {
                    "Add to cart"
                } else {
                    "Compare stores"
                },
                color = MH.PrimaryButton,
                action = onFirstAction,
                modifier = Modifier.weight(1f)
            )

            PanelButton(
                label = if (reference) {
                    "Buy now"
                } else {
                    "View catalogue"
                },
                color = WarmIvory,
                action = onSecondAction,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetailGallery(
    data: DetailDisplay,
    reference: Boolean,
    previewPhoto: ImageBitmap?,
    onClose: () -> Unit
) {
    val galleryState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val activeIndex by remember(galleryState) {
        derivedStateOf {
            val layout = galleryState.layoutInfo

            val center = (
                    layout.viewportStartOffset +
                            layout.viewportEndOffset
                    ) / 2

            layout.visibleItemsInfo.minByOrNull {
                kotlin.math.abs(
                    it.offset + it.size / 2 - center
                )
            }?.index ?: 0
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MH.CardShape)
            .background(MH.Sage)
    ) {
        val width = maxWidth
        val height = (width / 1.55f)
            .coerceAtMost(290.dp)

        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            when {
                previewPhoto != null -> Image(
                    bitmap = previewPhoto,
                    contentDescription = data.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(MH.ProductImagePadding),
                    contentScale = ContentScale.Fit
                )

                data.images.isEmpty() -> NoPhoto()

                else -> LazyRow(
                    state = galleryState,
                    modifier = Modifier.fillMaxSize(),
                    flingBehavior = rememberSnapFlingBehavior(galleryState)
                ) {
                    items(
                        data.images,
                        key = { it.id }
                    ) { photo ->
                        SubcomposeAsyncImage(
                            model = photo.imageUrl,
                            contentDescription =
                                photo.altText.ifBlank {
                                    data.name
                                },
                            modifier = Modifier
                                .width(width)
                                .fillMaxHeight()
                                .padding(MH.ProductImagePadding),
                            contentScale = ContentScale.Fit,
                            error = {
                                NoPhoto()
                            }
                        )
                    }
                }
            }

            if (
                previewPhoto == null &&
                data.images.size > 1
            ) {
                val currentPage = activeIndex.coerceIn(
                    0,
                    data.images.lastIndex
                )

                IconButton(
                    onClick = {
                        scope.launch {
                            galleryState.animateScrollToItem(
                                (currentPage - 1)
                                    .coerceAtLeast(0)
                            )
                        }
                    },
                    enabled = currentPage > 0,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 5.dp)
                        .size(38.dp)
                        .background(
                            WarmIvory.copy(alpha = 0.95f),
                            CircleShape
                        )
                        .semantics {
                            contentDescription =
                                "Previous product image"
                        }
                ) {
                    Text(
                        "‹",
                        color = if (currentPage > 0) {
                            DarkGreen
                        } else {
                            Color.LightGray
                        },
                        fontSize = 38.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            galleryState.animateScrollToItem(
                                (currentPage + 1)
                                    .coerceAtMost(
                                        data.images.lastIndex
                                    )
                            )
                        }
                    },
                    enabled = currentPage <
                            data.images.lastIndex,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 5.dp)
                        .size(38.dp)
                        .background(
                            WarmIvory.copy(alpha = 0.95f),
                            CircleShape
                        )
                        .semantics {
                            contentDescription =
                                "Next product image"
                        }
                ) {
                    Text(
                        "›",
                        color = if (
                            currentPage < data.images.lastIndex
                        ) {
                            DarkGreen
                        } else {
                            Color.LightGray
                        },
                        fontSize = 38.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(7.dp)
                    .size(39.dp)
                    .background(
                        WarmIvory,
                        CircleShape
                    )
                    .semantics {
                        contentDescription =
                            "Close product details"
                    }
            ) {
                Text(
                    "×",
                    color = DarkGreen,
                    fontSize = 24.sp,
                    lineHeight = 25.sp
                )
            }

            Text(
                data.stockLabel,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        if (data.inStock) {
                            TrolleyLime
                        } else {
                            WarmIvory
                        },
                        CircleShape
                    )
                    .padding(
                        horizontal = 10.dp,
                        vertical = 5.dp
                    ),
                color = DarkGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    val dotCount =
        if (reference) 3 else data.images.size

    if (dotCount > 1) {
        Box(
            Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            LazyRow(
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(dotCount) { index ->
                    val active = if (reference) {
                        index == 1
                    } else {
                        index == activeIndex
                    }

                    Box(
                        modifier = Modifier
                            .size(
                                width = 18.dp,
                                height = 22.dp
                            )
                            .clickable(
                                enabled = !reference,
                                role = Role.Button
                            ) {
                                scope.launch {
                                    galleryState.animateScrollToItem(
                                        index
                                    )
                                }
                            }
                            .semantics {
                                contentDescription =
                                    "Photo ${index + 1} of $dotCount"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Spacer(
                            Modifier
                                .width(
                                    if (active) 11.dp else 6.dp
                                )
                                .height(6.dp)
                                .background(
                                    if (active) {
                                        DarkGreen
                                    } else {
                                        MH.Divider
                                    },
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    } else {
        Spacer(Modifier.height(9.dp))
    }
}

@Composable
private fun NoPhoto() {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "No product photo",
            color = MutedGreen,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun FactCards(
    facts: List<DetailFact>
) {
    val columns =
        if (LocalDensity.current.fontScale >= 1.6f) {
            1
        } else {
            2
        }

    Column(
        verticalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {
        facts.chunked(columns).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                row.forEach { fact ->
                    Column(
                        modifier = Modifier
                            .weight(if (row.size == 1 && columns > 1) columns.toFloat() else 1f)
                            .shadow(
                                1.dp,
                                MH.CardShape
                            )
                            .background(
                                Color.White,
                                MH.CardShape
                            )
                            .padding(
                                horizontal = 12.dp,
                                vertical = 7.dp
                            )
                    ) {
                        Text(
                            fact.label,
                            color = MutedGreen,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )

                        Text(
                            fact.value,
                            color = DarkGreen,
                            fontSize = 14.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelButton(
    label: String,
    color: Color,
    action: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color)
            .clickable(
                role = Role.Button,
                onClick = action
            )
            .heightIn(min = 43.dp)
            .padding(
                horizontal = 6.dp,
                vertical = 6.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            textAlign = TextAlign.Center,
            color = MH.OnPrimaryButton,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ReferenceQuantity() {
    Row(
        Modifier.background(
            Color.White,
            CircleShape
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("−", "2", "+").forEachIndexed { index, value ->
            Box(
                Modifier
                    .size(29.dp)
                    .background(
                        if (index == 1) {
                            Color.Transparent
                        } else {
                            PaleSage
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    value,
                    color = DarkGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatusContent(
    message: String,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            message,
            color = DarkGreen
        )

        TextButton(onClick = onRetry) {
            Text(
                "Retry",
                color = DarkGreen
            )
        }

        TextButton(onClick = onBack) {
            Text(
                "Back to products",
                color = DarkGreen
            )
        }
    }
}

@Composable
private fun OfferDialog(
    offers: List<ApiStoreAssociation>,
    activeStoreId: Int?,
    onClose: () -> Unit,
    onChoose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = WarmIvory,
        shape = MH.SectionShape,
        title = {
            Text(
                "Store availability",
                color = DarkGreen,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (offers.isEmpty()) {
                Text(
                    "No store offers are available.",
                    color = MutedGreen
                )
            } else {
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        offers.sortedByDescending {
                            it.store.id == activeStoreId
                        },
                        key = { it.store.id }
                    ) { offer ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    Color.White,
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                offer.store.name +
                                        if (
                                            offer.store.id == activeStoreId
                                        ) {
                                            " (selected)"
                                        } else {
                                            ""
                                        },
                                color = DarkGreen,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "${CatalogPricing.format(offer.price)} · " +
                                        stockDescription(offer),
                                color = MutedGreen,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onChoose) {
                Text(
                    "Change store",
                    color = DarkGreen
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text(
                    "Close",
                    color = DarkGreen
                )
            }
        }
    )
}

@Preview(
    name = "Fresh Tomatoes — reference layout",
    widthDp = 390,
    heightDp = 844,
    showBackground = true
)
@Composable
private fun TomatoesReferencePreview() {
    val assets = LocalContext.current.assets

    val photo = remember(assets) {
        try {
            assets.open("tomatoes_preview.webp").use { stream ->
                BitmapFactory.decodeStream(stream)
                    ?.asImageBitmap()
            }
        } catch (_: IOException) {
            null
        }
    }

    val referenceData = DetailDisplay(
        name = "Fresh Tomatoes 1 kg",
        category = "Vegetables",
        diet = "Veg",
        price = "AED 6.50",
        priceBasis = "per kg",
        stockLabel = "120 in stock",
        inStock = true,
        facts = listOf(
            DetailFact("Origin", "UAE farms"),
            DetailFact("Shelf life", "5 days"),
            DetailFact("Storage", "Keep chilled"),
            DetailFact("Pack size", "1 kg")
        ),
        description =
            "Vine-ripened tomatoes, firm and juicy. " +
                    "Ideal for salads, curries and sauces.",
        storeLabel = "Palm Jumeirah · 25 min",
        images = emptyList()
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(HeaderGreen)
    ) {
        DetailHeader(
            showReferenceBadge = true,
            openCatalogue = {}
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = WarmIvory,
            shape = RoundedCornerShape(
                topStart = 26.dp,
                topEnd = 26.dp
            )
        ) {
            DetailBody(
                data = referenceData,
                reference = true,
                previewPhoto = photo,
                onClose = {},
                onChange = {},
                onFirstAction = {},
                onSecondAction = {}
            )
        }

        Box(
            Modifier.background(WarmIvory)
        ) {
            ForestBottomBar(
                selected = CatalogueTab.PRODUCTS,
                onSelect = {}
            )
        }
    }
}
