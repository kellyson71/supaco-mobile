package com.example.supacomobile.data.remote

import com.example.supacomobile.data.local.TokenManager
import com.example.supacomobile.data.model.RefreshRequest
import com.example.supacomobile.data.model.TokenResponse
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TokenAuthenticator(
    private val tokenManager: TokenManager
) : Authenticator, KoinComponent {

    private val json: Json by inject()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Se a própria requisição de refresh falhou (código 401), não tentamos novamente para evitar loop
        if (response.request.url.pathSegments.contains("refresh")) {
            tokenManager.clear()
            return null
        }

        val refreshToken = tokenManager.getRefreshToken() ?: return null

        val client = OkHttpClient()
        val requestBody = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refreshToken))
            .toRequestBody("application/json".toMediaType())

        val refreshRequest = Request.Builder()
            .url("https://suap.ifrn.edu.br/api/token/refresh")
            .post(requestBody)
            .build()

        return try {
            val refreshResponse = client.newCall(refreshRequest).execute()
            if (refreshResponse.isSuccessful) {
                val responseBody = refreshResponse.body?.string()
                if (responseBody != null) {
                    val newTokens = json.decodeFromString(TokenResponse.serializer(), responseBody)
                    // Às vezes o backend retorna um novo refresh, outras não. Usamos fallback para o antigo.
                    val safeRefresh = newTokens.refresh ?: refreshToken 
                    tokenManager.saveToken(newTokens.access, safeRefresh)
                    
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${newTokens.access}")
                        .build()
                } else {
                    tokenManager.clear()
                    null
                }
            } else {
                tokenManager.clear()
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
