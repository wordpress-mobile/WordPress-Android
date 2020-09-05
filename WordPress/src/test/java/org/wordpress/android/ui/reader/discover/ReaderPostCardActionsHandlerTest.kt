package org.wordpress.android.ui.reader.discover

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.TEST_SCOPE
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.test
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBookmarkedSavedOnlyLocallyDialog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBookmarkedTab
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BOOKMARK
import org.wordpress.android.ui.reader.reblog.ReblogUseCase
import org.wordpress.android.ui.reader.repository.usecases.BlockBlogUseCase
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase
import org.wordpress.android.ui.reader.repository.usecases.UndoBlockBlogUseCase
import org.wordpress.android.ui.reader.usecases.BookmarkPostState.Success
import org.wordpress.android.ui.reader.usecases.ReaderPostBookmarkUseCase
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase
import org.wordpress.android.ui.reader.usecases.ReaderSiteNotificationsUseCase
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderPostCardActionsHandlerTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var actionHandler: ReaderPostCardActionsHandler
    @Mock private lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock private lateinit var reblogUseCase: ReblogUseCase
    @Mock private lateinit var bookmarkUseCase: ReaderPostBookmarkUseCase
    @Mock private lateinit var followUseCase: ReaderSiteFollowUseCase
    @Mock private lateinit var blockBlogUseCase: BlockBlogUseCase
    @Mock private lateinit var likeUseCase: PostLikeUseCase
    @Mock private lateinit var siteNotificationsUseCase: ReaderSiteNotificationsUseCase
    @Mock private lateinit var undoBlockBlogUseCase: UndoBlockBlogUseCase
    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var resourceProvider: ResourceProvider
    @Mock private lateinit var htmlMessageUtils: HtmlMessageUtils

    @Before
    fun setUp() {
        actionHandler = ReaderPostCardActionsHandler(
                analyticsTrackerWrapper,
                reblogUseCase,
                bookmarkUseCase,
                followUseCase,
                blockBlogUseCase,
                likeUseCase,
                siteNotificationsUseCase,
                undoBlockBlogUseCase,
                appPrefsWrapper,
                dispatcher,
                resourceProvider,
                htmlMessageUtils,
                TEST_DISPATCHER,
                TEST_SCOPE,
                TEST_SCOPE
        )
        whenever(appPrefsWrapper.shouldShowBookmarksSavedLocallyDialog()).thenReturn(false)
    }

    /** BOOKMARK ACTION begin **/
    @Test
    fun `shows dialog when bookmark action is successful and shouldShowDialog returns true`() = test {
        // Arrange
        whenever(bookmarkUseCase.toggleBookmark(anyLong(), anyLong(), anyBoolean())).thenReturn(flowOf(Success(true)))
        whenever(appPrefsWrapper.shouldShowBookmarksSavedLocallyDialog()).thenReturn(true)

        var observedValue: Event<ReaderNavigationEvents>? = null
        actionHandler.navigationEvents.observeForever {
            observedValue = it
        }
        // Act
        actionHandler.onAction(dummyReaderPostModel(), BOOKMARK, false)

        // Assert
        assertThat(observedValue!!.peekContent()).isInstanceOf(ShowBookmarkedSavedOnlyLocallyDialog::class.java)
    }

    @Test
    fun `doesn't shows when dialog bookmark action is successful and shouldShowDialog returns false`() = test {
        // Arrange
        whenever(bookmarkUseCase.toggleBookmark(anyLong(), anyLong(), anyBoolean())).thenReturn(flowOf(Success(true)))
        whenever(appPrefsWrapper.shouldShowBookmarksSavedLocallyDialog()).thenReturn(false)

        var observedValue: Event<ReaderNavigationEvents>? = null
        actionHandler.navigationEvents.observeForever {
            observedValue = it
        }
        // Act
        actionHandler.onAction(dummyReaderPostModel(), BOOKMARK, false)

        // Assert
        assertThat(observedValue).isNull()
    }

    @Test
    fun `shows snackbar on successful bookmark action`() = test {
        // Arrange
        whenever(bookmarkUseCase.toggleBookmark(anyLong(), anyLong(), anyBoolean())).thenReturn(flowOf(Success(true)))

        var observedValue: Event<SnackbarMessageHolder>? = null
        actionHandler.snackbarEvents.observeForever {
            observedValue = it
        }
        // Act
        actionHandler.onAction(dummyReaderPostModel(), BOOKMARK, false)

        // Assert
        assertThat(observedValue!!.peekContent()).isNotNull
    }

    @Test
    fun `Doesn't show snackbar on successful bookmark action when on bookmark(saved) tab`() = test {
        // Arrange
        whenever(bookmarkUseCase.toggleBookmark(anyLong(), anyLong(), anyBoolean())).thenReturn(flowOf(Success(true)))

        var observedValue: Event<SnackbarMessageHolder>? = null
        actionHandler.snackbarEvents.observeForever {
            observedValue = it
        }
        val isBookmarkList = true
        // Act
        actionHandler.onAction(dummyReaderPostModel(), BOOKMARK, isBookmarkList)

        // Assert
        assertThat(observedValue).isNull()
    }

    @Test
    fun `Doesn't show snackbar on successful UNbookmark action`() = test {
        // Arrange
        whenever(bookmarkUseCase.toggleBookmark(anyLong(), anyLong(), anyBoolean())).thenReturn(flowOf(Success(false)))

        var observedValue: Event<SnackbarMessageHolder>? = null
        actionHandler.snackbarEvents.observeForever {
            observedValue = it
        }
        val isBookmarkList = true
        // Act
        actionHandler.onAction(dummyReaderPostModel(), BOOKMARK, isBookmarkList)

        // Assert
        assertThat(observedValue).isNull()
    }

    @Test
    fun `navigates to bookmark tab on bookmark snackbar action clicked`() = test {
        // Arrange
        whenever(bookmarkUseCase.toggleBookmark(anyLong(), anyLong(), anyBoolean())).thenReturn(flowOf(Success(true)))

        var snackBarObservedValue: Event<SnackbarMessageHolder>? = null
        actionHandler.snackbarEvents.observeForever {
            snackBarObservedValue = it
        }

        var navigationObservedValue: Event<ReaderNavigationEvents>? = null
        actionHandler.navigationEvents.observeForever {
            navigationObservedValue = it
        }

        // Act
        actionHandler.onAction(dummyReaderPostModel(), BOOKMARK, false)
        snackBarObservedValue!!.peekContent().buttonAction.invoke()

        // Assert
        assertThat(navigationObservedValue!!.peekContent()).isEqualTo(ShowBookmarkedTab)
    }
    /** BOOKMARK ACTION end **/

    private fun dummyReaderPostModel(): ReaderPost {
        return ReaderPost().apply {
            postId = 1
            blogId = 1
        }
    }
}
