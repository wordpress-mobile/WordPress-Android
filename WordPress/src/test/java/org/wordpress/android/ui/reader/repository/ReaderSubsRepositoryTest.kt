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
import org.wordpress.android.ui.reader.repository.ReaderSubsRepository.Companion.urlKey

@ExperimentalCoroutinesApi
class ReaderSubsRepositoryTest : BaseUnitTest() {
    @Mock lateinit var tagsAndSitesUpdater: ReaderTagsAndSitesUpdater
    @Mock lateinit var blogRepository: ReaderBlogRepository

    private val updaterEvents = MutableSharedFlow<ReaderTagsAndSitesEvent>()

    private lateinit var repository: ReaderSubsRepository

    @Before
    fun setUp() {
        whenever(tagsAndSitesUpdater.events).thenReturn(updaterEvents)
        repository = ReaderSubsRepository(tagsAndSitesUpdater, blogRepository, testDispatcher())
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

    @Test
    fun `urlKey ignores the scheme, www, case and a trailing slash`() {
        val key = urlKey("http://example.com")
        assertThat(key).isEqualTo("example.com")
        assertThat(urlKey("https://www.Example.com/")).isEqualTo(key)
        assertThat(urlKey("example.com")).isEqualTo(key)
        assertThat(urlKey("https://example.com/?ref=1#top")).isEqualTo(key)
        assertThat(urlKey("https://example.com/Blog/")).isEqualTo(urlKey("http://example.com/blog"))
    }

    @Test
    fun `urlKey keeps the path, so a site in a subfolder doesn't match its domain`() {
        assertThat(urlKey("example.com/blog")).isNotEqualTo(urlKey("example.com"))
        assertThat(urlKey("example.com/feed")).isNotEqualTo(urlKey("example.com"))
    }

    @Test
    fun `urlKey returns null for a url without a host`() {
        assertThat(urlKey("")).isNull()
        assertThat(urlKey("http://")).isNull()
        assertThat(urlKey("not a url")).isNull()
    }
}
