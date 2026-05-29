package br.com.zenith.data.sleep

import android.content.Context

class SleepSettingsRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "zenith_sleep_settings",
        Context.MODE_PRIVATE
    )

    fun load(): SleepMonitoringSettings {
        return SleepMonitoringSettings(
            enabled = preferences.getBoolean(KEY_ENABLED, false),
            restWindowStartMinutes = preferences.getInt(KEY_START_MINUTES, 19 * 60),
            restWindowEndMinutes = preferences.getInt(KEY_END_MINUTES, 6 * 60),
            useHealthConnect = preferences.getBoolean(KEY_HEALTH_CONNECT, true),
            useSleepApi = preferences.getBoolean(KEY_SLEEP_API, true),
            useDeviceEstimate = preferences.getBoolean(KEY_DEVICE_ESTIMATE, true)
        )
    }

    fun save(settings: SleepMonitoringSettings) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putInt(KEY_START_MINUTES, settings.restWindowStartMinutes)
            .putInt(KEY_END_MINUTES, settings.restWindowEndMinutes)
            .putBoolean(KEY_HEALTH_CONNECT, settings.useHealthConnect)
            .putBoolean(KEY_SLEEP_API, settings.useSleepApi)
            .putBoolean(KEY_DEVICE_ESTIMATE, settings.useDeviceEstimate)
            .apply()
    }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_START_MINUTES = "rest_window_start_minutes"
        const val KEY_END_MINUTES = "rest_window_end_minutes"
        const val KEY_HEALTH_CONNECT = "use_health_connect"
        const val KEY_SLEEP_API = "use_sleep_api"
        const val KEY_DEVICE_ESTIMATE = "use_device_estimate"
    }
}
