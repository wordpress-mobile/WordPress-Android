package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener
import org.wordpress.android.ui.reader.actions.ReaderPostActionsWrapper
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase.PostLikeState.Failed
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase.PostLikeState.Failed.NoNetwork
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase.PostLikeState.Success
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase.PostLikeState.Unchanged
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.util.NetworkUtilsWrapper

private const val POST_AND_BLOG_ID = 1L
private const val SOURCE = "source"

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PostLikeUseCaseTest : BaseUnitTest() {
    @Mock private lateinit var accountStore: AccountStore
    @Mock private lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock private lateinit var readerPostActionsWrapper: ReaderPostActionsWrapper
    @Mock private lateinit var readerTracker: ReaderTracker

    private lateinit var useCase: PostLikeUseCase

    @Before
    fun setUp() {
        val account = AccountModel()
        account.userId = 100

        whenever(accountStore.account).thenReturn(account)

        useCase = PostLikeUseCase(
                readerPostActionsWrapper,
                readerTracker,
                accountStore,
                networkUtilsWrapper
        )
    }

    @Test
    fun `NoNetwork returned when no network found`() = test {
        // Given
        val readerPost = init(isNetworkAvailable = false)

        // When
        val result = useCase.perform(
                readerPost,
                true,
                SOURCE
        ).toList(mutableListOf())

        // Then
        assertThat(result[0]).isEqualTo(NoNetwork)
    }

    @Test
    fun `unchanged returned when already liked and requesting to like`() = test {
        // Given
        val readerPost = init(isLikedByCurrentUser = true, localSucceeds = false)

        // When
        val result = useCase.perform(
                readerPost,
                true,
                SOURCE
        ).toList(mutableListOf())

        // Then
        assertThat(result[0]).isEqualTo(Unchanged)
    }

    @Test
    fun `unchanged returned when already unliked and requesting to unlike`() = test {
        // Given
        val readerPost = init(isLikedByCurrentUser = false, localSucceeds = false)

        // When
        val result = useCase.perform(
                readerPost,
                false,
                SOURCE
        ).toList(mutableListOf())

        // Then
        assertThat(result[0]).isEqualTo(Unchanged)
    }

    @Test
    fun `success is returned when liking an unliked post`() =
            test {
                val readerPost = init(isLikedByCurrentUser = false)
                // Act
                val result = useCase.perform(
                        readerPost,
                        true,
                        SOURCE
                ).toList(mutableListOf())

                // Assert
                assertThat((result)).contains(Success)
            }

    @Test
    fun `success is returned when unliking a like post`() =
            test {
                val readerPost = init(isLikedByCurrentUser = true)

                // Act
                val result = useCase.perform(
                        readerPost,
                        false,
                        SOURCE
                ).toList(mutableListOf())

                // Assert
                assertThat((result)).contains(Success)
            }

    @Test
    fun `failure is returned when liking an unliked post`() =
            test {
                val readerPost = init(isLikedByCurrentUser = false, remoteSucceeds = false)

                // Act
                val result = useCase.perform(
                        readerPost,
                        true,
                        SOURCE
                ).toList(mutableListOf())

                // Assert
                assertThat((result)).contains(Failed.RequestFailed)
            }

    @Test
    fun `failure is returned when unliking a like post`() =
            test {
                val readerPost = init(isLikedByCurrentUser = true, remoteSucceeds = false)

                // Act
                val result = useCase.perform(
                        readerPost,
                        false,
                        SOURCE
                ).toList(mutableListOf())

                // Assert
                assertThat((result)).contains(Failed.RequestFailed)
            }

    @Test
    fun `like local action is triggered for selected reader post`() =
            test {
                val readerPost = init(isLikedByCurrentUser = false)

                // Act
                useCase.perform(
                        readerPost,
                        true,
                        SOURCE
                ).toList(mutableListOf())

                // Assert
                verify(readerPostActionsWrapper).performLikeActionLocal(
                        anyOrNull(),
                        anyBoolean(),
                        anyLong()
                )
            }

    @Test
    fun `like remote action is triggered for selected reader post`() =
            test {
                val readerPost = init(isLikedByCurrentUser = false)

                // Act
                useCase.perform(
                        readerPost,
                        true,
                        SOURCE
                ).toList(mutableListOf())

                // Assert
                verify(readerPostActionsWrapper).performLikeActionRemote(
                        anyOrNull(),
                        anyBoolean(),
                        anyLong(),
                        anyOrNull()
                )
            }

    @Test
    fun `Post views bumped when asking to like`() =
            test {
                val readerPost = init(isLikedByCurrentUser = false)
                // Act
                useCase.perform(
                        readerPost,
                        true,
                        SOURCE
                ).toList(mutableListOf())

                // Assert
                verify(readerPostActionsWrapper).bumpPageViewForPost(anyOrNull())
            }

    @Test
    fun `Post views NOT bumped when asking to unlike`() =
            test {
                val readerPost = init(isLikedByCurrentUser = true)

                // Act
                useCase.perform(
                        readerPost,
                        false,
                        SOURCE
                ).toList(mutableListOf())

                // Assert
                verify(readerPostActionsWrapper, never()).bumpPageViewForPost(anyOrNull())
            }

    private fun init(
        isLikedByCurrentUser: Boolean = false,
        isNetworkAvailable: Boolean = true,
        localSucceeds: Boolean = true,
        remoteSucceeds: Boolean = true
    ): ReaderPost {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(isNetworkAvailable)
        whenever(readerPostActionsWrapper.performLikeActionLocal(anyOrNull(), anyBoolean(), anyLong()))
                .thenReturn(localSucceeds)
        whenever(readerPostActionsWrapper.performLikeActionRemote(anyOrNull(), anyBoolean(), anyLong(), anyOrNull()))
                .then {
                    (it.arguments[3] as ActionListener).onActionResult(remoteSucceeds)
                }
        return createDummyReaderPost(isLikedByCurrentUser = isLikedByCurrentUser)
    }

    private fun createDummyReaderPost(
        id: Long = POST_AND_BLOG_ID,
        isLikedByCurrentUser: Boolean = false
    ): ReaderPost =
            ReaderPost().apply {
                this.postId = id
                this.blogId = id
                this.isLikedByCurrentUser = isLikedByCurrentUser
            }
}
