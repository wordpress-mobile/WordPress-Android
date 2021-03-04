package org.wordpress.android.ui.reader.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R.dimen
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.test
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.OpenEditorForReblog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ReplaceRelatedPostDetailsWithHistory
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowPostsByTag
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowRelatedPostDetails
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowSitePickerForResult
import org.wordpress.android.ui.reader.discover.ReaderPostActions
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.PrimaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BOOKMARK
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.COMMENTS
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.FOLLOW
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.LIKE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.REBLOG
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionsHandler
import org.wordpress.android.ui.reader.discover.ReaderPostMoreButtonUiStateBuilder
import org.wordpress.android.ui.reader.discover.interests.TagUiState
import org.wordpress.android.ui.reader.models.ReaderSimplePost
import org.wordpress.android.ui.reader.models.ReaderSimplePostList
import org.wordpress.android.ui.reader.reblog.ReblogUseCase
import org.wordpress.android.ui.reader.usecases.ReaderFetchRelatedPostsUseCase
import org.wordpress.android.ui.reader.usecases.ReaderFetchRelatedPostsUseCase.FetchRelatedPostsState
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.FollowStatusChanged
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.ReaderPostDetailsUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.ReaderPostDetailsUiState.RelatedPostsUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.ReaderPostDetailsUiState.RelatedPostsUiState.ReaderRelatedPostUiState
import org.wordpress.android.ui.reader.views.uistates.FollowButtonUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderBlogSectionUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderBlogSectionUiState.ReaderBlogSectionClickData
import org.wordpress.android.ui.reader.views.uistates.ReaderPostDetailsHeaderViewUiState.ReaderPostDetailsHeaderUiState
import org.wordpress.android.ui.utils.UiDimen.UIDimenRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.util.image.ImageType.BLAVATAR_CIRCULAR
import org.wordpress.android.viewmodel.Event

private const val POST_PARAM_POSITION = 0
private const val ON_BUTTON_CLICKED_PARAM_POSITION = 2
private const val ON_POST_BLOG_SECTION_CLICKED_PARAM_POSITION = 3
private const val ON_POST_FOLLOW_BUTTON_CLICKED_PARAM_POSITION = 4
private const val ON_TAG_CLICKED_PARAM_POSITION = 5

