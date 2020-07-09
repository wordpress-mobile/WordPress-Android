package org.wordpress.android.ui.reader.discover

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.test
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.ContentUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.LoadingUiState
import org.wordpress.android.ui.reader.reblog.ReblogUseCase
import org.wordpress.android.ui.reader.repository.ReaderDiscoverRepository
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ReactiveMutableLiveData

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderDiscoverViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var readerDiscoverRepositoryFactory: ReaderDiscoverRepository.Factory
    @Mock private lateinit var readerDiscoverRepository: ReaderDiscoverRepository
    @Mock private lateinit var uiStateBuilder: ReaderPostUiStateBuilder
    @Mock private lateinit var readerPostCardActionsHandler: ReaderPostCardActionsHandler
    @Mock private lateinit var reblogUseCase: ReblogUseCase

    private val fakeDiscoverFeed = ReactiveMutableLiveData<ReaderPostList>()
    private val communicationChannel = MutableLiveData<Event<ReaderRepositoryCommunication>>()

    private lateinit var viewModel: ReaderDiscoverViewModel

    @Before
    fun setUp() = test {
        whenever(readerDiscoverRepositoryFactory.create()).thenReturn(readerDiscoverRepository)
        viewModel = ReaderDiscoverViewModel(
                readerDiscoverRepositoryFactory,
                uiStateBuilder,
                readerPostCardActionsHandler,
                reblogUseCase,
                TEST_DISPATCHER,
                TEST_DISPATCHER
        )
        whenever(readerDiscoverRepository.discoverFeed).thenReturn(fakeDiscoverFeed)
        whenever(
                uiStateBuilder.mapPostToUiState(
                        anyOrNull(), anyInt(), anyInt(), anyOrNull(), anyBoolean(), anyOrNull(),
                        anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()
                )
        ).thenReturn(mock())
        whenever(readerDiscoverRepository.communicationChannel).thenReturn(communicationChannel)
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
        fakeDiscoverFeed.value = createDummyReaderPostList() // mock finished loading

        // Assert
        assertThat(uiStates.size).isEqualTo(2)
        assertThat(uiStates[1]).isInstanceOf(ContentUiState::class.java)
    }

    private fun createDummyReaderPostList(): ReaderPostList = ReaderPostList().apply {
        this.add(createDummyReaderPost())
    }

    private fun createDummyReaderPost(): ReaderPost = ReaderPost().apply {
        postId = 1
        title = "DummyPost"
    }
}
