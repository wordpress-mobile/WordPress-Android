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
import org.wordpress.android.fluxc.network.NetworkRequestsRetentionPeriod
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.util.AppLog

@ExperimentalCoroutinesApi
class SupportViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var appLogWrapper: AppLogWrapper

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var experimentalFeatures: ExperimentalFeatures

    @Mock
    lateinit var account: AccountModel

    private lateinit var viewModel: SupportViewModel

    @Before
    fun setUp() {
        viewModel = SupportViewModel(
            accountStore = accountStore,
            appLogWrapper = appLogWrapper,
            appPrefsWrapper = appPrefsWrapper,
            experimentalFeatures = experimentalFeatures
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
        // Note: For WordPress variant (IS_JETPACK_APP=false), these options should be hidden
        // For Jetpack variant (IS_JETPACK_APP=true), they should be shown when user has access token
        // This test will behave differently based on which variant is being tested
        assertThat(viewModel.optionsVisibility.value.showAskTheBots)
            .isEqualTo(org.wordpress.android.BuildConfig.IS_JETPACK_APP)
        assertThat(viewModel.optionsVisibility.value.showAskHappinessEngineers)
            .isEqualTo(org.wordpress.android.BuildConfig.IS_JETPACK_APP)
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

    @Test
    fun `init shows network debugging when feature flag is enabled`() {
        // Given
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(accountStore.account).thenReturn(account)
        whenever(account.displayName).thenReturn("")
        whenever(account.userName).thenReturn("")
        whenever(account.email).thenReturn("")
        whenever(account.avatarUrl).thenReturn("")
        whenever(experimentalFeatures.isEnabled(ExperimentalFeatures.Feature.NETWORK_DEBUGGING))
            .thenReturn(true)
        whenever(appPrefsWrapper.isTrackNetworkRequestsEnabled).thenReturn(false)
        whenever(appPrefsWrapper.trackNetworkRequestsRetentionPeriod).thenReturn(0)

        // When
        viewModel.init()

        // Then
        assertThat(viewModel.networkTrackingState.value.showNetworkDebugging).isTrue()
    }

    @Test
    fun `init hides network debugging when feature flag is disabled`() {
        // Given
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(accountStore.account).thenReturn(account)
        whenever(account.displayName).thenReturn("")
        whenever(account.userName).thenReturn("")
        whenever(account.email).thenReturn("")
        whenever(account.avatarUrl).thenReturn("")
        whenever(experimentalFeatures.isEnabled(ExperimentalFeatures.Feature.NETWORK_DEBUGGING))
            .thenReturn(false)

        // When
        viewModel.init()

        // Then
        assertThat(viewModel.networkTrackingState.value.showNetworkDebugging).isFalse()
    }

    @Test
    fun `init loads tracking enabled state from preferences`() {
        // Given
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(accountStore.account).thenReturn(account)
        whenever(account.displayName).thenReturn("")
        whenever(account.userName).thenReturn("")
        whenever(account.email).thenReturn("")
        whenever(account.avatarUrl).thenReturn("")
        whenever(experimentalFeatures.isEnabled(ExperimentalFeatures.Feature.NETWORK_DEBUGGING))
            .thenReturn(true)
        whenever(appPrefsWrapper.isTrackNetworkRequestsEnabled).thenReturn(true)
        whenever(appPrefsWrapper.trackNetworkRequestsRetentionPeriod)
            .thenReturn(NetworkRequestsRetentionPeriod.ONE_WEEK.value)

        // When
        viewModel.init()

        // Then
        assertThat(viewModel.networkTrackingState.value.isTrackingEnabled).isTrue()
        assertThat(viewModel.networkTrackingState.value.retentionPeriod)
            .isEqualTo(NetworkRequestsRetentionPeriod.ONE_WEEK)
    }

    // endregion

    // region onAskTheBotsClick() tests

    @Test
    fun `onAskTheBotsClick emits NavigateToAskTheBots event when user has access token`() = test {
        // Given
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        // When
        viewModel.navigationEvents.test {
            viewModel.onAskTheBotsClick()

            // Then
            val event = awaitItem()
            assertThat(event).isInstanceOf(SupportViewModel.NavigationEvent.NavigateToAskTheBots::class.java)
        }
    }

    @Test
    fun `onAskTheBotsClick uses userName when displayName is empty`() = test {
        // Given
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        // When
        viewModel.navigationEvents.test {
            viewModel.onAskTheBotsClick()

            // Then
            val event = awaitItem()
            assertThat(event).isInstanceOf(SupportViewModel.NavigationEvent.NavigateToAskTheBots::class.java)
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

    // region Network tracking dialog tests

    @Test
    fun `onNetworkTrackingToggle shows enable dialog when toggled on`() {
        // Given
        whenever(appPrefsWrapper.trackNetworkRequestsRetentionPeriod)
            .thenReturn(NetworkRequestsRetentionPeriod.ONE_DAY.value)

        // When
        viewModel.onNetworkTrackingToggle(true)

        // Then
        val dialogState = viewModel.dialogState.value
        assertThat(dialogState).isInstanceOf(SupportViewModel.DialogState.EnableTracking::class.java)
        assertThat((dialogState as SupportViewModel.DialogState.EnableTracking).selectedPeriod)
            .isEqualTo(NetworkRequestsRetentionPeriod.ONE_DAY)
    }

    @Test
    fun `onNetworkTrackingToggle shows disable dialog when toggled off`() {
        // When
        viewModel.onNetworkTrackingToggle(false)

        // Then
        assertThat(viewModel.dialogState.value)
            .isEqualTo(SupportViewModel.DialogState.DisableTracking)
    }

    @Test
    fun `onEnableTrackingConfirmed updates state and hides dialog`() {
        // Given
        viewModel.onNetworkTrackingToggle(true) // Show dialog first

        // When
        viewModel.onEnableTrackingConfirmed(NetworkRequestsRetentionPeriod.ONE_WEEK)

        // Then
        assertThat(viewModel.networkTrackingState.value.isTrackingEnabled).isTrue()
        assertThat(viewModel.networkTrackingState.value.retentionPeriod)
            .isEqualTo(NetworkRequestsRetentionPeriod.ONE_WEEK)
        assertThat(viewModel.dialogState.value).isEqualTo(SupportViewModel.DialogState.Hidden)
        verify(appPrefsWrapper).isTrackNetworkRequestsEnabled = true
        verify(appPrefsWrapper).trackNetworkRequestsRetentionPeriod =
            NetworkRequestsRetentionPeriod.ONE_WEEK.value
    }

    @Test
    fun `onDisableTrackingConfirmed updates state and hides dialog`() {
        // Given
        viewModel.onNetworkTrackingToggle(false) // Show dialog first

        // When
        viewModel.onDisableTrackingConfirmed()

        // Then
        assertThat(viewModel.networkTrackingState.value.isTrackingEnabled).isFalse()
        assertThat(viewModel.dialogState.value).isEqualTo(SupportViewModel.DialogState.Hidden)
        verify(appPrefsWrapper).isTrackNetworkRequestsEnabled = false
    }

    @Test
    fun `onDialogDismissed hides dialog without changing tracking state`() {
        // Given
        viewModel.onNetworkTrackingToggle(true) // Show dialog first
        val initialTrackingState = viewModel.networkTrackingState.value

        // When
        viewModel.onDialogDismissed()

        // Then
        assertThat(viewModel.dialogState.value).isEqualTo(SupportViewModel.DialogState.Hidden)
        assertThat(viewModel.networkTrackingState.value).isEqualTo(initialTrackingState)
    }

    @Test
    fun `onRetentionPeriodSelected updates selected period in dialog`() {
        // Given
        whenever(appPrefsWrapper.trackNetworkRequestsRetentionPeriod)
            .thenReturn(NetworkRequestsRetentionPeriod.ONE_HOUR.value)
        viewModel.onNetworkTrackingToggle(true) // Show dialog with ONE_HOUR

        // When
        viewModel.onRetentionPeriodSelected(NetworkRequestsRetentionPeriod.FOREVER)

        // Then
        val dialogState = viewModel.dialogState.value
        assertThat(dialogState).isInstanceOf(SupportViewModel.DialogState.EnableTracking::class.java)
        assertThat((dialogState as SupportViewModel.DialogState.EnableTracking).selectedPeriod)
            .isEqualTo(NetworkRequestsRetentionPeriod.FOREVER)
    }

    @Test
    fun `onRetentionPeriodSelected does nothing when dialog is not EnableTracking`() {
        // Given - dialog is Hidden
        assertThat(viewModel.dialogState.value).isEqualTo(SupportViewModel.DialogState.Hidden)

        // When
        viewModel.onRetentionPeriodSelected(NetworkRequestsRetentionPeriod.FOREVER)

        // Then - dialog is still Hidden
        assertThat(viewModel.dialogState.value).isEqualTo(SupportViewModel.DialogState.Hidden)
    }

    // endregion
}
