package org.wordpress.android.ui.comments.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.notification.Failure
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.DELETED
import org.wordpress.android.fluxc.model.CommentStatus.TRASH
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType.INVALID_INPUT
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.fluxc.store.CommentsStore.CommentsActionPayload
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.CommentsActionData
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.DoNotCare
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.ModerateCommentsAction.OnModerateComments
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.Parameters.ModerateCommentsParameters
import org.wordpress.android.models.usecases.CommentsUseCaseType
import org.wordpress.android.models.usecases.CommentsUseCaseType.BATCH_MODERATE_USE_CASE
import org.wordpress.android.models.usecases.LocalCommentCacheUpdateHandler
import org.wordpress.android.models.usecases.ModerateCommentsResourceProvider
import org.wordpress.android.ui.comments.utils.approvedComment
import org.wordpress.android.ui.comments.utils.pendingComment
import org.wordpress.android.ui.comments.utils.trashedComment
import org.wordpress.android.usecase.UseCaseResult
import org.wordpress.android.util.NoDelayCoroutineDispatcher

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class BatchModerateCommentsUseCaseTest : BaseUnitTest() {
    @Mock
    private lateinit var commentStore: CommentsStore

    @Mock
    private lateinit var localCommentCacheUpdateHandler: LocalCommentCacheUpdateHandler

    @Mock
    private lateinit var moderateCommentsResourceProvider: ModerateCommentsResourceProvider

    private lateinit var batchModerateCommentsUseCase: BatchModerateCommentsUseCase

    val site = SiteModel().also { it.id = 5 }.also { it.name = "Test Site" }

    @Before
    fun setup() = test {
        whenever(moderateCommentsResourceProvider.commentsStore).thenReturn(commentStore)
        whenever(moderateCommentsResourceProvider.localCommentCacheUpdateHandler).thenReturn(
            localCommentCacheUpdateHandler
        )
        whenever(moderateCommentsResourceProvider.bgDispatcher).thenReturn(NoDelayCoroutineDispatcher())

        `when`(commentStore.getCommentByLocalSiteAndRemoteId(eq(site.id), eq(1)))
            .thenReturn(listOf(approvedComment))
        `when`(commentStore.getCommentByLocalSiteAndRemoteId(eq(site.id), eq(2)))
            .thenReturn(listOf(pendingComment))
        `when`(commentStore.getCommentByLocalSiteAndRemoteId(eq(site.id), eq(3)))
            .thenReturn(listOf(trashedComment))

        batchModerateCommentsUseCase = BatchModerateCommentsUseCase(moderateCommentsResourceProvider)
    }

    @Test
    fun `if the error happens when moderating a generic comment error event is emitted`() = test {
        val error = CommentError(INVALID_INPUT, "")

        whenever(commentStore.moderateCommentLocally(eq(site), eq(1), eq(UNAPPROVED)))
            .thenReturn(CommentsActionPayload(error))

        val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, DoNotCare>>()

        val job = launch {
            batchModerateCommentsUseCase.subscribe().collectLatest {
                result.add(it)
            }
        }

        batchModerateCommentsUseCase.manageAction(
            OnModerateComments(
                ModerateCommentsParameters(
                    site,
                    listOf(1),
                    UNAPPROVED
                )
            )
        )

        assertThat(result).size().isEqualTo(1)

        val errorResult = result[0]

        assertThat(errorResult).isInstanceOf(UseCaseResult.Failure::class.java)
        assertThat(errorResult.type).isEqualTo(BATCH_MODERATE_USE_CASE)
        assertThat((errorResult as UseCaseResult.Failure).cachedData).isEqualTo(DoNotCare)
        assertThat(errorResult.error.type).isEqualTo(CommentErrorType.GENERIC_ERROR)
        assertThat(errorResult.error.message).isEmpty()

        job.cancel()
    }

    @Test
    fun `if the error happens when moderating multiple comments single error event is emitted`() = test {
        val error = CommentError(INVALID_INPUT, "")

        whenever(commentStore.moderateCommentLocally(eq(site), eq(1), eq(UNAPPROVED)))
            .thenReturn(CommentsActionPayload(error))

        whenever(commentStore.moderateCommentLocally(eq(site), eq(2), eq(UNAPPROVED)))
            .thenReturn(CommentsActionPayload(error))

        val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, DoNotCare>>()

        val job = launch {
            batchModerateCommentsUseCase.subscribe().collectLatest {
                result.add(it)
            }
        }

        batchModerateCommentsUseCase.manageAction(
            OnModerateComments(
                ModerateCommentsParameters(
                    site,
                    listOf(1, 2),
                    UNAPPROVED
                )
            )
        )

        assertThat(result).size().isEqualTo(1)

        val errorResult = result[0]

        assertThat(errorResult).isInstanceOf(UseCaseResult.Failure::class.java)
        assertThat(errorResult.type).isEqualTo(BATCH_MODERATE_USE_CASE)
        assertThat((errorResult as UseCaseResult.Failure).cachedData).isEqualTo(DoNotCare)
        assertThat(errorResult.error.type).isEqualTo(CommentErrorType.GENERIC_ERROR)
        assertThat(errorResult.error.message).isEmpty()

        job.cancel()
    }

    @Test
    fun `if the error happens only with some of many moderated comments single error event is emitted`() = test {
        val error = CommentError(INVALID_INPUT, "")

        whenever(commentStore.moderateCommentLocally(eq(site), eq(1), eq(UNAPPROVED)))
            .thenReturn(CommentsActionPayload(error))

        whenever(commentStore.moderateCommentLocally(eq(site), eq(3), eq(UNAPPROVED)))
            .thenReturn(
                CommentsActionPayload(
                    CommentsActionData(
                        comments = listOf(trashedComment.copy(status = UNAPPROVED.toString())),
                        rowsAffected = 1
                    )
                )
            )

        whenever(commentStore.pushLocalCommentByRemoteId(eq(site), eq(3)))
            .thenReturn(
                CommentsActionPayload(
                    CommentsActionData(
                        listOf(trashedComment.copy(status = UNAPPROVED.toString())),
                        1
                    )
                )
            )

        val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, DoNotCare>>()

        val job = launch {
            batchModerateCommentsUseCase.subscribe().collectLatest {
                result.add(it)
            }
        }

        batchModerateCommentsUseCase.manageAction(
            OnModerateComments(
                ModerateCommentsParameters(
                    site,
                    listOf(1, 3),
                    UNAPPROVED
                )
            )
        )

        assertThat(result).size().isEqualTo(1)

        val errorResult = result[0]

        assertThat(errorResult).isInstanceOf(UseCaseResult.Failure::class.java)
        assertThat(errorResult.type).isEqualTo(BATCH_MODERATE_USE_CASE)
        assertThat((errorResult as UseCaseResult.Failure).cachedData).isEqualTo(DoNotCare)
        assertThat(errorResult.error.type).isEqualTo(CommentErrorType.GENERIC_ERROR)
        assertThat(errorResult.error.message).isEmpty()

        job.cancel()
    }

    @Test
    @Suppress("LongMethod")
    fun `moderating multiple comments works as expected`() = test {
        whenever(commentStore.moderateCommentLocally(eq(site), eq(1), eq(UNAPPROVED)))
            .thenReturn(
                CommentsActionPayload(
                    CommentsActionData(
                        comments = listOf(approvedComment.copy(status = UNAPPROVED.toString())),
                        rowsAffected = 1
                    )
                )
            )

        whenever(commentStore.moderateCommentLocally(eq(site), eq(3), eq(UNAPPROVED)))
            .thenReturn(
                CommentsActionPayload(
                    CommentsActionData(
                        comments = listOf(trashedComment.copy(status = UNAPPROVED.toString())),
                        rowsAffected = 1
                    )
                )
            )

        whenever(commentStore.pushLocalCommentByRemoteId(eq(site), eq(1)))
            .thenReturn(
                CommentsActionPayload(
                    CommentsActionData(
                        listOf(approvedComment.copy(status = UNAPPROVED.toString())),
                        1
                    )
                )
            )

        whenever(commentStore.pushLocalCommentByRemoteId(eq(site), eq(3)))
            .thenReturn(
                CommentsActionPayload(
                    CommentsActionData(
                        listOf(trashedComment.copy(status = UNAPPROVED.toString())),
                        1
                    )
                )
            )

        val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, DoNotCare>>()

        val job = launch {
            batchModerateCommentsUseCase.subscribe().collectLatest {
                result.add(it)
            }
        }

        batchModerateCommentsUseCase.manageAction(
            OnModerateComments(
                ModerateCommentsParameters(
                    site,
                    listOf(1, 3),
                    UNAPPROVED
                )
            )
        )

        result.forEach { assertThat(it).isNotInstanceOf(Failure::class.java) }

        verify(commentStore, times(1)).getCommentByLocalSiteAndRemoteId(site.id, 1)
        verify(commentStore, times(1)).getCommentByLocalSiteAndRemoteId(site.id, 3)
        verify(commentStore, times(1)).moderateCommentLocally(site, 1, UNAPPROVED)
        verify(commentStore, times(1)).moderateCommentLocally(site, 3, UNAPPROVED)
        verify(commentStore, times(1)).pushLocalCommentByRemoteId(site, 1)
        verify(commentStore, times(1)).pushLocalCommentByRemoteId(site, 3)
        verify(localCommentCacheUpdateHandler, times(2)).requestCommentsUpdate()

        job.cancel()
    }

    @Test
    @Suppress("LongMethod")
    fun `if we are deleting comments deleteComment method of a store is called`() = test {
        whenever(commentStore.moderateCommentLocally(eq(site), eq(1), eq(DELETED)))
            .thenReturn(
                CommentsActionPayload(
                    CommentsActionData(
                        comments = listOf(approvedComment.copy(status = DELETED.toString())),
                        rowsAffected = 1
                    )
                )
            )

        whenever(commentStore.moderateCommentLocally(eq(site), eq(3), eq(DELETED)))
            .thenReturn(
                CommentsActionPayload(
                    CommentsActionData(
                        comments = listOf(trashedComment.copy(status = DELETED.toString())),
                        rowsAffected = 1
                    )
                )
            )

        whenever(commentStore.deleteComment(eq(site), eq(1), anyOrNull()))
            .thenReturn(
                CommentsActionPayload(
                    CommentsActionData(
                        listOf(approvedComment.copy(status = DELETED.toString())),
                        1
                    )
                )
            )

        whenever(commentStore.deleteComment(eq(site), eq(3), anyOrNull()))
            .thenReturn(
                CommentsActionPayload(
                    CommentsActionData(
                        listOf(trashedComment.copy(status = DELETED.toString())),
                        1
                    )
                )
            )

        val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, DoNotCare>>()

        val job = launch {
            batchModerateCommentsUseCase.subscribe().collectLatest {
                result.add(it)
            }
        }

        batchModerateCommentsUseCase.manageAction(
            OnModerateComments(
                ModerateCommentsParameters(
                    site,
                    listOf(1, 3),
                    DELETED
                )
            )
        )

        result.forEach { assertThat(it).isNotInstanceOf(Failure::class.java) }

        verify(commentStore, times(1)).getCommentByLocalSiteAndRemoteId(site.id, 1)
        verify(commentStore, times(1)).getCommentByLocalSiteAndRemoteId(site.id, 3)
        verify(commentStore, times(1)).moderateCommentLocally(site, 1, DELETED)
        verify(commentStore, times(1)).moderateCommentLocally(site, 3, DELETED)
        verify(commentStore, times(1)).deleteComment(site, 1, null)
        verify(commentStore, times(1)).deleteComment(site, 3, null)
        verify(localCommentCacheUpdateHandler, times(2)).requestCommentsUpdate()

        job.cancel()
    }

    @Test
    @Suppress("LongMethod")
    fun `rollback if an error when deleting or moderating a comment and request local cache refresh`() = test {
        val error = CommentError(INVALID_INPUT, "")

        whenever(commentStore.moderateCommentLocally(eq(site), eq(1), eq(UNAPPROVED)))
            .thenReturn(
                CommentsActionPayload(
                    CommentsActionData(
                        comments = listOf(approvedComment.copy(status = UNAPPROVED.toString())),
                        rowsAffected = 1
                    )
                )
            )

        whenever(commentStore.moderateCommentLocally(eq(site), eq(3), eq(DELETED)))
            .thenReturn(
                CommentsActionPayload(
                    CommentsActionData(
                        comments = listOf(trashedComment.copy(status = DELETED.toString())),
                        rowsAffected = 1
                    )
                )
            )

        whenever(commentStore.pushLocalCommentByRemoteId(eq(site), eq(1)))
            .thenReturn(
                CommentsActionPayload(
                    error
                )
            )

        whenever(commentStore.deleteComment(eq(site), eq(3), anyOrNull()))
            .thenReturn(
                CommentsActionPayload(
                    error
                )
            )

        val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, DoNotCare>>()

        val job = launch {
            batchModerateCommentsUseCase.subscribe().collectLatest {
                result.add(it)
            }
        }

        batchModerateCommentsUseCase.manageAction(
            OnModerateComments(
                ModerateCommentsParameters(
                    site,
                    listOf(1),
                    UNAPPROVED
                )
            )
        )

        batchModerateCommentsUseCase.manageAction(
            OnModerateComments(
                ModerateCommentsParameters(
                    site,
                    listOf(3),
                    DELETED
                )
            )
        )

        assertThat(result.size).isEqualTo(2)
        result.filterIsInstance<UseCaseResult.Failure<CommentsUseCaseType, CommentError, DoNotCare>>().let {
            assertThat(it.size).isEqualTo(2)
        }

        // getting a backup from DB

        verify(commentStore, times(1)).getCommentByLocalSiteAndRemoteId(site.id, 1)
        verify(commentStore, times(1)).getCommentByLocalSiteAndRemoteId(site.id, 3)

        // moderating locally

        verify(commentStore, times(1)).moderateCommentLocally(site, 1, UNAPPROVED)
        verify(commentStore, times(1)).moderateCommentLocally(site, 3, DELETED)

        // try and fail to moderate remotely

        verify(commentStore, times(1)).pushLocalCommentByRemoteId(site, 1)
        verify(commentStore, times(1)).deleteComment(site, 3, null)

        // reverting original state

        verify(commentStore, times(1)).moderateCommentLocally(site, 1, APPROVED)

        verify(commentStore, times(1)).moderateCommentLocally(site, 3, TRASH)
        verify(localCommentCacheUpdateHandler, times(4)).requestCommentsUpdate()

        job.cancel()
    }
}
