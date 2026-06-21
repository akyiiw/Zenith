package br.com.zenith.viewmodels.challenge

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.ChallengeForumComment
import br.com.zenith.data.models.ChallengeForumEntry
import br.com.zenith.data.models.ChallengeForumMedia
import br.com.zenith.data.models.ChallengeForumPost
import br.com.zenith.data.models.Amizade
import br.com.zenith.data.models.ChallengeAwardCalculator.isFinished
import br.com.zenith.data.models.Atividade as UserActivity
import br.com.zenith.data.models.Desafio as Challenge
import br.com.zenith.data.models.DesafioParticipacao as ChallengeParticipation
import br.com.zenith.data.models.Profile
import br.com.zenith.ui.notifications.ZenithNotifier
import br.com.zenith.utils.ImageUtils
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

data class ChallengeEntry(
    val profile: Profile,
    val progress: Double,
    val activities: Int,
    val minutes: Int,
    val verifiedActivities: Int
)

data class ChallengeDetails(
    val challenge: Challenge,
    val entries: List<ChallengeEntry>
) {
    fun userPosition(userId: String): Int? {
        val index = entries.indexOfFirst { it.profile.id == userId }
        return if (index >= 0) index + 1 else null
    }

    fun userEntry(userId: String): ChallengeEntry? {
        return entries.firstOrNull { it.profile.id == userId }
    }
}

data class ChallengeForumCommentItem(
    val comment: ChallengeForumComment,
    val author: Profile?
)

data class ChallengeForumItem(
    val post: ChallengeForumPost,
    val author: Profile?,
    val entry: ChallengeForumEntry?,
    val media: List<ChallengeForumMedia>,
    val comments: List<ChallengeForumCommentItem>
)

data class ChallengeDraft(
    val title: String,
    val description: String?,
    val designatedActivity: String,
    val goalMode: String,
    val rankingType: String,
    val objectiveMetric: String?,
    val objectiveValue: Double?,
    val visibility: String,
    val premiumOnly: Boolean,
    val maxParticipants: Int?,
    val startDate: OffsetDateTime,
    val endDate: OffsetDateTime,
    val allowManualEntries: Boolean,
    val bannerUri: Uri?
)

data class ChallengeUiState(
    val currentUserId: String = "",
    val challenges: List<Challenge> = emptyList(),
    val participations: List<ChallengeParticipation> = emptyList(),
    val profiles: List<Profile> = emptyList(),
    val selectedChallengeId: String? = null,
    val selectedChallengeDetails: ChallengeDetails? = null,
    val selectedForumPosts: List<ChallengeForumItem> = emptyList()
) {
    fun participantCount(challengeId: String): Int {
        return participations.count { it.desafioId == challengeId }
    }

    fun isParticipating(challengeId: String): Boolean {
        return participations.any { it.desafioId == challengeId && it.userId == currentUserId }
    }

    fun creatorName(creatorId: String): String {
        val profile = profiles.firstOrNull { it.id == creatorId }
        return profile?.name
            ?.takeIf { it.isNotBlank() }
            ?.let { "@$it" }
            ?: "Criador desconhecido"
    }

    fun currentUserIsPremium(): Boolean {
        return profiles.firstOrNull { it.id == currentUserId }?.isPremium == true
    }
}

class ChallengeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChallengeUiState())
    val uiState: StateFlow<ChallengeUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private var profilesCache: List<Profile> = emptyList()
    private var activitiesCache: List<UserActivity> = emptyList()
    private var challengesCache: List<Challenge> = emptyList()
    private var participationsCache: List<ChallengeParticipation> = emptyList()
    private var friendshipsCache: List<Amizade> = emptyList()
    private var forumPostsCache: List<ChallengeForumPost> = emptyList()
    private var forumEntriesCache: List<ChallengeForumEntry> = emptyList()
    private var forumMediaCache: List<ChallengeForumMedia> = emptyList()
    private var forumCommentsCache: List<ChallengeForumComment> = emptyList()

    fun fetchChallenges(context: Context, selectedChallengeId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                loadData(context)
                publishState(selectedChallengeId = selectedChallengeId ?: _uiState.value.selectedChallengeId)
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao carregar desafios: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun openChallenge(challengeId: String) {
        publishState(selectedChallengeId = challengeId)
    }

    fun returnToChallenges() {
        publishState(selectedChallengeId = null)
    }

    fun createChallenge(
        title: String,
        description: String?,
        type: String,
        designatedActivity: String,
        premiumOnly: Boolean,
        goal: Double,
        days: Int,
        bannerUri: Uri?,
        context: Context,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuario nao autenticado")
                val startDate = OffsetDateTime.now(ZoneOffset.UTC)
                val endDate = startDate.plusDays(days.toLong())
                val unit = when (type) {
                    "minutos" -> "min"
                    "distancia" -> "km"
                    else -> "atividades"
                }
                val bannerHash = bannerUri?.let { uri ->
                    uploadChallengeBanner(
                        context = context,
                        uri = uri,
                        path = "$userId/challenge_${System.currentTimeMillis()}.jpg"
                    )
                }

                client.postgrest.from("desafios").insert(
                    buildJsonObject {
                        put("titulo", title.trim())
                        description?.trim()?.takeIf { it.isNotBlank() }?.let { put("descricao", it) }
                        put("criador_id", userId)
                        put("tipo", type)
                        put("atividade_designada", designatedActivity)
                        put("apenas_premium", premiumOnly)
                        put("meta", goal)
                        put("unidade", unit)
                        put("inicio_em", startDate.toString())
                        put("fim_em", endDate.toString())
                        bannerHash?.let { put("banner_hash", it) }
                    }
                )

                ZenithNotifier.success("Desafio criado")
                loadData(context)
                publishState(selectedChallengeId = null)
                onSuccess()
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao criar desafio: ${e.localizedMessage}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun createChallenge(
        draft: ChallengeDraft,
        context: Context,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val title = draft.title.trim()
                if (title.isBlank()) throw Exception("Informe o nome do desafio")
                if (draft.endDate.isBefore(draft.startDate)) throw Exception("Data final inválida")
                if (draft.goalMode == "fixa" && (draft.objectiveValue == null || draft.objectiveValue <= 0.0)) {
                    throw Exception("Informe a meta fixa")
                }
                if (draft.visibility != "convite" && draft.maxParticipants != null && draft.maxParticipants <= 0) {
                    throw Exception("Limite de participantes inválido")
                }

                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuario nao autenticado")
                val serverNow = fetchServerNow(client)
                if (draft.endDate.isBefore(serverNow)) {
                    throw Exception("O período do desafio não pode ficar no passado")
                }
                val currentProfile = profilesCache.firstOrNull { it.id == userId }
                    ?: runCatching {
                        client.postgrest.from("profiles").select {
                            filter { eq("id", userId) }
                        }.decodeSingle<Profile>()
                    }.getOrNull()

                if (currentProfile?.isPremium != true) {
                    throw Exception("Apenas usuários Premium podem criar desafios")
                }

                val compatibilityMetric = compatibilityMetric(draft.rankingType)
                val unit = unitForMetric(compatibilityMetric)
                val goal = if (draft.goalMode == "livre") 0.0 else draft.objectiveValue ?: 0.0
                val type = typeForMetric(compatibilityMetric)
                val bannerHash = draft.bannerUri?.let { uri ->
                    uploadChallengeBanner(
                        context = context,
                        uri = uri,
                        path = "$userId/challenge_${System.currentTimeMillis()}.jpg"
                    )
                }

                val challengeId = UUID.randomUUID().toString()
                client.postgrest.from("desafios").insert(
                    buildJsonObject {
                        put("id", challengeId)
                        put("titulo", title)
                        draft.description?.trim()?.takeIf { it.isNotBlank() }?.let { put("descricao", it) }
                        put("criador_id", userId)
                        put("tipo", type)
                        put("atividade_designada", draft.designatedActivity)
                        put("apenas_premium", draft.premiumOnly)
                        put("meta", goal)
                        put("unidade", unit)
                        put("inicio_em", draft.startDate.toString())
                        put("fim_em", draft.endDate.toString())
                        put("modo_meta", draft.goalMode)
                        put("metrica", compatibilityMetric)
                        put("visibilidade", draft.visibility)
                        put("ranking_tipo", draft.rankingType)
                        draft.objectiveMetric?.let { put("objetivo_metrica", it) }
                        draft.objectiveValue?.let { put("objetivo_valor", it) }
                        draft.maxParticipants?.takeIf { draft.visibility != "convite" }?.let {
                            put("max_participantes", it)
                        }
                        put("aceita_registro_manual", draft.allowManualEntries)
                        bannerHash?.let { put("banner_hash", it) }
                    }
                )

                ZenithNotifier.success("Desafio criado")
                loadData(context)
                publishState(selectedChallengeId = challengeId)
                onSuccess()
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao criar desafio: ${e.localizedMessage}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun joinChallenge(challenge: Challenge, context: Context) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuario nao autenticado")

                if (challenge.isFinished()) {
                    throw Exception("Este desafio já foi finalizado")
                }
                if (_uiState.value.isParticipating(challenge.id)) return@launch
                val currentProfile = profilesCache.firstOrNull { it.id == userId }
                if (challenge.apenasPremium && currentProfile?.isPremium != true) {
                    throw Exception("Este desafio é apenas para usuários Premium")
                }
                val participantCount = participationsCache.count { it.desafioId == challenge.id }
                if (challenge.maxParticipantes != null && participantCount >= challenge.maxParticipantes) {
                    throw Exception("Este desafio atingiu o limite de participantes")
                }
                if (challenge.inscricoesFechadas) {
                    throw Exception("As inscrições deste desafio estão fechadas")
                }
                if (!canJoinChallenge(userId, challenge)) {
                    throw Exception("Você não tem acesso a este desafio")
                }

                client.postgrest.from("desafio_participacoes").insert(
                    buildJsonObject {
                        put("desafio_id", challenge.id)
                        put("user_id", userId)
                    }
                )

                ZenithNotifier.success("Você entrou no desafio")
                loadData(context)
                publishState(selectedChallengeId = challenge.id)
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao entrar no desafio: ${e.localizedMessage}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun setChallengeClosed(challengeId: String, closed: Boolean, context: Context) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuario nao autenticado")
                val challenge = challengesCache.firstOrNull { it.id == challengeId }
                    ?: throw Exception("Desafio não encontrado")
                if (challenge.criadorId != userId) throw Exception("Apenas o criador pode moderar")

                client.postgrest.from("desafios").update(
                    buildJsonObject { put("inscricoes_fechadas", closed) }
                ) { filter { eq("id", challengeId) } }

                ZenithNotifier.success(if (closed) "Inscrições fechadas" else "Inscrições reabertas")
                loadData(context)
                publishState(selectedChallengeId = challengeId)
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao moderar desafio: ${e.localizedMessage}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteChallengeForumPost(challengeId: String, postId: String, context: Context) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuario nao autenticado")
                val challenge = challengesCache.firstOrNull { it.id == challengeId }
                    ?: throw Exception("Desafio não encontrado")
                if (challenge.criadorId != userId) throw Exception("Apenas o criador pode moderar")

                client.postgrest.from("posts").delete {
                    filter {
                        eq("id", postId)
                        eq("desafio_id", challengeId)
                    }
                }

                ZenithNotifier.success("Publicação removida")
                loadData(context)
                publishState(selectedChallengeId = challengeId)
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao apagar publicação: ${e.localizedMessage}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun createChallengeForumPost(
        challengeId: String,
        content: String,
        imageUris: List<Uri>,
        context: Context
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val trimmedContent = content.trim()
                if (trimmedContent.isBlank() && imageUris.isEmpty()) return@launch

                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuario nao autenticado")

                if (!_uiState.value.isParticipating(challengeId)) {
                    throw Exception("Entre no desafio para publicar")
                }

                val postId = UUID.randomUUID().toString()
                client.postgrest.from("posts").insert(
                    buildJsonObject {
                        put("id", postId)
                        put("author_id", userId)
                        put("desafio_id", challengeId)
                        if (trimmedContent.isNotBlank()) put("content", trimmedContent)
                    }
                )

                imageUris.take(4).forEachIndexed { index, uri ->
                    val storagePath = uploadPostMedia(
                        context = context,
                        uri = uri,
                        path = "$userId/forum_${postId}_$index.jpg"
                    )
                    client.postgrest.from("post_media").insert(
                        buildJsonObject {
                            put("id", UUID.randomUUID().toString())
                            put("post_id", postId)
                            put("author_id", userId)
                            put("storage_path", storagePath)
                            put("position", index)
                        }
                    )
                }

                ZenithNotifier.success("Publicação enviada")
                loadData(context)
                publishState(selectedChallengeId = challengeId)
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao publicar: ${e.localizedMessage}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun createChallengeForumComment(
        challengeId: String,
        entryId: String,
        content: String,
        context: Context
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val trimmedContent = content.trim()
                if (trimmedContent.isBlank()) return@launch

                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuario nao autenticado")

                if (!_uiState.value.isParticipating(challengeId)) {
                    throw Exception("Entre no desafio para comentar")
                }

                client.postgrest.from("feed_comments").insert(
                    buildJsonObject {
                        put("id", UUID.randomUUID().toString())
                        put("entry_id", entryId)
                        put("author_id", userId)
                        put("content", trimmedContent)
                    }
                )

                loadData(context)
                publishState(selectedChallengeId = challengeId)
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao comentar: ${e.localizedMessage}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    private suspend fun loadData(context: Context) {
        SupabaseConfig.init(context)
        val client = SupabaseConfig.getClient()
        val currentUserId = client.auth.currentUserOrNull()?.id
            ?: throw Exception("Usuario nao autenticado")

        profilesCache = client.postgrest.from("profiles")
            .select()
            .decodeList<Profile>()

        challengesCache = client.postgrest.from("desafios")
            .select()
            .decodeList<Challenge>()

        participationsCache = client.postgrest.from("desafio_participacoes")
            .select()
            .decodeList<ChallengeParticipation>()

        friendshipsCache = runCatching {
            client.postgrest.from("amizades")
                .select()
                .decodeList<Amizade>()
        }.getOrDefault(emptyList())

        activitiesCache = client.postgrest.from("atividades")
            .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, exercicios(*)"))
            .decodeList<UserActivity>()

        forumPostsCache = runCatching {
            client.postgrest.from("posts")
                .select()
                .decodeList<ChallengeForumPost>()
        }.getOrDefault(emptyList())

        forumEntriesCache = runCatching {
            client.postgrest.from("feed_entries")
                .select()
                .decodeList<ChallengeForumEntry>()
        }.getOrDefault(emptyList())

        forumMediaCache = runCatching {
            client.postgrest.from("post_media")
                .select()
                .decodeList<ChallengeForumMedia>()
        }.getOrDefault(emptyList())

        forumCommentsCache = runCatching {
            client.postgrest.from("feed_comments")
                .select()
                .decodeList<ChallengeForumComment>()
        }.getOrDefault(emptyList())

        _uiState.value = _uiState.value.copy(currentUserId = currentUserId)
    }

    private fun publishState(selectedChallengeId: String?) {
        val selectedChallengeDetails = selectedChallengeId
            ?.let { challengeId -> challengesCache.firstOrNull { it.id == challengeId } }
            ?.let { challenge -> ChallengeDetails(challenge, buildChallengeEntries(challenge)) }

        val currentUserId = _uiState.value.currentUserId
        _uiState.value = ChallengeUiState(
            currentUserId = _uiState.value.currentUserId,
            challenges = challengesCache
                .filter { canSeeChallenge(currentUserId, it) }
                .sortedByDescending { it.criadoEm.orEmpty() },
            participations = participationsCache,
            profiles = profilesCache,
            selectedChallengeId = selectedChallengeId,
            selectedChallengeDetails = selectedChallengeDetails,
            selectedForumPosts = buildForum(selectedChallengeId)
        )
    }

    private suspend fun fetchServerNow(client: io.github.jan.supabase.SupabaseClient): OffsetDateTime {
        return OffsetDateTime.parse(client.postgrest.rpc("server_now").decodeAs<String>())
    }

    private fun buildForum(challengeId: String?): List<ChallengeForumItem> {
        if (challengeId == null) return emptyList()
        val posts = forumPostsCache
            .filter { it.desafioId == challengeId }
            .sortedByDescending { it.createdAt.orEmpty() }
        val entriesByPost = forumEntriesCache
            .filter { it.entryType == "post" && it.postId != null }
            .associateBy { it.postId }
        val mediaByPost = forumMediaCache
            .groupBy { it.postId }
        val commentsByEntry = forumCommentsCache
            .groupBy { it.entryId }

        return posts.map { post ->
            val entry = entriesByPost[post.id]
            val comments = entry
                ?.let { commentsByEntry[it.id].orEmpty() }
                .orEmpty()
                .sortedBy { it.createdAt.orEmpty() }
                .map { comment ->
                    ChallengeForumCommentItem(
                        comment = comment,
                        author = profilesCache.firstOrNull { it.id == comment.authorId }
                    )
                }
            ChallengeForumItem(
                post = post,
                author = profilesCache.firstOrNull { it.id == post.authorId },
                entry = entry,
                media = mediaByPost[post.id].orEmpty().sortedBy { it.position },
                comments = comments
            )
        }
    }

    private fun buildChallengeEntries(challenge: Challenge): List<ChallengeEntry> {
        val participantIds = participationsCache
            .filter { it.desafioId == challenge.id }
            .map { it.userId }
            .toSet()

        val activitiesByUser = activitiesCache
            .asSequence()
            .filter { it.desafioId == challenge.id }
            .filter { participantIds.isEmpty() || it.userId in participantIds }
            .filter { isCompatibleExercise(it, challenge) }
            .filter { isWithinChallengePeriod(it, challenge) }
            .filter { canUseActivityForChallenge(it, challenge) }
            .groupBy { it.userId }

        val userIds = participantIds + activitiesByUser.keys

        return profilesCache
            .filter { it.id in userIds }
            .map { profile ->
                val activities = activitiesByUser[profile.id].orEmpty()
                val minutes = activities.sumOf { it.duracaoMin ?: 0 }
                val verifiedActivities = activities.count { it.verificada }
                val selectedActivities = rankedActivitiesForChallenge(activities, challenge)
                val progress = when (challenge.rankingTipo) {
                    "menor_tempo", "menor_pace" -> selectedActivities.firstOrNull()?.duracaoMin?.toDouble() ?: 0.0
                    "maior_distancia" -> selectedActivities.firstOrNull()?.let { activityMetric(it, challenge) } ?: 0.0
                    "tempo_total" -> minutes.toDouble()
                    else -> activities.sumOf { activityMetric(it, challenge) }
                }
                ChallengeEntry(
                    profile = profile,
                    progress = progress,
                    activities = selectedActivities.ifEmpty { activities }.size,
                    minutes = selectedActivities.ifEmpty { activities }.sumOf { it.duracaoMin ?: 0 },
                    verifiedActivities = selectedActivities.ifEmpty { activities }.count { it.verificada }
                )
            }
            .filter { challenge.modoMeta == "livre" || it.progress > 0.0 }
            .sortedWith(challengeEntryComparator(challenge))
    }

    private fun isWithinChallengePeriod(activity: UserActivity, challenge: Challenge): Boolean {
        val completedAt = activity.realizadaEm
            ?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
            ?: return true
        val startDate = challenge.inicioEm?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
        val endDate = challenge.fimEm?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
        return (startDate == null || !completedAt.isBefore(startDate)) &&
            (endDate == null || !completedAt.isAfter(endDate))
    }

    private fun rankedActivitiesForChallenge(
        activities: List<UserActivity>,
        challenge: Challenge
    ): List<UserActivity> {
        if (challenge.modoMeta == "livre") return activities
        return when (challenge.rankingTipo) {
            "maior_distancia" -> activities
                .filter { (it.duracaoMin ?: 0).toDouble() <= (challenge.objetivoValor ?: challenge.meta) && (it.duracaoMin ?: 0) > 0 }
                .sortedWith(
                    compareByDescending<UserActivity> { activityMetric(it, challenge) }
                        .thenBy { it.duracaoMin ?: Int.MAX_VALUE }
                        .thenByDescending { it.verificada }
                )
                .take(1)
            else -> activities
                .filter { activityMetric(it, challenge) >= (challenge.objetivoValor ?: challenge.meta) }
                .sortedWith(
                    compareBy<UserActivity> { rankingDurationOrPace(it, challenge) }
                        .thenByDescending { it.verificada }
                        .thenByDescending { activityMetric(it, challenge) }
                )
                .take(1)
        }
    }

    private fun challengeEntryComparator(challenge: Challenge): Comparator<ChallengeEntry> {
        return when {
            challenge.rankingTipo == "menor_tempo" || challenge.rankingTipo == "menor_pace" ->
                compareBy<ChallengeEntry> { if (it.minutes > 0) it.minutes else Int.MAX_VALUE }
                    .thenByDescending { it.verifiedActivities }
                    .thenBy { it.profile.name }
            else ->
                compareByDescending<ChallengeEntry> { it.progress }
                    .thenByDescending { it.verifiedActivities }
                    .thenBy { it.profile.name }
        }
    }

    private fun activityMetric(activity: UserActivity, challenge: Challenge): Double {
        return when (challenge.metrica) {
            "passos" -> activity.passos?.toDouble() ?: if (challenge.unidade.contains("pass", true)) activity.valor else 0.0
            "tempo" -> (activity.duracaoMin ?: 0).toDouble()
            else -> distanceValue(activity)
        }
    }

    private fun distanceValue(activity: UserActivity): Double = activity.valor

    private fun rankingDurationOrPace(activity: UserActivity, challenge: Challenge): Double {
        val minutes = activity.duracaoMin ?: Int.MAX_VALUE
        if (challenge.rankingTipo != "menor_pace") return minutes.toDouble()
        val distance = distanceValue(activity).coerceAtLeast(0.01)
        return minutes / distance
    }

    private fun canUseActivityForChallenge(activity: UserActivity, challenge: Challenge): Boolean {
        if (activity.desafioId != challenge.id) return false
        if (activity.gpsQualidade == "ruim") return false
        return activity.verificada || challenge.aceitaRegistroManual
    }

    private fun isCompatibleExercise(activity: UserActivity, challenge: Challenge): Boolean {
        val exercise = activity.exercicio ?: return true
        val text = "${exercise.slug} ${exercise.nome} ${exercise.grupo.orEmpty()}".lowercase()
        return when (challenge.atividadeDesignada) {
            "corrida" -> text.contains("corrida") || text.contains("correr") || text.contains("run")
            "ciclismo" -> text.contains("ciclismo") || text.contains("bicicleta") || text.contains("bike") || text.contains("cycling")
            else -> text.contains("caminhada") || text.contains("caminhar") || text.contains("walk")
        }
    }

    private fun canSeeChallenge(userId: String, challenge: Challenge): Boolean {
        if (challenge.visibilidade == "publico") return true
        if (challenge.criadorId == userId) return true
        if (participationsCache.any { it.desafioId == challenge.id && it.userId == userId }) return true
        return challenge.visibilidade == "amigos" && areFriends(userId, challenge.criadorId)
    }

    private fun canJoinChallenge(userId: String, challenge: Challenge): Boolean {
        if (challenge.visibilidade == "publico") return true
        if (challenge.criadorId == userId) return true
        return challenge.visibilidade == "amigos" && areFriends(userId, challenge.criadorId)
    }

    private fun areFriends(userId: String, otherUserId: String): Boolean {
        return friendshipsCache.any {
            it.status == "aceito" &&
                ((it.userId == userId && it.friendId == otherUserId) ||
                    (it.userId == otherUserId && it.friendId == userId))
        }
    }

    private fun unitForMetric(metric: String): String = when (metric) {
        "passos" -> "passos"
        "tempo" -> "min"
        else -> "km"
    }

    private fun typeForMetric(metric: String): String = when (metric) {
        "tempo" -> "minutos"
        "distancia" -> "distancia"
        else -> "atividades"
    }

    private fun compatibilityMetric(rankingType: String): String = when (rankingType) {
        "maior_distancia", "distancia_total" -> "distancia"
        "tempo_total" -> "tempo"
        else -> "distancia"
    }

    private suspend fun uploadChallengeBanner(
        context: Context,
        uri: Uri,
        path: String
    ): String {
        val originalFile = copyUriToCacheFile(context, uri)
        val compressedFile = ImageUtils.comprimirImagem(context, originalFile, maxDimensao = 1080)
        val bytes = compressedFile.readBytes()

        if (bytes.isEmpty()) {
            throw Exception("Imagem inválida")
        }

        SupabaseConfig.getClient().storage.from("challenge-banners").upload(path, bytes) {
            upsert = true
        }

        return path
    }

    private suspend fun uploadPostMedia(
        context: Context,
        uri: Uri,
        path: String
    ): String {
        val originalFile = copyUriToCacheFile(context, uri)
        val compressedFile = ImageUtils.comprimirImagem(context, originalFile, maxDimensao = 1280)
        val bytes = compressedFile.readBytes()

        if (bytes.isEmpty()) {
            throw Exception("Imagem inválida")
        }

        SupabaseConfig.getClient().storage.from("post-media").upload(path, bytes) {
            upsert = true
        }

        return path
    }

    private fun copyUriToCacheFile(context: Context, uri: Uri): File {
        val file = File(context.cacheDir, "zenith_challenge_${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw Exception("Não foi possível abrir a imagem")
        return file
    }
}
