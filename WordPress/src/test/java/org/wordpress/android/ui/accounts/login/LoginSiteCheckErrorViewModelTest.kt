package org.wordpress.android.ui.accounts.login

import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.ui.accounts.LoginNavigationEvents
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowInstructions
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSignInForResultJetpackOnly

@InternalCoroutinesApi
class LoginSiteCheckErrorViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: LoginSiteCheckErrorViewModel

    @Before
    fun setUp() {
        viewModel = LoginSiteCheckErrorViewModel(TEST_DISPATCHER)
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
    fun `when see instructions pressed, then ShowInstructions navigation event is posted`() {
        val navigationEvents = initObservers().navigationEvents

        viewModel.start()
        viewModel.onSeeInstructionsPressed()

        Assertions.assertThat(navigationEvents.last()).isInstanceOf(ShowInstructions::class.java)
    }

    @Test
    fun `when choose another account is pressed, then ShowSignInForResultJetpackOnly navigation event is posted`() {
        val navigationEvents = initObservers().navigationEvents

        viewModel.start()
        viewModel.onTryAnotherAccountPressed()

        Assertions.assertThat(navigationEvents.last()).isInstanceOf(ShowSignInForResultJetpackOnly::class.java)
    }

    @Test
    fun `given on back pressed, then ShowSignInForResultJetpackOnly navigation event is posted`() {
        val navigationEvents = initObservers().navigationEvents

        viewModel.start()
        viewModel.onBackPressed()

        Assertions.assertThat(navigationEvents.last()).isInstanceOf(ShowSignInForResultJetpackOnly::class.java)
    }
}
