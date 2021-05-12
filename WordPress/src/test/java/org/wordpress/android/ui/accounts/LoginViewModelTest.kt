package org.wordpress.android.ui.accounts

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowNoJetpackSites
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSiteAddressError
import org.wordpress.android.viewmodel.ResourceProvider

class LoginViewModelTest : BaseUnitTest() {
    @Mock lateinit var resourceProvider: ResourceProvider
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        viewModel = LoginViewModel()
    }

    @Test
    fun `given no jetpack sites, then ShowNoJetpackSitesError navigation event is posted`() {
        val navigationEvents = initObservers().navigationEvents

        viewModel.onHandleNoJetpackSites()

        Assertions.assertThat(navigationEvents.last()).isInstanceOf(ShowNoJetpackSites::class.java)
    }

    @Test
    fun `given site is not jetpack, then ShowSiteAddressError navigation event is posted`() {
        val navigationEvents = initObservers().navigationEvents
        val url = "nojetpack.wordpress.com"

        val connectSiteInfoPayload = getConnectSiteInfoPayload(url)
        viewModel.onHandleSiteAddressError(connectSiteInfoPayload)

        Assertions.assertThat(navigationEvents.last()).isInstanceOf(ShowSiteAddressError::class.java)
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
