package br.com.zenith.viewmodels.activity

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.ActivityGroup
import br.com.zenith.data.models.ActivityGroupItem
import br.com.zenith.data.models.ActivityMention
import br.com.zenith.data.models.ActivityMentionInsert
import br.com.zenith.data.models.Amizade
import br.com.zenith.data.models.Atividade
import br.com.zenith.data.models.Desafio
import br.com.zenith.data.models.Exercicio
import br.com.zenith.data.models.Profile
import br.com.zenith.data.repositories.StreakRepository
import br.com.zenith.ui.notifications.ZenithNotifier
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put
import java.util.UUID

data class ActivityDetailUiState(
    val isLoading: Boolean = false,
    val activity: Atividade? = null,
    val authorUsername: String? = null,
    val groupNames: List<String> = emptyList(),
    val errorMessage: String? = null
)

class ActivityViewModel : ViewModel() {

    private val _atividades = MutableStateFlow<List<Atividade>>(value = emptyList())
    val atividades: StateFlow<List<Atividade>> = _atividades
    private val _desafios = MutableStateFlow<Map<String, Desafio>>(emptyMap())
    val desafios: StateFlow<Map<String, Desafio>> = _desafios
    private val _exercicios = MutableStateFlow<List<Exercicio>>(emptyList())
    val exercicios: StateFlow<List<Exercicio>> = _exercicios
    private val _mentionFriends = MutableStateFlow<List<Profile>>(emptyList())
    val mentionFriends: StateFlow<List<Profile>> = _mentionFriends
    private val _activityGroups = MutableStateFlow<List<ActivityGroup>>(emptyList())
    val activityGroups: StateFlow<List<ActivityGroup>> = _activityGroups
    private val _activityGroupItems = MutableStateFlow<List<ActivityGroupItem>>(emptyList())
    val activityGroupItems: StateFlow<List<ActivityGroupItem>> = _activityGroupItems
    private val _detailState = MutableStateFlow(ActivityDetailUiState())
    val detailState: StateFlow<ActivityDetailUiState> = _detailState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving
    private val streakRepository = StreakRepository()

