package br.com.zenith.data.sleep

data class SleepMonitoringSettings(
    val enabled: Boolean = false,
    val restWindowStartMinutes: Int = 19 * 60,
    val restWindowEndMinutes: Int = 6 * 60,
    val useHealthConnect: Boolean = true,
    val useSleepApi: Boolean = true,
    val useDeviceEstimate: Boolean = true
) {
    val restWindowLabel: String
        get() = "${restWindowStartMinutes.asClockLabel()} - ${restWindowEndMinutes.asClockLabel()}"
}

enum class SleepDataSource(
    val title: String,
    val description: String
) {
    HealthConnect(
        title = "Health Connect",
        description = "Prioriza sessões de sono vindas de relógios, pulseiras e apps de saúde."
    ),
    SleepApi(
        title = "Sleep API",
        description = "Usa reconhecimento de atividade do Google Play Services quando disponível."
    ),
    DeviceEstimate(
        title = "Estimativa do aparelho",
        description = "Usa janela de descanso, tela ligada/desligada e desbloqueios como fallback."
    ),
    Manual(
        title = "Manual",
        description = "Permite corrigir ou registrar sono quando a detecção automática falhar."
    )
}

data class SleepDetectionPlan(
    val sources: List<SleepDataSource>,
    val interruptionPenaltyEnabled: Boolean
) {
    companion object {
        fun from(settings: SleepMonitoringSettings): SleepDetectionPlan {
            if (!settings.enabled) {
                return SleepDetectionPlan(
                    sources = listOf(SleepDataSource.Manual),
                    interruptionPenaltyEnabled = false
                )
            }

            val sources = buildList {
                if (settings.useHealthConnect) add(SleepDataSource.HealthConnect)
                if (settings.useSleepApi) add(SleepDataSource.SleepApi)
                if (settings.useDeviceEstimate) add(SleepDataSource.DeviceEstimate)
                add(SleepDataSource.Manual)
            }

            return SleepDetectionPlan(
                sources = sources,
                interruptionPenaltyEnabled = settings.useDeviceEstimate
            )
        }
    }
}

fun Int.asClockLabel(): String {
    val normalized = floorMod(this, 24 * 60)
    val hour = normalized / 60
    val minute = normalized % 60
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

private fun floorMod(value: Int, divisor: Int): Int {
    return ((value % divisor) + divisor) % divisor
}
