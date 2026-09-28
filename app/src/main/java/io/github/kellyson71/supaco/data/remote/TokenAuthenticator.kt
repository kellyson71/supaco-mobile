package io.github.kellyson71.supaco.data.remote

import io.github.kellyson71.supaco.data.local.TokenStore
import io.github.kellyson71.supaco.data.model.RefreshRequest
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException

/**
 * Renova o access token quando o SUAP responde 401.
 *
 * - Sincronizado: várias requisições com 401 ao mesmo tempo disparam um único refresh;
 *   as demais reaproveitam o token novo.
 * - Refresh recusado → [onSessionExpired] (SessionManager.expire), a UI volta ao login.
 * - Falha de rede no refresh → a sessão é mantida; só esta requisição falha.
 */
class TokenAuthenticator(
    private val tokenManager: TokenStore,
    private val refreshApi: SuapRefreshApi,
    private val onSessionExpired: () -> Unit,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val request = response.request
        // Login com senha errada também volta 401 — não há o que renovar.
        if (request.isTokenEndpoint()) return null
        // Já tentamos com um token renovado e ainda deu 401: desiste.
        if (response.priorResponse != null) return null

        val sentToken = request.header("Authorization")?.removePrefix("Bearer ")

        synchronized(this) {
            val current = tokenManager.getAccessToken()
            if (current != null && current != sentToken) {
                return request.withToken(current)
            }

            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken == null) {
                onSessionExpired()
                return null
            }

            val refreshResponse = try {
                refreshApi.refresh(RefreshRequest(refreshToken)).execute()
            } catch (e: IOException) {
                return null
            } catch (e: RuntimeException) {
                // Resposta em formato inesperado — trata como refresh recusado.
                onSessionExpired()
                return null
            }

            if (!refreshResponse.isSuccessful) {
                // 4xx = refresh inválido/expirado; 5xx = SUAP instável, tenta de novo depois
                if (refreshResponse.code() in 400..499) onSessionExpired()
                return null
            }
            val access = refreshResponse.body()?.access
            if (access.isNullOrBlank()) {
                onSessionExpired()
                return null
            }

            // O SUAP nem sempre devolve um refresh novo; mantém o anterior nesse caso.
            val newRefresh = refreshResponse.body()?.refresh?.takeIf { it.isNotBlank() } ?: refreshToken
            tokenManager.saveToken(access, newRefresh)
            return request.withToken(access)
        }
    }

    private fun Request.withToken(token: String): Request =
        newBuilder().header("Authorization", "Bearer $token").build()
}
