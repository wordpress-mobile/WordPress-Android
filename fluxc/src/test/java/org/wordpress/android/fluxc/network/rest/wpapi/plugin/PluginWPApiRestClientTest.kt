package org.wordpress.android.fluxc.network.rest.wpapi.plugin

import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.google.gson.reflect.TypeToken
import org.assertj.core.api.Assertions.assertThat
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
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Available
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.network.rest.wpapi.plugin.PluginResponseModel.Description
import org.wordpress.android.fluxc.test
import java.lang.reflect.Type

@RunWith(MockitoJUnitRunner::class)
class PluginWPApiRestClientTest {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var wpApiGsonRequestBuilder: WPAPIGsonRequestBuilder
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var userAgent: UserAgent
    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var bodyCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var restClient: PluginWPAPIRestClient
    private lateinit var site: SiteModel
    private val siteId: Long = 12
    private val siteUrl = "http://site.com"

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        bodyCaptor = argumentCaptor()
        restClient = PluginWPAPIRestClient(
                wpApiGsonRequestBuilder,
                dispatcher,
                requestQueue,
                userAgent
        )
        site = SiteModel()
        site.url = siteUrl
    }

    @Test
    fun `fetches plugins`() = test {
        initFetchPluginsResponse(listOf(testPlugin))
        val responseModel = restClient.fetchPlugins(site, Available("nonce"), false)
        assertThat(responseModel.data).isNotNull()
        assertMappedPlugin(responseModel.data!![0], testPlugin)
        assertThat(urlCaptor.lastValue)
                .isEqualTo("http://site.com/wp-json/wp/v2/plugins")
        assertThat(paramsCaptor.lastValue).isEqualTo(emptyMap<String, String>())
        assertThat(bodyCaptor.lastValue).isEqualTo(emptyMap<String, String>())
    }

    @Test
    fun `returns error response on fetch`() = test {
        val errorMessage = "message"
        val error = WPAPINetworkError(
            BaseNetworkError(
                NETWORK_ERROR,
                errorMessage,
                VolleyError(errorMessage)
            )
        )
        initFetchPluginsResponse(
            error = error
        )
        val responseModel = restClient.fetchPlugins(site, Available("nonce"), false)
        assertThat(responseModel.error).isEqualTo(error)
    }

    @Test
    fun `installs a plugin`() = test {
        initInstallPluginResponse(testPlugin)
        val installedPluginSlug = "plugin_slug"
        val responseModel = restClient.installPlugin(site, Available("nonce"), installedPluginSlug)
        assertMappedPlugin(responseModel.data!!, testPlugin)
        assertThat(urlCaptor.lastValue)
                .isEqualTo("http://site.com/wp-json/wp/v2/plugins")
        assertThat(bodyCaptor.lastValue).isEqualTo(mapOf("slug" to installedPluginSlug))
    }

    @Test
    fun `sets plugin as active`() = test {
        initConfigurePluginResponse(testPlugin)
        val installedPluginSlug = "plugin_slug"
        val active = true
        val responseModel = restClient.updatePlugin(site, Available("nonce"), installedPluginSlug, active)
        assertMappedPlugin(responseModel.data!!, testPlugin)
        assertThat(urlCaptor.lastValue)
                .isEqualTo("http://site.com/wp-json/wp/v2/plugins/$installedPluginSlug")
        assertThat(bodyCaptor.lastValue).isEqualTo(mapOf("status" to "active"))
    }

    @Test
    fun `sets plugin as inactive`() = test {
        initConfigurePluginResponse(testPlugin)
        val installedPluginSlug = "plugin_slug"
        val active = false
        val responseModel = restClient.updatePlugin(site, Available("nonce"), installedPluginSlug, active)
        assertMappedPlugin(responseModel.data!!, testPlugin)
        assertThat(urlCaptor.lastValue)
                .isEqualTo("http://site.com/wp-json/wp/v2/plugins/$installedPluginSlug")
        assertThat(bodyCaptor.lastValue).isEqualTo(mapOf("status" to "inactive"))
    }

    @Test
    fun `deletes a plugin`() = test {
        initDeletePluginResponse(testPlugin)
        val installedPluginSlug = "plugin_slug"
        val responseModel = restClient.deletePlugin(site, Available("nonce"), installedPluginSlug)
        assertMappedPlugin(responseModel.data!!, testPlugin)
        assertThat(urlCaptor.lastValue)
                .isEqualTo("http://site.com/wp-json/wp/v2/plugins/$installedPluginSlug")
        assertThat(bodyCaptor.lastValue).isEqualTo(emptyMap<String, String>())
    }

    private fun assertMappedPlugin(
        responseModel: SitePluginModel,
        plugin: PluginResponseModel
    ) {
        assertThat(responseModel.isActive).isEqualTo(plugin.status == "active")
        assertThat(responseModel.authorUrl).isEqualTo(plugin.authorUri)
        assertThat(responseModel.authorName).isEqualTo(plugin.author)
        assertThat(responseModel.description).isEqualTo(plugin.description!!.raw)
        assertThat(responseModel.displayName).isEqualTo(plugin.name)
        assertThat(responseModel.name).isEqualTo(plugin.plugin)
        assertThat(responseModel.pluginUrl).isEqualTo(plugin.pluginUri)
        assertThat(responseModel.version).isEqualTo(plugin.version)
        assertThat(responseModel.slug).isEqualTo(plugin.textDomain)
    }

    private suspend fun initFetchPluginsResponse(
        data: List<PluginResponseModel>? = null,
        error: WPAPINetworkError? = null
    ): WPAPIResponse<List<PluginResponseModel>> {
        val typeToken = object : TypeToken<List<PluginResponseModel>>() {}
        return initSyncGetResponse(typeToken.type, data ?: mock(), error)
    }

    private suspend fun <T> initSyncGetResponse(
        type: Type,
        data: T,
        error: WPAPINetworkError? = null,
        cachingEnabled: Boolean = false
    ): WPAPIResponse<T> {
        val response = if (error != null) WPAPIResponse.Error(error) else WPAPIResponse.Success(data)
        whenever(
                wpApiGsonRequestBuilder.syncGetRequest<T>(
                        eq(restClient),
                        urlCaptor.capture(),
                        paramsCaptor.capture(),
                        bodyCaptor.capture(),
                        eq(type),
                        eq(cachingEnabled),
                        any(),
                        any()
                )
        ).thenReturn(response)
        site.siteId = siteId
        return response
    }

    private suspend fun initInstallPluginResponse(
        data: PluginResponseModel? = null,
        error: WPAPINetworkError? = null
    ): WPAPIResponse<PluginResponseModel> {
        val response = if (error != null) Error(error) else Success(data ?: mock())
        whenever(
                wpApiGsonRequestBuilder.syncPostRequest(
                        eq(restClient),
                        urlCaptor.capture(),
                        bodyCaptor.capture(),
                        eq(PluginResponseModel::class.java),
                        any()
                )
        ).thenReturn(response)
        site.siteId = siteId
        return response
    }

    private suspend fun initConfigurePluginResponse(
        data: PluginResponseModel? = null,
        error: WPAPINetworkError? = null
    ): WPAPIResponse<PluginResponseModel> {
        val response = if (error != null) Error(error) else Success(data ?: mock())
        whenever(
                wpApiGsonRequestBuilder.syncPutRequest(
                        eq(restClient),
                        urlCaptor.capture(),
                        bodyCaptor.capture(),
                        eq(PluginResponseModel::class.java),
                        any()
                )
        ).thenReturn(response)
        site.siteId = siteId
        return response
    }

    private suspend fun initDeletePluginResponse(
        data: PluginResponseModel? = null,
        error: WPAPINetworkError? = null
    ): WPAPIResponse<PluginResponseModel> {
        val response = if (error != null) Error(error) else Success(data ?: mock())
        whenever(
                wpApiGsonRequestBuilder.syncDeleteRequest(
                        eq(restClient),
                        urlCaptor.capture(),
                        bodyCaptor.capture(),
                        eq(PluginResponseModel::class.java),
                        any()
                )
        ).thenReturn(response)
        site.siteId = siteId
        return response
    }

    companion object {
        private val testPlugin = PluginResponseModel(
                "test-plugin/test-plugin",
                "status",
                "name",
                "pluginUri",
                "author",
                "authorUri",
                Description("raw", "renderd"),
                "1.2.3",
                false,
                "",
                "",
                "plugin"
        )
    }
}
