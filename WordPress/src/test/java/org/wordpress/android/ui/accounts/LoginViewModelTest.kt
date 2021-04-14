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
import org.wordpress.android.ui.accounts.LoginNavigationEvents.SlideInFragment
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
    fun `given no jetpack sites, then SlideInFragment navigation event is posted`() {
        val navigationEvents = initObservers().navigationEvents

        viewModel.handleNoJetpackSites()

        Assertions.assertThat(navigationEvents.last()).isInstanceOf(SlideInFragment::class.java)
    }

    @Test
    fun `given site is not jetpack, then SlideInFragment navigation event is posted`() {
        val navigationEvents = initObservers().navigationEvents
        val url = "nojetpack.wordpress.com"

        val connectSiteInfoPayload = getConnectSiteInfoPayload(url)
        viewModel.handleSiteAddressError(connectSiteInfoPayload)

        Assertions.assertThat(navigationEvents.last()).isInstanceOf(SlideInFragment::class.java)
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
