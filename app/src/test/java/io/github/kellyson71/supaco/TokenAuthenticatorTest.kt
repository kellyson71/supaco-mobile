package io.github.kellyson71.supaco

import io.github.kellyson71.supaco.data.local.TokenStore
import io.github.kellyson71.supaco.data.remote.AuthInterceptor
import io.github.kellyson71.supaco.data.remote.SuapRefreshApi
import io.github.kellyson71.supaco.data.remote.TokenAuthenticator
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TokenAuthenticatorTest {

    private class FakeTokens(var access: String?, var refresh: String?) : TokenStore {
        override fun saveToken(access: String, refresh: String) { this.access = access; this.refresh = refresh }
        override fun getAccessToken() = access
        override fun getRefreshToken() = refresh
        override fun clear() { access = null; refresh = null }
    }

    private lateinit var server: MockWebServer
    private val refreshCalls = AtomicInteger()
    private var refreshResponse: MockResponse = MockResponse()
    private var expired = false

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/api/token/refresh" -> {
                    refreshCalls.incrementAndGet()
                    Thread.sleep(100) // dá tempo de outras requisições baterem no 401
                    refreshResponse
                }
                "/api/token/pair" -> MockResponse().setResponseCode(401)
                else -> if (request.getHeader("Authorization") == "Bearer novo") {
                    MockResponse().setBody("ok")
                } else MockResponse().setResponseCode(401)
            }
        }
        server.start()
    }

    @After
    fun tearDown() = server.shutdown()

    private fun client(tokens: FakeTokens): OkHttpClient {
        val refreshApi = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SuapRefreshApi::class.java)
        return OkHttpClient.Builder()
            .authenticator(TokenAuthenticator(tokens, refreshApi) { expired = true })
            .addInterceptor(AuthInterceptor(tokens))
            .build()
    }

    private fun get(client: OkHttpClient, path: String = "/api/rh/eu/") =
        client.newCall(Request.Builder().url(server.url(path)).build()).execute()

    @Test
    fun `renova o token e repete a requisicao`() {
        refreshResponse = MockResponse().setBody("""{"access": "novo", "refresh": "r2"}""")
        val tokens = FakeTokens("velho", "r1")

        get(client(tokens)).use { assertEquals(200, it.code) }
        assertEquals("novo", tokens.access)
        assertEquals("r2", tokens.refresh)
        assertFalse(expired)
    }

    @Test
    fun `mantem o refresh antigo quando o SUAP nao manda outro`() {
        refreshResponse = MockResponse().setBody("""{"access": "novo"}""")
        val tokens = FakeTokens("velho", "r1")
        get(client(tokens)).use { assertEquals(200, it.code) }
        assertEquals("r1", tokens.refresh)
    }

    @Test
    fun `refresh recusado expira a sessao`() {
        refreshResponse = MockResponse().setResponseCode(401)
        val tokens = FakeTokens("velho", "r1")
        get(client(tokens)).use { assertEquals(401, it.code) }
        assertTrue(expired)
    }

    @Test
    fun `refresh com access nulo expira a sessao`() {
        refreshResponse = MockResponse().setBody("""{"access": null, "refresh": "r1"}""")
        get(client(FakeTokens("velho", "r1"))).use { assertEquals(401, it.code) }
        assertTrue(expired)
    }

    @Test
    fun `SUAP fora do ar no refresh nao derruba a sessao`() {
        refreshResponse = MockResponse().setResponseCode(503)
        val tokens = FakeTokens("velho", "r1")
        get(client(tokens)).use { assertEquals(401, it.code) }
        assertFalse(expired)
        assertEquals("r1", tokens.refresh)
    }

    @Test
    fun `senha errada no login nao tenta renovar`() {
        val tokens = FakeTokens(null, null)
        get(client(tokens), "/api/token/pair").use { assertEquals(401, it.code) }
        assertEquals(0, refreshCalls.get())
        assertFalse(expired)
        assertNull(tokens.access)
    }

    @Test
    fun `varios 401 simultaneos fazem um unico refresh`() {
        refreshResponse = MockResponse().setBody("""{"access": "novo", "refresh": "r2"}""")
        val tokens = FakeTokens("velho", "r1")
        val c = client(tokens)
        val pool = Executors.newFixedThreadPool(4)
        val codes = (1..4).map { pool.submit<Int> { get(c).use { it.code } } }.map { it.get(10, TimeUnit.SECONDS) }
        pool.shutdown()

        assertEquals(listOf(200, 200, 200, 200), codes)
        assertEquals(1, refreshCalls.get())
    }
}
