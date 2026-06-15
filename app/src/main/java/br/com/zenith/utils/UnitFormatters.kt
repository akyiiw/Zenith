package br.com.zenith.utils

import java.util.Locale
import kotlin.math.roundToInt

object UnitFormatters {
    private val ptBr: Locale = Locale.forLanguageTag("pt-BR")

    fun compactNumber(value: Double, maxDecimals: Int = 1): String {
        val rounded = if (maxDecimals <= 0) value.roundToInt().toDouble() else value
        return if (rounded % 1.0 == 0.0) {
            rounded.toInt().toString()
        } else {
            "%.${maxDecimals}f".format(ptBr, rounded).trimEnd('0').trimEnd(',')
        }
    }

    fun kilometers(value: Double): String = "${compactNumber(value)}km"

    fun kilometersWithSpace(value: Double): String = "${compactNumber(value)} km"

    fun steps(value: Int): String = "%,d".format(ptBr, value)

    fun minutes(totalMinutes: Int?): String {
        if (totalMinutes == null || totalMinutes <= 0) return "-"
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }
}
