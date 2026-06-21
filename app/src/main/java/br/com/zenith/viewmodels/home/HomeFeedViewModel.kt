package br.com.zenith.viewmodels.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.ActivityGroup
import br.com.zenith.data.models.ActivityGroupItem
import br.com.zenith.data.models.ActivityMention
import br.com.zenith.data.models.Amizade
import br.com.zenith.data.models.Atividade
import br.com.zenith.data.models.ChallengeForumEntry
import br.com.zenith.data.models.ChallengeForumMedia
import br.com.zenith.data.models.ChallengeForumPost
import br.com.zenith.data.models.FeedEntryView
import br.com.zenith.data.models.Profile
import br.com.zenith.ui.notifications.ZenithNotifier
import br.com.zenith.utils.ImageUtils
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class HomeFeedItem(
    val entry: ChallengeForumEntry,
    val author: Profile,
    val activity: Atividade? = null,
    val post: ChallengeForumPost? = null,
    val media: List<ChallengeForumMedia> = emptyList(),
    val groupNames: List<String> = emptyList(),
    val acceptedMentions: List<Profile> = emptyList()
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

    fun publishPost(content: String, imageUris: List<Uri>, context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPublishing = true)
            try {
                val trimmedContent = content.trim()
                val selectedImages = imageUris.take(4)
                if (trimmedContent.isBlank() && selectedImages.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isPublishing = false)
                    return@launch
                }

                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuario nao autenticado")
                val postId = UUID.randomUUID().toString()

                client.postgrest.from("posts").insert(
                    buildJsonObject {
                        put("id", postId)
                        put("author_id", userId)
                        if (trimmedContent.isNotBlank()) put("content", trimmedContent)
                    }
                )

                selectedImages.forEachIndexed { index, uri ->
                    val storagePath = uploadPostMedia(
                        context = context,
                        uri = uri,
                        path = "$userId/feed_${postId}_$index.jpg"
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

                _uiState.value = loadState(context).copy(isPublishing = false)
                hasLoadedOnce = true
                ZenithNotifier.success("Publicacao enviada")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isPublishing = false)
                ZenithNotifier.error("Erro ao publicar: ${e.localizedMessage}")
            }
        }
    }

    fun markAsViewed(entryId: String, context: Context) {
        viewModelScope.launch {
            val previousState = _uiState.value
            _uiState.value = previousState.copy(
                items = previousState.items.filterNot { it.entry.id == entryId }
            )
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuario nao autenticado")

                runCatching {
                    client.postgrest.from("feed_entry_views").insert(
                        buildJsonObject {
                            put("user_id", userId)
                            put("feed_entry_id", entryId)
                        }
                    )
                }.onFailure { error ->
                    if (!error.isDuplicateFeedView()) throw error
                }
            } catch (e: Exception) {
                _uiState.value = previousState
                ZenithNotifier.error("Erro ao ocultar post visto: ${e.localizedMessage}")
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
                .select()
                .decodeList<ChallengeForumEntry>()
        }.getOrDefault(emptyList())
            .filter { it.activityId != null || it.postId != null }

        val viewedEntryIds = runCatching {
            client.postgrest.from("feed_entry_views")
                .select {
                    filter { eq("user_id", currentUserId) }
                }
                .decodeList<FeedEntryView>()
                .map { it.feedEntryId }
                .toSet()
        }.getOrDefault(emptySet())

        val activities = client.postgrest.from("atividades")
            .select(columns = Columns.raw("*, exercicios(*)"))
            .decodeList<Atividade>()
        val posts = runCatching {
            client.postgrest.from("posts")
                .select()
                .decodeList<ChallengeForumPost>()
        }.getOrDefault(emptyList())
        val postMedia = runCatching {
            client.postgrest.from("post_media")
                .select()
                .decodeList<ChallengeForumMedia>()
        }.getOrDefault(emptyList())

        val publishedActivityIds = entries.mapNotNull { it.activityId }.toSet()
        val activitiesById = activities.associateBy { it.id }
        val postsById = posts.associateBy { it.id }
        val mediaByPostId = postMedia
            .sortedBy { it.position }
            .groupBy { it.postId }
        val visibleActivityIds = entries
            .filter { (it.authorId in friendIds || it.authorId == currentUserId) && it.id !in viewedEntryIds }
            .mapNotNull { it.activityId }
            .toSet()
        val groupNamesByActivityId = loadGroupNamesByActivityId(visibleActivityIds)
        val acceptedMentionsByActivityId = loadAcceptedMentionsByActivityId(visibleActivityIds)

        val items = entries
            .filter { it.authorId in friendIds || it.authorId == currentUserId }
            .filter { it.id !in viewedEntryIds }
            .mapNotNull { entry ->
                val author = profiles[entry.authorId] ?: return@mapNotNull null
                when (entry.entryType) {
                    "activity" -> {
                        val activity = entry.activityId?.let { activitiesById[it] } ?: return@mapNotNull null
                        HomeFeedItem(
                            entry = entry,
                            author = author,
                            activity = activity,
                            groupNames = groupNamesByActivityId[activity.id].orEmpty(),
                            acceptedMentions = acceptedMentionsByActivityId[activity.id].orEmpty()
                        )
                    }
                    "post" -> {
                        val post = entry.postId?.let { postsById[it] } ?: return@mapNotNull null
                        HomeFeedItem(
                            entry = entry,
                            author = author,
                            post = post,
                            media = mediaByPostId[post.id].orEmpty()
                        )
                    }
                    else -> null
                }
            }
            .sortedByDescending {
                it.entry.createdAt ?: it.activity?.realizadaEm ?: it.activity?.criadaEm ?: it.post?.createdAt.orEmpty()
            }

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

    private suspend fun loadGroupNamesByActivityId(activityIds: Set<String>): Map<String, List<String>> {
        if (activityIds.isEmpty()) return emptyMap()
        val client = SupabaseConfig.getClient()
        val items = runCatching {
            client.postgrest.from("activity_group_items")
                .select()
                .decodeList<ActivityGroupItem>()
                .filter { it.activityId in activityIds }
        }.getOrDefault(emptyList())
        if (items.isEmpty()) return emptyMap()

        val groupIds = items.map { it.groupId }.toSet()
        val groups = runCatching {
            client.postgrest.from("activity_groups")
                .select()
                .decodeList<ActivityGroup>()
                .filter { it.id in groupIds }
                .associateBy { it.id }
        }.getOrDefault(emptyMap())

        return items
            .groupBy { it.activityId }
            .mapValues { entry ->
                entry.value.mapNotNull { groups[it.groupId]?.name }.distinct().sorted()
            }
    }

    private suspend fun loadAcceptedMentionsByActivityId(activityIds: Set<String>): Map<String, List<Profile>> {
        if (activityIds.isEmpty()) return emptyMap()
        val client = SupabaseConfig.getClient()
        val mentions = runCatching {
            client.postgrest.from("activity_mentions")
                .select {
                    filter { eq("status", ActivityMention.STATUS_ACCEPTED) }
                }
                .decodeList<ActivityMention>()
                .filter { it.activityId in activityIds && it.showOnMentionedProfile }
        }.getOrDefault(emptyList())
        if (mentions.isEmpty()) return emptyMap()

        val mentionedUserIds = mentions.map { it.mentionedUserId }.toSet()
        val profiles = runCatching {
            client.postgrest.from("profiles")
                .select()
                .decodeList<Profile>()
                .filter { it.id in mentionedUserIds }
                .associateBy { it.id }
        }.getOrDefault(emptyMap())

        return mentions
            .groupBy { it.activityId }
            .mapValues { entry ->
                entry.value.mapNotNull { profiles[it.mentionedUserId] }
                    .distinctBy { it.id }
                    .sortedBy { it.name.lowercase() }
            }
    }

    private suspend fun uploadPostMedia(context: Context, uri: Uri, path: String): String {
        val originalFile = copyUriToCacheFile(context, uri)
        val compressedFile = ImageUtils.comprimirImagem(context, originalFile, maxDimensao = 1280)
        val bytes = compressedFile.readBytes()
        if (bytes.isEmpty()) throw Exception("Imagem invalida")
        SupabaseConfig.getClient().storage.from("post-media").upload(path, bytes) {
            upsert = true
        }
        return path
    }

    private fun copyUriToCacheFile(context: Context, uri: Uri): File {
        val file = File(context.cacheDir, "zenith_feed_${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw Exception("Nao foi possivel abrir a imagem")
        return file
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

    private fun Throwable.isDuplicateFeedView(): Boolean {
        val message = listOfNotNull(message, localizedMessage, cause?.message)
            .joinToString(" ")
            .lowercase()
        return "duplicate" in message ||
            "unique" in message ||
            "feed_entry_views_user_entry_unique" in message
    }
}
