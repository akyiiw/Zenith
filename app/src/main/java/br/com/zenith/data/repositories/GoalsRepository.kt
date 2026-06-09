package br.com.zenith.data.repositories

import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.PersonalGoal
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class GoalsRepository {
    private val supabase
        get() = SupabaseConfig.getClient()

    private val goalsTable
        get() = supabase.postgrest.from("personal_goals")

    suspend fun getActiveGoals(): List<PersonalGoal> {
        val userId = currentUserId()
        return goalsTable
            .select {
                filter {
                    eq("user_id", userId)
                    eq("is_active", true)
                }
            }.decodeList<PersonalGoal>()
    }

    suspend fun getAllGoals(): List<PersonalGoal> {
        val userId = currentUserId()
        return goalsTable
            .select {
                filter {
                    eq("user_id", userId)
                }
            }.decodeList<PersonalGoal>()
    }

    suspend fun saveGoal(goal: PersonalGoal) {
        val userId = goal.userId.ifBlank { currentUserId() }
        val payload = buildJsonObject {
            put("user_id", userId)
            put("title", goal.title)
            put("metric", goal.metric)
            put("period", goal.period)
            put("target_value", goal.targetValue)
            put("is_active", goal.isActive)
        }

        if (goal.id == null) {
            goalsTable.insert(payload)
        } else {
            goalsTable.update(payload) {
                filter {
                    eq("id", goal.id)
                    eq("user_id", userId)
                }
            }
        }
    }

    suspend fun deleteGoal(id: String) {
        goalsTable.delete {
            filter {
                eq("id", id)
                eq("user_id", currentUserId())
            }
        }
    }

    suspend fun toggleGoalActive(id: String, isActive: Boolean) {
        goalsTable.update(
            buildJsonObject {
                put("is_active", isActive)
            }
        ) {
            filter {
                eq("id", id)
                eq("user_id", currentUserId())
            }
        }
    }

    private fun currentUserId(): String {
        return supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("User must be authenticated to manage goals.")
    }
}
