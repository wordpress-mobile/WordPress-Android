package org.wordpress.android.ui.jetpackoverlay.individualplugin

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.testCollect
import org.wordpress.android.ui.jetpackoverlay.individualplugin.WPJetpackIndividualPluginViewModel.ActionEvent
import org.wordpress.android.ui.jetpackoverlay.individualplugin.WPJetpackIndividualPluginViewModel.UiState

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class WPJetpackIndividualPluginViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var helper: WPJetpackIndividualPluginHelper

    private lateinit var viewModel: WPJetpackIndividualPluginViewModel

    @Before
    fun setUp() {
        viewModel = WPJetpackIndividualPluginViewModel(helper, testDispatcher())
    }

    @Test
    fun `WHEN onScreenShown THEN UI state is updated only once`() = test {
        whenever(helper.getJetpackConnectedSitesWithIndividualPlugins()).thenReturn(connectedSites)
        val uiStates = viewModel.uiState.testCollect(this) {
            viewModel.onScreenShown()
            viewModel.onScreenShown()
            viewModel.onScreenShown()
        }

        assertThat(uiStates).hasSize(2)
        assertThat(uiStates[0]).isEqualTo(UiState.None)
        assertThat(uiStates[1]).isEqualTo(UiState.Loaded(connectedSites))
    }

    @Test
    fun `WHEN onScreenShown THEN overlay shown helper method is called only once`() = test {
        whenever(helper.getJetpackConnectedSitesWithIndividualPlugins()).thenReturn(connectedSites)
        viewModel.onScreenShown()
        viewModel.onScreenShown()
        viewModel.onScreenShown()

        verify(helper).onJetpackIndividualPluginOverlayShown()
    }

    @Test
    fun `WHEN onDismissScreenClick THEN emit appropriate event`() = test {
        val result = viewModel.actionEvents.testCollect(this) {
            viewModel.onDismissScreenClick()
        }

        assertThat(result).hasSize(1)
        assertThat(result.first()).isEqualTo(ActionEvent.Dismiss)
    }

    @Test
    fun `WHEN onPrimaryButtonClick THEN emit appropriate event`() = test {
        val result = viewModel.actionEvents.testCollect(this) {
            viewModel.onPrimaryButtonClick()
        }

        assertThat(result).hasSize(1)
        assertThat(result.first()).isEqualTo(ActionEvent.PrimaryButtonClick)
    }

    companion object {
        private val connectedSites = listOf(
            SiteWithIndividualJetpackPlugins(
                name = "Site 1",
                url = "site1.wordpress.com",
                individualPluginNames = listOf("Jetpack Social")
            ),
        )
    }
}
