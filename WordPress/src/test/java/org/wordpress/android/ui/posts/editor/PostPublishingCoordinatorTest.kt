package org.wordpress.android.ui.posts.editor

import android.content.Context
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.EditPostRepository.UpdatePostResult
import org.wordpress.android.ui.posts.PostEditorAnalyticsSession
import org.wordpress.android.ui.posts.PostUtilsWrapper
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@RunWith(MockitoJUnitRunner.Silent::class)
class PostPublishingCoordinatorTest {
    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var dispatcher: Dispatcher

    @Mock
    private lateinit var postUtilsWrapper: PostUtilsWrapper

    @Mock
    private lateinit var uploadUtilsWrapper: UploadUtilsWrapper

    @Mock
    private lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Mock
    private lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper

    @Mock
    private lateinit var listener: PostPublishingCoordinator.PublishingListener

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var storePostViewModel: StorePostViewModel

    @Mock
    private lateinit var editPostRepository: EditPostRepository

    @Mock
    private lateinit var siteModel: SiteModel

    @Mock
    private lateinit var analyticsSession: PostEditorAnalyticsSession

    private lateinit var coordinator: PostPublishingCoordinator

    @Before
    fun setUp() {
        coordinator = PostPublishingCoordinator(
            accountStore,
            dispatcher,
            postUtilsWrapper,
            uploadUtilsWrapper,
            analyticsTrackerWrapper,
            dateTimeUtilsWrapper
        )
        coordinator.start(listener)

        whenever(listener.getContext()).thenReturn(context)
        whenever(listener.provideStorePostViewModel()).thenReturn(storePostViewModel)
        whenever(listener.getEditPostRepository()).thenReturn(editPostRepository)
        whenever(listener.getSiteModel()).thenReturn(siteModel)
        whenever(listener.getPostEditorAnalyticsSession()).thenReturn(analyticsSession)
    }

    // region performPrimaryAction tests
    @Test
    fun `performPrimaryAction tracks analytics and shows bottom sheet for PUBLISH_NOW`() {
        whenever(listener.providePrimaryAction()).thenReturn(PrimaryEditorAction.PUBLISH_NOW)

        coordinator.performPrimaryAction()

        verify(analyticsTrackerWrapper).track(Stat.EDITOR_POST_PUBLISH_TAPPED)
        verify(listener).showPrepublishingNudgeBottomSheet()
    }

    @Test
    fun `performPrimaryAction shows bottom sheet for UPDATE action`() {
        whenever(listener.providePrimaryAction()).thenReturn(PrimaryEditorAction.UPDATE)

        coordinator.performPrimaryAction()

        verify(listener).showPrepublishingNudgeBottomSheet()
        verify(analyticsTrackerWrapper, never()).track(Stat.EDITOR_POST_PUBLISH_TAPPED)
    }

    @Test
    fun `performPrimaryAction shows bottom sheet for SCHEDULE action`() {
        whenever(listener.providePrimaryAction()).thenReturn(PrimaryEditorAction.SCHEDULE)

        coordinator.performPrimaryAction()

        verify(listener).showPrepublishingNudgeBottomSheet()
    }

    @Test
    fun `performPrimaryAction shows bottom sheet for SUBMIT_FOR_REVIEW action`() {
        whenever(listener.providePrimaryAction()).thenReturn(PrimaryEditorAction.SUBMIT_FOR_REVIEW)

        coordinator.performPrimaryAction()

        verify(listener).showPrepublishingNudgeBottomSheet()
    }

    @Test
    fun `performPrimaryAction shows bottom sheet for CONTINUE action`() {
        whenever(listener.providePrimaryAction()).thenReturn(PrimaryEditorAction.CONTINUE)

        coordinator.performPrimaryAction()

        verify(listener).showPrepublishingNudgeBottomSheet()
    }

    @Test
    fun `performPrimaryAction calls uploadPost for SAVE action`() {
        whenever(listener.providePrimaryAction()).thenReturn(PrimaryEditorAction.SAVE)

        coordinator.performPrimaryAction()

        verify(listener).updateAndSavePostAsyncOnEditorExit(any())
    }
    // endregion

    // region uploadPost tests
    @Test
    fun `uploadPost calls updateAndSavePostAsyncOnEditorExit`() {
        coordinator.uploadPost(false)

        verify(listener).updateAndSavePostAsyncOnEditorExit(any())
    }
    // endregion

    // region shouldPerformPostUpdateAndPublish tests
    @Test
    fun `shouldPerformPostUpdateAndPublish returns true when email is verified and post is null`() {
        val account = mock<AccountModel>()
        whenever(account.emailVerified).thenReturn(true)
        whenever(accountStore.account).thenReturn(account)
        whenever(editPostRepository.getPost()).thenReturn(null)

        val result = coordinator.shouldPerformPostUpdateAndPublish()

        assertThat(result).isTrue()
    }

    @Test
    fun `shouldPerformPostUpdateAndPublish returns false when no listener`() {
        val newCoordinator = PostPublishingCoordinator(
            accountStore,
            dispatcher,
            postUtilsWrapper,
            uploadUtilsWrapper,
            analyticsTrackerWrapper,
            dateTimeUtilsWrapper
        )
        // Not started with listener

        val result = newCoordinator.shouldPerformPostUpdateAndPublish()

        assertThat(result).isFalse()
    }
    // endregion

    // region start tests
    @Test
    fun `coordinator works without listener before start`() {
        val newCoordinator = PostPublishingCoordinator(
            accountStore,
            dispatcher,
            postUtilsWrapper,
            uploadUtilsWrapper,
            analyticsTrackerWrapper,
            dateTimeUtilsWrapper
        )
        // Should not throw
        newCoordinator.performPrimaryAction()
    }
    // endregion
}
