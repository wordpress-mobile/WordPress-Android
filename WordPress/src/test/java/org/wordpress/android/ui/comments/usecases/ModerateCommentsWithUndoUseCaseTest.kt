package org.wordpress.android.ui.comments.usecases

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.DELETED
import org.wordpress.android.fluxc.model.CommentStatus.TRASH
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType.INVALID_INPUT
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.fluxc.store.CommentsStore.CommentsActionPayload
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.CommentsActionData
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.DoNotCare
import org.wordpress.android.models.usecases.CommentsUseCaseType
import org.wordpress.android.models.usecases.CommentsUseCaseType.MODERATE_USE_CASE
import org.wordpress.android.models.usecases.LocalCommentCacheUpdateHandler
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase.ModerateCommentsAction.OnModerateComment
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase.ModerateCommentsAction.OnPushComment
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase.ModerateCommentsAction.OnUndoModerateComment
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase.Parameters.ModerateCommentParameters
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase.Parameters.ModerateWithFallbackParameters
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase.SingleCommentModerationResult
import org.wordpress.android.models.usecases.ModerateCommentsResourceProvider
import org.wordpress.android.test
import org.wordpress.android.ui.comments.utils.approvedComment
import org.wordpress.android.ui.comments.utils.trashedComment
import org.wordpress.android.usecase.UseCaseResult

@ExperimentalCoroutinesApi
class ModerateCommentsWithUndoUseCaseTest : BaseUnitTest() {
    @Mock private lateinit var commentStore: CommentsStore
    @Mock private lateinit var localCommentCacheUpdateHandler: LocalCommentCacheUpdateHandler

    @Mock private lateinit var moderateCommentsResourceProvider: ModerateCommentsResourceProvider

    private lateinit var moderateCommentWithUndoUseCase: ModerateCommentWithUndoUseCase

    val site = SiteModel().also { it.id = 5 }.also { it.name = "Test Site" }

    @Before
    fun setup() = test {
        whenever(moderateCommentsResourceProvider.commentsStore).thenReturn(commentStore)
        whenever(moderateCommentsResourceProvider.localCommentCacheUpdateHandler).thenReturn(
                localCommentCacheUpdateHandler
        )

        `when`(commentStore.getCommentByLocalSiteAndRemoteId(eq(site.id), eq(1)))
                .thenReturn(listOf(approvedComment))

        moderateCommentWithUndoUseCase = ModerateCommentWithUndoUseCase(moderateCommentsResourceProvider)
    }

    // OnModerateComment

    @Test
    fun `OnModerateComment action moderates comment locally and emits success when there are no errors`() =
            runBlockingTest {
                whenever(commentStore.moderateCommentLocally(eq(site), eq(1), eq(UNAPPROVED)))
                        .thenReturn(
                                CommentsActionPayload(
                                        CommentsActionData(
                                                comments = listOf(approvedComment.copy(status = UNAPPROVED.toString())),
                                                rowsAffected = 1
                                        )
                                )
                        )

                val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, Any>>()

                val job = launch {
                    moderateCommentWithUndoUseCase.subscribe().collectLatest {
                        result.add(it)
                    }
                }

                moderateCommentWithUndoUseCase.manageAction(
                        OnModerateComment(ModerateCommentParameters(site, 1, UNAPPROVED))
                )

                assertThat(result).size().isEqualTo(1)

                val successResult = result.first()

                assertThat(successResult).isInstanceOf(UseCaseResult.Success::class.java)
                assertThat(successResult.type).isEqualTo(MODERATE_USE_CASE)

                val resultData = (successResult as UseCaseResult.Success).data

                assertThat(resultData).isInstanceOf(SingleCommentModerationResult::class.java)
                assertThat((resultData as SingleCommentModerationResult).newStatus).isEqualTo(UNAPPROVED)
                assertThat(resultData.oldStatus).isEqualTo(APPROVED)
                assertThat(resultData.remoteCommentId).isEqualTo(1)

                verify(commentStore, times(1)).getCommentByLocalSiteAndRemoteId(site.id, 1)

                verify(commentStore, times(1)).moderateCommentLocally(site, 1, UNAPPROVED)

                verify(localCommentCacheUpdateHandler, times(1)).requestCommentsUpdate()

                job.cancel()
            }

