package com.example.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YahooChartResponse(
    val chart: YahooChartContainer?
)

@JsonClass(generateAdapter = true)
data class YahooChartContainer(
    val result: List<YahooChartResult>?,
    val error: YahooChartError?
)

@JsonClass(generateAdapter = true)
data class YahooChartError(
    val code: String?,
    val description: String?
)

@JsonClass(generateAdapter = true)
data class YahooChartResult(
    val meta: YahooChartMeta?,
    val timestamp: List<Long>?,
    val indicators: YahooIndicators?
)

@JsonClass(generateAdapter = true)
data class YahooChartMeta(
    val symbol: String?,
    val shortName: String?,
    val longName: String?,
    val currency: String?,
    val exchangeName: String?,
    val fullExchangeName: String?,
    val regularMarketPrice: Double?,
    val chartPreviousClose: Double?,
    val previousClose: Double?,
    val marketState: String?,
    val regularMarketTime: Long?
)

@JsonClass(generateAdapter = true)
data class YahooIndicators(
    val quote: List<YahooQuoteIndicator>?
)

@JsonClass(generateAdapter = true)
data class YahooQuoteIndicator(
    val open: List<Double?>?,
    val high: List<Double?>?,
    val low: List<Double?>?,
    val close: List<Double?>?,
    val volume: List<Long?>?
)

@JsonClass(generateAdapter = true)
data class YahooSearchResponse(
    val quotes: List<YahooSearchQuote>?
)

@JsonClass(generateAdapter = true)
data class YahooSearchQuote(
    val symbol: String?,
    val shortname: String?,
    val longname: String?,
    val exchange: String?,
    val exchDisp: String?,
    val quoteType: String?
)
