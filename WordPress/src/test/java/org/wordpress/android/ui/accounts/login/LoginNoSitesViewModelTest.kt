package org.wordpress.android.ui.accounts.login

import android.os.Bundle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.accounts.LoginNavigationEvents
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowInstructions
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSignInForResultJetpackOnly
import org.wordpress.android.ui.accounts.UnifiedLoginTracker
import org.wordpress.android.ui.accounts.login.LoginNoSitesViewModel.State.NoUser
import org.wordpress.android.ui.accounts.login.LoginNoSitesViewModel.State.ShowUser
import org.wordpress.android.ui.accounts.login.LoginNoSitesViewModel.UiModel

@InternalCoroutinesApi
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
                TEST_DISPATCHER,
                TEST_DISPATCHER
        )
    }

    @Test
    fun `given started, then application sign out is invoked`() {
        setupAccountItem()

        startViewModel()

        verify(wordPress).wordPressComSignOut()
    }

    @Test
    fun `given savedInstanceState is NoUser, when started, then state is recreated as NoUser`() {
        val uiModel = initObservers().uiModel
        setupInstanceStateForNoUser()

        startViewModel(savedInstanceState)

        Assertions.assertThat(uiModel.last().state).isInstanceOf(NoUser::class.java)
    }

    @Test
    fun `given savedInstanceState is ShowUser, when started, then state is recreated as ShowUser`() {
        val uiModel = initObservers().uiModel
        setupInstanceStateForShowUser()

        startViewModel(savedInstanceState)

        Assertions.assertThat(uiModel.last().state).isInstanceOf(ShowUser::class.java)
    }

    @Test
    fun `given savedInstanceState is null, when account is null, then NoUser state is created`() {
        val uiModel = initObservers().uiModel

        startViewModel()

        Assertions.assertThat(uiModel.last().state).isInstanceOf(NoUser::class.java)
    }

    @Test
    fun `given savedInstanceState is null, when account is not null, then ShowUser state is created`() {
        val uiModel = initObservers().uiModel
        setupAccountItem()

        startViewModel()

        Assertions.assertThat(uiModel.last().state).isInstanceOf(ShowUser::class.java)
    }

    @Test
    fun `given on back pressed, then ShowSignInForResultJetpackOnly navigation event is posted`() {
        val navigationEvents = initObservers().navigationEvents
        setupAccountItem()

        startViewModel()
        viewModel.onBackPressed()

        Assertions.assertThat(navigationEvents.last()).isInstanceOf(ShowSignInForResultJetpackOnly::class.java)
    }

    @Test
    fun `when see instructions pressed, then ShowInstructions navigation event is posted`() {
        val navigationEvents = initObservers().navigationEvents
        setupAccountItem()

        startViewModel()
        viewModel.onSeeInstructionsPressed()

        Assertions.assertThat(navigationEvents.last()).isInstanceOf(ShowInstructions::class.java)
    }

    @Test
    fun `when choose another account is pressed, then ShowSignInForResultJetpackOnly navigation event is posted`() {
        val navigationEvents = initObservers().navigationEvents
        setupAccountItem()

        startViewModel()
        viewModel.onTryAnotherAccountPressed()

        Assertions.assertThat(navigationEvents.last()).isInstanceOf(ShowSignInForResultJetpackOnly::class.java)
    }

    @Test
    fun `given NoUser, when writeToBundle is invoked, then NoUser is writtenToBundle`() {
        startViewModel()

        viewModel.writeToBundle(savedInstanceState)

        verify(savedInstanceState).putSerializable(any(), argThat { this is NoUser })
    }

    @Test
    fun `given ShowUser, when writeToBundle is invoked, then ShowUser is writtenToBundle`() {
        setupAccountItem()
        startViewModel()

        viewModel.writeToBundle(savedInstanceState)

        verify(savedInstanceState).putSerializable(any(), argThat { this is ShowUser })
    }

    private val userName = "username"
    private val displayName = "displayname"
    private val avatarUrl = "avatarurl"

    private fun setupInstanceStateForNoUser() {
        whenever(savedInstanceState.getSerializable(KEY_STATE)).thenReturn(NoUser)
    }

    private fun setupInstanceStateForShowUser() {
        whenever(savedInstanceState.getSerializable(KEY_STATE)).thenReturn(
                ShowUser(
                        userName = userName,
                        displayName = displayName,
                        accountAvatarUrl = avatarUrl
                )
        )
    }

    private fun startViewModel(savedInstanceState: Bundle? = null) {
        viewModel.start(wordPress, savedInstanceState)
    }

    private fun setupAccountItem(
        userName: String = this.userName,
        displayName: String = this.displayName,
        avatarUrl: String = this.avatarUrl
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
