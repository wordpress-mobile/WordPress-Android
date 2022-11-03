package org.wordpress.android.fluxc.store

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.JetpackAction.ACTIVATE_STATS_MODULE
import org.wordpress.android.fluxc.action.JetpackAction.INSTALL_JETPACK
import org.wordpress.android.fluxc.generated.JetpackActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIAuthenticator
import org.wordpress.android.fluxc.network.rest.wpapi.jetpack.JetpackWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackRestClient
import org.wordpress.android.fluxc.store.JetpackStore.ActivateStatsModuleError
import org.wordpress.android.fluxc.store.JetpackStore.ActivateStatsModuleErrorType
import org.wordpress.android.fluxc.store.JetpackStore.ActivateStatsModuleErrorType.API_ERROR
import org.wordpress.android.fluxc.store.JetpackStore.ActivateStatsModuleErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.store.JetpackStore.ActivateStatsModulePayload
import org.wordpress.android.fluxc.store.JetpackStore.ActivateStatsModuleResultPayload
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallError
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstalledPayload
import org.wordpress.android.fluxc.store.JetpackStore.OnJetpackInstalled
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class JetpackStoreTest {
    @Mock private lateinit var jetpackRestClient: JetpackRestClient
    @Mock private lateinit var jetpackWPAPIRestClient: JetpackWPAPIRestClient
    @Mock private lateinit var wpApiAuthenticator: WPAPIAuthenticator
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var siteStore: SiteStore
    @Mock private lateinit var site: SiteModel
    private lateinit var jetpackStore: JetpackStore

    @Before
    fun setUp() {
        jetpackStore = JetpackStore(
            jetpackRestClient,
            jetpackWPAPIRestClient,
            siteStore,
            initCoroutineEngine(),
            wpApiAuthenticator,
            dispatcher
        )
        val siteId = 1
        whenever(site.id).thenReturn(siteId)
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(site)
    }

    @Test
    fun `on install triggers rest client and returns success`() = test {
        val success = true
        whenever(jetpackRestClient.installJetpack(site)).thenReturn(JetpackInstalledPayload(site, success))

        var result: OnJetpackInstalled? = null
        launch(Dispatchers.Unconfined) { result = jetpackStore.install(site, INSTALL_JETPACK) }

        jetpackStore.onSiteChanged(OnSiteChanged(1))

        assertThat(result!!.success).isTrue
        val expectedChangeEvent = OnJetpackInstalled(success, INSTALL_JETPACK)
        verify(dispatcher).emitChange(eq(expectedChangeEvent))
    }

    @Test
    fun `on install action triggers rest client and returns success`() = test {
        val success = true
        whenever(jetpackRestClient.installJetpack(site)).thenReturn(JetpackInstalledPayload(site, success))

        jetpackStore.onAction(JetpackActionBuilder.newInstallJetpackAction(site))

        jetpackStore.onSiteChanged(OnSiteChanged(1))

        val expectedChangeEvent = OnJetpackInstalled(success, INSTALL_JETPACK)
        verify(dispatcher).emitChange(eq(expectedChangeEvent))
    }

    @Test
    fun `on install triggers rest client and returns error`() = test {
        val installError = JetpackInstallError(GENERIC_ERROR)
        val payload = JetpackInstalledPayload(installError, site)
        whenever(jetpackRestClient.installJetpack(site)).thenReturn(payload)

        var result: OnJetpackInstalled? = null
        launch(Dispatchers.Unconfined) { result = jetpackStore.install(site, INSTALL_JETPACK) }

        jetpackStore.onSiteChanged(OnSiteChanged(1))

        assertThat(result!!.success).isFalse
        val expectedChangeEvent = OnJetpackInstalled(installError, INSTALL_JETPACK)
        verify(dispatcher).emitChange(eq(expectedChangeEvent))
    }

    @Test
    fun `on install action triggers rest client and returns error`() = test {
        val installError = JetpackInstallError(GENERIC_ERROR)
        val payload = JetpackInstalledPayload(installError, site)
        whenever(jetpackRestClient.installJetpack(site)).thenReturn(payload)

        jetpackStore.onAction(JetpackActionBuilder.newInstallJetpackAction(site))

        jetpackStore.onSiteChanged(OnSiteChanged(1))

        val expectedChangeEvent = OnJetpackInstalled(installError, INSTALL_JETPACK)
        verify(dispatcher).emitChange(eq(expectedChangeEvent))
    }

    @Test
    fun `given activate stats request, then rest client is triggered`() = test {
        val requestPayload = ActivateStatsModulePayload(site)
        val successPayload = ActivateStatsModuleResultPayload(true, site)
        val enabled = "stats,other"

        whenever(jetpackRestClient.activateStatsModule(eq(requestPayload))).thenReturn(successPayload)
        whenever(site.activeModules).thenReturn(enabled)

        val action = JetpackActionBuilder.newActivateStatsModuleAction(requestPayload)
        jetpackStore.onAction(action)
        verify(jetpackRestClient).activateStatsModule(requestPayload)
    }

    @Test
    fun `given activate stats request, when rest client is triggered successfully, then success is returned`() = test {
        val requestPayload = ActivateStatsModulePayload(site)
        val successPayload = ActivateStatsModuleResultPayload(true, site)
        val enabled = "stats,other"
        whenever(jetpackRestClient.activateStatsModule(eq(requestPayload))).thenReturn(successPayload)
        whenever(site.activeModules).thenReturn(enabled)

        val action = JetpackActionBuilder.newActivateStatsModuleAction(requestPayload)
        jetpackStore.onAction(action)

        val expected = JetpackStore.OnActivateStatsModule(ACTIVATE_STATS_MODULE)
        verify(dispatcher).emitChange(expected)
    }

    @Test
    fun `given activate stats request, when rest client triggers invalid response, then error is returned`() = test {
        val error = ActivateStatsModuleError(INVALID_RESPONSE, "error")
        val requestPayload = ActivateStatsModulePayload(site)
        val resultPayload = ActivateStatsModuleResultPayload(error, site)
        whenever(jetpackRestClient.activateStatsModule(eq(requestPayload))).thenReturn(resultPayload)

        val action = JetpackActionBuilder.newActivateStatsModuleAction(requestPayload)
        jetpackStore.onAction(action)

        val expectedEventWithError = JetpackStore.OnActivateStatsModule(resultPayload.error, ACTIVATE_STATS_MODULE)
        verify(dispatcher).emitChange(expectedEventWithError)
    }

    @Test
    fun `given activate stats request, when rest client triggers api error, then error is returned`() = test {
        val error = ActivateStatsModuleError(API_ERROR, "error")
        val requestPayload = ActivateStatsModulePayload(site)
        val resultPayload = ActivateStatsModuleResultPayload(error, site)
        whenever(jetpackRestClient.activateStatsModule(eq(requestPayload))).thenReturn(resultPayload)

        val action = JetpackActionBuilder.newActivateStatsModuleAction(requestPayload)
        jetpackStore.onAction(action)

        val expectedEventWithError = JetpackStore.OnActivateStatsModule(resultPayload.error, ACTIVATE_STATS_MODULE)
        verify(dispatcher).emitChange(expectedEventWithError)
    }

    @Test
    fun `given activate stats request, when rest client triggers generic error, then error is returned`() = test {
        val error = ActivateStatsModuleError(ActivateStatsModuleErrorType.GENERIC_ERROR, "error")
        val requestPayload = ActivateStatsModulePayload(site)
        val resultPayload = ActivateStatsModuleResultPayload(error, site)
        whenever(jetpackRestClient.activateStatsModule(eq(requestPayload))).thenReturn(resultPayload)

        val action = JetpackActionBuilder.newActivateStatsModuleAction(requestPayload)
        jetpackStore.onAction(action)

        val expectedEventWithError = JetpackStore.OnActivateStatsModule(resultPayload.error, ACTIVATE_STATS_MODULE)
        verify(dispatcher).emitChange(expectedEventWithError)
    }

    @Test
    fun `given activate stats request, when rest client triggers auth error, then error is returned`() = test {
        val error = ActivateStatsModuleError(ActivateStatsModuleErrorType.AUTHORIZATION_REQUIRED, "error")
        val requestPayload = ActivateStatsModulePayload(site)
        val resultPayload = ActivateStatsModuleResultPayload(error, site)
        whenever(jetpackRestClient.activateStatsModule(eq(requestPayload))).thenReturn(resultPayload)

        val action = JetpackActionBuilder.newActivateStatsModuleAction(requestPayload)
        jetpackStore.onAction(action)

        val expectedEventWithError = JetpackStore.OnActivateStatsModule(resultPayload.error, ACTIVATE_STATS_MODULE)
        verify(dispatcher).emitChange(expectedEventWithError)
    }
}