    fun fetchExercicios(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                _exercicios.value = client.postgrest
                    .from("exercicios")
                    .select()
                    .decodeList<Exercicio>()
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao carregar exercícios")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchMentionFriends(context: Context) {
        viewModelScope.launch {
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                val currentUserId = client.auth.currentUserOrNull()?.id ?: return@launch
                val friendships = client.postgrest.from("amizades")
                    .select()
                    .decodeList<Amizade>()
                    .filter {
                        it.status == "aceito" &&
                            (it.userId == currentUserId || it.friendId == currentUserId)
                    }
                val friendIds = friendships
                    .map { if (it.userId == currentUserId) it.friendId else it.userId }
                    .toSet()
                _mentionFriends.value = if (friendIds.isEmpty()) {
                    emptyList()
                } else {
                    client.postgrest.from("profiles")
                        .select()
                        .decodeList<Profile>()
                        .filter { it.id in friendIds }
                        .sortedBy { it.name.lowercase() }
                }
            } catch (_: Exception) {
                _mentionFriends.value = emptyList()
            }
        }
    }

    fun fetchActivityGroups(context: Context) {
        viewModelScope.launch {
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                val currentUserId = client.auth.currentUserOrNull()?.id ?: return@launch
                val groups = client.postgrest.from("activity_groups")
                    .select {
                        filter { eq("user_id", currentUserId) }
                    }
                    .decodeList<ActivityGroup>()
                    .sortedByDescending { it.createdAt.orEmpty() }
                _activityGroups.value = groups

                _activityGroupItems.value = if (groups.isEmpty()) {
                    emptyList()
                } else {
                    client.postgrest.from("activity_group_items")
                        .select()
                        .decodeList<ActivityGroupItem>()
                        .filter { item -> groups.any { it.id == item.groupId } }
                }
            } catch (_: Exception) {
                _activityGroups.value = emptyList()
                _activityGroupItems.value = emptyList()
            }
        }
    }

    fun fetchAtividades(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                val atividades = client.postgrest
                    .from(table = "atividades")
                    .select(columns = Columns.raw("*, exercicios(*)")) {
                        filter {
                            eq("user_id", client.auth.currentUserOrNull()?.id ?: "")
                        }
                    }
                    .decodeList<Atividade>()
                val mentionedActivities = runCatching {
                    client.postgrest.from("activity_mentions")
                        .select(columns = Columns.raw("*, atividades(*, exercicios(*))")) {
                            filter {
                                eq("mentioned_user_id", client.auth.currentUserOrNull()?.id ?: "")
                                eq("status", ActivityMention.STATUS_ACCEPTED)
                                eq("show_on_mentioned_profile", true)
                            }
                        }
                        .decodeList<ActivityMention>()
                        .mapNotNull { it.activity?.copy(verificada = false) }
                }.getOrDefault(emptyList())
                val allActivities = (atividades + mentionedActivities)
                    .distinctBy { it.id }
                _atividades.value = allActivities
                loadGroupItemsForActivities(allActivities)

                val linkedChallengeIds = allActivities.mapNotNull { it.desafioId }.toSet()
                _desafios.value = if (linkedChallengeIds.isEmpty()) {
                    emptyMap()
                } else {
                    runCatching {
                        client.postgrest
                            .from("desafios")
                            .select()
                            .decodeList<Desafio>()
                            .filter { it.id in linkedChallengeIds }
                            .associateBy { it.id }
                    }.getOrDefault(emptyMap())
                }
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao carregar atividades")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchAtividadePorId(atividadeId: String, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _detailState.value = ActivityDetailUiState(isLoading = true)
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                val currentUserId = client.auth.currentUserOrNull()?.id.orEmpty()
                val loadedActivity = client.postgrest
                    .from(table = "atividades")
                    .select(columns = Columns.raw("*, exercicios(*)")) {
                        filter { eq("id", atividadeId) }
                    }
                    .decodeSingle<Atividade>()
                val isMentionedParticipation = loadedActivity.userId != currentUserId && runCatching {
                    client.postgrest.from("activity_mentions")
                        .select {
                            filter {
                                eq("activity_id", loadedActivity.id)
                                eq("mentioned_user_id", currentUserId)
                                eq("status", ActivityMention.STATUS_ACCEPTED)
                            }
                        }
                        .decodeList<ActivityMention>()
                        .isNotEmpty()
                }.getOrDefault(false)
                val atividade = if (isMentionedParticipation) {
                    loadedActivity.copy(verificada = false)
                } else {
                    loadedActivity
                }
                val author = runCatching {
                    client.postgrest.from("profiles")
                        .select {
                            filter { eq("id", atividade.userId) }
                        }
                        .decodeSingle<Profile>()
                }.getOrNull()
                val groupNames = loadGroupNamesForActivity(atividade.id)

                _atividades.value = _atividades.value.filterNot { it.id == atividade.id } + atividade
                atividade.desafioId?.let { challengeId ->
                    if (!_desafios.value.containsKey(challengeId)) {
                        runCatching {
                            client.postgrest.from("desafios")
                                .select { filter { eq("id", challengeId) } }
                                .decodeSingle<Desafio>()
                        }.getOrNull()?.let { desafio ->
                            _desafios.value = _desafios.value + (desafio.id to desafio)
                        }
                    }
                }
                _detailState.value = ActivityDetailUiState(
                    activity = atividade,
                    authorUsername = author?.name,
                    groupNames = groupNames
                )
            } catch (e: Exception) {
                val message = "Erro ao carregar atividade: ${e.localizedMessage}"
                ZenithNotifier.error(message)
                _detailState.value = ActivityDetailUiState(errorMessage = message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registrarAtividade(
        exercicioId: String,
        valor: Double,
        verificada: Boolean,
        duracaoMin: Int?,
        titulo: String?,
        descricao: String?,
        data: String,
        nota: Int?,
        intensidade: String?,
        humor: String?,
        fotoUri: Uri?,
        rota: String? = null,
        desafioId: String? = null,
        passos: Int? = null,
        distanciaBruta: Double? = null,
        gpsAccuracyMedia: Double? = null,
        gpsPontosAceitos: Int? = null,
        gpsPontosRejeitados: Int? = null,
        gpsQualidade: String? = null,
        mentionedFriendIds: List<String> = emptyList(),
        activityGroupId: String? = null,
        newActivityGroupName: String? = null,
        publicarNoFeed: Boolean = false,
        context: Context,
        onSucesso: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuário não autenticado")
                validateActivityValues(valor, duracaoMin, passos)

                val atividadeId = UUID.randomUUID().toString()
                val localDateTime = parseActivityDate(data)
                val realizadaEm = localDateTime
                    .atZone(ZoneId.systemDefault())
                    .toOffsetDateTime()
                val serverNow = fetchServerNow(client)
                if (realizadaEm.isAfter(serverNow)) {
                    throw Exception("A data e o horário da atividade não podem ser futuros")
                }
                duracaoMin?.let { duration ->
                    if (realizadaEm.plusMinutes(duration.toLong()).isAfter(serverNow)) {
                        throw Exception("A duração informada termina depois do horário atual")
                    }
                }

                val atividade = client.postgrest.from("atividades").insert(
                    buildJsonObject {
                        put("id", atividadeId)
                        put("user_id", userId)
                        put("exercicio_id", exercicioId)
                        put("valor", valor)
                        put("verificada", verificada)
                        put("realizada_em", realizadaEm.toString())
                        duracaoMin?.let { put("duracao_min", it) }
                        titulo?.let { put("titulo", it) }
                        descricao?.let { put("descricao", it) }
                        nota?.let { put("nota", it) }
                        intensidade?.let { put("intensidade", it) }
                        humor?.let { put("humor", it) }
                        rota?.let { put("rota", it) }
                        desafioId?.takeIf { it.isNotBlank() }?.let { put("desafio_id", it) }
                        passos?.let { put("passos", it) }
                        distanciaBruta?.let { put("distancia_bruta", it) }
                        gpsAccuracyMedia?.let { put("gps_accuracy_media", it) }
                        gpsPontosAceitos?.let { put("gps_pontos_aceitos", it) }
                        gpsPontosRejeitados?.let { put("gps_pontos_rejeitados", it) }
                        gpsQualidade?.let { put("gps_qualidade", it) }
                    }
                ) {
                    select()
                }.decodeSingle<Atividade>()

                mentionedFriendIds.distinct().filter { it != userId }.forEach { friendId ->
                    client.postgrest.from("activity_mentions").insert(
                        ActivityMentionInsert(
                            activityId = atividade.id,
                            publisherId = userId,
                            mentionedUserId = friendId
                        )
                    )
                }

                resolveActivityGroupId(userId, activityGroupId, newActivityGroupName)?.let { groupId ->
                    runCatching {
                        client.postgrest.from("activity_group_items").insert(
                            buildJsonObject {
                                put("group_id", groupId)
                                put("activity_id", atividade.id)
                            }
                        )
                    }
                    fetchActivityGroups(context)
                }

                if (publicarNoFeed) {
                    publicarAtividadeNoFeed(atividade.id, userId)
                }

                val streakUpdated = runCatching {
                    streakRepository.recordActivity(localDateTime.toLocalDate())
                }.isSuccess

                if (streakUpdated) {
                    ZenithNotifier.success("Atividade registrada!")
                } else {
                    ZenithNotifier.warning("Atividade registrada, mas o streak não foi atualizado")
                }
                onSucesso(atividade.id)
            } catch (e: Exception) {
                ZenithNotifier.error("Erro: ${e.localizedMessage}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun editarAtividade(
        atividadeId: String,
        exercicioId: String,
        valor: Double,
        duracaoMin: Int?,
        titulo: String?,
        descricao: String?,
        data: String,
        nota: Int?,
        intensidade: String?,
        humor: String?,
        context: Context,
        onSucesso: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                validateActivityValues(valor, duracaoMin, null)
                val localDateTime = parseActivityDate(data)
                val serverNow = fetchServerNow(SupabaseConfig.getClient())
                val selectedInstant = localDateTime
                    .atZone(ZoneId.systemDefault())
                    .toOffsetDateTime()
                if (selectedInstant.isAfter(serverNow)) {
                    throw Exception("A data e o horário da atividade não podem ser futuros")
                }
                duracaoMin?.let { duration ->
                    if (selectedInstant.plusMinutes(duration.toLong()).isAfter(serverNow)) {
                        throw Exception("A duração informada termina depois do horário atual")
                    }
                }
                val realizadaEm = localDateTime
                    .atZone(ZoneId.systemDefault())
                    .toOffsetDateTime()
                    .toString()

                SupabaseConfig.getClient().postgrest.from("atividades").update(
                    buildJsonObject {
                        put("exercicio_id", exercicioId)
                        put("valor", valor)
                        put("duracao_min", duracaoMin)
                        put("titulo", titulo)
                        put("descricao", descricao)
                        put("realizada_em", realizadaEm)
                        put("nota", nota)
                        put("intensidade", intensidade)
                        put("humor", humor)
                    }
                ) {
                    filter {
                        eq("id", atividadeId)
                        eq("verificada", false)
                    }
                }

                ZenithNotifier.success("Atividade atualizada!")
                fetchAtividades(context)
                onSucesso()
            } catch (e: Exception) {
                ZenithNotifier.error("Erro: ${e.localizedMessage}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun editarMetadadosAtividade(
        atividadeId: String,
        titulo: String?,
        descricao: String?,
        nota: Int?,
        intensidade: String?,
        humor: String?,
        context: Context,
        onSucesso: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val client = SupabaseConfig.getClient()
                client.postgrest.from("atividades").update(
                    buildJsonObject {
                        put("titulo", titulo)
                        put("descricao", descricao)
                        put("nota", nota)
                        put("intensidade", intensidade)
                        put("humor", humor)
                    }
                ) {
                    filter { eq("id", atividadeId) }
                }
                _detailState.value.activity?.takeIf { it.id == atividadeId }?.let { current ->
                    _detailState.value = ActivityDetailUiState(
                        activity = current.copy(
                            titulo = titulo,
                            descricao = descricao,
                            nota = nota,
                            intensidade = intensidade,
                            humor = humor
                        )
                    )
                }
                fetchAtividades(context)
                ZenithNotifier.success("Atividade atualizada!")
                onSucesso()
            } catch (e: Exception) {
                ZenithNotifier.error("Erro: ${e.localizedMessage}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun apagarAtividade(
        atividadeId: String,
        context: Context,
        onSucesso: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val client = SupabaseConfig.getClient()
                client.postgrest.from("feed_entries").delete {
                    filter { eq("activity_id", atividadeId) }
                }
                client.postgrest.from("atividades").delete {
                    filter { eq("id", atividadeId) }
                }
                _atividades.value = _atividades.value.filterNot { it.id == atividadeId }
                _detailState.value = ActivityDetailUiState()
                fetchAtividades(context)
                ZenithNotifier.success("Atividade apagada")
                onSucesso()
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao apagar: ${e.localizedMessage}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun parseActivityDate(data: String): LocalDateTime {
        return LocalDateTime.parse(data, ACTIVITY_DATE_FORMATTER)
    }

    private suspend fun fetchServerNow(client: io.github.jan.supabase.SupabaseClient): OffsetDateTime {
        return OffsetDateTime.parse(client.postgrest.rpc("server_now").decodeAs<String>())
    }

    private fun validateActivityValues(valor: Double, duracaoMin: Int?, passos: Int?) {
        if (valor <= 0.0) throw Exception("Informe um valor maior que zero")
        if (valor > 1000.0) throw Exception("Valor alto demais para uma atividade")
        if ((duracaoMin ?: 0) > 24 * 60) throw Exception("Duração alta demais para uma atividade")
        if ((passos ?: 0) > 120_000) throw Exception("Passos altos demais para uma atividade")
    }

    private suspend fun resolveActivityGroupId(
        userId: String,
        activityGroupId: String?,
        newActivityGroupName: String?
    ): String? {
        activityGroupId?.takeIf { it.isNotBlank() }?.let { return it }
        val cleanName = newActivityGroupName?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val client = SupabaseConfig.getClient()
        val existing = _activityGroups.value.firstOrNull {
            it.name.equals(cleanName, ignoreCase = true)
        }
        if (existing != null) return existing.id

        return client.postgrest.from("activity_groups").insert(
            buildJsonObject {
                put("id", UUID.randomUUID().toString())
                put("user_id", userId)
                put("name", cleanName)
            }
        ) {
            select()
        }.decodeSingle<ActivityGroup>().id
    }

    private suspend fun loadGroupItemsForActivities(activities: List<Atividade>) {
        val userId = SupabaseConfig.getClient().auth.currentUserOrNull()?.id ?: return
        val groups = runCatching {
            SupabaseConfig.getClient().postgrest.from("activity_groups")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<ActivityGroup>()
        }.getOrDefault(emptyList())
        _activityGroups.value = groups.sortedByDescending { it.createdAt.orEmpty() }
        val activityIds = activities.map { it.id }.toSet()
        _activityGroupItems.value = runCatching {
            SupabaseConfig.getClient().postgrest.from("activity_group_items")
                .select()
                .decodeList<ActivityGroupItem>()
                .filter { it.activityId in activityIds }
        }.getOrDefault(emptyList())
    }

    private suspend fun loadGroupNamesForActivity(activityId: String): List<String> {
        val client = SupabaseConfig.getClient()
        val items = runCatching {
            client.postgrest.from("activity_group_items")
                .select {
                    filter { eq("activity_id", activityId) }
                }
                .decodeList<ActivityGroupItem>()
        }.getOrDefault(emptyList())
        if (items.isEmpty()) return emptyList()
        val groupIds = items.map { it.groupId }.toSet()
        val groups = runCatching {
            client.postgrest.from("activity_groups")
                .select()
                .decodeList<ActivityGroup>()
                .filter { it.id in groupIds }
        }.getOrDefault(emptyList())
        return groups.map { it.name }.sorted()
    }

    private suspend fun publicarAtividadeNoFeed(atividadeId: String, userId: String) {
        val client = SupabaseConfig.getClient()
        val alreadyPublished = client.postgrest.from("feed_entries")
            .select {
                filter {
                    eq("author_id", userId)
                    eq("entry_type", "activity")
                    eq("activity_id", atividadeId)
                }
            }
            .decodeList<JsonObject>()
            .isNotEmpty()

        if (alreadyPublished) return

        runCatching {
            client.postgrest.from("feed_entries").insert(
                buildJsonObject {
                    put("id", UUID.randomUUID().toString())
                    put("author_id", userId)
                    put("entry_type", "activity")
                    put("activity_id", atividadeId)
                }
            )
        }.onFailure { error ->
            if (!error.isDuplicateActivityPublication()) throw error
        }
    }

    private fun Throwable.isDuplicateActivityPublication(): Boolean {
        val text = listOfNotNull(message, localizedMessage, cause?.message)
            .joinToString(" ")
            .lowercase()
        return "duplicate" in text ||
            "unique" in text ||
            "feed_entries_activity_id" in text ||
            "feed_entries_activity_unique" in text
    }

    companion object {
        private val ACTIVITY_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    }
}
