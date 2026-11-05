package org.wordpress.android.ui.reader.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResultListener

@ExperimentalCoroutinesApi
class ReaderPostUpdaterTest : BaseUnitTest() {
    private val readerPostRepository: ReaderPostRepository = mock()

    private lateinit var updater: ReaderPostUpdater

    @Before
    fun setUp() {
        updater = ReaderPostUpdater(
            readerPostRepository = readerPostRepository,
            applicationScope = testScope(),
            ioDispatcher = testDispatcher()
        )
    }

    @Test
    fun `WHEN posts with a tag are updated THEN started and ended are emitted with the tag`() = test {
        val tag = ReaderTag("dogs", "dogs", "dogs", null, ReaderTagType.FOLLOWED)
        doAnswer { it.getArgument<UpdateResultListener>(2).onUpdateResult(UpdateResult.HAS_NEW) }
            .whenever(readerPostRepository).requestPostsWithTag(eq(tag), eq(UpdateAction.REQUEST_NEWER), any())
        val events = collectEvents()

        updater.updatePosts(ReaderPostListTarget.Tag(tag), UpdateAction.REQUEST_NEWER)

        assertThat(events).containsExactly(
            ReaderPostListEvent.UpdateStarted(tag, UpdateAction.REQUEST_NEWER),
            ReaderPostListEvent.UpdateEnded(tag, UpdateAction.REQUEST_NEWER, UpdateResult.HAS_NEW)
        )
    }

    @Test
    fun `WHEN posts in a blog are updated THEN events are emitted without a tag`() = test {
        doAnswer { it.getArgument<UpdateResultListener>(2).onUpdateResult(UpdateResult.FAILED) }
            .whenever(readerPostRepository).requestPostsForBlog(eq(BLOG_ID), eq(UpdateAction.REQUEST_OLDER), any())
        val events = collectEvents()

        updater.updatePosts(ReaderPostListTarget.Blog(BLOG_ID), UpdateAction.REQUEST_OLDER)

        assertThat(events).containsExactly(
            ReaderPostListEvent.UpdateStarted(null, UpdateAction.REQUEST_OLDER),
            ReaderPostListEvent.UpdateEnded(null, UpdateAction.REQUEST_OLDER, UpdateResult.FAILED)
        )
    }

    @Test
    fun `WHEN a search succeeds THEN started and a successful end are emitted`() = test {
        whenever(readerPostRepository.searchPosts(QUERY, 0)).thenReturn(true)
        val events = collectEvents()

        updater.searchPosts(QUERY, 0)

        assertThat(events).containsExactly(
            ReaderPostListEvent.SearchStarted(QUERY, 0),
            ReaderPostListEvent.SearchEnded(QUERY, 0, didSucceed = true)
        )
    }

    @Test
    fun `WHEN a search fails THEN a failed end is emitted`() = test {
        whenever(readerPostRepository.searchPosts(QUERY, OFFSET)).thenReturn(false)
        val events = collectEvents()

        updater.searchPosts(QUERY, OFFSET)

        assertThat(events.last()).isEqualTo(ReaderPostListEvent.SearchEnded(QUERY, OFFSET, didSucceed = false))
    }

    private fun TestScope.collectEvents(): List<ReaderPostListEvent> {
        val events = mutableListOf<ReaderPostListEvent>()
        backgroundScope.launch(testDispatcher()) { updater.events.collect { events.add(it) } }
        return events
    }

    companion object {
        private const val BLOG_ID = 123L
        private const val QUERY = "kittens"
        private const val OFFSET = 20
    }
}
