package org.wordpress.android.ui.accounts.login.jetpack

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.accounts.LoginNavigationEvents
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowInstructions
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSignInForResultJetpackOnly

@ExperimentalCoroutinesApi
class LoginSiteCheckErrorViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: LoginSiteCheckErrorViewModel

    @Before
    fun setUp() {
        viewModel = LoginSiteCheckErrorViewModel(
                coroutinesTestRule.testDispatcher
        )
    }

    private data class Observers(
        val navigationEvents: List<LoginNavigationEvents>
    )

    private fun initObservers(): Observers {
        val navigationEvents = mutableListOf<LoginNavigationEvents>()
        viewModel.navigationEvents.observeForever { navigationEvents.add(it.peekContent()) }

        return Observers(navigationEvents)
    }

    @Test
    fun `when see instructions pressed, then a show instructions navigation event is posted`() {
        val navigationEvents = initObservers().navigationEvents

        viewModel.start()
        viewModel.onSeeInstructionsPressed()

        assertThat(navigationEvents.last()).isInstanceOf(ShowInstructions::class.java)
    }

    @Test
    fun `when choose another account is pressed, then a sign in navigation event is posted`() {
        val navigationEvents = initObservers().navigationEvents

        viewModel.start()
        viewModel.onTryAnotherAccountPressed()

        assertThat(navigationEvents.last()).isInstanceOf(ShowSignInForResultJetpackOnly::class.java)
    }

    @Test
    fun `given on back pressed, then show sign in navigation event is posted`() {
        val navigationEvents = initObservers().navigationEvents

        viewModel.start()
        viewModel.onBackPressed()

        assertThat(navigationEvents.last()).isInstanceOf(ShowSignInForResultJetpackOnly::class.java)
    }
}
