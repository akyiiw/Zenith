package br.com.zenith.viewmodels.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.Atividade
import br.com.zenith.data.models.ChallengeAwardCalculator
import br.com.zenith.data.models.ChallengeAwardSummary
import br.com.zenith.data.models.Desafio
import br.com.zenith.data.models.DesafioParticipacao
import br.com.zenith.data.models.Profile
import br.com.zenith.ui.notifications.ZenithNotifier
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChallengeAwardsUiState(
    val profile: Profile? = null,
    val isOwnProfile: Boolean = false,
    val summary: ChallengeAwardSummary = ChallengeAwardSummary()
)

class ChallengeAwardsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChallengeAwardsUiState())
    val uiState: StateFlow<ChallengeAwardsUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun fetchAwards(userId: String, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                val currentUserId = client.auth.currentUserOrNull()?.id.orEmpty()

                val profile = runCatching {
                    client.postgrest.from("profiles")
                        .select { filter { eq("id", userId) } }
                        .decodeSingle<Profile>()
                }.getOrNull()

                val participations = client.postgrest.from("desafio_participacoes")
                    .select()
                    .decodeList<DesafioParticipacao>()
                val challenges = client.postgrest.from("desafios")
                    .select()
                    .decodeList<Desafio>()
                val activities = client.postgrest.from("atividades")
                    .select(columns = Columns.raw("*, exercicios(*)"))
                    .decodeList<Atividade>()

                _uiState.value = ChallengeAwardsUiState(
                    profile = profile,
                    isOwnProfile = currentUserId == userId,
                    summary = ChallengeAwardCalculator.buildSummary(
                        userId = userId,
                        challenges = challenges,
                        participations = participations,
                        activities = activities
                    )
                )
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao carregar desafios: ${e.localizedMessage}")
                _uiState.value = ChallengeAwardsUiState()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
