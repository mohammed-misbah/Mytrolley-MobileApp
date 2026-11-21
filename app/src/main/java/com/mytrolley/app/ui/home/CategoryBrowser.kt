package com.mytrolley.app.ui.home

import com.mytrolley.app.ui.theme.MH
import com.mytrolley.app.ui.theme.MarketHallSerif
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mytrolley.app.data.repository.CatalogRepository
import com.mytrolley.app.data.repository.userMessage
import com.mytrolley.app.model.Category
import com.mytrolley.app.model.CategoryPanel
import com.mytrolley.app.model.Product
import kotlinx.coroutines.CancellationException

private val pageSage = MH.Cream
private val panelIvory = MH.Cream
private val tileIvory = MH.Paper
private val selectedTile = MH.Sage
private val ink = MH.Ink
private val olive = MH.Olive
private val dividerColor = MH.Divider

@Composable
internal fun CategoryBrowser(
    storeId: Int?,
    categories: List<Category>,
    catalogLoading: Boolean,
    catalogError: String?,
    search: String,
    preferredCategoryId: Int?,
    onOpenProduct: (Int) -> Unit,
    onSeeAll: (Int) -> Unit,
    onChooseStore: () -> Unit,
    onRetryCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = remember { CatalogRepository() }
    var retry by remember { mutableIntStateOf(0) }

    var panels by remember(storeId, categories) {
        mutableStateOf<List<CategoryPanel>?>(null)
    }
    var failure by remember(storeId, categories) {
        mutableStateOf<String?>(null)
    }
    var chosen by rememberSaveable(storeId) {
        mutableStateOf<Int?>(null)
    }
    var expanded by rememberSaveable(storeId) {
        mutableStateOf<Int?>(null)
    }

    LaunchedEffect(storeId, categories, catalogLoading, catalogError, retry) {
        if (storeId == null || catalogLoading || catalogError != null) {
            return@LaunchedEffect
        }

        failure = null

        try {
            panels = repository.loadCategoryPanels(
                storeId = storeId,
                categories = categories,
                forceRefresh = retry > 0
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            failure = exception.userMessage()
        }
    }

    val query = search.trim()
    val visible = remember(panels, query) {
        if (query.isEmpty()) {
            panels.orEmpty()
        } else {
            // Product matches take priority so "milk" finds milk, not just a category name.
            val productMatches = panels.orEmpty()
                .flatMap { panel -> panel.products + panel.groups.flatMap { it.products } }
                .distinctBy { it.id }
                .filter { product ->
                    product.name.contains(query, ignoreCase = true) ||
                            product.category.contains(query, ignoreCase = true)
                }

            if (productMatches.isNotEmpty()) {
                listOf(
                    CategoryPanel(
                        id = -1,
                        name = "Results for “$query”",
                        products = productMatches,
                        groups = emptyList()
                    )
                )
            } else {
                panels.orEmpty().mapNotNull { panel ->
                    if (panel.name.contains(query, ignoreCase = true)) {
                        panel
                    } else {
                        val matches = panel.groups.filter {
                            it.name.contains(query, ignoreCase = true)
                        }

                        if (matches.isEmpty()) {
                            null
                        } else {
                            panel.copy(products = emptyList(), groups = matches)
                        }
                    }
                }
            }
        }
    }

    val selected = visible.firstOrNull { it.id == chosen }
        ?: visible.firstOrNull { panel ->
            panel.id == preferredCategoryId ||
                    panel.groups.any { group -> group.id == preferredCategoryId }
        }
        ?: visible.firstOrNull { it.name.equals("Vegetables", ignoreCase = true) }
        ?: visible.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(pageSage)
    ) {
        Text(
            text = "All Categories",
            modifier = Modifier.padding(start = 16.dp, top = 15.dp, bottom = 12.dp),
            color = MH.Forest,
            fontFamily = MarketHallSerif,
            fontSize = 24.sp,
            lineHeight = 28.sp
        )

        val message = catalogError ?: failure

        when {
            message != null -> Notice(message, "Retry") {
                if (catalogError != null) {
                    onRetryCatalog()
                } else {
                    retry += 1
                }
            }

            catalogLoading -> Spinner()

            storeId == null -> Notice(
                message = "Choose a store to browse its categories.",
                button = "Choose store",
                action = onChooseStore
            )

            panels == null -> Spinner()

            panels.orEmpty().isEmpty() -> Notice(
                message = "This store has no products assigned to a category.",
                button = "Change store",
                action = onChooseStore
            )

            selected == null -> Text(
                text = "No categories or products match “$query”.",
                modifier = Modifier.padding(20.dp),
                color = ink
            )

            else -> Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    contentPadding = PaddingValues(bottom = 5.dp)
                ) {
                    items(visible, key = { it.id }) { category ->
                        RailTile(
                            name = category.name,
                            selected = category.id == selected.id,
                            onClick = {
                                if (selected.id != category.id) {
                                    expanded = null
                                }
                                chosen = category.id
                            }
                        )
                    }
                }

                key(storeId, selected.id) {
                    DetailPanel(
                        panel = selected,
                        expanded = expanded,
                        onExpand = { id ->
                            expanded = if (expanded == id) null else id
                        },
                        onOpen = onOpenProduct,
                        onAll = { onSeeAll(selected.id) },
                        modifier = Modifier
                            .weight(0.75f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun RailTile(
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) selectedTile else tileIvory)
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp)
                .padding(horizontal = 5.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CategoryNameIcon(name, Modifier.size(30.dp))

            Spacer(Modifier.height(6.dp))

            Text(
                text = name,
                color = ink,
                fontSize = 13.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (selected) {
            Box(Modifier.matchParentSize()) {
                Spacer(
                    Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(ink)
                )
            }
        }
    }
}

@Composable
private fun DetailPanel(
    panel: CategoryPanel,
    expanded: Int?,
    onExpand: (Int) -> Unit,
    onOpen: (Int) -> Unit,
    onAll: () -> Unit,
    modifier: Modifier
) {
    LazyColumn(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(panelIvory),
        contentPadding = PaddingValues(
            start = 10.dp,
            end = 10.dp,
            bottom = 14.dp
        )
    ) {
        item(key = "heading") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = panel.name,
                    modifier = Modifier.weight(1f),
                    color = MH.Forest,
                    fontFamily = MarketHallSerif,
                    fontSize = 20.sp,
                    lineHeight = 24.sp
                )

                TextButton(onClick = onAll) {
                    Text(
                        text = "See all",
                        color = olive,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Rule()
        }

        panel.groups.forEach { group ->
            val open = expanded == group.id

            item(key = "group-${group.id}") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onExpand(group.id) }
                        .semantics {
                            stateDescription = if (open) "Expanded" else "Collapsed"
                        }
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 3.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = group.name,
                        modifier = Modifier.weight(1f),
                        color = ink,
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Icon(
                        imageVector = if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = ink
                    )
                }

                if (!open) Rule()
            }

            if (open) {
                items(
                    items = group.products.chunked(2),
                    key = { row -> "g-${group.id}-${row.first().id}" }
                ) { pair ->
                    ProductRow(pair, onOpen)
                }

                item(key = "end-${group.id}") {
                    Rule()
                }
            }
        }

        items(
            items = panel.products.chunked(2),
            key = { row -> "p-${row.first().id}" }
        ) { pair ->
            ProductRow(pair, onOpen)
        }
    }
}

@Composable
private fun ProductRow(
    products: List<Product>,
    onOpen: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        products.forEachIndexed { index, product ->
            MarketHallProductCard(
                product = product,
                onOpen = onOpen,
                modifier = Modifier.weight(1f),
                entranceIndex = index
            )
        }

        if (products.size == 1) {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun Rule() {
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(dividerColor)
    )
}

@Composable
private fun Spinner() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = ink)
    }
}

@Composable
private fun Notice(
    message: String,
    button: String,
    action: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .background(panelIvory, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(message, color = ink)

        TextButton(onClick = action) {
            Text(button, color = olive)
        }
    }
}