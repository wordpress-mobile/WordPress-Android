package org.wordpress.android.ui.accounts.login.jetpack

import android.os.Bundle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.accounts.LoginNavigationEvents
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowInstructions
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSignInForResultJetpackOnly
import org.wordpress.android.ui.accounts.UnifiedLoginTracker
import org.wordpress.android.ui.accounts.login.jetpack.LoginNoSitesViewModel.State.NoUser
import org.wordpress.android.ui.accounts.login.jetpack.LoginNoSitesViewModel.State.ShowUser
import org.wordpress.android.ui.accounts.login.jetpack.LoginNoSitesViewModel.UiModel

private const val USERNAME = "username"
private const val DISPLAY_NAME = "display_name"
private const val AVATAR_URL = "avatar_url"

@ExperimentalCoroutinesApi
class LoginNoSitesViewModelTest : BaseUnitTest() {
    @Mock lateinit var unifiedLoginTracker: UnifiedLoginTracker
    @Mock lateinit var wordPress: WordPress
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var savedInstanceState: Bundle
    private lateinit var viewModel: LoginNoSitesViewModel

    @Before
    fun setUp() {
        viewModel = LoginNoSitesViewModel(
                unifiedLoginTracker,
                accountStore,
                coroutinesTestRule.testDispatcher,
                coroutinesTestRule.testDispatcher
        )
    }

    @Test
    fun `given started, then application sign out is invoked`() {
        setupAccountItem()

        startViewModel()

        verify(wordPress).wordPressComSignOut()
    }

    @Test
    fun `given state does not include a user, when started, then state is recreated without a user`() {
        val uiModel = initObservers().uiModel
        setupInstanceStateForNoUser()

        startViewModel(savedInstanceState)

        assertThat(uiModel.last().state).isInstanceOf(NoUser::class.java)
    }

    @Test
    fun `given state includes a user, when started, then state is recreated with a user`() {
        val uiModel = initObservers().uiModel
        setupInstanceStateForShowUser()

        startViewModel(savedInstanceState)

        assertThat(uiModel.last().state).isInstanceOf(ShowUser::class.java)
    }

    @Test
    fun `given state is null, when account is null, then state is created without a user`() {
        val uiModel = initObservers().uiModel

        startViewModel()

        assertThat(uiModel.last().state).isInstanceOf(NoUser::class.java)
    }

    @Test
    fun `given state is null, when account is not null, then state is created with a user`() {
        val uiModel = initObservers().uiModel
        setupAccountItem()

        startViewModel()

        assertThat(uiModel.last().state).isInstanceOf(ShowUser::class.java)
    }

    @Test
    fun `given on back pressed, then a sign in navigation event is posted`() {
        val navigationEvents = initObservers().navigationEvents
        setupAccountItem()

        startViewModel()
        viewModel.onBackPressed()

        assertThat(navigationEvents.last()).isInstanceOf(ShowSignInForResultJetpackOnly::class.java)
    }

    @Test
    fun `when see instructions pressed, then a show instructions navigation event is posted`() {
        val navigationEvents = initObservers().navigationEvents
        setupAccountItem()

        startViewModel()
        viewModel.onSeeInstructionsPressed()

        assertThat(navigationEvents.last()).isInstanceOf(ShowInstructions::class.java)
    }

    @Test
    fun `when choose another account is pressed, then a sign in navigation event is posted`() {
        val navigationEvents = initObservers().navigationEvents
        setupAccountItem()

        startViewModel()
        viewModel.onTryAnotherAccountPressed()

        assertThat(navigationEvents.last()).isInstanceOf(ShowSignInForResultJetpackOnly::class.java)
    }

    @Test
    fun `given state does not include a user, when save state is invoked, then a no user state is written`() {
        startViewModel()

        viewModel.writeToBundle(savedInstanceState)

        verify(savedInstanceState).putSerializable(any(), argThat { this is NoUser })
    }

    @Test
    fun `given state includes a user, when save state is invoked, then a user state is written`() {
        setupAccountItem()
        startViewModel()

        viewModel.writeToBundle(savedInstanceState)

        verify(savedInstanceState).putSerializable(any(), argThat { this is ShowUser })
    }

    private fun setupInstanceStateForNoUser() {
        whenever(savedInstanceState.getSerializable(KEY_STATE)).thenReturn(NoUser)
    }

    private fun setupInstanceStateForShowUser() {
        whenever(savedInstanceState.getSerializable(KEY_STATE)).thenReturn(
                ShowUser(
                        userName = USERNAME,
                        displayName = DISPLAY_NAME,
                        accountAvatarUrl = AVATAR_URL
                )
        )
    }

    private fun startViewModel(savedInstanceState: Bundle? = null) {
        viewModel.start(wordPress, savedInstanceState)
    }

    private fun setupAccountItem(
        userName: String = USERNAME,
        displayName: String = DISPLAY_NAME,
        avatarUrl: String = AVATAR_URL
    ) {
        val accountModel = AccountModel()
        accountModel.userName = userName
        accountModel.displayName = displayName
        accountModel.avatarUrl = avatarUrl
        whenever(accountStore.account).thenReturn(accountModel)
    }

    private data class Observers(
        val navigationEvents: List<LoginNavigationEvents>,
        val uiModel: List<UiModel>
    )

    private fun initObservers(): Observers {
        val navigationEvents = mutableListOf<LoginNavigationEvents>()
        viewModel.navigationEvents.observeForever { navigationEvents.add(it.peekContent()) }

        val uiModel = mutableListOf<UiModel>()
        viewModel.uiModel.observeForever { uiModel.add(it) }

        return Observers(navigationEvents, uiModel)
    }
}
