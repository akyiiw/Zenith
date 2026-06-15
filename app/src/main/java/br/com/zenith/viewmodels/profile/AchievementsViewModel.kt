package br.com.zenith.viewmodels.profile

import android.content.Context
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.AchievementUnlock
import br.com.zenith.data.models.Profile
import br.com.zenith.ui.notifications.ZenithNotifier
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AchievementCategory(val label: String) {
    FirstSteps("Primeiros passos"),
    Activities("Atividades"),
    Streaks("Sequências")
}

data class AchievementDefinition(
    val slug: String,
    val title: String,
    val description: String,
    val category: AchievementCategory,
    val icon: ImageVector
)

data class AchievementItem(
    val definition: AchievementDefinition,
    val unlock: AchievementUnlock?
) {
    val unlocked: Boolean
        get() = unlock != null
}

data class AchievementsUiState(
    val profile: Profile? = null,
    val isOwnProfile: Boolean = false,
    val achievements: List<AchievementItem> = emptyList()
)

class AchievementsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun fetchAchievements(userId: String, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                val currentUserId = client.auth.currentUserOrNull()?.id.orEmpty()
                val isOwnProfile = currentUserId == userId
                val profile = runCatching {
                    client.postgrest.from("profiles")
                        .select { filter { eq("id", userId) } }
                        .decodeSingle<Profile>()
                }.getOrNull()
                val unlocks = client.postgrest.from("conquistas")
                    .select {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<AchievementUnlock>()
                    .associateBy { it.slug }

                val items = definitions.map { definition ->
                    AchievementItem(
                        definition = definition,
                        unlock = unlocks[definition.slug]
                    )
                }.let { allItems ->
                    if (isOwnProfile) allItems else allItems.filter { it.unlocked }
                }

                _uiState.value = AchievementsUiState(
                    profile = profile,
                    isOwnProfile = isOwnProfile,
                    achievements = items
                )
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao carregar conquistas: ${e.localizedMessage}")
                _uiState.value = AchievementsUiState()
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        val definitions = listOf(
            AchievementDefinition(
                slug = "primeira_atividade",
                title = "Primeira atividade",
                description = "Registre pelo menos uma atividade.",
                category = AchievementCategory.FirstSteps,
                icon = Icons.AutoMirrored.Filled.DirectionsRun
            ),
            AchievementDefinition(
                slug = "atividades_10",
                title = "Atleta",
                description = "Registre 10 atividades.",
                category = AchievementCategory.Activities,
                icon = Icons.Default.WorkspacePremium
            ),
            AchievementDefinition(
                slug = "streak_7",
                title = "Dedicado",
                description = "Mantenha uma sequência de 7 dias.",
                category = AchievementCategory.Streaks,
                icon = Icons.Default.LocalFireDepartment
            ),
            AchievementDefinition(
                slug = "streak_30",
                title = "Lenda",
                description = "Mantenha uma sequência de 30 dias.",
                category = AchievementCategory.Streaks,
                icon = Icons.Default.EmojiEvents
            )
        )
    }
}
