package br.com.zenith.viewmodels.sleep

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zenith.data.SupabaseConfig
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SleepRecordViewModel : ViewModel() {
    fun addManualSleep(
        hours: Float,
        quality: Int?,
        context: Context,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                SupabaseConfig.init(context)
                val client = SupabaseConfig.getClient()
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuário não autenticado")
                val durationMinutes = (hours.coerceIn(0.5f, 16f) * 60).roundToInt()
                val zone = ZoneId.systemDefault()
                val endedAt = LocalDate.now().atTime(7, 0)
                val startedAt = endedAt.minusMinutes(durationMinutes.toLong())

                client.postgrest.from("sleep_records").insert(
                    buildJsonObject {
                        put("user_id", userId)
                        put("started_at", startedAt.atZone(zone).toOffsetDateTime().toString())
                        put("ended_at", endedAt.atZone(zone).toOffsetDateTime().toString())
                        put("duration_minutes", durationMinutes)
                        put("source", "manual")
                        quality?.let { put("quality", it) }
                    }
                )

                Toast.makeText(context, "Sono registrado", Toast.LENGTH_SHORT).show()
                onSuccess()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Erro ao registrar sono: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
