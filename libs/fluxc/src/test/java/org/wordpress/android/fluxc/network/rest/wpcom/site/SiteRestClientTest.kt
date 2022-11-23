package org.wordpress.android.fluxc.network.rest.wpcom.site

import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets
import org.wordpress.android.fluxc.network.rest.wpcom.site.NewSiteResponse.BlogDetails
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteWPComRestResponse.SitesResponse
import org.wordpress.android.fluxc.store.SiteStore.PostFormatsErrorType
import org.wordpress.android.fluxc.store.SiteStore.SiteFilter.WPCOM
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility.PUBLIC
import org.wordpress.android.fluxc.test

@RunWith(MockitoJUnitRunner::class)
class SiteRestClientTest {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent
    @Mock private lateinit var appSecrets: AppSecrets
    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var bodyCaptor: KArgumentCaptor<Map<String, Any>>
    private lateinit var restClient: SiteRestClient
    private val siteId: Long = 12

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        bodyCaptor = argumentCaptor()
        restClient = SiteRestClient(
                null,
                dispatcher,
                requestQueue,
                appSecrets,
                wpComGsonRequestBuilder,
                accessToken,
                userAgent
        )
        whenever(site.siteId).thenReturn(siteId)
    }

    @Test
    fun `returns fetched site`() = test {
        val response = SiteWPComRestResponse()
        response.ID = siteId
        val name = "Updated name"
        response.name = name
        response.URL = "site.com"

        initSiteResponse(response)

        val responseModel = restClient.fetchSite(site)
        assertThat(responseModel.name).isEqualTo(name)
        assertThat(responseModel.siteId).isEqualTo(siteId)
        assertThat(urlCaptor.lastValue)
                .isEqualTo("https://public-api.wordpress.com/rest/v1.1/sites/12")
        assertThat(paramsCaptor.lastValue).isEqualTo(
                mapOf(
                        "fields" to "ID,URL,name,description,jetpack,jetpack_connection," +
                                "visible,is_private,options,plan,capabilities,quota,icon,meta,zendesk_site_meta," +
                                "organization_id"
                )
        )
    }

    @Test
    fun `fetchSite returns error when API call fails`() = test {
        val errorMessage = "message"
        initSiteResponse(
                error = WPComGsonNetworkError(
                        BaseNetworkError(
                                GenericErrorType.NETWORK_ERROR,
                                errorMessage,
                                VolleyError(errorMessage)
                        )
                )
        )
        val errorResponse = restClient.fetchSite(site)

        assertThat(errorResponse.error).isNotNull()
        assertThat(errorResponse.error.type).isEqualTo(GenericErrorType.NETWORK_ERROR)
        assertThat(errorResponse.error.message).isEqualTo(errorMessage)
    }

    @Test
    fun `returns fetched sites`() = test {
        val response = SiteWPComRestResponse()
        response.ID = siteId
        val name = "Updated name"
        response.name = name
        response.URL = "site.com"

        val sitesResponse = SitesResponse()
        sitesResponse.sites = listOf(response)

        initSitesResponse(data = sitesResponse)

        val responseModel = restClient.fetchSites(listOf(WPCOM), false)
        assertThat(responseModel.sites).hasSize(1)
        assertThat(responseModel.sites[0].name).isEqualTo(name)
        assertThat(responseModel.sites[0].siteId).isEqualTo(siteId)

        assertThat(urlCaptor.lastValue)
                .isEqualTo("https://public-api.wordpress.com/rest/v1.2/me/sites/")
        assertThat(paramsCaptor.lastValue).isEqualTo(
                mapOf(
                        "filters" to "wpcom",
                        "fields" to "ID,URL,name,description,jetpack,jetpack_connection," +
                                "visible,is_private,options,plan,capabilities,quota,icon,meta,zendesk_site_meta," +
                                "organization_id"
                )
        )
    }

    @Test
    fun `fetched sites can filter JP connected package sites`() = test {
        val response = SiteWPComRestResponse()
        response.ID = siteId
        val name = "Updated name"
        response.name = name
        response.URL = "site.com"
        response.jetpack = false
        response.jetpack_connection = true

        val sitesResponse = SitesResponse()
        sitesResponse.sites = listOf(response)

        initSitesResponse(data = sitesResponse)

        val responseModel = restClient.fetchSites(listOf(WPCOM), true)

        assertThat(responseModel.sites).hasSize(0)
    }

    @Test
    fun `fetchSites returns error when API call fails`() = test {
        val errorMessage = "message"
        initSitesResponse(
                error = WPComGsonNetworkError(
                        BaseNetworkError(
                                GenericErrorType.NETWORK_ERROR,
                                errorMessage,
                                VolleyError(errorMessage)
                        )
                )
        )
        val errorResponse = restClient.fetchSites(listOf(), false)

        assertThat(errorResponse.error).isNotNull()
        assertThat(errorResponse.error.type).isEqualTo(GenericErrorType.NETWORK_ERROR)
        assertThat(errorResponse.error.message).isEqualTo(errorMessage)
    }

    @Test
    fun `creates new site with all params`() = test {
        val data = NewSiteResponse()
        val blogDetails = BlogDetails()
        blogDetails.blogid = siteId.toString()
        data.blog_details = blogDetails
        val appId = "app_id"
        whenever(appSecrets.appId).thenReturn(appId)
        val appSecret = "app_secret"
        whenever(appSecrets.appSecret).thenReturn(appSecret)

        initNewSiteResponse(data)

        val dryRun = false
        val siteName = "Site name"
        val siteTitle = "site title"
        val language = "CZ"
        val visibility = PUBLIC
        val segmentId = 123L
        val siteDesign = "design"
        val timeZoneId = "Europe/London"

        val result = restClient.newSite(
            siteName,
            siteTitle,
            language,
            timeZoneId,
            visibility,
            segmentId,
            siteDesign,
            dryRun
        )

        assertThat(result.newSiteRemoteId).isEqualTo(siteId)
        assertThat(result.dryRun).isEqualTo(dryRun)

        assertThat(urlCaptor.lastValue)
                .isEqualTo("https://public-api.wordpress.com/rest/v1.1/sites/new/")
        assertThat(bodyCaptor.lastValue).isEqualTo(
                mapOf(
                        "blog_name" to siteName,
                        "blog_title" to siteTitle,
                        "lang_id" to language,
                        "public" to "1",
                        "validate" to "0",
                        "client_id" to appId,
                        "client_secret" to appSecret,
                        "options" to mapOf<String, Any>(
                                "site_segment" to segmentId,
                                "template" to siteDesign,
                                "timezone_string" to timeZoneId
                        )
                )
        )
    }

    @Test
    fun `creates new site without a site name`() = test {
        val data = NewSiteResponse()
        val blogDetails = BlogDetails()
        blogDetails.blogid = siteId.toString()
        data.blog_details = blogDetails
        val appId = "app_id"
        whenever(appSecrets.appId).thenReturn(appId)
        val appSecret = "app_secret"
        whenever(appSecrets.appSecret).thenReturn(appSecret)

        initNewSiteResponse(data)

        val dryRun = false
        val siteName = null
        val siteTitle = "site title"
        val language = "CZ"
        val visibility = PUBLIC
        val segmentId = 123L
        val siteDesign = "design"
        val timeZoneId = "Europe/London"

        val result = restClient.newSite(
            siteName,
            siteTitle,
            language,
            timeZoneId,
            visibility,
            segmentId,
            siteDesign,
            dryRun
        )

        assertThat(result.newSiteRemoteId).isEqualTo(siteId)
        assertThat(result.dryRun).isEqualTo(dryRun)

        assertThat(urlCaptor.lastValue)
            .isEqualTo("https://public-api.wordpress.com/rest/v1.1/sites/new/")
        assertThat(bodyCaptor.lastValue).isEqualTo(
            mapOf(
                "blog_name" to siteTitle,
                "blog_title" to siteTitle,
                "lang_id" to language,
                "public" to "1",
                "validate" to "0",
                "find_available_url" to "1",
                "client_id" to appId,
                "client_secret" to appSecret,
                "options" to mapOf<String, Any>(
                    "site_segment" to segmentId,
                    "template" to siteDesign,
                    "site_creation_flow" to "with-design-picker",
                    "timezone_string" to timeZoneId
                )
            )
        )
    }

    @Test
    fun `creates new site without a site name and site title`() = test {
        val data = NewSiteResponse()
        val blogDetails = BlogDetails()
        blogDetails.blogid = siteId.toString()
        data.blog_details = blogDetails
        val appId = "app_id"
        whenever(appSecrets.appId).thenReturn(appId)
        val appSecret = "app_secret"
        whenever(appSecrets.appSecret).thenReturn(appSecret)

        initNewSiteResponse(data)

        val dryRun = false
        val siteName = null
        val siteTitle = null
        val language = "CZ"
        val visibility = PUBLIC
        val segmentId = 123L
        val siteDesign = "design"
        val timeZoneId = "Europe/London"

        val result = restClient.newSite(
            siteName,
            siteTitle,
            language,
            timeZoneId,
            visibility,
            segmentId,
            siteDesign,
            dryRun
        )

        assertThat(result.newSiteRemoteId).isEqualTo(siteId)
        assertThat(result.dryRun).isEqualTo(dryRun)

        assertThat(urlCaptor.lastValue)
            .isEqualTo("https://public-api.wordpress.com/rest/v1.1/sites/new/")
        assertThat(bodyCaptor.lastValue).isEqualTo(
            mapOf(
                "blog_name" to "",
                "lang_id" to language,
                "public" to "1",
                "validate" to "0",
                "find_available_url" to "1",
                "client_id" to appId,
                "client_secret" to appSecret,
                "options" to mapOf<String, Any>(
                    "site_segment" to segmentId,
                    "template" to siteDesign,
                    "site_creation_flow" to "with-design-picker",
                    "timezone_string" to timeZoneId
                )
            )
        )
    }

    @Test
    fun `creates new site without params with dry run`() = test {
        val data = NewSiteResponse()
        val blogDetails = BlogDetails()
        blogDetails.blogid = siteId.toString()
        data.blog_details = blogDetails
        val appId = "app_id"
        whenever(appSecrets.appId).thenReturn(appId)
        val appSecret = "app_secret"
        whenever(appSecrets.appSecret).thenReturn(appSecret)

        initNewSiteResponse(data)

        val dryRun = true
        val siteName = "Site name"
        val siteTitle = null
        val language = "CZ"
        val visibility = SiteVisibility.PRIVATE
        val timeZoneId = "Europe/London"

        val result = restClient.newSite(
            siteName,
            siteTitle,
            language,
            timeZoneId,
            visibility,
            null,
            null,
            dryRun
        )

        assertThat(result.newSiteRemoteId).isEqualTo(siteId)
        assertThat(result.dryRun).isEqualTo(dryRun)

        assertThat(urlCaptor.lastValue)
                .isEqualTo("https://public-api.wordpress.com/rest/v1.1/sites/new/")
        assertThat(bodyCaptor.lastValue).isEqualTo(
                mapOf(
                        "blog_name" to siteName,
                        "lang_id" to language,
                        "public" to "-1",
                        "validate" to "1",
                        "client_id" to appId,
                        "client_secret" to appSecret,
                        "options" to mapOf<String, Any>("timezone_string" to timeZoneId)
                )
        )
    }

    @Test
    fun `returns fetched post formats`() = test {
        val response = PostFormatsResponse()
        val slug = "testSlug"
        val displayName = "testDisplayName"
        response.formats = mapOf(slug to displayName)

        initPostFormatsResponse(response)

        val responseModel = restClient.fetchPostFormats(site)
        assertThat(responseModel.postFormats).hasSize(1)
        assertThat(responseModel.postFormats[0].slug).isEqualTo(slug)
        assertThat(responseModel.postFormats[0].displayName).isEqualTo(displayName)
        assertThat(urlCaptor.lastValue)
                .isEqualTo("https://public-api.wordpress.com/rest/v1.1/sites/12/post-formats/")
    }

    @Test
    fun `fetchPostFormats returns error when API call fails`() = test {
        val errorMessage = "message"
        initPostFormatsResponse(
                error = WPComGsonNetworkError(
                        BaseNetworkError(
                                GenericErrorType.NETWORK_ERROR,
                                errorMessage,
                                VolleyError(errorMessage)
                        )
                )
        )
        val errorResponse = restClient.fetchPostFormats(site)

        assertThat(errorResponse.error).isNotNull()
        assertThat(errorResponse.error.type).isEqualTo(PostFormatsErrorType.GENERIC_ERROR)
    }

    private suspend fun initSiteResponse(
        data: SiteWPComRestResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<SiteWPComRestResponse> {
        return initGetResponse(SiteWPComRestResponse::class.java, data ?: mock(), error)
    }

    private suspend fun initSitesResponse(
        data: SitesResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<SitesResponse> {
        return initGetResponse(SitesResponse::class.java, data ?: mock(), error)
    }

    private suspend fun initNewSiteResponse(
        data: NewSiteResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<NewSiteResponse> {
        return initPostResponse(NewSiteResponse::class.java, data ?: mock(), error)
    }

    private suspend fun initPostFormatsResponse(
        data: PostFormatsResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<PostFormatsResponse> {
        return initGetResponse(PostFormatsResponse::class.java, data ?: mock(), error)
    }

    private suspend fun <T> initGetResponse(
        kclass: Class<T>,
        data: T,
        error: WPComGsonNetworkError? = null
    ): Response<T> {
        val response = if (error != null) Response.Error(error) else Success(data)
        whenever(
                wpComGsonRequestBuilder.syncGetRequest(
                        eq(restClient),
                        urlCaptor.capture(),
                        paramsCaptor.capture(),
                        eq(kclass),
                        any(),
                        any(),
                        any()

                )
        ).thenReturn(response)
        return response
    }

    private suspend fun <T> initPostResponse(
        kclass: Class<T>,
        data: T,
        error: WPComGsonNetworkError? = null
    ): Response<T> {
        val response = if (error != null) Response.Error(error) else Success(data)
        whenever(
                wpComGsonRequestBuilder.syncPostRequest(
                        eq(restClient),
                        urlCaptor.capture(),
                        paramsCaptor.capture(),
                        bodyCaptor.capture(),
                        eq(kclass),
                        anyOrNull()
                )
        ).thenReturn(response)
        return response
    }
}
