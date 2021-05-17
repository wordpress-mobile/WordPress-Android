package org.wordpress.android.ui.accounts

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before

import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper

class LoginEpilogueViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: LoginEpilogueViewModel

    @Mock lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock lateinit var siteStore: SiteStore

    @Before
    fun setUp() {
        viewModel = LoginEpilogueViewModel(appPrefsWrapper, siteStore)
    }

    /* Post Signup Interstitial Screen */
    @Test
    fun `given no sites, when continued from epilogue first time, then signup interstitial shown`() {
        init(hasSite = false, postSignupInterstitialShownEarlier = false)
        val navigationEvents = initObservers().navigationEvents

        viewModel.onContinue()

        assertThat(navigationEvents.first())
                .isInstanceOf(LoginNavigationEvents.ShowPostSignupInterstitialScreen::class.java)
    }

    @Test
    fun `given no sites, when continued from epilogue next time, then signup interstitial not shown`() {
        init(hasSite = false, postSignupInterstitialShownEarlier = true)
        val navigationEvents = initObservers().navigationEvents

        viewModel.onContinue()

        assertThat(
                navigationEvents.filterIsInstance(LoginNavigationEvents.ShowPostSignupInterstitialScreen::class.java)
        ).isEmpty()
    }

    @Test
    fun `given sites present, when continued from epilogue, then signup interstitial not shown`() {
        init(hasSite = true)
        val navigationEvents = initObservers().navigationEvents

        viewModel.onContinue()

        assertThat(
                navigationEvents.filterIsInstance(LoginNavigationEvents.ShowPostSignupInterstitialScreen::class.java)
        ).isEmpty()
    }

    /* Eplilogue Screen Close */
    @Test
    fun `given no sites, when continued from epilogue, then epilogue closes with ok result`() {
        init(hasSite = false)
        val navigationEvents = initObservers().navigationEvents

        viewModel.onContinue()

        assertThat(navigationEvents.last())
                .isInstanceOf(LoginNavigationEvents.CloseWithResultOk::class.java)
    }

    @Test
    fun `given sites present, when continued from epilogue, then screen closes with ok result`() {
        init(hasSite = true)
        val navigationEvents = initObservers().navigationEvents

        viewModel.onContinue()

        assertThat(navigationEvents.last()).isInstanceOf(LoginNavigationEvents.CloseWithResultOk::class.java)
    }

    private data class Observers(
        val navigationEvents: List<LoginNavigationEvents>,
    )

    private fun initObservers(): Observers {
        val navigationEvents = mutableListOf<LoginNavigationEvents>()
        viewModel.navigationEvents.observeForever { navigationEvents.add(it.peekContent()) }

        return Observers(navigationEvents)
    }

    fun init(
        hasSite: Boolean = false,
        postSignupInterstitialShownEarlier: Boolean = false
    ) {
        whenever(siteStore.hasSite()).thenReturn(hasSite)
        whenever(appPrefsWrapper.shouldShowPostSignupInterstitial).thenReturn(!postSignupInterstitialShownEarlier)
    }
}
