package com.example.domain.model

enum class TimeframeOption(
    val interval: String,
    val range: String,
    val label: String,
    val shortLabel: String,
    val bucketSeconds: Long,
    val defaultRollingWindow: Int
) {
    MIN_1("1m", "1d", "1 min · Oggi", "1M", 60L, 15),
    MIN_5("5m", "5d", "5 min · 5 Giorni", "5M", 300L, 20),
    MIN_15("15m", "1mo", "15 min · 1 Mese", "15M", 900L, 20),
    HOUR_1("1h", "3mo", "1 ora · 3 Mesi", "1H", 3600L, 20),
    DAY_1("1d", "1y", "1 giorno · 1 Anno", "1G", 86400L, 20)
}

enum class CorrelationMode(val label: String, val description: String) {
    RETURNS("Rendimenti", "Correlazione sulle variazioni % periodo per periodo"),
    PRICES("Prezzi", "Correlazione diretta sull'andamento dei livelli di prezzo")
}
