package org.wordpress.android.ui.engagement

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.ui.engagement.AuthorName.AuthorNameString
import org.wordpress.android.ui.engagement.EngageItem.LikedItem
import org.wordpress.android.ui.engagement.EngageItem.Liker
import org.wordpress.android.ui.engagement.EngageItem.NextLikesPageLoader
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.OpenUserProfileBottomSheet
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.PreviewCommentInReader
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.PreviewPostInReader
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.PreviewSiteById
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.PreviewSiteByUrl
import org.wordpress.android.ui.engagement.EngagedListServiceRequestEvent.RequestBlogPost
import org.wordpress.android.ui.engagement.EngagedListServiceRequestEvent.RequestComment
import org.wordpress.android.ui.engagement.EngagedPeopleListViewModel.EngagedPeopleListUiState
import org.wordpress.android.ui.engagement.EngagementNavigationSource.LIKE_NOTIFICATION_LIST
import org.wordpress.android.ui.engagement.EngagementNavigationSource.LIKE_READER_LIST
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.Failure
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.LikesData
import org.wordpress.android.ui.engagement.GetLikesUseCase.LikeGroupFingerPrint
import org.wordpress.android.ui.engagement.ListScenarioType.LOAD_COMMENT_LIKES
import org.wordpress.android.ui.engagement.ListScenarioType.LOAD_POST_LIKES
import org.wordpress.android.ui.engagement.utils.GetLikesTestConfig.TEST_CONFIG_1
import org.wordpress.android.ui.engagement.utils.GetLikesTestConfig.TEST_CONFIG_2
import org.wordpress.android.ui.engagement.utils.GetLikesTestConfig.TEST_CONFIG_3
import org.wordpress.android.ui.engagement.utils.GetLikesTestConfig.TEST_CONFIG_4
import org.wordpress.android.ui.engagement.utils.GetLikesTestConfig.TEST_CONFIG_5
import org.wordpress.android.ui.engagement.utils.generatesEquivalentLikedItem
import org.wordpress.android.ui.engagement.utils.getGetLikesState
import org.wordpress.android.ui.engagement.utils.isEqualTo
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.viewmodel.Event

@InternalCoroutinesApi
class EngagedPeopleListViewModelTest : BaseUnitTest() {
    @Mock lateinit var getLikesHandler: GetLikesHandler
    @Mock lateinit var readerUtilsWrapper: ReaderUtilsWrapper
    @Mock lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper
    @Mock lateinit var listScenario: ListScenario
    @Mock lateinit var headerData: HeaderData

    private lateinit var viewModel: EngagedPeopleListViewModel

    private val engagementUtils = EngagementUtils()

    private val snackbarEvents = MutableLiveData<Event<SnackbarMessageHolder>>()
    private val getLikesState = MutableLiveData<GetLikesState>()

    private var uiState: EngagedPeopleListUiState? = null
    private var navigationEvent: EngagedListNavigationEvent? = null
    private var serviceRequestEvent: MutableList<EngagedListServiceRequestEvent> = mutableListOf()
    private var holder: SnackbarMessageHolder? = null

    private val siteId = 100L
    private val postId = 1000L
    private val commentId = 10000L
    private val expectedNumLikes = 6

    @Before
    fun setup() {
        setupMocksForPostOrComment(LOAD_POST_LIKES)

        whenever(getLikesHandler.snackbarEvents).thenReturn(snackbarEvents)
        whenever(getLikesHandler.likesStatusUpdate).thenReturn(getLikesState)

        viewModel = EngagedPeopleListViewModel(
                TEST_DISPATCHER,
                TEST_DISPATCHER,
                getLikesHandler,
                readerUtilsWrapper,
                engagementUtils,
                analyticsUtilsWrapper
        )

        setupObservers()
    }

    @Test
    fun `onCleared call clear on likes handler`() {
        viewModel.onCleared()

        verify(getLikesHandler, times(1)).clear()
    }

    @Test
    fun `data refresh is requested on start`() = test {
        viewModel.start(listScenario)

        verify(getLikesHandler, times(1)).handleGetLikesForPost(
                LikeGroupFingerPrint(
                        siteId,
                        postId,
                        expectedNumLikes
                ),
                false
        )
    }

