package br.com.zenith.viewmodels.social

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.Amizade
import br.com.zenith.data.models.AmizadeInsert
import br.com.zenith.data.models.Atividade
import br.com.zenith.data.models.Badge
import br.com.zenith.data.models.Profile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class SocialUiState(
    val currentUserId: String = "",
    val profiles: List<Profile> = emptyList(),
    val friendships: List<Amizade> = emptyList()
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
                Toast.makeText(context, "Erro ao carregar social: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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

        _uiState.value = SocialUiState(
            currentUserId = currentUserId,
            profiles = profiles,
            friendships = friendships
        )
    }

    private suspend fun aceitarConviteInterno(currentUserId: String, profileId: String) {
        val client = SupabaseConfig.getClient()

        client.postgrest.from("amizades").update(
            buildJsonObject { put("status", SocialUiState.STATUS_ACEITO) }
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
                    status = SocialUiState.STATUS_ACEITO
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
            Toast.makeText(context, "Erro: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        } finally {
            _isSaving.value = false
        }
    }
}

data class PublicProfileUiState(
    val profile: Profile? = null,
    val badge: Badge? = null,
    val stats: br.com.zenith.viewmodels.profile.UserStats = br.com.zenith.viewmodels.profile.UserStats(),
    val atividades: List<Atividade> = emptyList()
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

                val atividades = runCatching {
                    client.postgrest.from("atividades")
                        .select(columns = Columns.raw("*, exercicios(*)")) {
                            filter { eq("user_id", userId) }
                        }
                        .decodeList<Atividade>()
                }.getOrDefault(emptyList())

                val friendships = runCatching {
                    client.postgrest.from("amizades")
                        .select()
                        .decodeList<Amizade>()
                }.getOrDefault(emptyList())

                val amigos = friendships
                    .filter {
                        it.status == SocialUiState.STATUS_ACEITO &&
                            (it.userId == userId || it.friendId == userId)
                    }
                    .map { if (it.userId == userId) it.friendId else it.userId }
                    .distinct()
                    .size

                _uiState.value = PublicProfileUiState(
                    profile = profile,
                    badge = badge,
                    stats = br.com.zenith.viewmodels.profile.UserStats(
                        amigos = amigos,
                        conquistas = 0,
                        desafios = 0,
                        medalhas = 0
                    ),
                    atividades = atividades
                )
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao carregar perfil: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
