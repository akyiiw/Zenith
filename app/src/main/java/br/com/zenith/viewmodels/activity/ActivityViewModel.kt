package br.com.zenith.viewmodels.activity

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.Atividade
import br.com.zenith.data.models.Exercicio
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ActivityViewModel : ViewModel() {

    private val _atividades = MutableStateFlow<List<Atividade>>(value = emptyList())
    val atividades: StateFlow<List<Atividade>> = _atividades
    private val _exercicios = MutableStateFlow<List<Exercicio>>(emptyList())
    val exercicios: StateFlow<List<Exercicio>> = _exercicios

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    fun fetchExercicios(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                _exercicios.value = client.postgrest
                    .from("exercicios")
                    .select()
                    .decodeList<Exercicio>()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao carregar exercícios", Toast.LENGTH_SHORT).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchAtividades(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                _atividades.value = client.postgrest
                    .from(table = "atividades")
                    .select(columns = Columns.raw("*, exercicios(*)")) {
                        filter {
                            eq("user_id", client.auth.currentUserOrNull()?.id ?: "")
                        }
                    }
                    .decodeList<Atividade>()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao carregar atividades", Toast.LENGTH_SHORT).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registrarAtividade(
        exercicioId: String,
        valor: Double,
        verificada: Boolean,
        duracaoMin: Int?,
        titulo: String?,
        data: String,
        nota: Int?,
        intensidade: String?,
        humor: String?,
        fotoUri: Uri?,
        rota: String? = null,
        desafioId: String? = null,
        passos: Int? = null,
        distanciaBruta: Double? = null,
        gpsAccuracyMedia: Double? = null,
        gpsPontosAceitos: Int? = null,
        gpsPontosRejeitados: Int? = null,
        gpsQualidade: String? = null,
        context: Context,
        onSucesso: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuário não autenticado")

                // Parse da data
                val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                val localDateTime = java.time.LocalDateTime.parse(data, formatter)
                val realizadaEm = localDateTime
                    .atOffset(java.time.ZoneOffset.systemDefault().rules.getOffset(localDateTime))
                    .toString()

                client.postgrest.from("atividades").insert(
                    buildJsonObject {
                        put("user_id", userId)
                        put("exercicio_id", exercicioId)
                        put("valor", valor)
                        put("verificada", verificada)
                        put("realizada_em", realizadaEm)
                        duracaoMin?.let { put("duracao_min", it) }
                        titulo?.let { put("titulo", it) }
                        nota?.let { put("nota", it) }
                        intensidade?.let { put("intensidade", it) }
                        humor?.let { put("humor", it) }
                        rota?.let { put("rota", it) }
                        desafioId?.takeIf { it.isNotBlank() }?.let { put("desafio_id", it) }
                        passos?.let { put("passos", it) }
                        distanciaBruta?.let { put("distancia_bruta", it) }
                        gpsAccuracyMedia?.let { put("gps_accuracy_media", it) }
                        gpsPontosAceitos?.let { put("gps_pontos_aceitos", it) }
                        gpsPontosRejeitados?.let { put("gps_pontos_rejeitados", it) }
                        gpsQualidade?.let { put("gps_qualidade", it) }
                    }
                )

                Toast.makeText(context, "Atividade registrada!", Toast.LENGTH_SHORT).show()
                onSucesso()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun editarAtividade(
        atividadeId: String,
        exercicioId: String,
        valor: Double,
        duracaoMin: Int?,
        titulo: String?,
        data: String,
        nota: Int?,
        intensidade: String?,
        humor: String?,
        context: Context,
        onSucesso: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                val localDateTime = java.time.LocalDateTime.parse(data, formatter)
                val realizadaEm = localDateTime
                    .atOffset(java.time.ZoneOffset.systemDefault().rules.getOffset(localDateTime))
                    .toString()

                SupabaseConfig.getClient().postgrest.from("atividades").update(
                    buildJsonObject {
                        put("exercicio_id", exercicioId)
                        put("valor", valor)
                        put("duracao_min", duracaoMin)
                        put("titulo", titulo)
                        put("realizada_em", realizadaEm)
                        put("nota", nota)
                        put("intensidade", intensidade)
                        put("humor", humor)
                    }
                ) {
                    filter {
                        eq("id", atividadeId)
                        eq("verificada", false)
                    }
                }

                Toast.makeText(context, "Atividade atualizada!", Toast.LENGTH_SHORT).show()
                fetchAtividades(context)
                onSucesso()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                _isSaving.value = false
            }
        }
    }
}
