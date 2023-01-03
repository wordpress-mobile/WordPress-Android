package org.wordpress.android.ui.reader.discover

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload.SubscriptionAction
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.OpenEditorForReblog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.OpenPost
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.SharePost
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBlogPreview
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBookmarkedSavedOnlyLocallyDialog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBookmarkedTab
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowNoSitesToReblog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowPostDetail
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowReaderComments
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowReportPost
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowReportUser
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowSitePickerForResult
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowVideoViewer
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BLOCK_SITE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BLOCK_USER
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BOOKMARK
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.COMMENTS
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.FOLLOW
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.LIKE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.REBLOG
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.SHARE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.SITE_NOTIFICATIONS
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.VISIT_SITE
import org.wordpress.android.ui.reader.reblog.ReblogState.MultipleSites
import org.wordpress.android.ui.reader.reblog.ReblogState.NoSite
import org.wordpress.android.ui.reader.reblog.ReblogState.SingleSite
import org.wordpress.android.ui.reader.reblog.ReblogState.Unknown
import org.wordpress.android.ui.reader.reblog.ReblogUseCase
import org.wordpress.android.ui.reader.repository.usecases.BlockBlogUseCase
import org.wordpress.android.ui.reader.repository.usecases.BlockSiteState.Failed
import org.wordpress.android.ui.reader.repository.usecases.BlockSiteState.SiteBlockedInLocalDb
import org.wordpress.android.ui.reader.repository.usecases.BlockUserState.UserBlockedInLocalDb
import org.wordpress.android.ui.reader.repository.usecases.BlockUserUseCase
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase.PostLikeState
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase.PostLikeState.PostLikedInLocalDb
import org.wordpress.android.ui.reader.repository.usecases.UndoBlockBlogUseCase
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.usecases.BookmarkPostState.PreLoadPostContent
import org.wordpress.android.ui.reader.usecases.BookmarkPostState.Success
import org.wordpress.android.ui.reader.usecases.ReaderFetchSiteUseCase
import org.wordpress.android.ui.reader.usecases.ReaderFetchSiteUseCase.FetchSiteState
import org.wordpress.android.ui.reader.usecases.ReaderPostBookmarkUseCase
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.Failed.NoNetwork
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.Failed.RequestFailed
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.FollowStatusChanged
import org.wordpress.android.ui.reader.usecases.ReaderSiteNotificationsUseCase
import org.wordpress.android.ui.reader.usecases.ReaderSiteNotificationsUseCase.SiteNotificationState
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.viewmodel.ResourceProvider

private const val SOURCE = "source"

