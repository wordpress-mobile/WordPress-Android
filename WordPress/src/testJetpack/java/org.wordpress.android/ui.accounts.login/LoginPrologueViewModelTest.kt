package org.wordpress.android.ui.accounts.login

import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.ui.accounts.LoginNavigationEvents
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowEmailLoginScreen
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowLoginViaSiteAddressScreen
import org.wordpress.android.ui.accounts.UnifiedLoginTracker
import org.wordpress.android.ui.accounts.login.LoginPrologueViewModel.UiState
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event

@InternalCoroutinesApi
class LoginPrologueViewModelTest : BaseUnitTest() {
    @Mock lateinit var unifiedLoginTracker: UnifiedLoginTracker
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock lateinit var buildConfigWrapper: BuildConfigWrapper
    private lateinit var viewModel: LoginPrologueViewModel

    @Before
    fun setUp() {
        viewModel = LoginPrologueViewModel(
                unifiedLoginTracker,
                analyticsTrackerWrapper,
                buildConfigWrapper,
                TEST_DISPATCHER
        )
    }

    @Test
    fun `given signup disabled, when view starts, then continue with wpcom button is displayed with correct title`() {
        whenever(buildConfigWrapper.isSignupEnabled).thenReturn(false)
        val observers = init()

        assertThat(observers.uiStates.last().continueWithWpcomButtonState.title)
                .isEqualTo(R.string.continue_with_wpcom_no_signup)
    }

    @Test
    fun `given signup enabled, when view starts, then continue with wpcom button is displayed with correct title`() {
        whenever(buildConfigWrapper.isSignupEnabled).thenReturn(true)

        val observers = init()

        assertThat(observers.uiStates.last().continueWithWpcomButtonState.title)
                .isEqualTo(R.string.continue_with_wpcom)
    }

    @Test
    fun `when view starts, enter your site address button is displayed with correct title`() {
        val observers = init()

        assertThat(observers.uiStates.last().enterYourSiteAddressButtonState.title)
                .isEqualTo(R.string.enter_your_site_address)
    }

    @Test
    fun `when continue with wpcom button is clicked, then app navigates to email login screen`() {
        val observers = init()

        (observers.uiStates.last().continueWithWpcomButtonState).onClick.invoke()

        assertThat(observers.navigationEvents.last().peekContent()).isEqualTo(ShowEmailLoginScreen)
    }

    @Test
    fun `when enter your site address button is clicked, then app navigates to show login via site address screen`() {
        val observers = init()

        (observers.uiStates.last().enterYourSiteAddressButtonState).onClick.invoke()

        assertThat(observers.navigationEvents.last().peekContent()).isEqualTo(ShowLoginViaSiteAddressScreen)
    }

    private fun init(): Observers {
        val uiStates = mutableListOf<UiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }

        val navigationEvents = mutableListOf<Event<LoginNavigationEvents>>()
        viewModel.navigationEvents.observeForever {
            navigationEvents.add(it)
        }

        viewModel.start()

        return Observers(uiStates, navigationEvents)
    }

    private data class Observers(
        val uiStates: List<UiState>,
        val navigationEvents: List<Event<LoginNavigationEvents>>
    )
}
