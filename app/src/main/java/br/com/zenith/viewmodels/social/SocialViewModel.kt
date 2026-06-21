package br.com.zenith.viewmodels.social

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.ActivityMention
import br.com.zenith.data.models.Amizade
import br.com.zenith.data.models.AmizadeInsert
import br.com.zenith.data.models.ActivityGroup
import br.com.zenith.data.models.ActivityGroupItem
import br.com.zenith.data.models.Atividade
import br.com.zenith.data.models.Badge
import br.com.zenith.data.models.ChallengeAwardCalculator
import br.com.zenith.data.models.ChallengeAwardSummary
import br.com.zenith.data.models.ChallengeForumEntry
import br.com.zenith.data.models.Desafio
import br.com.zenith.data.models.DesafioParticipacao
import br.com.zenith.data.models.Profile
import br.com.zenith.ui.notifications.ZenithNotifier
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class SocialUiState(
    val currentUserId: String = "",
    val profiles: List<Profile> = emptyList(),
    val friendships: List<Amizade> = emptyList(),
    val activityMentions: List<ActivityMention> = emptyList()
) {
    fun friendshipWith(profileId: String): Amizade? {
        return friendships.firstOrNull {
            (it.userId == currentUserId && it.friendId == profileId) ||
                (it.friendId == currentUserId && it.userId == profileId)
        }
    }

    fun friends(): List<Profile> {
        val friendIds = friendships
            .filter { it.status == STATUS_ACEITO && (it.userId == currentUserId || it.friendId == currentUserId) }
            .map { if (it.userId == currentUserId) it.friendId else it.userId }
            .toSet()
        return profiles.filter { it.id in friendIds }
    }

    fun receivedRequests(): List<Profile> {
        val ids = friendships
            .filter { it.friendId == currentUserId && it.status == STATUS_PENDENTE }
            .map { it.userId }
            .toSet()
        return profiles.filter { it.id in ids }
    }

    fun sentRequests(): List<Profile> {
        val ids = friendships
            .filter { it.userId == currentUserId && it.status == STATUS_PENDENTE }
            .map { it.friendId }
            .toSet()
        return profiles.filter { it.id in ids }
    }

    fun pendingMentions(): List<ActivityMention> =
        activityMentions.filter { it.status == ActivityMention.STATUS_PENDING }

    fun acceptedMentions(): List<ActivityMention> =
        activityMentions.filter { it.status == ActivityMention.STATUS_ACCEPTED }

    fun declinedMentions(): List<ActivityMention> =
        activityMentions.filter { it.status == ActivityMention.STATUS_DECLINED }

    companion object {
        const val STATUS_PENDENTE = "pendente"
        const val STATUS_ACEITO = "aceito"
    }
}

class SocialViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun fetchSocial(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                loadSocial(context)
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao carregar social: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun enviarConvite(profileId: String, context: Context) {
        viewModelScope.launch {
            runSaving(context) {
                val currentUserId = requireCurrentUserId()
                val existing = _uiState.value.friendshipWith(profileId)

                if (existing?.friendId == currentUserId && existing.status == SocialUiState.STATUS_PENDENTE) {
                    aceitarConviteInterno(currentUserId, profileId)
                } else if (existing == null) {
                    SupabaseConfig.getClient().postgrest.from("amizades").insert(
                        AmizadeInsert(
                            userId = currentUserId,
                            friendId = profileId,
                            status = SocialUiState.STATUS_PENDENTE
                        )
                    )
                }

                loadSocial(context)
            }
        }
    }

    fun aceitarConvite(profileId: String, context: Context) {
        viewModelScope.launch {
            runSaving(context) {
                aceitarConviteInterno(requireCurrentUserId(), profileId)
                loadSocial(context)
            }
        }
    }

    fun recusarConvite(profileId: String, context: Context) {
        viewModelScope.launch {
            runSaving(context) {
                val currentUserId = requireCurrentUserId()
                SupabaseConfig.getClient().postgrest.from("amizades").delete {
                    filter {
                        eq("user_id", profileId)
                        eq("amigo_id", currentUserId)
                        eq("status", SocialUiState.STATUS_PENDENTE)
                    }
                }
                loadSocial(context)
            }
        }
    }

    fun removerAmizade(profileId: String, context: Context) {
        viewModelScope.launch {
            runSaving(context) {
                val currentUserId = requireCurrentUserId()
                val client = SupabaseConfig.getClient()

                client.postgrest.from("amizades").delete {
                    filter {
                        eq("user_id", currentUserId)
                        eq("amigo_id", profileId)
                    }
                }
                client.postgrest.from("amizades").delete {
                    filter {
                        eq("user_id", profileId)
                        eq("amigo_id", currentUserId)
                    }
                }

                loadSocial(context)
            }
        }
    }

    fun aceitarMencao(mentionId: String, showOnMyProfile: Boolean, context: Context) {
        viewModelScope.launch {
            runSaving(context) {
                SupabaseConfig.getClient().postgrest.from("activity_mentions").update(
                    buildJsonObject {
                        put("status", ActivityMention.STATUS_ACCEPTED)
                        put("show_on_mentioned_profile", showOnMyProfile)
                        put("responded_at", java.time.OffsetDateTime.now().toString())
                    }
                ) {
                    filter { eq("id", mentionId) }
                }
                ZenithNotifier.success("Menção aceita.")
                loadSocial(context)
            }
        }
    }

    fun recusarMencao(mentionId: String, context: Context) {
        viewModelScope.launch {
            runSaving(context) {
                SupabaseConfig.getClient().postgrest.from("activity_mentions").update(
                    buildJsonObject {
                        put("status", ActivityMention.STATUS_DECLINED)
                        put("responded_at", java.time.OffsetDateTime.now().toString())
                    }
                ) {
                    filter { eq("id", mentionId) }
                }
                ZenithNotifier.success("Menção recusada.")
                loadSocial(context)
            }
        }
    }

    private suspend fun loadSocial(context: Context) {
        SupabaseConfig.init(context)
        val client = SupabaseConfig.getClient()
        val currentUserId = client.auth.currentUserOrNull()?.id
            ?: throw Exception("Usuário não autenticado")

        val profiles = client.postgrest.from("profiles")
            .select()
            .decodeList<Profile>()
            .filter { it.id != currentUserId }

        val friendships = client.postgrest.from("amizades")
            .select()
            .decodeList<Amizade>()
            .filter { it.userId == currentUserId || it.friendId == currentUserId }

        val activityMentions = runCatching {
            client.postgrest.from("activity_mentions")
                .select(columns = Columns.raw("*, atividades(*, exercicios(*)), publisher:profiles!activity_mentions_publisher_id_fkey(*)")) {
                    filter {
                        eq("mentioned_user_id", currentUserId)
                    }
                }
                .decodeList<ActivityMention>()
        }.getOrDefault(emptyList())
            .sortedByDescending { it.createdAt.orEmpty() }

        _uiState.value = SocialUiState(
            currentUserId = currentUserId,
            profiles = profiles,
            friendships = friendships,
            activityMentions = activityMentions
        )
    }

    private suspend fun aceitarConviteInterno(currentUserId: String, profileId: String) {
        val client = SupabaseConfig.getClient()
        val acceptedAt = OffsetDateTime.now().toString()

        client.postgrest.from("amizades").update(
            buildJsonObject {
                put("status", SocialUiState.STATUS_ACEITO)
                put("accepted_at", acceptedAt)
            }
        ) {
            filter {
                eq("user_id", profileId)
                eq("amigo_id", currentUserId)
            }
        }

        val reciprocalExists = _uiState.value.friendships.any {
            it.userId == currentUserId && it.friendId == profileId
        }
        if (!reciprocalExists) {
            client.postgrest.from("amizades").insert(
                AmizadeInsert(
                    userId = currentUserId,
                    friendId = profileId,
                    status = SocialUiState.STATUS_ACEITO,
                    acceptedAt = acceptedAt
                )
            )
        }
    }

    private fun requireCurrentUserId(): String {
        return SupabaseConfig.getClient().auth.currentUserOrNull()?.id
            ?: throw Exception("Usuário não autenticado")
    }

    private suspend fun runSaving(context: Context, action: suspend () -> Unit) {
        _isSaving.value = true
        try {
            action()
        } catch (e: Exception) {
            ZenithNotifier.error("Erro: ${e.localizedMessage}")
        } finally {
            _isSaving.value = false
        }
    }
}

data class PublicProfileUiState(
    val profile: Profile? = null,
    val badge: Badge? = null,
    val stats: br.com.zenith.viewmodels.profile.UserStats = br.com.zenith.viewmodels.profile.UserStats(),
    val challengeAwards: ChallengeAwardSummary = ChallengeAwardSummary(),
    val atividades: List<Atividade> = emptyList(),
    val friends: List<ProfileFriendItem> = emptyList(),
    val friendshipLabel: String? = null,
    val activityGroups: List<ActivityGroup> = emptyList(),
    val activityGroupItems: List<ActivityGroupItem> = emptyList(),
    val acceptedMentionsByActivityId: Map<String, List<Profile>> = emptyMap(),
    val canViewActivities: Boolean = true
)

data class ProfileFriendItem(
    val profile: Profile,
    val acceptedAt: String?
)

@Serializable
private data class ProfileFriendCount(
    @SerialName("profile_id") val profileId: String,
    @SerialName("friend_count") val friendCount: Int = 0
)