private const val IS_GLOBAL_RELATED_POSTS_PARAM_POSITION = 2
private const val ON_RELATED_POST_ITEM_CLICKED_PARAM_POSITION = 3

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderPostDetailViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: ReaderPostDetailViewModel

    @Mock private lateinit var readerPostCardActionsHandler: ReaderPostCardActionsHandler
    @Mock private lateinit var readerUtilsWrapper: ReaderUtilsWrapper
    @Mock private lateinit var postDetailsUiStateBuilder: ReaderPostDetailUiStateBuilder
    @Mock private lateinit var readerPostTableWrapper: ReaderPostTableWrapper
    @Mock private lateinit var menuUiStateBuilder: ReaderPostMoreButtonUiStateBuilder
    @Mock private lateinit var reblogUseCase: ReblogUseCase
    @Mock private lateinit var readerFetchRelatedPostsUseCase: ReaderFetchRelatedPostsUseCase
    @Mock private lateinit var eventBusWrapper: EventBusWrapper
    @Mock private lateinit var readerSimplePost: ReaderSimplePost
    @Mock private lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper

    private val fakePostFollowStatusChangedFeed = MutableLiveData<FollowStatusChanged>()
    private val fakeRefreshPostFeed = MutableLiveData<Event<Unit>>()
    private val fakeNavigationFeed = MutableLiveData<Event<ReaderNavigationEvents>>()
    private val fakeSnackBarFeed = MutableLiveData<Event<SnackbarMessageHolder>>()

    private val readerPost = createDummyReaderPost(2)

    private lateinit var relatedPosts: ReaderSimplePostList

    @Before
    fun setUp() = test {
        viewModel = ReaderPostDetailViewModel(
                readerPostCardActionsHandler,
                readerUtilsWrapper,
                readerPostTableWrapper,
                menuUiStateBuilder,
                postDetailsUiStateBuilder,
                reblogUseCase,
                readerFetchRelatedPostsUseCase,
                analyticsUtilsWrapper,
                eventBusWrapper,
                TEST_DISPATCHER,
                TEST_DISPATCHER
        )
        whenever(readerPostCardActionsHandler.followStatusUpdated).thenReturn(fakePostFollowStatusChangedFeed)
        whenever(readerPostCardActionsHandler.refreshPosts).thenReturn(fakeRefreshPostFeed)
        whenever(readerPostCardActionsHandler.navigationEvents).thenReturn(fakeNavigationFeed)
        whenever(readerPostCardActionsHandler.snackbarEvents).thenReturn(fakeSnackBarFeed)

        whenever(readerUtilsWrapper.getTagFromTagName(anyOrNull(), anyOrNull())).thenReturn(mock())

        whenever(readerPostTableWrapper.getBlogPost(
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
        )).thenReturn(readerPost)

        whenever(
                postDetailsUiStateBuilder.mapPostToUiState(
                        anyOrNull(),
                        anyOrNull(),
                        anyOrNull(),
                        anyOrNull(),
                        anyOrNull(),
                        anyOrNull()
                )
        ).thenAnswer {
            val post = it.getArgument<ReaderPost>(POST_PARAM_POSITION)
            // propagate some of the arguments
            createDummyReaderPostDetailsUiState(
                    post,
                    it.getArgument(ON_TAG_CLICKED_PARAM_POSITION),
                    it.getArgument(ON_BUTTON_CLICKED_PARAM_POSITION),
                    it.getArgument(ON_POST_BLOG_SECTION_CLICKED_PARAM_POSITION),
                    it.getArgument(ON_POST_FOLLOW_BUTTON_CLICKED_PARAM_POSITION)
            )
        }

        relatedPosts = ReaderSimplePostList().apply { add(readerSimplePost) }

        whenever(
                postDetailsUiStateBuilder.mapRelatedPostsToUiState(
                        anyOrNull(),
                        anyOrNull(),
                        anyOrNull(),
                        anyOrNull()
                )
        ).thenAnswer {
            // propagate some of the arguments
            createDummyRelatedPostsUiState(
                    it.getArgument(IS_GLOBAL_RELATED_POSTS_PARAM_POSITION),
                    it.getArgument(ON_RELATED_POST_ITEM_CLICKED_PARAM_POSITION)
            )
        }

        whenever(reblogUseCase.onReblogSiteSelected(ArgumentMatchers.anyInt(), anyOrNull())).thenReturn(mock())
        whenever(reblogUseCase.convertReblogStateToNavigationEvent(anyOrNull())).thenReturn(mock<OpenEditorForReblog>())
    }

    @Test
    fun `when post show is triggered, then ui is updated`() = test {
        val uiStates = init().uiStates

        assertThat(uiStates.size).isEqualTo(1)
        assertThat(uiStates.first()).isInstanceOf(ReaderPostDetailsUiState::class.java)
    }

    @Test
    fun `when post is updated, then ui is updated`() = test {
        val uiStates = init().uiStates

        viewModel.onUpdatePost(readerPost)

        assertThat(uiStates.size).isEqualTo(2)
        assertThat(uiStates.last()).isInstanceOf(ReaderPostDetailsUiState::class.java)
    }

    @Test
    fun `when like button is clicked, then like action is invoked`() = test {
        val uiStates = init().uiStates

        uiStates.last().actions.likeAction.onClicked!!.invoke(readerPost.postId, 200, LIKE)

        verify(readerPostCardActionsHandler).onAction(
                eq(readerPost),
                eq(LIKE),
                eq(false),
                eq(true)
        )
    }

    @Test
    fun `when comments button is clicked, then comments action is invoked`() = test {
        val uiStates = init().uiStates

        uiStates.last().actions.commentsAction.onClicked!!.invoke(readerPost.postId, 200, COMMENTS)

        verify(readerPostCardActionsHandler).onAction(
                eq(readerPost),
                eq(COMMENTS),
                eq(false),
                eq(true)
        )
    }

    @Test
    fun `when reblog button is clicked, then reblog action is invoked`() = test {
        val uiStates = init().uiStates

        uiStates.last().actions.commentsAction.onClicked!!.invoke(readerPost.postId, 200, REBLOG)

        verify(readerPostCardActionsHandler).onAction(
                eq(readerPost),
                eq(REBLOG),
                eq(false),
                eq(true)
        )
    }

    @Test
    fun `when site is picked for reblog action, then editor is opened for reblog`() {
        val navigaitonObserver = init().navigation
        fakeNavigationFeed.value = Event(ShowSitePickerForResult(mock(), mock(), mock()))

        viewModel.onReblogSiteSelected(1)

        assertThat(navigaitonObserver.last().peekContent()).isInstanceOf(OpenEditorForReblog::class.java)
    }

    @Test
    fun `when bookmark button is clicked, then bookmark action is invoked`() = test {
        val uiStates = init().uiStates

        uiStates.last().actions.commentsAction.onClicked!!.invoke(readerPost.postId, 200, BOOKMARK)

        verify(readerPostCardActionsHandler).onAction(
                eq(readerPost),
                eq(BOOKMARK),
                eq(false),
                eq(true)
        )
    }

    @Test
    fun `when tag is clicked, then posts for tag are shown`() = test {
        val observers = init()

        observers.uiStates.last().headerUiState.tagItems[0].onClick!!.invoke("t")

        assertThat(observers.navigation[0].peekContent()).isInstanceOf(ShowPostsByTag::class.java)
    }

    @Test
    fun `when header blog section is clicked, then selected blog's header click action is invoked`() = test {
        val uiStates = init().uiStates

        uiStates.last().headerUiState.blogSectionUiState.blogSectionClickData!!.onBlogSectionClicked!!
                .invoke(readerPost.postId, readerPost.blogId)

        verify(readerPostCardActionsHandler).handleHeaderClicked(
                eq(readerPost.blogId),
                eq(readerPost.feedId)
        )
    }

    @Test
    fun `when more button is clicked, then more menu is shown`() = test {
        val uiStates = init().uiStates

        viewModel.onMoreButtonClicked()

        assertThat(uiStates.last().moreMenuItems).isNotNull
    }

    @Test
    fun `when user dismisses the menu, then more menu is not shown`() = test {
        val uiStates = init().uiStates

        viewModel.onMoreMenuDismissed()

        assertThat(uiStates.last().moreMenuItems).isNull()
    }

    @Test
    fun `when more menu list item is clicked, then corresponding action is invoked`() = test {
        init().uiStates

        viewModel.onMoreMenuItemClicked(FOLLOW)

        verify(readerPostCardActionsHandler).onAction(
                eq(readerPost),
                eq(FOLLOW),
                eq(false),
                eq(true)
        )
    }

    @Test
    fun `when header follow button is clicked, then follow action is invoked`() = test {
        val uiStates = init().uiStates

        uiStates.last().headerUiState.followButtonUiState.onFollowButtonClicked!!.invoke()

        verify(readerPostCardActionsHandler).onAction(
                eq(readerPost),
                eq(FOLLOW),
                eq(false),
                eq(true)
        )
    }

    @Test
    fun `given local related posts fetch succeeds, when related posts are requested, then local related posts shown`() =
            test {
                val localRelatedPostsUiState = createDummyRelatedPostsUiState(isGlobal = false)
                whenever(
                        postDetailsUiStateBuilder.mapRelatedPostsToUiState(
                                sourcePost = eq(readerPost),
                                relatedPosts = eq(relatedPosts),
                                isGlobal = eq(false),
                                onItemClicked = any()
                        )
                ).thenReturn(localRelatedPostsUiState)
                whenever(readerFetchRelatedPostsUseCase.fetchRelatedPosts(readerPost))
                        .thenReturn(
                                FetchRelatedPostsState.Success(
                                        localRelatedPosts = relatedPosts,
                                        globalRelatedPosts = ReaderSimplePostList()
                                )
                        )
                val uiStates = init().uiStates

                viewModel.onRelatedPostsRequested(readerPost)

                assertThat(uiStates.last().localRelatedPosts).isEqualTo(localRelatedPostsUiState)
            }

    @Test
    fun `given global related posts fetch succeeds, when related posts requested, then global related posts shown`() =
            test {
                val globalRelatedPostsUiState = createDummyRelatedPostsUiState(isGlobal = false)
                whenever(
                        postDetailsUiStateBuilder.mapRelatedPostsToUiState(
                                sourcePost = eq(readerPost),
                                relatedPosts = eq(relatedPosts),
                                isGlobal = eq(true),
                                onItemClicked = any()
                        )
                ).thenReturn(globalRelatedPostsUiState)
                whenever(readerFetchRelatedPostsUseCase.fetchRelatedPosts(readerPost))
                        .thenReturn(
                                FetchRelatedPostsState.Success(
                                        localRelatedPosts = ReaderSimplePostList(),
                                        globalRelatedPosts = relatedPosts
                                )
                        )
                val uiStates = init().uiStates

                viewModel.onRelatedPostsRequested(readerPost)

                assertThat(uiStates.last().globalRelatedPosts).isEqualTo(globalRelatedPostsUiState)
            }

    @Test
    fun `given related posts fetch fails, when related posts are requested, then related posts are not shown`() =
            test {
                whenever(readerFetchRelatedPostsUseCase.fetchRelatedPosts(readerPost))
                        .thenReturn(FetchRelatedPostsState.Failed.RequestFailed)
                val uiStates = init().uiStates

                viewModel.onRelatedPostsRequested(readerPost)

                with(uiStates.last()) {
                    assertThat(localRelatedPosts).isNull()
                    assertThat(globalRelatedPosts).isNull()
                }
            }

    @Test
    fun `given no network, when related posts are requested, then related posts are not shown`() =
            test {
                whenever(readerFetchRelatedPostsUseCase.fetchRelatedPosts(readerPost))
                        .thenReturn(FetchRelatedPostsState.Failed.NoNetwork)
                val uiStates = init().uiStates

                viewModel.onRelatedPostsRequested(readerPost)

                with(uiStates.last()) {
                    assertThat(localRelatedPosts).isNull()
                    assertThat(globalRelatedPosts).isNull()
                }
            }

    @Test
    fun `given related posts fetch in progress, when related posts are requested, then related posts are not shown`() =
            test {
                whenever(readerFetchRelatedPostsUseCase.fetchRelatedPosts(readerPost))
                        .thenReturn(FetchRelatedPostsState.AlreadyRunning)
                val uiStates = init().uiStates

                viewModel.onRelatedPostsRequested(readerPost)

                with(uiStates.last()) {
                    assertThat(localRelatedPosts).isNull()
                    assertThat(globalRelatedPosts).isNull()
                }
            }

    @Test
    fun `given wp com post, when related posts are requested, then related posts are fetched`() =
            test {
                whenever(readerFetchRelatedPostsUseCase.fetchRelatedPosts(readerPost)).thenReturn(mock())

                viewModel.onRelatedPostsRequested(readerPost)

                verify(readerFetchRelatedPostsUseCase).fetchRelatedPosts(readerPost)
            }

    @Test
    fun `given non wp com post, when related posts are requested, then related posts are not fetched`() =
            test {
                val nonWpComPost = createDummyReaderPost(id = 1, isWpComPost = false)

                viewModel.onRelatedPostsRequested(nonWpComPost)

                verify(readerFetchRelatedPostsUseCase, times(0)).fetchRelatedPosts(readerPost)
            }

    @Test
    fun `when related post is clicked from non related post details screen, then related post details screen shown`() =
            test {
                whenever(readerFetchRelatedPostsUseCase.fetchRelatedPosts(readerPost))
                        .thenReturn(
                                FetchRelatedPostsState.Success(
                                        localRelatedPosts = ReaderSimplePostList(),
                                        globalRelatedPosts = relatedPosts
                                )
                        )
                val observers = init(isRelatedPost = false)

                viewModel.onRelatedPostsRequested(readerPost)
                val relatedPost = observers.uiStates.last().globalRelatedPosts?.cards?.first()
                relatedPost?.onItemClicked?.invoke(relatedPost.postId, relatedPost.blogId, relatedPost.isGlobal)

                assertThat(observers.navigation[0].peekContent()).isInstanceOf(ShowRelatedPostDetails::class.java)
            }

    @Test
    fun `when related post is clicked from related post screen, then related post replaced with history`() =
            test {
                whenever(readerFetchRelatedPostsUseCase.fetchRelatedPosts(readerPost))
                        .thenReturn(
                                FetchRelatedPostsState.Success(
                                        localRelatedPosts = ReaderSimplePostList(),
                                        globalRelatedPosts = relatedPosts
                                )
                        )
                val observers = init(isRelatedPost = true)

                viewModel.onRelatedPostsRequested(readerPost)
                val relatedPost = observers.uiStates.last().globalRelatedPosts?.cards?.first()
                relatedPost?.onItemClicked?.invoke(relatedPost.postId, relatedPost.blogId, relatedPost.isGlobal)

                assertThat(observers.navigation[0].peekContent())
                        .isInstanceOf(ReplaceRelatedPostDetailsWithHistory::class.java)
            }

    private fun createDummyReaderPost(id: Long, isWpComPost: Boolean = true): ReaderPost = ReaderPost().apply {
        this.postId = id
        this.blogId = id * 100
        this.feedId = id * 1000
        this.title = "DummyPost"
        this.featuredVideo = id.toString()
        this.isExternal = !isWpComPost
    }

    private fun createDummyReaderPostDetailsUiState(
        post: ReaderPost,
        onTagClicked: (String) -> Unit,
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit,
        onBlogSectionClicked: (Long, Long) -> Unit,
        onFollowButtonClicked: (() -> Unit)
    ): ReaderPostDetailsUiState {
        return ReaderPostDetailsUiState(
                postId = post.postId,
                blogId = post.blogId,
                headerUiState = ReaderPostDetailsHeaderUiState(
                        UiStringText(post.title),
                        post.authorName,
                        listOf(TagUiState("", "", false, onTagClicked)),
                        true,
                        ReaderBlogSectionUiState(
                            postId = post.postId,
                            blogId = post.blogId,
                            dateLine = "",
                            blogName = mock(),
                            blogUrl = "",
                            avatarOrBlavatarUrl = "",
                            authorAvatarUrl = "",
                            isAuthorAvatarVisible = false,
                            blavatarType = BLAVATAR_CIRCULAR,
                            blogSectionClickData = ReaderBlogSectionClickData(onBlogSectionClicked, 0)
                        ),
                        FollowButtonUiState(
                            onFollowButtonClicked = onFollowButtonClicked,
                            isFollowed = false,
                            isEnabled = true,
                            isVisible = true
                        ),
                        ""
                ),
                moreMenuItems = mock(),
                actions = ReaderPostActions(
                    bookmarkAction = PrimaryAction(true, onClicked = onButtonClicked, type = BOOKMARK),
                    likeAction = PrimaryAction(true, onClicked = onButtonClicked, type = LIKE),
                    reblogAction = PrimaryAction(true, onClicked = onButtonClicked, type = REBLOG),
                    commentsAction = PrimaryAction(true, onClicked = onButtonClicked, type = COMMENTS)
                )
        )
    }

    private fun createDummyRelatedPostsUiState(
        isGlobal: Boolean,
        onRelatedPostItemClicked: ((Long, Long, Boolean) -> Unit)? = null
    ) = RelatedPostsUiState(
            cards = relatedPosts.map {
                ReaderRelatedPostUiState(
                        postId = it.postId,
                        blogId = it.siteId,
                        isGlobal = isGlobal,
                        title = UiStringText(""),
                        excerpt = UiStringText(""),
                        featuredImageUrl = "",
                        featuredImageVisibility = false,
                        featuredImageCornerRadius = UIDimenRes(dimen.reader_featured_image_corner_radius),
                        onItemClicked = onRelatedPostItemClicked ?: mock()
                )
            },
            isGlobal = isGlobal,
            headerLabel = UiStringText("")
    )

    private fun init(showPost: Boolean = true, isRelatedPost: Boolean = false): Observers {
        val uiStates = mutableListOf<ReaderPostDetailsUiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        val navigation = mutableListOf<Event<ReaderNavigationEvents>>()
        viewModel.navigationEvents.observeForever {
            navigation.add(it)
        }
        val msgs = mutableListOf<Event<SnackbarMessageHolder>>()
        viewModel.snackbarEvents.observeForever {
            msgs.add(it)
        }
        viewModel.start(isRelatedPost)

        if (showPost) {
            viewModel.onShowPost(readerPost)
        }

        return Observers(
                uiStates,
                navigation,
                msgs
        )
    }

    private data class Observers(
        val uiStates: List<ReaderPostDetailsUiState>,
        val navigation: List<Event<ReaderNavigationEvents>>,
        val snackbarMsgs: List<Event<SnackbarMessageHolder>>
    )
}
