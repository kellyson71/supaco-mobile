package io.github.kellyson71.supaco.data.remote

import io.github.kellyson71.supaco.data.model.BoletimItem
import io.github.kellyson71.supaco.data.model.LoginRequest
import io.github.kellyson71.supaco.data.model.PaginatedResponse
import io.github.kellyson71.supaco.data.model.PeriodoLetivo
import io.github.kellyson71.supaco.data.model.ProfileResponse
import io.github.kellyson71.supaco.data.model.RefreshRequest
import io.github.kellyson71.supaco.data.model.TokenResponse
import io.github.kellyson71.supaco.data.model.TurmaVirtual
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SuapApi {

    @POST("/api/token/pair")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @GET("/api/rh/eu/")
    suspend fun getProfile(): ProfileResponse

    @GET("/api/ensino/meus-periodos-letivos/")
    suspend fun getPeriodosLetivos(): PaginatedResponse<PeriodoLetivo>

    @GET("/api/ensino/meu-boletim/{ano}/{periodo}/")
    suspend fun getBoletim(
        @Path("ano") ano: Int,
        @Path("periodo") periodo: Int,
    ): PaginatedResponse<BoletimItem>

    @GET("/api/ensino/minhas-turmas-virtuais/{ano}/{periodo}/")
    suspend fun getTurmasVirtuais(
        @Path("ano") ano: Int,
        @Path("periodo") periodo: Int,
    ): PaginatedResponse<TurmaVirtual>

    @POST("/api/token/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): TokenResponse

    @GET("/api/rh/servidores/")
    suspend fun buscarServidores(
        @retrofit2.http.Query("nome") nome: String?,
        @retrofit2.http.Query("campus") campus: String?,
        @retrofit2.http.Query("page") page: Int = 1
    ): PaginatedResponse<io.github.kellyson71.supaco.data.model.Servidor>
}

