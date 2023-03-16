package org.wordpress.android.ui.jetpackplugininstall.remoteplugin

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.JetpackAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallError
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallErrorType
import org.wordpress.android.fluxc.store.JetpackStore.OnJetpackInstalled
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.JetpackConnectionSource
import org.wordpress.android.ui.JetpackConnectionUtils
import org.wordpress.android.ui.jetpackplugininstall.install.UiState
import org.wordpress.android.ui.jetpackplugininstall.remoteplugin.JetpackRemoteInstallViewModel.JetpackResultActionData

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class JetpackRemoteInstallViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var dispatcher: Dispatcher

    @Mock
    private lateinit var jetpackStore: JetpackStore

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var siteStore: SiteStore

    @Mock
    private lateinit var site: SiteModel
    private lateinit var actionCaptor: KArgumentCaptor<Action<Any>>
    private val siteId = 1

    private lateinit var viewModel: JetpackRemoteInstallViewModel
    private val viewStates = mutableListOf<UiState>()
    private var jetpackResultActionData: JetpackResultActionData? = null

    @Before
    fun setUp() {
        whenever(site.id).thenReturn(siteId)
        viewModel = JetpackRemoteInstallViewModel(dispatcher, accountStore, siteStore, jetpackStore)
        viewModel.liveViewState.observeForever { if (it != null) viewStates.add(it) }
        viewModel.liveActionOnResult.observeForever { if (it != null) jetpackResultActionData = it }
        actionCaptor = argumentCaptor()
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(site)
        whenever(accountStore.hasAccessToken()).thenReturn(false)
    }

    @Test
    fun `on click starts jetpack install`() = test {
        viewModel.initialize(site, null)

        val initialState = viewStates[0]
        initialState.assertInitialState()

        // Trigger install
        viewModel.onInitialButtonClick()

        val installingState = viewStates[1]
        installingState.assertInstallingState()

        verify(dispatcher).dispatch(actionCaptor.capture())
        assertThat(actionCaptor.lastValue.type).isEqualTo(JetpackAction.INSTALL_JETPACK)
        assertThat(actionCaptor.lastValue.payload).isEqualTo(site)
    }

    @Test
    fun `on successful result finishes jetpack install`() = test {
        val updatedSite = mock<SiteModel>()
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(updatedSite)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        viewModel.initialize(site, null)

        val initialState = viewStates[0]
        initialState.assertInitialState()

        viewModel.onEventsUpdated(OnJetpackInstalled(true, JetpackAction.INSTALL_JETPACK))
        val installedState = viewStates[1]
        installedState.assertDoneState()

        // Continue after Jetpack is installed
        viewModel.onDoneButtonClick()

        val connectionData = jetpackResultActionData!!
        assertThat(connectionData.loggedIn).isTrue
        assertThat(connectionData.site == updatedSite).isTrue
        assertThat(connectionData.action == JetpackResultActionData.Action.CONNECT).isTrue
    }

    @Test
    fun `on error result shows failure`() = test {
        val installError = JetpackInstallError(JetpackInstallErrorType.GENERIC_ERROR, "error")
        viewModel.initialize(site, null)

        val initialState = viewStates[0]
        initialState.assertInitialState()

        viewModel.onEventsUpdated(OnJetpackInstalled(installError, JetpackAction.INSTALL_JETPACK))

        val errorState = viewStates[1]
        errorState.assertErrorState()

        viewModel.onRetryButtonClick()

        verify(dispatcher).dispatch(actionCaptor.capture())
        assertThat(actionCaptor.lastValue.type).isEqualTo(JetpackAction.INSTALL_JETPACK)
        assertThat(actionCaptor.lastValue.payload).isEqualTo(site)
    }

    @Test
    fun `on invalid credentials triggers manual install`() = test {
        val installError = JetpackInstallError(
            JetpackInstallErrorType.GENERIC_ERROR,
            "INVALID_CREDENTIALS",
            message = "msg"
        )
        viewModel.initialize(site, null)

        val initialState = viewStates[0]
        initialState.assertInitialState()

        viewModel.onEventsUpdated(OnJetpackInstalled(installError, JetpackAction.INSTALL_JETPACK))

        val connectionData = jetpackResultActionData!!
        assertThat(connectionData.action == JetpackResultActionData.Action.MANUAL_INSTALL).isTrue
        assertThat(connectionData.site == site).isTrue
    }

    @Test
    fun `on login retries jetpack connect with access token`() = test {
        assertThat(jetpackResultActionData).isNull()

        val updatedSite = mock<SiteModel>()
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(updatedSite)
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        viewModel.onLogin(siteId)

        val connectionData = jetpackResultActionData!!
        assertThat(connectionData.loggedIn).isTrue
        assertThat(connectionData.site == updatedSite).isTrue
    }

    @Test
    fun `calling isBackButtonEnabled corresponds to expectations`() {
        // for START type, back button should be enabled
        val type = JetpackRemoteInstallViewModel.Type.START
        viewModel.initialize(site, type)
        assertThat(viewModel.isBackButtonEnabled()).isTrue

        // for INSTALLING type, back button should be disabled
        viewModel.onInitialButtonClick()
        assertThat(viewModel.isBackButtonEnabled()).isFalse

        // for ERROR type, back button should be enabled
        viewModel.onEventsUpdated(
            OnJetpackInstalled(
                JetpackInstallError(JetpackInstallErrorType.GENERIC_ERROR, "error"),
                JetpackAction.INSTALL_JETPACK
            )
        )
        assertThat(viewModel.isBackButtonEnabled()).isTrue

        // for INSTALLED type, back button should be disabled
        viewModel.onRetryButtonClick()
        assertThat(viewModel.isBackButtonEnabled()).isFalse
    }

    @Test
    fun `calling onBackPressed tracks install cancellation`() {
        val mockUtils = Mockito.mockStatic(JetpackConnectionUtils::class.java)

        val source = JetpackConnectionSource.STATS
        viewModel.onBackPressed(source)
        mockUtils.verify { JetpackConnectionUtils.trackWithSource(Stat.INSTALL_JETPACK_CANCELLED, source) }
    }

    private fun UiState.assertInitialState() {
        assertThat(this).isInstanceOf(UiState.Initial::class.java)

        with(this as UiState.Initial) {
            assertThat(buttonText).isEqualTo(R.string.jetpack_plugin_install_initial_button)
        }
    }

    private fun UiState.assertInstallingState() {
        assertThat(this).isInstanceOf(UiState.Installing::class.java)
    }

    private fun UiState.assertDoneState() {
        assertThat(this).isInstanceOf(UiState.Done::class.java)

        with(this as UiState.Done) {
            assertThat(description).isEqualTo(R.string.jetpack_plugin_install_remote_plugin_done_description)
            assertThat(buttonText).isEqualTo(R.string.jetpack_plugin_install_remote_plugin_done_button)
        }
    }

    private fun UiState.assertErrorState() {
        assertThat(this).isInstanceOf(UiState.Error::class.java)

        with(this as UiState.Error) {
            assertThat(retryButtonText).isEqualTo(R.string.jetpack_plugin_install_error_button_retry)
            assertThat(contactSupportButtonText)
                .isEqualTo(R.string.jetpack_plugin_install_error_button_contact_support)
        }
    }
}
