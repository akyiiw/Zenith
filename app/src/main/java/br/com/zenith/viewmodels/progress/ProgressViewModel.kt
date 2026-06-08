package br.com.zenith.viewmodels.progress

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.Atividade
import br.com.zenith.data.models.SleepRecord
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

enum class ProgressPeriod(val days: Int) {
    Weekly(7),
    Monthly(30)
}

data class ProgressDay(
    val date: LocalDate,
    val label: String,
    val sleepHours: Float,
    val steps: Int,
    val distanceKm: Double,
    val durationMin: Int,
    val activities: Int
)

data class ProgressSummary(
    val activities: Int = 0,
    val distanceKm: Double = 0.0,
    val steps: Int = 0,
    val durationMin: Int = 0,
    val averageSleepHours: Double = 0.0,
    val challenges: Int = 0,
    val achievements: Int = 0,
    val streak: Int = 0
)

data class ProgressUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isPremium: Boolean = false,
    val weeklyDays: List<ProgressDay> = emptyList(),
    val monthlyDays: List<ProgressDay> = emptyList(),
    val weeklySummary: ProgressSummary = ProgressSummary(),
    val monthlySummary: ProgressSummary = ProgressSummary(),
    val sleepRecordsAvailable: Boolean = true
)

class ProgressViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProgressUiState(isLoading = true))
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    private var activitiesCache: List<Atividade> = emptyList()
    private var sleepCache: List<SleepRecord> = emptyList()
    private var challengesCache: Int = 0
    private var achievementsCache: Int = 0
    private var streakCache: Int = 0

    fun load(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuário não autenticado")

                val profile = client.postgrest.from("profiles")
                    .select(columns = Columns.raw("\"isPremium\", streak")) {
                        filter { eq("id", userId) }
                    }
                    .decodeSingle<ProgressProfile>()

                activitiesCache = client.postgrest.from("atividades")
                    .select(columns = Columns.raw("*, exercicios(*)")) {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<Atividade>()

                var sleepRecordsAvailable = true
                sleepCache = try {
                    client.postgrest.from("sleep_records")
                        .select {
                            filter { eq("user_id", userId) }
                        }
                        .decodeList<SleepRecord>()
                } catch (_: Exception) {
                    sleepRecordsAvailable = false
                    emptyList()
                }

                challengesCache = countRows("desafio_participacoes", userId)
                achievementsCache = countRows("conquistas", userId)
                streakCache = profile.streak

                _uiState.value = buildState(
                    isPremium = profile.isPremium,
                    sleepRecordsAvailable = sleepRecordsAvailable
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Erro ao carregar progresso"
                )
            }
        }
    }

    fun addManualSleep(
        hours: Float,
        quality: Int?,
        context: Context
    ) {
        viewModelScope.launch {
            try {
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuário não autenticado")
                val durationMinutes = (hours.coerceIn(0.5f, 16f) * 60).roundToInt()
                val zone = ZoneId.systemDefault()
                val endedAt = LocalDate.now().atTime(7, 0)
                val startedAt = endedAt.minusMinutes(durationMinutes.toLong())

                client.postgrest.from("sleep_records").insert(
                    buildJsonObject {
                        put("user_id", userId)
                        put("started_at", startedAt.atZone(zone).toOffsetDateTime().toString())
                        put("ended_at", endedAt.atZone(zone).toOffsetDateTime().toString())
                        put("duration_minutes", durationMinutes)
                        put("source", "manual")
                        quality?.let { put("quality", it) }
                    }
                )

                Toast.makeText(context, "Sono registrado", Toast.LENGTH_SHORT).show()
                load(context)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Erro ao registrar sono: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun countRows(table: String, userId: String): Int {
        return SupabaseConfig.getClient().postgrest.from(table)
            .select {
                filter { eq("user_id", userId) }
                count(Count.EXACT)
            }
            .countOrNull()
            ?.toInt() ?: 0
    }

    private fun buildState(
        isPremium: Boolean,
        sleepRecordsAvailable: Boolean
    ): ProgressUiState {
        val weeklyDays = buildDays(ProgressPeriod.Weekly)
        val monthlyDays = buildDays(ProgressPeriod.Monthly)

        return ProgressUiState(
            isLoading = false,
            isPremium = isPremium,
            weeklyDays = weeklyDays,
            monthlyDays = monthlyDays,
            weeklySummary = summarize(weeklyDays),
            monthlySummary = summarize(monthlyDays),
            sleepRecordsAvailable = sleepRecordsAvailable
        )
    }

    private fun buildDays(period: ProgressPeriod): List<ProgressDay> {
        val today = LocalDate.now()
        val dates = (period.days - 1 downTo 0).map { today.minusDays(it.toLong()) }

        return dates.map { date ->
            val dayActivities = activitiesCache.filter { it.activityDate() == date }
            val daySleep = sleepCache.filter { it.sleepDate() == date }
            ProgressDay(
                date = date,
                label = "${date.dayOfMonth}/${date.monthValue}",
                sleepHours = daySleep.sumOf { it.durationMinutes }.toFloat() / 60f,
                steps = dayActivities.sumOf { it.stepsValue() },
                distanceKm = dayActivities.sumOf { it.distanceKm() },
                durationMin = dayActivities.sumOf { it.duracaoMin ?: 0 },
                activities = dayActivities.size
            )
        }
    }

    private fun summarize(days: List<ProgressDay>): ProgressSummary {
        val daysWithSleep = days.filter { it.sleepHours > 0f }
        return ProgressSummary(
            activities = days.sumOf { it.activities },
            distanceKm = days.sumOf { it.distanceKm },
            steps = days.sumOf { it.steps },
            durationMin = days.sumOf { it.durationMin },
            averageSleepHours = if (daysWithSleep.isEmpty()) 0.0 else daysWithSleep.sumOf { it.sleepHours.toDouble() } / daysWithSleep.size,
            challenges = challengesCache,
            achievements = achievementsCache,
            streak = streakCache
        )
    }

    private fun Atividade.activityDate(): LocalDate? {
        return parseDate(realizadaEm ?: criadaEm)
    }

    private fun SleepRecord.sleepDate(): LocalDate? {
        return parseDate(endedAt)
    }

    private fun parseDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return runCatching { OffsetDateTime.parse(value).toLocalDate() }
            .getOrElse {
                runCatching { LocalDateTime.parse(value).toLocalDate() }.getOrNull()
            }
    }

    private fun Atividade.stepsValue(): Int {
        passos?.let { return it }
        val unit = exercicio?.unidade.orEmpty().lowercase()
        return if ("pass" in unit) valor.roundToInt() else 0
    }

    private fun Atividade.distanceKm(): Double {
        distanciaBruta?.let { return if (it > 100) it / 1000.0 else it }
        val unit = exercicio?.unidade.orEmpty().lowercase()
        return when {
            "km" in unit || "quil" in unit -> valor
            "metro" in unit || unit == "m" -> valor / 1000.0
            else -> 0.0
        }
    }

    @Serializable
    private data class ProgressProfile(
        @SerialName("isPremium") val isPremium: Boolean = false,
        val streak: Int = 0
    )
}
