package com.example.domain.model

data class PricePoint(
    val timestampSec: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

data class StockSeries(
    val symbol: String,
    val displayName: String,
    val exchange: String,
    val currency: String,
    val currentPrice: Double,
    val previousClose: Double,
    val marketState: String,
    val regularMarketTimeSec: Long,
    val points: List<PricePoint>
) {
    val changeAmount: Double get() = currentPrice - previousClose
    val changePercent: Double
        get() = if (previousClose > 0.0) (changeAmount / previousClose) * 100.0 else 0.0
}

data class TickerSearchResult(
    val symbol: String,
    val shortName: String,
    val exchange: String,
    val quoteType: String
)

data class SynchronizedPoint(
    val timestampSec: Long,
    val priceA: Double,
    val priceB: Double?,
    val normalizedPctA: Double,
    val normalizedPctB: Double?,
    val rollingCorrelation: Double?
)

data class SynchronizedAnalysis(
    val seriesA: StockSeries,
    val seriesB: StockSeries?,
    val points: List<SynchronizedPoint>,
    val overallCorrelation: Double?,
    val latestRollingCorrelation: Double?,
    val rSquared: Double?,
    val sampleCount: Int,
    val rollingWindow: Int
)

data class CuratedPair(
    val symbolA: String,
    val symbolB: String,
    val title: String,
    val subtitle: String
)
