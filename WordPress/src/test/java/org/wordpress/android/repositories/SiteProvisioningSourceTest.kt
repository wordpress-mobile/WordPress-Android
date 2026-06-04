package org.wordpress.android.repositories

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordCredentials
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnApplicationPasswordCreated
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.accounts.login.SiteApiRestUrlRecoverer
import org.wordpress.android.ui.mysite.cards.applicationpassword.ApplicationPasswordValidator
import org.wordpress.android.util.NetworkUtilsWrapper

private const val TEST_SITE_LOCAL_ID = 7

@ExperimentalCoroutinesApi
class SiteProvisioningSourceTest : BaseUnitTest(StandardTestDispatcher()) {
    @Mock lateinit var siteStore: SiteStore
    @Mock lateinit var applicationPasswordLoginHelper: ApplicationPasswordLoginHelper
    @Mock lateinit var applicationPasswordValidator: ApplicationPasswordValidator
    @Mock lateinit var wpApiClientProvider: WpApiClientProvider
    @Mock lateinit var siteApiRestUrlRecoverer: SiteApiRestUrlRecoverer
    @Mock lateinit var editorSettingsRepository: EditorSettingsRepository
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var appLogWrapper: AppLogWrapper

    private lateinit var site: SiteModel
    private lateinit var source: SiteProvisioningSource

    @Before
    fun setUp() {
        site = SiteModel().apply {
            id = TEST_SITE_LOCAL_ID
            url = "https://test.example.com"
            // A non-null REST root so recoverRestUrl short-circuits unless a test clears it.
            wpApiRestUrl = "https://test.example.com/wp-json"
        }
        source = SiteProvisioningSource(
            siteStore,
            applicationPasswordLoginHelper,
            applicationPasswordValidator,
            wpApiClientProvider,
            siteApiRestUrlRecoverer,
            editorSettingsRepository,
            networkUtilsWrapper,
            appLogWrapper,
            testScope(),
        )
    }

    private fun stubHasStoredCredentials(value: Boolean) =
        whenever(applicationPasswordLoginHelper.siteHasBadCredentials(any())).thenReturn(!value)

    private suspend fun stubValidate(outcome: ApplicationPasswordValidator.Outcome) =
        whenever(applicationPasswordValidator.validate(any())).thenReturn(outcome)

    private suspend fun stubMintSuccess() =
        whenever(siteStore.createApplicationPassword(any())).thenReturn(
            OnApplicationPasswordCreated(site, ApplicationPasswordCredentials("user", "pass", uuid = "u"))
        )

    private suspend fun stubMintFailure() =
        whenever(siteStore.createApplicationPassword(any())).thenReturn(
            OnApplicationPasswordCreated(site, BaseNetworkError(GenericErrorType.UNKNOWN, "fail"), notSupported = false)
        )

