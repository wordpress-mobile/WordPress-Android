package org.wordpress.android.ui.accounts

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowNoJetpackSites
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSiteAddressError
import org.wordpress.android.viewmodel.ResourceProvider

class LoginViewModelTest : BaseUnitTest() {
    @Mock lateinit var resourceProvider: ResourceProvider
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        viewModel = LoginViewModel(resourceProvider)
        whenever(resourceProvider.getString(ArgumentMatchers.anyInt(), ArgumentMatchers.anyString()))
                .thenReturn("Not a jetpack site")
        whenever(resourceProvider.getString(R.string.login_no_jetpack_sites)).thenReturn("No Jetpack sites.")
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

    @Test
    fun `given no jetpack sites, then error message in navigation matches expected message`() {
        val errorMessage = "No Jetpack sites."
        whenever(resourceProvider.getString(R.string.login_no_jetpack_sites)).thenReturn(errorMessage)

        val navigationEvents = initObservers().navigationEvents

        viewModel.onHandleNoJetpackSites()
        val navigationEvent = navigationEvents.last() as ShowNoJetpackSites

        Assertions.assertThat(navigationEvent.errorMessage == errorMessage)
    }

    @Test
    fun `given site is not jetpack, then error message in navigation matches expected message`() {
        val errorMessage = "Not a jetpack site"
        val url = "nojetpack.wordpress.com"
        whenever(resourceProvider.getString(R.string.login_not_a_jetpack_site, url)).thenReturn(errorMessage)
        val navigationEvents = initObservers().navigationEvents

        viewModel.onHandleSiteAddressError(getConnectSiteInfoPayload(url))
        val navigationEvent = navigationEvents.last() as ShowSiteAddressError

        Assertions.assertThat(navigationEvent.errorMessage == errorMessage)
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
