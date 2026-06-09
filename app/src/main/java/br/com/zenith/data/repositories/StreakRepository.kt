package br.com.zenith.data.repositories

import br.com.zenith.data.SupabaseConfig
import io.github.jan.supabase.postgrest.postgrest
import java.time.LocalDate
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class StreakRepository {
    suspend fun recordActivity(activityDate: LocalDate): Int {
        val result = SupabaseConfig.getClient().postgrest.rpc(
            function = "record_activity_streak",
            parameters = buildJsonObject {
                put("p_activity_date", activityDate.toString())
            }
        )

        return result.decodeAs<Int>()
    }
}
