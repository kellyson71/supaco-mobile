package io.github.kellyson71.supaco.data.repository

import io.github.kellyson71.supaco.data.local.TokenManager
import io.github.kellyson71.supaco.data.model.LoginRequest
import io.github.kellyson71.supaco.data.remote.NotStudentException
import io.github.kellyson71.supaco.data.remote.SuapApi
import io.github.kellyson71.supaco.data.session.SessionManager

class AuthRepository(
    private val suapApi: SuapApi,
    private val tokenManager: TokenManager,
    private val profileRepository: ProfileRepository,
    private val sessionManager: SessionManager,
) {
    suspend fun login(matricula: String, senha: String): Result<Unit> = runCatching {
        val tokens = suapApi.login(LoginRequest(matricula.trim(), senha))
        tokenManager.saveToken(tokens.access, tokens.refresh)
        val profile = runCatching { profileRepository.refresh() }
            .onFailure { tokenManager.clear() }
            .getOrThrow()
        if (!profile.isAluno) {
            tokenManager.clear()
            throw NotStudentException()
        }
        sessionManager.onLoggedIn()
    }

    suspend fun logout() = sessionManager.logout()
}
