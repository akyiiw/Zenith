package br.com.zenith.viewmodels.progress

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.Atividade
import br.com.zenith.data.models.SleepRecord
import br.com.zenith.data.models.PersonalGoal
import br.com.zenith.data.models.ProgressGoal
import br.com.zenith.data.repositories.GoalsRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val sleepRecordsAvailable: Boolean = true,
    val weeklyGoals: List<ProgressGoal> = emptyList(),
    val monthlyGoals: List<ProgressGoal> = emptyList()
)

class ProgressViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProgressUiState(isLoading = true))
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    private var activitiesCache: List<Atividade> = emptyList()
    private var sleepCache: List<SleepRecord> = emptyList()
    private var challengesCache: Int = 0
    private var achievementsCache: Int = 0
    private var streakCache: Int = 0
    private val goalsRepository = GoalsRepository()

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
                    sleepRecordsAvailable = sleepRecordsAvailable,
                    activeGoals = goalsRepository.getActiveGoals()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Erro ao carregar progresso"
                )
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
        sleepRecordsAvailable: Boolean,
        activeGoals: List<PersonalGoal> = emptyList()
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
            sleepRecordsAvailable = sleepRecordsAvailable,
            weeklyGoals = calculateProgressGoals(activeGoals, ProgressPeriod.Weekly),
            monthlyGoals = calculateProgressGoals(activeGoals, ProgressPeriod.Monthly)
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

    private fun calculateProgressGoals(goals: List<PersonalGoal>, period: ProgressPeriod): List<ProgressGoal> {
        val today = LocalDate.now()
        val startDate = when (period) {
            ProgressPeriod.Weekly -> today.minusDays(6)
            ProgressPeriod.Monthly -> today.minusDays(29)
        }

        return goals.filter { it.period == period.name.lowercase() }.map { goal ->
            val currentVal = when (goal.metric) {
                "distance_km" -> activitiesCache.filter { it.activityDate()?.isAfter(startDate) == true || it.activityDate() == startDate }.sumOf { it.distanceKm() }
                "steps" -> activitiesCache.filter { it.activityDate()?.isAfter(startDate) == true || it.activityDate() == startDate }.sumOf { it.stepsValue() }.toDouble()
                "active_minutes" -> activitiesCache.filter { it.activityDate()?.isAfter(startDate) == true || it.activityDate() == startDate }.sumOf { it.duracaoMin ?: 0 }.toDouble()
                "activities" -> activitiesCache.filter { it.activityDate()?.isAfter(startDate) == true || it.activityDate() == startDate }.size.toDouble()
                "sleep_hours" -> sleepCache.filter { it.sleepDate()?.isAfter(startDate) == true || it.sleepDate() == startDate }.sumOf { it.durationMinutes }.toDouble() / 60.0
                else -> 0.0
            }

            val progressPercent = (currentVal / goal.targetValue).toFloat().coerceIn(0f, 1f)

            ProgressGoal(
                title = goal.title,
                currentValue = currentVal,
                targetValue = goal.targetValue,
                unit = when (goal.metric) {
                    "distance_km" -> "km"
                    "steps" -> "passos"
                    "active_minutes" -> "min"
                    "activities" -> "atividades"
                    "sleep_hours" -> "horas"
                    else -> ""
                },
                progressPercent = progressPercent,
                isComplete = currentVal >= goal.targetValue
            )
        }
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
