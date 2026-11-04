package org.wordpress.android.ui.reader.repository

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.reader.ReaderEvents
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.viewmodel.ContextProvider

@ExperimentalCoroutinesApi
class ReaderSubsRepositoryTest : BaseUnitTest() {
    @Mock lateinit var contextProvider: ContextProvider
    @Mock lateinit var eventBusWrapper: EventBusWrapper
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var readerTracker: ReaderTracker

    private lateinit var repository: ReaderSubsRepository

    @Before
    fun setUp() {
        repository = ReaderSubsRepository(
            contextProvider,
            eventBusWrapper,
            accountStore,
            readerTracker,
            testDispatcher()
        )
    }

    @Test
    fun `events relays the followed tags and sites events from EventBus`() = test {
        repository.events.test {
            val subscriber = registeredSubscriber()

            subscriber.onEvent(ReaderEvents.FollowedTagsFetched(true, 3))
            subscriber.onEvent(ReaderEvents.FollowedBlogsFetched(5, true))
            subscriber.onEvent(ReaderEvents.FollowedBlogsFetched(5, false))

            assertThat(awaitItem()).isEqualTo(ReaderSubsEvent.FollowedTagsFetched)
            assertThat(awaitItem()).isEqualTo(ReaderSubsEvent.FollowedBlogsFetched(didChange = true))
            assertThat(awaitItem()).isEqualTo(ReaderSubsEvent.FollowedBlogsFetched(didChange = false))
        }
    }

    @Test
    fun `events unregisters its subscriber when collection stops`() = test {
        lateinit var subscriber: ReaderSubsRepository.EventSubscriber
        repository.events.test {
            subscriber = registeredSubscriber()
            cancelAndIgnoreRemainingEvents()
        }

        verify(eventBusWrapper).unregister(subscriber)
    }

    private fun registeredSubscriber(): ReaderSubsRepository.EventSubscriber {
        val captor = argumentCaptor<Any>()
        verify(eventBusWrapper).register(captor.capture())
        return captor.firstValue as ReaderSubsRepository.EventSubscriber
    }
}
