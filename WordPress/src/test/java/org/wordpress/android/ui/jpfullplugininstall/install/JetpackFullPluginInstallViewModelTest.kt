package org.wordpress.android.ui.jpfullplugininstall.install

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PluginAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PluginStore
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.mysite.SelectedSiteRepository

@ExperimentalCoroutinesApi
class JetpackFullPluginInstallViewModelTest : BaseUnitTest() {
    private val uiStateMapper: JetpackFullPluginInstallUiStateMapper = mock()
    private val selectedSiteRepository: SelectedSiteRepository = mock()
    private val pluginStore: PluginStore = mock()
    private val dispatcher: Dispatcher = mock()
    private lateinit var actionCaptor: KArgumentCaptor<Action<Any>>
    private val classToTest = JetpackFullPluginInstallViewModel(
        uiStateMapper = uiStateMapper,
        selectedSiteRepository = selectedSiteRepository,
        bgDispatcher = testDispatcher(),
        pluginStore,
        dispatcher
    )

    private val selectedSite = SiteModel().apply {
        id = 123
        name = "Site name"
    }

    @Before
    fun setUp() {
        actionCaptor = argumentCaptor()
    }

    @Test
    fun `Should have UI state initial value as Initial`() {
        whenever(uiStateMapper.mapInitial()).thenReturn(UiState.Initial(0))
        val initialMockedClassToTest = JetpackFullPluginInstallViewModel(
            uiStateMapper = uiStateMapper,
            selectedSiteRepository = selectedSiteRepository,
            bgDispatcher = testDispatcher(),
            pluginStore,
            dispatcher
        )
        assertThat(initialMockedClassToTest.uiState.value).isInstanceOf(UiState.Initial::class.java)
    }

    @Test
    fun `Should post Installing UI state and dispatch install action when onContinueClick is called`() {
        mockSelectedSite(selectedSite)
        whenever(uiStateMapper.mapInstalling()).thenReturn(UiState.Installing)
        classToTest.onContinueClick()
        assertThat(classToTest.uiState.value).isInstanceOf(UiState.Installing::class.java)
        verify(dispatcher, times(1)).dispatch(actionCaptor.capture())
        assertThat(actionCaptor.lastValue.type).isEqualTo(PluginAction.INSTALL_JP_FOR_INDIVIDUAL_PLUGIN_SITE)
    }

    @Test
    fun `Should post Installing UI state when onRetryClick is called`() {
        mockSelectedSite(selectedSite)
        whenever(uiStateMapper.mapInstalling()).thenReturn(UiState.Installing)
        classToTest.onRetryClick()
        assertThat(classToTest.uiState.value).isInstanceOf(UiState.Installing::class.java)
        verify(dispatcher, times(1)).dispatch(actionCaptor.capture())
        assertThat(actionCaptor.lastValue.type).isEqualTo(PluginAction.INSTALL_JP_FOR_INDIVIDUAL_PLUGIN_SITE)
    }

    @Test
    fun `Should post action Dismiss when onDismissScreenClick is called`() = test {
        val result = ArrayList<ActionEvent>()
        val job = launch {
            classToTest.actionEvents.collectLatest {
                result.add(it)
            }
        }
        classToTest.onDismissScreenClick()
        assertThat(result.first()).isEqualTo(ActionEvent.Dismiss)
        job.cancel()
    }

    @Test
    fun `Should post action Dismiss when onDoneClick is called`() = test {
        val result = ArrayList<ActionEvent>()
        val job = launch {
            classToTest.actionEvents.collectLatest {
                result.add(it)
            }
        }
        classToTest.onDoneClick()
        assertThat(result.first()).isEqualTo(ActionEvent.Dismiss)
        job.cancel()
    }

    @Test
    fun `Should post action ContactSupport when onContactSupportClick is called`() = test {
        mockSelectedSite(selectedSite)
        val result = ArrayList<ActionEvent>()
        val job = launch {
            classToTest.actionEvents.collectLatest {
                result.add(it)
            }
        }
        classToTest.onContactSupportClick()
        assertThat(result.first()).isEqualTo(
            ActionEvent.ContactSupport(
                origin = HelpActivity.Origin.JETPACK_INSTALL_FULL_PLUGIN_ERROR,
                selectedSite = selectedSiteRepository.getSelectedSite(),
            )
        )
        job.cancel()
    }

    private fun mockSelectedSite(selectedSite: SiteModel) {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(selectedSite)
    }
}
