package org.wordpress.android.ui.reader.repository

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.reader.tracker.ReaderTracker

@ExperimentalCoroutinesApi
class ReaderSubsRepositoryTest : BaseUnitTest() {
    @Mock lateinit var tagsAndSitesUpdater: ReaderTagsAndSitesUpdater
    @Mock lateinit var readerTracker: ReaderTracker

    private val updaterEvents = MutableSharedFlow<ReaderTagsAndSitesEvent>()

    private lateinit var repository: ReaderSubsRepository

    @Before
    fun setUp() {
        whenever(tagsAndSitesUpdater.events).thenReturn(updaterEvents)
        repository = ReaderSubsRepository(tagsAndSitesUpdater, readerTracker, testDispatcher())
    }

    @Test
    fun `events relays the followed tags and sites events from the updater`() = test {
        repository.events.test {
            updaterEvents.emit(ReaderTagsAndSitesEvent.FollowedTagsChanged(totalTags = 3, didChange = true))
            updaterEvents.emit(ReaderTagsAndSitesEvent.FollowedBlogsChanged(totalSubscriptions = 5, didChange = true))
            updaterEvents.emit(ReaderTagsAndSitesEvent.FollowedBlogsChanged(totalSubscriptions = 5, didChange = false))

            assertThat(awaitItem()).isEqualTo(ReaderSubsEvent.FollowedTagsFetched)
            assertThat(awaitItem()).isEqualTo(ReaderSubsEvent.FollowedBlogsFetched(didChange = true))
            assertThat(awaitItem()).isEqualTo(ReaderSubsEvent.FollowedBlogsFetched(didChange = false))
        }
    }
}
