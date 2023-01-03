package org.wordpress.android.ui.accounts

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailPayloadScheme
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowNoJetpackSites
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSiteAddressError
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class LoginViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    lateinit var resourceProvider: ResourceProvider
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        viewModel = LoginViewModel(buildConfigWrapper)
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
