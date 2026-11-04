package org.wordpress.android.fluxc.network

import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import okhttp3.OkHttpClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OkHttpStackTest {
    @Test
    fun `given url path with dot segment, when checked, then it is detected`() {
        listOf(
            "https://h/rest/v1.1/read/tags/../../me/posts",
            "https://h/rest/v1.1/read/tags/./posts",
            "https://h/rest/v1.1/read/tags/%2e%2e/me",
            "https://h/rest/v1.1/read/tags/%2E./me",
            "https://h/rest/v1.1/read/tags/..\\..\\me",
            "https://h/rest/v1.1/read/tags/.\t./me",
            "https://h\\..\\me",
            "https://h/rest/v1.1/read/tags/..",
            "https://h/rest/v1.1/read/tags/.. ",
            "https://h/rest/v1.1/read/tags/..\n",
            "https://h/rest/v1.1/read/tags/..?x=1",
            "https://h/rest/v1.1/read/tags/..#x"
        ).forEach {
            assertThat(OkHttpStack.hasDotSegment(it)).describedAs(it).isTrue()
        }
    }

    @Test
    fun `given url path without dot segment, when checked, then it is not detected`() {
        listOf(
            "https://h/rest/v1.1/read/tags/dogs/posts",
            "https://h/rest/v1.1/read/tags/dogs.json",
            "https://h/rest/v1.1/read/tags/..dogs/posts",
            "https://h/rest/v1.1/read/tags/%2e%2e%2fme/posts",
            "https://h/rest/v1.1/read/tags/ハリー・ポッター/posts",
            "https://h/rest/v1.1/read/tags/.. /posts",
            "https://h/rest/v1.1/me?redirect_to=/x/../y",
            "https://h/rest/v1.1/me#/../y",
            "https://h?redirect_to=/x/../y",
            "https://h"
        ).forEach {
            assertThat(OkHttpStack.hasDotSegment(it)).describedAs(it).isFalse()
        }
    }

    @Test
    fun `given url path with dot segment, when executed, then request is rejected before reaching OkHttp`() {
        val client = OkHttpClient.Builder()
            .addInterceptor { throw AssertionError("request should not reach OkHttp") }
            .build()
        val request = TestRequest("https://public-api.wordpress.com/rest/v1.1/read/tags/../../me/mine/delete")

        assertThatThrownBy { OkHttpStack(client).executeRequest(request, emptyMap()) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    private class TestRequest(url: String) : Request<String>(Method.POST, url, null) {
        override fun deliverResponse(response: String?) = Unit

        override fun parseNetworkResponse(response: NetworkResponse?): Response<String> =
            Response.success("", null)
    }
}
