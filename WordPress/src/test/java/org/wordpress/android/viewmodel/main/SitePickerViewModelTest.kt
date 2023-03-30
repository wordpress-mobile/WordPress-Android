package org.wordpress.android.viewmodel.main

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.jetpackoverlay.individualplugin.WPJetpackIndividualPluginHelper
import org.wordpress.android.ui.main.SitePickerAdapter.SiteRecord
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.main.SitePickerViewModel.Action
import org.wordpress.android.viewmodel.main.SitePickerViewModel.Action.AskForSiteSelection
import org.wordpress.android.viewmodel.main.SitePickerViewModel.Action.ContinueReblogTo
import org.wordpress.android.viewmodel.main.SitePickerViewModel.Action.NavigateToState
import org.wordpress.android.viewmodel.main.SitePickerViewModel.Action.ShowJetpackIndividualPluginOverlay
import org.wordpress.android.viewmodel.main.SitePickerViewModel.NavigateState.TO_NO_SITE_SELECTED
import org.wordpress.android.viewmodel.main.SitePickerViewModel.NavigateState.TO_SITE_SELECTED

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SitePickerViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: SitePickerViewModel

    @Mock
    private lateinit var siteRecord: SiteRecord

    @Mock
    private lateinit var wpJetpackIndividualPluginHelper: WPJetpackIndividualPluginHelper

    @Before
    fun setUp() {
        viewModel = SitePickerViewModel(wpJetpackIndividualPluginHelper)
    }

    @Test
    fun `when a site is selected then navigate to site selected is emitted`() {
        var result: Event<Action>? = null

        viewModel.onActionTriggered.observeForever { result = it }
        viewModel.onSiteForReblogSelected(siteRecord)

        assertThat(result!!.peekContent()).isEqualTo(NavigateToState(TO_SITE_SELECTED, siteRecord))
    }

    @Test
    fun `when continue is tapped then ContinueReblogTo is emitted`() {
        var result: Event<Action>? = null

        viewModel.onActionTriggered.observeForever { result = it }
        viewModel.onSiteForReblogSelected(siteRecord)
        viewModel.onContinueFlowSelected()

        assertThat(result!!.peekContent()).isInstanceOf(ContinueReblogTo::class.java)
        assertThat((result!!.peekContent() as ContinueReblogTo).siteForReblog).isEqualTo(siteRecord)
    }

    @Test
    fun `when continue is tapped but no site was selected then AskForSiteSelection is emitted`() {
        var result: Event<Action>? = null

        viewModel.onActionTriggered.observeForever { result = it }
        viewModel.onContinueFlowSelected()

        assertThat(result!!.peekContent()).isInstanceOf(AskForSiteSelection::class.java)
    }

    @Test
    fun `when back is tapped on ReblogActionMode then navigate to no site is emitted`() {
        var result: Event<Action>? = null

        viewModel.onActionTriggered.observeForever { result = it }
        viewModel.onReblogActionBackSelected()

        assertThat(result!!.peekContent()).isEqualTo(NavigateToState(TO_NO_SITE_SELECTED))
    }

    @Test
    fun `when onRefreshReblogActionMode is invoked and site was selected then navigate to site selected is emitted`() {
        var result: Event<Action>? = null

        viewModel.onActionTriggered.observeForever {
            result = it
        }
        viewModel.onSiteForReblogSelected(siteRecord)
        assertThat(result!!.peekContent()).isEqualTo(NavigateToState(TO_SITE_SELECTED, siteRecord))

        result = null

        viewModel.onRefreshReblogActionMode()
        assertThat(result!!.peekContent()).isEqualTo(NavigateToState(TO_SITE_SELECTED, siteRecord))
    }

    @Test
    fun `when onSiteListLoaded is invoked then show jetpack individual plugin overlay`() =
        test {
            whenever(wpJetpackIndividualPluginHelper.shouldShowJetpackIndividualPluginOverlay()).thenReturn(true)

            viewModel.onSiteListLoaded()
            advanceUntilIdle()

            assertThat(viewModel.onActionTriggered.value?.peekContent()).isEqualTo(ShowJetpackIndividualPluginOverlay)
        }

    @Test
    fun `when onSiteListLoaded is invoked then don't show jetpack individual plugin overlay`() =
        test {
            whenever(wpJetpackIndividualPluginHelper.shouldShowJetpackIndividualPluginOverlay()).thenReturn(false)

            viewModel.onSiteListLoaded()
            advanceUntilIdle()

            assertThat(viewModel.onActionTriggered.value?.peekContent()).isNull()
        }
}
