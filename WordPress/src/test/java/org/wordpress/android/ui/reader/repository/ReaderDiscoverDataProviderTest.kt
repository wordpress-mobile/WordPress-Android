package org.wordpress.android.ui.reader.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.MainCoroutineScopeRule
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.discover.ReaderDiscoverCard.ReaderPostCard
import org.wordpress.android.models.discover.ReaderDiscoverCards
import org.wordpress.android.test
import org.wordpress.android.ui.reader.ReaderEvents.FetchDiscoverCardsEnded
import org.wordpress.android.ui.reader.ReaderEvents.FollowedTagsChanged
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.FAILED
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.HAS_NEW
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.UNCHANGED
import org.wordpress.android.ui.reader.repository.ReaderDiscoverCommunication.Error.RemoteRequestFailure
import org.wordpress.android.ui.reader.repository.ReaderDiscoverCommunication.Started
import org.wordpress.android.ui.reader.repository.ReaderDiscoverCommunication.Success
import org.wordpress.android.ui.reader.repository.ReaderRepositoryEvent.ReaderPostTableActionEnded
import org.wordpress.android.ui.reader.repository.usecases.FetchDiscoverCardsUseCase
import org.wordpress.android.ui.reader.repository.usecases.GetDiscoverCardsUseCase
import org.wordpress.android.ui.reader.repository.usecases.ShouldAutoUpdateTagUseCase
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks.REQUEST_FIRST_PAGE
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks.REQUEST_MORE
import org.wordpress.android.ui.reader.utils.ReaderTagWrapper
import org.wordpress.android.util.EventBusWrapper

