package com.mytrolley.app.ui.home

import com.mytrolley.app.ui.theme.MH
import com.mytrolley.app.ui.theme.MarketHallSerif
import com.mytrolley.app.ui.sheetEntrance
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class StoreChoice(
    val id: Int,
    val name: String,
    val detail: String
)

private val SheetIvory = MH.Cream
private val SheetForest = MH.ForestButton
private val SheetInk = MH.Ink
private val SheetMuted = MH.Muted
private val SheetSage = MH.Sage
private val SheetOutline = MH.Divider
private val SheetLime = MH.LimeBright

@Composable
internal fun StoreSelectionSheet(
    stores: List<StoreChoice>,
    currentStoreId: Int?,
    loading: Boolean,
    error: String?,
    onClose: () -> Unit,
    onConfirm: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focus = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    var pendingId by rememberSaveable(currentStoreId) {
        mutableStateOf(currentStoreId)
    }

    val matches = remember(stores, query) {
        val term = query.trim()
        stores.filter { store ->
            term.isEmpty() ||
                    store.name.contains(term, ignoreCase = true) ||
                    store.detail.contains(term, ignoreCase = true)
        }
    }

    val pendingStore = stores.firstOrNull { it.id == pendingId }
    val shape = MH.SheetShape

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SheetIvory)
            .semantics { paneTitle = "Choose your store" }
            .padding(horizontal = 18.dp)
            .sheetEntrance()
    ) {
        Spacer(Modifier.height(11.dp))
        Spacer(
            Modifier
                .align(Alignment.CenterHorizontally)
                .width(45.dp)
                .height(5.dp)
                .background(SheetOutline, CircleShape)
        )

        // The search and rows scroll together on short screens.
        // The confirmation button stays below the list.
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .selectableGroup(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 5.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "title") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Choose your store",
                            modifier = Modifier.weight(1f),
                            color = MH.Forest,
                            fontFamily = MarketHallSerif,
                            fontSize = 22.sp,
                            lineHeight = 26.sp
                        )

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(44.dp)
                                .semantics { contentDescription = "Close store picker" }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(SheetSage, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = SheetForest
                                )
                            }
                        }
                    }

                    Text(
                        "Products and stock change by store.",
                        color = SheetMuted,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            item(key = "search") {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .semantics { contentDescription = "Search stores" },
                    singleLine = true,
                    textStyle = TextStyle(color = SheetInk, fontSize = 15.sp),
                    cursorBrush = SolidColor(SheetForest),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    decorationBox = { inner ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 15.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = SheetMuted,
                                modifier = Modifier.size(19.dp)
                            )
                            Box(Modifier.weight(1f)) {
                                if (query.isEmpty()) {
                                    Text("Search stores", color = SheetMuted, fontSize = 15.sp)
                                }
                                inner()
                            }
                        }
                    }
                )
            }

            when {
                stores.isEmpty() && loading -> item(key = "loading") {
                    Box(
                        Modifier.fillMaxWidth().padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SheetForest)
                    }
                }

                stores.isEmpty() && error != null -> item(key = "error") {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text(error, color = SheetMuted)
                        TextButton(onClick = onRetry) { Text("Retry", color = SheetForest) }
                    }
                }

                stores.isEmpty() -> item(key = "empty") {
                    Text(
                        "No active stores are available.",
                        Modifier.padding(vertical = 12.dp),
                        color = SheetMuted
                    )
                }

                matches.isEmpty() -> item(key = "no_matches") {
                    Text(
                        "No stores match your search.",
                        Modifier.padding(vertical = 12.dp),
                        color = SheetMuted
                    )
                }

                else -> items(matches, key = { it.id }) { store ->
                    StoreChoiceRow(
                        store = store,
                        selected = pendingId == store.id,
                        onSelect = {
                            pendingId = store.id
                            focus.clearFocus()
                        }
                    )
                }
            }
        }

        Button(
            onClick = {
                pendingStore?.let { store ->
                    focus.clearFocus()
                    onConfirm(store.id)
                }
            },
            enabled = pendingStore != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 10.dp)
                .heightIn(min = 50.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MH.PrimaryButton,
                contentColor = MH.OnPrimaryButton,
                disabledContainerColor = MH.PrimaryButtonDisabled,
                disabledContentColor = MH.OnPrimaryButtonDisabled
            )
        ) {
            Text(
                "Confirm store",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(7.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MH.OnPrimaryButton,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun StoreChoiceRow(
    store: StoreChoice,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val outline = if (selected) Modifier.border(1.2.dp, SheetForest, shape) else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .then(outline)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .heightIn(min = 56.dp)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(if (selected) SheetForest else SheetSage, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            StorefrontIcon(if (selected) Color.White else SheetForest)
        }

        Column(Modifier.weight(1f)) {
            Text(
                store.name,
                color = SheetInk,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (store.detail.isNotBlank()) {
                Text(
                    store.detail,
                    color = SheetMuted,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (selected) {
            Box(
                Modifier.size(26.dp).background(SheetLime, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = SheetForest
                )
            }
        } else {
            Spacer(
                Modifier.size(23.dp).border(1.2.dp, SheetOutline, CircleShape)
            )
        }
    }
}

@Composable
private fun StorefrontIcon(tint: Color) {
    Icon(
        imageVector = Icons.Outlined.Storefront,
        contentDescription = null,
        modifier = Modifier.size(22.dp),
        tint = tint
    )
}

@Preview(
    name = "Store sheet — reference data",
    showBackground = true,
    widthDp = 390,
    heightDp = 510
)
@Composable
private fun StoreSelectionReferencePreview() {
    StoreSelectionSheet(
        stores = listOf(
            StoreChoice(1, "Palm Jumeirah", "1.2 km · Open until 11 PM"),
            StoreChoice(2, "Dubai Marina", "4.8 km · Open until 11 PM"),
            StoreChoice(3, "JLT", "7.1 km · Open until 10 PM"),
            StoreChoice(4, "Business Bay", "14 km · Opens 8 AM")
        ),
        currentStoreId = 1,
        loading = false,
        error = null,
        onClose = {},
        onConfirm = {},
        onRetry = {},
        modifier = Modifier.fillMaxSize()
    )
}