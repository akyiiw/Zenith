package br.com.zenith.viewmodels.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.ActivityMention
import br.com.zenith.data.models.Amizade
import br.com.zenith.data.models.Atividade
import br.com.zenith.data.models.Desafio
import br.com.zenith.data.models.DesafioParticipacao
import br.com.zenith.data.models.Profile
import br.com.zenith.ui.notifications.ZenithNotifier
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Count
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class HomeNotificationType {
    FriendRequest,
    Mention,
    Ranking,
    Achievement
}

data class HomeNotificationItem(
    val id: String,
    val type: HomeNotificationType,
    val title: String,
    val body: String,
    val actionLabel: String? = null,
    val targetRoute: String? = null
)

data class HomeNotificationsUiState(
    val isLoading: Boolean = false,
    val notifications: List<HomeNotificationItem> = emptyList()
) {
    val unreadCount: Int get() = notifications.size
}

class HomeNotificationsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeNotificationsUiState())
    val uiState: StateFlow<HomeNotificationsUiState> = _uiState.asStateFlow()

    fun load(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuário não autenticado")

                val profiles = client.postgrest.from("profiles")
                    .select()
                    .decodeList<Profile>()

                val friendships = client.postgrest.from("amizades")
                    .select()
                    .decodeList<Amizade>()

                val friendRequests = buildFriendRequests(
                    userId = userId,
                    profiles = profiles,
                    friendships = friendships
                )
                val mentionRequests = buildMentionRequests(userId = userId)

                val rankingNotifications = buildRankingNotifications(userId = userId)
                val achievementsNotification = buildAchievementsNotification(userId = userId)

                _uiState.value = HomeNotificationsUiState(
                    notifications = friendRequests + mentionRequests + rankingNotifications + listOfNotNull(achievementsNotification)
                )
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao carregar notificações: ${e.localizedMessage}")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun buildMentionRequests(userId: String): List<HomeNotificationItem> {
        val mentions = SupabaseConfig.getClient().postgrest.from("activity_mentions")
            .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, atividades(*, exercicios(*)), publisher:profiles!activity_mentions_publisher_id_fkey(*)")) {
                filter {
                    eq("mentioned_user_id", userId)
                    eq("status", ActivityMention.STATUS_PENDING)
                }
            }
            .decodeList<ActivityMention>()

        return mentions.map { mention ->
            val publisherName = mention.publisher?.displayName?.takeIf { it.isNotBlank() }
                ?: mention.publisher?.name
                ?: "Um amigo"
            val activityName = mention.activity?.titulo?.takeIf { it.isNotBlank() }
                ?: mention.activity?.exercicio?.nome
                ?: "uma atividade"
            HomeNotificationItem(
                id = "mention-${mention.id}",
                type = HomeNotificationType.Mention,
                title = "Menção em atividade",
                body = "$publisherName marcou você em $activityName.",
                actionLabel = "Responder",
                targetRoute = "social"
            )
        }
    }

    private fun buildFriendRequests(
        userId: String,
        profiles: List<Profile>,
        friendships: List<Amizade>
    ): List<HomeNotificationItem> {
        return friendships
            .filter { it.friendId == userId && it.status == "pendente" }
            .map { request ->
                val profile = profiles.firstOrNull { it.id == request.userId }
                val name = profile?.displayName?.takeIf { it.isNotBlank() }
                    ?: profile?.name
                    ?: "Alguém"
                HomeNotificationItem(
                    id = "friend-${request.userId}",
                    type = HomeNotificationType.FriendRequest,
                    title = "Solicitação de amizade",
                    body = "$name quer se conectar com você.",
                    actionLabel = "Ver social",
                    targetRoute = "social"
                )
            }
    }

    private suspend fun buildRankingNotifications(userId: String): List<HomeNotificationItem> {
        val client = SupabaseConfig.getClient()
        val now = OffsetDateTime.now()
        val challenges = client.postgrest.from("desafios")
            .select()
            .decodeList<Desafio>()
        val participations = client.postgrest.from("desafio_participacoes")
            .select()
            .decodeList<DesafioParticipacao>()
        val activities = client.postgrest.from("atividades")
            .select()
            .decodeList<Atividade>()

        val challengeIds = participations
            .filter { it.userId == userId }
            .map { it.desafioId }
            .toSet()

        return challenges
            .filter { it.id in challengeIds }
            .filter { challenge ->
                challenge.fimEm
                    ?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
                    ?.isBefore(now) == true
            }
            .mapNotNull { challenge ->
                val entries = rankingEntries(challenge, activities)
                val position = entries.indexOfFirst { it.userId == userId }.takeIf { it >= 0 }?.plus(1)
                    ?: return@mapNotNull null
                HomeNotificationItem(
                    id = "ranking-${challenge.id}",
                    type = HomeNotificationType.Ranking,
                    title = "Ranking final publicado",
                    body = "Você ficou em ${position}º no desafio ${challenge.titulo}.",
                    actionLabel = "Ver desafio",
                    targetRoute = "challenge"
                )
            }
            .take(3)
    }

    private suspend fun buildAchievementsNotification(userId: String): HomeNotificationItem? {
        val count = SupabaseConfig.getClient().postgrest.from("conquistas")
            .select {
                filter { eq("user_id", userId) }
                count(Count.EXACT)
            }
            .countOrNull()
            ?.toInt() ?: 0

        if (count <= 0) return null
        return HomeNotificationItem(
            id = "achievements-summary",
            type = HomeNotificationType.Achievement,
            title = "Conquistas",
            body = "Você já desbloqueou $count conquista${if (count == 1) "" else "s"}.",
            actionLabel = "Ver perfil",
            targetRoute = "profile"
        )
    }

    private fun rankingEntries(
        challenge: Desafio,
        activities: List<Atividade>
    ): List<RankingEntry> {
        return activities
            .filter { it.desafioId == challenge.id }
            .filter { it.gpsQualidade != "ruim" }
            .filter { it.verificada || challenge.aceitaRegistroManual }
            .groupBy { it.userId }
            .mapNotNull { (userId, userActivities) ->
                val score = rankingScore(challenge, userActivities) ?: return@mapNotNull null
                RankingEntry(userId = userId, score = score)
            }
            .sortedWith(
                if (challenge.rankingTipo == "menor_tempo" || challenge.rankingTipo == "menor_pace") {
                    compareBy<RankingEntry> { it.score }
                } else {
                    compareByDescending { it.score }
                }
            )
    }

    private fun rankingScore(challenge: Desafio, activities: List<Atividade>): Double? {
        return when (challenge.rankingTipo) {
            "menor_tempo" -> activities.mapNotNull { it.duracaoMin?.toDouble() }.minOrNull()
            "menor_pace" -> activities.mapNotNull { activity ->
                val duration = activity.duracaoMin ?: return@mapNotNull null
                duration / activity.valor.coerceAtLeast(0.01)
            }.minOrNull()
            "maior_distancia" -> activities.maxOfOrNull { it.valor }
            "tempo_total" -> activities.sumOf { it.duracaoMin ?: 0 }.toDouble().takeIf { it > 0.0 }
            else -> activities.sumOf { it.valor }.takeIf { it > 0.0 }
        }
    }

    private data class RankingEntry(
        val userId: String,
        val score: Double
    )
}
