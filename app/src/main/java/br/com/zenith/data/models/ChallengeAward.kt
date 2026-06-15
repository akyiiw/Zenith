package br.com.zenith.data.models

import java.time.OffsetDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ChallengeMedalType {
    GOLD,
    SILVER,
    BRONZE,
    PARTICIPATION
}

data class ChallengeAward(
    val challengeId: String,
    val challengeTitle: String,
    val medalType: ChallengeMedalType,
    val position: Int?,
    val finishedAt: String?,
    val score: Double,
    val unit: String
)

data class ChallengeAwardSummary(
    val gold: Int = 0,
    val silver: Int = 0,
    val bronze: Int = 0,
    val participation: Int = 0,
    val history: List<ChallengeAward> = emptyList()
) {
    val total: Int
        get() = gold + silver + bronze + participation
}

object ChallengeAwardCalculator {
    fun buildSummary(
        userId: String,
        challenges: List<Desafio>,
        participations: List<DesafioParticipacao>,
        activities: List<Atividade>,
        now: OffsetDateTime = OffsetDateTime.now()
    ): ChallengeAwardSummary {
        if (userId.isBlank()) return ChallengeAwardSummary()

        val userChallengeIds = participations
            .filter { it.userId == userId }
            .map { it.desafioId }
            .toSet()
        if (userChallengeIds.isEmpty()) return ChallengeAwardSummary()

        val awards = challenges
            .filter { it.id in userChallengeIds }
            .filter { it.isFinished(now) }
            .map { challenge ->
                val ranking = buildRanking(challenge, participations, activities)
                val position = ranking.indexOfFirst { it.userId == userId }
                    .takeIf { it >= 0 }
                    ?.plus(1)
                val entry = ranking.firstOrNull { it.userId == userId }
                ChallengeAward(
                    challengeId = challenge.id,
                    challengeTitle = challenge.titulo.ifBlank { "Desafio" },
                    medalType = medalTypeForPosition(position),
                    position = position,
                    finishedAt = challenge.fimEm,
                    score = entry?.score ?: 0.0,
                    unit = awardUnit(challenge)
                )
            }
            .sortedByDescending { parseDate(it.finishedAt)?.toInstant()?.toEpochMilli() ?: Long.MIN_VALUE }

        return ChallengeAwardSummary(
            gold = awards.count { it.medalType == ChallengeMedalType.GOLD },
            silver = awards.count { it.medalType == ChallengeMedalType.SILVER },
            bronze = awards.count { it.medalType == ChallengeMedalType.BRONZE },
            participation = awards.count { it.medalType == ChallengeMedalType.PARTICIPATION },
            history = awards
        )
    }

    fun Desafio.isFinished(now: OffsetDateTime = OffsetDateTime.now()): Boolean {
        val end = parseDate(fimEm) ?: return false
        return !end.isAfter(now)
    }

    fun formatFinishedDate(value: String?): String {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        return parseDate(value)?.format(formatter).orEmpty()
    }

    private fun buildRanking(
        challenge: Desafio,
        participations: List<DesafioParticipacao>,
        activities: List<Atividade>
    ): List<RankingEntry> {
        val participantIds = participations
            .filter { it.desafioId == challenge.id }
            .map { it.userId }
            .toSet()

        val activitiesByUser = activities
            .asSequence()
            .filter { it.desafioId == challenge.id }
            .filter { participantIds.isEmpty() || it.userId in participantIds }
            .filter { isCompatibleExercise(it, challenge) }
            .filter { isWithinChallengePeriod(it, challenge) }
            .filter { canUseActivityForChallenge(it, challenge) }
            .groupBy { it.userId }

        val entries = (participantIds + activitiesByUser.keys).mapNotNull { userId ->
            val userActivities = activitiesByUser[userId].orEmpty()
            val selectedActivities = rankedActivitiesForChallenge(userActivities, challenge)
            val relevantActivities = selectedActivities.ifEmpty { userActivities }
            val score = when (challenge.rankingTipo) {
                "menor_tempo", "menor_pace" -> selectedActivities.firstOrNull()?.duracaoMin?.toDouble() ?: 0.0
                "maior_distancia" -> selectedActivities.firstOrNull()?.let { activityMetric(it, challenge) } ?: 0.0
                "tempo_total" -> userActivities.sumOf { it.duracaoMin ?: 0 }.toDouble()
                else -> userActivities.sumOf { activityMetric(it, challenge) }
            }

            if (challenge.modoMeta != "livre" && score <= 0.0) return@mapNotNull null

            RankingEntry(
                userId = userId,
                score = score,
                minutes = relevantActivities.sumOf { it.duracaoMin ?: 0 },
                verifiedActivities = relevantActivities.count { it.verificada }
            )
        }

        return entries.sortedWith(entryComparator(challenge))
    }

