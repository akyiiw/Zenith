package br.com.zenith.data.repositories

import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.PersonalGoal
import io.github.jan.supabase.postgrest.postgrest

class GoalsRepository {
    private val client = SupabaseConfig.getClient().postgrest

    suspend fun getActiveGoals(): List<PersonalGoal> {
        return client.from("personal_goals")
            .select {
                filter {
                    eq("is_active", true)
                }
            }.decodeList<PersonalGoal>()
    }

    suspend fun getAllGoals(): List<PersonalGoal> {
        return client.from("personal_goals").select().decodeList<PersonalGoal>()
    }

    suspend fun saveGoal(goal: PersonalGoal) {
        if (goal.id == null) {
            client.from("personal_goals").insert(goal)
        } else {
            client.from("personal_goals").update(goal) {
                filter {
                    eq("id", goal.id)
                }
            }
        }
    }

    suspend fun deleteGoal(id: String) {
        client.from("personal_goals").delete {
            filter {
                eq("id", id)
            }
        }
    }

    suspend fun toggleGoalActive(id: String, isActive: Boolean) {
        client.from("personal_goals").update(mapOf("is_active" to isActive)) {
            filter {
                eq("id", id)
            }
        }
    }
}
