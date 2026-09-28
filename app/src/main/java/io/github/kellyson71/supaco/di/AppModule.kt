package io.github.kellyson71.supaco.di

import androidx.room.Room
import io.github.kellyson71.supaco.BuildConfig
import io.github.kellyson71.supaco.data.local.AppDatabase
import io.github.kellyson71.supaco.data.local.CacheStore
import io.github.kellyson71.supaco.data.local.FaltasHistory
import io.github.kellyson71.supaco.data.local.SettingsManager
import io.github.kellyson71.supaco.data.local.TokenManager
import io.github.kellyson71.supaco.data.local.TokenStore
import io.github.kellyson71.supaco.data.remote.AuthInterceptor
import io.github.kellyson71.supaco.data.remote.SuapApi
import io.github.kellyson71.supaco.data.remote.SuapRefreshApi
import io.github.kellyson71.supaco.data.remote.TokenAuthenticator
import io.github.kellyson71.supaco.data.repository.AcademicRepository
import io.github.kellyson71.supaco.data.repository.AuthRepository
import io.github.kellyson71.supaco.data.repository.ProfileRepository
import io.github.kellyson71.supaco.data.session.SessionManager
import io.github.kellyson71.supaco.ui.auth.AuthViewModel
import io.github.kellyson71.supaco.ui.dashboard.DashboardViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

private const val SUAP_BASE_URL = "https://suap.ifrn.edu.br"
private val REFRESH_CLIENT = named("refresh")

val appModule = module {

    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single { TokenManager(androidContext()) } bind TokenStore::class
    single { SettingsManager(androidContext()) }
    single { CacheStore(androidContext()) }
    single { FaltasHistory(androidContext()) }

    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "supaco-db")
            // O banco é só cache do SUAP: numa mudança de schema basta baixar de novo.
            .fallbackToDestructiveMigration()
            .build()
    }
    single { get<AppDatabase>().profileDao() }
    single { get<AppDatabase>().boletimDao() }
    single { get<AppDatabase>().horarioDao() }

    single { SessionManager(androidContext(), get(), get(), get(), get(), get(), get()) }

    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            explicitNulls = false
        }
    }

    // Cliente base: timeouts e, só em debug, log de linha de status (nunca corpo:
    // o corpo do login contém a senha).
    single(named("base")) {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                            redactHeader("Authorization")
                        }
                    )
                }
            }
            .build()
    }

    single(REFRESH_CLIENT) {
        Retrofit.Builder()
            .baseUrl(SUAP_BASE_URL)
            .client(get(named("base")))
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SuapRefreshApi::class.java)
    }

    single {
        TokenAuthenticator(
            tokenManager = get<TokenManager>(),
            refreshApi = get(REFRESH_CLIENT),
            onSessionExpired = { get<SessionManager>().expire() },
        )
    }

    single {
        get<OkHttpClient>(named("base")).newBuilder()
            .authenticator(get<TokenAuthenticator>())
            .addInterceptor(AuthInterceptor(get()))
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl(SUAP_BASE_URL)
            .client(get())
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SuapApi::class.java)
    }

    single { ProfileRepository(get(), get()) }
    single { AuthRepository(get(), get(), get(), get()) }
    single { AcademicRepository(get(), get(), get(), get(), androidContext(), get()) }

    viewModel { AuthViewModel(get(), get()) }
    viewModel { DashboardViewModel(get(), get(), get(), get(), get()) }
}
