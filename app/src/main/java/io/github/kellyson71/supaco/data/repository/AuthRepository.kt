package io.github.kellyson71.supaco.data.repository

import io.github.kellyson71.supaco.data.local.AppDatabase
import io.github.kellyson71.supaco.data.local.TokenManager
import io.github.kellyson71.supaco.data.model.LoginRequest
import io.github.kellyson71.supaco.data.remote.SuapApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val suapApi: SuapApi,
    private val tokenManager: TokenManager,
    private val appDatabase: AppDatabase
) {
    suspend fun login(matricula: String, senha: String): Result<Unit> {
        return try {
            val response = suapApi.login(LoginRequest(matricula, senha))
            tokenManager.saveToken(response.access, response.refresh ?: "")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        tokenManager.clear()
        withContext(Dispatchers.IO) {
            appDatabase.clearAllTables()
        }
    }

    fun isLoggedIn(): Boolean {
        return tokenManager.getAccessToken() != null
    }
}
