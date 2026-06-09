package br.com.zenith.viewmodels.activity

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zenith.data.SupabaseConfig
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ActivityViewModel : ViewModel() {

    private val _atividades = MutableStateFlow<List<Atividade>>(value = emptyList())
    val atividades: StateFlow<List<Atividade>> = _atividades
    private val _desafios = MutableStateFlow<Map<String, Desafio>>(emptyMap())
    val desafios: StateFlow<Map<String, Desafio>> = _desafios
    private val _exercicios = MutableStateFlow<List<Exercicio>>(emptyList())
    val exercicios: StateFlow<List<Exercicio>> = _exercicios
    private val _mentionFriends = MutableStateFlow<List<Profile>>(emptyList())
    val mentionFriends: StateFlow<List<Profile>> = _mentionFriends

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
                        .sortedBy { it.displayName.lowercase() }
                }
            } catch (_: Exception) {
                _mentionFriends.value = emptyList()
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
                        .mapNotNull { it.activity }
                }.getOrDefault(emptyList())
                val allActivities = (atividades + mentionedActivities)
                    .distinctBy { it.id }
                _atividades.value = allActivities

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

    fun registrarAtividade(
        exercicioId: String,
        valor: Double,
        verificada: Boolean,
        duracaoMin: Int?,
        titulo: String?,
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
        context: Context,
        onSucesso: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuário não autenticado")

                val localDateTime = parseActivityDate(data)
                val realizadaEm = localDateTime
                    .atZone(ZoneId.systemDefault())
                    .toOffsetDateTime()
                    .toString()

                val atividade = client.postgrest.from("atividades").insert(
                    buildJsonObject {
                        put("user_id", userId)
                        put("exercicio_id", exercicioId)
                        put("valor", valor)
                        put("verificada", verificada)
                        put("realizada_em", realizadaEm)
                        duracaoMin?.let { put("duracao_min", it) }
                        titulo?.let { put("titulo", it) }
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

                val streakUpdated = runCatching {
                    streakRepository.recordActivity(localDateTime.toLocalDate())
                }.isSuccess

                if (streakUpdated) {
                    ZenithNotifier.success("Atividade registrada!")
                } else {
                    ZenithNotifier.warning("Atividade registrada, mas o streak não foi atualizado")
                }
                onSucesso()
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
                val localDateTime = parseActivityDate(data)
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

    private fun parseActivityDate(data: String): LocalDateTime {
        return LocalDateTime.parse(data, ACTIVITY_DATE_FORMATTER)
    }

    companion object {
        private val ACTIVITY_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    }
}
