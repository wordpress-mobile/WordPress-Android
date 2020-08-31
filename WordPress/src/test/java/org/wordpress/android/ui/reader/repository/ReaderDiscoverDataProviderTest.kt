package org.wordpress.android.ui.reader.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.MainCoroutineScopeRule
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.discover.ReaderDiscoverCard.ReaderPostCard
import org.wordpress.android.models.discover.ReaderDiscoverCards
import org.wordpress.android.test
import org.wordpress.android.ui.reader.ReaderEvents.FetchDiscoverCardsEnded
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.FAILED
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.HAS_NEW
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.UNCHANGED
import org.wordpress.android.ui.reader.repository.ReaderDiscoverCommunication.Error.RemoteRequestFailure
import org.wordpress.android.ui.reader.repository.ReaderDiscoverCommunication.Started
import org.wordpress.android.ui.reader.repository.ReaderDiscoverCommunication.Success
import org.wordpress.android.ui.reader.repository.usecases.FetchDiscoverCardsUseCase
import org.wordpress.android.ui.reader.repository.usecases.GetDiscoverCardsUseCase
import org.wordpress.android.ui.reader.repository.usecases.ShouldAutoUpdateTagUseCase
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks.REQUEST_FIRST_PAGE
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks.REQUEST_MORE
import org.wordpress.android.ui.reader.utils.ReaderTagWrapper
import org.wordpress.android.util.EventBusWrapper

private const val NUMBER_OF_ITEMS = 10L

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderDiscoverDataProviderTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Rule
    @JvmField val coroutineScope = MainCoroutineScopeRule()

    private lateinit var dataProvider: ReaderDiscoverDataProvider
    private lateinit var readerTag: ReaderTag

    @Mock private lateinit var eventBusWrapper: EventBusWrapper
    @Mock private lateinit var getDiscoverCardsUseCase: GetDiscoverCardsUseCase
    @Mock private lateinit var shouldAutoUpdateTagUseCase: ShouldAutoUpdateTagUseCase
    @Mock private lateinit var fetchDiscoverCardsUseCase: FetchDiscoverCardsUseCase
    @Mock private lateinit var readerTagWrapper: ReaderTagWrapper

    @Before
    fun setUp() {
        dataProvider = ReaderDiscoverDataProvider(
                TEST_DISPATCHER,
                eventBusWrapper,
                readerTagWrapper,
                getDiscoverCardsUseCase,
                shouldAutoUpdateTagUseCase,
                fetchDiscoverCardsUseCase
        )
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `when refreshCards is requested started gets posted on comm channel`() = test {
        whenever(fetchDiscoverCardsUseCase.fetch(REQUEST_FIRST_PAGE)).thenReturn(Started(REQUEST_FIRST_PAGE))

        dataProvider.communicationChannel.observeForever { }

        dataProvider.refreshCards()

        Assertions.assertThat(requireNotNull(dataProvider.communicationChannel.value?.peekContent()))
                .isEqualTo(Started(REQUEST_FIRST_PAGE))
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `when fetch request fails then failure gets posted to comm channel`() = test {
        // Arrange
        val event = FetchDiscoverCardsEnded(REQUEST_FIRST_PAGE, FAILED)

        dataProvider.communicationChannel.observeForever { }

        dataProvider.onCardsUpdated(event)

        Assertions.assertThat(requireNotNull(dataProvider.communicationChannel.value?.peekContent()))
                .isEqualTo(RemoteRequestFailure(REQUEST_FIRST_PAGE))
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `when fetch request succeeds success gets posted to comm channel`() = test {
        // Arrange
        val event = FetchDiscoverCardsEnded(REQUEST_FIRST_PAGE, HAS_NEW)

        dataProvider.communicationChannel.observeForever { }

        dataProvider.onCardsUpdated(event)

        Assertions.assertThat(requireNotNull(dataProvider.communicationChannel.value?.peekContent()))
                .isEqualTo(Success(REQUEST_FIRST_PAGE))
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `when fetch request unchanged success gets posted to comm channel`() = test {
        // Arrange
        val event = FetchDiscoverCardsEnded(REQUEST_FIRST_PAGE, UNCHANGED)

        dataProvider.communicationChannel.observeForever { }

        dataProvider.onCardsUpdated(event)

        Assertions.assertThat(requireNotNull(dataProvider.communicationChannel.value?.peekContent()))
                .isEqualTo(Success(REQUEST_FIRST_PAGE))
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `on cards updated has new the data gets posted to discover feed`() = test {
        // Arrange

        whenever(getDiscoverCardsUseCase.get()).thenReturn(createDummyReaderCardsList())

        val event = FetchDiscoverCardsEnded(REQUEST_FIRST_PAGE, HAS_NEW)

        dataProvider.onCardsUpdated(event)

        Assertions.assertThat(requireNotNull(dataProvider.discoverFeed.value))
                .isInstanceOf(ReaderDiscoverCards::class.java)

        Assertions.assertThat(requireNotNull(dataProvider.discoverFeed.value).cards.size)
                .isEqualTo(NUMBER_OF_ITEMS)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `when loadMoreRequest in progress another started not posted to comm channel`() = test {
        whenever(fetchDiscoverCardsUseCase.fetch(REQUEST_MORE)).thenReturn(Started(REQUEST_MORE))

        dataProvider.communicationChannel.observeForever { }

        dataProvider.loadMoreCards()

        val started = dataProvider.communicationChannel.value?.getContentIfNotHandled()
        Assertions.assertThat(requireNotNull(started)).isEqualTo(Started(REQUEST_MORE))

        // Pause the dispatcher
        coroutineScope.pauseDispatcher()

        dataProvider.loadMoreCards()

        val noUnhandledContent = dataProvider.communicationChannel.value?.getContentIfNotHandled()
        Assertions.assertThat(noUnhandledContent).isNull()

        // Resume pending coroutine so next tests don't get hung up
        coroutineScope.resumeDispatcher()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `when observers connect request is started and posted to comm channel`() = test {
        whenever(fetchDiscoverCardsUseCase.fetch(REQUEST_FIRST_PAGE)).thenReturn(Started(REQUEST_FIRST_PAGE))
        whenever(getDiscoverCardsUseCase.get()).thenReturn(createDummyReaderCardsList())
        whenever(shouldAutoUpdateTagUseCase.get(dataProvider.readerTag)).thenReturn(true)

        dataProvider.communicationChannel.observeForever { }
        dataProvider.discoverFeed.observeForever{ }

        val started = dataProvider.communicationChannel.value?.getContentIfNotHandled()
        Assertions.assertThat(requireNotNull(started)).isEqualTo(Started(REQUEST_FIRST_PAGE))
    }

    // Helper functions lifted from ReaderDiscoverViewModelTest because why reinvent the wheel
    private fun createDummyReaderCardsList(numberOfItems: Long = NUMBER_OF_ITEMS): ReaderDiscoverCards {
        return ReaderDiscoverCards(createDummyReaderPostCardList(numberOfItems))
    }

    private fun createDummyReaderPostCardList(numberOfItems: Long = NUMBER_OF_ITEMS) =
            (1..numberOfItems).map { ReaderPostCard(createDummyReaderPost(it)) }.toList()

    private fun createDummyReaderPost(id: Long): ReaderPost = ReaderPost().apply {
        this.postId = id
        this.blogId = id
        this.title = "DummyPost"
    }

    private fun createDummyReaderTag() = readerTagWrapper.createDiscoverPostCardsTag()
}