    @Test
    fun `post is requested for reader when post does not exist`() {
        whenever(readerUtilsWrapper.postExists(siteId, postId)).thenReturn(false)

        viewModel.start(listScenario)

        assertThat(serviceRequestEvent).isNotEmpty
        assertThat(serviceRequestEvent).isEqualTo(listOf(RequestBlogPost(siteId, postId)))
    }

    @Test
    fun `comment is requested for reader when comment does not exist`() {
        setupMocksForPostOrComment(LOAD_COMMENT_LIKES)

        whenever(readerUtilsWrapper.postExists(siteId, postId)).thenReturn(true)
        whenever(readerUtilsWrapper.commentExists(siteId, postId, commentId)).thenReturn(false)

        viewModel.start(listScenario)

        assertThat(serviceRequestEvent).isNotEmpty
        assertThat(serviceRequestEvent).isEqualTo(listOf(RequestComment(siteId, postId, commentId)))
    }

    @Test
    fun `post and comment are requested for reader when they do not exist`() {
        setupMocksForPostOrComment(LOAD_COMMENT_LIKES)

        whenever(readerUtilsWrapper.postExists(siteId, postId)).thenReturn(false)
        whenever(readerUtilsWrapper.commentExists(siteId, postId, commentId)).thenReturn(false)

        viewModel.start(listScenario)

        assertThat(serviceRequestEvent).isNotEmpty
        assertThat(serviceRequestEvent).isEqualTo(
                listOf(
                        RequestBlogPost(siteId, postId),
                        RequestComment(siteId, postId, commentId)
                )
        )
    }

    @Test
    fun `likes are requested for post`() = test {
        viewModel.start(listScenario)

        verify(getLikesHandler, times(1)).handleGetLikesForPost(
                LikeGroupFingerPrint(
                        siteId,
                        postId,
                        expectedNumLikes
                ),
                false
        )
    }

    @Test
    fun `likes are requested for comment`() = test {
        setupMocksForPostOrComment(LOAD_COMMENT_LIKES)

        viewModel.start(listScenario)

        verify(getLikesHandler, times(1)).handleGetLikesForComment(
                LikeGroupFingerPrint(
                        siteId,
                        commentId,
                        expectedNumLikes
                ),
                false
        )
    }

    @Test
    fun `uiState is updated with post liked item, likers and no page loader`() = test {
        val likesState = getGetLikesState(TEST_CONFIG_1)

        viewModel.start(listScenario)

        getLikesState.value = likesState

        requireNotNull(uiState).let {
            assertThat(it.showLoading).isFalse
            with(it.engageItemsList) {
                val likedItem = this.filterIsInstance<LikedItem>()
                val likerItems = this.filterIsInstance<Liker>()
                val pageLoader = this.filterIsInstance<NextLikesPageLoader>()

                assertThat(likedItem.size).isEqualTo(1)
                assertThat(listScenario.generatesEquivalentLikedItem(likedItem.first()))
                assertThat((likesState as LikesData).likes.isEqualTo(likerItems)).isTrue
                assertThat(pageLoader).isEmpty()
            }
        }
    }

    @Test
    fun `uiState is updated with post liked item, likers and page loader`() = test {
        val likesState = getGetLikesState(TEST_CONFIG_2)

        viewModel.start(listScenario)

        getLikesState.value = likesState

        requireNotNull(uiState).let {
            assertThat(it.showLoading).isFalse
            with(it.engageItemsList) {
                val likedItem = this.filterIsInstance<LikedItem>()
                val likerItems = this.filterIsInstance<Liker>()
                val pageLoader = this.filterIsInstance<NextLikesPageLoader>()

                assertThat(likedItem.size).isEqualTo(1)
                assertThat(listScenario.generatesEquivalentLikedItem(likedItem.first()))
                assertThat((likesState as LikesData).likes.isEqualTo(likerItems)).isTrue
                assertThat(pageLoader.size).isEqualTo(1)
            }
        }
    }

    @Test
    fun `uiState is updated with comment liked item, likers and no page loader`() = test {
        val likesState = getGetLikesState(TEST_CONFIG_3)

        setupMocksForPostOrComment(LOAD_COMMENT_LIKES)

        viewModel.start(listScenario)

        getLikesState.value = likesState

        requireNotNull(uiState).let {
            assertThat(it.showLoading).isFalse
            with(it.engageItemsList) {
                val likedItem = this.filterIsInstance<LikedItem>()
                val likerItems = this.filterIsInstance<Liker>()
                val pageLoader = this.filterIsInstance<NextLikesPageLoader>()

                assertThat(likedItem.size).isEqualTo(1)
                assertThat(listScenario.generatesEquivalentLikedItem(likedItem.first()))
                assertThat((likesState as LikesData).likes.isEqualTo(likerItems)).isTrue
                assertThat(pageLoader).isEmpty()
            }
        }
    }

