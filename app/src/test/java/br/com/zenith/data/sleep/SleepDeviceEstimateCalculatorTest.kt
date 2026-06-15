package br.com.zenith.data.sleep

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SleepDeviceEstimateCalculatorTest {
    private val zoneId: ZoneId = ZoneId.of("America/Sao_Paulo")
    private val date: LocalDate = LocalDate.of(2026, 6, 14)
    private val settings = SleepMonitoringSettings(
        enabled = true,
        restWindowStartMinutes = 22 * 60,
        restWindowEndMinutes = 6 * 60,
        useDeviceEstimate = true
    )

    @Test
    fun `ends inside rest window when interaction happens before configured end`() {
        val estimate = SleepDeviceEstimateCalculator.estimateForDate(
            settings = settings,
            events = listOf(
                event(date.atTime(22, 30), SleepDeviceEventType.ScreenOff),
                event(date.plusDays(1).atTime(5, 30), SleepDeviceEventType.UserPresent)
            ),
            date = date,
            zoneId = zoneId
        )

        assertEquals(millis(date.atTime(22, 30)), estimate?.startedAtMillis)
        assertEquals(millis(date.plusDays(1).atTime(5, 30)), estimate?.endedAtMillis)
        assertEquals(7 * 60, estimate?.durationMinutes)
    }

    @Test
    fun `continues past rest window until first user present event`() {
        val estimate = SleepDeviceEstimateCalculator.estimateForDate(
            settings = settings,
            events = listOf(
                event(date.atTime(22, 0), SleepDeviceEventType.ScreenOff),
                event(date.plusDays(1).atTime(7, 0), SleepDeviceEventType.ScreenOn),
                event(date.plusDays(1).atTime(8, 15), SleepDeviceEventType.UserPresent)
            ),
            date = date,
            zoneId = zoneId
        )

        assertEquals(millis(date.atTime(22, 0)), estimate?.startedAtMillis)
        assertEquals(millis(date.plusDays(1).atTime(8, 15)), estimate?.endedAtMillis)
        assertEquals(10 * 60 + 15, estimate?.durationMinutes)
        assertEquals(1, estimate?.interruptions)
    }

    @Test
    fun `uses first post-window screen on when there is no user present event`() {
        val estimate = SleepDeviceEstimateCalculator.estimateForDate(
            settings = settings,
            events = listOf(
                event(date.atTime(22, 10), SleepDeviceEventType.ScreenOff),
                event(date.plusDays(1).atTime(7, 5), SleepDeviceEventType.ScreenOn)
            ),
            date = date,
            zoneId = zoneId
        )

        assertEquals(millis(date.plusDays(1).atTime(7, 5)), estimate?.endedAtMillis)
        assertEquals(1, estimate?.interruptions)
    }

    @Test
    fun `caps open sleep estimate at maximum duration when there is no wake interaction`() {
        val estimate = SleepDeviceEstimateCalculator.estimateForDate(
            settings = settings,
            events = listOf(
                event(date.atTime(22, 0), SleepDeviceEventType.ScreenOff)
            ),
            date = date,
            zoneId = zoneId,
            maxSleepMinutes = 14 * 60
        )

        assertEquals(millis(date.atTime(22, 0)), estimate?.startedAtMillis)
        assertEquals(millis(date.plusDays(1).atTime(12, 0)), estimate?.endedAtMillis)
        assertEquals(14 * 60, estimate?.durationMinutes)
    }

    @Test
    fun `infers sleep from window start when there are no rest window interactions`() {
        val estimate = SleepDeviceEstimateCalculator.estimateForDate(
            settings = settings,
            events = listOf(
                event(date.plusDays(1).atTime(8, 0), SleepDeviceEventType.UserPresent)
            ),
            date = date,
            zoneId = zoneId
        )

        assertEquals(millis(date.atTime(22, 0)), estimate?.startedAtMillis)
        assertEquals(millis(date.plusDays(1).atTime(8, 0)), estimate?.endedAtMillis)
        assertEquals(10 * 60, estimate?.durationMinutes)
    }

    @Test
    fun `returns null when monitoring is disabled`() {
        val estimate = SleepDeviceEstimateCalculator.estimateForDate(
            settings = settings.copy(enabled = false),
            events = listOf(event(date.atTime(22, 0), SleepDeviceEventType.ScreenOff)),
            date = date,
            zoneId = zoneId
        )

        assertNull(estimate)
    }

    @Test
    fun `returns null when device estimate is disabled`() {
        val estimate = SleepDeviceEstimateCalculator.estimateForDate(
            settings = settings.copy(useDeviceEstimate = false),
            events = listOf(event(date.atTime(22, 0), SleepDeviceEventType.ScreenOff)),
            date = date,
            zoneId = zoneId
        )

        assertNull(estimate)
    }

    private fun event(
        at: LocalDateTime,
        type: SleepDeviceEventType
    ): SleepDeviceEvent {
        return SleepDeviceEvent(
            occurredAtMillis = millis(at),
            type = type
        )
    }

    private fun millis(at: LocalDateTime): Long {
        return at.atZone(zoneId).toInstant().toEpochMilli()
    }
}