    private suspend fun stubCapabilityProbe(ok: Boolean, cached: Boolean = false) {
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(any())).thenReturn(ok)
        // `ok || hasCache` short-circuits, so the cache is only read (and only needs stubbing)
        // when the live probe failed — stubbing it on success would be an unnecessary stub.
        if (!ok) whenever(editorSettingsRepository.hasCachedCapabilities(any())).thenReturn(cached)
    }

    // region auth stage

    @Test
    fun `given valid stored credentials, then provisioned and ready`() = test {
        stubHasStoredCredentials(true)
        stubValidate(ApplicationPasswordValidator.Outcome.Valid)
        stubCapabilityProbe(ok = true)

        assertThat(source.await(site)).isEqualTo(SiteReadiness.Ready)
        verify(siteStore, never()).createApplicationPassword(any())
    }

    @Test
    fun `given no credentials, then mints and is ready`() = test {
        stubHasStoredCredentials(false)
        stubMintSuccess()
        stubCapabilityProbe(ok = true)

        assertThat(source.await(site)).isEqualTo(SiteReadiness.Ready)
        verify(siteStore).createApplicationPassword(any())
    }

    @Test
    fun `given mint fails with no prior credentials, then needs first-time auth`() = test {
        stubHasStoredCredentials(false)
        stubMintFailure()

        val result = source.await(site)

        assertThat(result).isEqualTo(SiteReadiness.NeedsAuth(SiteAuthState.Unprovisionable(hadCredentials = false)))
        // Auth failed — the capability probe must not run.
        verify(editorSettingsRepository, never()).fetchEditorCapabilitiesForSite(any())
    }

    @Test
    fun `given stored credentials that fail to mint, then needs re-auth`() = test {
        stubHasStoredCredentials(true)
        stubValidate(ApplicationPasswordValidator.Outcome.Invalid)
        stubMintFailure()

        val result = source.await(site)

        assertThat(result).isEqualTo(SiteReadiness.NeedsAuth(SiteAuthState.Unprovisionable(hadCredentials = true)))
        verify(siteStore).deleteStoredApplicationPasswordCredentials(any())
    }

    @Test
    fun `given a transient validation error, then provisioning and no mint or probe`() = test {
        stubHasStoredCredentials(true)
        stubValidate(ApplicationPasswordValidator.Outcome.NetworkUnavailable)

        val result = source.await(site)

        assertThat(result).isEqualTo(SiteReadiness.NeedsAuth(SiteAuthState.Provisioning))
        verify(siteStore, never()).createApplicationPassword(any())
        verify(editorSettingsRepository, never()).fetchEditorCapabilitiesForSite(any())
    }

    // endregion

    // region url recovery + capability stages

    @Test
    fun `given provisioned with a missing REST root, then it recovers and persists the url`() = test {
        site.wpApiRestUrl = ""
        stubHasStoredCredentials(true)
        stubValidate(ApplicationPasswordValidator.Outcome.Valid)
        whenever(siteApiRestUrlRecoverer.discoverApiRootUrl(site.url))
            .thenReturn("https://test.example.com/custom-rest")
        stubCapabilityProbe(ok = true)

        source.await(site)

        verify(siteApiRestUrlRecoverer).persistApiRootUrl(eq(site.id), eq("https://test.example.com/custom-rest"))
    }

    @Test
    fun `given probe fails while offline, then transient error`() = test {
        stubHasStoredCredentials(true)
        stubValidate(ApplicationPasswordValidator.Outcome.Valid)
        stubCapabilityProbe(ok = false)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        assertThat(source.await(site)).isEqualTo(SiteReadiness.TransientError)
    }

    @Test
    fun `given probe fails while online, then unreachable`() = test {
        stubHasStoredCredentials(true)
        stubValidate(ApplicationPasswordValidator.Outcome.Valid)
        stubCapabilityProbe(ok = false)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        assertThat(source.await(site)).isEqualTo(SiteReadiness.Unreachable)
    }

    @Test
    fun `given probe fails but capabilities are cached, then ready`() = test {
        stubHasStoredCredentials(true)
        stubValidate(ApplicationPasswordValidator.Outcome.Valid)
        stubCapabilityProbe(ok = false, cached = true)

        assertThat(source.await(site)).isEqualTo(SiteReadiness.Ready)
    }

    // endregion

    // region single-flight / dedup / invalidate / clear

    @Test
    fun `given a prior ready run, when awaited again, then it does not re-run`() = test {
        stubHasStoredCredentials(true)
        stubValidate(ApplicationPasswordValidator.Outcome.Valid)
        stubCapabilityProbe(ok = true)

        source.await(site)
        source.await(site)

        verify(applicationPasswordValidator, times(1)).validate(any())
    }

    @Test
    fun `given a prior ready run, when invalidated, then it re-runs`() = test {
        stubHasStoredCredentials(true)
        stubValidate(ApplicationPasswordValidator.Outcome.Valid)
        stubCapabilityProbe(ok = true)

        source.await(site)
        source.invalidate(site)
        source.await(site)

        verify(applicationPasswordValidator, times(2)).validate(any())
    }

    @Test
    fun `given a ready site, when cleared, then the next run re-runs`() = test {
        stubHasStoredCredentials(true)
        stubValidate(ApplicationPasswordValidator.Outcome.Valid)
        stubCapabilityProbe(ok = true)

        source.await(site)
        source.clear()
        source.await(site)

        verify(applicationPasswordValidator, times(2)).validate(any())
    }

    // endregion
}
