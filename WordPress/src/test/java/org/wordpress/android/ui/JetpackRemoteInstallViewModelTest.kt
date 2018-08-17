package org.wordpress.android.ui

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNull
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.action.JetpackAction.INSTALL_JETPACK
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallError
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.JetpackStore.OnJetpackInstalled
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.JetpackConnectionData
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.ViewState
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.ViewState.Error
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.ViewState.Installed
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.ViewState.Installing
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.ViewState.Start

@RunWith(MockitoJUnitRunner::class)
class JetpackRemoteInstallViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var jetpackStore: JetpackStore
    @Mock private lateinit var accountStore: AccountStore
    @Mock private lateinit var siteStore: SiteStore
    @Mock private lateinit var site: SiteModel
    private val siteId = 1

    private lateinit var viewModel: JetpackRemoteInstallViewModel
    private val viewStates = mutableListOf<ViewState>()
    private var jetpackConnectionData: JetpackConnectionData? = null

    @Before
    fun setUp() {
        whenever(site.id).thenReturn(siteId)
        viewModel = JetpackRemoteInstallViewModel(jetpackStore, accountStore, siteStore)
        viewModel.liveViewState.observeForever { if (it != null) viewStates.add(it) }
        viewModel.liveJetpackConnectionFlow.observeForever { if (it != null) jetpackConnectionData = it }
    }

    @Test
    fun `on click starts and finishes jetpack install`() = runBlocking {
        whenever(jetpackStore.install(site)).thenReturn(OnJetpackInstalled(true, INSTALL_JETPACK))
        val updatedSite = mock<SiteModel>()
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(updatedSite)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        viewModel.start(site, null)

        val startState = awaitViewState(0)
        assertStartState(startState)

        //Trigger install
        startState.onClick()

        val installingState = awaitViewState(1)
        assertInstallingState(installingState)

        val installedState = awaitViewState(2)
        assertInstalledState(installedState)

        //Continue after Jetpack is installed
        installedState.onClick()

        val connectionData = awaitJetpackConnectionData()
        assertTrue(connectionData.loggedIn)
        assertTrue(connectionData.site == updatedSite)
    }

    @Test
    fun `on click starts jetpack install and shows error and retries`() = runBlocking {
        val installError = JetpackInstallError(GENERIC_ERROR, "error")
        whenever(jetpackStore.install(site)).thenReturn(
                OnJetpackInstalled(installError, INSTALL_JETPACK),
                OnJetpackInstalled(true, INSTALL_JETPACK)
        )
        val updatedSite = mock<SiteModel>()
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(updatedSite)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        viewModel.start(site, null)

        val startState = awaitViewState(0)
        assertStartState(startState)

        //Trigger install
        startState.onClick()

        val installingState = awaitViewState(1)
        assertInstallingState(installingState)

        val errorState = awaitViewState(2)
        assertErrorState(errorState)

        errorState.onClick()

        val reinstallingState = awaitViewState(3)
        assertInstallingState(reinstallingState)

        val installedState = awaitViewState(4)
        assertInstalledState(installedState)

        //Continue after Jetpack is installed
        installedState.onClick()

        val connectionData = awaitJetpackConnectionData()
        assertTrue(connectionData.loggedIn)
        assertTrue(connectionData.site == updatedSite)
    }

    @Test
    fun `on login retries jetpack connect with access token`() = runBlocking {
        assertNull(jetpackConnectionData)

        val updatedSite = mock<SiteModel>()
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(updatedSite)
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        viewModel.onLogin(siteId)

        val connectionData = awaitJetpackConnectionData()
        assertTrue(connectionData.loggedIn)
        assertTrue(connectionData.site == updatedSite)
    }

    private fun assertStartState(state: ViewState) {
        assertTrue(state is Start)
        assertEquals(state.type, ViewState.Type.START)
        assertEquals(state.titleResource, R.string.install_jetpack)
        assertEquals(state.messageResource, R.string.install_jetpack_message)
        assertEquals(state.icon, R.drawable.ic_jetpack_icon_green_88dp)
        assertEquals(state.buttonResource, R.string.install_jetpack_continue)
        assertEquals(state.progressBarVisible, false)
    }

    private fun assertInstallingState(state: ViewState) {
        assertTrue(state is Installing)
        assertEquals(state.type, ViewState.Type.INSTALLING)
        assertEquals(state.titleResource, R.string.installing_jetpack)
        assertEquals(state.messageResource, R.string.installing_jetpack_message)
        assertEquals(state.icon, R.drawable.ic_jetpack_icon_green_88dp)
        assertNull(state.buttonResource)
        assertEquals(state.progressBarVisible, true)
    }

    private fun assertInstalledState(state: ViewState) {
        assertTrue(state is Installed)
        assertEquals(state.type, ViewState.Type.INSTALLED)
        assertEquals(state.titleResource, R.string.jetpack_installed)
        assertEquals(state.messageResource, R.string.jetpack_installed_message)
        assertEquals(state.icon, R.drawable.ic_jetpack_icon_green_88dp)
        assertEquals(state.buttonResource, R.string.install_jetpack_continue)
        assertEquals(state.progressBarVisible, false)
    }

    private fun assertErrorState(state: ViewState) {
        assertTrue(state is Error)
        assertEquals(state.type, ViewState.Type.ERROR)
        assertEquals(state.titleResource, R.string.jetpack_installation_problem)
        assertEquals(state.messageResource, R.string.jetpack_installation_problem_message)
        assertEquals(state.icon, R.drawable.ic_exclamation_mark_88dp)
        assertEquals(state.buttonResource, R.string.install_jetpack_retry)
        assertEquals(state.progressBarVisible, false)
    }

    private suspend fun awaitViewState(position: Int): ViewState {
        var counter = 0
        val max = 10
        while (counter < max && viewStates.size <= position) {
            delay(50)
            counter++
        }
        assertTrue(viewStates.size > position)
        return viewStates[position]
    }

    private suspend fun awaitJetpackConnectionData(): JetpackConnectionData {
        var counter = 0
        val max = 10
        while (counter < max && jetpackConnectionData == null) {
            delay(50)
            counter++
        }
        assertTrue(jetpackConnectionData != null)
        return jetpackConnectionData!!
    }
}
