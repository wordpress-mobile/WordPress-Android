package org.wordpress.android.ui.main.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.utils.AppLogWrapper
import java.io.IOException
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GravatarEditorTargetResolverTest : BaseUnitTest() {
    @Mock
    private lateinit var clientFactory: GravatarProbeClientFactory

    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    @Mock
    private lateinit var client: Call.Factory

    private lateinit var resolver: GravatarEditorTargetResolver

    @Before
    fun setUp() {
        resolver = GravatarEditorTargetResolver(clientFactory, appLogWrapper, testDispatcher())
    }

    @Test
    fun `returns quick editor when Gravatar answers successfully`() = test {
        givenResponse(code = 200, body = """{"hash":"abc"}""", contentType = "application/json")

        assertThat(resolver.resolve(TOKEN)).isEqualTo(GravatarEditorTarget.QUICK_EDITOR)
    }

    @Test
    fun `returns web editor on the edge 403 that strands the user`() = test {
        givenResponse(code = 403, body = NGINX_403, contentType = "text/html")

        assertThat(resolver.resolve(TOKEN)).isEqualTo(GravatarEditorTarget.WEB_EDITOR)
    }

    @Test
    fun `returns web editor for other refusals the SDK cannot recover from`() = test {
        listOf(404, 429, 500).forEach { code ->
            givenResponse(code = code, body = "", contentType = "application/json")

            assertThat(resolver.resolve(TOKEN)).isEqualTo(GravatarEditorTarget.WEB_EDITOR)
        }
    }

    @Test
    fun `returns quick editor on 401 so the sheet can offer its session expired recovery`() = test {
        givenResponse(code = 401, body = """{"error":"not authorized"}""", contentType = "application/json")

        assertThat(resolver.resolve(TOKEN)).isEqualTo(GravatarEditorTarget.QUICK_EDITOR)
    }

    @Test
    fun `falls open to quick editor when the probe cannot complete`() = test {
        val call = mock<Call>()
        whenever(clientFactory.create()).thenReturn(client)
        whenever(client.newCall(any())).thenReturn(call)
        whenever(call.execute()).thenThrow(IOException("offline"))

        assertThat(resolver.resolve(TOKEN)).isEqualTo(GravatarEditorTarget.QUICK_EDITOR)
    }

    @Test
    fun `does not probe without a token`() = test {
        assertThat(resolver.resolve("")).isEqualTo(GravatarEditorTarget.QUICK_EDITOR)

        verify(clientFactory, never()).create()
    }

    @Test
    fun `sends the same request the SDK sends`() = test {
        givenResponse(code = 200, body = "{}", contentType = "application/json")

        resolver.resolve(TOKEN)

        val captor = argumentCaptor<Request>()
        verify(client).newCall(captor.capture())
        with(captor.firstValue) {
            assertThat(url.toString()).isEqualTo("https://api.gravatar.com/v3/me/profile")
            assertThat(method).isEqualTo("GET")
            assertThat(header("Authorization")).isEqualTo("Bearer $TOKEN")
            assertThat(header("Accept")).isEqualTo("application/json")
            assertThat(header("X-Platform")).isEqualTo("Android")
            assertThat(header("X-SDK-Version")).isNotBlank()
            // Left unset on purpose so OkHttp stamps its own, exactly as the SDK's client does
            assertThat(header("User-Agent")).isNull()
        }
    }

    @Test
    fun `builds a new client per probe, matching the SDK's connection churn`() = test {
        givenResponse(code = 200, body = "{}", contentType = "application/json")

        resolver.resolve(TOKEN)
        resolver.resolve(TOKEN)

        verify(clientFactory, times(2)).create()
    }

    private fun givenResponse(code: Int, body: String, contentType: String) {
        val call = mock<Call>()
        whenever(clientFactory.create()).thenReturn(client)
        whenever(client.newCall(any())).thenReturn(call)
        whenever(call.execute()).thenAnswer {
            Response.Builder()
                .request(Request.Builder().url("https://api.gravatar.com/v3/me/profile").build())
                .protocol(Protocol.HTTP_2)
                .code(code)
                .message("probe")
                .body(body.toResponseBody(contentType.toMediaType()))
                .build()
        }
    }

    companion object {
        private const val TOKEN = "a-wpcom-access-token"
        private val NGINX_403 = """
            <html>
            <head><title>403 Forbidden</title></head>
            <body><center><h1>403 Forbidden</h1></center><hr><center>nginx</center></body>
            </html>
        """.trimIndent()
    }
}
