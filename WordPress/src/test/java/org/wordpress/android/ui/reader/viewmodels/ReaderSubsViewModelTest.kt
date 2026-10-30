package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.repository.ReaderSubsRepository
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.EnumSet

@ExperimentalCoroutinesApi
class ReaderSubsViewModelTest : BaseUnitTest() {
    @Mock lateinit var repository: ReaderSubsRepository
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var readerTracker: ReaderTracker
    @Mock lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: ReaderSubsViewModel

    private val cats = tag("cats")
    private val dogs = tag("dogs")

    @Before
    fun setUp() = runBlocking {
        whenever(repository.events).thenReturn(emptyFlow())
        whenever(repository.getFollowedTags()).thenReturn(listOf(cats, dogs))
        whenever(repository.getFollowedBlogs()).thenReturn(emptyList())
        whenever(repository.getHost(any())).thenAnswer { (it.arguments[0] as String).substringAfter("://") }
        whenever(repository.isValidTagName(any())).thenReturn(true)
        whenever(repository.createFollowedTag(any())).thenAnswer { tag(it.arguments[0] as String) }
        whenever(resourceProvider.getString(any())).thenAnswer { string(it.arguments[0] as Int) }
        whenever(resourceProvider.getString(any(), anyVararg())).thenAnswer { string(it.arguments[0] as Int) }
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        viewModel = createViewModel()
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `opening tracks the screen, requests an update and loads the tags`() {
        verify(readerTracker).track(Stat.READER_MANAGE_VIEW_DISPLAYED)
        verify(repository).requestUpdate(EnumSet.of(UpdateTask.TAGS, UpdateTask.FOLLOWED_BLOGS))
        assertThat(viewModel.uiState.value.tags!!.map { it.slug }).containsExactly("cats", "dogs")
        assertThat(viewModel.uiState.value.sites).isNull()
    }

    @Test
    fun `an update that couldn't be requested offline is retried on resume`() {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        viewModel = createViewModel()
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        viewModel.onResume()
        viewModel.onResume()

        // once from the first view model's init, and once from the first resume of the second
        verify(repository, times(2)).requestUpdate(EnumSet.of(UpdateTask.TAGS, UpdateTask.FOLLOWED_BLOGS))
    }

    @Test
    fun `opens on the tab the caller asked for`() {
        viewModel = createViewModel(SavedStateHandle(mapOf(ReaderConstants.ARG_SUBS_TAB_POSITION to 1)))

        assertThat(viewModel.uiState.value.selectedTab).isEqualTo(ReaderSubsTab.SITES)
    }

    @Test
    fun `a selected tab outlives the caller's choice`() {
        val savedStateHandle = SavedStateHandle(mapOf(ReaderConstants.ARG_SUBS_TAB_POSITION to 1))
        viewModel = createViewModel(savedStateHandle)

        viewModel.onTabSelected(ReaderSubsTab.TAGS)
        viewModel = createViewModel(savedStateHandle)

        assertThat(viewModel.uiState.value.selectedTab).isEqualTo(ReaderSubsTab.TAGS)
    }

    @Test
    fun `unfollowing a tag keeps its row and changes the result`() = test {
        whenever(repository.followTag(cats, false)).thenReturn(true)
        assertThat(viewModel.hasChangedTags()).isFalse

        viewModel.onTagFollowClicked(viewModel.uiState.value.tags!!.first())

        val row = viewModel.uiState.value.tags!!.first()
        assertThat(row.isFollowing).isFalse
        assertThat(row.isFollowEnabled).isTrue
        assertThat(viewModel.hasChangedTags()).isTrue
        verify(readerTracker).trackTag(Stat.READER_TAG_UNFOLLOWED, "cats", ReaderTracker.SOURCE_SETTINGS)
    }

    @Test
    fun `a failed tag unfollow shows an error and reloads the tags`() = test {
        whenever(repository.followTag(cats, false)).thenReturn(false)

        viewModel.events.test {
            viewModel.onTagFollowClicked(viewModel.uiState.value.tags!!.first())

            assertThat(awaitItem()).isEqualTo(message(R.string.reader_toast_err_removing_tag))
        }
        verify(repository, times(2)).getFollowedTags()
        assertThat(viewModel.uiState.value.tags!!.first().isFollowing).isTrue
    }

    @Test
    fun `toggling a tag offline does nothing but say so`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        viewModel.events.test {
            viewModel.onTagFollowClicked(viewModel.uiState.value.tags!!.first())

            assertThat(awaitItem()).isEqualTo(message(R.string.no_network_message))
        }
        verify(repository, never()).followTag(any(), any())
    }

