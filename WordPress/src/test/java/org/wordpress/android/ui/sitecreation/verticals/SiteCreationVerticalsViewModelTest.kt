package org.wordpress.android.ui.sitecreation.verticals

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.test
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewModel.VerticalsUiState
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewModel.VerticalsUiState.VerticalsContentUiState

private const val SEGMENT_ID = 1L

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteCreationVerticalsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock private lateinit var uiStateObserver: Observer<VerticalsUiState>
    @Mock private lateinit var verticalSelectedObserver: Observer<String?>
    @Mock private lateinit var skipBtnClickedObservable: Observer<Unit>
    @Mock private lateinit var onHelpClickedObserver: Observer<Unit>

    private lateinit var viewModel: SiteCreationVerticalsViewModel

    @Before
    fun setUp() {
        viewModel = SiteCreationVerticalsViewModel()
        viewModel.uiState.observeForever(uiStateObserver)
        viewModel.verticalSelected.observeForever(verticalSelectedObserver)
        viewModel.skipBtnClicked.observeForever(skipBtnClickedObservable)
        viewModel.onHelpClicked.observeForever(onHelpClickedObserver)
    }

    private fun <T> testWithSuccessResponses(block: suspend CoroutineScope.() -> T) {
        test {
            block()
        }
    }

    @Test
    fun verifyHeaderAndSkipBtnShownAfterPromptFetched() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        verifySkipButtonVisible(viewModel.uiState)
    }

    @Test
    fun verifyOnSkipIsPropagated() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.onSkipStepBtnClicked()
        val captor = ArgumentCaptor.forClass(Unit::class.java)
        verify(skipBtnClickedObservable).onChanged(captor.capture())

        assertThat(captor.allValues.size).isEqualTo(1)
    }

    @Test
    fun verifyOnHelpClickedPropagated() = testWithSuccessResponses {
        viewModel.onHelpClicked()
        val captor = ArgumentCaptor.forClass(Unit::class.java)
        verify(onHelpClickedObserver).onChanged(captor.capture())

        assertThat(captor.allValues.size).isEqualTo(1)
    }

    private fun verifySkipButtonVisible(uiStateLiveData: LiveData<VerticalsUiState>) {
        verifySkipButtonVisible(uiStateLiveData.value!! as VerticalsContentUiState)
    }

    private fun verifySkipButtonVisible(uiState: VerticalsContentUiState) {
        assertThat(uiState.showSkipButton).isTrue()
    }
}
