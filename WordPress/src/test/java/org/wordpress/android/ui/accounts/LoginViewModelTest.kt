package org.wordpress.android.ui.accounts

import com.sun.jna.Pointer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailPayloadScheme
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowNoJetpackSites
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSiteAddressError
import org.wordpress.android.ui.accounts.login.WPcomLoginHelper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import rs.wordpress.api.kotlin.WpLoginClient
import uniffi.wp_api.AutoDiscoveryAttemptSuccess
import uniffi.wp_api.ParsedUrl
import uniffi.wp_api.WpApiDetails

private const val TEST_URL = "https://www.test.com"
private const val TEST_URL_AUTH = "https://www.test.com/auth"
private const val TEST_URL_AUTH_SUFFIX = "?app_name=android-jetpack-client&success_url=null"

@ExperimentalCoroutinesApi
class LoginViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    lateinit var resourceProvider: ResourceProvider

    @Mock
    lateinit var wpLoginClient: WpLoginClient

    @Mock
    lateinit var wpComLoginHelper: WPcomLoginHelper

    @Mock
    lateinit var wpApiDetails: WpApiDetails

    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(wpComLoginHelper.appendParamsToRestAuthorizationUrl(any()))
            .thenReturn("$TEST_URL_AUTH$TEST_URL_AUTH_SUFFIX")
        viewModel = LoginViewModel(buildConfigWrapper, wpLoginClient, wpComLoginHelper)
    }

    @Test
    fun `given no jetpack sites, then ShowNoJetpackSitesError navigation event is posted`() {
        val navigationEvents = initObservers().navigationEvents

        viewModel.onHandleNoJetpackSites()

        assertThat(navigationEvents.last()).isInstanceOf(ShowNoJetpackSites::class.java)
    }

    @Test
    fun `given site is not jetpack, then ShowSiteAddressError navigation event is posted`() {
        val navigationEvents = initObservers().navigationEvents
        val url = "nojetpack.wordpress.com"

        val connectSiteInfoPayload = getConnectSiteInfoPayload(url)
        viewModel.onHandleSiteAddressError(connectSiteInfoPayload)

        assertThat(navigationEvents.last()).isInstanceOf(ShowSiteAddressError::class.java)
    }

    @Test
    fun `given jetpack app, when magic link scheme is requested, then jetpack scheme is returned`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)

        val scheme = viewModel.getMagicLinkScheme()

        assertThat(scheme).isEqualTo(AuthEmailPayloadScheme.JETPACK)
    }

    @Test
    fun `given wordpress app, when magic link scheme is requested, then wordpress scheme is returned`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)

        val scheme = viewModel.getMagicLinkScheme()

        assertThat(scheme).isEqualTo(AuthEmailPayloadScheme.WORDPRESS)
    }

    @Test
    fun `given login scenario, when api discovery is success, then return the authentication url`() = runTest {
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL)))
            .thenReturn(
                AutoDiscoveryAttemptSuccess(
                    ParsedUrl(Pointer.createConstant(1)),
                    ParsedUrl(Pointer.createConstant(1)),
                    wpApiDetails
                )
            )
        whenever(wpApiDetails.findApplicationPasswordsAuthenticationUrl()).thenReturn(TEST_URL_AUTH)

        val result = viewModel.runApiDiscovery(TEST_URL)

        assertEquals("$TEST_URL_AUTH$TEST_URL_AUTH_SUFFIX", result)
        verify(wpLoginClient).apiDiscovery(eq(TEST_URL))
    }

    @Test
    fun `given login scenario, when api discovery is fails, then return empty authentication url`() = runTest {
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL))).doThrow(RuntimeException("API discovery failed"))

        val result = viewModel.runApiDiscovery(TEST_URL)

        assertEquals("", result)
        verify(wpLoginClient).apiDiscovery(eq(TEST_URL))
    }

    private fun getConnectSiteInfoPayload(url: String): ConnectSiteInfoPayload =
        ConnectSiteInfoPayload(url, null)

    private data class Observers(
        val navigationEvents: List<LoginNavigationEvents>
    )

    private fun initObservers(): Observers {
        val navigationEvents = mutableListOf<LoginNavigationEvents>()
        viewModel.navigationEvents.observeForever { navigationEvents.add(it.peekContent()) }

        return Observers(navigationEvents)
    }
}