    @Test
    fun `sites are sorted by title and filtered by title or host`() = test {
        whenever(repository.getFollowedBlogs()).thenReturn(
            listOf(blog(1, "zebra", "https://stripes.com"), blog(2, "Apple", "https://fruit.com"))
        )
        viewModel.onResume()

        assertThat(viewModel.uiState.value.sites!!.map { it.title }).containsExactly("Apple", "zebra")

        viewModel.onSiteSearchOpened()
        viewModel.onSiteSearchQueryChanged("STRIPES")

        assertThat(viewModel.uiState.value.sites!!.map { it.title }).containsExactly("zebra")

        viewModel.onSiteSearchClosed()

        assertThat(viewModel.uiState.value.sites).hasSize(2)
    }

    @Test
    fun `a reload during a site follow keeps its button disabled`() = test {
        whenever(repository.getFollowedBlogs()).thenReturn(listOf(blog(1, "Apple", "https://fruit.com")))
        val follow = CompletableDeferred<Boolean>()
        whenever(repository.followSite(1, 0, false)).doSuspendableAnswer { follow.await() }
        viewModel.onResume()

        viewModel.onSiteFollowClicked(viewModel.uiState.value.sites!!.first())
        viewModel.onResume()

        with(viewModel.uiState.value.sites!!.first()) {
            assertThat(isFollowing).isFalse
            assertThat(isFollowEnabled).isFalse
        }

        follow.complete(true)

        assertThat(viewModel.uiState.value.sites!!.first().isFollowEnabled).isTrue
    }

    @Test
    fun `a failed site unfollow reverts the row`() = test {
        whenever(repository.getFollowedBlogs()).thenReturn(listOf(blog(1, "Apple", "https://fruit.com", feedId = 7)))
        whenever(repository.followSite(1, 7, false)).thenReturn(false)
        viewModel.onResume()

        viewModel.events.test {
            viewModel.onSiteFollowClicked(viewModel.uiState.value.sites!!.first())

            assertThat(awaitItem()).isEqualTo(message(R.string.reader_toast_err_unable_to_unfollow_blog))
        }
        assertThat(viewModel.uiState.value.sites!!.first().isFollowing).isTrue
        assertThat(viewModel.uiState.value.sites!!.first().isFollowEnabled).isTrue
    }

    @Test
    fun `a half-typed entry survives the view model being recreated`() {
        val savedStateHandle = SavedStateHandle()
        viewModel = createViewModel(savedStateHandle)

        viewModel.onEntryChanged("example.com")
        viewModel = createViewModel(savedStateHandle)

        assertThat(viewModel.uiState.value.entry).isEqualTo("example.com")
    }

    @Test
    fun `an invalid tag isn't followed`() = test {
        whenever(repository.isValidTagName("a#b")).thenReturn(false)

        viewModel.events.test {
            add("a#b")

            assertThat(awaitItem()).isEqualTo(message(R.string.reader_toast_err_tag_not_valid))
        }
        verify(repository, never()).followTag(any(), any())
    }

    @Test
    fun `a tag that's already followed isn't followed again`() = test {
        whenever(repository.isFollowedTagName("cats")).thenReturn(true)

        viewModel.events.test {
            add("cats")

            assertThat(awaitItem()).isEqualTo(message(R.string.reader_toast_err_tag_already_following))
        }
    }

