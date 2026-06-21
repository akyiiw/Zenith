package br.com.zenith.viewmodels.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.Badge
import br.com.zenith.data.models.Atividade
import br.com.zenith.data.models.ChallengeAwardCalculator
import br.com.zenith.data.models.ChallengeAwardSummary
import br.com.zenith.data.models.Desafio
import br.com.zenith.data.models.DesafioParticipacao
import br.com.zenith.data.models.Medalha
import br.com.zenith.data.models.Profile
import br.com.zenith.data.models.Titulo
import br.com.zenith.data.models.UsuarioTitulo
import br.com.zenith.data.models.UsuarioMedalha
import br.com.zenith.data.models.UserBadgeEntitlement
import br.com.zenith.ui.notifications.ZenithNotifier
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.time.Duration
import java.time.OffsetDateTime
import br.com.zenith.utils.ImageUtils

data class UserStats(
    val medalhas: Int = 0,
    val desafios: Int = 0,
    val amigos: Int = 0,
    val conquistas: Int = 0
)

class UserViewModel : ViewModel() {
    private val _userState = MutableStateFlow<Profile?>(null)
    val userState: StateFlow<Profile?> = _userState.asStateFlow()

    private val _statsState = MutableStateFlow(UserStats())
    val statsState: StateFlow<UserStats> = _statsState.asStateFlow()

    private val _badgeState = MutableStateFlow<Badge?>(null)
    val badgeState: StateFlow<Badge?> = _badgeState.asStateFlow()

    private val _badgesDisponiveis = MutableStateFlow<List<Badge>>(emptyList())
    val badgesDisponiveis: StateFlow<List<Badge>> = _badgesDisponiveis.asStateFlow()

    private val _medalhas = MutableStateFlow<List<Medalha>>(emptyList())
    val medalhas: StateFlow<List<Medalha>> = _medalhas.asStateFlow()

    private val _challengeAwards = MutableStateFlow(ChallengeAwardSummary())
    val challengeAwards: StateFlow<ChallengeAwardSummary> = _challengeAwards.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _titulosDisponiveis = MutableStateFlow<List<Titulo>>(emptyList())
    val titulosDisponiveis: StateFlow<List<Titulo>> = _titulosDisponiveis.asStateFlow()

