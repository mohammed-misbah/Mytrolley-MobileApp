package com.mytrolley.app

import com.google.gson.Gson
import com.mytrolley.app.data.remote.ApiProductDetail
import com.mytrolley.app.data.remote.ApiProductListItem
import com.mytrolley.app.data.remote.CatalogPricing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Verifies the Android DTOs against the real public catalogue JSON contract. */
class CatalogPublicDtoTest {
    private val gson = Gson()

    @Test
    fun parsesNestedCategoryAndStorePrice() {
        val json = """
            {
              "id": 7,
              "name": "Milk",
              "slug": "milk",
              "description": "Fresh milk",
              "category": {"id": 2, "name": "Dairy", "slug": "dairy"},
              "is_active": true,
              "primary_image": {"id": 9, "image_url": "http://10.0.2.2:8000/api/v1/public/images/9/"},
              "starting_price": "15.0000",
              "selected_store_price": {
                "store": {"id": 1, "name": "Store A", "code": "A"},
                "price": "15.0000", "quantity": 300, "is_available": true
              },
              "available_stores": []
            }
        """.trimIndent()
        val item = gson.fromJson(json, ApiProductListItem::class.java)
        assertEquals("Dairy", item.category?.name)
        assertEquals("http://10.0.2.2:8000/api/v1/public/images/9/", item.primaryImage?.imageUrl)
        val offer = CatalogPricing.offerFor(item, 1)
        assertEquals("15.0000", offer?.price)
        assertEquals(300, offer?.quantity)
        assertNull(CatalogPricing.offerFor(item, 999))
    }

    @Test
    fun selectedStorePriceNeverFallsBackToAnotherStore() {
        val json = """
            {
              "id": 8, "name": "Bread",
              "selected_store_price": {
                "store": {"id": 2, "name": "Store B", "code": "B"},
                "price": "20.0000", "quantity": 200, "is_available": true
              },
              "available_stores": [
                {"store": {"id": 1, "name": "Store A", "code": "A"},
                 "price": "10.0000", "quantity": 5, "is_available": true}
              ]
            }
        """.trimIndent()
        val item = gson.fromJson(json, ApiProductListItem::class.java)
        assertEquals("10.0000", CatalogPricing.offerFor(item, 1)?.price)
        assertEquals("20.0000", CatalogPricing.offerFor(item, 2)?.price)
        assertNull(CatalogPricing.offerFor(item, 3))
    }

    @Test
    fun parsesDetailStoresAndSelectedStore() {
        val json = """
            {
              "id": 7, "name": "Milk", "description": "Fresh",
              "category": {"id": 2, "name": "Dairy", "slug": "dairy"},
              "attributes": [{"id": 1, "name": "Brand", "value": "Acme"}],
              "images": [{"id": 9, "image_url": "http://10.0.2.2:8000/api/v1/public/images/9/", "is_primary": true}],
              "stores": [
                {"store": {"id": 1, "name": "Store A", "code": "A"}, "price": "15.0000", "quantity": 300, "is_available": true},
                {"store": {"id": 2, "name": "Store B", "code": "B"}, "price": "20.0000", "quantity": 200, "is_available": true}
              ],
              "selected_store": {"store": {"id": 2, "name": "Store B", "code": "B"}, "price": "20.0000", "quantity": 200, "is_available": true}
            }
        """.trimIndent()
        val detail = gson.fromJson(json, ApiProductDetail::class.java)
        assertEquals(2, detail.stores.size)
        assertEquals("Brand", detail.attributes.first().name)
        assertEquals("20.0000", CatalogPricing.offerFor(detail, 2)?.price)
        assertEquals("15.0000", CatalogPricing.offerFor(detail, 1)?.price)
    }

    @Test
    fun formatsAedCurrencyConsistentlyAndShowsZeroStock() {
        assertEquals("AED 15.00", CatalogPricing.format(15.0))
        assertEquals("AED 0.00", CatalogPricing.format(0.0))
    }
}
