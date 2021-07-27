package org.wordpress.android.ui.accounts

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before

import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper

class LoginEpilogueViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: LoginEpilogueViewModel

    @Mock lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock lateinit var buildConfigWrapper: BuildConfigWrapper
    @Mock lateinit var siteStore: SiteStore

    @Before
    fun setUp() {
        viewModel = LoginEpilogueViewModel(appPrefsWrapper, buildConfigWrapper, siteStore)
    }

    @Test
    fun `when site click is triggered, then select site with local id`() {
        val navigationEvents = initObservers().navigationEvents
        val localId = 1

        viewModel.onSiteClick(localId)

        val navigationEvent = navigationEvents.first() as LoginNavigationEvents.SelectSite
        assertThat(navigationEvent.localId).isEqualTo(localId)
    }

    @Test
    fun `when create new site is triggered, then launch create new site flow`() {
        val navigationEvents = initObservers().navigationEvents

        viewModel.onCreateNewSite()

        assertThat(navigationEvents.first()).isInstanceOf(LoginNavigationEvents.CreateNewSite::class.java)
    }

    /* WordPress app - Post Signup Interstitial Screen */
    @Test
    fun `given wp app with no sites, when continued from epilogue first time, then signup interstitial shown`() {
        init(isJetpackApp = false, hasSite = false, postSignupInterstitialShownEarlier = false)
        val navigationEvents = initObservers().navigationEvents

        viewModel.onContinue()

        assertThat(navigationEvents.first())
                .isInstanceOf(LoginNavigationEvents.ShowPostSignupInterstitialScreen::class.java)
    }

    @Test
    fun `given wp app with no sites, when continued from epilogue next time, then signup interstitial not shown`() {
        init(isJetpackApp = false, hasSite = false, postSignupInterstitialShownEarlier = true)
        val navigationEvents = initObservers().navigationEvents

        viewModel.onContinue()

        assertThat(
                navigationEvents.filterIsInstance(LoginNavigationEvents.ShowPostSignupInterstitialScreen::class.java)
        ).isEmpty()
    }

    @Test
    fun `given wp app with sites, when continued from epilogue, then signup interstitial not shown`() {
        init(isJetpackApp = false, hasSite = true)
        val navigationEvents = initObservers().navigationEvents

        viewModel.onContinue()

        assertThat(
                navigationEvents.filterIsInstance(LoginNavigationEvents.ShowPostSignupInterstitialScreen::class.java)
        ).isEmpty()
    }

    /* WordPress app - Eplilogue Screen Close */
    @Test
    fun `given wp app with no sites, when continued from epilogue, then epilogue closes with ok result`() {
        init(isJetpackApp = false, hasSite = false)
        val navigationEvents = initObservers().navigationEvents

        viewModel.onContinue()

        assertThat(navigationEvents.last())
                .isInstanceOf(LoginNavigationEvents.CloseWithResultOk::class.java)
    }

    @Test
    fun `given wp app with sites, when continued from epilogue, then epilogue closes with ok result`() {
        init(isJetpackApp = false, hasSite = true)
        val navigationEvents = initObservers().navigationEvents

        viewModel.onContinue()

        assertThat(navigationEvents.last()).isInstanceOf(LoginNavigationEvents.CloseWithResultOk::class.java)
    }

    /* Jetpack app - No Jetpack Sites Screen */
    @Test
    fun `given jetpack app with no sites, when continued from epilogue, then no jetpack sites is shown`() {
        init(isJetpackApp = true, hasSite = false)
        val navigationEvents = initObservers().navigationEvents

        viewModel.onContinue()

        assertThat(navigationEvents.last()).isInstanceOf(LoginNavigationEvents.ShowNoJetpackSites::class.java)
    }

    /* Jetpack app - Eplilogue Screen Close */
    @Test
    fun `given jetpack app with sites, when continued from epilogue, then screen closes with ok result`() {
        init(isJetpackApp = true, hasSite = true)
        val navigationEvents = initObservers().navigationEvents

        viewModel.onContinue()

        assertThat(navigationEvents.last()).isInstanceOf(LoginNavigationEvents.CloseWithResultOk::class.java)
    }

    private data class Observers(val navigationEvents: List<LoginNavigationEvents>)

    private fun initObservers(): Observers {
        val navigationEvents = mutableListOf<LoginNavigationEvents>()
        viewModel.navigationEvents.observeForever { navigationEvents.add(it.peekContent()) }

        return Observers(navigationEvents)
    }

    fun init(
        isJetpackApp: Boolean,
        hasSite: Boolean = false,
        postSignupInterstitialShownEarlier: Boolean = false
    ) {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(isJetpackApp)
        whenever(siteStore.hasSite()).thenReturn(hasSite)
        whenever(appPrefsWrapper.shouldShowPostSignupInterstitial).thenReturn(!postSignupInterstitialShownEarlier)
    }
}
