package com.example

import com.example.domain.engine.CorrelationEngine
import com.example.domain.model.AlertCondition
import com.example.domain.model.CorrelationAlertRule
import com.example.domain.model.CorrelationMode
import com.example.domain.model.PricePoint
import com.example.domain.model.StockSeries
import com.example.domain.model.TimeframeOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrelationEngineTest {

    private fun makeSeries(symbol: String, timestamps: List<Long>, prices: List<Double>): StockSeries {
        val pts = timestamps.zip(prices).map { (ts, p) ->
            PricePoint(ts, p, p, p, p, 1000L)
        }
        return StockSeries(
            symbol = symbol,
            displayName = symbol,
            exchange = "NASDAQ",
            currency = "USD",
            currentPrice = prices.last(),
            previousClose = prices.first(),
            marketState = "REGULAR",
            regularMarketTimeSec = timestamps.last(),
            points = pts
        )
    }

    @Test
    fun pearsonCorrelation_perfectPositiveAndNegative() {
        val x = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val yPos = listOf(10.0, 20.0, 30.0, 40.0, 50.0)
        val yNeg = listOf(50.0, 40.0, 30.0, 20.0, 10.0)

        val rPos = CorrelationEngine.pearsonCorrelation(x, yPos)
        val rNeg = CorrelationEngine.pearsonCorrelation(x, yNeg)

        assertNotNull(rPos)
        assertNotNull(rNeg)
        assertEquals(1.0, rPos!!, 1e-6)
        assertEquals(-1.0, rNeg!!, 1e-6)
    }

    @Test
    fun analyze_synchronizesTimestampsAndComputesRollingCorrelation() {
        val baseTs = 1_700_000_000L
        val tsA = (0 until 20).map { baseTs + it * 300L + 12L } // slight offset within 5m bucket
        val tsB = (2 until 22).map { baseTs + it * 300L + 45L } // overlapping 18 buckets

        val pricesA = (0 until 20).map { 100.0 + it * 2.0 }
        val pricesB = (2 until 22).map { 200.0 - it * 1.5 }

        val seriesA = makeSeries("AAA", tsA, pricesA)
        val seriesB = makeSeries("BBB", tsB, pricesB)

        val analysis = CorrelationEngine.analyze(
            seriesA = seriesA,
            seriesB = seriesB,
            timeframe = TimeframeOption.MIN_5,
            mode = CorrelationMode.PRICES,
            rollingWindow = 6
        )

        assertEquals(18, analysis.sampleCount)
        assertNotNull(analysis.overallCorrelation)
        assertEquals(-1.0, analysis.overallCorrelation!!, 1e-6)
        assertNotNull(analysis.latestRollingCorrelation)
        assertEquals(-1.0, analysis.latestRollingCorrelation!!, 1e-6)
    }

    @Test
    fun evaluateAlerts_triggersOnThresholdAndRespectsCooldown() {
        val rule = CorrelationAlertRule(
            id = "r1",
            symbolA = "NVDA",
            symbolB = "AMD",
            applyToAllPairs = false,
            condition = AlertCondition.ABOVE,
            threshold = 0.80,
            enabled = true,
            systemNotification = true
        )

        val (updated1, events1) = CorrelationEngine.evaluateAlerts(
            rules = listOf(rule),
            symbolA = "AMD",
            symbolB = "NVDA", // reversed order should still match symmetrically
            observedCorrelation = 0.87,
            nowMillis = 100_000L
        )
        assertEquals(1, events1.size)
        assertEquals(0.87, events1.first().observedCorrelation, 1e-6)

        // Immediate re-evaluation within cooldown should not re-trigger
        val (_, events2) = CorrelationEngine.evaluateAlerts(
            rules = updated1,
            symbolA = "NVDA",
            symbolB = "AMD",
            observedCorrelation = 0.90,
            nowMillis = 120_000L
        )
        assertTrue(events2.isEmpty())

        // After cooldown (90s+), should re-trigger
        val (_, events3) = CorrelationEngine.evaluateAlerts(
            rules = updated1,
            symbolA = "NVDA",
            symbolB = "AMD",
            observedCorrelation = 0.91,
            nowMillis = 200_000L
        )
        assertEquals(1, events3.size)
    }
}
