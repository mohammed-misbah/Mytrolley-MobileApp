package com.mytrolley.app.ui.home

import com.mytrolley.app.ui.theme.MH
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import coil.compose.SubcomposeAsyncImage
import com.mytrolley.app.data.remote.ApiProductAttribute
import com.mytrolley.app.data.remote.CatalogPricing
import com.mytrolley.app.data.repository.CatalogRepository
import com.mytrolley.app.model.Category
import com.mytrolley.app.model.Product
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.Locale

internal enum class DietFilter(val label: String) {
    ALL("All"),
    VEG("Veg"),
    NON_VEG("Non-veg")
}

internal enum class ProductSort(val label: String) {
    ORIGINAL("Catalogue order"),
    PRICE_LOW("Price: low to high"),
    PRICE_HIGH("Price: high to low"),
    NAME("Name: A to Z")
}

internal data class CatalogueAttributes(
    val weight: String = "",
    val diet: DietFilter? = null
)

internal data class AttributeKey(val storeId: Int, val productId: Int)

private fun normalize(value: String): String =
    value.trim().lowercase(Locale.ROOT)
        .replace('_', ' ')
        .replace('-', ' ')
        .replace(Regex("\\s+"), " ")

private fun readAttributes(attributes: List<ApiProductAttribute>): CatalogueAttributes {
    var weight = ""
    val diets = mutableSetOf<DietFilter>()

    for (attribute in attributes) {
        val key = normalize(attribute.name)
        val value = attribute.value.trim()
        val normalized = normalize(value)

        when (key) {
            "weight", "net weight", "pack weight", "volume", "pack size", "net quantity", "size" -> {
                if (weight.isEmpty() && value.isNotBlank()) weight = value
            }

            "diet", "diet type", "dietary type", "food type", "dietary preference" -> {
                when (normalized) {
                    "veg", "vegetarian", "vegan" -> diets.add(DietFilter.VEG)
                    "non veg", "non vegetarian" -> diets.add(DietFilter.NON_VEG)
                }
            }

            "vegetarian", "is vegetarian" -> {
                when (normalized) {
                    "true", "yes" -> diets.add(DietFilter.VEG)
                    "false", "no" -> diets.add(DietFilter.NON_VEG)
                }
            }
        }
    }

    return CatalogueAttributes(weight = weight, diet = diets.singleOrNull())
}

/**
 * Separate from HomeViewModel so the existing catalogue and detail navigation
 * keep their current state. Reading attributes never opens a detail screen.
 */
class CatalogueAttributesViewModel : ViewModel() {
    private val repository = CatalogRepository()

    internal val values = mutableStateMapOf<AttributeKey, CatalogueAttributes>()
    internal val failures = mutableStateMapOf<AttributeKey, Boolean>()

    internal suspend fun load(
        storeId: Int,
        productIds: List<Int>,
        retryFailures: Boolean
    ) = coroutineScope {
        val pending = productIds.distinct().map { AttributeKey(storeId, it) }
            .filter { key ->
                key !in values && (retryFailures || key !in failures)
            }

        for (batch in pending.chunked(3)) {
            batch.map { key ->
                async {
                    try {
                        val detail = repository.loadProductDetails(key.productId, key.storeId)
                        values[key] = readAttributes(detail.attributes)
                        failures.remove(key)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        failures[key] = true
                    }
                }
            }.awaitAll()
        }
        Unit
    }
}

@Composable
internal fun FilteredCategoryTabs(
    categories: List<Category>,
    selectedId: Int?,
    onSelect: (Int?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "all") {
            FilterChipPill("All", selectedId == null) { onSelect(null) }
        }

        items(categories, key = { it.id }) { category ->
            FilterChipPill(category.name, selectedId == category.id) {
                onSelect(category.id)
            }
        }
    }
}

@Composable
private fun FilterChipPill(text: String, selected: Boolean, click: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = click)
            .background(if (selected) MH.Forest else MH.SageSoft)
            .widthIn(min = 62.dp)
            .heightIn(min = 38.dp)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) MH.Ivory else MH.Forest,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
internal fun CatalogueFilterRow(
    diet: DietFilter,
    sort: ProductSort,
    onDiet: (DietFilter) -> Unit,
    onSort: (ProductSort) -> Unit
) {
    // Horizontal scrolling keeps every control reachable at larger font sizes.
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "diet") {
            Row(
                Modifier
                    .shadow(1.dp, CircleShape)
                    .background(CatalogueCream, CircleShape)
                    .selectableGroup()
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DietFilter.values().forEach { option ->
                    val active = diet == option
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (active) MH.Forest else Color.Transparent)
                            .selectable(
                                selected = active,
                                role = Role.RadioButton,
                                onClick = { onDiet(option) }
                            )
                            .heightIn(min = 34.dp)
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (active && option != DietFilter.ALL) {
                            DietSymbol(if (option == DietFilter.VEG) CatalogueLime else Color.White)
                        }
                        Text(
                            option.label,
                            color = if (active) Color.White else CatalogueForest,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        item(key = "sort") {
            var expanded by remember { mutableStateOf(false) }

            Box {
                Row(
                    modifier = Modifier
                        .shadow(1.dp, CircleShape)
                        .clip(CircleShape)
                        .background(CatalogueCream)
                        .clickable(role = Role.Button) { expanded = true }
                        .heightIn(min = 40.dp)
                        .padding(horizontal = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("↑↓", color = CatalogueForest, fontSize = 16.sp)
                    Text("Sort", color = CatalogueForest, fontWeight = FontWeight.Bold)
                }

                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    ProductSort.values().forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option.label,
                                    fontWeight = if (sort == option) FontWeight.Bold
                                    else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onSort(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun FilteredProductCard(
    product: Product,
    attributes: CatalogueAttributes?,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier,
    entranceIndex: Int = 0
) {
    val weight = attributes?.weight.orEmpty().ifBlank { product.unit }
    MarketHallProductCard(
        product = product,
        weight = weight,
        onOpen = onOpen,
        modifier = modifier,
        entranceIndex = entranceIndex
    )
}

@Composable
private fun DietSymbol(color: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .border(1.2.dp, color),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Circle,
            contentDescription = null,
            modifier = Modifier.size(6.dp),
            tint = color
        )
    }
}