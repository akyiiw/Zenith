package br.com.zenith.viewmodels.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.Amizade
import br.com.zenith.data.models.Atividade
import br.com.zenith.data.models.ChallengeForumEntry
import br.com.zenith.data.models.Profile
import br.com.zenith.ui.notifications.ZenithNotifier
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class HomeFeedItem(
    val entry: ChallengeForumEntry,
    val activity: Atividade,
    val author: Profile
)

data class HomeFeedUiState(
    val currentUserId: String = "",
    val isLoading: Boolean = false,
    val isPublishing: Boolean = false,
    val error: String? = null,
    val items: List<HomeFeedItem> = emptyList(),
    val publishableActivities: List<Atividade> = emptyList()
)

class HomeFeedViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeFeedUiState(isLoading = true))
    val uiState: StateFlow<HomeFeedUiState> = _uiState.asStateFlow()
    private var hasLoadedOnce = false

    fun load(context: Context, forceRefresh: Boolean = false) {
        if (hasLoadedOnce && !forceRefresh) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                _uiState.value = loadState(context)
                hasLoadedOnce = true
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Erro ao carregar feed"
                )
            }
        }
    }

    fun publishActivity(activityId: String, context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPublishing = true)
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuario nao autenticado")

                val alreadyPublished = client.postgrest.from("feed_entries")
                    .select {
                        filter {
                            eq("author_id", userId)
                            eq("entry_type", "activity")
                            eq("activity_id", activityId)
                        }
                    }
                    .decodeList<ChallengeForumEntry>()
                    .isNotEmpty()

                if (alreadyPublished) {
                    refreshAfterPublishAttempt(context)
                    ZenithNotifier.warning("Essa atividade ja foi publicada")
                    return@launch
                }

                client.postgrest.from("feed_entries").insert(
                    buildJsonObject {
                        put("id", UUID.randomUUID().toString())
                        put("author_id", userId)
                        put("entry_type", "activity")
                        put("activity_id", activityId)
                    }
                )

                _uiState.value = loadState(context).copy(isPublishing = false)
                hasLoadedOnce = true
                ZenithNotifier.success("Atividade publicada")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isPublishing = false)
                if (e.isDuplicateActivityPublication()) {
                    refreshAfterPublishAttempt(context)
                    ZenithNotifier.warning("Essa atividade ja foi publicada")
                } else {
                    ZenithNotifier.error("Erro ao publicar: ${e.localizedMessage}")
                }
            }
        }
    }

    private suspend fun loadState(context: Context): HomeFeedUiState {
        SupabaseConfig.init(context)
        val client = SupabaseConfig.getClient()
        val currentUserId = client.auth.currentUserOrNull()?.id
            ?: throw Exception("Usuario nao autenticado")

        val friendships = client.postgrest.from("amizades")
            .select()
            .decodeList<Amizade>()
            .filter {
                it.status == "aceito" &&
                    (it.userId == currentUserId || it.friendId == currentUserId)
            }
        val friendIds = friendships.map { if (it.userId == currentUserId) it.friendId else it.userId }.toSet()

        val profiles = client.postgrest.from("profiles")
            .select()
            .decodeList<Profile>()
            .associateBy { it.id }

        val entries = runCatching {
            client.postgrest.from("feed_entries")
                .select {
                    filter {
                        eq("entry_type", "activity")
                    }
                }
                .decodeList<ChallengeForumEntry>()
        }.getOrDefault(emptyList())
            .filter { it.activityId != null }

        val activities = client.postgrest.from("atividades")
            .select(columns = Columns.raw("*, exercicios(*)"))
            .decodeList<Atividade>()

        val publishedActivityIds = entries.mapNotNull { it.activityId }.toSet()
        val activitiesById = activities.associateBy { it.id }

        val items = entries
            .filter { it.authorId in friendIds }
            .mapNotNull { entry ->
                val activity = entry.activityId?.let { activitiesById[it] } ?: return@mapNotNull null
                val author = profiles[entry.authorId] ?: return@mapNotNull null
                HomeFeedItem(entry = entry, activity = activity, author = author)
            }
            .sortedByDescending { it.entry.createdAt ?: it.activity.realizadaEm ?: it.activity.criadaEm.orEmpty() }

        val publishableActivities = activities
            .filter { it.userId == currentUserId && it.id !in publishedActivityIds }
            .sortedByDescending { it.realizadaEm ?: it.criadaEm.orEmpty() }

        return HomeFeedUiState(
            currentUserId = currentUserId,
            isLoading = false,
            items = items,
            publishableActivities = publishableActivities
        )
    }

    private suspend fun refreshAfterPublishAttempt(context: Context) {
        _uiState.value = runCatching { loadState(context) }
            .getOrElse { _uiState.value }
            .copy(isPublishing = false)
        hasLoadedOnce = true
    }

    private fun Exception.isDuplicateActivityPublication(): Boolean {
        val message = listOfNotNull(message, localizedMessage, cause?.message)
            .joinToString(" ")
            .lowercase()
        return "duplicate" in message ||
            "unique" in message ||
            "feed_entries_activity_id" in message ||
            "feed_entries_activity_unique" in message
    }
}
