package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.reader.ReaderTestUtils
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState.CompactBlogSectionData
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState.InteractionSectionData
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.PrimaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionsHandler
import org.wordpress.android.ui.reader.discover.ReaderPostMoreButtonUiStateBuilder
import org.wordpress.android.ui.reader.discover.ReaderPostUiStateBuilder
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId
import org.wordpress.android.ui.reader.reblog.ReblogUseCase
import org.wordpress.android.ui.reader.repository.ReaderPostListEvent
import org.wordpress.android.ui.reader.repository.ReaderPostListRepository
import org.wordpress.android.ui.reader.repository.ReaderPostListTarget
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.UpdateAction
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.usecases.ReaderFetchSiteUseCase
import org.wordpress.android.ui.reader.usecases.ReaderFetchSiteUseCase.FetchSiteState
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.FollowStatusChanged
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.image.ImageType

@ExperimentalCoroutinesApi
@Suppress("LargeClass")
class ReaderPostListViewModelTest : BaseUnitTest(StandardTestDispatcher()) {
    @Mock lateinit var actionsHandler: ReaderPostCardActionsHandler
    @Mock lateinit var reblogUseCase: ReblogUseCase
    @Mock lateinit var readerTracker: ReaderTracker
    @Mock lateinit var seenStatusToggleUseCase: ReaderSeenStatusToggleUseCase
    @Mock lateinit var repository: ReaderPostListRepository
    @Mock lateinit var postUiStateBuilder: ReaderPostUiStateBuilder
    @Mock lateinit var moreMenuBuilder: ReaderPostMoreButtonUiStateBuilder
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var fetchSiteUseCase: ReaderFetchSiteUseCase

    private val repositoryEvents = MutableSharedFlow<ReaderPostListEvent>()
    private val followStatusUpdated = MutableLiveData<FollowStatusChanged>()
    private val scrollToSiteId = MutableSharedFlow<Long>()
    private val tag = ReaderTestUtils.createTag("cats")
    private val tagTarget = ReaderPostListTarget.Tag(tag)
    private lateinit var viewModel: ReaderPostListViewModel

