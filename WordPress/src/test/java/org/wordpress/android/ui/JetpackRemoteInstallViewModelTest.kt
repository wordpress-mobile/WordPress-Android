package org.wordpress.android.ui

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockito_kotlin.KArgumentCaptor
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNull
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.JetpackAction
import org.wordpress.android.fluxc.action.JetpackAction.INSTALL_JETPACK
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallError
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.JetpackStore.OnJetpackInstalled
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.JetpackConnectionData
import org.wordpress.android.ui.JetpackRemoteInstallViewState.Error
import org.wordpress.android.ui.JetpackRemoteInstallViewState.Installed
import org.wordpress.android.ui.JetpackRemoteInstallViewState.Installing
import org.wordpress.android.ui.JetpackRemoteInstallViewState.Start

@RunWith(MockitoJUnitRunner::class)
class JetpackRemoteInstallViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var accountStore: AccountStore
    @Mock private lateinit var siteStore: SiteStore
    @Mock private lateinit var site: SiteModel
    private lateinit var actionCaptor: KArgumentCaptor<Action<Any>>
    private val siteId = 1

    private lateinit var viewModel: JetpackRemoteInstallViewModel
    private val viewStates = mutableListOf<JetpackRemoteInstallViewState>()
    private var jetpackConnectionData: JetpackConnectionData? = null

    @Before
    fun setUp() {
        whenever(site.id).thenReturn(siteId)
        viewModel = JetpackRemoteInstallViewModel(dispatcher, accountStore, siteStore)
        viewModel.liveViewState.observeForever { if (it != null) viewStates.add(it) }
        viewModel.liveJetpackConnectionFlow.observeForever { if (it != null) jetpackConnectionData = it }
        actionCaptor = argumentCaptor()
    }

    @Test
    fun `on click starts jetpack install`() = runBlocking {
        viewModel.start(site, null)

        val startState = viewStates[0]
        assertStartState(startState)

        // Trigger install
        startState.onClick()

        val installingState = viewStates[1]
        assertInstallingState(installingState)

        verify(dispatcher).dispatch(actionCaptor.capture())
        assertEquals(actionCaptor.lastValue.type, JetpackAction.INSTALL_JETPACK)
        assertEquals(actionCaptor.lastValue.payload, site)
    }

    @Test
    fun `on successful result finishes jetpack install`() = runBlocking {
        val updatedSite = mock<SiteModel>()
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(updatedSite)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        viewModel.start(site, null)

        val startState = viewStates[0]
        assertStartState(startState)

        viewModel.onEventsUpdated(OnJetpackInstalled(true, INSTALL_JETPACK))
        val installedState = viewStates[1]
        assertInstalledState(installedState)

        // Continue after Jetpack is installed
        installedState.onClick()

        val connectionData = jetpackConnectionData!!
        assertTrue(connectionData.loggedIn)
        assertTrue(connectionData.site == updatedSite)
    }

    @Test
    fun `on error result shows failure`() = runBlocking {
        val installError = JetpackInstallError(GENERIC_ERROR, "error")
        viewModel.start(site, null)

        val startState = viewStates[0]
        assertStartState(startState)

        viewModel.onEventsUpdated(OnJetpackInstalled(installError, INSTALL_JETPACK))

        val errorState = viewStates[1]
        assertErrorState(errorState)

        errorState.onClick()

        verify(dispatcher).dispatch(actionCaptor.capture())
        assertEquals(actionCaptor.lastValue.type, JetpackAction.INSTALL_JETPACK)
        assertEquals(actionCaptor.lastValue.payload, site)
    }

    @Test
    fun `on login retries jetpack connect with access token`() = runBlocking {
        assertNull(jetpackConnectionData)

        val updatedSite = mock<SiteModel>()
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(updatedSite)
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        viewModel.onLogin(siteId)

        val connectionData = jetpackConnectionData!!
        assertTrue(connectionData.loggedIn)
        assertTrue(connectionData.site == updatedSite)
    }

    private fun assertStartState(state: JetpackRemoteInstallViewState) {
        assertTrue(state is Start)
        assertEquals(state.type, JetpackRemoteInstallViewState.Type.START)
        assertEquals(state.titleResource, R.string.install_jetpack)
        assertEquals(state.messageResource, R.string.install_jetpack_message)
        assertEquals(state.icon, R.drawable.ic_jetpack_icon_green_88dp)
        assertEquals(state.buttonResource, R.string.install_jetpack_continue)
        assertEquals(state.progressBarVisible, false)
    }

    private fun assertInstallingState(state: JetpackRemoteInstallViewState) {
        assertTrue(state is Installing)
        assertEquals(state.type, JetpackRemoteInstallViewState.Type.INSTALLING)
        assertEquals(state.titleResource, R.string.installing_jetpack)
        assertEquals(state.messageResource, R.string.installing_jetpack_message)
        assertEquals(state.icon, R.drawable.ic_jetpack_icon_green_88dp)
        assertNull(state.buttonResource)
        assertEquals(state.progressBarVisible, true)
    }

    private fun assertInstalledState(state: JetpackRemoteInstallViewState) {
        assertTrue(state is Installed)
        assertEquals(state.type, JetpackRemoteInstallViewState.Type.INSTALLED)
        assertEquals(state.titleResource, R.string.jetpack_installed)
        assertEquals(state.messageResource, R.string.jetpack_installed_message)
        assertEquals(state.icon, R.drawable.ic_jetpack_icon_green_88dp)
        assertEquals(state.buttonResource, R.string.install_jetpack_continue)
        assertEquals(state.progressBarVisible, false)
    }

    private fun assertErrorState(state: JetpackRemoteInstallViewState) {
        assertTrue(state is Error)
        assertEquals(state.type, JetpackRemoteInstallViewState.Type.ERROR)
        assertEquals(state.titleResource, R.string.jetpack_installation_problem)
        assertEquals(state.messageResource, R.string.jetpack_installation_problem_message)
        assertEquals(state.icon, R.drawable.ic_exclamation_mark_88dp)
        assertEquals(state.buttonResource, R.string.install_jetpack_retry)
        assertEquals(state.progressBarVisible, false)
    }
}
