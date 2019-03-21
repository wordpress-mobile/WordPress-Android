package org.wordpress.android.ui.sitecreation.siteinfo

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.test
import org.wordpress.android.ui.sitecreation.misc.NewSiteCreationTracker
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationSiteInfoViewModel
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationSiteInfoViewModel.SiteInfoUiState

private const val SITE_TITLE = "Test Site Title"
private const val TAG_LINE = "Test Tag Line"

private val EMPTY_UI_STATE = SiteInfoUiState(siteTitle = "", tagLine = "")

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class NewSiteCreationSiteInfoViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()
    @Mock private lateinit var tracker: NewSiteCreationTracker
    @Mock private lateinit var uiStateObserver: Observer<SiteInfoUiState>
    @Mock private lateinit var onSkipClickedObserver: Observer<Unit>
    @Mock private lateinit var onNextClickedObserver: Observer<SiteInfoUiState>
    @Mock private lateinit var onHelpClickedObserver: Observer<Unit>

    private lateinit var viewModel: NewSiteCreationSiteInfoViewModel

    @Before
    fun setUp() {
        viewModel = NewSiteCreationSiteInfoViewModel(tracker, TEST_DISPATCHER)
        viewModel.uiState.observeForever(uiStateObserver)
        viewModel.skipBtnClicked.observeForever(onSkipClickedObserver)
        viewModel.nextBtnClicked.observeForever(onNextClickedObserver)
        viewModel.onHelpClicked.observeForever(onHelpClickedObserver)
    }

    @Test
    fun verifyInitialUiStateIsEmpty() {
        assertThat(viewModel.uiState.value).isEqualToComparingFieldByField(EMPTY_UI_STATE)
    }

    @Test
    fun verifyUpdateSiteTitle() {
        viewModel.updateSiteTitle(SITE_TITLE)
        val updatedUiState = EMPTY_UI_STATE.copy(siteTitle = SITE_TITLE)
        assertThat(viewModel.uiState.value).isEqualToComparingFieldByField(updatedUiState)
    }

    @Test
    fun verifyUpdateTagLine() {
        viewModel.updateTagLine(TAG_LINE)
        val updatedUiState = EMPTY_UI_STATE.copy(tagLine = TAG_LINE)
        assertThat(viewModel.uiState.value).isEqualToComparingFieldByField(updatedUiState)
    }

    @Test
    fun verifyOnSkipPropagatedWhenUiStateIsEmpty() {
        viewModel.onSkipNextClicked()
        val captor = ArgumentCaptor.forClass(Unit::class.java)
        verify(onSkipClickedObserver).onChanged(captor.capture())

        assertThat(captor.allValues.size).isEqualTo(1)
    }

    @Test
    fun verifyOnNextPropagatedWhenSiteTitleIsNotEmpty() {
        viewModel.updateSiteTitle(SITE_TITLE)
        val updatedUiState = EMPTY_UI_STATE.copy(siteTitle = SITE_TITLE)

        viewModel.onSkipNextClicked()
        val captor = ArgumentCaptor.forClass(SiteInfoUiState::class.java)
        verify(onNextClickedObserver).onChanged(captor.capture())

        assertThat(captor.allValues.size).isEqualTo(1)
        assertThat(captor.value).isEqualTo(updatedUiState)
    }

    @Test
    fun verifyOnHelpClickedPropagated() = test {
        viewModel.onHelpClicked()
        val captor = ArgumentCaptor.forClass(Unit::class.java)
        verify(onHelpClickedObserver).onChanged(captor.capture())

        assertThat(captor.allValues.size).isEqualTo(1)
    }
}