    @Before
    fun setUp() = test {
        whenever(actionsHandler.navigationEvents).thenReturn(MutableLiveData())
        whenever(actionsHandler.snackbarEvents).thenReturn(MutableLiveData())
        whenever(actionsHandler.preloadPostEvents).thenReturn(MutableLiveData())
        whenever(actionsHandler.refreshPosts).thenReturn(MutableLiveData())
        whenever(actionsHandler.followStatusUpdated).thenReturn(followStatusUpdated)
        whenever(actionsHandler.scrollToSiteId).thenReturn(scrollToSiteId)
        whenever(repository.events).thenReturn(repositoryEvents)
        whenever(repository.getPosts(any())).thenReturn(ReaderPostList())
        whenever(repository.getPostCount(any())).thenReturn(0)
        whenever(repository.shouldAutoUpdateTag(any())).thenReturn(false)
        whenever(repository.isFollowedTag(any())).thenReturn(false)
        whenever(repository.isTimeToUpdateBlogInfo(any())).thenReturn(false)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(fetchSiteUseCase.fetchSite(any(), any(), anyOrNull())).thenReturn(FetchSiteState.Success)
        whenever(readerTracker.getSource(any(), anyOrNull())).thenReturn("source")
        whenever(
            postUiStateBuilder.mapPostToUiStateBlocking(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyOrNull()
            )
        ).thenAnswer { createUiState(it) }

        viewModel = ReaderPostListViewModel(
            actionsHandler,
            reblogUseCase,
            readerTracker,
            seenStatusToggleUseCase,
            repository,
            postUiStateBuilder,
            moreMenuBuilder,
            ReaderPostListEmptyStateBuilder(),
            networkUtilsWrapper,
            accountStore,
            fetchSiteUseCase,
            testDispatcher(),
            testDispatcher()
        )
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `loads posts for the tag when started`() = test {
        whenever(repository.getPosts(tagTarget)).thenReturn(posts(1, 2))

        startAndLoad()

        assertThat(postIds()).containsExactly(1L, 2L)
    }

    @Test
    fun `gap marker appears below the post it belongs to`() = test {
        whenever(repository.getPosts(tagTarget)).thenReturn(posts(1, 2, 3))
        whenever(repository.getGapMarkerIds(tag)).thenReturn(ReaderBlogIdPostId(BLOG_ID, 1))

        startAndLoad()

        val items = viewModel.uiState.value.items
        assertThat(items[1]).isEqualTo(ReaderPostListItem.GapMarker(isFilling = false))
        assertThat(items).hasSize(4)
    }

    @Test
    fun `gap marker on the last post is removed`() = test {
        whenever(repository.getPosts(tagTarget)).thenReturn(posts(1, 2))
        whenever(repository.getGapMarkerIds(tag)).thenReturn(ReaderBlogIdPostId(BLOG_ID, 2))

        startAndLoad()

        assertThat(viewModel.uiState.value.items).noneMatch { it is ReaderPostListItem.GapMarker }
        verify(repository).removeGapMarker(tag)
    }

    @Test
    fun `blog preview starts with a site header`() = test {
        val blogTarget = ReaderPostListTarget.Blog(BLOG_ID)
        whenever(repository.getPosts(blogTarget)).thenReturn(posts(1))
        whenever(repository.getBlogInfo(BLOG_ID, 0)).thenReturn(blog())

        startAndLoad(ReaderPostListArgs(listType = ReaderPostListType.BLOG_PREVIEW, blogId = BLOG_ID))

        assertThat(viewModel.uiState.value.items.first())
            .isInstanceOf(ReaderPostListItem.SiteHeader::class.java)
        verify(repository).requestPosts(blogTarget, UpdateAction.REQUEST_NEWER)
    }

    @Test
    fun `following a site from a card updates its posts and the site header`() = test {
        whenever(repository.getPosts(ReaderPostListTarget.Blog(BLOG_ID))).thenReturn(posts(1, 2))
        whenever(repository.getBlogInfo(BLOG_ID, 0)).thenReturn(blog())
        startAndLoad(blogPreviewArgs())

        followStatusUpdated.value = FollowStatusChanged(BLOG_ID, 0, following = true)
        advanceUntilIdle()

        assertThat(siteHeader()?.isFollowed).isTrue
        assertThat(cardState(0).isFollowed).isTrue
        assertThat(cardState(1).isFollowed).isTrue
    }

    @Test
    fun `site header shows the stored blog info, then the server's when it's time to update`() = test {
        whenever(repository.getBlogInfo(BLOG_ID, 0))
            .thenReturn(blog(name = "stored"))
            .thenReturn(blog(name = "fetched"))
        whenever(repository.isTimeToUpdateBlogInfo(any())).thenReturn(true)
        val fetch = CompletableDeferred<FetchSiteState>()
        whenever(fetchSiteUseCase.fetchSite(any(), any(), anyOrNull())).doSuspendableAnswer { fetch.await() }

        startAndLoad(blogPreviewArgs())
        assertThat(siteHeader()?.name).isEqualTo("stored")

        fetch.complete(FetchSiteState.Success)
        advanceUntilIdle()

        verify(fetchSiteUseCase).fetchSite(BLOG_ID, 0)
        assertThat(siteHeader()?.name).isEqualTo("fetched")
    }

    @Test
    fun `site header doesn't fetch blog info that's up to date`() = test {
        whenever(repository.getBlogInfo(BLOG_ID, 0)).thenReturn(blog(name = "stored"))

        startAndLoad(blogPreviewArgs())

        verify(fetchSiteUseCase, never()).fetchSite(any(), any(), anyOrNull())
        assertThat(siteHeader()?.name).isEqualTo("stored")
    }

    @Test
    fun `blog info failing with nothing stored emits BlogInfoFailed`() = test {
        whenever(fetchSiteUseCase.fetchSite(any(), any(), anyOrNull())).thenReturn(FetchSiteState.Failed.RequestFailed)

        viewModel.uiEvents.filterIsInstance<ReaderPostListUiEvent.BlogInfoFailed>().test {
            startAndLoad(blogPreviewArgs())
            assertThat(awaitItem()).isEqualTo(ReaderPostListUiEvent.BlogInfoFailed)
        }
        // the header stays out of the list until there's blog info to show
        assertThat(viewModel.uiState.value.items).noneMatch { it is ReaderPostListItem.SiteHeader }
    }

    @Test
    fun `a failed fetch doesn't report the blog as missing when it's stored`() = test {
        whenever(repository.getBlogInfo(BLOG_ID, 0)).thenReturn(blog(name = "stored"))
        whenever(repository.isTimeToUpdateBlogInfo(any())).thenReturn(true)
        whenever(fetchSiteUseCase.fetchSite(any(), any(), anyOrNull())).thenReturn(FetchSiteState.Failed.RequestFailed)

        viewModel.uiEvents.filterIsInstance<ReaderPostListUiEvent.BlogInfoFailed>().test {
            startAndLoad(blogPreviewArgs())
            expectNoEvents()
        }
        assertThat(siteHeader()?.name).isEqualTo("stored")
    }

    @Test
    fun `a failed blog info fetch is tried again on the next reload`() = test {
        whenever(repository.getBlogInfo(BLOG_ID, 0)).thenReturn(blog(name = "stored"))
        whenever(repository.isTimeToUpdateBlogInfo(any())).thenReturn(true)
        whenever(fetchSiteUseCase.fetchSite(any(), any(), anyOrNull())).thenReturn(FetchSiteState.Failed.NoNetwork)
        startAndLoad(blogPreviewArgs())

        viewModel.reload()
        advanceUntilIdle()

        verify(fetchSiteUseCase, times(2)).fetchSite(BLOG_ID, 0)
    }

    @Test
    fun `following from the site header shows progress until the change is final`() = test {
        whenever(repository.getBlogInfo(BLOG_ID, 0)).thenReturn(blog())
        startAndLoad(blogPreviewArgs())

        viewModel.onSiteHeaderFollowClicked()
        advanceUntilIdle()
        verify(actionsHandler).handleFollowSiteClicked(BLOG_ID, 0, "blog", "source")

        followStatusUpdated.value = FollowStatusChanged(BLOG_ID, 0, following = true, isChangeFinal = false)
        advanceUntilIdle()
        assertThat(siteHeader()?.isFollowInProgress).isTrue
        assertThat(siteHeader()?.isFollowed).isTrue

        followStatusUpdated.value = FollowStatusChanged(BLOG_ID, 0, following = true, isChangeFinal = true)
        advanceUntilIdle()
        assertThat(siteHeader()?.isFollowInProgress).isFalse
    }

    @Test
    fun `the site header's settings button opens the subscription settings`() = test {
        whenever(repository.getBlogInfo(BLOG_ID, 0)).thenReturn(blog(isFollowing = true))
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        startAndLoad(blogPreviewArgs())
        assertThat(siteHeader()?.showSettings).isTrue

        viewModel.uiEvents.filterIsInstance<ReaderPostListUiEvent.ShowSubscriptionSettings>().test {
            viewModel.onSiteHeaderSettingsClicked()
            assertThat(awaitItem()).isEqualTo(ReaderPostListUiEvent.ShowSubscriptionSettings(BLOG_ID, "blog", ""))
        }
    }

    @Test
    fun `the site header has no settings button for a site that isn't followed`() = test {
        whenever(repository.getBlogInfo(BLOG_ID, 0)).thenReturn(blog(isFollowing = false))
        // lenient since the check can stop before reading it, but it has to be true for the test to mean anything
        lenient().`when`(accountStore.hasAccessToken()).thenReturn(true)

        startAndLoad(blogPreviewArgs())

        assertThat(siteHeader()?.showSettings).isFalse
    }

    @Test
    fun `the site header has no settings button when logged out`() = test {
        whenever(repository.getBlogInfo(BLOG_ID, 0)).thenReturn(blog(isFollowing = true))
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        startAndLoad(blogPreviewArgs())

        assertThat(siteHeader()?.showSettings).isFalse
    }

    @Test
    fun `the site header has no settings button for an external feed`() = test {
        whenever(repository.getBlogInfo(0, FEED_ID)).thenReturn(blog(isFollowing = true))
        lenient().`when`(accountStore.hasAccessToken()).thenReturn(true)

        startAndLoad(ReaderPostListArgs(listType = ReaderPostListType.BLOG_PREVIEW, feedId = FEED_ID))

        assertThat(siteHeader()?.showSettings).isFalse
    }

    @Test
    fun `a site header follow tap can't start a second follow before the first reports back`() = test {
        whenever(repository.getBlogInfo(BLOG_ID, 0)).thenReturn(blog())
        val follow = CompletableDeferred<Unit>()
        whenever(actionsHandler.handleFollowSiteClicked(any(), any(), any(), any()))
            .doSuspendableAnswer { follow.await() }
        startAndLoad(blogPreviewArgs())

        viewModel.onSiteHeaderFollowClicked()
        viewModel.onSiteHeaderFollowClicked()
        advanceUntilIdle()
        assertThat(siteHeader()?.isFollowInProgress).isTrue

        follow.complete(Unit)
        advanceUntilIdle()

        verify(actionsHandler, times(1)).handleFollowSiteClicked(any(), any(), any(), any())
        assertThat(siteHeader()?.isFollowInProgress).isFalse
    }

    @Test
    fun `duplicate posts appear once, since the list can't show duplicate keys`() = test {
        whenever(repository.getPosts(tagTarget)).thenReturn(posts(1, 1, 2))

        startAndLoad()

        assertThat(postIds()).containsExactly(1L, 2L)
    }

    @Test
    fun `scrolling to a site accounts for the header and gap marker`() = test {
        whenever(repository.getPosts(tagTarget))
            .thenReturn(posts(1, 2, 3).apply { last().blogId = OTHER_BLOG_ID })
        whenever(repository.getGapMarkerIds(tag)).thenReturn(ReaderBlogIdPostId(BLOG_ID, 1))
        startAndLoad(args(listType = ReaderPostListType.TAG_PREVIEW))

        scrollToSiteId.emit(OTHER_BLOG_ID)
        endUpdate(UpdateResult.CHANGED)

        // tag header, post 1, gap marker, post 2, post 3
        assertThat(viewModel.uiState.value.scrollToPosition).isEqualTo(4)
    }

    @Test
    fun `tag preview starts with a tag header once there are posts`() = test {
        whenever(repository.getPosts(tagTarget)).thenReturn(posts(1))
        whenever(repository.isFollowedTag(tag)).thenReturn(true)

        startAndLoad(args(listType = ReaderPostListType.TAG_PREVIEW))

        assertThat(viewModel.uiState.value.items.first())
            .isEqualTo(ReaderPostListItem.TagHeader(tag.label, isFollowed = true))
    }

    @Test
    fun `loading more requests older posts when under the cap`() = test {
        whenever(repository.getPostCount(tagTarget)).thenReturn(10)
        startAndLoad()

        viewModel.onLoadMore()
        advanceUntilIdle()

        verify(repository).requestPosts(tagTarget, UpdateAction.REQUEST_OLDER)
    }

    @Test
    fun `loading more does nothing at the cap`() = test {
        whenever(repository.getPostCount(tagTarget)).thenReturn(MAX_POSTS)
        startAndLoad()

        viewModel.onLoadMore()
        advanceUntilIdle()

        verify(repository, never()).requestPosts(tagTarget, UpdateAction.REQUEST_OLDER)
    }

    @Test
    fun `loading more does nothing while an update is running`() = test {
        startAndLoad()
        repositoryEvents.emit(ReaderPostListEvent.UpdateStarted(tag, UpdateAction.REQUEST_NEWER))

        viewModel.onLoadMore()
        advanceUntilIdle()

        verify(repository, never()).getPostCount(any())
    }

    @Test
    fun `update progress shows the footer only when loading older posts`() = test {
        startAndLoad()

        repositoryEvents.emit(ReaderPostListEvent.UpdateStarted(tag, UpdateAction.REQUEST_OLDER))
        assertThat(viewModel.uiState.value.isLoadingMore).isTrue
        assertThat(viewModel.uiState.value.isRefreshing).isFalse

        repositoryEvents.emit(ReaderPostListEvent.UpdateStarted(tag, UpdateAction.REQUEST_NEWER))
        assertThat(viewModel.uiState.value.isLoadingMore).isFalse
        assertThat(viewModel.uiState.value.isRefreshing).isTrue
    }

    @Test
    fun `pull to refresh is off while an update runs and in search`() = test {
        startAndLoad()
        assertThat(viewModel.uiState.value.canPullToRefresh).isTrue

        repositoryEvents.emit(ReaderPostListEvent.UpdateStarted(tag, UpdateAction.REQUEST_NEWER))
        assertThat(viewModel.uiState.value.canPullToRefresh).isFalse

        endUpdate(UpdateResult.UNCHANGED)
        assertThat(viewModel.uiState.value.canPullToRefresh).isTrue
    }

    @Test
    fun `pull to refresh is off for search results`() = test {
        startAndLoad(ReaderPostListArgs(listType = ReaderPostListType.SEARCH_RESULTS))

        assertThat(viewModel.uiState.value.canPullToRefresh).isFalse
    }

    @Test
    fun `events for other tags are ignored`() = test {
        startAndLoad()

        repositoryEvents.emit(
            ReaderPostListEvent.UpdateStarted(ReaderTestUtils.createTag("dogs"), UpdateAction.REQUEST_NEWER)
        )

        assertThat(viewModel.uiState.value.isRefreshing).isFalse
    }

    @Test
    fun `new or changed posts are reloaded`() = test {
        startAndLoad()

        endUpdate(UpdateResult.CHANGED)

        verify(repository, times(2)).getPosts(tagTarget)
    }

    @Test
    fun `new posts show the bar instead of reloading when scrolled away from the top`() = test {
        whenever(repository.getPosts(tagTarget)).thenReturn(posts(1, 2))
        whenever(repository.getGapMarkerIds(tag)).thenReturn(ReaderBlogIdPostId(BLOG_ID, 1))
        startAndLoad()
        viewModel.onFirstItemVisibilityChanged(false)

        endUpdate(UpdateResult.HAS_NEW)

        assertThat(viewModel.uiState.value.newPostsBarVisible).isTrue
        assertThat(viewModel.uiState.value.items).noneMatch { it is ReaderPostListItem.GapMarker }
        verify(repository, times(1)).getPosts(tagTarget)

        viewModel.onNewPostsBarScrolledAway()
        assertThat(viewModel.uiState.value.newPostsBarVisible).isFalse
    }

    @Test
    fun `new posts are shown right away when the top is visible`() = test {
        whenever(repository.getPosts(tagTarget)).thenReturn(posts(1, 2))
        startAndLoad()

        endUpdate(UpdateResult.HAS_NEW)

        assertThat(viewModel.uiState.value.newPostsBarVisible).isFalse
        verify(repository, times(2)).getPosts(tagTarget)
    }

    @Test
    fun `failed update shows the request failed empty state`() = test {
        startAndLoad()

        endUpdate(UpdateResult.FAILED)

        assertThat(viewModel.uiState.value.emptyState)
            .isEqualTo(ReaderPostListEmptyState.RequestFailed(isSearch = false))
    }

    @Test
    fun `empty state waits for the first update`() = test {
        startAndLoad()
        assertThat(viewModel.uiState.value.emptyState).isNull()

        endUpdate(UpdateResult.UNCHANGED)

        assertThat(viewModel.uiState.value.emptyState).isEqualTo(ReaderPostListEmptyState.NoPostsWithTag)
    }

    @Test
    fun `empty state shows offline without update actions`() = test {
        startAndLoad()
        repositoryEvents.emit(ReaderPostListEvent.UpdateStarted(tag, UpdateAction.REQUEST_NEWER))
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        viewModel.onSubfilterChanged(null)

        assertThat(viewModel.uiState.value.emptyState).isEqualTo(ReaderPostListEmptyState.NoConnection)
        assertThat(viewModel.uiState.value.isRefreshing).isFalse
    }

    @Test
    fun `filling the gap shows progress until the reload after a failure`() = test {
        whenever(repository.getPosts(tagTarget)).thenReturn(posts(1, 2))
        whenever(repository.getGapMarkerIds(tag)).thenReturn(ReaderBlogIdPostId(BLOG_ID, 1))
        startAndLoad()

        viewModel.onGapMarkerClicked()

        verify(repository).requestPosts(tagTarget, UpdateAction.REQUEST_OLDER_THAN_GAP)
        assertThat(viewModel.uiState.value.items[1]).isEqualTo(ReaderPostListItem.GapMarker(isFilling = true))

        endUpdate(UpdateResult.UNCHANGED, UpdateAction.REQUEST_OLDER_THAN_GAP)

        assertThat(viewModel.uiState.value.items[1]).isEqualTo(ReaderPostListItem.GapMarker(isFilling = false))
    }

    @Test
    fun `pull to refresh updates tags and refreshes posts`() = test {
        startAndLoad()

        viewModel.onPullToRefresh()

        verify(repository).updateTagsAndFollowedBlogs()
        verify(repository).requestPosts(tagTarget, UpdateAction.REQUEST_REFRESH)
        assertThat(viewModel.uiState.value.isRefreshing).isTrue
    }

    @Test
    fun `pull to refresh on saved posts purges unbookmarked posts`() = test {
        val bookmarksTag = ReaderTestUtils.createTag("saved", ReaderTagType.BOOKMARKED)
        startAndLoad(args(tag = bookmarksTag))

        viewModel.onPullToRefresh()
        advanceUntilIdle()

        verify(repository).purgeUnbookmarkedPosts()
    }

    @Test
    fun `pull to refresh offline does nothing`() = test {
        startAndLoad()
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        viewModel.onPullToRefresh()

        verify(repository, never()).requestPosts(any(), eq(UpdateAction.REQUEST_REFRESH))
        assertThat(viewModel.uiState.value.isRefreshing).isFalse
    }

    @Test
    fun `saved posts list shows unbookmarked posts as removed`() = test {
        val bookmarksTag = ReaderTestUtils.createTag("saved", ReaderTagType.BOOKMARKED)
        whenever(repository.getPosts(ReaderPostListTarget.Tag(bookmarksTag)))
            .thenReturn(posts(1).apply { first().isBookmarked = false })

        startAndLoad(args(tag = bookmarksTag))

        assertThat(viewModel.uiState.value.items.single()).isInstanceOf(ReaderPostListItem.RemovedPost::class.java)
    }

    @Test
    fun `more menu opens, survives a reload and closes`() = test {
        val menuItems = listOf<ReaderPostCardAction>(PrimaryAction(true, type = ReaderPostCardActionType.SHARE))
        whenever(repository.getPosts(tagTarget)).thenReturn(posts(1))
        whenever(moreMenuBuilder.buildMoreMenuItems(any(), any(), any())).thenReturn(menuItems)
        startAndLoad()

        val card = cardState(0)
        card.onMoreButtonClicked(card)
        advanceUntilIdle()
        assertThat(cardState(0).moreMenuItems).isEqualTo(menuItems)

        endUpdate(UpdateResult.CHANGED)
        assertThat(cardState(0).moreMenuItems).isEqualTo(menuItems)

        cardState(0).onMoreDismissed(cardState(0))
        assertThat(cardState(0).moreMenuItems).isNull()
    }

    @Test
    fun `tapping a card emits the post`() = test {
        val posts = posts(1)
        whenever(repository.getPosts(tagTarget)).thenReturn(posts)
        startAndLoad()

        viewModel.uiEvents.filterIsInstance<ReaderPostListUiEvent.PostSelected>().test {
            cardState(0).onItemClicked(1, BLOG_ID)
            assertThat(awaitItem()).isEqualTo(ReaderPostListUiEvent.PostSelected(posts.first()))
        }
    }

    @Test
    fun `railcar is tracked once per post`() = test {
        whenever(repository.getPosts(tagTarget)).thenReturn(posts(1).apply { first().railcarJson = "{}" })
        startAndLoad()

        repeat(2) { cardState(0).onItemRendered(cardState(0)) }

        verify(readerTracker, times(1)).trackRailcar(any())
    }

    @Test
    fun `back goes to the previous tag in the history`() = test {
        val previousTag = ReaderTestUtils.createTag("dogs")
        whenever(repository.getFollowedTag("dogs")).thenReturn(previousTag)
        startAndLoad(args(listType = ReaderPostListType.TAG_PREVIEW, tagHistory = listOf("dogs", "cats")))

        assertThat(viewModel.goBackInTagHistory()).isTrue
        advanceUntilIdle()

        assertThat(viewModel.currentTag).isEqualTo(previousTag)
        assertThat(viewModel.currentArgs(0).tagHistory).containsExactly("dogs")
    }

    @Test
    fun `back is not handled when there's no earlier tag`() = test {
        startAndLoad(args(listType = ReaderPostListType.TAG_PREVIEW))

        assertThat(viewModel.goBackInTagHistory()).isFalse
    }

    @Test
    fun `first subfilter mode change does not request newer posts`() = test {
        startAndLoad()

        viewModel.changeReaderMode(modeInfo(isFirstLoad = true))
        advanceUntilIdle()

        verify(repository, never()).requestPosts(any(), eq(UpdateAction.REQUEST_NEWER))
        verify(repository, times(2)).getPosts(tagTarget)
    }

    @Test
    fun `selecting a subfilter requests newer posts`() = test {
        val siteTarget = ReaderPostListTarget.Blog(BLOG_ID)
        startAndLoad()

        viewModel.changeReaderMode(
            modeInfo(isFirstLoad = false).copy(tag = null, listType = ReaderPostListType.BLOG_PREVIEW, blogId = BLOG_ID)
        )
        advanceUntilIdle()

        assertThat(viewModel.listType).isEqualTo(ReaderPostListType.BLOG_PREVIEW)
        verify(repository).requestPosts(siteTarget, UpdateAction.REQUEST_NEWER)
    }

    @Test
    fun `replayed subfilter mode is ignored`() = test {
        startAndLoad()
        val info = modeInfo(isFirstLoad = true)
        viewModel.changeReaderMode(info)
        advanceUntilIdle()

        viewModel.changeReaderMode(info)
        advanceUntilIdle()

        verify(repository, times(2)).getPosts(tagTarget)
    }

    @Test
    fun `starting requests newer posts when it's time to update the tag`() = test {
        whenever(repository.shouldAutoUpdateTag(tag)).thenReturn(true)

        startAndLoad()

        verify(repository).requestPosts(tagTarget, UpdateAction.REQUEST_NEWER)
    }

    @Test
    fun `search results load once the search succeeds`() = test {
        val searchTarget = ReaderPostListTarget.Tag(
            ReaderTag("cats", "cats", "cats", null, ReaderTagType.SEARCH)
        )
        startAndLoad(ReaderPostListArgs(listType = ReaderPostListType.SEARCH_RESULTS))

        viewModel.onSearchSubmitted("cats")
        advanceUntilIdle()
        verify(repository).searchPosts("cats", 0)
        assertThat(viewModel.uiState.value.emptyState).isEqualTo(ReaderPostListEmptyState.SearchUpdating)

        repositoryEvents.emit(ReaderPostListEvent.SearchEnded("cats", 0, didSucceed = true))
        advanceUntilIdle()
        verify(repository, times(2)).getPosts(searchTarget)
        assertThat(viewModel.uiState.value.emptyState).isEqualTo(ReaderPostListEmptyState.SearchNoResults("cats"))
    }

    @Test
    fun `a successful search keeps showing as running until its results are loaded`() = test {
        val searchTarget = ReaderPostListTarget.Tag(
            ReaderTag("cats", "cats", "cats", null, ReaderTagType.SEARCH)
        )
        val resultsLoaded = CompletableDeferred<Unit>()
        var loads = 0
        whenever(repository.getPosts(searchTarget)).doSuspendableAnswer {
            // the first load is the empty list on submit, the second is the one after the search ends
            if (++loads > 1) resultsLoaded.await()
            ReaderPostList()
        }
        startAndLoad(ReaderPostListArgs(listType = ReaderPostListType.SEARCH_RESULTS))
        viewModel.onSearchSubmitted("cats")
        advanceUntilIdle()

        repositoryEvents.emit(ReaderPostListEvent.SearchEnded("cats", 0, didSucceed = true))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.emptyState).isEqualTo(ReaderPostListEmptyState.SearchUpdating)

        resultsLoaded.complete(Unit)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.emptyState).isEqualTo(ReaderPostListEmptyState.SearchNoResults("cats"))
    }

    @Test
    fun `re-running a failed search clears the error when it starts`() = test {
        startAndLoad(ReaderPostListArgs(listType = ReaderPostListType.SEARCH_RESULTS))
        viewModel.onSearchSubmitted("cats")
        advanceUntilIdle()
        repositoryEvents.emit(ReaderPostListEvent.SearchEnded("cats", 0, didSucceed = false))
        advanceUntilIdle()

        repositoryEvents.emit(ReaderPostListEvent.SearchStarted("cats", 0))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.emptyState).isEqualTo(ReaderPostListEmptyState.SearchUpdating)
    }

    @Test
    fun `failed search shows the search error`() = test {
        startAndLoad(ReaderPostListArgs(listType = ReaderPostListType.SEARCH_RESULTS))
        viewModel.onSearchSubmitted("cats")
        advanceUntilIdle()

        repositoryEvents.emit(ReaderPostListEvent.SearchEnded("cats", 0, didSucceed = false))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.emptyState).isEqualTo(ReaderPostListEmptyState.RequestFailed(true))
    }

    @Test
    fun `an older search ending doesn't end the current one`() = test {
        startAndLoad(ReaderPostListArgs(listType = ReaderPostListType.SEARCH_RESULTS))
        viewModel.onSearchSubmitted("cats")
        advanceUntilIdle()
        viewModel.onSearchSubmitted("dogs")
        advanceUntilIdle()

        repositoryEvents.emit(ReaderPostListEvent.SearchEnded("cats", 0, didSucceed = true))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.emptyState).isEqualTo(ReaderPostListEmptyState.SearchUpdating)
    }

    @Test
    fun `clearing the search before it starts cancels it`() = test {
        startAndLoad(ReaderPostListArgs(listType = ReaderPostListType.SEARCH_RESULTS))

        viewModel.onSearchSubmitted("cats")
        viewModel.onSearchCleared()
        advanceUntilIdle()

        verify(repository, never()).searchPosts(any(), any())
    }

    @Test
    fun `search shows no empty state before a query is submitted or after it's cleared`() = test {
        startAndLoad(ReaderPostListArgs(listType = ReaderPostListType.SEARCH_RESULTS))
        assertThat(viewModel.uiState.value.emptyState).isNull()

        viewModel.onSearchSubmitted("cats")
        advanceUntilIdle()
        viewModel.onSearchCleared()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.emptyState).isNull()
    }

    private suspend fun TestScope.startAndLoad(args: ReaderPostListArgs = args()) {
        viewModel.start(args, null, PHOTON_WIDTH, PHOTON_HEIGHT)
        viewModel.onStart()
        advanceUntilIdle()
    }

    private suspend fun TestScope.endUpdate(
        result: UpdateResult,
        action: UpdateAction = UpdateAction.REQUEST_NEWER
    ) {
        repositoryEvents.emit(ReaderPostListEvent.UpdateEnded(tag, action, result))
        advanceUntilIdle()
    }

    private fun args(
        listType: ReaderPostListType = ReaderPostListType.TAG_FOLLOWED,
        tag: ReaderTag = this.tag,
        tagHistory: List<String> = emptyList()
    ) = ReaderPostListArgs(listType = listType, tag = tag, originalTag = tag, tagHistory = tagHistory)

    private fun modeInfo(isFirstLoad: Boolean) = ReaderModeInfo(
        tag = tag,
        listType = ReaderPostListType.TAG_FOLLOWED,
        blogId = 0,
        feedId = 0,
        requestNewerPosts = true,
        label = null,
        isFirstLoad = isFirstLoad,
        isFiltered = false
    )

    private fun blogPreviewArgs() = ReaderPostListArgs(listType = ReaderPostListType.BLOG_PREVIEW, blogId = BLOG_ID)

    private fun blog(name: String = "blog", isFollowing: Boolean = false) = ReaderBlog().apply {
        blogId = BLOG_ID
        this.name = name
        this.isFollowing = isFollowing
    }

    private fun siteHeader() =
        (viewModel.uiState.value.items.first() as? ReaderPostListItem.SiteHeader)?.info

    private fun postIds() = viewModel.uiState.value.items
        .filterIsInstance<ReaderPostListItem.Post>()
        .map { it.uiState.postId }

    private fun cardState(index: Int) =
        viewModel.uiState.value.items.filterIsInstance<ReaderPostListItem.Post>()[index].uiState

    private fun posts(vararg ids: Long) = ReaderPostList().apply {
        ids.forEach { id ->
            add(ReaderPost().apply {
                postId = id
                blogId = BLOG_ID
                pseudoId = "pseudo-$id"
                isBookmarked = true
            })
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createUiState(invocation: InvocationOnMock): ReaderPostUiState {
        val post = invocation.getArgument<ReaderPost>(1)
        return ReaderPostUiState(
            source = "source",
            postId = post.postId,
            blogId = post.blogId,
            feedId = post.feedId,
            isFollowed = post.isFollowedByCurrentUser,
            blogSection = CompactBlogSectionData(
                post.postId, post.blogId, "", UiStringText(""), null, null, false, ImageType.BLAVATAR, null
            ),
            interactionSection = InteractionSectionData(0, 0),
            title = null,
            excerpt = null,
            featuredImageUrl = null,
            fullVideoUrl = null,
            thumbnailStripSection = null,
            videoOverlayVisibility = false,
            featuredImageVisibility = false,
            moreMenuVisibility = true,
            likeAction = PrimaryAction(true, type = ReaderPostCardActionType.LIKE),
            reblogAction = PrimaryAction(true, type = ReaderPostCardActionType.REBLOG),
            commentsAction = PrimaryAction(true, type = ReaderPostCardActionType.COMMENTS),
            moreMenuItems = invocation.getArgument(12),
            onItemClicked = invocation.getArgument(6),
            onItemRendered = invocation.getArgument(7),
            onMoreButtonClicked = invocation.getArgument(8),
            onMoreDismissed = invocation.getArgument(9),
            onVideoOverlayClicked = invocation.getArgument(10)
        )
    }

    companion object {
        private const val BLOG_ID = 100L
        private const val OTHER_BLOG_ID = 200L
        private const val FEED_ID = 300L
        private const val MAX_POSTS = 200
        private const val PHOTON_WIDTH = 1000
        private const val PHOTON_HEIGHT = 500
    }
}
