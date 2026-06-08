package br.com.zenith.viewmodels.app

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zenith.data.SupabaseConfig
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun registrarUsuario(
        emailInput: String,
        senhaInput: String,
        context: Context,
        onSucesso: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                SupabaseConfig.getClient().auth.signUpWith(Email) {
                    email = emailInput
                    password = senhaInput
                }
                Toast.makeText(context, "Cadastro realizado!", Toast.LENGTH_LONG).show()
                onSucesso()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro no cadastro: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logarUsuario(
        emailInput: String,
        senhaInput: String,
        context: Context,
        onSucesso: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                SupabaseConfig.getClient().auth.signInWith(Email) {
                    email = emailInput
                    password = senhaInput
                }
                Toast.makeText(context, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                onSucesso()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro no login: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sairDaConta(
        context: Context,
        onSucesso: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                SupabaseConfig.getClient().auth.signOut()
                Toast.makeText(context, "Você saiu da conta", Toast.LENGTH_SHORT).show()
                onSucesso()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao sair: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun salvarPerfil(
        name: String,
        displayName: String,
        photoUri: Uri?,
        context: Context,
        onSucesso: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                client.auth.refreshCurrentSession()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuário não autenticado")
                val email = client.auth.currentUserOrNull()?.email
                    ?: throw Exception("Email não encontrado")

                val pictureHash: String? = try {
                    photoUri?.let { uri ->
                        val bytes = context.contentResolver
                            .openInputStream(uri)
                            ?.readBytes()

                        if (bytes == null || bytes.isEmpty()) {
                            null
                        } else {
                            val path = "avatars/$userId.jpg"
                            client.storage.from("profiles").upload(path, bytes) {
                                upsert = true
                            }
                            path
                        }
                    }
                } catch (_: Exception) {
                    null
                }

                val perfil = buildJsonObject {
                    put("id", userId)
                    put("name", name)
                    put("displayName", displayName)
                    put("email", email)
                    put("isPremium", false)
                    put("accountStatus", true)
                    pictureHash?.let { put("pictureHash", it) }
                }

                try {
                    client.postgrest.from("profiles").insert(perfil)
                } catch (_: Exception) {
                    client.postgrest.from("profiles").update(
                        buildJsonObject {
                            put("name", name)
                            put("displayName", displayName)
                            pictureHash?.let { put("pictureHash", it) }
                        }
                    ) {
                        filter { eq("id", userId) }
                    }
                }

                onSucesso()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao salvar perfil: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
