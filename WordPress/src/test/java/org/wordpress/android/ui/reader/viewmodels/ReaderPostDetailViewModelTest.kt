package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.datasets.wrappers.ReaderCommentTableWrapper
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.LikeModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderComment
import org.wordpress.android.models.ReaderCommentList
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.AvatarItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.TrailingLabelTextItem
import org.wordpress.android.ui.engagement.EngageItem.Liker
import org.wordpress.android.ui.engagement.EngagementUtils
import org.wordpress.android.ui.engagement.GetLikesHandler
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.Failure
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.LikesData
import org.wordpress.android.ui.engagement.utils.GetLikesTestConfig.TEST_CONFIG_1
import org.wordpress.android.ui.engagement.utils.GetLikesTestConfig.TEST_CONFIG_5
import org.wordpress.android.ui.engagement.utils.getGetLikesState
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.ReaderPostDetailUiStateBuilder
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.OpenEditorForReblog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.OpenUrl
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ReplaceRelatedPostDetailsWithHistory
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowEngagedPeopleList
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowMediaPreview
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowPostInWebView
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
import org.wordpress.android.ui.reader.services.comment.wrapper.ReaderCommentServiceStarterWrapper
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase.FetchReaderPostState
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase.FetchReaderPostState.AlreadyRunning
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase.FetchReaderPostState.Failed
import org.wordpress.android.ui.reader.usecases.ReaderFetchRelatedPostsUseCase
import org.wordpress.android.ui.reader.usecases.ReaderFetchRelatedPostsUseCase.FetchRelatedPostsState
import org.wordpress.android.ui.reader.usecases.ReaderGetPostUseCase
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.FollowStatusChanged
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.CommentSnippetUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.TrainOfFacesUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ErrorUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.LoadingUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState.RelatedPostsUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState.RelatedPostsUiState.ReaderRelatedPostUiState
import org.wordpress.android.ui.reader.views.uistates.FollowButtonUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderBlogSectionUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderBlogSectionUiState.ReaderBlogSectionClickData
import org.wordpress.android.ui.reader.views.uistates.ReaderPostDetailsHeaderViewUiState.ReaderPostDetailsHeaderUiState
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiDimen.UIDimenRes
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.WpUrlUtilsWrapper
import org.wordpress.android.util.config.CommentsSnippetFeatureConfig
import org.wordpress.android.util.config.LikesEnhancementsFeatureConfig
import org.wordpress.android.util.image.ImageType.BLAVATAR_CIRCULAR
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.Event

private const val POST_PARAM_POSITION = 0
private const val ON_BUTTON_CLICKED_PARAM_POSITION = 2
private const val ON_POST_BLOG_SECTION_CLICKED_PARAM_POSITION = 3
private const val ON_POST_FOLLOW_BUTTON_CLICKED_PARAM_POSITION = 4
private const val ON_TAG_CLICKED_PARAM_POSITION = 5

private const val IS_GLOBAL_RELATED_POSTS_PARAM_POSITION = 2
private const val ON_RELATED_POST_ITEM_CLICKED_PARAM_POSITION = 3

private const val INTERCEPTED_URI = "intercepted uri"

