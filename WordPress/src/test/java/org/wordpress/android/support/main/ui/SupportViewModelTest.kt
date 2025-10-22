package org.wordpress.android.support.main.ui

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog

@ExperimentalCoroutinesApi
class SupportViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var appLogWrapper: AppLogWrapper

    @Mock
    lateinit var account: AccountModel

    private lateinit var viewModel: SupportViewModel

    @Before
    fun setUp() {
        viewModel = SupportViewModel(
            accountStore = accountStore,
            appLogWrapper = appLogWrapper
        )
    }

    // region init() tests

    @Test
    fun `init sets user info when user has access token`() {
        // Given
        val displayName = "Test User"
        val email = "test@example.com"
        val avatarUrl = "https://example.com/avatar.jpg"

        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.account).thenReturn(account)
        whenever(account.displayName).thenReturn(displayName)
        whenever(account.email).thenReturn(email)
        whenever(account.avatarUrl).thenReturn(avatarUrl)

        // When
        viewModel.init()

        // Then
        assertThat(viewModel.userInfo.value.userName).isEqualTo(displayName)
        assertThat(viewModel.userInfo.value.userEmail).isEqualTo(email)
        assertThat(viewModel.userInfo.value.avatarUrl).isEqualTo(avatarUrl)
    }

    @Test
    fun `init uses userName when displayName is empty`() {
        // Given
        val userName = "testuser"
        val email = "test@example.com"

        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.account).thenReturn(account)
        whenever(account.displayName).thenReturn("")
        whenever(account.userName).thenReturn(userName)
        whenever(account.email).thenReturn(email)
        whenever(account.avatarUrl).thenReturn("")

        // When
        viewModel.init()

        // Then
        assertThat(viewModel.userInfo.value.userName).isEqualTo(userName)
    }

    @Test
    fun `init sets avatarUrl to null when empty`() {
        // Given
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.account).thenReturn(account)
        whenever(account.displayName).thenReturn("Test User")
        whenever(account.email).thenReturn("test@example.com")
        whenever(account.avatarUrl).thenReturn("")

        // When
        viewModel.init()

        // Then
        assertThat(viewModel.userInfo.value.avatarUrl).isNull()
    }

    @Test
    fun `init sets isLoggedIn to true when user has access token`() {
        // Given
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.account).thenReturn(account)
        whenever(account.displayName).thenReturn("Test User")
        whenever(account.email).thenReturn("test@example.com")
        whenever(account.avatarUrl).thenReturn("")

        // When
        viewModel.init()

        // Then
        assertThat(viewModel.isLoggedIn.value).isTrue()
    }

    @Test
    fun `init sets hasAccessToken to false when user has no access token`() {
        // Given
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(accountStore.account).thenReturn(account)
        whenever(account.displayName).thenReturn("")
        whenever(account.userName).thenReturn("")
        whenever(account.email).thenReturn("")
        whenever(account.avatarUrl).thenReturn("")

        // When
        viewModel.init()

        // Then
        assertThat(viewModel.isLoggedIn.value).isFalse()
    }

    @Test
    fun `init shows all support options when user has access token`() {
        // Given
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.account).thenReturn(account)
        whenever(account.displayName).thenReturn("Test User")
        whenever(account.email).thenReturn("test@example.com")
        whenever(account.avatarUrl).thenReturn("")

        // When
        viewModel.init()

        // Then
        assertThat(viewModel.optionsVisibility.value.showAskTheBots).isTrue()
        assertThat(viewModel.optionsVisibility.value.showAskHappinessEngineers).isTrue()
    }

    @Test
    fun `init hides support options when user has no access token`() {
        // Given
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(accountStore.account).thenReturn(account)
        whenever(account.displayName).thenReturn("")
        whenever(account.userName).thenReturn("")
        whenever(account.email).thenReturn("")
        whenever(account.avatarUrl).thenReturn("")

        // When
        viewModel.init()

        // Then
        assertThat(viewModel.optionsVisibility.value.showAskTheBots).isFalse()
        assertThat(viewModel.optionsVisibility.value.showAskHappinessEngineers).isFalse()
    }

    // endregion

    // region onAskTheBotsClick() tests

    @Test
    fun `onAskTheBotsClick emits NavigateToAskTheBots event when user has access token`() = test {
        // Given
        val accessToken = "test_access_token"
        val displayName = "Test User"

        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(accessToken)
        whenever(accountStore.account).thenReturn(account)
        whenever(account.displayName).thenReturn(displayName)

        // When
        viewModel.navigationEvents.test {
            viewModel.onAskTheBotsClick()

            // Then
            val event = awaitItem()
            assertThat(event).isInstanceOf(SupportViewModel.NavigationEvent.NavigateToAskTheBots::class.java)
            val navigateEvent = event as SupportViewModel.NavigationEvent.NavigateToAskTheBots
            assertThat(navigateEvent.accessToken).isEqualTo(accessToken)
            assertThat(navigateEvent.userName).isEqualTo(displayName)
        }
    }

    @Test
    fun `onAskTheBotsClick uses userName when displayName is empty`() = test {
        // Given
        val accessToken = "test_access_token"
        val userName = "testuser"

        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(accessToken)
        whenever(accountStore.account).thenReturn(account)
        whenever(account.displayName).thenReturn("")
        whenever(account.userName).thenReturn(userName)

        // When
        viewModel.navigationEvents.test {
            viewModel.onAskTheBotsClick()

            // Then
            val event = awaitItem()
            assertThat(event).isInstanceOf(SupportViewModel.NavigationEvent.NavigateToAskTheBots::class.java)
            val navigateEvent = event as SupportViewModel.NavigationEvent.NavigateToAskTheBots
            assertThat(navigateEvent.userName).isEqualTo(userName)
        }
    }

    @Test
    fun `onAskTheBotsClick logs debug message and does not emit event when user has no access token`() = test {
        // Given
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        // When
        viewModel.navigationEvents.test {
            viewModel.onAskTheBotsClick()

            // Then
            verify(appLogWrapper).d(
                eq(AppLog.T.SUPPORT),
                eq("Trying to open a bot conversation without access token")
            )
            expectNoEvents()
        }
    }

    // endregion

    // region onLoginClick() tests

    @Test
    fun `onLoginClick emits NavigateToLogin event`() = test {
        // When
        viewModel.navigationEvents.test {
            viewModel.onLoginClick()

            // Then
            val event = awaitItem()
            assertThat(event).isEqualTo(SupportViewModel.NavigationEvent.NavigateToLogin)
        }
    }

    // endregion

    // region onHelpCenterClick() tests

    @Test
    fun `onHelpCenterClick emits NavigateToHelpCenter event`() = test {
        // When
        viewModel.navigationEvents.test {
            viewModel.onHelpCenterClick()

            // Then
            val event = awaitItem()
            assertThat(event).isEqualTo(SupportViewModel.NavigationEvent.NavigateToHelpCenter)
        }
    }

    // endregion

    // region onApplicationLogsClick() tests

    @Test
    fun `onApplicationLogsClick emits NavigateToApplicationLogs event`() = test {
        // When
        viewModel.navigationEvents.test {
            viewModel.onApplicationLogsClick()

            // Then
            val event = awaitItem()
            assertThat(event).isEqualTo(SupportViewModel.NavigationEvent.NavigateToApplicationLogs)
        }
    }

    // endregion

    // region placeholder tests for unimplemented methods

    @Test
    fun `onAskHappinessEngineersClick does not throw exception`() {
        // When/Then - should not throw
        viewModel.onAskHappinessEngineersClick()
    }

    // endregion

    // region StateFlow initial values tests

    @Test
    fun `userInfo has correct initial values before init`() {
        // Then
        assertThat(viewModel.userInfo.value.userName).isEmpty()
        assertThat(viewModel.userInfo.value.userEmail).isEmpty()
        assertThat(viewModel.userInfo.value.avatarUrl).isNull()
    }

    @Test
    fun `optionsVisibility has correct initial values before init`() {
        // Then
        assertThat(viewModel.optionsVisibility.value.showAskTheBots).isTrue()
        assertThat(viewModel.optionsVisibility.value.showAskHappinessEngineers).isTrue()
    }

    @Test
    fun `hasAccessToken is false by default before init`() {
        // Then
        assertThat(viewModel.isLoggedIn.value).isFalse()
    }

    // endregion
}
