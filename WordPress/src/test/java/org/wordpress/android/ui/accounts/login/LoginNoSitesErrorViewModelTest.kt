package org.wordpress.android.ui.accounts.login

import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.WordPress
import org.wordpress.android.ui.accounts.LoginNavigationEvents
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSignInForResultJetpackOnly
import org.wordpress.android.ui.accounts.UnifiedLoginTracker

@InternalCoroutinesApi
class LoginNoSitesErrorViewModelTest : BaseUnitTest() {
    @Mock lateinit var unifiedLoginTracker: UnifiedLoginTracker
    @Mock lateinit var wordPress: WordPress
    private lateinit var viewModel: LoginNoSitesErrorViewModel

    @Before
    fun setUp() {
        viewModel = LoginNoSitesErrorViewModel(
                unifiedLoginTracker,
                TEST_DISPATCHER,
                TEST_DISPATCHER
        )
    }

    @Test
    fun `given started, then application sign out is invoked`() {
        viewModel.start(wordPress)

        verify(wordPress).wordPressComSignOut()
    }

    @Test
    fun `given on back pressed, then ShowSignInForResultJetpackOnly navigation event is posted`() {
        val navigationEvents = initObservers().navigationEvents

        viewModel.start(wordPress)
        viewModel.onBackPressed()

        Assertions.assertThat(navigationEvents.last()).isInstanceOf(ShowSignInForResultJetpackOnly::class.java)
    }

    private data class Observers(
        val navigationEvents: List<LoginNavigationEvents>
    )

    private fun initObservers(): Observers {
        val navigationEvents = mutableListOf<LoginNavigationEvents>()
        viewModel.navigationEvents.observeForever { navigationEvents.add(it.peekContent()) }

        return Observers(navigationEvents)
    }
}