@Suppress("LargeClass")
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderPostCardActionsHandlerTest : BaseUnitTest() {
    private lateinit var actionHandler: ReaderPostCardActionsHandler

    @Mock
    private lateinit var readerTracker: ReaderTracker

    @Mock
    private lateinit var reblogUseCase: ReblogUseCase

    @Mock
    private lateinit var bookmarkUseCase: ReaderPostBookmarkUseCase

    @Mock
    private lateinit var followUseCase: ReaderSiteFollowUseCase

    @Mock
    private lateinit var blockBlogUseCase: BlockBlogUseCase

    @Mock
    private lateinit var blockUserUseCase: BlockUserUseCase

    @Mock
    private lateinit var likeUseCase: PostLikeUseCase

    @Mock
    private lateinit var siteNotificationsUseCase: ReaderSiteNotificationsUseCase

    @Mock
    private lateinit var seenStatusToggleUseCase: ReaderSeenStatusToggleUseCase

    @Mock
    private lateinit var undoBlockBlogUseCase: UndoBlockBlogUseCase

    @Mock
    private lateinit var fetchSiteUseCase: ReaderFetchSiteUseCase

    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    private lateinit var readerBlogTableWrapper: ReaderBlogTableWrapper

    @Mock
    private lateinit var dispatcher: Dispatcher

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var htmlMessageUtils: HtmlMessageUtils

    @Before
    fun setUp() = test {
        actionHandler = ReaderPostCardActionsHandler(
            readerTracker,
            reblogUseCase,
            bookmarkUseCase,
            followUseCase,
            blockBlogUseCase,
            blockUserUseCase,
            likeUseCase,
            siteNotificationsUseCase,
            undoBlockBlogUseCase,
            fetchSiteUseCase,
            appPrefsWrapper,
            dispatcher,
            resourceProvider,
            htmlMessageUtils,
            mock(),
            seenStatusToggleUseCase,
            readerBlogTableWrapper,
            testDispatcher()
        )
        actionHandler.initScope(testScope())
        whenever(appPrefsWrapper.shouldShowBookmarksSavedLocallyDialog()).thenReturn(false)
        whenever(htmlMessageUtils.getHtmlMessageFromStringFormatResId(anyInt(), anyOrNull())).thenReturn(mock())
        whenever(readerBlogTableWrapper.getReaderBlog(any(), any())).thenReturn(mock())
    }

    /** BOOKMARK ACTION begin **/
    @Test
    fun `shows dialog when bookmark action is successful and shouldShowDialog returns true`() = test {
        // Arrange
        whenever(bookmarkUseCase.toggleBookmark(any(), anyBoolean(), anyString()))
            .thenReturn(flowOf(Success(true)))
        whenever(appPrefsWrapper.shouldShowBookmarksSavedLocallyDialog())
            .thenReturn(true)

        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            dummyReaderPostModel(),
            BOOKMARK,
            false,
            SOURCE
        )

        // Assert
        assertThat(observedValues.navigation[0]).isInstanceOf(ShowBookmarkedSavedOnlyLocallyDialog::class.java)
    }

    @Test
    fun `doesn't shows when dialog bookmark action is successful and shouldShowDialog returns false`() = test {
        // Arrange
        whenever(bookmarkUseCase.toggleBookmark(any(), anyBoolean(), anyString()))
            .thenReturn(flowOf(Success(true)))
        whenever(appPrefsWrapper.shouldShowBookmarksSavedLocallyDialog())
            .thenReturn(false)

        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            dummyReaderPostModel(),
            BOOKMARK,
            false,
            SOURCE
        )

        // Assert
        assertThat(observedValues.navigation).isEmpty()
    }

    @Test
    fun `shows snackbar on successful bookmark action`() = test {
        // Arrange
        whenever(bookmarkUseCase.toggleBookmark(any(), anyBoolean(), anyString()))
            .thenReturn(flowOf(Success(true)))

        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            dummyReaderPostModel(),
            BOOKMARK,
            false,
            SOURCE
        )

        // Assert
        assertThat(observedValues.snackbarMsgs.size).isEqualTo(1)
    }

    @Test
    fun `Doesn't show snackbar on successful bookmark action when on bookmark(saved) tab`() = test {
        // Arrange
        whenever(bookmarkUseCase.toggleBookmark(any(), anyBoolean(), anyString()))
            .thenReturn(flowOf(Success(true)))

        val observedValues = startObserving()
        val isBookmarkList = true
        // Act
        actionHandler.onAction(
            dummyReaderPostModel(),
            BOOKMARK,
            isBookmarkList,
            SOURCE
        )

        // Assert
        assertThat(observedValues.snackbarMsgs).isEmpty()
    }

    @Test
    fun `Doesn't show snackbar on successful UNbookmark action`() = test {
        // Arrange
        whenever(bookmarkUseCase.toggleBookmark(any(), anyBoolean(), anyString()))
            .thenReturn(flowOf(Success(false)))

        val observedValues = startObserving()
        val isBookmarkList = true
        // Act
        actionHandler.onAction(
            dummyReaderPostModel(),
            BOOKMARK,
            isBookmarkList,
            SOURCE
        )

        // Assert
        assertThat(observedValues.snackbarMsgs).isEmpty()
    }

    @Test
    fun `navigates to bookmark tab on bookmark snackbar action clicked`() = test {
        // Arrange
        whenever(bookmarkUseCase.toggleBookmark(any(), anyBoolean(), anyString()))
            .thenReturn(flowOf(Success(true)))

        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            dummyReaderPostModel(),
            BOOKMARK,
            false,
            SOURCE
        )
        observedValues.snackbarMsgs[0].buttonAction.invoke()

        // Assert
        assertThat(observedValues.navigation[0]).isEqualTo(ShowBookmarkedTab)
    }

    /** BOOKMARK ACTION end **/
    /** FOLLOW ACTION begin **/
    @Test
    fun `Emit followStatusUpdated after follow status update`() = test {
        // Arrange
        whenever(followUseCase.toggleFollow(anyOrNull(), anyString()))
            .thenReturn(flowOf(mock<FollowStatusChanged>()))
        val observedValues = startObserving()

        // Act
        actionHandler.onAction(
            dummyReaderPostModel(),
            FOLLOW,
            false,
            SOURCE
        )

        // Assert
        assertThat(observedValues.followStatusUpdated.size).isEqualTo(1)
    }

    @Test
    fun `Fetch subscriptions after follow status update`() = test {
        // Arrange
        whenever(followUseCase.toggleFollow(anyOrNull(), anyString()))
            .thenReturn(flowOf(mock<FollowStatusChanged>()))

        // Act
        actionHandler.onAction(
            dummyReaderPostModel(),
            FOLLOW,
            false,
            SOURCE
        )

        // Assert
        verify(siteNotificationsUseCase).fetchSubscriptions()
    }

    @Test
    fun `Enable notifications snackbar shown when user follows a post`() = test {
        // Arrange
        whenever(followUseCase.toggleFollow(anyOrNull(), anyString()))
            .thenReturn(
                flowOf(FollowStatusChanged(-1, -1, following = true, showEnableNotification = true))
            )
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            dummyReaderPostModel(),
            FOLLOW,
            false,
            SOURCE
        )

        // Assert
        assertThat(observedValues.snackbarMsgs.size).isEqualTo(1)
    }

    @Test
    fun `Post notifications are disabled when user unfollows a post`() = test {
        // Arrange
        whenever(followUseCase.toggleFollow(anyOrNull(), anyString()))
            .thenReturn(
                flowOf(
                    FollowStatusChanged(
                        -1,
                        -1,
                        following = false,
                        deleteNotificationSubscription = true
                    )
                )
            )

        // Act
        actionHandler.onAction(
            dummyReaderPostModel(),
            FOLLOW,
            false,
            SOURCE
        )

        // Assert
        verify(siteNotificationsUseCase).updateSubscription(anyLong(), eq(SubscriptionAction.DELETE))
        verify(siteNotificationsUseCase).updateNotificationEnabledForBlogInDb(anyLong(), eq(false))
    }

    @Test
    fun `Post notifications are enabled when user clicks on enable notifications snackbar action`() = test {
        // Arrange
        whenever(followUseCase.toggleFollow(anyOrNull(), anyString()))
            .thenReturn(flowOf(FollowStatusChanged(-1, -1, following = true, showEnableNotification = true)))
        val observedValues = startObserving()

        actionHandler.onAction(
            dummyReaderPostModel(),
            FOLLOW,
            false,
            SOURCE
        )

        // Act
        observedValues.snackbarMsgs[0].buttonAction.invoke()

        // Assert
        verify(siteNotificationsUseCase).updateSubscription(anyLong(), eq(SubscriptionAction.NEW))
        verify(siteNotificationsUseCase).updateNotificationEnabledForBlogInDb(anyLong(), eq(true))
    }

    @Test
    fun `Error message is shown when follow action fails with NoNetwork error`() = test {
        // Arrange
        whenever(followUseCase.toggleFollow(anyOrNull(), anyString()))
            .thenReturn(flowOf(NoNetwork))
        val observedValues = startObserving()

        // Act
        actionHandler.onAction(
            dummyReaderPostModel(),
            FOLLOW,
            false,
            SOURCE
        )

        // Assert
        assertThat(observedValues.snackbarMsgs.size).isEqualTo(1)
    }

    @Test
    fun `Error message is shown when follow action fails with RequestFailed error`() = test {
        // Arrange
        whenever(followUseCase.toggleFollow(anyOrNull(), anyString()))
            .thenReturn(flowOf(RequestFailed))
        val observedValues = startObserving()

        // Act
        actionHandler.onAction(
            dummyReaderPostModel(),
            FOLLOW,
            false,
            SOURCE
        )

        // Assert
        assertThat(observedValues.snackbarMsgs.size).isEqualTo(1)
    }

    @Test
    fun `given site present in db, when follow action is requested, follow site is triggered`() = test {
        // Arrange
        whenever(followUseCase.toggleFollow(anyOrNull(), anyString()))
            .thenReturn(flowOf(FollowSiteState.Success))

        // Act
        actionHandler.onAction(
            dummyReaderPostModel(),
            FOLLOW,
            false,
            SOURCE
        )

        // Assert
        verify(followUseCase, times(1)).toggleFollow(any(), anyString())
    }

    @Test
    fun `given site not present in db, when follow action is requested, fetch site is triggered`() = test {
        // Arrange
        whenever(readerBlogTableWrapper.getReaderBlog(any(), any()))
            .thenReturn(null)
        whenever(fetchSiteUseCase.fetchSite(any(), any(), anyOrNull()))
            .thenReturn(FetchSiteState.Success)
        whenever(followUseCase.toggleFollow(anyOrNull(), anyString()))
            .thenReturn(flowOf(FollowSiteState.Success))

        // Act
        actionHandler.onAction(
            dummyReaderPostModel(),
            FOLLOW,
            false,
            SOURCE
        )

        // Assert
        verify(fetchSiteUseCase, times(1)).fetchSite(any(), any(), anyOrNull())
    }

    @Test
    fun `given fetch site request fails, when follow action is requested, error snackbar is shown`() = test {
        // Arrange
        whenever(readerBlogTableWrapper.getReaderBlog(any(), any()))
            .thenReturn(null)
        whenever(fetchSiteUseCase.fetchSite(any(), any(), anyOrNull()))
            .thenReturn(FetchSiteState.Failed.RequestFailed)
        val observedValues = startObserving()

        // Act
        actionHandler.onAction(
            dummyReaderPostModel(),
            FOLLOW,
            false,
            SOURCE
        )

        // Assert
        assertThat(observedValues.snackbarMsgs.size).isEqualTo(1)
    }

    @Test
    fun `given fetch site request succeeds, when follow action is requested, follow site is triggered`() = test {
        // Arrange
        whenever(readerBlogTableWrapper.getReaderBlog(any(), any()))
            .thenReturn(null)
        whenever(fetchSiteUseCase.fetchSite(any(), any(), anyOrNull()))
            .thenReturn(FetchSiteState.Success)
        whenever(followUseCase.toggleFollow(anyOrNull(), anyString()))
            .thenReturn(flowOf(FollowSiteState.Success))

        // Act
        actionHandler.onAction(
            dummyReaderPostModel(),
            FOLLOW,
            false,
            SOURCE
        )

        // Assert
        verify(followUseCase, times(1)).toggleFollow(any(), anyString())
    }
    /** FOLLOW ACTION end **/

    /** SITE NOTIFICATIONS ACTION Begin **/
    @Test
    fun `ToggleNotifications when user clicks on Notifcations button`() = test {
        // Arrange
        whenever(siteNotificationsUseCase.toggleNotification(anyLong(), anyLong()))
            .thenReturn(SiteNotificationState.Success)
        // Act
        actionHandler.onAction(
            mock(),
            SITE_NOTIFICATIONS,
            false,
            SOURCE
        )

        // Assert
        verify(siteNotificationsUseCase).toggleNotification(anyLong(), anyLong())
    }

    @Test
    fun `Show snackbar message when toggleNotification return network error`() = test {
        // Arrange
        whenever(siteNotificationsUseCase.toggleNotification(anyLong(), anyLong()))
            .thenReturn(SiteNotificationState.Failed.NoNetwork)
        val observedValues = startObserving()

        // Act
        actionHandler.onAction(
            mock(),
            SITE_NOTIFICATIONS,
            false,
            SOURCE
        )

        // Assert
        assertThat(observedValues.snackbarMsgs.size).isEqualTo(1)
    }

    @Test
    fun `Show snackbar message when toggleNotification returns request error`() = test {
        // Arrange
        whenever(siteNotificationsUseCase.toggleNotification(anyLong(), anyLong()))
            .thenReturn(SiteNotificationState.Failed.RequestFailed)
        val observedValues = startObserving()

        // Act
        actionHandler.onAction(
            mock(),
            SITE_NOTIFICATIONS,
            false,
            SOURCE
        )

        // Assert
        assertThat(observedValues.snackbarMsgs.size).isEqualTo(1)
    }

    @Test
    fun `Do not Show snackbar message when toggleNotification returns alreadyRunning error`() = test {
        // Arrange
        whenever(siteNotificationsUseCase.toggleNotification(anyLong(), anyLong()))
            .thenReturn(SiteNotificationState.Failed.AlreadyRunning)
        val observedValues = startObserving()

        // Act
        actionHandler.onAction(
            mock(),
            SITE_NOTIFICATIONS,
            false,
            SOURCE
        )

        // Assert
        assertThat(observedValues.snackbarMsgs).isEmpty()
    }

    @Test
    fun `given site present in db, when site notifications action is requested, toggle notifications is triggered`() =
        test {
            // Arrange
            whenever(siteNotificationsUseCase.toggleNotification(anyLong(), anyLong()))
                .thenReturn(SiteNotificationState.Success)

            // Act
            actionHandler.onAction(
                dummyReaderPostModel(),
                SITE_NOTIFICATIONS,
                false,
                SOURCE
            )

            // Assert
            verify(siteNotificationsUseCase, times(1)).toggleNotification(any(), any())
        }

    @Test
    fun `given site not present in db, when site notifications action is requested, fetch site is triggered`() = test {
        // Arrange
        whenever(readerBlogTableWrapper.getReaderBlog(any(), any()))
            .thenReturn(null)
        whenever(fetchSiteUseCase.fetchSite(any(), any(), anyOrNull()))
            .thenReturn(FetchSiteState.Success)
        whenever(siteNotificationsUseCase.toggleNotification(anyLong(), anyLong()))
            .thenReturn(SiteNotificationState.Success)

        // Act
        actionHandler.onAction(
            dummyReaderPostModel(),
            SITE_NOTIFICATIONS,
            false,
            SOURCE
        )

        // Assert
        verify(fetchSiteUseCase, times(1)).fetchSite(any(), any(), anyOrNull())
    }

    @Test
    fun `given fetch site request fails, when site notifications action is requested, error snackbar is shown`() =
        test {
            // Arrange
            whenever(readerBlogTableWrapper.getReaderBlog(any(), any()))
                .thenReturn(null)
            whenever(fetchSiteUseCase.fetchSite(any(), any(), anyOrNull()))
                .thenReturn(FetchSiteState.Failed.RequestFailed)
            val observedValues = startObserving()

            // Act
            actionHandler.onAction(
                dummyReaderPostModel(),
                SITE_NOTIFICATIONS,
                false,
                SOURCE
            )

            // Assert
            assertThat(observedValues.snackbarMsgs.size).isEqualTo(1)
        }

    @Test
    fun `given fetch site request succeeds, when site notifications is requested, toggle notifications is triggered`() =
        test {
            // Arrange
            whenever(readerBlogTableWrapper.getReaderBlog(any(), any()))
                .thenReturn(null)
            whenever(fetchSiteUseCase.fetchSite(any(), any(), anyOrNull()))
                .thenReturn(FetchSiteState.Success)
            whenever(siteNotificationsUseCase.toggleNotification(anyLong(), anyLong()))
                .thenReturn(SiteNotificationState.Success)

            // Act
            actionHandler.onAction(
                dummyReaderPostModel(),
                SITE_NOTIFICATIONS,
                false,
                SOURCE
            )

            // Assert
            verify(siteNotificationsUseCase, times(1)).toggleNotification(any(), any())
        }
    /** SITE NOTIFICATIONS ACTION end **/

    /** SHARE ACTION Begin **/
    @Test
    fun `Share button opens share dialog`() = test {
        // Arrange
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            mock(),
            SHARE,
            false,
            SOURCE
        )

        // Assert
        assertThat(observedValues.navigation[0]).isInstanceOf(SharePost::class.java)
    }
    /** SHARE ACTION end **/

    /** VISIT SITE ACTION end **/
    @Test
    fun `Visit Site button opens browser`() = test {
        // Arrange
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            mock(),
            VISIT_SITE,
            false,
            SOURCE
        )

        // Assert
        assertThat(observedValues.navigation[0]).isInstanceOf(OpenPost::class.java)
    }
    /** VISIT SITE ACTION end **/

    /** BLOCK SITE ACTION begin **/
    @Test
    fun `Posts are refreshed when site blocked in local db`() = test {
        // Arrange
        whenever(blockBlogUseCase.blockBlog(anyLong(), anyLong()))
            .thenReturn(flowOf(SiteBlockedInLocalDb(mock())))
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            mock(),
            BLOCK_SITE,
            false,
            SOURCE
        )

        // Assert
        assertThat(observedValues.refreshPosts.size).isEqualTo(1)
    }

    @Test
    fun `Snackbar shown when site blocked in local db`() = test {
        // Arrange
        whenever(blockBlogUseCase.blockBlog(anyLong(), anyLong()))
            .thenReturn(flowOf(SiteBlockedInLocalDb(mock())))
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            mock(),
            BLOCK_SITE,
            false,
            SOURCE
        )

        // Assert
        assertThat(observedValues.snackbarMsgs.size).isEqualTo(1)
    }

    @Test
    fun `Snackbar shown when request to block site failes with no network error`() = test {
        // Arrange
        whenever(blockBlogUseCase.blockBlog(anyLong(), anyLong()))
            .thenReturn(flowOf(Failed.NoNetwork))
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            mock(),
            BLOCK_SITE,
            false,
            SOURCE
        )

        // Assert
        assertThat(observedValues.snackbarMsgs.size).isEqualTo(1)
    }

    @Test
    fun `Posts are refreshed when request to block site failes with request failed error`() = test {
        // Arrange
        whenever(blockBlogUseCase.blockBlog(anyLong(), anyLong()))
            .thenReturn(flowOf(Failed.RequestFailed))
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            mock(),
            BLOCK_SITE,
            false,
            SOURCE
        )

        // Assert
        assertThat(observedValues.refreshPosts.size).isEqualTo(1)
    }

    @Test
    fun `Snackbar shown when request to block site failes with request failed error`() = test {
        // Arrange
        whenever(blockBlogUseCase.blockBlog(anyLong(), anyLong()))
            .thenReturn(flowOf(Failed.RequestFailed))
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            mock(),
            BLOCK_SITE,
            false,
            SOURCE
        )

        // Assert
        assertThat(observedValues.snackbarMsgs.size).isEqualTo(1)
    }

    @Test
    fun `Undo action is invoked when user clicks on undo action in snackbar`() = test {
        // Arrange
        whenever(blockBlogUseCase.blockBlog(anyLong(), anyLong()))
            .thenReturn(flowOf(SiteBlockedInLocalDb(mock())))
        val observedValues = startObserving()
        actionHandler.onAction(
            mock(),
            BLOCK_SITE,
            false,
            SOURCE
        )
        // Act
        observedValues.snackbarMsgs[0].buttonAction.invoke()
        // Assert
        verify(undoBlockBlogUseCase).undoBlockBlog(anyOrNull(), anyString())
    }

    @Test
    fun `Post refreshed when user clicks on undo action in snackbar`() = test {
        // Arrange
        whenever(blockBlogUseCase.blockBlog(anyLong(), anyLong()))
            .thenReturn(flowOf(SiteBlockedInLocalDb(mock())))
        val observedValues = startObserving()
        actionHandler.onAction(
            mock(),
            BLOCK_SITE,
            false,
            SOURCE
        )
        // Act
        observedValues.snackbarMsgs[0].buttonAction.invoke()
        // Assert
        assertThat(observedValues.refreshPosts.size).isEqualTo(2)
    }
    /** BLOCK SITE ACTION end **/

    /** BLOCK USER ACTION begin **/
    @Test
    fun `Posts are refreshed when user blocked in local db`() = test {
        // Arrange
        whenever(blockUserUseCase.blockUser(anyLong(), anyLong()))
            .thenReturn(flowOf(UserBlockedInLocalDb(mock())))
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            mock(),
            BLOCK_USER,
            false,
            SOURCE
        )

        // Assert
        assertThat(observedValues.refreshPosts.size).isEqualTo(1)
    }

    @Test
    fun `Snackbar shown when user blocked in local db`() = test {
        // Arrange
        whenever(blockUserUseCase.blockUser(anyLong(), anyLong()))
            .thenReturn(flowOf(UserBlockedInLocalDb(mock())))
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            mock(),
            BLOCK_USER,
            false,
            SOURCE
        )

        // Assert
        assertThat(observedValues.snackbarMsgs.size).isEqualTo(1)
    }

    @Test
    fun `Undo action is invoked when user clicks on undo block user action in snackbar`() = test {
        // Arrange
        whenever(blockUserUseCase.blockUser(anyLong(), anyLong()))
            .thenReturn(flowOf(UserBlockedInLocalDb(mock())))
        val observedValues = startObserving()
        actionHandler.onAction(
            mock(),
            BLOCK_USER,
            false,
            SOURCE
        )
        // Act
        observedValues.snackbarMsgs[0].buttonAction.invoke()
        // Assert
        verify(blockUserUseCase).undoBlockUser(anyOrNull())
    }

    @Test
    fun `Post refreshed when user clicks on undo block user action in snackbar`() = test {
        // Arrange
        whenever(blockUserUseCase.blockUser(anyLong(), anyLong()))
            .thenReturn(flowOf(UserBlockedInLocalDb(mock())))
        val observedValues = startObserving()
        actionHandler.onAction(
            mock(),
            BLOCK_USER,
            false,
            SOURCE
        )
        // Act
        observedValues.snackbarMsgs[0].buttonAction.invoke()
        // Assert
        assertThat(observedValues.refreshPosts.size).isEqualTo(2)
    }
    /** BLOCK USER ACTION end **/

    /** LIKE ACTION begin **/
    @Test
    fun `Like action is initiated when user clicks on like button`() = test {
        // Arrange
        whenever(likeUseCase.perform(anyOrNull(), anyBoolean(), anyString()))
            .thenReturn(flowOf())
        // Act
        actionHandler.onAction(
            mock(),
            LIKE,
            false,
            SOURCE
        )
        // Assert
        verify(likeUseCase).perform(anyOrNull(), anyBoolean(), anyString())
    }

    @Test
    fun `Like use cases is initiated with like action when the post is not liked by the current user`() = test {
        // Arrange
        whenever(likeUseCase.perform(anyOrNull(), anyBoolean(), anyString()))
            .thenReturn(flowOf())
        val isLiked = false
        val post = ReaderPost().apply { isLikedByCurrentUser = isLiked }
        // Act
        actionHandler.onAction(
            post,
            LIKE,
            false,
            SOURCE
        )
        // Assert
        verify(likeUseCase).perform(anyOrNull(), eq(!isLiked), anyString())
    }

    @Test
    fun `Like use cases is initiated with unlike action when the post is not liked by the current user`() = test {
        // Arrange
        whenever(likeUseCase.perform(anyOrNull(), anyBoolean(), anyString()))
            .thenReturn(flowOf())
        val isLiked = true
        val post = ReaderPost().apply { isLikedByCurrentUser = isLiked }
        // Act
        actionHandler.onAction(
            post,
            LIKE,
            false,
            SOURCE
        )
        // Assert
        verify(likeUseCase).perform(anyOrNull(), eq(!isLiked), anyString())
    }

    @Test
    fun `Posts are refreshed when user likes a post`() = test {
        // Arrange
        whenever(likeUseCase.perform(anyOrNull(), anyBoolean(), anyString()))
            .thenReturn(flowOf(PostLikedInLocalDb))
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            mock(),
            LIKE,
            false,
            SOURCE
        )
        // Assert
        assertThat(observedValues.refreshPosts.size).isEqualTo(1)
    }

    @Test
    fun `Posts are refreshed when like action fails with RequestFailed error`() = test {
        // Arrange
        whenever(likeUseCase.perform(anyOrNull(), anyBoolean(), anyString()))
            .thenReturn(flowOf(PostLikeState.Failed.RequestFailed))
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            mock(),
            LIKE,
            false,
            SOURCE
        )
        // Assert
        assertThat(observedValues.refreshPosts.size).isEqualTo(1)
    }

    @Test
    fun `Snackbar shown when like action fails with no network error`() = test {
        // Arrange
        whenever(likeUseCase.perform(anyOrNull(), anyBoolean(), anyString()))
            .thenReturn(flowOf(PostLikeState.Failed.NoNetwork))
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            mock(),
            LIKE,
            false,
            SOURCE
        )
        // Assert
        assertThat(observedValues.snackbarMsgs.size).isEqualTo(1)
    }

    @Test
    fun `Snackbar shown when like action fails with no RequestFailed error`() = test {
        // Arrange
        whenever(likeUseCase.perform(anyOrNull(), anyBoolean(), anyString()))
            .thenReturn(flowOf(PostLikeState.Failed.RequestFailed))
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            mock(),
            LIKE,
            false,
            SOURCE
        )
        // Assert
        assertThat(observedValues.snackbarMsgs.size).isEqualTo(1)
    }

    @Test
    fun `Nothing happens when like action succeeds`() = test {
        // Arrange
        whenever(likeUseCase.perform(anyOrNull(), anyBoolean(), anyString()))
            .thenReturn(flowOf(PostLikeState.Success))
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            mock(),
            LIKE,
            false,
            SOURCE
        )
        // Assert
        assertThat(observedValues.snackbarMsgs).isEmpty()
        assertThat(observedValues.navigation).isEmpty()
        assertThat(observedValues.refreshPosts).isEmpty()
        assertThat(observedValues.preloadPost).isEmpty()
        assertThat(observedValues.followStatusUpdated).isEmpty()
    }

    @Test
    fun `Nothing happens when like action results in Unchanged state`() = test {
        // Arrange
        whenever(likeUseCase.perform(anyOrNull(), anyBoolean(), anyString()))
            .thenReturn(flowOf(PostLikeState.Unchanged))
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            mock(),
            LIKE,
            false,
            SOURCE
        )
        // Assert
        assertThat(observedValues.snackbarMsgs).isEmpty()
        assertThat(observedValues.navigation).isEmpty()
        assertThat(observedValues.refreshPosts).isEmpty()
        assertThat(observedValues.preloadPost).isEmpty()
        assertThat(observedValues.followStatusUpdated).isEmpty()
    }

    @Test
    fun `Nothing happens when like action results in AlreadyRunning`() = test {
        // Arrange
        whenever(likeUseCase.perform(anyOrNull(), anyBoolean(), anyString()))
            .thenReturn(flowOf(PostLikeState.AlreadyRunning))
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            mock(),
            LIKE,
            false,
            SOURCE
        )
        // Assert
        assertThat(observedValues.snackbarMsgs).isEmpty()
        assertThat(observedValues.navigation).isEmpty()
        assertThat(observedValues.refreshPosts).isEmpty()
        assertThat(observedValues.preloadPost).isEmpty()
        assertThat(observedValues.followStatusUpdated).isEmpty()
    }

    /** LIKE ACTION end **/

    /** REVLOG ACTION begin **/
    @Test
    fun `Reblog action is initiated when user clicks on reblog button`() = test {
        // Arrange
        whenever(reblogUseCase.onReblogButtonClicked(anyOrNull()))
            .thenReturn(mock())
        whenever(reblogUseCase.convertReblogStateToNavigationEvent(anyOrNull()))
            .thenReturn(mock())
        // Act
        actionHandler.onAction(
            mock(),
            REBLOG,
            false,
            SOURCE
        )
        // Assert
        verify(reblogUseCase).onReblogButtonClicked(anyOrNull())
    }

    @Test
    fun `Show NoSitesToReblog screen when user does not have any sites attached`() = test {
        // Arrange
        whenever(reblogUseCase.onReblogButtonClicked(anyOrNull()))
            .thenReturn(NoSite)
        whenever(reblogUseCase.convertReblogStateToNavigationEvent(anyOrNull()))
            .thenCallRealMethod()
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            mock(),
            REBLOG,
            false,
            SOURCE
        )
        // Assert
        assertThat(observedValues.navigation[0]).isInstanceOf(ShowNoSitesToReblog::class.java)
    }

    @Test
    fun `Show SitePicker when user has multiple sites attached`() = test {
        // Arrange
        whenever(reblogUseCase.onReblogButtonClicked(anyOrNull()))
            .thenReturn(MultipleSites(mock(), mock()))
        whenever(reblogUseCase.convertReblogStateToNavigationEvent(anyOrNull()))
            .thenCallRealMethod()
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            mock(),
            REBLOG,
            false,
            SOURCE
        )
        // Assert
        assertThat(observedValues.navigation[0]).isInstanceOf(ShowSitePickerForResult::class.java)
    }

    @Test
    fun `Show Editor when user has a single site attached or they selected a site they want to reblog to`() = test {
        // Arrange
        whenever(reblogUseCase.onReblogButtonClicked(anyOrNull()))
            .thenReturn(SingleSite(mock(), mock()))
        whenever(reblogUseCase.convertReblogStateToNavigationEvent(anyOrNull()))
            .thenCallRealMethod()
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            mock(),
            REBLOG,
            false,
            SOURCE
        )
        // Assert
        assertThat(observedValues.navigation[0]).isInstanceOf(OpenEditorForReblog::class.java)
    }

    @Test
    fun `Show snackbar when an error occurs`() = test {
        // Arrange
        whenever(reblogUseCase.onReblogButtonClicked(anyOrNull()))
            .thenReturn(Unknown)
        whenever(reblogUseCase.convertReblogStateToNavigationEvent(anyOrNull()))
            .thenCallRealMethod()
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            mock(),
            REBLOG,
            false,
            SOURCE
        )
        // Assert
        assertThat(observedValues.snackbarMsgs.size).isEqualTo(1)
    }
    /** REBLOG ACTION end **/

    /** COMMENTS ACTION begin **/
    @Test
    fun `Comments screen shown when the user clicks on comments button`() = test {
        // Arrange
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(
            mock(),
            COMMENTS,
            false,
            SOURCE
        )
        // Assert
        assertThat(observedValues.navigation[0]).isInstanceOf(ShowReaderComments::class.java)
    }

    /** COMMENTS ACTION end **/

    @Test
    fun `Clicking on a post opens post detail`() = test {
        // Arrange
        val observedValues = startObserving()

        // Act
        actionHandler.handleOnItemClicked(mock(), anyString())

        // Assert
        assertThat(observedValues.navigation[0]).isInstanceOf(ShowPostDetail::class.java)
    }

    @Test
    fun `Clicking on a video overlay opens video viewer`() = test {
        // Arrange
        val observedValues = startObserving()

        // Act
        actionHandler.handleVideoOverlayClicked("mock")

        // Assert
        assertThat(observedValues.navigation[0]).isInstanceOf(ShowVideoViewer::class.java)
    }

    @Test
    fun `Clicking on a header opens blog preview`() = test {
        // Arrange
        val observedValues = startObserving()

        // Act
        actionHandler.handleHeaderClicked(0L, 0L, false)

        // Assert
        assertThat(observedValues.navigation[0]).isInstanceOf(ShowBlogPreview::class.java)
    }

    private fun startObserving(): Observers {
        val navigation = mutableListOf<ReaderNavigationEvents>()
        actionHandler.navigationEvents.observeForever {
            navigation.add(it.peekContent())
        }

        val snackbarMsgs = mutableListOf<SnackbarMessageHolder>()
        actionHandler.snackbarEvents.observeForever {
            snackbarMsgs.add(it.peekContent())
        }

        val preloadPost = mutableListOf<PreLoadPostContent>()
        actionHandler.preloadPostEvents.observeForever {
            preloadPost.add(it.peekContent())
        }

        val followStatusUpdated = mutableListOf<FollowStatusChanged>()
        actionHandler.followStatusUpdated.observeForever {
            followStatusUpdated.add(it)
        }

        val refreshPosts = mutableListOf<Unit>()
        actionHandler.refreshPosts.observeForever {
            refreshPosts.add(it.peekContent())
        }
        return Observers(navigation, snackbarMsgs, preloadPost, followStatusUpdated, refreshPosts)
    }

    /** REPORT ACTIONS start **/
    @Test
    fun `Clicking on a report this post opens webview`() = test {
        // Arrange
        val navigation = mutableListOf<ReaderNavigationEvents>()
        actionHandler.navigationEvents.observeForever {
            navigation.add(it.peekContent())
        }
        // Act
        actionHandler.handleReportPostClicked(dummyReaderPostModel())

        // Assert
        assertThat(navigation[0]).isInstanceOf(ShowReportPost::class.java)
    }

    @Test
    fun `Clicking on report user opens webview`() = test {
        // Arrange
        val navigation = mutableListOf<ReaderNavigationEvents>()
        actionHandler.navigationEvents.observeForever {
            navigation.add(it.peekContent())
        }
        // Act
        actionHandler.handleReportUserClicked(dummyReaderPostModel())

        // Assert
        assertThat(navigation[0]).isInstanceOf(ShowReportUser::class.java)
    }

    /** REPORT ACTIONS end **/

    private fun dummyReaderPostModel(): ReaderPost {
        return ReaderPost().apply {
            postId = 1
            blogId = 1
            blogName = "DummyName"
        }
    }

    private data class Observers(
        val navigation: List<ReaderNavigationEvents>,
        val snackbarMsgs: List<SnackbarMessageHolder>,
        val preloadPost: List<PreLoadPostContent>,
        val followStatusUpdated: List<FollowStatusChanged>,
        val refreshPosts: List<Unit>
    )
}