@Serializable
private data class ProfileFriendLink(
    @SerialName("profile_id") val profileId: String,
    @SerialName("friend_id") val friendId: String,
    @SerialName("accepted_at") val acceptedAt: String? = null
)

class PublicProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun fetchProfile(userId: String, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()

                val profile = client.postgrest.from("profiles")
                    .select(columns = Columns.raw("*, titulos(*)")) {
                        filter { eq("id", userId) }
                    }
                    .decodeSingle<Profile>()

                val badge = profile.badgeId?.let { badgeId ->
                    runCatching {
                        client.postgrest.from("badge")
                            .select { filter { eq("id", badgeId.toLong()) } }
                            .decodeSingle<Badge>()
                    }.getOrNull()
                }

                val ownActivities = runCatching {
                    client.postgrest.from("atividades")
                        .select(columns = Columns.raw("*, exercicios(*)")) {
                            filter { eq("user_id", userId) }
                        }
                        .decodeList<Atividade>()
                }.getOrDefault(emptyList())
                val mentionedActivities = runCatching {
                    client.postgrest.from("activity_mentions")
                        .select(columns = Columns.raw("*, atividades(*, exercicios(*))")) {
                            filter {
                                eq("mentioned_user_id", userId)
                                eq("status", ActivityMention.STATUS_ACCEPTED)
                                eq("show_on_mentioned_profile", true)
                            }
                        }
                        .decodeList<ActivityMention>()
                        .mapNotNull { it.activity?.copy(verificada = false) }
                }.getOrDefault(emptyList())

                val friendships = runCatching {
                    client.postgrest.from("amizades")
                        .select()
                        .decodeList<Amizade>()
                }.getOrDefault(emptyList())

                val currentUserId = client.auth.currentUserOrNull()?.id.orEmpty()
                val isFriend = friendships.any {
                    it.status == SocialUiState.STATUS_ACEITO &&
                        ((it.userId == currentUserId && it.friendId == userId) ||
                            (it.friendId == currentUserId && it.userId == userId))
                }
                val friendshipLabel = when {
                    currentUserId == userId -> null
                    isFriend -> "Amigos"
                    friendships.any {
                        it.status == SocialUiState.STATUS_PENDENTE &&
                            it.userId == currentUserId &&
                            it.friendId == userId
                    } -> "Convite enviado"
                    friendships.any {
                        it.status == SocialUiState.STATUS_PENDENTE &&
                            it.userId == userId &&
                            it.friendId == currentUserId
                    } -> "Solicitou amizade"
                    else -> null
                }
                val canViewActivities = profile.profileVisibility != "privado" || isFriend || currentUserId == userId
                val publishedActivityIds = if (profile.profileVisibility == "privado" && isFriend) {
                    runCatching {
                        client.postgrest.from("feed_entries")
                            .select()
                            .decodeList<ChallengeForumEntry>()
                            .filter { it.authorId == userId && it.entryType == "activity" }
                            .mapNotNull { it.activityId }
                            .toSet()
                    }.getOrDefault(emptySet())
                } else {
                    emptySet()
                }
                val visibleActivities = when {
                    !canViewActivities -> emptyList()
                    profile.profileVisibility == "privado" && isFriend -> {
                        (ownActivities.filter { it.id in publishedActivityIds } + mentionedActivities)
                            .distinctBy { it.id }
                    }
                    else -> (ownActivities + mentionedActivities).distinctBy { it.id }
                }
                val visibleActivityIds = visibleActivities.map { it.id }.toSet()
                val groupItems = if (visibleActivityIds.isEmpty()) {
                    emptyList()
                } else {
                    runCatching {
                        client.postgrest.from("activity_group_items")
                            .select()
                            .decodeList<ActivityGroupItem>()
                            .filter { it.activityId in visibleActivityIds }
                    }.getOrDefault(emptyList())
                }
                val groupIds = groupItems.map { it.groupId }.toSet()
                val activityGroups = if (groupIds.isEmpty()) {
                    emptyList()
                } else {
                    runCatching {
                        client.postgrest.from("activity_groups")
                            .select()
                            .decodeList<ActivityGroup>()
                            .filter { it.id in groupIds }
                    }.getOrDefault(emptyList())
                }
                val acceptedMentionsByActivityId = if (visibleActivityIds.isEmpty()) {
                    emptyMap()
                } else {
                    val acceptedMentions = runCatching {
                        client.postgrest.from("activity_mentions")
                            .select {
                                filter { eq("status", ActivityMention.STATUS_ACCEPTED) }
                            }
                            .decodeList<ActivityMention>()
                            .filter { it.activityId in visibleActivityIds && it.showOnMentionedProfile }
                    }.getOrDefault(emptyList())
                    val mentionedUserIds = acceptedMentions.map { it.mentionedUserId }.toSet()
                    val mentionProfiles = if (mentionedUserIds.isEmpty()) {
                        emptyMap()
                    } else {
                        runCatching {
                            client.postgrest.from("profiles")
                                .select()
                                .decodeList<Profile>()
                                .filter { it.id in mentionedUserIds }
                                .associateBy { it.id }
                        }.getOrDefault(emptyMap())
                    }
                    acceptedMentions
                        .groupBy { it.activityId }
                        .mapValues { entry ->
                            entry.value.mapNotNull { mentionProfiles[it.mentionedUserId] }
                                .distinctBy { it.id }
                                .sortedBy { it.name.lowercase() }
                        }
                }

                val visibleFriendCount = friendships
                    .filter {
                        it.status == SocialUiState.STATUS_ACEITO &&
                            (it.userId == userId || it.friendId == userId)
                    }
                    .map { if (it.userId == userId) it.friendId else it.userId }
                    .distinct()
                    .size
                val amigos = runCatching {
                    client.postgrest.from("profile_friend_counts")
                        .select { filter { eq("profile_id", userId) } }
                        .decodeList<ProfileFriendCount>()
                        .firstOrNull()
                        ?.friendCount
                }.getOrNull() ?: visibleFriendCount
                val friendLinks = runCatching {
                    client.postgrest.from("profile_friend_links")
                        .select { filter { eq("profile_id", userId) } }
                        .decodeList<ProfileFriendLink>()
                }.getOrDefault(emptyList())
                val fallbackFriendIds =
                    friendships
                        .filter {
                            it.status == SocialUiState.STATUS_ACEITO &&
                                (it.userId == userId || it.friendId == userId)
                        }
                        .map { if (it.userId == userId) it.friendId else it.userId }
                        .toSet()
                val friendIds = friendLinks.map { it.friendId }.toSet().ifEmpty { fallbackFriendIds }
                val acceptedAtByFriendId = friendLinks
                    .associate { it.friendId to it.acceptedAt }
                    .ifEmpty {
                        friendships
                            .filter {
                                it.status == SocialUiState.STATUS_ACEITO &&
                                    (it.userId == userId || it.friendId == userId)
                            }
                            .associate {
                                val friendId = if (it.userId == userId) it.friendId else it.userId
                                friendId to it.acceptedAt
                            }
                    }
                val friends = if (friendIds.isEmpty()) {
                    emptyList()
                } else {
                    client.postgrest.from("profiles")
                        .select()
                        .decodeList<Profile>()
                        .filter { it.id in friendIds }
                        .map { ProfileFriendItem(it, acceptedAtByFriendId[it.id]) }
                        .sortedBy { it.profile.displayName.lowercase() }
                }
                val desafios = runCatching {
                    client.postgrest.from("desafio_participacoes")
                        .select {
                            filter { eq("user_id", userId) }
                            count(Count.EXACT)
                        }
                        .countOrNull()
                        ?.toInt()
                }.getOrNull() ?: 0
                val conquistas = runCatching {
                    client.postgrest.from("conquistas")
                        .select {
                            filter { eq("user_id", userId) }
                            count(Count.EXACT)
                        }
                        .countOrNull()
                        ?.toInt()
                }.getOrNull() ?: 0
                val challengeAwards = runCatching {
                    val challengeParticipations = client.postgrest.from("desafio_participacoes")
                        .select()
                        .decodeList<DesafioParticipacao>()
                    val challenges = client.postgrest.from("desafios")
                        .select()
                        .decodeList<Desafio>()
                    val challengeActivities = client.postgrest.from("atividades")
                        .select(columns = Columns.raw("*, exercicios(*)"))
                        .decodeList<Atividade>()
                    ChallengeAwardCalculator.buildSummary(
                        userId = userId,
                        challenges = challenges,
                        participations = challengeParticipations,
                        activities = challengeActivities
                    )
                }.getOrDefault(ChallengeAwardSummary())

                _uiState.value = PublicProfileUiState(
                    profile = profile,
                    badge = badge,
                    stats = br.com.zenith.viewmodels.profile.UserStats(
                        amigos = amigos,
                        conquistas = conquistas,
                        desafios = desafios,
                        medalhas = challengeAwards.total
                    ),
                    challengeAwards = challengeAwards,
                    atividades = visibleActivities,
                    friends = friends,
                    friendshipLabel = friendshipLabel,
                    activityGroups = activityGroups,
                    activityGroupItems = groupItems,
                    acceptedMentionsByActivityId = acceptedMentionsByActivityId,
                    canViewActivities = canViewActivities
                )
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao carregar perfil: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