    @Test
    fun `OnModerateComment action emits error even when error is encountered`() =
            runBlockingTest {
                val error = CommentError(INVALID_INPUT, "test error message")

                whenever(commentStore.moderateCommentLocally(eq(site), eq(1), eq(UNAPPROVED)))
                        .thenReturn(CommentsActionPayload(error))

                val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, Any>>()

                val job = launch {
                    moderateCommentWithUndoUseCase.subscribe().collectLatest {
                        result.add(it)
                    }
                }

                moderateCommentWithUndoUseCase.manageAction(
                        OnModerateComment(ModerateCommentParameters(site, 1, UNAPPROVED))
                )

                assertThat(result).size().isEqualTo(1)

                val errorResult = result[0]

                assertThat(errorResult).isInstanceOf(UseCaseResult.Failure::class.java)
                assertThat(errorResult.type).isEqualTo(MODERATE_USE_CASE)
                assertThat((errorResult as UseCaseResult.Failure).cachedData).isEqualTo(DoNotCare)
                assertThat(errorResult.error.type).isEqualTo(INVALID_INPUT)
                assertThat(errorResult.error.message).isEqualTo("test error message")

                verify(commentStore, times(1)).getCommentByLocalSiteAndRemoteId(site.id, 1)

                verify(commentStore, times(1)).moderateCommentLocally(site, 1, UNAPPROVED)

                verify(localCommentCacheUpdateHandler, times(0)).requestCommentsUpdate()

                job.cancel()
            }

    // OnPushComment

