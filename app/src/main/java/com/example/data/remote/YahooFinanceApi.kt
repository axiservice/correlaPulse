package com.example.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface YahooFinanceApi {

    @GET("v8/finance/chart/{symbol}")
    suspend fun getChart(
        @Path("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("range") range: String,
        @Query("includePrePost") includePrePost: Boolean = false
    ): YahooChartResponse

    @GET
    suspend fun searchTickers(
        @Url url: String = "https://query2.finance.yahoo.com/v1/finance/search",
        @Query("q") query: String,
        @Query("quotesCount") quotesCount: Int = 15,
        @Query("newsCount") newsCount: Int = 0
    ): YahooSearchResponse
}
