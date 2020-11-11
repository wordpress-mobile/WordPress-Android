package org.wordpress.android.ui.stories

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
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.push.NotificationType
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.PostEditorAnalyticsSession
import org.wordpress.android.ui.posts.PostEditorAnalyticsSessionWrapper
import org.wordpress.android.ui.posts.SavePostToDbUseCase
import org.wordpress.android.ui.stories.usecase.SetUntitledStoryTitleIfTitleEmptyUseCase

class StoryComposerViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: StoryComposerViewModel
    private lateinit var editPostRepository: EditPostRepository
    @Mock lateinit var systemNotificationsTracker: SystemNotificationsTracker
    @Mock lateinit var saveInitialPostUseCase: SaveInitialPostUseCase
    @Mock lateinit var savePostToDbUseCase: SavePostToDbUseCase
    @Mock lateinit var setUntitledStoryTitleIfTitleEmptyUseCase: SetUntitledStoryTitleIfTitleEmptyUseCase
    @Mock lateinit var postEditorAnalyticsSessionWrapper: PostEditorAnalyticsSessionWrapper
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var postStore: PostStore
    @Mock lateinit var site: SiteModel

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        viewModel = StoryComposerViewModel(
                systemNotificationsTracker,
                saveInitialPostUseCase,
                savePostToDbUseCase,
                setUntitledStoryTitleIfTitleEmptyUseCase,
                postEditorAnalyticsSessionWrapper,
                dispatcher
        )
        editPostRepository = EditPostRepository(
                mock(),
                postStore,
                mock(),
                TEST_DISPATCHER,
                TEST_DISPATCHER
        )
    }

    @Test
    fun `if postId is 0 then create a new post with saveInitialPostUseCase`() {
        // arrange
        val expectedPostId = LocalId(0)

        // act
        viewModel.start(site, editPostRepository, expectedPostId, mock(), mock())

        verify(saveInitialPostUseCase, times(1)).saveInitialPost(eq(editPostRepository), eq(site))
    }

    @Test
    fun `if postId is 0 then trackEditorCreatedPost is not null`() {
        // arrange
        val expectedPostId = LocalId(0)

        // act
        viewModel.start(site, editPostRepository, expectedPostId, mock(), mock())

        assertThat(viewModel.trackEditorCreatedPost.value).isNotNull
    }

    @Test
    fun `if postId is not 0 then trackEditorCreatedPost is null`() {
        // arrange
        val expectedPostId = LocalId(2)

        // act
        viewModel.start(site, editPostRepository, expectedPostId, mock(), mock())

        assertThat(viewModel.trackEditorCreatedPost.value).isNull()
    }

    @Test
    fun `if postId is a value other than 0 then load the post using the EditPostRepository's PostStore`() {
        // arrange
        val expectedPostId = LocalId(2)

        // act
        viewModel.start(site, editPostRepository, expectedPostId, mock(), mock())

        verify(postStore, times(1)).getPostByLocalPostId(eq(expectedPostId.value))
    }

    @Test
    fun `if postEditorAnalyticsSession does not exist then create one with PostEditorAnalyticsSessionWrapper`() {
        // arrange
        val postEditorAnalyticsSession: PostEditorAnalyticsSession? = null
        whenever(
                postEditorAnalyticsSessionWrapper.getNewPostEditorAnalyticsSession(
                        any(),
                        anyOrNull(),
                        anyOrNull(),
                        any()
                )
        ).thenReturn(mock())

        // act
        viewModel.start(site, editPostRepository, LocalId(0), postEditorAnalyticsSession, mock())

        // assert
        verify(postEditorAnalyticsSessionWrapper, times(1)).getNewPostEditorAnalyticsSession(
                any(),
                anyOrNull(),
                anyOrNull(),
                any()
        )
    }

    @Test
    fun `if postEditorAnalyticsSession exists then don't create one with PostEditorAnalyticsSessionWrapper`() {
        // arrange
        val postEditorAnalyticsSession: PostEditorAnalyticsSession? = mock()

        // act
        viewModel.start(site, editPostRepository, LocalId(0), postEditorAnalyticsSession, mock())

        // assert
        verify(postEditorAnalyticsSessionWrapper, times(0)).getNewPostEditorAnalyticsSession(
                any(),
                anyOrNull(),
                anyOrNull(),
                any()
        )
    }

    @Test
    fun `if notificationType is not null then systemNotificationsTracker should track it`() {
        // arrange
        val notificationType: NotificationType? = mock()

        // act
        viewModel.start(site, editPostRepository, LocalId(0), mock(), notificationType)

        verify(systemNotificationsTracker, times(1)).trackTappedNotification(eq(notificationType!!))
    }

    @Test
    fun `if notificationType is null then systemNotificationsTracker should not track it`() {
        // arrange
        val notificationType: NotificationType? = null

        // act
        viewModel.start(site, editPostRepository, LocalId(0), mock(), notificationType)

        verify(systemNotificationsTracker, times(0)).trackTappedNotification(any())
    }

    @Test
    fun `If EditPostRepository is updated then the savePostToDbUseCase should be called`() {
        // arrange
        editPostRepository.set { mock() }
        val action = { _: PostModel -> true }

        // act
        viewModel.start(site, editPostRepository, LocalId(0), mock(), mock())
        editPostRepository.updateAsync(action, null)

        // assert
        verify(savePostToDbUseCase, times(1)).savePostToDb(any(), any())
    }

    @Test
    fun `If EditPostRepository is not updated then the savePostToDbUseCase should not be called`() {
        // act
        viewModel.start(site, editPostRepository, LocalId(0), mock(), mock())

        // assert
        verify(savePostToDbUseCase, times(0)).savePostToDb(any(), any())
    }

    @Test
    fun `If onStoryDiscarded is called then the post is removed with the dispatcher when deleteDiscardedPost true `() {
        // act
        viewModel.start(site, editPostRepository, LocalId(0), mock(), mock())
        viewModel.onStoryDiscarded(deleteDiscardedPost = true)

        // assert
        verify(dispatcher, times(1)).dispatch(any<Action<PostModel>>())
    }

    @Test
    fun `If onStoryDiscarded is called then the post is not removed when deleteDiscardedPost false `() {
        // act
        viewModel.start(site, editPostRepository, LocalId(0), mock(), mock())
        viewModel.onStoryDiscarded(deleteDiscardedPost = false)

        // assert
        verify(dispatcher, times(0)).dispatch(any<Action<PostModel>>())
    }

    @Test
    fun `verify that triggering onStorySaveButtonPressed will trigger the associated openPrepublishingBottomSheet`() {
        // act
        viewModel.onStorySaveButtonPressed()

        // assert
        assertThat(viewModel.openPrepublishingBottomSheet.value).isNotNull
    }

    @Test
    fun `if onSubmitClicked then setUntitledStoryTitleIfTitleEmptyUseCase should be triggered`() {
        // act
        viewModel.start(site, editPostRepository, LocalId(0), mock(), mock())
        viewModel.onSubmitButtonClicked()

        // assert
        verify(setUntitledStoryTitleIfTitleEmptyUseCase, times(1))
                .setUntitledStoryTitleIfTitleEmpty(any())
    }

    @Test
    fun `if onSubmitClicked then submitButtonClicked LiveData event should be triggered`() {
        // act
        viewModel.start(site, editPostRepository, LocalId(0), mock(), mock())
        viewModel.onSubmitButtonClicked()

        // assert
        assertThat(viewModel.submitButtonClicked.value).isNotNull
    }

    @Test
    fun `if appendMediaFiles is called then the _mediaFilesUris LiveData event is called`() {
        // act
        viewModel.appendMediaFiles(mock())

        // assert
        assertThat(viewModel.mediaFilesUris).isNotNull
    }
}
