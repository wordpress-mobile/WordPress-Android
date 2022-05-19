package org.wordpress.android.fluxc.network.rest.wpcom.bloggingreminders

import com.android.volley.RequestQueue
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
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
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingreminders.BloggingRemindersRestClient.BloggingRemindersSettingsErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingreminders.BloggingRemindersRestClient.BloggingRemindersSettingsErrorType.API_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingreminders.BloggingRemindersRestClient.BloggingRemindersSettingsErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingreminders.BloggingRemindersRestClient.BloggingRemindersSettingsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingreminders.BloggingRemindersRestClient.BloggingRemindersSettingsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingreminders.BloggingRemindersRestClient.BloggingRemindersSettingsErrorType.TIMEOUT
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingreminders.BloggingRemindersRestClient.BloggingRemindersSettingsPayload
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingreminders.BloggingRemindersRestClient.BloggingRemindersSettingsResponseContent
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingreminders.BloggingRemindersRestClient.SetBloggingRemindersSettingsResponse
import org.wordpress.android.fluxc.persistence.BloggingRemindersDao.BloggingReminders
import org.wordpress.android.fluxc.test

/* RESPONSE */

private val REMINDERS_RESPONSE = BloggingRemindersSettingsResponseContent(
    promptsCardOptedIn = true,
    promptsRemindersOptedIn = false,
    potentialBloggingSite = false,
    reminderDays = mapOf(
        Pair("monday", false),
        Pair("tuesday", false),
        Pair("wednesday", false),
        Pair("thursday", false),
        Pair("friday", false),
        Pair("saturday", false),
        Pair("sunday", true)
    ),
    remindersTime = "10.33 UTC"
)

private const val LOCAL_SITE_ID = 123

val bloggingReminders = BloggingReminders(
    localSiteId = LOCAL_SITE_ID,
    isPromptCardOptedIn = true,
    isPromptRemindersOptedIn = false,
    isPotentialBloggingSite = false,
    monday = false,
    tuesday = false,
    wednesday = false,
    thursday = false,
    friday = false,
    saturday = false,
    sunday = true,
    hour = 10,
    minute = 33
)

@RunWith(MockitoJUnitRunner::class)
class BloggingRemindersRestClientTest {
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent
    @Mock private lateinit var site: SiteModel

    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var restClient: BloggingRemindersRestClient

