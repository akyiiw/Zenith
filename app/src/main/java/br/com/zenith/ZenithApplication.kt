package br.com.zenith

import android.app.Application
import br.com.zenith.data.SupabaseConfig

class ZenithApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        SupabaseConfig.init(this)
    }
}