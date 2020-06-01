package org.wordpress.android.fluxc.store

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.JetpackAction.INSTALL_JETPACK
import org.wordpress.android.fluxc.generated.JetpackActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackRestClient
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
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var siteStore: SiteStore
    @Mock private lateinit var site: SiteModel
    private lateinit var jetpackStore: JetpackStore

    @Before
    fun setUp() {
        jetpackStore = JetpackStore(jetpackRestClient, siteStore, initCoroutineEngine(), dispatcher)
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

        assertThat(result!!.success).isTrue()
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

        assertThat(result!!.success).isFalse()
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
}