    @Test
    fun `adding a tag clears the entry, follows it and tracks it`() = test {
        whenever(repository.followTag(any(), eq(true))).thenReturn(true)

        viewModel.events.test {
            add("birds")

            assertThat(awaitItem()).isEqualTo(ReaderSubsUiEvent.HideKeyboard)
            assertThat(awaitItem()).isEqualTo(message(R.string.reader_label_added_tag))
        }
        assertThat(viewModel.uiState.value.entry).isEmpty()
        assertThat(viewModel.uiState.value.isAdding).isFalse
        verify(readerTracker).trackTag(Stat.READER_TAG_FOLLOWED, "birds", ReaderTracker.SOURCE_SETTINGS)
    }

    @Test
    fun `an entry that isn't a valid url is added as a tag`() = test {
        whenever(repository.normalizeEntryUrl("a.b")).thenReturn(null)
        whenever(repository.followTag(any(), eq(true))).thenReturn(true)

        add("a.b")

        verify(repository).createFollowedTag("a.b")
        verify(repository, never()).checkUrlReachable(any())
    }

    @Test
    fun `an unreachable url shows an error for its status code`() = test {
        val url = "http://example.com"
        whenever(repository.normalizeEntryUrl("example.com")).thenReturn(url)

        viewModel.events.test {
            whenever(repository.checkUrlReachable(url)).thenReturn(404)
            add("example.com")
            assertThat(awaitItem()).isEqualTo(message(R.string.reader_toast_err_follow_blog_could_not_be_found))

            whenever(repository.checkUrlReachable(url)).thenReturn(401)
            add("example.com")
            assertThat(awaitItem())
                .isEqualTo(message(R.string.reader_toast_err_follow_not_authorized_to_access_blog))

            whenever(repository.checkUrlReachable(url)).thenReturn(500)
            add("example.com")
            assertThat(awaitItem()).isEqualTo(
                ReaderSubsUiEvent.ShowMessage(string(R.string.reader_toast_err_unable_to_follow_blog) + " (500)")
            )
        }
        verify(repository, never()).followUrl(any())
        assertThat(viewModel.uiState.value.entry).isEqualTo("example.com")
    }

    @Test
    fun `a url that's already followed isn't followed again`() = test {
        val url = "http://example.com"
        whenever(repository.normalizeEntryUrl("example.com")).thenReturn(url)
        whenever(repository.isFollowedUrl(url)).thenReturn(true)

        viewModel.events.test {
            add("example.com")

            assertThat(awaitItem()).isEqualTo(message(R.string.reader_toast_err_already_follow_this_blog))
        }
        verify(repository, never()).checkUrlReachable(any())
    }

    @Test
    fun `following a url clears the entry, reloads the sites and requests an update`() = test {
        val url = "http://example.com"
        whenever(repository.normalizeEntryUrl("example.com")).thenReturn(url)
        whenever(repository.checkUrlReachable(url)).thenReturn(null)
        whenever(repository.followUrl(url)).thenReturn(true)

        viewModel.events.test {
            add("example.com")

            assertThat(awaitItem()).isEqualTo(ReaderSubsUiEvent.HideKeyboard)
            assertThat(awaitItem()).isEqualTo(message(R.string.reader_label_blog_subscribed))
        }
        assertThat(viewModel.uiState.value.entry).isEmpty()
        verify(repository).getFollowedBlogs()
        verify(repository, times(2)).requestUpdate(EnumSet.of(UpdateTask.TAGS, UpdateTask.FOLLOWED_BLOGS))
    }

    private fun add(entry: String) {
        viewModel.onEntryChanged(entry)
        viewModel.onAddClicked()
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) = ReaderSubsViewModel(
        savedStateHandle,
        repository,
        networkUtilsWrapper,
        readerTracker,
        resourceProvider,
        testDispatcher()
    )

    private fun string(resId: Int) = "string:$resId"

    private fun message(resId: Int) = ReaderSubsUiEvent.ShowMessage(string(resId))

    private fun tag(slug: String) = ReaderTag(slug, slug, slug, null, ReaderTagType.FOLLOWED)

    private fun blog(blogId: Long, name: String, url: String, feedId: Long = 0) = ReaderBlog().apply {
        this.blogId = blogId
        this.feedId = feedId
        this.name = name
        this.url = url
        isFollowing = true
    }
}
