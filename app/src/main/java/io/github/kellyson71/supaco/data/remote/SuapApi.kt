package io.github.kellyson71.supaco.data.remote

import io.github.kellyson71.supaco.data.model.BoletimItem
import io.github.kellyson71.supaco.data.model.DadosAlunoResponse
import io.github.kellyson71.supaco.data.model.LoginRequest
import io.github.kellyson71.supaco.data.model.PaginatedResponse
import io.github.kellyson71.supaco.data.model.PeriodoLetivo
import io.github.kellyson71.supaco.data.model.ProfileResponse
import io.github.kellyson71.supaco.data.model.RefreshRequest
import io.github.kellyson71.supaco.data.model.RefreshResponse
import io.github.kellyson71.supaco.data.model.Servidor
import io.github.kellyson71.supaco.data.model.TokenPairResponse
import io.github.kellyson71.supaco.data.model.TurmaVirtual
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SuapApi {

    @POST("/api/token/pair")
    suspend fun login(@Body request: LoginRequest): TokenPairResponse

    @GET("/api/rh/eu/")
    suspend fun getProfile(): ProfileResponse

    @GET("/api/ensino/meus-dados-aluno/")
    suspend fun getDadosAluno(): DadosAlunoResponse

    @GET("/api/ensino/meus-periodos-letivos/")
    suspend fun getPeriodosLetivos(@Query("page") page: Int = 1): PaginatedResponse<PeriodoLetivo>

    @GET("/api/ensino/meu-boletim/{ano}/{periodo}/")
    suspend fun getBoletim(
        @Path("ano") ano: Int,
        @Path("periodo") periodo: Int,
        @Query("page") page: Int = 1,
    ): PaginatedResponse<BoletimItem>

    @GET("/api/ensino/minhas-turmas-virtuais/{ano}/{periodo}/")
    suspend fun getTurmasVirtuais(
        @Path("ano") ano: Int,
        @Path("periodo") periodo: Int,
        @Query("page") page: Int = 1,
    ): PaginatedResponse<TurmaVirtual>

    @GET("/api/rh/servidores/")
    suspend fun buscarServidores(
        @Query("nome") nome: String?,
        @Query("campus") campus: String?,
        @Query("page") page: Int = 1,
    ): PaginatedResponse<Servidor>
}

/** Endpoint de refresh isolado: roda num cliente sem o [TokenAuthenticator]. */
interface SuapRefreshApi {
    @POST("/api/token/refresh")
    fun refresh(@Body request: RefreshRequest): retrofit2.Call<RefreshResponse>
}

/** Segue o `next` da paginação do SUAP até o fim (com teto de segurança). */
suspend fun <T> fetchAllPages(
    maxPages: Int = 20,
    page: suspend (Int) -> PaginatedResponse<T>,
): List<T> {
    val all = mutableListOf<T>()
    var current = 1
    while (current <= maxPages) {
        val response = page(current)
        all += response.results
        if (response.next == null || response.results.isEmpty()) break
        current++
    }
    return all
}
