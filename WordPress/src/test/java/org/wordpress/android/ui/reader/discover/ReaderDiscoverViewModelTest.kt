package org.wordpress.android.ui.reader.discover

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.test
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.ContentUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.LoadingUiState
import org.wordpress.android.ui.reader.repository.ReaderPostRepository

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderDiscoverViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var readerPostRepository: ReaderPostRepository

    private val fakeDiscoverFeed = MutableLiveData<ReaderPostList>()

    private lateinit var viewModel: ReaderDiscoverViewModel

    @Before
    fun setUp() = test {
        viewModel = ReaderDiscoverViewModel(readerPostRepository, TEST_DISPATCHER, TEST_DISPATCHER)
        whenever(readerPostRepository.discoveryFeed).thenReturn(fakeDiscoverFeed)
    }

    @Test
    fun `initial uiState is loading`() {
        // Arrange
        val uiStates = mutableListOf<DiscoverUiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        // Act
        viewModel.start()

        // Assert
        assertThat(uiStates.size).isEqualTo(1)
        assertThat(uiStates[0]).isEqualTo(LoadingUiState)
    }

    @Test
    fun `uiState updated when discover feed finishes loading`() = test {
        // Arrange
        val uiStates = mutableListOf<DiscoverUiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        viewModel.start()

        // Act
        fakeDiscoverFeed.value = mock() // mock finished loading

        // Assert
        assertThat(uiStates.size).isEqualTo(2)
        assertThat(uiStates[1]).isInstanceOf(ContentUiState::class.java)
    }
}
