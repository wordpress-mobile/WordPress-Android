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
import org.wordpress.android.models.discover.ReaderDiscoverCard.ReaderPostCard
import org.wordpress.android.models.discover.ReaderDiscoverCards
import org.wordpress.android.test
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.ContentUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.LoadingUiState
import org.wordpress.android.ui.reader.reblog.ReblogUseCase
import org.wordpress.android.ui.reader.repository.ReaderDiscoverDataProvider
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ReactiveMutableLiveData

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderDiscoverViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var readerDiscoverDataProviderFactory: ReaderDiscoverDataProvider.Factory
    @Mock private lateinit var readerDiscoverDataProvider: ReaderDiscoverDataProvider
    @Mock private lateinit var uiStateBuilder: ReaderPostUiStateBuilder
    @Mock private lateinit var readerPostCardActionsHandler: ReaderPostCardActionsHandler
    @Mock private lateinit var reblogUseCase: ReblogUseCase
    @Mock private lateinit var readerUtilsWrapper: ReaderUtilsWrapper

    private val fakeDiscoverFeed = ReactiveMutableLiveData<ReaderDiscoverCards>()
    private val communicationChannel = MutableLiveData<Event<ReaderRepositoryCommunication>>()

    private lateinit var viewModel: ReaderDiscoverViewModel

    @Before
    fun setUp() = test {
        whenever(readerDiscoverDataProviderFactory.create()).thenReturn(readerDiscoverDataProvider)
        viewModel = ReaderDiscoverViewModel(
                readerDiscoverDataProviderFactory,
                uiStateBuilder,
                readerPostCardActionsHandler,
                reblogUseCase,
                readerUtilsWrapper,
                TEST_DISPATCHER,
                TEST_DISPATCHER
        )
        whenever(readerDiscoverDataProvider.discoverFeed).thenReturn(fakeDiscoverFeed)
        whenever(
                uiStateBuilder.mapPostToUiState(
                        anyOrNull(), anyBoolean(), anyInt(), anyInt(), anyOrNull(), anyBoolean(), anyOrNull(),
                        anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()
                )
        ).thenReturn(mock())
        whenever(readerDiscoverDataProvider.communicationChannel).thenReturn(communicationChannel)
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
        fakeDiscoverFeed.value = createDummyReaderCardsList() // mock finished loading

        // Assert
        assertThat(uiStates.size).isEqualTo(2)
        assertThat(uiStates[1]).isInstanceOf(ContentUiState::class.java)
    }

    private fun createDummyReaderCardsList(): ReaderDiscoverCards {
        return ReaderDiscoverCards(listOf(ReaderPostCard(createDummyReaderPost())))
    }

    private fun createDummyReaderPost(): ReaderPost = ReaderPost().apply {
        postId = 1
        title = "DummyPost"
    }
}
