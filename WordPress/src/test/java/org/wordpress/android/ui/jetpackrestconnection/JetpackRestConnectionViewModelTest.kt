package org.wordpress.android.ui.jetpackrestconnection

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.BuildConfig
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.WpAppNotifierHandler
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.jetpackrestconnection.JetpackRestConnectionViewModel.ButtonType
import org.wordpress.android.ui.jetpackrestconnection.JetpackRestConnectionViewModel.ConnectionStatus
import org.wordpress.android.ui.jetpackrestconnection.JetpackRestConnectionViewModel.ConnectionStep
import org.wordpress.android.ui.jetpackrestconnection.JetpackRestConnectionViewModel.ErrorType
import org.wordpress.android.ui.jetpackrestconnection.JetpackRestConnectionViewModel.StepState
import org.wordpress.android.ui.jetpackrestconnection.JetpackRestConnectionViewModel.UiEvent
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import uniffi.wp_api.PluginStatus

@ExperimentalCoroutinesApi
class JetpackRestConnectionViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var jetpackInstaller: JetpackInstaller

    @Mock
    lateinit var jetpackConnector: JetpackConnector

    @Mock
    lateinit var jetpackModuleHelper: JetpackStatsModuleHelper

    @Mock
    lateinit var appLogWrapper: AppLogWrapper

    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Mock
    lateinit var wpAppNotifierHandler: WpAppNotifierHandler

    @Mock
    lateinit var siteModel: SiteModel

    private lateinit var viewModel: JetpackRestConnectionViewModel

    companion object {
        private const val TEST_SITE_ID = 12345UL
        private const val TEST_USER_ID = 67890UL
        private const val TEST_ACCESS_TOKEN = "test_token"
        private const val VALID_JETPACK_VERSION = "14.3"   // Above JETPACK_LIMIT_VERSION
        private const val INVALID_JETPACK_VERSION = "14.0" // Below JETPACK_LIMIT_VERSION

        // Test timing constants
        private const val TEST_UI_DELAY_MS = 10L         // Fast UI delay for tests
        private const val TEST_ADVANCE_TIME_MS = 50L     // Time to advance for UI delay tests
        private const val TEST_STEP_TIMEOUT_MS = 100L    // Fast timeout for tests
        private const val TEST_TIMEOUT_ADVANCE_MS = 150L // Time to advance for timeout tests
    }

    @Before
    fun setup() {
        // Configure the site model to be using WpCom REST API
        whenever(siteModel.isUsingWpComRestApi).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)

        viewModel = JetpackRestConnectionViewModel(
            mainDispatcher = UnconfinedTestDispatcher(),
            bgDispatcher = UnconfinedTestDispatcher(),
            selectedSiteRepository = selectedSiteRepository,
            accountStore = accountStore,
            jetpackInstaller = jetpackInstaller,
            jetpackConnector = jetpackConnector,
            jetpackModuleHelper = jetpackModuleHelper,
            appLogWrapper = appLogWrapper,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            wpAppNotifierHandler = wpAppNotifierHandler,
        )

        // Override delays for faster tests
        viewModel.uiDelayMs = TEST_UI_DELAY_MS
        viewModel.stepTimeoutMs = TEST_STEP_TIMEOUT_MS
    }

    // Helper to assert step status
    private fun assertStepStatus(
        step: ConnectionStep,
        expectedStatus: ConnectionStatus,
        expectedError: ErrorType? = null
    ) {
        assertThat(viewModel.stepStates.value[step]?.status).isEqualTo(expectedStatus)
        expectedError?.let {
            assertThat(viewModel.stepStates.value[step]?.errorType).isEqualTo(it)
        }
    }

    @Test
    fun `onDoneClick sets Done UI event`() = runTest {
        viewModel.onDoneClick()

        assertThat(viewModel.uiEvent.value).isEqualTo(UiEvent.Done)
    }

    @Test
    fun `onCloseClick shows confirmation when connection is active`() = runTest {
        viewModel.onStartClick()
        viewModel.onCloseClick()

        assertThat(viewModel.uiEvent.value).isEqualTo(UiEvent.ShowCancelConfirmation)
    }

    @Test
    fun `onCloseClick closes immediately when connection is not active`() = runTest {
        viewModel.onCloseClick()

        assertThat(viewModel.uiEvent.value).isEqualTo(UiEvent.Close)
    }

    @Test
    fun `onCancelConfirmed sets Close UI event`() = runTest {
        viewModel.onCancelConfirmed()

        assertThat(viewModel.uiEvent.value).isEqualTo(UiEvent.Close)
    }


    @Test
    fun `onRetryClick retries from failed step`() = runTest {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(jetpackInstaller.installJetpack(any())).thenReturn(Result.failure(Exception("Failed")))

        viewModel.onStartClick()
        advanceTimeBy(TEST_ADVANCE_TIME_MS)

        assertThat(viewModel.stepStates.value[ConnectionStep.InstallJetpack]?.status)
            .isEqualTo(ConnectionStatus.Failed)

        whenever(jetpackInstaller.installJetpack(any())).thenReturn(Result.success(PluginStatus.ACTIVE))

        viewModel.onRetryClick()
        advanceTimeBy(TEST_ADVANCE_TIME_MS) // Need to advance time for retry to complete

        assertThat(viewModel.stepStates.value[ConnectionStep.InstallJetpack]?.status)
            .isEqualTo(ConnectionStatus.Completed)
    }

    @Test
    fun `loginWpCom step completes immediately when already logged in`() = runTest {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        viewModel.onStartClick()
        advanceTimeBy(TEST_ADVANCE_TIME_MS)

        assertThat(viewModel.stepStates.value[ConnectionStep.LoginWpCom]?.status)
            .isEqualTo(ConnectionStatus.Completed)
    }

    @Test
    fun `loginWpCom step triggers login when not logged in`() = runTest {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        viewModel.onStartClick()

        assertThat(viewModel.uiEvent.value).isEqualTo(UiEvent.StartWPComLogin)
        assertThat(viewModel.stepStates.value[ConnectionStep.LoginWpCom]?.status)
            .isEqualTo(ConnectionStatus.InProgress)
    }

    @Test
    fun `onWPComLoginCompleted with success completes login step`() = runTest {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        viewModel.onStartClick()
        viewModel.onWPComLoginCompleted(true)

        assertThat(viewModel.stepStates.value[ConnectionStep.LoginWpCom]?.status)
            .isEqualTo(ConnectionStatus.Completed)
    }

    @Test
    fun `onWPComLoginCompleted with failure marks login step as failed`() = runTest {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        viewModel.onStartClick()
        viewModel.onWPComLoginCompleted(false)

        assertThat(viewModel.stepStates.value[ConnectionStep.LoginWpCom]?.status)
            .isEqualTo(ConnectionStatus.Failed)
        assertThat(viewModel.stepStates.value[ConnectionStep.LoginWpCom]?.errorType)
            .isEqualTo(ErrorType.LoginWpComFailed)
    }

    @Test
    fun `installJetpack step succeeds with active plugin`() = runTest {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(jetpackInstaller.installJetpack(any())).thenReturn(Result.success(PluginStatus.ACTIVE))

        viewModel.onStartClick()
        advanceTimeBy(TEST_ADVANCE_TIME_MS)

        assertStepStatus(ConnectionStep.InstallJetpack, ConnectionStatus.Completed)
    }


    @Test
    fun `installJetpack step fails with inactive plugin`() = runTest {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(jetpackInstaller.installJetpack(any())).thenReturn(Result.success(PluginStatus.INACTIVE))

        viewModel.onStartClick()
        advanceTimeBy(TEST_ADVANCE_TIME_MS)

        assertStepStatus(ConnectionStep.InstallJetpack, ConnectionStatus.Failed, ErrorType.InstallJetpackInactive)
    }


    @Test
    fun `connectSite step succeeds and updates site ID`() = runTest {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(jetpackInstaller.installJetpack(any())).thenReturn(Result.success(PluginStatus.ACTIVE))
        whenever(jetpackConnector.connectSite(any())).thenReturn(Result.success(TEST_SITE_ID))

        viewModel.onStartClick()
        advanceTimeBy(TEST_ADVANCE_TIME_MS)

        verify(siteModel).siteId = TEST_SITE_ID.toLong()
        assertStepStatus(ConnectionStep.ConnectSite, ConnectionStatus.Completed)
    }


    @Test
    fun `connectUser step fails without access token`() = runTest {
        whenever(accountStore.hasAccessToken())
            .thenReturn(true) // Initial check for LoginWpCom
            .thenReturn(false) // Check in ConnectUser step
        whenever(jetpackInstaller.installJetpack(any())).thenReturn(Result.success(PluginStatus.ACTIVE))
        whenever(jetpackConnector.connectSite(any())).thenReturn(Result.success(TEST_SITE_ID))

        viewModel.onStartClick()
        advanceTimeBy(TEST_ADVANCE_TIME_MS)

        assertStepStatus(ConnectionStep.ConnectUser, ConnectionStatus.Failed, ErrorType.MissingAccessToken)
    }

    @Test
    fun `connectUser step succeeds with access token`() = runTest {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(TEST_ACCESS_TOKEN)
        whenever(jetpackInstaller.installJetpack(any())).thenReturn(Result.success(PluginStatus.ACTIVE))
        whenever(jetpackConnector.connectSite(any())).thenReturn(Result.success(TEST_SITE_ID))
        whenever(jetpackConnector.connectUser(any(), any())).thenReturn(Result.success(TEST_USER_ID))

        viewModel.onStartClick()
        advanceTimeBy(TEST_ADVANCE_TIME_MS)

        verify(jetpackConnector).connectUser(eq(siteModel), eq(TEST_ACCESS_TOKEN))
        assertStepStatus(ConnectionStep.ConnectUser, ConnectionStatus.Completed)
    }


    @Test
    fun `finalize step succeeds and completes step`() = runTest {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(TEST_ACCESS_TOKEN)
        whenever(jetpackInstaller.installJetpack(any())).thenReturn(Result.success(PluginStatus.ACTIVE))
        whenever(jetpackConnector.connectSite(any())).thenReturn(Result.success(TEST_SITE_ID))
        whenever(jetpackConnector.connectUser(any(), any())).thenReturn(Result.success(TEST_USER_ID))
        whenever(jetpackModuleHelper.activateStatsModule(any())).thenReturn(Result.success(Unit))

        viewModel.onStartClick()
        advanceTimeBy(TEST_ADVANCE_TIME_MS)

        assertStepStatus(ConnectionStep.Finalize, ConnectionStatus.Completed)
    }

    @Test
    fun `finalize step fails on exception`() = runTest {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(TEST_ACCESS_TOKEN)
        whenever(jetpackInstaller.installJetpack(any())).thenReturn(Result.success(PluginStatus.ACTIVE))
        whenever(jetpackConnector.connectSite(any())).thenReturn(Result.success(TEST_SITE_ID))
        whenever(jetpackConnector.connectUser(any(), any())).thenReturn(Result.success(TEST_USER_ID))
        whenever(jetpackModuleHelper.activateStatsModule(any())).thenReturn(Result.failure(Exception("Stats failed")))

        viewModel.onStartClick()
        advanceTimeBy(TEST_ADVANCE_TIME_MS)

        assertStepStatus(
            ConnectionStep.Finalize,
            ConnectionStatus.Failed,
            ErrorType.ActivateStatsFailed("Stats failed")
        )
    }

    @Test
    fun `onRequestedWithInvalidAuthentication resets and restarts flow`() = runTest {
        viewModel.onRequestedWithInvalidAuthentication("https://example.com")

        verify(wpAppNotifierHandler).removeListener(viewModel)
        verify(accountStore).resetAccessToken()
        assertThat(viewModel.currentStep.value).isEqualTo(ConnectionStep.LoginWpCom)
    }

    @Test
    fun `step timeout triggers timeout error`() = runTest {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(jetpackInstaller.installJetpack(any())).doSuspendableAnswer {
            delay(TEST_STEP_TIMEOUT_MS + 10L) // Longer than TEST_STEP_TIMEOUT_MS to trigger timeout
            Result.success(PluginStatus.ACTIVE)
        }

        viewModel.onStartClick()
        advanceTimeBy(TEST_TIMEOUT_ADVANCE_MS)
        advanceUntilIdle()

        assertStepStatus(ConnectionStep.InstallJetpack, ConnectionStatus.Failed, ErrorType.Timeout)
    }

    /**
     * The canInitiateJetpackRestConnection tests only run for the Jetpack app since that companion function
     * checks BuildConfig.IS_JETPACK_APP. This is a problem when CI runs tests for the WordPress app variant.
     */

    @Test
    fun `canInitiateJetpackRestConnection returns true for valid self-hosted site`() {
        if (BuildConfig.IS_JETPACK_APP) {
            val site = mock<SiteModel> {
                on { isUsingSelfHostedRestApi } doReturn true
                on { wpApiRestUrl } doReturn "https://example.com/wp-json"
                on { isJetpackConnected } doReturn false
                on { isJetpackInstalled } doReturn false
            }

            assertThat(JetpackRestConnectionViewModel.canInitiateJetpackRestConnection(site)).isTrue
        }
    }

    @Test
    fun `canInitiateJetpackRestConnection returns true for site with valid Jetpack version`() {
        if (BuildConfig.IS_JETPACK_APP) {
            val site = mock<SiteModel> {
                on { isUsingSelfHostedRestApi } doReturn true
                on { wpApiRestUrl } doReturn "https://example.com/wp-json"
                on { isJetpackConnected } doReturn false
                on { isJetpackInstalled } doReturn true
                on { jetpackVersion } doReturn VALID_JETPACK_VERSION
            }

            assertThat(JetpackRestConnectionViewModel.canInitiateJetpackRestConnection(site)).isTrue
        }
    }

    @Test
    fun `canInitiateJetpackRestConnection returns false for non-self-hosted site`() {
        if (BuildConfig.IS_JETPACK_APP) {
            val site = mock<SiteModel> {
                on { isUsingSelfHostedRestApi } doReturn false
            }

            assertThat(JetpackRestConnectionViewModel.canInitiateJetpackRestConnection(site)).isFalse
        }
    }

    @Test
    fun `canInitiateJetpackRestConnection returns false for already connected site`() {
        if (BuildConfig.IS_JETPACK_APP) {
            val site = mock<SiteModel> {
                on { isUsingSelfHostedRestApi } doReturn true
                on { wpApiRestUrl } doReturn "https://example.com/wp-json"
                on { isJetpackConnected } doReturn true
            }

            assertThat(JetpackRestConnectionViewModel.canInitiateJetpackRestConnection(site)).isFalse
        }
    }

    @Test
    fun `canInitiateJetpackRestConnection returns false for old Jetpack version`() {
        if (BuildConfig.IS_JETPACK_APP) {
            val site = mock<SiteModel> {
                on { isUsingSelfHostedRestApi } doReturn true
                on { wpApiRestUrl } doReturn "https://example.com/wp-json"
                on { isJetpackConnected } doReturn false
                on { isJetpackInstalled } doReturn true
                on { jetpackVersion } doReturn INVALID_JETPACK_VERSION
            }

            assertThat(JetpackRestConnectionViewModel.canInitiateJetpackRestConnection(site)).isFalse
        }
    }

    @Test
    fun `successful flow completion sets Done button and removes listener`() = runTest {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(TEST_ACCESS_TOKEN)
        whenever(jetpackInstaller.installJetpack(any())).thenReturn(Result.success(PluginStatus.ACTIVE))
        whenever(jetpackConnector.connectSite(any())).thenReturn(Result.success(TEST_SITE_ID))
        whenever(jetpackConnector.connectUser(any(), any())).thenReturn(Result.success(TEST_USER_ID))
        whenever(jetpackModuleHelper.activateStatsModule(any())).thenReturn(Result.success(Unit))

        viewModel.onStartClick()
        advanceTimeBy(TEST_ADVANCE_TIME_MS)

        assertThat(viewModel.buttonType.value).isEqualTo(ButtonType.Done)
        verify(wpAppNotifierHandler).removeListener(viewModel)
    }

    @Test
    fun `failed flow completion sets Retry button`() = runTest {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(jetpackInstaller.installJetpack(any())).thenReturn(Result.failure(Exception("Failed")))

        viewModel.onStartClick()
        advanceTimeBy(TEST_ADVANCE_TIME_MS)

        assertThat(viewModel.buttonType.value).isEqualTo(ButtonType.Retry)
    }

    @Test
    fun `step states are initialized correctly`() {
        val initialStates = viewModel.stepStates.value

        assertThat(initialStates).hasSize(5)
        assertThat(initialStates[ConnectionStep.LoginWpCom]).isEqualTo(StepState())
        assertThat(initialStates[ConnectionStep.InstallJetpack]).isEqualTo(StepState())
        assertThat(initialStates[ConnectionStep.ConnectSite]).isEqualTo(StepState())
        assertThat(initialStates[ConnectionStep.ConnectUser]).isEqualTo(StepState())
        assertThat(initialStates[ConnectionStep.Finalize]).isEqualTo(StepState())
    }
}
