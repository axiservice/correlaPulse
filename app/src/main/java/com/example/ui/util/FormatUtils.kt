package com.example.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object FormatUtils {

    fun formatPrice(price: Double, currency: String = "USD"): String {
        val symbol = when (currency.uppercase()) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "CHF" -> "CHF "
            else -> "$currency "
        }
        val pattern = when {
            abs(price) >= 1000.0 -> "%.2f"
            abs(price) >= 1.0 -> "%.2f"
            else -> "%.4f"
        }
        return "$symbol${String.format(Locale.US, pattern, price)}"
    }

    fun formatSignedPercent(pct: Double): String {
        return String.format(Locale.US, "%+.2f%%", pct)
    }

    fun formatCorrelation(r: Double?): String {
        return if (r == null) "—" else String.format(Locale.US, "%+.2f", r)
    }

    fun formatThreshold(threshold: Double): String {
        return String.format(Locale.US, "%+.2f", threshold)
    }

    fun formatTimestamp(epochSec: Long, isDaily: Boolean = false): String {
        val date = Date(epochSec * 1000L)
        val pattern = if (isDaily) "dd MMM yyyy" else "dd MMM · HH:mm"
        return SimpleDateFormat(pattern, Locale.ITALIAN).format(date)
    }

    fun formatShortAxisTime(epochSec: Long, isDaily: Boolean = false): String {
        val date = Date(epochSec * 1000L)
        val pattern = if (isDaily) "dd/MM" else "HH:mm"
        return SimpleDateFormat(pattern, Locale.ITALIAN).format(date)
    }

    fun formatMillisDateTime(millis: Long): String {
        val date = Date(millis)
        return SimpleDateFormat("dd MMM HH:mm:ss", Locale.ITALIAN).format(date)
    }

    fun marketStateLabel(state: String): String {
        return when (state.uppercase()) {
            "REGULAR" -> "MERCATO APERTO"
            "PRE", "PREPRE" -> "PRE-MARKET"
            "POST", "POSTPOST" -> "AFTER-HOURS"
            "CLOSED" -> "MERCATO CHIUSO"
            else -> "DATI LIVE"
        }
    }
}
