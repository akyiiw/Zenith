package br.com.zenith.data

import android.content.Context
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import java.util.Properties

object SupabaseConfig {
    private var client: SupabaseClient? = null
    fun init(context: Context) {
        if (client == null) {
            try {
                val props = Properties()
                context.assets.open("properties.env").use { inputStream ->
                    props.load(inputStream)
                }

                val url = props.getProperty("SUPABASE_URL")
                val key = props.getProperty("ANON_KEY")

                client = createSupabaseClient(url, key) {
                    install(Postgrest)
                    install(Auth)
                    install(Storage)
                }
            } catch (_: Exception) {
                client = null
            }
        }
    }

    fun getClient(): SupabaseClient {
        return client ?: throw IllegalStateException("SupabaseConfig must be initialized with init(context) before use.")
    }
}