    @Test
    fun `uiState is updated with comment liked item, likers and page loader`() = test {
        val likesState = getGetLikesState(TEST_CONFIG_4)

        setupMocksForPostOrComment(LOAD_COMMENT_LIKES)

        viewModel.start(listScenario)

        getLikesState.value = likesState

        requireNotNull(uiState).let {
            assertThat(it.showLoading).isFalse
            with(it.engageItemsList) {
                val likedItem = this.filterIsInstance<LikedItem>()
                val likerItems = this.filterIsInstance<Liker>()
                val pageLoader = this.filterIsInstance<NextLikesPageLoader>()

                assertThat(likedItem.size).isEqualTo(1)
                assertThat(listScenario.generatesEquivalentLikedItem(likedItem.first()))
                assertThat((likesState as LikesData).likes.isEqualTo(likerItems)).isTrue
                assertThat(pageLoader.size).isEqualTo(1)
            }
        }
    }

    @Test
    fun `uiState shows empty state on failure with no values in cache`() = test {
        val likesState = getGetLikesState(TEST_CONFIG_5)

        viewModel.start(listScenario)

        getLikesState.value = likesState

        requireNotNull(uiState).let {
            assertThat(it.showLoading).isFalse
            with(it.engageItemsList) {
                val likedItem = this.filterIsInstance<LikedItem>()
                val likerItems = this.filterIsInstance<Liker>()
                val pageLoader = this.filterIsInstance<NextLikesPageLoader>()

                assertThat(likedItem.size).isEqualTo(1)
                assertThat(listScenario.generatesEquivalentLikedItem(likedItem.first()))
                assertThat((likesState as Failure).cachedLikes.isEqualTo(likerItems)).isTrue
                assertThat(pageLoader.size).isEqualTo(0)
            }
            assertThat(it.showEmptyState).isTrue
        }
    }

    @Test
    fun `when user profile holder is clicked, sheet is opened`() {
        val likesState = getGetLikesState(TEST_CONFIG_1)

        viewModel.start(listScenario)

        getLikesState.value = likesState

        requireNotNull(uiState).let {
            (it.engageItemsList[1] as Liker).onClick!!.invoke(mock(), listScenario.source)

            requireNotNull(navigationEvent).let {
                assertThat(navigationEvent is OpenUserProfileBottomSheet).isTrue
            }
        }
    }

    @Test
    fun `when site holder with site id is clicked, the site is previewed in reader`() {
        val likesState = getGetLikesState(TEST_CONFIG_1)

        viewModel.start(listScenario)

        getLikesState.value = likesState

        requireNotNull(uiState).let {
            val likedItem = it.engageItemsList[0] as LikedItem
            likedItem.onGravatarClick.invoke(
                    likedItem.authorPreferredSiteId,
                    likedItem.authorPreferredSiteUrl,
                    likedItem.blogPreviewSource
            )

            requireNotNull(navigationEvent).let {
                assertThat(navigationEvent is PreviewSiteById).isTrue
            }
        }
    }

    @Test
    fun `when site holder with zero site id is clicked, the site is previewed in webview`() {
        val likesState = getGetLikesState(TEST_CONFIG_1)

        viewModel.start(listScenario)

        getLikesState.value = likesState

        requireNotNull(uiState).let {
            val likedItem = it.engageItemsList[0] as LikedItem
            likedItem.onGravatarClick.invoke(
                    0,
                    likedItem.authorPreferredSiteUrl,
                    likedItem.blogPreviewSource
            )

            requireNotNull(navigationEvent).let {
                assertThat(navigationEvent is PreviewSiteByUrl).isTrue
            }
        }
    }

