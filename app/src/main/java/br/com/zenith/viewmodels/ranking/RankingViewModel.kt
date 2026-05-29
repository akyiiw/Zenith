package br.com.zenith.viewmodels.ranking

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.Atividade
import br.com.zenith.data.models.Desafio
import br.com.zenith.data.models.DesafioParticipacao
import br.com.zenith.data.models.Profile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.OffsetDateTime
import java.time.ZoneOffset

data class ChallengeRankingEntry(
    val profile: Profile,
    val progresso: Double,
    val atividades: Int,
    val minutos: Int,
    val verificadas: Int
)

data class ChallengeRanking(
    val desafio: Desafio,
    val entries: List<ChallengeRankingEntry>
) {
    fun minhaPosicao(userId: String): Int? {
        val index = entries.indexOfFirst { it.profile.id == userId }
        return if (index >= 0) index + 1 else null
    }

    fun meuEntry(userId: String): ChallengeRankingEntry? {
        return entries.firstOrNull { it.profile.id == userId }
    }
}

data class RankingUiState(
    val currentUserId: String = "",
    val desafios: List<Desafio> = emptyList(),
    val participacoes: List<DesafioParticipacao> = emptyList(),
    val selectedDesafioId: String? = null,
    val selectedRanking: ChallengeRanking? = null
) {
    fun participantes(desafioId: String): Int {
        return participacoes.count { it.desafioId == desafioId }
    }

    fun participando(desafioId: String): Boolean {
        return participacoes.any { it.desafioId == desafioId && it.userId == currentUserId }
    }
}

class RankingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RankingUiState())
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private var profilesCache: List<Profile> = emptyList()
    private var atividadesCache: List<Atividade> = emptyList()
    private var desafiosCache: List<Desafio> = emptyList()
    private var participacoesCache: List<DesafioParticipacao> = emptyList()

    fun fetchDesafios(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                loadData(context)
                publishState(selectedDesafioId = _uiState.value.selectedDesafioId)
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao carregar desafios: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun abrirDesafio(desafioId: String) {
        publishState(selectedDesafioId = desafioId)
    }

    fun voltarParaDesafios() {
        publishState(selectedDesafioId = null)
    }

    fun criarDesafio(
        titulo: String,
        descricao: String?,
        tipo: String,
        meta: Double,
        dias: Int,
        context: Context
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuario nao autenticado")
                val inicio = OffsetDateTime.now(ZoneOffset.UTC)
                val fim = inicio.plusDays(dias.toLong())
                val unidade = when (tipo) {
                    "minutos" -> "min"
                    "distancia" -> "km"
                    else -> "atividades"
                }

                client.postgrest.from("desafios").insert(
                    buildJsonObject {
                        put("titulo", titulo.trim())
                        descricao?.trim()?.takeIf { it.isNotBlank() }?.let { put("descricao", it) }
                        put("criador_id", userId)
                        put("tipo", tipo)
                        put("meta", meta)
                        put("unidade", unidade)
                        put("inicio_em", inicio.toString())
                        put("fim_em", fim.toString())
                    }
                )

                Toast.makeText(context, "Desafio criado", Toast.LENGTH_SHORT).show()
                loadData(context)
                publishState(selectedDesafioId = null)
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao criar desafio: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun participarDesafio(desafio: Desafio, context: Context) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuario nao autenticado")

                if (_uiState.value.participando(desafio.id)) return@launch

                client.postgrest.from("desafio_participacoes").insert(
                    buildJsonObject {
                        put("desafio_id", desafio.id)
                        put("user_id", userId)
                    }
                )

                Toast.makeText(context, "Voce entrou no desafio", Toast.LENGTH_SHORT).show()
                loadData(context)
                publishState(selectedDesafioId = desafio.id)
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao entrar no desafio: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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

        desafiosCache = client.postgrest.from("desafios")
            .select()
            .decodeList<Desafio>()

        participacoesCache = client.postgrest.from("desafio_participacoes")
            .select()
            .decodeList<DesafioParticipacao>()

        atividadesCache = client.postgrest.from("atividades")
            .select()
            .decodeList<Atividade>()

        _uiState.value = _uiState.value.copy(currentUserId = currentUserId)
    }

    private fun publishState(selectedDesafioId: String?) {
        val selectedRanking = selectedDesafioId
            ?.let { desafioId -> desafiosCache.firstOrNull { it.id == desafioId } }
            ?.let { desafio -> ChallengeRanking(desafio, buildRanking(desafio)) }

        _uiState.value = RankingUiState(
            currentUserId = _uiState.value.currentUserId,
            desafios = desafiosCache.sortedByDescending { it.criadoEm.orEmpty() },
            participacoes = participacoesCache,
            selectedDesafioId = selectedDesafioId,
            selectedRanking = selectedRanking
        )
    }

    private fun buildRanking(desafio: Desafio): List<ChallengeRankingEntry> {
        val participantIds = participacoesCache
            .filter { it.desafioId == desafio.id }
            .map { it.userId }
            .toSet()

        val atividadesPorUsuario = atividadesCache
            .asSequence()
            .filter { it.desafioId == desafio.id }
            .filter { participantIds.isEmpty() || it.userId in participantIds }
            .filter { dentroDoPeriodo(it, desafio) }
            .groupBy { it.userId }

        val userIds = participantIds + atividadesPorUsuario.keys

        return profilesCache
            .filter { it.id in userIds }
            .map { profile ->
                val atividades = atividadesPorUsuario[profile.id].orEmpty()
                val minutos = atividades.sumOf { it.duracaoMin ?: 0 }
                val verificadas = atividades.count { it.verificada }
                val progresso = when (desafio.tipo) {
                    "minutos" -> minutos.toDouble()
                    "distancia" -> atividades.sumOf { it.valor }
                    else -> atividades.size.toDouble()
                }
                ChallengeRankingEntry(
                    profile = profile,
                    progresso = progresso,
                    atividades = atividades.size,
                    minutos = minutos,
                    verificadas = verificadas
                )
            }
            .sortedWith(
                compareByDescending<ChallengeRankingEntry> { it.progresso }
                    .thenByDescending { it.verificadas }
                    .thenBy { it.profile.displayName }
            )
    }

    private fun dentroDoPeriodo(atividade: Atividade, desafio: Desafio): Boolean {
        val realizadaEm = atividade.realizadaEm
            ?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
            ?: return true
        val inicio = desafio.inicioEm?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
        val fim = desafio.fimEm?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
        return (inicio == null || !realizadaEm.isBefore(inicio)) &&
            (fim == null || !realizadaEm.isAfter(fim))
    }
}
