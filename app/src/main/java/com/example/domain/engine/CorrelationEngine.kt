package com.example.domain.engine

import com.example.domain.model.AlertCondition
import com.example.domain.model.AlertTriggerEvent
import com.example.domain.model.CorrelationAlertRule
import com.example.domain.model.CorrelationMode
import com.example.domain.model.StockSeries
import com.example.domain.model.SynchronizedAnalysis
import com.example.domain.model.SynchronizedPoint
import com.example.domain.model.TimeframeOption
import java.util.UUID
import kotlin.math.abs
import kotlin.math.sqrt

object CorrelationEngine {

    private const val ALERT_COOLDOWN_MILLIS = 90_000L

    fun analyze(
        seriesA: StockSeries,
        seriesB: StockSeries?,
        timeframe: TimeframeOption,
        mode: CorrelationMode,
        rollingWindow: Int = timeframe.defaultRollingWindow
    ): SynchronizedAnalysis {
        if (seriesB == null) {
            val pointsA = seriesA.points
            val baseA = pointsA.firstOrNull()?.close?.takeIf { it > 0.0 } ?: 1.0
            val syncPoints = pointsA.map { pt ->
                SynchronizedPoint(
                    timestampSec = pt.timestampSec,
                    priceA = pt.close,
                    priceB = null,
                    normalizedPctA = ((pt.close - baseA) / baseA) * 100.0,
                    normalizedPctB = null,
                    rollingCorrelation = null
                )
            }
            return SynchronizedAnalysis(
                seriesA = seriesA,
                seriesB = null,
                points = syncPoints,
                overallCorrelation = null,
                latestRollingCorrelation = null,
                rSquared = null,
                sampleCount = syncPoints.size,
                rollingWindow = rollingWindow
            )
        }

        val bucketSize = timeframe.bucketSeconds
        val mapA = LinkedHashMap<Long, Double>()
        for (pt in seriesA.points) {
            val bucket = (pt.timestampSec / bucketSize) * bucketSize
            mapA[bucket] = pt.close
        }

        val mapB = LinkedHashMap<Long, Double>()
        for (pt in seriesB.points) {
            val bucket = (pt.timestampSec / bucketSize) * bucketSize
            mapB[bucket] = pt.close
        }

        val commonTimestamps = mapA.keys.filter { mapB.containsKey(it) }.sorted()
        if (commonTimestamps.isEmpty()) {
            return SynchronizedAnalysis(
                seriesA = seriesA,
                seriesB = seriesB,
                points = emptyList(),
                overallCorrelation = null,
                latestRollingCorrelation = null,
                rSquared = null,
                sampleCount = 0,
                rollingWindow = rollingWindow
            )
        }

        val pricesA = commonTimestamps.map { mapA.getValue(it) }
        val pricesB = commonTimestamps.map { mapB.getValue(it) }

        val baseA = pricesA.firstOrNull()?.takeIf { it > 0.0 } ?: 1.0
        val baseB = pricesB.firstOrNull()?.takeIf { it > 0.0 } ?: 1.0

        val effectiveWindow = rollingWindow.coerceIn(5, maxOf(5, commonTimestamps.size / 2))

        val rollingCorrs = when (mode) {
            CorrelationMode.RETURNS -> computeRollingReturnsCorrelation(pricesA, pricesB, effectiveWindow)
            CorrelationMode.PRICES -> computeRollingPriceCorrelation(pricesA, pricesB, effectiveWindow)
        }

        val overallCorrelation = when (mode) {
            CorrelationMode.RETURNS -> {
                if (pricesA.size < 4) null
                else {
                    val retA = computeReturns(pricesA)
                    val retB = computeReturns(pricesB)
                    pearsonCorrelation(retA, retB)
                }
            }
            CorrelationMode.PRICES -> {
                if (pricesA.size < 3) null else pearsonCorrelation(pricesA, pricesB)
            }
        }

        val syncPoints = commonTimestamps.indices.map { i ->
            val pA = pricesA[i]
            val pB = pricesB[i]
            SynchronizedPoint(
                timestampSec = commonTimestamps[i],
                priceA = pA,
                priceB = pB,
                normalizedPctA = ((pA - baseA) / baseA) * 100.0,
                normalizedPctB = ((pB - baseB) / baseB) * 100.0,
                rollingCorrelation = rollingCorrs[i]
            )
        }

        val latestRolling = rollingCorrs.lastOrNull { it != null }
        val rSquared = overallCorrelation?.let { it * it }

        return SynchronizedAnalysis(
            seriesA = seriesA,
            seriesB = seriesB,
            points = syncPoints,
            overallCorrelation = overallCorrelation,
            latestRollingCorrelation = latestRolling,
            rSquared = rSquared,
            sampleCount = syncPoints.size,
            rollingWindow = effectiveWindow
        )
    }

    private fun computeReturns(prices: List<Double>): List<Double> {
        val returns = ArrayList<Double>(maxOf(0, prices.size - 1))
        for (i in 1 until prices.size) {
            val prev = prices[i - 1]
            val curr = prices[i]
            returns.add(if (prev > 0.0) (curr - prev) / prev else 0.0)
        }
        return returns
    }

