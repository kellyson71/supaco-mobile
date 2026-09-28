package io.github.kellyson71.supaco.data.remote

import io.github.kellyson71.supaco.data.local.TokenStore
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenManager: TokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val accessToken = tokenManager.getAccessToken()

        if (accessToken == null ||
            originalRequest.isTokenEndpoint() ||
            originalRequest.header("Authorization") != null
        ) {
            return chain.proceed(originalRequest)
        }

        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}

internal fun okhttp3.Request.isTokenEndpoint(): Boolean = url.encodedPath.startsWith("/api/token/")
