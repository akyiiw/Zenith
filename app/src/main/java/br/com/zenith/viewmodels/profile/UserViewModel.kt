package br.com.zenith.viewmodels.profile

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.Badge
import br.com.zenith.data.models.Profile
import br.com.zenith.data.models.Titulo
import br.com.zenith.data.models.UsuarioTitulo
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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _titulosDisponiveis = MutableStateFlow<List<Titulo>>(emptyList())
    val titulosDisponiveis: StateFlow<List<Titulo>> = _titulosDisponiveis.asStateFlow()

    fun atualizarPerfil(
        name: String,
        displayName: String,
        aboutMe: String?,
        pictureUri: Uri?,
        bannerUri: Uri?,
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

                val pictureHash = pictureUri?.let { uri ->
                    uploadProfileImage(
                        context = context,
                        uri = uri,
                        path = "avatars/$userId.jpg",
                        maxDimension = 400
                    )
                }

                val bannerHash = bannerUri?.let { uri ->
                    uploadProfileImage(
                        context = context,
                        uri = uri,
                        path = "banners/$userId.jpg",
                        maxDimension = 1080
                    )
                }

                client.postgrest.from("profiles").update(
                    buildJsonObject {
                        put("name", name)
                        put("displayName", displayName)
                        put("aboutMe", aboutMe)
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
                    nameUpdatedAt = if (nameChanged) now.toString() else _userState.value?.nameUpdatedAt
                )

                onSucesso()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao salvar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                _isSaving.value = false
            }
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
                Toast.makeText(context, "Erro ao salvar status: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                _isSaving.value = false
            }
        }
    }

    private suspend fun uploadProfileImage(
        context: Context,
        uri: Uri,
        path: String,
        maxDimension: Int
    ): String {
        val originalFile = copyUriToCacheFile(context, uri)
        val compressedFile = ImageUtils.comprimirImagem(context, originalFile, maxDimension)
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

                profile.badgeId?.let { badgeId ->
                    val badge = client.postgrest.from("badge")
                        .select { filter { eq("id", badgeId.toLong()) } }
                        .decodeSingle<Badge>()
                    _badgeState.value = badge
                }

                val medalhas = client.postgrest.from("conquistas")
                    .select { filter { eq("user_id", userId) }; count(Count.EXACT) }
                    .countOrNull() ?: 0

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

                _statsState.value = UserStats(
                    medalhas = medalhas.toInt(),
                    desafios = desafios.toInt(),
                    amigos = amigos,
                    conquistas = conquistas.toInt()
                )

            } catch (_: Exception) {
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
                Toast.makeText(context, "Erro: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                _isSaving.value = false
            }
        }
    }

    companion object {
        const val USERNAME_COOLDOWN_DAYS = 14L
    }
}
