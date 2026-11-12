package com.example.data.repository

import com.example.data.remote.YahooChartResponse
import com.example.data.remote.YahooFinanceApi
import com.example.domain.model.CuratedPair
import com.example.domain.model.PricePoint
import com.example.domain.model.StockSeries
import com.example.domain.model.TickerSearchResult
import com.example.domain.model.TimeframeOption
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class MarketRepository(
    private val api: YahooFinanceApi
) {

    val curatedPairs: List<CuratedPair> = listOf(
        CuratedPair("NVDA", "AMD", "NVDA / AMD", "Semiconduttori AI"),
        CuratedPair("AAPL", "MSFT", "AAPL / MSFT", "Big Tech US"),
        CuratedPair("BTC-USD", "ETH-USD", "BTC / ETH", "Crypto Live 24/7"),
        CuratedPair("SPY", "QQQ", "SPY / QQQ", "Indici S&P500 vs Nasdaq"),
        CuratedPair("SPY", "GLD", "SPY / GLD", "Azionario vs Oro"),
        CuratedPair("ENI.MI", "ISP.MI", "ENI / ISP", "Piazza Affari Milano"),
        CuratedPair("EURUSD=X", "GLD", "EUR/USD / Oro", "Valute & Materie Prime")
    )

    val quickTickers: List<TickerSearchResult> = listOf(
        TickerSearchResult("NVDA", "NVIDIA Corp.", "NASDAQ", "EQUITY"),
        TickerSearchResult("AMD", "Advanced Micro Devices", "NASDAQ", "EQUITY"),
        TickerSearchResult("AAPL", "Apple Inc.", "NASDAQ", "EQUITY"),
        TickerSearchResult("MSFT", "Microsoft Corp.", "NASDAQ", "EQUITY"),
        TickerSearchResult("TSLA", "Tesla Inc.", "NASDAQ", "EQUITY"),
        TickerSearchResult("GOOGL", "Alphabet Inc.", "NASDAQ", "EQUITY"),
        TickerSearchResult("META", "Meta Platforms", "NASDAQ", "EQUITY"),
        TickerSearchResult("AMZN", "Amazon.com Inc.", "NASDAQ", "EQUITY"),
        TickerSearchResult("SPY", "SPDR S&P 500 ETF", "NYSEARCA", "ETF"),
        TickerSearchResult("QQQ", "Invesco QQQ Trust", "NASDAQ", "ETF"),
        TickerSearchResult("GLD", "SPDR Gold Shares", "NYSEARCA", "ETF"),
        TickerSearchResult("TLT", "iShares 20+ Year Treasury", "NASDAQ", "ETF"),
        TickerSearchResult("ENI.MI", "Eni S.p.A.", "MIL", "EQUITY"),
        TickerSearchResult("ISP.MI", "Intesa Sanpaolo", "MIL", "EQUITY"),
        TickerSearchResult("UCG.MI", "UniCredit S.p.A.", "MIL", "EQUITY"),
        TickerSearchResult("RACE.MI", "Ferrari N.V.", "MIL", "EQUITY"),
        TickerSearchResult("BTC-USD", "Bitcoin USD", "CCC", "CRYPTOCURRENCY"),
        TickerSearchResult("ETH-USD", "Ethereum USD", "CCC", "CRYPTOCURRENCY"),
        TickerSearchResult("EURUSD=X", "EUR / USD", "CCY", "CURRENCY")
    )

    suspend fun fetchSynchronizedPair(
        symbolA: String,
        symbolB: String?,
        timeframe: TimeframeOption
    ): Result<Pair<StockSeries, StockSeries?>> = runCatching {
        coroutineScope {
            val deferredA = async { fetchSeries(symbolA, timeframe) }
            val deferredB = symbolB?.let { async { fetchSeries(it, timeframe) } }
            val seriesA = deferredA.await()
            val seriesB = deferredB?.await()
            seriesA to seriesB
        }
    }

    suspend fun fetchSeries(symbol: String, timeframe: TimeframeOption): StockSeries {
        val cleanSymbol = symbol.trim().uppercase()
        val response: YahooChartResponse = api.getChart(
            symbol = cleanSymbol,
            interval = timeframe.interval,
            range = timeframe.range
        )
        val container = response.chart
        if (container?.error != null) {
            throw IllegalStateException(
                container.error.description ?: "Errore nel recupero di $cleanSymbol"
            )
        }
        val result = container?.result?.firstOrNull()
            ?: throw IllegalStateException("Nessun dato disponibile per $cleanSymbol")

        val meta = result.meta
        val timestamps = result.timestamp.orEmpty()
        val quote = result.indicators?.quote?.firstOrNull()
        val closes = quote?.close.orEmpty()
        val opens = quote?.open.orEmpty()
        val highs = quote?.high.orEmpty()
        val lows = quote?.low.orEmpty()
        val volumes = quote?.volume.orEmpty()

        val points = ArrayList<PricePoint>(timestamps.size)
        for (i in timestamps.indices) {
            val close = closes.getOrNull(i) ?: continue
            if (close.isNaN() || close <= 0.0) continue
            val open = opens.getOrNull(i)?.takeIf { !it.isNaN() && it > 0.0 } ?: close
            val high = highs.getOrNull(i)?.takeIf { !it.isNaN() && it > 0.0 } ?: close
            val low = lows.getOrNull(i)?.takeIf { !it.isNaN() && it > 0.0 } ?: close
            val vol = volumes.getOrNull(i) ?: 0L
            points.add(
                PricePoint(
                    timestampSec = timestamps[i],
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = vol
                )
            )
        }

        if (points.isEmpty()) {
            throw IllegalStateException("Serie prezzi vuota per $cleanSymbol")
        }

        val lastClose = points.last().close
        val regularPrice = meta?.regularMarketPrice?.takeIf { it > 0.0 } ?: lastClose
        val prevClose = meta?.chartPreviousClose?.takeIf { it > 0.0 }
            ?: meta?.previousClose?.takeIf { it > 0.0 }
            ?: points.first().close

        return StockSeries(
            symbol = meta?.symbol ?: cleanSymbol,
            displayName = meta?.shortName ?: meta?.longName ?: cleanSymbol,
            exchange = meta?.exchangeName ?: meta?.fullExchangeName ?: "",
            currency = meta?.currency ?: "USD",
            currentPrice = regularPrice,
            previousClose = prevClose,
            marketState = meta?.marketState ?: "UNKNOWN",
            regularMarketTimeSec = meta?.regularMarketTime ?: points.last().timestampSec,
            points = points
        )
    }

    suspend fun searchTickers(query: String): Result<List<TickerSearchResult>> = runCatching {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@runCatching quickTickers

        val response = api.searchTickers(query = trimmed)
        val remote = response.quotes.orEmpty()
            .mapNotNull { q ->
                val sym = q.symbol?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val type = q.quoteType.orEmpty()
                if (type !in setOf("EQUITY", "ETF", "INDEX", "CRYPTOCURRENCY", "CURRENCY", "FUTURE", "")) {
                    return@mapNotNull null
                }
                TickerSearchResult(
                    symbol = sym,
                    shortName = q.shortname ?: q.longname ?: sym,
                    exchange = q.exchDisp ?: q.exchange ?: "",
                    quoteType = type
                )
            }
        if (remote.isNotEmpty()) remote else {
            quickTickers.filter {
                it.symbol.contains(trimmed, ignoreCase = true) ||
                    it.shortName.contains(trimmed, ignoreCase = true)
            }
        }
    }
}