    private fun medalTypeForPosition(position: Int?): ChallengeMedalType {
        return when (position) {
            1 -> ChallengeMedalType.GOLD
            2 -> ChallengeMedalType.SILVER
            3 -> ChallengeMedalType.BRONZE
            else -> ChallengeMedalType.PARTICIPATION
        }
    }

    private fun entryComparator(challenge: Desafio): Comparator<RankingEntry> {
        return when (challenge.rankingTipo) {
            "menor_tempo", "menor_pace" ->
                compareBy<RankingEntry> { if (it.minutes > 0) it.minutes else Int.MAX_VALUE }
                    .thenByDescending { it.verifiedActivities }
                    .thenBy { it.userId }
            else ->
                compareByDescending<RankingEntry> { it.score }
                    .thenByDescending { it.verifiedActivities }
                    .thenBy { it.userId }
        }
    }

    private fun rankedActivitiesForChallenge(
        activities: List<Atividade>,
        challenge: Desafio
    ): List<Atividade> {
        if (challenge.modoMeta == "livre") return activities
        return when (challenge.rankingTipo) {
            "maior_distancia" -> activities
                .filter { (it.duracaoMin ?: 0).toDouble() <= (challenge.objetivoValor ?: challenge.meta) && (it.duracaoMin ?: 0) > 0 }
                .sortedWith(
                    compareByDescending<Atividade> { activityMetric(it, challenge) }
                        .thenBy { it.duracaoMin ?: Int.MAX_VALUE }
                        .thenByDescending { it.verificada }
                )
                .take(1)
            else -> activities
                .filter { activityMetric(it, challenge) >= (challenge.objetivoValor ?: challenge.meta) }
                .sortedWith(
                    compareBy<Atividade> { rankingDurationOrPace(it, challenge) }
                        .thenByDescending { it.verificada }
                        .thenByDescending { activityMetric(it, challenge) }
                )
                .take(1)
        }
    }

    private fun isWithinChallengePeriod(activity: Atividade, challenge: Desafio): Boolean {
        val completedAt = parseDate(activity.realizadaEm) ?: return true
        val startDate = parseDate(challenge.inicioEm)
        val endDate = parseDate(challenge.fimEm)
        return (startDate == null || !completedAt.isBefore(startDate)) &&
            (endDate == null || !completedAt.isAfter(endDate))
    }

    private fun canUseActivityForChallenge(activity: Atividade, challenge: Desafio): Boolean {
        if (activity.desafioId != challenge.id) return false
        if (activity.gpsQualidade == "ruim") return false
        return activity.verificada || challenge.aceitaRegistroManual
    }

    private fun isCompatibleExercise(activity: Atividade, challenge: Desafio): Boolean {
        val exercise = activity.exercicio ?: return true
        val text = "${exercise.slug} ${exercise.nome} ${exercise.grupo.orEmpty()}".lowercase(Locale.ROOT)
        return when (challenge.atividadeDesignada) {
            "corrida" -> text.contains("corrida") || text.contains("correr") || text.contains("run")
            "ciclismo" -> text.contains("ciclismo") || text.contains("bicicleta") || text.contains("bike") || text.contains("cycling")
            else -> text.contains("caminhada") || text.contains("caminhar") || text.contains("walk")
        }
    }

    private fun activityMetric(activity: Atividade, challenge: Desafio): Double {
        return when (challenge.metrica) {
            "passos" -> activity.passos?.toDouble() ?: if (challenge.unidade.contains("pass", true)) activity.valor else 0.0
            "tempo" -> (activity.duracaoMin ?: 0).toDouble()
            else -> activity.valor
        }
    }

    private fun rankingDurationOrPace(activity: Atividade, challenge: Desafio): Double {
        val minutes = activity.duracaoMin ?: Int.MAX_VALUE
        if (challenge.rankingTipo != "menor_pace") return minutes.toDouble()
        return minutes / activity.valor.coerceAtLeast(0.01)
    }

    private fun awardUnit(challenge: Desafio): String {
        return when (challenge.rankingTipo) {
            "menor_tempo" -> "min"
            "menor_pace" -> "min/km"
            "maior_distancia", "distancia_total" -> "km"
            "tempo_total" -> "min"
            else -> challenge.unidade
        }
    }

    private fun parseDate(value: String?): OffsetDateTime? {
        val raw = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        runCatching { return OffsetDateTime.parse(raw) }
        val normalized = raw.replace(" ", "T")
        runCatching { return OffsetDateTime.parse(normalized) }
        runCatching {
            return LocalDateTime.parse(normalized)
                .atOffset(ZoneOffset.UTC)
        }
        runCatching {
            return LocalDate.parse(raw)
                .atTime(LocalTime.MAX)
                .atOffset(ZoneOffset.UTC)
        }
        return null
    }

    private data class RankingEntry(
        val userId: String,
        val score: Double,
        val minutes: Int,
        val verifiedActivities: Int
    )
}
