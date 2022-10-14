package org.wordpress.android.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
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
import org.wordpress.android.test
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.JetpackResultActionData
import org.wordpress.android.ui.JetpackRemoteInstallViewState.Error
import org.wordpress.android.ui.JetpackRemoteInstallViewState.Installed
import org.wordpress.android.ui.JetpackRemoteInstallViewState.Installing
import org.wordpress.android.ui.JetpackRemoteInstallViewState.Start

@RunWith(MockitoJUnitRunner::class)
class JetpackRemoteInstallViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var jetpackStore: JetpackStore
    @Mock private lateinit var accountStore: AccountStore
    @Mock private lateinit var siteStore: SiteStore
    @Mock private lateinit var site: SiteModel
    private lateinit var actionCaptor: KArgumentCaptor<Action<Any>>
    private val siteId = 1

    private lateinit var viewModel: JetpackRemoteInstallViewModel
    private val viewStates = mutableListOf<JetpackRemoteInstallViewState>()
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
        viewModel.start(site, null)

        val startState = viewStates[0]
        assertStartState(startState)

        // Trigger install
        startState.onClick()

        val installingState = viewStates[1]
        assertInstallingState(installingState)

        verify(dispatcher).dispatch(actionCaptor.capture())
        assertThat(actionCaptor.lastValue.type).isEqualTo(JetpackAction.INSTALL_JETPACK)
        assertThat(actionCaptor.lastValue.payload).isEqualTo(site)
    }

    @Test
    fun `on successful result finishes jetpack install`() = test {
        val updatedSite = mock<SiteModel>()
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(updatedSite)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        viewModel.start(site, null)

        val startState = viewStates[0]
        assertStartState(startState)

        viewModel.onEventsUpdated(OnJetpackInstalled(true, JetpackAction.INSTALL_JETPACK))
        val installedState = viewStates[1]
        assertInstalledState(installedState)

        // Continue after Jetpack is installed
        installedState.onClick()

        val connectionData = jetpackResultActionData!!
        assertThat(connectionData.loggedIn).isTrue
        assertThat(connectionData.site == updatedSite).isTrue
        assertThat(connectionData.action == JetpackResultActionData.Action.CONNECT).isTrue
    }

    @Test
    fun `on error result shows failure`() = test {
        val installError = JetpackInstallError(JetpackInstallErrorType.GENERIC_ERROR, "error")
        viewModel.start(site, null)

        val startState = viewStates[0]
        assertStartState(startState)

        viewModel.onEventsUpdated(OnJetpackInstalled(installError, JetpackAction.INSTALL_JETPACK))

        val errorState = viewStates[1]
        assertErrorState(errorState)

        errorState.onClick()

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
        viewModel.start(site, null)

        val startState = viewStates[0]
        assertStartState(startState)

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

    private fun assertStartState(state: JetpackRemoteInstallViewState) {
        assertThat(state).isInstanceOf(Start::class.java)
        assertThat(state.type).isEqualTo(JetpackRemoteInstallViewState.Type.START)
        assertThat(state.titleResource).isEqualTo(R.string.install_jetpack)
        assertThat(state.messageResource).isEqualTo(R.string.install_jetpack_message)
        assertThat(state.icon).isEqualTo(R.drawable.ic_plans_white_24dp)
        assertThat(state.buttonResource).isEqualTo(R.string.install_jetpack_continue)
        assertThat(state.progressBarVisible).isEqualTo(false)
    }

    private fun assertInstallingState(state: JetpackRemoteInstallViewState) {
        assertThat(state).isInstanceOf(Installing::class.java)
        assertThat(state.type).isEqualTo(JetpackRemoteInstallViewState.Type.INSTALLING)
        assertThat(state.titleResource).isEqualTo(R.string.installing_jetpack)
        assertThat(state.messageResource).isEqualTo(R.string.installing_jetpack_message)
        assertThat(state.icon).isEqualTo(R.drawable.ic_plans_white_24dp)
        assertThat(state.buttonResource).isNull()
        assertThat(state.progressBarVisible).isEqualTo(true)
    }

    private fun assertInstalledState(state: JetpackRemoteInstallViewState) {
        assertThat(state).isInstanceOf(Installed::class.java)
        assertThat(state.type).isEqualTo(JetpackRemoteInstallViewState.Type.INSTALLED)
        assertThat(state.titleResource).isEqualTo(R.string.jetpack_installed)
        assertThat(state.messageResource).isEqualTo(R.string.jetpack_installed_message)
        assertThat(state.icon).isEqualTo(R.drawable.ic_plans_white_24dp)
        assertThat(state.buttonResource).isEqualTo(R.string.install_jetpack_continue)
        assertThat(state.progressBarVisible).isEqualTo(false)
    }

    private fun assertErrorState(state: JetpackRemoteInstallViewState) {
        assertThat(state).isInstanceOf(Error::class.java)
        assertThat(state.type).isEqualTo(JetpackRemoteInstallViewState.Type.ERROR)
        assertThat(state.titleResource).isEqualTo(R.string.jetpack_installation_problem)
        assertThat(state.messageResource).isEqualTo(R.string.jetpack_installation_problem_message)
        assertThat(state.icon).isEqualTo(R.drawable.img_illustration_info_outline_88dp)
        assertThat(state.buttonResource).isEqualTo(R.string.install_jetpack_retry)
        assertThat(state.progressBarVisible).isEqualTo(false)
    }
}
