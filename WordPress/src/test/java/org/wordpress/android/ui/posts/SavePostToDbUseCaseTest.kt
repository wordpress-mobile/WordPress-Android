package org.wordpress.android.ui.posts

import android.content.Context
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.model.post.PostStatus.PENDING
import org.wordpress.android.fluxc.model.post.PostStatus.PRIVATE
import org.wordpress.android.fluxc.model.post.PostStatus.PUBLISHED
import org.wordpress.android.fluxc.model.post.PostStatus.SCHEDULED
import org.wordpress.android.fluxc.model.post.PostStatus.TRASHED
import org.wordpress.android.fluxc.model.post.PostStatus.UNKNOWN
import org.wordpress.android.ui.notifications.utils.PendingDraftsNotificationsUtilsWrapper
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.util.DateTimeUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
class SavePostToDbUseCaseTest {
    @Mock lateinit var uploadUtils: UploadUtilsWrapper
    @Mock lateinit var dateTimeUtils: DateTimeUtilsWrapper
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var pendingDraftsNotificationsUtils: PendingDraftsNotificationsUtilsWrapper
    @Mock lateinit var postRepository: EditPostRepository
    @Mock lateinit var context: Context
    private lateinit var siteModel: SiteModel
    private lateinit var postModel: PostModel
    private lateinit var savePostToDbUseCase: SavePostToDbUseCase
    private lateinit var actionCaptor: KArgumentCaptor<Action<PostModel>>
    private val currentTime = "2019-08-09T10:01:03+00:00"
    private val postId = 1

    @Before
    fun setUp() {
        savePostToDbUseCase = SavePostToDbUseCase(
                uploadUtils,
                dateTimeUtils,
                dispatcher,
                pendingDraftsNotificationsUtils,
                context
        )
        actionCaptor = argumentCaptor()
        whenever(dateTimeUtils.currentTimeInIso8601()).thenReturn(currentTime)
        whenever(dispatcher.dispatch(actionCaptor.capture())).doAnswer { }
        siteModel = SiteModel()
        postModel = PostModel()
    }

    @Test
    fun `sets locally changed and saves to DB when there are changes to the post`() {
        // Given
        setupPost(postHasChanges = true)
        // When
        savePostToDbUseCase.savePostToDb(postRepository, siteModel)
        // Then
        verify(postRepository).savePostSnapshot()
        assertThat(actionCaptor.firstValue).isNotNull
        assertThat(postModel.isLocallyChanged).isTrue()
        assertThat(postModel.dateLocallyChanged).isEqualTo(currentTime)
    }

    @Test
    fun `does not save post with no changes`() {
        // Given
        setupPost(postHasChanges = false)
        // When
        savePostToDbUseCase.savePostToDb(postRepository, siteModel)
        // Then
        verify(postRepository, never()).savePostSnapshot()
        assertThat(actionCaptor.allValues).isEmpty()
        assertThat(postModel.isLocallyChanged).isFalse()
        assertThat(postModel.dateLocallyChanged).isNullOrEmpty()
    }

    @Test
    fun `sets status PENDING when cannot publish and is PUBLISHED, SCHEDULED, PRIVATE, UNKNOWN`() {
        listOf(
                UNKNOWN,
                PUBLISHED,
                SCHEDULED,
                PRIVATE
        ).forEach { postStatus ->
            // Given
            setupPost(userCanPublish = false, postStatus = postStatus)
            // When
            savePostToDbUseCase.savePostToDb(postRepository, siteModel)
            // Then
            assertThat(postModel.status).isEqualTo(PENDING.toString())
        }
    }

    @Test
    fun `does not change status when cannot publish and is DRAFT, PENDING, TRASHED`() {
        listOf(
                DRAFT,
                PENDING,
                TRASHED
        ).forEach { postStatus ->
            // Given
            setupPost(userCanPublish = false, postStatus = postStatus)
            // When
            savePostToDbUseCase.savePostToDb(postRepository, siteModel)
            // Then
            assertThat(postModel.status).isEqualTo(postStatus.toString())
        }
    }

    @Test
    fun `does not change status when user can publish`() {
        listOf(
                UNKNOWN,
                PUBLISHED,
                SCHEDULED,
                PRIVATE,
                DRAFT,
                PENDING,
                TRASHED
        ).forEach { postStatus ->
            // Given
            setupPost(userCanPublish = true, postStatus = postStatus)
            // When
            savePostToDbUseCase.savePostToDb(postRepository, siteModel)
            // Then
            assertThat(postModel.status).isEqualTo(postStatus.toString())
        }
    }

    @Test
    fun `cancels pending draft notifications when post is not draft`() {
        // Given
        setupPost(postStatus = PUBLISHED)
        // When
        savePostToDbUseCase.savePostToDb(postRepository, siteModel)
        // Then
        verify(pendingDraftsNotificationsUtils).cancelPendingDraftAlarms(context, postId)
    }

    @Test
    fun `schedules pending draft notifications when post is draft`() {
        // Given
        setupPost(postStatus = DRAFT)
        whenever(postRepository.dateLocallyChanged).thenReturn(currentTime)
        // When
        savePostToDbUseCase.savePostToDb(postRepository, siteModel)
        // Then
        verify(pendingDraftsNotificationsUtils).scheduleNextNotifications(
                context,
                postId,
                currentTime
        )
    }

    private fun setupPost(
        postHasChanges: Boolean = true,
        userCanPublish: Boolean = true,
        postStatus: PostStatus = UNKNOWN
    ) {
        whenever(postRepository.postHasChanges()).thenReturn(postHasChanges)
        whenever(postRepository.getEditablePost()).thenReturn(postModel)
        whenever(uploadUtils.userCanPublish(siteModel)).thenReturn(userCanPublish)
        whenever(postRepository.status).thenReturn(postStatus)
        whenever(postRepository.id).thenReturn(postId)
        postModel.setStatus(postStatus.toString())
        postModel.setId(postId)
    }
}
