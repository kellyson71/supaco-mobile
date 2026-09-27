package io.github.kellyson71.supaco.di

import androidx.room.Room
import io.github.kellyson71.supaco.data.local.AppDatabase
import io.github.kellyson71.supaco.data.local.TokenManager
import io.github.kellyson71.supaco.data.remote.AuthInterceptor
import io.github.kellyson71.supaco.data.remote.TokenAuthenticator
import io.github.kellyson71.supaco.data.remote.SuapApi
import io.github.kellyson71.supaco.data.repository.AcademicRepository
import io.github.kellyson71.supaco.data.repository.AuthRepository
import io.github.kellyson71.supaco.data.repository.ProfileRepository
import io.github.kellyson71.supaco.ui.auth.AuthViewModel
import io.github.kellyson71.supaco.ui.dashboard.DashboardViewModel
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit

val appModule = module {

    single { TokenManager(androidContext()) }
    single { io.github.kellyson71.supaco.data.local.SettingsManager(androidContext()) }
    single { io.github.kellyson71.supaco.data.local.FaltasHistory(androidContext()) }
    single { AuthInterceptor(get()) }
    single { TokenAuthenticator(get()) }
    
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "supaco-db"
        ).fallbackToDestructiveMigration().build()
    }
    
    single { get<AppDatabase>().profileDao() }
    single { get<AppDatabase>().boletimDao() }
    single { get<AppDatabase>().horarioDao() }
    
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    single {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .authenticator(get<TokenAuthenticator>())
            .addInterceptor(get<AuthInterceptor>())
            .addInterceptor(loggingInterceptor)
            .build()
    }

    single {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl("https://suap.ifrn.edu.br")
            .client(get())
            .addConverterFactory(get<Json>().asConverterFactory(contentType))
            .build()
    }

    single { get<Retrofit>().create(SuapApi::class.java) }

    single { AuthRepository(get(), get(), get()) }
    single { ProfileRepository(get(), get()) }
    single { AcademicRepository(get(), get(), get(), androidContext(), get()) }

    viewModel { AuthViewModel(get()) }
    viewModel { DashboardViewModel(get(), get(), get(), get()) }
}