    private val siteId: Long = 1
    private val numberOfPromptsToFetch: Int = 40

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        restClient = BloggingRemindersRestClient(
            wpComGsonRequestBuilder,
            dispatcher,
            null,
            requestQueue,
            accessToken,
            userAgent
        )
    }

    @Test
    fun `reminder settings response is correctly mapped to the  BloggingReminders model`() = test {
        val json = UnitTestUtils.getStringFromResourceFile(javaClass, GET_SETTINGS_JSON)
        val response = getReminderSettingsResponseFromJsonString(json)

        val model = response.toBloggingReminders(123)

        assertEquals(model, bloggingReminders)
    }

    @Test
    fun `when fetch reminder settings gets triggered, then the correct request url is used`() =
        test {
            val json = UnitTestUtils.getStringFromResourceFile(javaClass, GET_SETTINGS_JSON)
            initFetchRemindersSettings(data = getReminderSettingsResponseFromJsonString(json))

            restClient.fetchBloggingReminders(site)

            assertEquals(
                urlCaptor.firstValue,
                "$API_SITE_PATH/${site.siteId}/$API_BLOGGING_REMINDERS_SETTINGS_PATH"
            )
        }

    @Test
    fun `given success call, when fetch settings gets triggered, then correct response is returned`() =
        test {
            val json = UnitTestUtils.getStringFromResourceFile(javaClass, GET_SETTINGS_JSON)
            initFetchRemindersSettings(data = getReminderSettingsResponseFromJsonString(json))

            val result = restClient.fetchBloggingReminders(site)

            assertSuccess(REMINDERS_RESPONSE, result)
        }

    @Test
    fun `given unknown error, when fetch settings gets triggered, then return timeout error`() =
        test {
            initFetchRemindersSettings(
                error = WPComGsonNetworkError(
                    BaseNetworkError(
                        GenericErrorType.UNKNOWN
                    )
                )
            )

            val result = restClient.fetchBloggingReminders(site)

            assertError(GENERIC_ERROR, result)
        }

    @Test
    fun `given timeout, when fetch settings gets triggered, then return timeout error`() =
        test {
            initFetchRemindersSettings(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.TIMEOUT)))

            val result = restClient.fetchBloggingReminders(site)

            assertError(TIMEOUT, result)
        }

    @Test
    fun `given network error, when fetch settings gets triggered, then return api error`() =
        test {
            initFetchRemindersSettings(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.NETWORK_ERROR)))

            val result = restClient.fetchBloggingReminders(site)

            assertError(API_ERROR, result)
        }

    @Test
    fun `given invalid response, when fetch prompts gets triggered, then return prompts invalid response error`() =
        test {
            initFetchRemindersSettings(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.INVALID_RESPONSE)))

            val result = restClient.fetchBloggingReminders(site)

            assertError(INVALID_RESPONSE, result)
        }

    @Test
    fun `given not authenticated, when fetch prompts gets triggered, then return prompts auth required error`() =
        test {
            initFetchRemindersSettings(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.NOT_AUTHENTICATED)))

            val result = restClient.fetchBloggingReminders(site)

            assertError(AUTHORIZATION_REQUIRED, result)
        }

    private fun getReminderSettingsResponseFromJsonString(json: String): BloggingRemindersSettingsResponseContent {
        val responseType = object : TypeToken<BloggingRemindersSettingsResponseContent>() {}.type
        return GsonBuilder()
            .create().fromJson(json, responseType) as BloggingRemindersSettingsResponseContent
    }

    private fun getSetReminderSettingsResponseFromJsonString(json: String): SetBloggingRemindersSettingsResponse {
        val responseType = object : TypeToken<SetBloggingRemindersSettingsResponse>() {}.type
        return GsonBuilder()
            .create().fromJson(json, responseType) as SetBloggingRemindersSettingsResponse
    }

    private suspend fun initFetchRemindersSettings(
        data: BloggingRemindersSettingsResponseContent? = null,
        error: WPComGsonNetworkError? = null
    ): Response<BloggingRemindersSettingsResponseContent> {
        val nonNullData = data ?: mock()
        val response = if (error != null) Response.Error(error) else Success(nonNullData)
        whenever(
            wpComGsonRequestBuilder.syncGetRequest(
                eq(restClient),
                urlCaptor.capture(),
                paramsCaptor.capture(),
                eq(BloggingRemindersSettingsResponseContent::class.java),
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
        expected: BloggingRemindersSettingsResponseContent,
        actual: BloggingRemindersSettingsPayload<BloggingRemindersSettingsResponseContent>
    ) {
        with(actual) {
            assertEquals(site, this@BloggingRemindersRestClientTest.site)
            assertFalse(isError)
            assertEquals(BloggingRemindersSettingsPayload(expected), this)
        }
    }

    private fun assertError(
        expected: BloggingRemindersSettingsErrorType,
        actual: BloggingRemindersSettingsPayload<BloggingRemindersSettingsResponseContent>
    ) {
        with(actual) {
            assertEquals(site, this@BloggingRemindersRestClientTest.site)
            assertTrue(isError)
            assertEquals(expected, error.type)
            assertEquals(null, error.message)
        }
    }

    companion object {
        private const val API_BASE_PATH = "https://public-api.wordpress.com/wpcom/v2"
        private const val API_SITE_PATH = "$API_BASE_PATH/sites"
        private const val API_BLOGGING_REMINDERS_SETTINGS_PATH = "blogging-prompts/settings/"

        private const val GET_SETTINGS_JSON = "wp/bloggingreminders/get_reminders.json"
        private const val SET_SETTINGS_JSON = "wp/bloggingreminders/set_reminders.json"
    }
}
