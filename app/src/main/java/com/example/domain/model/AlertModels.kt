package com.example.domain.model

import com.squareup.moshi.JsonClass

enum class AlertCondition(
    val shortLabel: String,
    val description: String
) {
    ABOVE("Sopra (>)", "Scatta quando r supera la soglia"),
    BELOW("Sotto (<)", "Scatta quando r scende sotto la soglia"),
    ABS_ABOVE("Forte (|r| >)", "Scatta quando |r| supera la soglia (+ o -)"),
    ABS_BELOW("Decorrel. (|r| <)", "Scatta quando |r| scende vicino a zero")
}

@JsonClass(generateAdapter = true)
data class CorrelationAlertRule(
    val id: String,
    val symbolA: String,
    val symbolB: String,
    val applyToAllPairs: Boolean = false,
    val condition: AlertCondition,
    val threshold: Double,
    val enabled: Boolean = true,
    val systemNotification: Boolean = true,
    val lastTriggeredAtMillis: Long? = null,
    val lastTriggeredValue: Double? = null,
    val note: String = ""
)

@JsonClass(generateAdapter = true)
data class AlertTriggerEvent(
    val id: String,
    val ruleId: String,
    val symbolA: String,
    val symbolB: String,
    val condition: AlertCondition,
    val threshold: Double,
    val observedCorrelation: Double,
    val triggeredAtMillis: Long
)