private const val NUMBER_OF_ITEMS = 10L

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderDiscoverDataProviderTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Rule
    @JvmField val coroutineScope = MainCoroutineScopeRule()

    private lateinit var dataProvider: ReaderDiscoverDataProvider

    @Mock private lateinit var eventBusWrapper: EventBusWrapper
    @Mock private lateinit var getDiscoverCardsUseCase: GetDiscoverCardsUseCase
    @Mock private lateinit var shouldAutoUpdateTagUseCase: ShouldAutoUpdateTagUseCase
    @Mock private lateinit var fetchDiscoverCardsUseCase: FetchDiscoverCardsUseCase
    @Mock private lateinit var readerTagWrapper: ReaderTagWrapper

    @Before
    fun setUp() {
        dataProvider = ReaderDiscoverDataProvider(
                TEST_DISPATCHER,
                TEST_DISPATCHER,
                eventBusWrapper,
                readerTagWrapper,
                getDiscoverCardsUseCase,
                shouldAutoUpdateTagUseCase,
                fetchDiscoverCardsUseCase
        )
    }

    @Test
    fun `when refreshCards is requested started gets posted on comm channel`() = test {
        whenever(fetchDiscoverCardsUseCase.fetch(REQUEST_FIRST_PAGE)).thenReturn(Started(REQUEST_FIRST_PAGE))

        dataProvider.communicationChannel.observeForever { }

        dataProvider.refreshCards()

        Assertions.assertThat(requireNotNull(dataProvider.communicationChannel.value?.peekContent()))
                .isEqualTo(Started(REQUEST_FIRST_PAGE))
    }

    @Test
    fun `when fetch request fails then failure gets posted to comm channel`() = test {
        // Arrange
        val event = FetchDiscoverCardsEnded(REQUEST_FIRST_PAGE, FAILED)

        dataProvider.communicationChannel.observeForever { }

        dataProvider.onCardsUpdated(event)

        Assertions.assertThat(requireNotNull(dataProvider.communicationChannel.value?.peekContent()))
                .isEqualTo(RemoteRequestFailure(REQUEST_FIRST_PAGE))
    }

    @Test
    fun `when fetch request succeeds success gets posted to comm channel`() = test {
        // Arrange
        val event = FetchDiscoverCardsEnded(REQUEST_FIRST_PAGE, HAS_NEW)

        dataProvider.communicationChannel.observeForever { }

        dataProvider.onCardsUpdated(event)

        Assertions.assertThat(requireNotNull(dataProvider.communicationChannel.value?.peekContent()))
                .isEqualTo(Success(REQUEST_FIRST_PAGE))
    }

    @Test
    fun `when fetch request unchanged success gets posted to comm channel`() = test {
        // Arrange
        val event = FetchDiscoverCardsEnded(REQUEST_FIRST_PAGE, UNCHANGED)

        dataProvider.communicationChannel.observeForever { }

        dataProvider.onCardsUpdated(event)

        Assertions.assertThat(requireNotNull(dataProvider.communicationChannel.value?.peekContent()))
                .isEqualTo(Success(REQUEST_FIRST_PAGE))
    }

    @Test
    fun `when new observer added and db is empty, does NOT post anything to discover feed`() = test {
        whenever(shouldAutoUpdateTagUseCase.get(dataProvider.readerTag)).thenReturn(false)
        whenever(getDiscoverCardsUseCase.get()).thenReturn(ReaderDiscoverCards(listOf()))

        dataProvider.discoverFeed.observeForever { }

        Assertions.assertThat(dataProvider.discoverFeed.value).isNull()
    }

    @Test
    fun `when new observer added and db is NOT empty, the data gets posted to discover feed`() = test {
        whenever(shouldAutoUpdateTagUseCase.get(dataProvider.readerTag)).thenReturn(false)
        whenever(getDiscoverCardsUseCase.get()).thenReturn(createDummyReaderCardsList())

        dataProvider.discoverFeed.observeForever { }

        Assertions.assertThat(dataProvider.discoverFeed.value).isNotNull
    }

    @Test
    fun `when request first page finishes, the data gets posted to discover feed`() = test {
        // Make sure the provider doesn't emit any values when a new observer is added
        whenever(getDiscoverCardsUseCase.get()).thenReturn(ReaderDiscoverCards(listOf()))
        whenever(shouldAutoUpdateTagUseCase.get(dataProvider.readerTag)).thenReturn(false)
        dataProvider.discoverFeed.observeForever { }
        // Make sure the db returns some data
        whenever(getDiscoverCardsUseCase.get()).thenReturn(createDummyReaderCardsList())
        val event = FetchDiscoverCardsEnded(REQUEST_FIRST_PAGE, HAS_NEW)

        dataProvider.onCardsUpdated(event)

        Assertions.assertThat(requireNotNull(dataProvider.discoverFeed.value))
                .isInstanceOf(ReaderDiscoverCards::class.java)
        Assertions.assertThat(requireNotNull(dataProvider.discoverFeed.value).cards.size)
                .isEqualTo(NUMBER_OF_ITEMS)
    }

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

    // The following test the loadData(), which is kicked off when discoverFeed obtains observers
    @Test
    fun `when loadData with refresh request is started and posted to comm channel`() = test {
        whenever(fetchDiscoverCardsUseCase.fetch(REQUEST_FIRST_PAGE)).thenReturn(Started(REQUEST_FIRST_PAGE))
        whenever(getDiscoverCardsUseCase.get()).thenReturn(createDummyReaderCardsList())
        whenever(shouldAutoUpdateTagUseCase.get(dataProvider.readerTag)).thenReturn(true)

        dataProvider.communicationChannel.observeForever { }
        dataProvider.discoverFeed.observeForever { }

        val started = dataProvider.communicationChannel.value?.getContentIfNotHandled()
        Assertions.assertThat(requireNotNull(started)).isEqualTo(Started(REQUEST_FIRST_PAGE))
    }

    @Test
    fun `when loadData without refresh no start message posted to comm channel`() = test {
        whenever(getDiscoverCardsUseCase.get()).thenReturn(createDummyReaderCardsList())
        whenever(shouldAutoUpdateTagUseCase.get(dataProvider.readerTag)).thenReturn(false)

        dataProvider.communicationChannel.observeForever { }
        dataProvider.discoverFeed.observeForever { }

        val started = dataProvider.communicationChannel.value?.getContentIfNotHandled()
        Assertions.assertThat(started).isNull()
    }

    @Test
    fun `when loadData with forceReload true data posted to discover channel`() = test {
        whenever(getDiscoverCardsUseCase.get()).thenReturn(createDummyReaderCardsList())
        whenever(shouldAutoUpdateTagUseCase.get(dataProvider.readerTag)).thenReturn(false)

        // No observer
        dataProvider.onReaderPostTableAction(ReaderPostTableActionEnded)

        // Add observer
        dataProvider.discoverFeed.observeForever { }

        val data = dataProvider.discoverFeed.value
        Assertions.assertThat(data).isNotNull
    }

    @Test
    fun `when loadData with existsInMemory data posted to discover feed`() = test {
        val discoverFeedObserver = Observer<ReaderDiscoverCards> { }

        whenever(getDiscoverCardsUseCase.get()).thenReturn(createDummyReaderCardsList())
        whenever(shouldAutoUpdateTagUseCase.get(dataProvider.readerTag)).thenReturn(false)

        dataProvider.communicationChannel.observeForever { }

        // Force loading of data into discover feed on next observe
        dataProvider.onReaderPostTableAction(ReaderPostTableActionEnded)

        // connect observers
        dataProvider.discoverFeed.observeForever(discoverFeedObserver)

        // disconnect observer
        dataProvider.discoverFeed.removeObserver(discoverFeedObserver)

        // add an observer
        dataProvider.discoverFeed.observeForever { }

        // Validate that data exists in the feed
        val data = dataProvider.discoverFeed.value
        Assertions.assertThat(data).isNotNull

        Assertions.assertThat(data?.cards?.size).isEqualTo(NUMBER_OF_ITEMS)
    }

    @Test
    fun `when followed tags change the discover feed gets refreshed`() = test {
        // Act
        dataProvider.onFollowedTagsChanged(FollowedTagsChanged(true))
        // Assert
        verify(fetchDiscoverCardsUseCase).fetch(REQUEST_FIRST_PAGE)
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
}
