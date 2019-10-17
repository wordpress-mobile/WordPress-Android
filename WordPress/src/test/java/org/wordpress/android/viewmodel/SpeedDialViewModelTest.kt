package org.wordpress.android.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.ui.main.SpeedDialAction
import org.wordpress.android.ui.main.SpeedDialState
import org.wordpress.android.ui.main.SpeedDialState.CLOSED
import org.wordpress.android.ui.main.SpeedDialState.HIDDEN
import org.wordpress.android.ui.main.SpeedDialUiState
import org.wordpress.android.ui.main.SpeedDialViewModel

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SpeedDialViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var uiStateObserver: Observer<SpeedDialUiState>

    private lateinit var viewModel: SpeedDialViewModel

    @Before
    fun setUp() {
        viewModel = SpeedDialViewModel(TEST_DISPATCHER)
        viewModel.uiState.observeForever(uiStateObserver)
        viewModel.start(true)
    }

    @Test
    fun `speed dial visible and closed when asked`() {
        viewModel.onPageChanged(CLOSED)
        assertThat(viewModel.uiState.value).isEqualTo(SpeedDialUiState(speedDialState = CLOSED))
    }

    @Test
    fun `speed dial hidden when asked`() {
        viewModel.onPageChanged(HIDDEN)
        assertThat(viewModel.uiState.value).isEqualTo(SpeedDialUiState(speedDialState = HIDDEN))
    }

    @Test
    fun `speed dial action is new post when new post fab tapped`() {
        val job = viewModel.onSpeedDialAction(R.id.fab_add_new_post)
        runBlocking {
            job.join()
        }
        assertThat(viewModel.speedDialAction.value).isEqualTo(SpeedDialAction.SD_ACTION_NEW_POST)
    }

    @Test
    fun `speed dial action is new page when new page fab tapped`() {
        val job = viewModel.onSpeedDialAction(R.id.fab_add_new_page)
        runBlocking {
            job.join()
        }
        assertThat(viewModel.speedDialAction.value).isEqualTo(SpeedDialAction.SD_ACTION_NEW_PAGE)
    }
}
