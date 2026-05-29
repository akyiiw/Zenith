package br.com.zenith.viewmodels.sleep

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import br.com.zenith.data.sleep.SleepDetectionPlan
import br.com.zenith.data.sleep.SleepMonitoringSettings
import br.com.zenith.data.sleep.SleepSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SleepSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SleepSettingsRepository(application)

    private val _settings = MutableStateFlow(repository.load())
    val settings: StateFlow<SleepMonitoringSettings> = _settings

    private val _detectionPlan = MutableStateFlow(SleepDetectionPlan.from(_settings.value))
    val detectionPlan: StateFlow<SleepDetectionPlan> = _detectionPlan

    fun updateSettings(transform: (SleepMonitoringSettings) -> SleepMonitoringSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        _detectionPlan.value = SleepDetectionPlan.from(updated)
        repository.save(updated)
    }
}
