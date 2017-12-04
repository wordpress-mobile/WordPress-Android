package org.wordpress.android.fluxc.jetpacktunnel

import android.net.Uri
import com.google.gson.Gson
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.WPComJPTunnelGsonRequest
import org.wordpress.android.util.UrlUtils
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class JetpackTunnelGsonRequestTest {
    private val gson by lazy { Gson() }

    @Test
    fun testCreateGetRequest() {
        val url = "/"
        val params = mapOf("context" to "view")

        val request = WPComJPTunnelGsonRequest.buildGetRequest(url, 567, params,
                Any::class.java,
                { _: Any? -> },
                BaseErrorListener { _ -> }
        )

        // Verify that the request was built and wrapped as expected
        assertEquals(WPCOMREST.jetpack_blogs.site(567).rest_api.urlV1_1, UrlUtils.removeQuery(request?.url))
        val parsedUri = Uri.parse(request?.url)
        assertEquals(2, parsedUri.queryParameterNames.size)
        assertEquals("/&_method=get&context=view", parsedUri.getQueryParameter("path"))
        assertEquals("true", parsedUri.getQueryParameter("json"))

        // The wrapped GET request should have no body
        val bodyField = request!!::class.java.superclass.getDeclaredField("mBody")
        bodyField.isAccessible = true
        assertNull(bodyField.get(request))
    }

    @Test
    fun testCreatePostRequest() {
        val url = "/wp/v2/settings/"

        val requestBody = mapOf<String, Any>("title" to "New Title", "description" to "New Description")

        val request = WPComJPTunnelGsonRequest.buildPostRequest(url, 567, requestBody,
                Any::class.java,
                { _: Any? -> },
                BaseErrorListener { _ -> }
        )

        // Verify that the request was built and wrapped as expected
        assertEquals(WPCOMREST.jetpack_blogs.site(567).rest_api.urlV1_1, UrlUtils.removeQuery(request?.url))
        val parsedUri = Uri.parse(request?.url)
        assertEquals(0, parsedUri.queryParameterNames.size)
        val body = String(request?.body!!)
        val generatedBody = gson.fromJson(body, HashMap<String, String>()::class.java)
        assertEquals(3, generatedBody.size)
        assertEquals("/wp/v2/settings/&_method=post", generatedBody["path"])
        assertEquals("true", generatedBody["json"])
        assertEquals("{\"title\":\"New Title\",\"description\":\"New Description\"}", generatedBody["body"])
    }

    @Test
    fun testCreatePutRequest() {
        val url = "/wp/v2/settings/"

        val requestBody = mapOf<String, Any>("title" to "New Title", "description" to "New Description")

        val request = WPComJPTunnelGsonRequest.buildPutRequest(url, 567, requestBody,
                Any::class.java,
                { _: Any? -> },
                BaseErrorListener { _ -> }
        )

        // Verify that the request was built and wrapped as expected
        assertEquals(WPCOMREST.jetpack_blogs.site(567).rest_api.urlV1_1, UrlUtils.removeQuery(request?.url))
        val parsedUri = Uri.parse(request?.url)
        assertEquals(0, parsedUri.queryParameterNames.size)
        val body = String(request?.body!!)
        val generatedBody = gson.fromJson(body, HashMap<String, String>()::class.java)
        assertEquals(3, generatedBody.size)
        assertEquals("/wp/v2/settings/&_method=put", generatedBody["path"])
        assertEquals("true", generatedBody["json"])
        assertEquals("{\"title\":\"New Title\",\"description\":\"New Description\"}", generatedBody["body"])
    }

    @Test
    fun testCreatePatchRequest() {
        val url = "/wp/v2/settings/"

        val requestBody = mapOf<String, Any>("title" to "New Title", "description" to "New Description")

        val request = WPComJPTunnelGsonRequest.buildPatchRequest(url, 567, requestBody,
                Any::class.java,
                { _: Any? -> },
                BaseErrorListener { _ -> }
        )

        // Verify that the request was built and wrapped as expected
        assertEquals(WPCOMREST.jetpack_blogs.site(567).rest_api.urlV1_1, UrlUtils.removeQuery(request?.url))
        val parsedUri = Uri.parse(request?.url)
        assertEquals(0, parsedUri.queryParameterNames.size)
        val body = String(request?.body!!)
        val generatedBody = gson.fromJson(body, HashMap<String, String>()::class.java)
        assertEquals(3, generatedBody.size)
        assertEquals("/wp/v2/settings/&_method=patch", generatedBody["path"])
        assertEquals("true", generatedBody["json"])
        assertEquals("{\"title\":\"New Title\",\"description\":\"New Description\"}", generatedBody["body"])
    }

    @Test
    fun testCreateDeleteRequest() {
        val url = "/wp/v2/posts/6"
        val params = mapOf("force" to "true")

        val request = WPComJPTunnelGsonRequest.buildDeleteRequest(url, 567, params,
                Any::class.java,
                { _: Any? -> },
                BaseErrorListener { _ -> }
        )

        // Verify that the request was built and wrapped as expected
        assertEquals(WPCOMREST.jetpack_blogs.site(567).rest_api.urlV1_1, UrlUtils.removeQuery(request?.url))
        val parsedUri = Uri.parse(request?.url)
        assertEquals(0, parsedUri.queryParameterNames.size)
        val body = String(request?.body!!)
        val generatedBody = gson.fromJson(body, HashMap<String, String>()::class.java)
        assertEquals(2, generatedBody.size)
        assertEquals("/wp/v2/posts/6&_method=delete&force=true", generatedBody["path"])
        assertEquals("true", generatedBody["json"])
    }
}
