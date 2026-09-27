package io.github.kellyson71.supaco

import android.app.Application
import io.github.kellyson71.supaco.data.local.SettingsManager
import io.github.kellyson71.supaco.di.appModule
import io.github.kellyson71.supaco.notifications.FaltasWorker
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SupacoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val koin = startKoin {
            androidContext(this@SupacoApplication)
            modules(appModule)
        }.koin

        if (koin.get<SettingsManager>().notificationsEnabled.value) {
            FaltasWorker.schedule(this)
        }
    }
}
