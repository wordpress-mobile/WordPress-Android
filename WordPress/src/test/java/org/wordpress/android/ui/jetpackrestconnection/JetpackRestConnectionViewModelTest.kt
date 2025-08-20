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
    }

    @Before
    fun setup() {
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
        advanceTimeBy(1100)

        assertThat(viewModel.stepStates.value[ConnectionStep.InstallJetpack]?.status)
            .isEqualTo(ConnectionStatus.Failed)

        whenever(jetpackInstaller.installJetpack(any())).thenReturn(Result.success(PluginStatus.ACTIVE))

        viewModel.onRetryClick()

        assertThat(viewModel.stepStates.value[ConnectionStep.InstallJetpack]?.status)
            .isEqualTo(ConnectionStatus.Completed)
    }

    @Test
    fun `loginWpCom step completes immediately when already logged in`() = runTest {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        viewModel.onStartClick()
        advanceUntilIdle()

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
        advanceUntilIdle()

        assertThat(viewModel.stepStates.value[ConnectionStep.InstallJetpack]?.status)
            .isEqualTo(ConnectionStatus.Completed)
    }


    @Test
    fun `installJetpack step fails with inactive plugin`() = runTest {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(jetpackInstaller.installJetpack(any())).thenReturn(Result.success(PluginStatus.INACTIVE))

        viewModel.onStartClick()
        advanceTimeBy(1100)

        assertThat(viewModel.stepStates.value[ConnectionStep.InstallJetpack]?.status)
            .isEqualTo(ConnectionStatus.Failed)
        assertThat(viewModel.stepStates.value[ConnectionStep.InstallJetpack]?.errorType)
            .isEqualTo(ErrorType.InstallJetpackInactive)
    }


    @Test
    fun `connectSite step succeeds and updates site ID`() = runTest {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(jetpackInstaller.installJetpack(any())).thenReturn(Result.success(PluginStatus.ACTIVE))
        whenever(jetpackConnector.connectSite(any())).thenReturn(Result.success(TEST_SITE_ID))

        viewModel.onStartClick()
        advanceUntilIdle()

        verify(siteModel).siteId = TEST_SITE_ID.toLong()
        assertThat(viewModel.stepStates.value[ConnectionStep.ConnectSite]?.status)
            .isEqualTo(ConnectionStatus.Completed)
    }


    @Test
    fun `connectUser step fails without access token`() = runTest {
        whenever(accountStore.hasAccessToken())
            .thenReturn(true) // Initial check for LoginWpCom
            .thenReturn(false) // Check in ConnectUser step
        whenever(jetpackInstaller.installJetpack(any())).thenReturn(Result.success(PluginStatus.ACTIVE))
        whenever(jetpackConnector.connectSite(any())).thenReturn(Result.success(TEST_SITE_ID))

        viewModel.onStartClick()
        advanceUntilIdle()

        assertThat(viewModel.stepStates.value[ConnectionStep.ConnectUser]?.status)
            .isEqualTo(ConnectionStatus.Failed)
        assertThat(viewModel.stepStates.value[ConnectionStep.ConnectUser]?.errorType)
            .isEqualTo(ErrorType.MissingAccessToken)
    }

    @Test
    fun `connectUser step succeeds with access token`() = runTest {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(TEST_ACCESS_TOKEN)
        whenever(jetpackInstaller.installJetpack(any())).thenReturn(Result.success(PluginStatus.ACTIVE))
        whenever(jetpackConnector.connectSite(any())).thenReturn(Result.success(TEST_SITE_ID))
        whenever(jetpackConnector.connectUser(any(), any())).thenReturn(Result.success(TEST_USER_ID))

        viewModel.onStartClick()
        advanceUntilIdle()

        verify(jetpackConnector).connectUser(eq(siteModel), eq(TEST_ACCESS_TOKEN))
        assertThat(viewModel.stepStates.value[ConnectionStep.ConnectUser]?.status)
            .isEqualTo(ConnectionStatus.Completed)
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
        advanceTimeBy(1100)

        assertThat(viewModel.stepStates.value[ConnectionStep.Finalize]?.status)
            .isEqualTo(ConnectionStatus.Completed)
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
        advanceTimeBy(1100)

        assertThat(viewModel.stepStates.value[ConnectionStep.Finalize]?.status)
            .isEqualTo(ConnectionStatus.Failed)
        assertThat(viewModel.stepStates.value[ConnectionStep.Finalize]?.errorType)
            .isEqualTo(ErrorType.ActivateStatsFailed)
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
            delay(50000)
            Result.success(PluginStatus.ACTIVE)
        }

        viewModel.onStartClick()
        advanceTimeBy(46000)
        advanceUntilIdle()

        assertThat(viewModel.stepStates.value[ConnectionStep.InstallJetpack]?.status)
            .isEqualTo(ConnectionStatus.Failed)
        assertThat(viewModel.stepStates.value[ConnectionStep.InstallJetpack]?.errorType)
            .isEqualTo(ErrorType.Timeout)
    }

    @Test
    fun `canInitiateJetpackRestConnection returns true for valid self-hosted site`() {
        val site = mock<SiteModel> {
            on { isUsingSelfHostedRestApi } doReturn true
            on { wpApiRestUrl } doReturn "https://example.com/wp-json"
            on { isJetpackConnected } doReturn false
            on { isJetpackInstalled } doReturn false
        }

        assertThat(JetpackRestConnectionViewModel.canInitiateJetpackRestConnection(site)).isTrue
    }

    @Test
    fun `canInitiateJetpackRestConnection returns true for site with valid Jetpack version`() {
        val site = mock<SiteModel> {
            on { isUsingSelfHostedRestApi } doReturn true
            on { wpApiRestUrl } doReturn "https://example.com/wp-json"
            on { isJetpackConnected } doReturn false
            on { isJetpackInstalled } doReturn true
            on { jetpackVersion } doReturn "14.3"
        }

        assertThat(JetpackRestConnectionViewModel.canInitiateJetpackRestConnection(site)).isTrue
    }

    @Test
    fun `canInitiateJetpackRestConnection returns false for non-self-hosted site`() {
        val site = mock<SiteModel> {
            on { isUsingSelfHostedRestApi } doReturn false
        }

        assertThat(JetpackRestConnectionViewModel.canInitiateJetpackRestConnection(site)).isFalse
    }

    @Test
    fun `canInitiateJetpackRestConnection returns false for already connected site`() {
        val site = mock<SiteModel> {
            on { isUsingSelfHostedRestApi } doReturn true
            on { wpApiRestUrl } doReturn "https://example.com/wp-json"
            on { isJetpackConnected } doReturn true
        }

        assertThat(JetpackRestConnectionViewModel.canInitiateJetpackRestConnection(site)).isFalse
    }

    @Test
    fun `canInitiateJetpackRestConnection returns false for old Jetpack version`() {
        val site = mock<SiteModel> {
            on { isUsingSelfHostedRestApi } doReturn true
            on { wpApiRestUrl } doReturn "https://example.com/wp-json"
            on { isJetpackConnected } doReturn false
            on { isJetpackInstalled } doReturn true
            on { jetpackVersion } doReturn "14.0"
        }

        assertThat(JetpackRestConnectionViewModel.canInitiateJetpackRestConnection(site)).isFalse
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
        advanceTimeBy(1100)

        assertThat(viewModel.buttonType.value).isEqualTo(ButtonType.Done)
        verify(wpAppNotifierHandler).removeListener(viewModel)
    }

    @Test
    fun `failed flow completion sets Retry button`() = runTest {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(jetpackInstaller.installJetpack(any())).thenReturn(Result.failure(Exception("Failed")))

        viewModel.onStartClick()
        advanceTimeBy(1100)

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
