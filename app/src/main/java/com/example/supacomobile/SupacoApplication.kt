package com.example.supacomobile

import android.app.Application
import com.example.supacomobile.data.local.SettingsManager
import com.example.supacomobile.di.appModule
import com.example.supacomobile.notifications.FaltasWorker
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