    @Test
    fun `when header holder for comment is clicked, reader is opened if data is available`() {
        setupMocksForPostOrComment(LOAD_COMMENT_LIKES)

        val likesState = getGetLikesState(TEST_CONFIG_2)

        whenever(readerUtilsWrapper.postAndCommentExists(anyLong(), anyLong(), anyLong())).thenReturn(true)

        viewModel.start(listScenario)

        getLikesState.value = likesState

        requireNotNull(uiState).let {
            val likedItem = it.engageItemsList[0] as LikedItem
            likedItem.onHeaderClicked.invoke(
                    likedItem.likedItemSiteId,
                    likedItem.likedItemSiteUrl,
                    likedItem.likedItemId,
                    likedItem.likedItemPostId
            )

            requireNotNull(navigationEvent).let {
                assertThat(navigationEvent is PreviewCommentInReader).isTrue
            }
        }
    }

    @Test
    fun `when header holder for comment is clicked, webview is opened if data is not available`() {
        setupMocksForPostOrComment(LOAD_COMMENT_LIKES)

        val likesState = getGetLikesState(TEST_CONFIG_2)

        whenever(readerUtilsWrapper.postAndCommentExists(anyLong(), anyLong(), anyLong())).thenReturn(false)

        viewModel.start(listScenario)

        getLikesState.value = likesState

        requireNotNull(uiState).let {
            val likedItem = it.engageItemsList[0] as LikedItem
            likedItem.onHeaderClicked.invoke(
                    likedItem.likedItemSiteId,
                    likedItem.likedItemSiteUrl,
                    likedItem.likedItemId,
                    likedItem.likedItemPostId
            )

            requireNotNull(navigationEvent).let {
                assertThat(navigationEvent is PreviewSiteByUrl).isTrue
            }
        }
    }

    @Test
    fun `when header holder for post is clicked, reader is opened`() {
        val likesState = getGetLikesState(TEST_CONFIG_1)

        viewModel.start(listScenario)

        getLikesState.value = likesState

        requireNotNull(uiState).let {
            val likedItem = it.engageItemsList[0] as LikedItem
            likedItem.onHeaderClicked.invoke(
                    likedItem.likedItemSiteId,
                    likedItem.likedItemSiteUrl,
                    likedItem.likedItemId,
                    likedItem.likedItemPostId
            )

            requireNotNull(navigationEvent).let {
                assertThat(navigationEvent is PreviewPostInReader).isTrue
            }
        }
    }

    private fun setupMocksForPostOrComment(type: ListScenarioType) {
        whenever(headerData.authorName).thenReturn(AuthorNameString("authorName"))
        whenever(headerData.snippetText).thenReturn("snippetText")
        whenever(headerData.authorAvatarUrl).thenReturn("authorAvatarUrl")
        whenever(headerData.numLikes).thenReturn(expectedNumLikes)
        whenever(headerData.authorUserId).thenReturn(100)

        whenever(headerData.authorPreferredSiteId).thenReturn(1000)
        whenever(headerData.authorPreferredSiteUrl).thenReturn("authorPreferredSiteUrl")

        whenever(listScenario.commentSiteUrl).thenReturn("commentSiteUrl")

        when (type) {
            LOAD_POST_LIKES -> {
                whenever(listScenario.type).thenReturn(LOAD_POST_LIKES)
                whenever(listScenario.siteId).thenReturn(siteId)
                whenever(listScenario.postOrCommentId).thenReturn(postId)
                whenever(listScenario.commentPostId).thenReturn(0L)
                whenever(listScenario.headerData).thenReturn(headerData)
                whenever(listScenario.source).thenReturn(LIKE_READER_LIST)
            }
            LOAD_COMMENT_LIKES -> {
                whenever(listScenario.type).thenReturn(LOAD_COMMENT_LIKES)
                whenever(listScenario.siteId).thenReturn(siteId)
                whenever(listScenario.postOrCommentId).thenReturn(commentId)
                whenever(listScenario.commentPostId).thenReturn(postId)
                whenever(listScenario.headerData).thenReturn(headerData)
                whenever(listScenario.source).thenReturn(LIKE_NOTIFICATION_LIST)
            }
        }
    }

    private fun setupObservers() {
        uiState = null
        navigationEvent = null
        serviceRequestEvent.clear()
        holder = null

        viewModel.uiState.observeForever {
            uiState = it
        }

        viewModel.onNavigationEvent.observeForever {
            it.applyIfNotHandled {
                navigationEvent = this
            }
        }

        viewModel.onServiceRequestEvent.observeForever {
            it.applyIfNotHandled {
                serviceRequestEvent.add(this)
            }
        }

        viewModel.onSnackbarMessage.observeForever {
            it.applyIfNotHandled {
                holder = this
            }
        }
    }
}