    private fun computeRollingReturnsCorrelation(
        pricesA: List<Double>,
        pricesB: List<Double>,
        window: Int
    ): List<Double?> {
        val n = pricesA.size
        val result = MutableList<Double?>(n) { null }
        if (n <= window || window < 3) return result

        val retA = computeReturns(pricesA)
        val retB = computeReturns(pricesB)

        // point i maps to return index (i - 1)
        for (i in window until n) {
            val endRetIdx = i - 1
            val startRetIdx = endRetIdx - window + 1
            if (startRetIdx >= 0) {
                result[i] = pearsonCorrelation(
                    retA.subList(startRetIdx, endRetIdx + 1),
                    retB.subList(startRetIdx, endRetIdx + 1)
                )
            }
        }
        return result
    }

    private fun computeRollingPriceCorrelation(
        pricesA: List<Double>,
        pricesB: List<Double>,
        window: Int
    ): List<Double?> {
        val n = pricesA.size
        val result = MutableList<Double?>(n) { null }
        if (n < window || window < 3) return result

        for (i in (window - 1) until n) {
            val start = i - window + 1
            result[i] = pearsonCorrelation(
                pricesA.subList(start, i + 1),
                pricesB.subList(start, i + 1)
            )
        }
        return result
    }

    fun pearsonCorrelation(xs: List<Double>, ys: List<Double>): Double? {
        val n = minOf(xs.size, ys.size)
        if (n < 3) return null

        var sumX = 0.0
        var sumY = 0.0
        for (i in 0 until n) {
            sumX += xs[i]
            sumY += ys[i]
        }
        val meanX = sumX / n
        val meanY = sumY / n

        var num = 0.0
        var denX = 0.0
        var denY = 0.0
        for (i in 0 until n) {
            val dx = xs[i] - meanX
            val dy = ys[i] - meanY
            num += dx * dy
            denX += dx * dx
            denY += dy * dy
        }
        val denom = sqrt(denX * denY)
        if (denom < 1e-12) return 0.0
        return (num / denom).coerceIn(-1.0, 1.0)
    }

    fun isConditionMet(condition: AlertCondition, threshold: Double, correlation: Double): Boolean {
        return when (condition) {
            AlertCondition.ABOVE -> correlation > threshold
            AlertCondition.BELOW -> correlation < threshold
            AlertCondition.ABS_ABOVE -> abs(correlation) > abs(threshold)
            AlertCondition.ABS_BELOW -> abs(correlation) < abs(threshold)
        }
    }

    fun ruleMatchesPair(rule: CorrelationAlertRule, symbolA: String, symbolB: String): Boolean {
        if (rule.applyToAllPairs) return true
        val directMatch = rule.symbolA.equals(symbolA, ignoreCase = true) &&
            rule.symbolB.equals(symbolB, ignoreCase = true)
        val reverseMatch = rule.symbolA.equals(symbolB, ignoreCase = true) &&
            rule.symbolB.equals(symbolA, ignoreCase = true)
        return directMatch || reverseMatch
    }

    fun evaluateAlerts(
        rules: List<CorrelationAlertRule>,
        symbolA: String,
        symbolB: String?,
        observedCorrelation: Double?,
        nowMillis: Long = System.currentTimeMillis()
    ): Pair<List<CorrelationAlertRule>, List<AlertTriggerEvent>> {
        if (symbolB == null || observedCorrelation == null) {
            return rules to emptyList()
        }

        val triggeredEvents = mutableListOf<AlertTriggerEvent>()
        val updatedRules = rules.map { rule ->
            if (!rule.enabled || !ruleMatchesPair(rule, symbolA, symbolB)) {
                rule
            } else {
                val breached = isConditionMet(rule.condition, rule.threshold, observedCorrelation)
                val lastAt = rule.lastTriggeredAtMillis
                val cooldownElapsed = lastAt == null || (nowMillis - lastAt) >= ALERT_COOLDOWN_MILLIS
                if (breached && cooldownElapsed) {
                    triggeredEvents.add(
                        AlertTriggerEvent(
                            id = UUID.randomUUID().toString(),
                            ruleId = rule.id,
                            symbolA = symbolA,
                            symbolB = symbolB,
                            condition = rule.condition,
                            threshold = rule.threshold,
                            observedCorrelation = observedCorrelation,
                            triggeredAtMillis = nowMillis
                        )
                    )
                    rule.copy(
                        lastTriggeredAtMillis = nowMillis,
                        lastTriggeredValue = observedCorrelation
                    )
                } else {
                    rule
                }
            }
        }
        return updatedRules to triggeredEvents
    }

    fun describeCorrelationStrength(r: Double?): String {
        if (r == null) return "Dati insufficienti"
        return when {
            r >= 0.80 -> "Positiva Molto Forte"
            r >= 0.55 -> "Positiva Forte"
            r >= 0.25 -> "Positiva Moderata"
            r > -0.25 -> "Debole / Neutra"
            r > -0.55 -> "Negativa Moderata"
            r > -0.80 -> "Negativa Forte"
            else -> "Inversa Molto Forte"
        }
    }
}
