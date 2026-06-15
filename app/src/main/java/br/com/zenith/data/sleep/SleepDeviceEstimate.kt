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
        zoneId: ZoneId = ZoneId.systemDefault(),
        maxSleepMinutes: Int = 14 * 60
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
        val analysisEndMillis = windowStartMillis + maxSleepMinutes.coerceAtLeast(MIN_SLEEP_MINUTES) * 60_000L
        val eventsForAnalysis = events
            .filter { it.occurredAtMillis in windowStartMillis..analysisEndMillis }
            .sortedBy { it.occurredAtMillis }

        val inactiveRanges = buildInactiveRanges(
            events = eventsForAnalysis,
            windowStartMillis = windowStartMillis,
            windowEndMillis = windowEndMillis,
            analysisEndMillis = analysisEndMillis
        )

        val longestInactiveRange = inactiveRanges.maxByOrNull { it.endMillis - it.startMillis }
            ?: return null

        val durationMinutes = ((longestInactiveRange.endMillis - longestInactiveRange.startMillis) / 60_000L).toInt()
        if (durationMinutes < MIN_SLEEP_MINUTES) return null

        val eventsInSleepRange = eventsForAnalysis.filter {
            it.occurredAtMillis in longestInactiveRange.startMillis..longestInactiveRange.endMillis
        }
        val unlocks = eventsInSleepRange.count { it.type == SleepDeviceEventType.UserPresent }
        val screenOns = eventsInSleepRange.count { it.type == SleepDeviceEventType.ScreenOn }
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
        windowEndMillis: Long,
        analysisEndMillis: Long
    ): List<InactiveRange> {
        val ranges = mutableListOf<InactiveRange>()
        var screenOffStartedAt: Long? = null

        events.forEach { event ->
            when (event.type) {
                SleepDeviceEventType.ScreenOff -> {
                    if (screenOffStartedAt == null && event.occurredAtMillis <= windowEndMillis) {
                        screenOffStartedAt = max(event.occurredAtMillis, windowStartMillis)
                    }
                }

                SleepDeviceEventType.ScreenOn,
                SleepDeviceEventType.UserPresent -> {
                    val startedAt = screenOffStartedAt
                    if (
                        startedAt != null &&
                        event.occurredAtMillis <= windowEndMillis &&
                        event.occurredAtMillis > startedAt
                    ) {
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

        val openRangeStart = screenOffStartedAt ?: inferWindowStartIfNoRestWindowInteraction(
            events = events,
            windowStartMillis = windowStartMillis,
            windowEndMillis = windowEndMillis
        )
        val openRangeEnd = openRangeStart?.let {
            postWindowWakeMillis(
                events = events,
                afterMillis = windowEndMillis,
                fallbackEndMillis = analysisEndMillis
            )
        }
        if (openRangeStart != null && openRangeEnd != null && openRangeEnd > openRangeStart) {
            ranges.add(
                InactiveRange(
                    startMillis = openRangeStart,
                    endMillis = openRangeEnd
                )
            )
        }

        return ranges
    }

    private fun inferWindowStartIfNoRestWindowInteraction(
        events: List<SleepDeviceEvent>,
        windowStartMillis: Long,
        windowEndMillis: Long
    ): Long? {
        return if (events.none { it.occurredAtMillis <= windowEndMillis && it.isInteraction() }) {
            windowStartMillis
        } else {
            null
        }
    }

    private fun postWindowWakeMillis(
        events: List<SleepDeviceEvent>,
        afterMillis: Long,
        fallbackEndMillis: Long
    ): Long {
        val postWindowEvents = events.filter { it.occurredAtMillis > afterMillis }
        return postWindowEvents.firstOrNull { it.type == SleepDeviceEventType.UserPresent }?.occurredAtMillis
            ?: postWindowEvents.firstOrNull { it.type == SleepDeviceEventType.ScreenOn }?.occurredAtMillis
            ?: fallbackEndMillis
    }

    private fun SleepDeviceEvent.isInteraction(): Boolean {
        return type == SleepDeviceEventType.ScreenOn || type == SleepDeviceEventType.UserPresent
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
