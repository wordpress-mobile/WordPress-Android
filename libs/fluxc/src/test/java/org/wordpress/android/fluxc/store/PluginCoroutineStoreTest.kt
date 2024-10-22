package org.wordpress.android.fluxc.store

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
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType.SITE
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.HTTP_AUTH_ERROR
import org.wordpress.android.fluxc.network.rest.wpapi.plugin.PluginWPAPIRestClient
import org.wordpress.android.fluxc.persistence.PluginSqlUtilsWrapper
import org.wordpress.android.fluxc.store.PluginCoroutineStore.WPApiPluginsPayload
import org.wordpress.android.fluxc.store.PluginStore.ConfigureSitePluginErrorType
import org.wordpress.android.fluxc.store.PluginStore.DeleteSitePluginErrorType
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginErrorType
import org.wordpress.android.fluxc.store.PluginStore.OnPluginDirectoryFetched
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginConfigured
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginDeleted
import org.wordpress.android.fluxc.store.PluginStore.PluginDirectoryErrorType.UNAUTHORIZED
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class PluginCoroutineStoreTest {
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var pluginWPAPIRestClient: PluginWPAPIRestClient
    @Mock lateinit var pluginSqlUtils: PluginSqlUtilsWrapper
    private lateinit var store: PluginCoroutineStore
    private val site: SiteModel = SiteModel().apply {
        url = "site.com"
        username = "username"
    }
    private lateinit var onFetchedEventCaptor: KArgumentCaptor<OnPluginDirectoryFetched>
    private lateinit var onDeletedEventCaptor: KArgumentCaptor<OnSitePluginDeleted>
    private lateinit var onConfiguredEventCaptor: KArgumentCaptor<OnSitePluginConfigured>

    @Before
    fun setUp() {
        store = PluginCoroutineStore(
            initCoroutineEngine(),
            dispatcher,
            pluginWPAPIRestClient,
            pluginSqlUtils
        )
        onFetchedEventCaptor = argumentCaptor()
        onDeletedEventCaptor = argumentCaptor()
        onConfiguredEventCaptor = argumentCaptor()
    }

    @Test
    fun `fetches WP Api plugins with success`() = test {
        val fetchedPlugins = listOf(
            SitePluginModel()
        )
        whenever(pluginWPAPIRestClient.fetchPlugins(site)).thenReturn(
            WPApiPluginsPayload(
                site,
                fetchedPlugins
            )
        )

        val result = store.syncFetchWPApiPlugins(site)

        assertThat(result.isError).isFalse
        assertThat(result.type).isEqualTo(SITE)
        verify(pluginSqlUtils).insertOrReplaceSitePlugins(site, fetchedPlugins)
    }

    @Test
    fun `fetches WP Api plugins with error `() = test {
        whenever(pluginWPAPIRestClient.fetchPlugins(site)).thenReturn(
            WPApiPluginsPayload(
                BaseNetworkError(
                    GenericErrorType.AUTHORIZATION_REQUIRED
                )
            )
        )

        val result = store.syncFetchWPApiPlugins(site)

        assertThat(result.isError).isTrue
        assertThat(result.error.type).isEqualTo(UNAUTHORIZED)
        verifyNoInteractions(pluginSqlUtils)
    }

    @Test
    fun `fetches WP Api plugins and emits event`() = test {
        val fetchedPlugins = listOf(
            SitePluginModel()
        )
        whenever(pluginWPAPIRestClient.fetchPlugins(site)).thenReturn(
            WPApiPluginsPayload(
                site,
                fetchedPlugins
            )
        )

        store.fetchWPApiPlugins(site)

        verify(dispatcher).emitChange(onFetchedEventCaptor.capture())
        assertThat(onFetchedEventCaptor.lastValue.isError).isFalse
        assertThat(onFetchedEventCaptor.lastValue.type).isEqualTo(SITE)
        verify(pluginSqlUtils).insertOrReplaceSitePlugins(site, fetchedPlugins)
    }

    @Test
    fun `deletes a plugin with success`() = test {
        val pluginName = "plugin_name"
        val slug = "plugin_slug"
        val sitePluginModel = SitePluginModel()
        whenever(pluginSqlUtils.getSitePluginBySlug(site, slug)).thenReturn(sitePluginModel)
        whenever(pluginWPAPIRestClient.deletePlugin(site, pluginName)).thenReturn(
            WPApiPluginsPayload(
                site,
                sitePluginModel
            )
        )

        val result = store.syncDeleteSitePlugin(site, pluginName, slug)

        assertThat(result.isError).isFalse
        assertThat(result.pluginName).isEqualTo(pluginName)
        assertThat(result.slug).isEqualTo(slug)
        verify(pluginSqlUtils).deleteSitePlugin(site, slug)
    }

    @Test
    fun `deletes a plugin and emits an event`() = test {
        val pluginName = "plugin_name"
        val slug = "plugin_slug"
        val sitePluginModel = SitePluginModel()
        sitePluginModel.name = pluginName
        whenever(pluginSqlUtils.getSitePluginBySlug(site, slug)).thenReturn(sitePluginModel)
        whenever(pluginWPAPIRestClient.deletePlugin(site, pluginName)).thenReturn(
            WPApiPluginsPayload(
                site,
                sitePluginModel
            )
        )

        store.deleteSitePlugin(site, pluginName, slug)

        verify(dispatcher).emitChange(onDeletedEventCaptor.capture())
        assertThat(onDeletedEventCaptor.lastValue.isError).isFalse
        assertThat(onDeletedEventCaptor.lastValue.pluginName).isEqualTo(pluginName)
        assertThat(onDeletedEventCaptor.lastValue.slug).isEqualTo(slug)
        verify(pluginSqlUtils).deleteSitePlugin(site, slug)
    }

    @Test
    fun `does not delete a plugin with a failure`() = test {
        val pluginName = "plugin_name"
        val slug = "plugin_slug"
        val sitePluginModel = SitePluginModel()
        whenever(pluginSqlUtils.getSitePluginBySlug(site, slug)).thenReturn(sitePluginModel)
        whenever(pluginWPAPIRestClient.deletePlugin(site, pluginName)).thenReturn(
            WPApiPluginsPayload(
                BaseNetworkError(HTTP_AUTH_ERROR)
            )
        )

        val result = store.syncDeleteSitePlugin(site, pluginName, slug)

        assertThat(result.isError).isTrue
        assertThat(result.error.type).isEqualTo(DeleteSitePluginErrorType.UNAUTHORIZED)
        verify(pluginSqlUtils, never()).deleteSitePlugin(eq(site), any())
    }

    @Test
    fun `deletes a plugin with a UNKNOWN_PLUGIN failure`() = test {
        val pluginName = "plugin_name"
        val slug = "plugin_slug"
        val sitePluginModel = SitePluginModel()
        whenever(pluginSqlUtils.getSitePluginBySlug(site, slug)).thenReturn(sitePluginModel)
        whenever(pluginWPAPIRestClient.deletePlugin(site, pluginName)).thenReturn(
            WPApiPluginsPayload(
                BaseNetworkError(GenericErrorType.NOT_FOUND)
            )
        )

        val result = store.syncDeleteSitePlugin(site, pluginName, slug)

        assertThat(result.isError).isFalse
        verify(pluginSqlUtils).deleteSitePlugin(site, slug)
    }

    @Test
    fun `configures a plugin with success`() = test {
        val pluginName = "plugin_name"
        val slug = "plugin_slug"
        val sitePluginModel = SitePluginModel()
        whenever(pluginSqlUtils.getSitePluginBySlug(site, slug)).thenReturn(sitePluginModel)
        val active = true
        whenever(pluginWPAPIRestClient.updatePlugin(site, pluginName, active)).thenReturn(
            WPApiPluginsPayload(
                site,
                sitePluginModel
            )
        )

        val result = store.syncConfigureSitePlugin(site, pluginName, slug, active)

        assertThat(result.isError).isFalse
        assertThat(result.pluginName).isEqualTo(pluginName)
        assertThat(result.slug).isEqualTo(slug)
        verify(pluginSqlUtils).insertOrUpdateSitePlugin(site, sitePluginModel)
    }

    @Test
    fun `configures a plugin and emits an event`() = test {
        val pluginName = "plugin_name"
        val slug = "plugin_slug"
        val sitePluginModel = SitePluginModel()
        sitePluginModel.name = pluginName
        whenever(pluginSqlUtils.getSitePluginBySlug(site, slug)).thenReturn(sitePluginModel)
        val active = true
        whenever(pluginWPAPIRestClient.updatePlugin(site, pluginName, active)).thenReturn(
            WPApiPluginsPayload(
                site,
                sitePluginModel
            )
        )

        store.configureSitePlugin(site, pluginName, slug, active)

        verify(dispatcher).emitChange(onConfiguredEventCaptor.capture())
        assertThat(onConfiguredEventCaptor.lastValue.isError).isFalse
        assertThat(onConfiguredEventCaptor.lastValue.pluginName).isEqualTo(pluginName)
        assertThat(onConfiguredEventCaptor.lastValue.slug).isEqualTo(slug)
        verify(pluginSqlUtils).insertOrUpdateSitePlugin(site, sitePluginModel)
    }

    @Test
    fun `does not configure a plugin with a failure`() = test {
        val pluginName = "plugin_name"
        val slug = "plugin_slug"
        val sitePluginModel = SitePluginModel()
        whenever(pluginSqlUtils.getSitePluginBySlug(site, slug)).thenReturn(sitePluginModel)
        val active = true
        whenever(pluginWPAPIRestClient.updatePlugin(site, pluginName, active)).thenReturn(
            WPApiPluginsPayload(
                BaseNetworkError(HTTP_AUTH_ERROR)
            )
        )

        val result = store.syncConfigureSitePlugin(site, pluginName, slug, active)

        assertThat(result.isError).isTrue
        assertThat(result.error.type).isEqualTo(ConfigureSitePluginErrorType.UNAUTHORIZED)
        verify(pluginSqlUtils, never()).insertOrUpdateSitePlugin(eq(site), any())
    }

    @Test
    fun `installs and activates a plugin with success`() = test {
        val slug = "plugin_slug"
        val sitePluginModel = SitePluginModel()
        val name = "plugin_name"
        sitePluginModel.name = name
        sitePluginModel.slug = slug
        whenever(pluginWPAPIRestClient.installPlugin(site, slug)).thenReturn(
            WPApiPluginsPayload(
                site,
                sitePluginModel
            )
        )
        whenever(pluginWPAPIRestClient.updatePlugin(site, name, true)).thenReturn(
            WPApiPluginsPayload(
                site,
                sitePluginModel
            )
        )

        val result = store.syncInstallSitePlugin(site, slug)

        assertThat(result.isError).isFalse
        assertThat(result.slug).isEqualTo(slug)
        verify(pluginSqlUtils, times(2)).insertOrUpdateSitePlugin(site, sitePluginModel)
        verify(pluginWPAPIRestClient).updatePlugin(site, name, true)
        verify(dispatcher, times(2)).emitChange(any())
    }

    @Test
    fun `does not activate plugin on install failure`() = test {
        val slug = "plugin_slug"
        val sitePluginModel = SitePluginModel()
        val name = "plugin_name"
        sitePluginModel.name = name
        sitePluginModel.slug = slug
        whenever(
            pluginWPAPIRestClient.installPlugin(
                site,

                slug
            )
        ).thenReturn(WPApiPluginsPayload(BaseNetworkError(HTTP_AUTH_ERROR)))

        val result = store.syncInstallSitePlugin(site, slug)

        assertThat(result.isError).isTrue
        assertThat(result.slug).isEqualTo(slug)
        assertThat(result.error.type).isEqualTo(InstallSitePluginErrorType.UNAUTHORIZED)
        verifyNoInteractions(pluginSqlUtils)
        verify(pluginWPAPIRestClient, never()).updatePlugin(eq(site), any(), any())
        verify(dispatcher, times(1)).emitChange(any())
    }
}
