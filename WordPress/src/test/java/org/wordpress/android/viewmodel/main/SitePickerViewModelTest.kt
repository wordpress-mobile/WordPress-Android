package org.wordpress.android.viewmodel.main

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.main.SitePickerAdapter.SiteRecord
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.main.SitePickerViewModel.ReblogAction
import org.wordpress.android.viewmodel.main.SitePickerViewModel.ReblogAction.AskForSiteSelection
import org.wordpress.android.viewmodel.main.SitePickerViewModel.ReblogAction.ContinueReblogTo
import org.wordpress.android.viewmodel.main.SitePickerViewModel.ReblogAction.UpdateMenuState

@RunWith(MockitoJUnitRunner::class)
class SitePickerViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: SitePickerViewModel

    @Mock
    private lateinit var siteRecord: SiteRecord

    @Before
    fun setUp() {
        viewModel = SitePickerViewModel()
    }

    @Test
    fun `when a site is selected then siteForReblog is not null`() {
        viewModel.onSiteForReblogSelected(siteRecord)
        assertThat(viewModel.isReblogSiteSelected()).isEqualTo(true)
    }

    @Test
    fun `when a site is not selected then siteForReblog is null`() {
        assertThat(viewModel.isReblogSiteSelected()).isEqualTo(false)
    }

    @Test
    fun `when a site is selected then UpdateMenuState is emitted`() {
        var result: Event<ReblogAction>? = null

        viewModel.onReblogActionTriggered.observeForever { result = it }
        viewModel.onSiteForReblogSelected(siteRecord)

        assertThat(result!!.peekContent()).isInstanceOf(UpdateMenuState::class.java)
    }

    @Test
    fun `when continue is tapped then ContinueReblogTo is emitted`() {
        var result: Event<ReblogAction>? = null

        viewModel.onReblogActionTriggered.observeForever { result = it }
        viewModel.onSiteForReblogSelected(siteRecord)
        viewModel.onContinueFlowSelected()

        assertThat(result!!.peekContent()).isInstanceOf(ContinueReblogTo::class.java)
        assertThat((result!!.peekContent() as ContinueReblogTo).siteForReblog).isEqualTo(siteRecord)
    }

    @Test
    fun `when continue is tapped but no site was selected then AskForSiteSelection is emitted`() {
        var result: Event<ReblogAction>? = null

        viewModel.onReblogActionTriggered.observeForever { result = it }
        viewModel.onContinueFlowSelected()

        assertThat(result!!.peekContent()).isInstanceOf(AskForSiteSelection::class.java)
    }
}
