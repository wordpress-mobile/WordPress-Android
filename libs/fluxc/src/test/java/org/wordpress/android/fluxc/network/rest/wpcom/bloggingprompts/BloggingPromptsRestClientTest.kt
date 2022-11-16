package org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts

import com.android.volley.RequestQueue
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsErrorType.API_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsErrorType.TIMEOUT
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsRestClient.BloggingPromptResponse
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsRestClient.BloggingPromptsListResponse
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsRestClient.BloggingPromptsRespondentAvatar
import org.wordpress.android.fluxc.test
import java.util.Date

/* RESPONSE */

private val PROMPT_ONE = BloggingPromptResponse(
    id = 1010,
    text = "You have 15 minutes to address the whole world live (on television or radio — " +
        "choose your format). What would you say?",
    title = "Prompt number 4",
    content = "<!-- wp:pullquote -->\n" +
        "<figure class=\"wp-block-pullquote\"><blockquote><p>You have 15 minutes to address the" +
        " whole world live (on television or radio — choose your format). What would you say?</p>" +
        "<cite>(courtesy of plinky.com)</cite></blockquote></figure>\n" +
        "<!-- /wp:pullquote -->",
    date = "2022-01-04",
    attribution = "",
    isAnswered = false,
    respondentsCount = 0,
    respondentsAvatars = emptyList()
)

private val PROMPT_TWO = BloggingPromptResponse(
    id = 1011,
    text = "Do you play in your daily life? What says “playtime” to you?",
    title = "Prompt number 5",
    content = "<!-- wp:pullquote -->\n" +
        "<figure class=\"wp-block-pullquote\"><blockquote><p>Do you play in your daily life? What " +
        "says “playtime” to you?</p><cite>(courtesy of plinky.com)</cite></blockquote></figure>\n" +
        "<!-- /wp:pullquote -->",
    date = "2022-01-05",
    attribution = "dayone",
    isAnswered = true,
    respondentsCount = 1,
    respondentsAvatars = listOf(BloggingPromptsRespondentAvatar("http://site/avatar1.jpg"))
)

private val PROMPT_THREE = BloggingPromptResponse(
    id = 1012,
    text = "Are you good at what you do? What would you like to be better at.",
    title = "Prompt number 6",
    content = "<!-- wp:pullquote -->\n" +
        "<figure class=\"wp-block-pullquote\"><blockquote><p>Are you good at what you do? What" +
        " would you like to be better at.</p><cite>(courtesy of plinky.com)</cite></blockquote>" +
        "</figure>\n" +
        "<!-- /wp:pullquote -->",
    date = "2022-01-06",
    isAnswered = false,
    attribution = "",
    respondentsCount = 2,
    respondentsAvatars = listOf(
        BloggingPromptsRespondentAvatar("http://site/avatar2.jpg"),
        BloggingPromptsRespondentAvatar("http://site/avatar3.jpg")
    )
)

private val PROMPTS_RESPONSE = BloggingPromptsListResponse(
    prompts = listOf(PROMPT_ONE, PROMPT_TWO, PROMPT_THREE)
)

@RunWith(MockitoJUnitRunner::class)
class BloggingPromptsRestClientTest {
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent
    @Mock private lateinit var site: SiteModel

    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var restClient: BloggingPromptsRestClient