@Suppress("LargeClass")
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class ReaderPostDetailViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ReaderPostDetailViewModel

    @Mock private lateinit var readerPostCardActionsHandler: ReaderPostCardActionsHandler
    @Mock private lateinit var readerUtilsWrapper: ReaderUtilsWrapper
    @Mock private lateinit var postDetailsUiStateBuilder: ReaderPostDetailUiStateBuilder
    @Mock private lateinit var readerPostTableWrapper: ReaderPostTableWrapper
    @Mock private lateinit var menuUiStateBuilder: ReaderPostMoreButtonUiStateBuilder
    @Mock private lateinit var reblogUseCase: ReblogUseCase
    @Mock private lateinit var readerFetchRelatedPostsUseCase: ReaderFetchRelatedPostsUseCase
    @Mock private lateinit var readerGetPostUseCase: ReaderGetPostUseCase
    @Mock private lateinit var readerFetchPostUseCase: ReaderFetchPostUseCase
    @Mock private lateinit var eventBusWrapper: EventBusWrapper
    @Mock private lateinit var readerSimplePost: ReaderSimplePost
    @Mock private lateinit var readerTracker: ReaderTracker
    @Mock private lateinit var siteStore: SiteStore
    @Mock private lateinit var accountStore: AccountStore
    @Mock private lateinit var wpUrlUtilsWrapper: WpUrlUtilsWrapper
    @Mock private lateinit var getLikesHandler: GetLikesHandler
    @Mock private lateinit var likesEnhancementsFeatureConfig: LikesEnhancementsFeatureConfig
    @Mock private lateinit var contextProvider: ContextProvider
    @Mock private lateinit var engagementUtils: EngagementUtils
    @Mock private lateinit var htmlMessageUtils: HtmlMessageUtils
    @Mock private lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock private lateinit var commentsSnippetFeatureConfig: CommentsSnippetFeatureConfig
    @Mock private lateinit var readerCommentTableWrapper: ReaderCommentTableWrapper
    @Mock private lateinit var readerCommentServiceStarterWrapper: ReaderCommentServiceStarterWrapper

    private val fakePostFollowStatusChangedFeed = MutableLiveData<FollowStatusChanged>()
    private val fakeRefreshPostFeed = MutableLiveData<Event<Unit>>()
    private val fakeNavigationFeed = MutableLiveData<Event<ReaderNavigationEvents>>()
    private val fakeSnackBarFeed = MutableLiveData<Event<SnackbarMessageHolder>>()
    private val getLikesState = MutableLiveData<GetLikesState>()

    private lateinit var likesCaptor: KArgumentCaptor<List<LikeModel>>

    private val snackbarEvents = MutableLiveData<Event<SnackbarMessageHolder>>()

    private val readerPost = createDummyReaderPost(2)
    private val readerCommentSnippetList = createDummyReaderPostCommentSnippetList()
    private val site = SiteModel().apply { siteId = readerPost.blogId }

    private lateinit var relatedPosts: ReaderSimplePostList

    @Before
    @Suppress("LongMethod")
    fun setUp() = test {
        viewModel = ReaderPostDetailViewModel(
                readerPostCardActionsHandler,
                readerUtilsWrapper,
                readerPostTableWrapper,
                menuUiStateBuilder,
                postDetailsUiStateBuilder,
                reblogUseCase,
                readerFetchRelatedPostsUseCase,
                readerGetPostUseCase,
                readerFetchPostUseCase,
                siteStore,
                accountStore,
                readerTracker,
                eventBusWrapper,
                wpUrlUtilsWrapper,
                TEST_DISPATCHER,
                TEST_DISPATCHER,
                TEST_DISPATCHER,
                getLikesHandler,
                likesEnhancementsFeatureConfig,
                engagementUtils,
                htmlMessageUtils,
                contextProvider,
                networkUtilsWrapper,
                commentsSnippetFeatureConfig,
                readerCommentTableWrapper,
                readerCommentServiceStarterWrapper
        )
        whenever(readerGetPostUseCase.get(any(), any(), any())).thenReturn(Pair(readerPost, false))
        whenever(readerPostCardActionsHandler.followStatusUpdated).thenReturn(fakePostFollowStatusChangedFeed)
        whenever(readerPostCardActionsHandler.refreshPosts).thenReturn(fakeRefreshPostFeed)
        whenever(readerPostCardActionsHandler.navigationEvents).thenReturn(fakeNavigationFeed)
        whenever(readerPostCardActionsHandler.snackbarEvents).thenReturn(fakeSnackBarFeed)
        whenever(siteStore.getSiteBySiteId(readerPost.blogId)).thenReturn(site)

        whenever(readerUtilsWrapper.getTagFromTagName(anyOrNull(), anyOrNull())).thenReturn(mock())

        whenever(
                readerPostTableWrapper.getBlogPost(
                        anyOrNull(),
                        anyOrNull(),
                        anyOrNull()
                )
        ).thenReturn(readerPost)

        whenever(
                readerCommentTableWrapper.getCommentsForPostSnippet(
                        anyOrNull(),
                        anyOrNull()
                )
        ).thenReturn(readerCommentSnippetList)

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

        whenever(
                postDetailsUiStateBuilder.buildCommentSnippetUiState(
                        anyOrNull(),
                        anyOrNull(),
                        anyOrNull()
                )
        ).thenAnswer {
            createDummyCommentSnippetUiState()
        }

        whenever(reblogUseCase.onReblogSiteSelected(ArgumentMatchers.anyInt(), anyOrNull())).thenReturn(mock())
        whenever(reblogUseCase.convertReblogStateToNavigationEvent(anyOrNull())).thenReturn(mock<OpenEditorForReblog>())

        whenever(likesEnhancementsFeatureConfig.isEnabled()).thenReturn(true)
        whenever(getLikesHandler.snackbarEvents).thenReturn(snackbarEvents)
        whenever(getLikesHandler.likesStatusUpdate).thenReturn(getLikesState)

        whenever(commentsSnippetFeatureConfig.isEnabled()).thenReturn(true)

        likesCaptor = argumentCaptor()
    }

    /* SHOW POST - LOADING */
    @Test
    fun `given local post not found, when show post is triggered, then loading state is shown`() =
            testWithoutLocalPost {
                val observers = init(showPost = false)

                viewModel.onShowPost(blogId = readerPost.blogId, postId = readerPost.postId)

                assertThat(observers.uiStates.first()).isEqualTo(LoadingUiState)
            }

    @Test
    fun `given local post found, when show post is triggered, then loading state is not shown`() = test {
        val observers = init(showPost = false)
        whenever(readerGetPostUseCase.get(anyLong(), anyLong(), anyBoolean())).thenReturn(Pair(readerPost, false))

        viewModel.onShowPost(blogId = readerPost.blogId, postId = readerPost.postId)

        assertThat(observers.uiStates.first()).isNotInstanceOf(LoadingUiState::class.java)
    }

    /* SHOW POST - GET LOCAL POST */
    @Test
    fun `given local post is found, when show post is triggered, then ui is updated`() = test {
        val observers = init(showPost = false)
        whenever(readerGetPostUseCase.get(anyLong(), anyLong(), anyBoolean())).thenReturn(Pair(readerPost, false))

        viewModel.onShowPost(blogId = readerPost.blogId, postId = readerPost.postId)

        assertThat(observers.uiStates.last()).isInstanceOf(ReaderPostDetailsUiState::class.java)
    }

    @Test
    fun `given local post is found, when show post is triggered, then post is updated in webview`() = test {
        val observers = init(showPost = false)
        whenever(readerGetPostUseCase.get(anyLong(), anyLong(), anyBoolean())).thenReturn(Pair(readerPost, false))

        viewModel.onShowPost(blogId = readerPost.blogId, postId = readerPost.postId)

        assertThat(observers.navigation.last().peekContent()).isInstanceOf(ShowPostInWebView::class.java)
    }

    @Test
    fun `given local post not found, when show post is triggered, then post is fetched from remote server`() =
            testWithoutLocalPost {
                init(showPost = false)

                viewModel.onShowPost(blogId = readerPost.blogId, postId = readerPost.postId)

                verify(readerFetchPostUseCase).fetchPost(readerPost.blogId, readerPost.postId, false)
            }

    /* SHOW POST - FETCH SUCCESS HANDLING */
    @Test
    fun `given request succeeded, when post is fetched, then post details ui is updated`() = test {
        val observers = init()
        whenever(readerGetPostUseCase.get(any(), any(), any()))
                .thenReturn(Pair(null, false))
                .thenReturn(Pair(readerPost, false))
        whenever(readerFetchPostUseCase.fetchPost(anyLong(), anyLong(), anyBoolean()))
                .thenReturn(FetchReaderPostState.Success)

        viewModel.onShowPost(blogId = readerPost.blogId, postId = readerPost.postId)

        val postDetailsUiStates = observers.uiStates.filterIsInstance<ReaderPostDetailsUiState>()
        assertThat(postDetailsUiStates.size).isEqualTo(2)
    }

    @Test
    fun `given request succeeded, when post is fetched, then post is shown in web view`() = test {
        val observers = init()
        whenever(readerGetPostUseCase.get(any(), any(), any()))
                .thenReturn(Pair(null, false))
                .thenReturn(Pair(readerPost, false))
        whenever(readerFetchPostUseCase.fetchPost(anyLong(), anyLong(), anyBoolean()))
                .thenReturn(FetchReaderPostState.Success)

        viewModel.onShowPost(blogId = readerPost.blogId, postId = readerPost.postId)

        assertThat(observers.navigation.last().peekContent()).isInstanceOf(ShowPostInWebView::class.java)
    }

    /* SHOW POST - FETCH ERROR HANDLING */
    @Test
    fun `given no network, when post is fetched, then no network message is shown`() = testWithoutLocalPost {
        val observers = init()
        whenever(readerFetchPostUseCase.fetchPost(anyLong(), anyLong(), anyBoolean())).thenReturn(Failed.NoNetwork)

        viewModel.onShowPost(blogId = readerPost.blogId, postId = readerPost.postId)

        assertThat(observers.uiStates.last()).isEqualTo(ErrorUiState(UiStringRes(R.string.no_network_message)))
    }

    @Test
    fun `given request failed, when post is fetched, then request failed message is shown`() = testWithoutLocalPost {
        val observers = init()
        whenever(readerFetchPostUseCase.fetchPost(anyLong(), anyLong(), anyBoolean()))
                .thenReturn(Failed.RequestFailed)

        viewModel.onShowPost(blogId = readerPost.blogId, postId = readerPost.postId)

        assertThat(observers.uiStates.last()).isEqualTo(ErrorUiState(UiStringRes(R.string.reader_err_get_post_generic)))
    }

    @Test
    fun `given request already running, when post is fetched, then no error is shown`() =
            testWithoutLocalPost {
                val observers = init()
                whenever(readerFetchPostUseCase.fetchPost(anyLong(), anyLong(), anyBoolean()))
                        .thenReturn(AlreadyRunning)

                viewModel.onShowPost(blogId = readerPost.blogId, postId = readerPost.postId)

                assertThat(observers.uiStates.filterIsInstance<ErrorUiState>().last().message).isNull()
            }

    @Test
    fun `given post not found, when post is fetched, then post not found message is shown`() = testWithoutLocalPost {
        val observers = init()
        whenever(readerFetchPostUseCase.fetchPost(anyLong(), anyLong(), anyBoolean()))
                .thenReturn(Failed.PostNotFound)

        viewModel.onShowPost(blogId = readerPost.blogId, postId = readerPost.postId)

        assertThat(observers.uiStates.last())
                .isEqualTo(ErrorUiState(UiStringRes(R.string.reader_err_get_post_not_found)))
    }

    @Test
    fun `given unauthorised, when post is fetched, then error ui is shown`() = testWithoutLocalPost {
        val observers = init()
        whenever(readerFetchPostUseCase.fetchPost(anyLong(), anyLong(), anyBoolean()))
                .thenReturn(Failed.PostNotFound)

        viewModel.onShowPost(blogId = readerPost.blogId, postId = readerPost.postId)

        assertThat(observers.uiStates.last()).isInstanceOf(ErrorUiState::class.java)
    }

    @Test
    fun `given unauthorised with signin offer, when error ui shown, then sign in button is visible`() =
            testWithoutLocalPost {
                val observers = init(offerSignIn = true)
                whenever(readerFetchPostUseCase.fetchPost(anyLong(), anyLong(), anyBoolean()))
                        .thenReturn(Failed.NotAuthorised)

                viewModel.onShowPost(blogId = readerPost.blogId, postId = readerPost.postId)

                assertThat((observers.uiStates.last() as ErrorUiState).signInButtonVisibility).isEqualTo(true)
            }

    @Test
    fun `given unauthorised without signin offer, when error ui shown, then sign in button is not visible`() =
            testWithoutLocalPost {
                val observers = init(offerSignIn = false)
                whenever(readerFetchPostUseCase.fetchPost(anyLong(), anyLong(), anyBoolean()))
                        .thenReturn(Failed.NotAuthorised)

                viewModel.onShowPost(blogId = readerPost.blogId, postId = readerPost.postId)

                assertThat((observers.uiStates.last() as ErrorUiState).signInButtonVisibility).isEqualTo(false)
            }

    @Test
    fun `given unauthorised with no signin offer and no intercept uri, when error ui shown, then correct msg exists`() =
            testWithoutLocalPost {
                val observers = init(offerSignIn = false, interceptedUrPresent = false)
                whenever(readerFetchPostUseCase.fetchPost(anyLong(), anyLong(), anyBoolean()))
                        .thenReturn(Failed.NotAuthorised)

                viewModel.onShowPost(blogId = readerPost.blogId, postId = readerPost.postId)

                assertThat((observers.uiStates.last() as ErrorUiState).message)
                        .isEqualTo(UiStringRes(R.string.reader_err_get_post_not_authorized))
            }

    @Test
    fun `given unauthorised with no signin offer and intercept uri, when error ui shown, then correct msg exists`() =
            testWithoutLocalPost {
                val observers = init(offerSignIn = false, interceptedUrPresent = true)
                whenever(readerFetchPostUseCase.fetchPost(anyLong(), anyLong(), anyBoolean()))
                        .thenReturn(Failed.NotAuthorised)

                viewModel.onShowPost(blogId = readerPost.blogId, postId = readerPost.postId)

                assertThat((observers.uiStates.last() as ErrorUiState).message)
                        .isEqualTo(UiStringRes(R.string.reader_err_get_post_not_authorized_fallback))
            }

    @Test
    fun `given unauthorised with signin offer and no intercept uri, when error ui shown, then correct msg exists`() =
            testWithoutLocalPost {
                val observers = init(offerSignIn = true, interceptedUrPresent = false)
                whenever(readerFetchPostUseCase.fetchPost(anyLong(), anyLong(), anyBoolean()))
                        .thenReturn(Failed.NotAuthorised)

                viewModel.onShowPost(blogId = readerPost.blogId, postId = readerPost.postId)

                assertThat((observers.uiStates.last() as ErrorUiState).message)
                        .isEqualTo(UiStringRes(R.string.reader_err_get_post_not_authorized_signin))
            }

    @Test
    fun `given unauthorised with signin offer and intercept uri, when error ui shown, then correct msg exists`() =
            testWithoutLocalPost {
                val observers = init(offerSignIn = true, interceptedUrPresent = true)
                whenever(readerFetchPostUseCase.fetchPost(anyLong(), anyLong(), anyBoolean()))
                        .thenReturn(Failed.NotAuthorised)

                viewModel.onShowPost(blogId = readerPost.blogId, postId = readerPost.postId)

                assertThat((observers.uiStates.last() as ErrorUiState).message)
                        .isEqualTo(UiStringRes(R.string.reader_err_get_post_not_authorized_signin_fallback))
            }

    /* UPDATE POST */
    @Test
    fun `when post is updated, then ui is updated`() = test {
        val uiStates = init().uiStates

        viewModel.onUpdatePost(readerPost)

        assertThat(uiStates.filterIsInstance<ReaderPostDetailsUiState>().size).isEqualTo(2)
    }

    /* READER POST FEATURED IMAGE */
    @Test
    fun `when featured image is clicked, then media preview is shown`() = test {
        val observers = init()

        viewModel.onFeaturedImageClicked(blogId = readerPost.blogId, featuredImageUrl = readerPost.featuredImage)

        assertThat(observers.navigation.last().peekContent()).isInstanceOf(ShowMediaPreview::class.java)
    }

    @Test
    fun `when media preview is requested, then correct media from selected post's site is previewed`() = test {
        val observers = init()

        viewModel.onFeaturedImageClicked(blogId = readerPost.blogId, featuredImageUrl = readerPost.featuredImage)

        assertThat(observers.navigation.last().peekContent() as ShowMediaPreview).isEqualTo(
                ShowMediaPreview(site = site, featuredImage = readerPost.featuredImage)
        )
    }

    /* MORE MENU */
    @Test
    fun `when more button is clicked, then more menu is shown`() = test {
        val uiState = (init().uiStates.last() as ReaderPostDetailsUiState)

        viewModel.onMoreButtonClicked()

        assertThat(uiState.moreMenuItems).isNotNull
    }

    @Test
    fun `when user dismisses the menu, then more menu is not shown`() = test {
        val uiStates = init().uiStates

        viewModel.onMoreMenuDismissed()

        assertThat((uiStates.last() as ReaderPostDetailsUiState).moreMenuItems).isNull()
    }

    @Test
    fun `when more menu list item is clicked, then corresponding action is invoked`() = test {
        init().uiStates

        viewModel.onMoreMenuItemClicked(FOLLOW)

        verify(readerPostCardActionsHandler).onAction(
                eq(readerPost),
                eq(FOLLOW),
                eq(false),
                anyString()
        )
    }

    /* HEADER */
    @Test
    fun `when tag is clicked, then posts for tag are shown`() = test {
        val observers = init()
        val uiState = (observers.uiStates.last() as ReaderPostDetailsUiState)

        uiState.headerUiState.tagItems[0].onClick!!.invoke("t")

        assertThat(observers.navigation.last().peekContent()).isInstanceOf(ShowPostsByTag::class.java)
    }

    @Test
    fun `when header blog section is clicked, then selected blog's header click action is invoked`() = test {
        val uiState = (init().uiStates.last() as ReaderPostDetailsUiState)

        uiState.headerUiState.blogSectionUiState.blogSectionClickData!!.onBlogSectionClicked!!
                .invoke(readerPost.postId, readerPost.blogId)

        verify(readerPostCardActionsHandler).handleHeaderClicked(
                eq(readerPost.blogId),
                eq(readerPost.feedId),
                eq(readerPost.isFollowedByCurrentUser)
        )
    }

    @Test
    fun `when header follow button is clicked, then follow action is invoked`() = test {
        val uiState = (init().uiStates.last() as ReaderPostDetailsUiState)

        uiState.headerUiState.followButtonUiState.onFollowButtonClicked!!.invoke()

        verify(readerPostCardActionsHandler).onAction(
                eq(readerPost),
                eq(FOLLOW),
                eq(false),
                anyString()
        )
    }

    /* EXCERPT FOOTER */
    @Test
    fun `when visit excerpt link is clicked, then post blog url is opened`() = test {
        val observers = init()

        viewModel.onVisitPostExcerptFooterClicked(postLink = readerPost.url)

        assertThat(observers.navigation.last().peekContent()).isEqualTo(OpenUrl(url = readerPost.url))
    }

    /* RELATED POSTS */
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

                val uiState = (uiStates.last() as ReaderPostDetailsUiState)
                assertThat(uiState.localRelatedPosts).isEqualTo(localRelatedPostsUiState)
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

                val uiState = (uiStates.last() as ReaderPostDetailsUiState)
                assertThat(uiState.globalRelatedPosts).isEqualTo(globalRelatedPostsUiState)
            }

    @Test
    fun `given related posts fetch fails, when related posts are requested, then related posts are not shown`() =
            test {
                whenever(readerFetchRelatedPostsUseCase.fetchRelatedPosts(readerPost))
                        .thenReturn(FetchRelatedPostsState.Failed.RequestFailed)
                val uiStates = init().uiStates

                viewModel.onRelatedPostsRequested(readerPost)

                val uiState = (uiStates.last() as ReaderPostDetailsUiState)
                with(uiState) {
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

                val uiState = (uiStates.last() as ReaderPostDetailsUiState)
                with(uiState) {
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

                val uiState = (uiStates.last() as ReaderPostDetailsUiState)
                with(uiState) {
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
                val uiState = (observers.uiStates.last() as ReaderPostDetailsUiState)
                val relatedPost = uiState.globalRelatedPosts?.cards?.first()
                relatedPost?.onItemClicked?.invoke(relatedPost.postId, relatedPost.blogId, relatedPost.isGlobal)

                assertThat(observers.navigation.last().peekContent()).isInstanceOf(ShowRelatedPostDetails::class.java)
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
                val uiState = (observers.uiStates.last() as ReaderPostDetailsUiState)
                val relatedPost = uiState.globalRelatedPosts?.cards?.first()
                relatedPost?.onItemClicked?.invoke(relatedPost.postId, relatedPost.blogId, relatedPost.isGlobal)

                assertThat(observers.navigation.last().peekContent())
                        .isInstanceOf(ReplaceRelatedPostDetailsWithHistory::class.java)
            }

    /* FOOTER */
    @Test
    fun `when like button is clicked, then like action is invoked`() = test {
        val uiState = (init().uiStates.last() as ReaderPostDetailsUiState)

        uiState.actions.likeAction.onClicked!!.invoke(readerPost.postId, 200, LIKE)

        verify(readerPostCardActionsHandler).onAction(
                eq(readerPost),
                eq(LIKE),
                eq(false),
                anyString()
        )
    }

    @Test
    fun `when comments button is clicked, then comments action is invoked`() = test {
        val uiState = (init().uiStates.last() as ReaderPostDetailsUiState)

        uiState.actions.commentsAction.onClicked!!.invoke(readerPost.postId, 200, COMMENTS)

        verify(readerPostCardActionsHandler).onAction(
                eq(readerPost),
                eq(COMMENTS),
                eq(false),
                anyString()
        )
    }

    @Test
    fun `when reblog button is clicked, then reblog action is invoked`() = test {
        val uiState = (init().uiStates.last() as ReaderPostDetailsUiState)

        uiState.actions.commentsAction.onClicked!!.invoke(readerPost.postId, 200, REBLOG)

        verify(readerPostCardActionsHandler).onAction(
                eq(readerPost),
                eq(REBLOG),
                eq(false),
                anyString()
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
        val uiState = (init().uiStates.last() as ReaderPostDetailsUiState)

        uiState.actions.commentsAction.onClicked!!.invoke(readerPost.postId, 200, BOOKMARK)

        verify(readerPostCardActionsHandler).onAction(
                eq(readerPost),
                eq(BOOKMARK),
                eq(false),
                anyString()
        )
    }

    @Test
    fun `likes for post are refreshed on request`() = test {
        val likesState = getGetLikesState(TEST_CONFIG_1) as LikesData
        whenever(accountStore.account).thenReturn(AccountModel().apply { userId = -1 })

        getLikesState.value = likesState
        init()

        viewModel.onRefreshLikersData(viewModel.post!!)
        verify(
                getLikesHandler,
                times(1)
        ).handleGetLikesForPost(anyOrNull(), anyBoolean(), anyInt())
    }

    @Test
    fun `user avatar is added as last on like action`() = test {
        val likesState = (getGetLikesState(TEST_CONFIG_1) as LikesData)
        whenever(accountStore.account).thenReturn(AccountModel().apply {
            userId = -1
            avatarUrl = "https://avatar.url.example/image.jpg"
        })
        whenever(engagementUtils.likesToTrainOfFaces(likesCaptor.capture())).thenReturn(listOf())

        getLikesState.value = likesState
        val post = ReaderPost().apply {
            isExternal = false
            isLikedByCurrentUser = true
        }
        viewModel.post = post
        init()

        viewModel.onRefreshLikersData(post, true)

        verify(
                getLikesHandler,
                times(0)
        ).handleGetLikesForPost(anyOrNull(), anyBoolean(), anyInt())

        val listOfFaces = likesCaptor.lastValue
        assertThat(listOfFaces.size).isEqualTo(ReaderPostDetailViewModel.MAX_NUM_LIKES_FACES_WITH_SELF)
        assertThat(listOfFaces.last().likerAvatarUrl).isEqualTo(accountStore.account.avatarUrl)
    }

    @Test
    fun `ui state show likers faces when data available`() {
        val likesState = getGetLikesState(TEST_CONFIG_1) as LikesData
        val likers = MutableList(5) { mock<AvatarItem>() }
        val testTextString = "10 bloggers like this."

        getLikesState.value = likesState
        whenever(accountStore.account).thenReturn(AccountModel().apply { userId = -1 })
        whenever(engagementUtils.likesToTrainOfFaces(anyList())).thenReturn(likers)
        whenever(htmlMessageUtils.getHtmlMessageFromStringFormatResId(anyInt(), anyInt())).thenReturn(testTextString)
        val post = mock<ReaderPost>()
        whenever(post.isWP).thenReturn(true)
        viewModel.post = post
        val likeObserver = init().likesUiState

        getLikesState.value = likesState

        assertThat(likeObserver).isNotEmpty
        with(likeObserver.first()) {
            assertThat(showLoading).isFalse
            assertThat(engageItemsList).isEqualTo(
                    likers + TrailingLabelTextItem(
                            UiStringText(
                                    testTextString
                            ),
                            R.attr.wpColorOnSurfaceMedium
                    )
            )
            assertThat(showEmptyState).isFalse
            assertThat(emptyStateTitle).isNull()
        }
    }

    @Test
    fun `ui state shows empty state on failure and no cached data`() {
        val likesState = getGetLikesState(TEST_CONFIG_5) as Failure
        val likers = listOf<Liker>()

        getLikesState.value = likesState
        val post = mock<ReaderPost>()
        whenever(post.isWP).thenReturn(true)
        viewModel.post = post
        val likeObserver = init().likesUiState

        getLikesState.value = likesState

        assertThat(likeObserver).isNotEmpty
        with(likeObserver.first()) {
            assertThat(showLoading).isFalse
            assertThat(engageItemsList).isEqualTo(likers)
            assertThat(showEmptyState).isTrue
            assertThat(emptyStateTitle is UiStringRes).isTrue
        }
    }

    @Test
    fun `likers list is shown when like faces are clicked`() {
        val post = mock<ReaderPost>()
        viewModel.post = post

        val navigation = init().navigation

        viewModel.onLikeFacesClicked()

        assertThat(navigation.last().peekContent()).isInstanceOf(ShowEngagedPeopleList::class.java)
    }

    @Test
    fun `navigating back from comments updates data in snippet and bottom bar`() {
        val commentSnippetUiStates = init().commentSnippetUiState

        val modifiedPost = createDummyReaderPost(readerPost.postId)
        modifiedPost.numReplies = 10
        whenever(
                readerPostTableWrapper.getBlogPost(
                        anyOrNull(),
                        anyOrNull(),
                        anyOrNull()
                )
        ).thenReturn(modifiedPost)

        whenever(
                postDetailsUiStateBuilder.buildCommentSnippetUiState(
                        anyOrNull(),
                        anyOrNull(),
                        anyOrNull()
                )
        ).thenAnswer {
            createDummyCommentSnippetUiState(10)
        }

        viewModel.onUserNavigateFromComments()

        assertThat(viewModel.post?.numReplies).isEqualTo(10)

        assertThat(commentSnippetUiStates).isNotEmpty
        with(commentSnippetUiStates.last()) {
            assertThat(commentsNumber).isEqualTo(10)
        }
    }

    @Test
    fun `onRefreshCommentsData does not start comment snippet service for external posts`() {
        val externalPost = createDummyReaderPost(1, isWpComPost = false)
        whenever(
                readerPostTableWrapper.getBlogPost(
                        anyOrNull(),
                        anyOrNull(),
                        anyOrNull()
                )
        ).thenReturn(externalPost)

        viewModel.onRefreshCommentsData(1, 1)

        verify(readerCommentServiceStarterWrapper, never()).startServiceForCommentSnippet(
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
        )
    }

    private fun <T> testWithoutLocalPost(block: suspend CoroutineScope.() -> T) {
        test {
            whenever(readerGetPostUseCase.get(any(), any(), any())).thenReturn(Pair(null, false))
            block()
        }
    }

    private fun createDummyReaderPost(id: Long, isWpComPost: Boolean = true): ReaderPost =
            ReaderPost().apply {
                this.postId = id
                this.blogId = id * 100
                this.feedId = id * 1000
                this.title = "DummyPost"
                this.featuredVideo = id.toString()
                this.featuredImage = "/featured_image/$id/url"
                this.isExternal = !isWpComPost
                this.numReplies = 1
            }

    private fun createDummyReaderPostCommentSnippetList(): ReaderCommentList =
            ReaderCommentList().apply {
                val comment = ReaderComment()
                comment.commentId = 3

                add(comment)
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
                featuredImageUiState = mock(),
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
                excerptFooterUiState = mock(),
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
                        featuredImageCornerRadius = UIDimenRes(R.dimen.reader_featured_image_corner_radius),
                        onItemClicked = onRelatedPostItemClicked ?: mock()
                )
            },
            isGlobal = isGlobal,
            headerLabel = UiStringText(""),
            railcarJsonStrings = emptyList()
    )

    private fun createDummyCommentSnippetUiState(numberOfComments: Int = 1) = CommentSnippetUiState(
            commentsNumber = numberOfComments,
            showFollowConversation = true,
            emptyList()
    )

    private fun init(
        showPost: Boolean = true,
        isRelatedPost: Boolean = false,
        isFeed: Boolean = false,
        offerSignIn: Boolean = false,
        interceptedUrPresent: Boolean = false
    ): Observers {
        val uiStates = mutableListOf<UiState>()
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
        val likesUiStates = mutableListOf<TrainOfFacesUiState>()
        viewModel.likesUiState.observeForever {
            likesUiStates.add(it)
        }

        val commentSnippetUiStates = mutableListOf<CommentSnippetUiState>()
        viewModel.commentSnippetState.observeForever {
            commentSnippetUiStates.add(it)
        }

        val interceptedUri = INTERCEPTED_URI.takeIf { interceptedUrPresent }

        if (offerSignIn) {
            whenever(wpUrlUtilsWrapper.isWordPressCom(interceptedUri)).thenReturn(true)
            whenever(accountStore.hasAccessToken()).thenReturn(false)
        } else {
            whenever(wpUrlUtilsWrapper.isWordPressCom(interceptedUri)).thenReturn(false)
        }

        viewModel.start(isRelatedPost = isRelatedPost, isFeed = isFeed, interceptedUri = interceptedUri)

        if (showPost) {
            viewModel.onShowPost(blogId = readerPost.blogId, postId = readerPost.postId)
        }

        return Observers(
                uiStates,
                navigation,
                msgs,
                likesUiStates,
                commentSnippetUiStates
        )
    }

    private data class Observers(
        val uiStates: List<UiState>,
        val navigation: List<Event<ReaderNavigationEvents>>,
        val snackbarMsgs: List<Event<SnackbarMessageHolder>>,
        val likesUiState: List<TrainOfFacesUiState>,
        val commentSnippetUiState: List<CommentSnippetUiState>
    )
}
