package br.com.zenith.data.sleep

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max

enum class SleepDeviceEventType {
    ScreenOff,
    ScreenOn,
    UserPresent
}

data class SleepDeviceEvent(
    val occurredAtMillis: Long,
    val type: SleepDeviceEventType
)

data class SleepDeviceEstimate(
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val durationMinutes: Int,
    val interruptions: Int,
    val confidence: Float
)

object SleepDeviceEstimateCalculator {
    private const val MIN_SLEEP_MINUTES = 90
    private const val INTERRUPTION_PENALTY = 0.08f

    fun estimateForDate(
        settings: SleepMonitoringSettings,
        events: List<SleepDeviceEvent>,
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): SleepDeviceEstimate? {
        if (!settings.enabled || !settings.useDeviceEstimate) return null

        val windowStart = date
            .atStartOfDay(zoneId)
            .plusMinutes(settings.restWindowStartMinutes.toLong())

        val windowEndDate = if (settings.restWindowEndMinutes <= settings.restWindowStartMinutes) {
            date.plusDays(1)
        } else {
            date
        }

        val windowEnd = windowEndDate
            .atStartOfDay(zoneId)
            .plusMinutes(settings.restWindowEndMinutes.toLong())

        val windowStartMillis = windowStart.toInstant().toEpochMilli()
        val windowEndMillis = windowEnd.toInstant().toEpochMilli()
        val eventsInWindow = events
            .filter { it.occurredAtMillis in windowStartMillis..windowEndMillis }
            .sortedBy { it.occurredAtMillis }

        val inactiveRanges = buildInactiveRanges(
            events = eventsInWindow,
            windowStartMillis = windowStartMillis,
            windowEndMillis = windowEndMillis
        )

        val longestInactiveRange = inactiveRanges.maxByOrNull { it.endMillis - it.startMillis }
            ?: return null

        val durationMinutes = ((longestInactiveRange.endMillis - longestInactiveRange.startMillis) / 60_000L).toInt()
        if (durationMinutes < MIN_SLEEP_MINUTES) return null

        val unlocks = eventsInWindow.count { it.type == SleepDeviceEventType.UserPresent }
        val screenOns = eventsInWindow.count { it.type == SleepDeviceEventType.ScreenOn }
        val interruptions = if (unlocks > 0) unlocks else screenOns

        val confidence = calculateConfidence(durationMinutes, interruptions)

        return SleepDeviceEstimate(
            startedAtMillis = longestInactiveRange.startMillis,
            endedAtMillis = longestInactiveRange.endMillis,
            durationMinutes = durationMinutes,
            interruptions = interruptions,
            confidence = confidence
        )
    }

    private fun buildInactiveRanges(
        events: List<SleepDeviceEvent>,
        windowStartMillis: Long,
        windowEndMillis: Long
    ): List<InactiveRange> {
        val ranges = mutableListOf<InactiveRange>()
        var screenOffStartedAt: Long? = null

        events.forEach { event ->
            when (event.type) {
                SleepDeviceEventType.ScreenOff -> {
                    if (screenOffStartedAt == null) {
                        screenOffStartedAt = max(event.occurredAtMillis, windowStartMillis)
                    }
                }

                SleepDeviceEventType.ScreenOn,
                SleepDeviceEventType.UserPresent -> {
                    val startedAt = screenOffStartedAt
                    if (startedAt != null && event.occurredAtMillis > startedAt) {
                        ranges.add(
                            InactiveRange(
                                startMillis = startedAt,
                                endMillis = event.occurredAtMillis
                            )
                        )
                    }
                    screenOffStartedAt = null
                }
            }
        }

        val openRangeStart = screenOffStartedAt ?: inferWindowStartIfNoScreenEvents(events, windowStartMillis)
        if (openRangeStart != null && windowEndMillis > openRangeStart) {
            ranges.add(
                InactiveRange(
                    startMillis = openRangeStart,
                    endMillis = windowEndMillis
                )
            )
        }

        return ranges
    }

    private fun inferWindowStartIfNoScreenEvents(
        events: List<SleepDeviceEvent>,
        windowStartMillis: Long
    ): Long? {
        return if (events.none { it.type == SleepDeviceEventType.ScreenOn || it.type == SleepDeviceEventType.UserPresent }) {
            windowStartMillis
        } else {
            null
        }
    }

    private fun calculateConfidence(durationMinutes: Int, interruptions: Int): Float {
        val durationScore = when {
            durationMinutes >= 6 * 60 -> 0.92f
            durationMinutes >= 4 * 60 -> 0.78f
            else -> 0.62f
        }
        return (durationScore - interruptions * INTERRUPTION_PENALTY).coerceIn(0.1f, 0.95f)
    }

    private data class InactiveRange(
        val startMillis: Long,
        val endMillis: Long
    )
}