    @OptIn(ExperimentalCoilApi::class)
    fun atualizarPerfil(
        name: String,
        displayName: String,
        aboutMe: String?,
        pictureUri: Uri?,
        bannerUri: Uri?,
        bannerBlurRadius: Int,
        pictureFocusX: Float,
        pictureFocusY: Float,
        pictureZoom: Float,
        bannerFocusX: Float,
        bannerFocusY: Float,
        bannerZoom: Float,
        context: Context,
        onSucesso: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuário não autenticado")
                val currentProfile = _userState.value
                val nameChanged = currentProfile?.name != name
                val now = OffsetDateTime.now()

                if (nameChanged) {
                    currentProfile?.nameUpdatedAt
                        ?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
                        ?.let { lastUpdate ->
                            if (Duration.between(lastUpdate, now).toDays() < USERNAME_COOLDOWN_DAYS) {
                                throw Exception("Você só pode alterar o @usuário a cada 14 dias")
                            }
                        }
                }

                val uploadVersion = System.currentTimeMillis()
                val pictureHash = pictureUri?.let { uri ->
                    uploadProfileImage(
                        context = context,
                        uri = uri,
                        path = "avatars/$userId-$uploadVersion.jpg",
                        maxDimension = 400,
                        aspectRatio = 1f,
                        focusX = pictureFocusX,
                        focusY = pictureFocusY,
                        zoom = pictureZoom
                    )
                }

                val bannerHash = bannerUri?.let { uri ->
                    uploadProfileImage(
                        context = context,
                        uri = uri,
                        path = "banners/$userId-$uploadVersion.jpg",
                        maxDimension = 1440,
                        aspectRatio = PROFILE_BANNER_ASPECT_RATIO,
                        focusX = bannerFocusX,
                        focusY = bannerFocusY,
                        zoom = bannerZoom
                    )
                }

                client.postgrest.from("profiles").update(
                    buildJsonObject {
                        put("name", name)
                        put("displayName", displayName)
                        put("aboutMe", aboutMe)
                        put("banner_blur_radius", bannerBlurRadius.coerceIn(0, 24))
                        pictureHash?.let { put("pictureHash", it) }
                        bannerHash?.let { put("bannerHash", it) }
                        if (nameChanged) {
                            put("name_updated_at", now.toString())
                        }
                    }
                ) { filter { eq("id", userId) } }

                _userState.value = _userState.value?.copy(
                    name = name,
                    displayName = displayName,
                    aboutMe = aboutMe,
                    pictureHash = pictureHash ?: _userState.value?.pictureHash,
                    bannerHash = bannerHash ?: _userState.value?.bannerHash,
                    nameUpdatedAt = if (nameChanged) now.toString() else _userState.value?.nameUpdatedAt,
                    bannerBlurRadius = bannerBlurRadius.coerceIn(0, 24)
                )

                deleteProfileImageIfReplaced(currentProfile?.pictureHash, pictureHash)
                deleteProfileImageIfReplaced(currentProfile?.bannerHash, bannerHash)

                if (pictureHash != null || bannerHash != null) {
                    context.imageLoader.memoryCache?.clear()
                    context.imageLoader.diskCache?.clear()
                }

                onSucesso()
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao salvar: ${e.localizedMessage}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    private suspend fun deleteProfileImageIfReplaced(oldPath: String?, newPath: String?) {
        if (oldPath.isNullOrBlank() || newPath.isNullOrBlank() || oldPath == newPath) return
        runCatching {
            SupabaseConfig.getClient().storage.from("profiles").delete(oldPath)
        }
    }

    fun atualizarStatus(
        status: String?,
        context: Context
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuário não autenticado")
                val cleanedStatus = status?.trim()?.takeIf { it.isNotBlank() }

                client.postgrest.from("profiles").update(
                    buildJsonObject {
                        if (cleanedStatus == null) put("status", JsonNull)
                        else put("status", cleanedStatus)
                    }
                ) { filter { eq("id", userId) } }

                _userState.value = _userState.value?.copy(status = cleanedStatus)
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao salvar status: ${e.localizedMessage}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun atualizarVisibilidadePerfil(
        privado: Boolean,
        context: Context
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuário não autenticado")
                val visibility = if (privado) "privado" else "publico"

                client.postgrest.from("profiles").update(
                    buildJsonObject {
                        put("visibilidade_perfil", visibility)
                    }
                ) { filter { eq("id", userId) } }

                _userState.value = _userState.value?.copy(profileVisibility = visibility)
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao salvar privacidade: ${e.localizedMessage}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    private suspend fun uploadProfileImage(
        context: Context,
        uri: Uri,
        path: String,
        maxDimension: Int,
        aspectRatio: Float,
        focusX: Float,
        focusY: Float,
        zoom: Float
    ): String {
        val originalFile = copyUriToCacheFile(context, uri)
        val compressedFile = ImageUtils.recortarEComprimirImagem(
            context = context,
            fileOriginal = originalFile,
            maxDimensao = maxDimension,
            aspectRatio = aspectRatio,
            focusX = focusX,
            focusY = focusY,
            zoom = zoom
        )
        val bytes = compressedFile.readBytes()

        if (bytes.isEmpty()) {
            throw Exception("Imagem inválida")
        }

        SupabaseConfig.getClient().storage.from("profiles").upload(path, bytes) {
            upsert = true
        }

        return path
    }

    private fun copyUriToCacheFile(context: Context, uri: Uri): File {
        val file = File(context.cacheDir, "zenith_profile_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw Exception("Não foi possível abrir a imagem")
        return file
    }

    fun fetchUserProfile(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()

                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuário não autenticado")

                val profile = client.postgrest.from("profiles")
                    .select(columns = Columns.raw("*, titulos(*)"))  {
                        filter { eq("id", userId) }
                    }
                    .decodeSingle<Profile>()
                _userState.value = profile

                if (profile.badgeId == null) {
                    _badgeState.value = null
                } else {
                    val badge = client.postgrest.from("badge")
                        .select { filter { eq("id", profile.badgeId.toLong()) } }
                        .decodeSingle<Badge>()
                    _badgeState.value = badge
                }

                val desafios = client.postgrest.from("desafio_participacoes")
                    .select { filter { eq("user_id", userId) }; count(Count.EXACT) }
                    .countOrNull() ?: 0

                val amigosEnviados = client.postgrest.from("amizades")
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("status", "aceito")
                        }
                    }
                    .decodeList<br.com.zenith.data.models.Amizade>()
                    .map { it.friendId }

                val amigosRecebidos = client.postgrest.from("amizades")
                    .select {
                        filter {
                            eq("amigo_id", userId)
                            eq("status", "aceito")
                        }
                    }
                    .decodeList<br.com.zenith.data.models.Amizade>()
                    .map { it.userId }

                val amigos = (amigosEnviados + amigosRecebidos).distinct().size

                val conquistas = client.postgrest.from("conquistas")
                    .select { filter { eq("user_id", userId) }; count(Count.EXACT) }
                    .countOrNull() ?: 0

                val challengeParticipations = client.postgrest.from("desafio_participacoes")
                    .select()
                    .decodeList<DesafioParticipacao>()
                val challenges = client.postgrest.from("desafios")
                    .select()
                    .decodeList<Desafio>()
                val challengeActivities = client.postgrest.from("atividades")
                    .select(columns = Columns.raw("*, exercicios(*)"))
                    .decodeList<Atividade>()
                val challengeAwards = ChallengeAwardCalculator.buildSummary(
                    userId = userId,
                    challenges = challenges,
                    participations = challengeParticipations,
                    activities = challengeActivities
                )
                _challengeAwards.value = challengeAwards

                _statsState.value = UserStats(
                    medalhas = challengeAwards.total,
                    desafios = desafios.toInt(),
                    amigos = amigos,
                    conquistas = conquistas.toInt()
                )

            } catch (_: Exception) {
                _challengeAwards.value = ChallengeAwardSummary()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchMedalhas(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuário não autenticado")
                _medalhas.value = client.postgrest.from("usuario_medalhas")
                    .select(columns = Columns.raw("*, medalhas(*)")) {
                        filter { eq("id_usuario", userId) }
                    }
                    .decodeList<UsuarioMedalha>()
                    .mapNotNull { it.medalha }
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao carregar medalhas: ${e.localizedMessage}")
                _medalhas.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchTitulosDisponiveis(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id ?: return@launch

                val ids = client.postgrest.from("usuario_titulos")
                    .select { filter { eq("user_id", userId) } }
                    .decodeList<UsuarioTitulo>()
                    .map { it.tituloId }

                if (ids.isEmpty()) {
                    _titulosDisponiveis.value = emptyList()
                    return@launch
                }

                val todosOsTitulos = client.postgrest.from("titulos")
                    .select()
                    .decodeList<Titulo>()

                val titulos = todosOsTitulos.filter { it.id in ids }

                _titulosDisponiveis.value = titulos
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchBadgesDisponiveis(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                SupabaseConfig.init(context)
                val badges = SupabaseConfig.getClient().postgrest
                    .from("user_badge_entitlements")
                    .select(columns = Columns.raw("*, badge(*)"))
                    .decodeList<UserBadgeEntitlement>()
                    .mapNotNull { it.badge }
                    .distinctBy { it.id }
                    .sortedBy { it.id }

                _badgesDisponiveis.value = badges
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao carregar badges: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selecionarTitulo(tituloId: String, context: Context, onSucesso: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id ?: return@launch

                client.postgrest.from("profiles").update(
                    buildJsonObject { put("titulo_id", tituloId) }
                ) { filter { eq("id", userId) } }

                _userState.value = _userState.value?.copy(tituloId = tituloId)
                onSucesso()
            } catch (e: Exception) {
                ZenithNotifier.error("Erro: ${e.localizedMessage}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun selecionarBadge(badgeId: Int?, context: Context, onSucesso: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuário não autenticado")

                client.postgrest.from("profiles").update(
                    buildJsonObject {
                        if (badgeId == null) put("badge_id", JsonNull)
                        else put("badge_id", badgeId)
                    }
                ) { filter { eq("id", userId) } }

                val badge = badgeId?.let { selectedId ->
                    _badgesDisponiveis.value.firstOrNull { it.id == selectedId }
                        ?: client.postgrest.from("badge")
                            .select { filter { eq("id", selectedId.toLong()) } }
                            .decodeSingle<Badge>()
                }

                _badgeState.value = badge
                _userState.value = _userState.value?.copy(badgeId = badgeId)
                ZenithNotifier.success("Badge atualizado.")
                onSucesso()
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao selecionar badge: ${e.localizedMessage}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    companion object {
        const val USERNAME_COOLDOWN_DAYS = 14L
        const val PROFILE_BANNER_ASPECT_RATIO = 3.2f
    }
}