    @Test
    fun `OnPushComment action moderates comment locally and remotely and emits success when there are no errors`() =
            runBlockingTest {
                whenever(commentStore.moderateCommentLocally(eq(site), eq(1), eq(UNAPPROVED)))
                        .thenReturn(
                                CommentsActionPayload(
                                        CommentsActionData(
                                                comments = listOf(approvedComment.copy(status = UNAPPROVED.toString())),
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

                val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, Any>>()

                val job = launch {
                    moderateCommentWithUndoUseCase.subscribe().collectLatest {
                        result.add(it)
                    }
                }

                moderateCommentWithUndoUseCase.manageAction(
                        OnPushComment(ModerateWithFallbackParameters(site, 1, UNAPPROVED, APPROVED))
                )

                assertThat(result).size().isEqualTo(1)

                val successResult = result[0]

                assertThat(successResult).isInstanceOf(UseCaseResult.Success::class.java)
                assertThat(successResult.type).isEqualTo(MODERATE_USE_CASE)
                assertThat((successResult as UseCaseResult.Success).data).isEqualTo(DoNotCare)

                verify(commentStore, times(1)).pushLocalCommentByRemoteId(site, 1)
                verify(commentStore, times(1)).moderateCommentLocally(site, 1, UNAPPROVED)
                verify(commentStore, never()).deleteComment(eq(site), any(), anyOrNull())

                // called twice - after local moderation, and after remote moderation
                verify(localCommentCacheUpdateHandler, times(2)).requestCommentsUpdate()

                job.cancel()
            }

    @Test
    fun `OnPushComment calls deleteComment method of the store when comment is being deleted`() =
            runBlockingTest {
                whenever(commentStore.moderateCommentLocally(eq(site), eq(3), eq(DELETED)))
                        .thenReturn(
                                CommentsActionPayload(
                                        CommentsActionData(
                                                comments = listOf(trashedComment.copy(status = DELETED.toString())),
                                                rowsAffected = 1
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

                val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, Any>>()

                val job = launch {
                    moderateCommentWithUndoUseCase.subscribe().collectLatest {
                        result.add(it)
                    }
                }

                moderateCommentWithUndoUseCase.manageAction(
                        OnPushComment(ModerateWithFallbackParameters(site, 3, DELETED, TRASH))
                )

                assertThat(result).size().isEqualTo(1)

                val successResult = result[0]

                assertThat(successResult).isInstanceOf(UseCaseResult.Success::class.java)
                assertThat(successResult.type).isEqualTo(MODERATE_USE_CASE)
                assertThat((successResult as UseCaseResult.Success).data).isEqualTo(DoNotCare)

                verify(commentStore, never()).pushLocalCommentByRemoteId(eq(site), any())
                verify(commentStore, times(1)).deleteComment(site, 3, null)
                verify(commentStore, times(1)).moderateCommentLocally(site, 3, DELETED)

                // called twice - after local moderation, and after remote moderation
                verify(localCommentCacheUpdateHandler, times(2)).requestCommentsUpdate()

                job.cancel()
            }

    @Test
    fun `moderation is rolled back and correct event is emitted when error is encountered when deleting comment`() =
            runBlockingTest {
                val error = CommentError(INVALID_INPUT, "test error message")

                whenever(commentStore.moderateCommentLocally(eq(site), eq(3), eq(DELETED)))
                        .thenReturn(
                                CommentsActionPayload(
                                        CommentsActionData(
                                                comments = listOf(trashedComment.copy(status = DELETED.toString())),
                                                rowsAffected = 1
                                        )
                                )
                        )

                whenever(commentStore.deleteComment(eq(site), eq(3), anyOrNull()))
                        .thenReturn(CommentsActionPayload(error))

                val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, Any>>()

                val job = launch {
                    moderateCommentWithUndoUseCase.subscribe().collectLatest {
                        result.add(it)
                    }
                }

                moderateCommentWithUndoUseCase.manageAction(
                        OnPushComment(ModerateWithFallbackParameters(site, 3, DELETED, TRASH))
                )

                assertThat(result).size().isEqualTo(1)

                val errorResult = result[0]

                assertThat(errorResult).isInstanceOf(UseCaseResult.Failure::class.java)
                assertThat(errorResult.type).isEqualTo(MODERATE_USE_CASE)
                assertThat((errorResult as UseCaseResult.Failure).cachedData).isEqualTo(DoNotCare)
                assertThat(errorResult.error.type).isEqualTo(INVALID_INPUT)
                assertThat(errorResult.error.message).isEqualTo("test error message")

                verify(commentStore, never()).pushLocalCommentByRemoteId(eq(site), any())
                verify(commentStore, times(1)).deleteComment(site, 3, null)
                verify(commentStore, times(1)).moderateCommentLocally(site, 3, DELETED)

                // rolling back moderation after error

                verify(commentStore, times(1)).moderateCommentLocally(site, 3, TRASH)

                // called twice - after local moderation, and after remote moderation
                verify(localCommentCacheUpdateHandler, times(2)).requestCommentsUpdate()

                job.cancel()
            }

    @Test
    fun `moderation is rolled back and correct event is emitted when error is encountered when moderating comment`() =
            runBlockingTest {
                val error = CommentError(INVALID_INPUT, "test error message")

                whenever(commentStore.moderateCommentLocally(eq(site), eq(1), eq(UNAPPROVED)))
                        .thenReturn(
                                CommentsActionPayload(
                                        CommentsActionData(
                                                comments = listOf(approvedComment.copy(status = UNAPPROVED.toString())),
                                                rowsAffected = 1
                                        )
                                )
                        )

                whenever(commentStore.pushLocalCommentByRemoteId(eq(site), eq(1)))
                        .thenReturn(CommentsActionPayload(error))

                val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, Any>>()

                val job = launch {
                    moderateCommentWithUndoUseCase.subscribe().collectLatest {
                        result.add(it)
                    }
                }

                moderateCommentWithUndoUseCase.manageAction(
                        OnPushComment(ModerateWithFallbackParameters(site, 1, UNAPPROVED, APPROVED))
                )

                assertThat(result).size().isEqualTo(1)

                val errorResult = result[0]

                assertThat(errorResult).isInstanceOf(UseCaseResult.Failure::class.java)
                assertThat(errorResult.type).isEqualTo(MODERATE_USE_CASE)
                assertThat((errorResult as UseCaseResult.Failure).cachedData).isEqualTo(DoNotCare)
                assertThat(errorResult.error.type).isEqualTo(INVALID_INPUT)
                assertThat(errorResult.error.message).isEqualTo("test error message")

                verify(commentStore, times(1)).pushLocalCommentByRemoteId(eq(site), any())
                verify(commentStore, never()).deleteComment(eq(site), any(), anyOrNull())
                verify(commentStore, times(1)).pushLocalCommentByRemoteId(site, 1)
                verify(commentStore, times(1)).moderateCommentLocally(site, 1, UNAPPROVED)

                // rolling back moderation after error

                verify(commentStore, times(1)).moderateCommentLocally(site, 1, APPROVED)

                // called twice - after local moderation, and after remote moderation
                verify(localCommentCacheUpdateHandler, times(2)).requestCommentsUpdate()

                job.cancel()
            }

    // OnUndoModerateComment

    @Test
    fun `undoing moderation rolls back local comment status and emits success`() =
            runBlockingTest {
                whenever(commentStore.moderateCommentLocally(eq(site), eq(3), eq(APPROVED)))
                        .thenReturn(
                                CommentsActionPayload(
                                        CommentsActionData(
                                                comments = listOf(trashedComment.copy(status = APPROVED.toString())),
                                                rowsAffected = 1
                                        )
                                )
                        )

                val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, Any>>()

                val job = launch {
                    moderateCommentWithUndoUseCase.subscribe().collectLatest {
                        result.add(it)
                    }
                }

                moderateCommentWithUndoUseCase.manageAction(
                        OnUndoModerateComment(ModerateWithFallbackParameters(site, 3, TRASH, APPROVED))
                )

                assertThat(result).size().isEqualTo(1)

                val successResult = result[0]

                assertThat(successResult).isInstanceOf(UseCaseResult.Success::class.java)
                assertThat(successResult.type).isEqualTo(MODERATE_USE_CASE)
                assertThat((successResult as UseCaseResult.Success).data).isEqualTo(DoNotCare)

                verify(commentStore, times(1)).moderateCommentLocally(site, 3, APPROVED)
                verify(localCommentCacheUpdateHandler, times(1)).requestCommentsUpdate()

                job.cancel()
            }
}