    private val siteId: Long = 1
    private val numberOfPromptsToFetch: Int = 40

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        restClient = BloggingPromptsRestClient(
            wpComGsonRequestBuilder,
            dispatcher,
            null,
            requestQueue,
            accessToken,
            userAgent
        )
    }

    @Test
    fun `when fetch prompts gets triggered, then the correct request url is used`() = test {
        val json = UnitTestUtils.getStringFromResourceFile(javaClass, PROMPTS_JSON)
        initFetchPrompts(data = getPromptsResponseFromJsonString(json))

        restClient.fetchPrompts(site, numberOfPromptsToFetch, Date())

        assertEquals(
            urlCaptor.firstValue,
            "$API_SITE_PATH/${site.siteId}/$API_BLOGGING_PROMPTS_PATH"
        )
    }

    @Test
    fun `given success call, when fetch prompts gets triggered, then prompts response is returned`() =
        test {
            val json = UnitTestUtils.getStringFromResourceFile(javaClass, PROMPTS_JSON)
            initFetchPrompts(data = getPromptsResponseFromJsonString(json))

            val result = restClient.fetchPrompts(site, numberOfPromptsToFetch, Date())

            assertSuccess(PROMPTS_RESPONSE, result)
        }

    @Test
    fun `given unknown error, when fetch prompts gets triggered, then return prompts timeout error`() =
        test {
            initFetchPrompts(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.UNKNOWN)))

            val result = restClient.fetchPrompts(site, numberOfPromptsToFetch, Date())

            assertError(GENERIC_ERROR, result)
        }

    @Test
    fun `given timeout, when fetch prompts gets triggered, then return prompts timeout error`() =
        test {
            initFetchPrompts(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.TIMEOUT)))

            val result = restClient.fetchPrompts(site, numberOfPromptsToFetch, Date())

            assertError(TIMEOUT, result)
        }

    @Test
    fun `given network error, when fetch prompts gets triggered, then return prompts api error`() =
        test {
            initFetchPrompts(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.NETWORK_ERROR)))

            val result = restClient.fetchPrompts(site, numberOfPromptsToFetch, Date())

            assertError(API_ERROR, result)
        }

    @Test
    fun `given invalid response, when fetch prompts gets triggered, then return prompts invalid response error`() =
        test {
            initFetchPrompts(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.INVALID_RESPONSE)))

            val result = restClient.fetchPrompts(site, numberOfPromptsToFetch, Date())

            assertError(INVALID_RESPONSE, result)
        }

    @Test
    fun `given not authenticated, when fetch prompts gets triggered, then return prompts auth required error`() =
        test {
            initFetchPrompts(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.NOT_AUTHENTICATED)))

            val result = restClient.fetchPrompts(site, numberOfPromptsToFetch, Date())

            assertError(AUTHORIZATION_REQUIRED, result)
        }

    private fun getPromptsResponseFromJsonString(json: String): BloggingPromptsListResponse {
        val responseType = object : TypeToken<BloggingPromptsListResponse>() {}.type
        return GsonBuilder()
            .create().fromJson(json, responseType) as BloggingPromptsListResponse
    }

    private suspend fun initFetchPrompts(
        data: BloggingPromptsListResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<BloggingPromptsListResponse> {
        val nonNullData = data ?: mock()
        val response = if (error != null) Response.Error(error) else Success(nonNullData)
        whenever(
            wpComGsonRequestBuilder.syncGetRequest(
                eq(restClient),
                urlCaptor.capture(),
                paramsCaptor.capture(),
                eq(BloggingPromptsListResponse::class.java),
                eq(false),
                any(),
                eq(false)
            )
        ).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)
        return response
    }

    @Suppress("SameParameterValue")
    private fun assertSuccess(
        expected: BloggingPromptsListResponse,
        actual: BloggingPromptsPayload<BloggingPromptsListResponse>
    ) {
        with(actual) {
            assertEquals(site, this@BloggingPromptsRestClientTest.site)
            assertFalse(isError)
            assertEquals(BloggingPromptsPayload(expected), this)
        }
    }

    private fun assertError(
        expected: BloggingPromptsErrorType,
        actual: BloggingPromptsPayload<BloggingPromptsListResponse>
    ) {
        with(actual) {
            assertEquals(site, this@BloggingPromptsRestClientTest.site)
            assertTrue(isError)
            assertEquals(expected, error.type)
            assertEquals(null, error.message)
        }
    }

    companion object {
        private const val API_BASE_PATH = "https://public-api.wordpress.com/wpcom/v2"
        private const val API_SITE_PATH = "$API_BASE_PATH/sites"
        private const val API_BLOGGING_PROMPTS_PATH = "blogging-prompts/"

        private const val PROMPTS_JSON = "wp/bloggingprompts/prompts.json"
    }
}
