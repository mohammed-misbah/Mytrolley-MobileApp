package com.mytrolley.app.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Anonymous, read-only customer catalogue.
 *
 * Base URL defaults to https://products.mytrolley.ae/api/v1/ (production) and
 * can be overridden to http://10.0.2.2:8000/api/v1/ for local development, so
 * these relative paths resolve to /api/v1/public/....
 * No tokens, cookies or customer accounts are involved.
 */
interface MyTrolleyApiService {
    @GET("public/stores/")
    suspend fun stores(): Response<List<ApiStore>>

    @GET("public/categories/")
    suspend fun categories(): Response<List<ApiCategory>>

    @GET("public/products/")
    suspend fun products(
        @Query("store") storeId: Int,
        @Query("search") search: String? = null,
        @Query("category") categoryId: Int? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): Response<PagedProductsResponse>

    @GET("public/products/{productId}/")
    suspend fun productDetails(
        @Path("productId") productId: Int,
        @Query("store") storeId: Int? = null
    ): Response<ApiProductDetail>
}
