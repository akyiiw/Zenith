package br.com.zenith.viewmodels.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.AppBlock
import br.com.zenith.ui.notifications.ZenithNotifier
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AppBlockViewModel : ViewModel() {
    private val _blocks = MutableStateFlow<List<AppBlock>>(emptyList())
    val blocks: StateFlow<List<AppBlock>> = _blocks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun load(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuário não autenticado")
                _blocks.value = client.postgrest.from("bloqueio_aplicativos")
                    .select {
                        filter { eq("id_usuario", userId) }
                    }
                    .decodeList<AppBlock>()
                    .sortedBy { it.startTime }
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao carregar bloqueios: ${e.localizedMessage}")
                _blocks.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun create(
        appName: String,
        startTime: String,
        endTime: String,
        blockedDays: String?,
        reminder: Boolean,
        context: Context
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuário não autenticado")
                client.postgrest.from("bloqueio_aplicativos").insert(
                    buildJsonObject {
                        put("id_usuario", userId)
                        put("nome_aplicativo", appName)
                        put("hora_inicio", normalizeTime(startTime))
                        put("hora_conclusao", normalizeTime(endTime))
                        put("status_bloqueio", true)
                        put("lembrete", reminder)
                        val cleanedDays = blockedDays?.trim()?.takeIf { it.isNotBlank() }
                        if (cleanedDays == null) put("dias_bloqueados", JsonNull)
                        else put("dias_bloqueados", cleanedDays)
                    }
                )
                ZenithNotifier.success("Bloqueio criado.")
                load(context)
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao criar bloqueio: ${e.localizedMessage}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun setActive(block: AppBlock, active: Boolean, context: Context) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                SupabaseConfig.getClient().postgrest.from("bloqueio_aplicativos").update(
                    buildJsonObject { put("status_bloqueio", active) }
                ) {
                    filter { eq("id_bloqueio", block.id) }
                }
                _blocks.value = _blocks.value.map {
                    if (it.id == block.id) it.copy(active = active) else it
                }
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao atualizar bloqueio: ${e.localizedMessage}")
                load(context)
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun delete(block: AppBlock, context: Context) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                SupabaseConfig.getClient().postgrest.from("bloqueio_aplicativos").delete {
                    filter { eq("id_bloqueio", block.id) }
                }
                _blocks.value = _blocks.value.filterNot { it.id == block.id }
                ZenithNotifier.success("Bloqueio removido.")
            } catch (e: Exception) {
                ZenithNotifier.error("Erro ao remover bloqueio: ${e.localizedMessage}")
                load(context)
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun normalizeTime(value: String): String {
        val parts = value.trim().split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        return "%02d:%02d:00".format(hour, minute)
    }
}
