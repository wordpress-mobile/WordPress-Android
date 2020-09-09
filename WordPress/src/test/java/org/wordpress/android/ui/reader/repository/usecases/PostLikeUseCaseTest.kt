package org.wordpress.android.ui.reader.repository.usecases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.test
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener
import org.wordpress.android.ui.reader.actions.ReaderPostActionsWrapper
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase.PostLikeState.Failed
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase.PostLikeState.Failed.NoNetwork
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase.PostLikeState.Success
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase.PostLikeState.Unchanged
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper

private const val POST_AND_BLOG_ID = 1L

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PostLikeUseCaseTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Mock private lateinit var accountStore: AccountStore
    @Mock private lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock private lateinit var readerPostActionsWrapper: ReaderPostActionsWrapper
    @Mock private lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper
    @Mock private lateinit var readerPostTableWrapper: ReaderPostTableWrapper
    @Mock private lateinit var siteStore: SiteStore

    private lateinit var useCase: PostLikeUseCase

    private val readerPost = createDummyReaderPost()

    @Before
    fun setUp() {
        val account = AccountModel()
        account.userId = 100
        val siteModel = SiteModel()
        siteModel.id = 100

        whenever(accountStore.account).thenReturn(account)
        whenever(siteStore.getSiteByLocalId(anyInt())).thenReturn(siteModel)

        useCase = PostLikeUseCase(
                readerPostActionsWrapper,
                analyticsUtilsWrapper,
                accountStore,
                networkUtilsWrapper,
                TEST_DISPATCHER
        )
    }

    @Test
    fun `NoNetwork returned when no network found`() = test {
        // Given
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        // When
        val result = useCase.perform(readerPost, true).toList(mutableListOf())

        // Then
        Assertions.assertThat(result[0]).isEqualTo(NoNetwork)
    }

    @Test
    fun `unchanged returned when already liked and requesting to like`() = test {
        // Given
        readerPost.isLikedByCurrentUser = true
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(
                readerPostActionsWrapper.performLikeActionLocal(
                        anyOrNull(),
                        anyBoolean(),
                        anyLong()
                )
        ).thenReturn(false)

        // When
        val result = useCase.perform(readerPost, true).toList(mutableListOf())

        // Then
        assertThat(result[0]).isEqualTo(Unchanged)
    }

    @Test
    fun `unchanged returned when already unliked and requesting to unlike`() = test {
        // Given
        readerPost.isLikedByCurrentUser = false
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(
                readerPostActionsWrapper.performLikeActionLocal(
                        anyOrNull(),
                        anyBoolean(),
                        anyLong()
                )
        ).thenReturn(false)

        // When
        val result = useCase.perform(readerPost, false).toList(mutableListOf())

        // Then
        assertThat(result[0]).isEqualTo(Unchanged)
    }

    @Test
    fun `success is returned when liking an unliked post`() =
            testWithUnlikedPost {
                // Act
                val result = useCase.perform(readerPost, true).toList(mutableListOf())

                // Assert
                assertThat((result)).contains(Success)
            }

    @Test
    fun `success is returned when unliking a like post`() =
            testWithLikedPost {
                // Act
                val result = useCase.perform(readerPost, false).toList(mutableListOf())

                // Assert
                assertThat((result)).contains(Success)
            }

    @Test
    fun `failure is returned when liking an unliked post`() =
            testWitFailurehUnlikedPost {
                // Act
                val result = useCase.perform(readerPost, true).toList(mutableListOf())

                // Assert
                assertThat((result)).contains(Failed.RequestFailed)
            }

    @Test
    fun `failure is returned when unliking a like post`() =
            testWithFailureLikedPost {
                // Act
                val result = useCase.perform(readerPost, false).toList(mutableListOf())

                // Assert
                assertThat((result)).contains(Failed.RequestFailed)
            }

    @Test
    fun `like local action is triggered for selected reader post`() =
            testWithUnlikedPost {
                // Act
                useCase.perform(readerPost, true).toList(mutableListOf())

                // Assert
                verify(readerPostActionsWrapper).performLikeActionLocal(
                        anyOrNull(),
                        anyBoolean(),
                        anyLong()
                )
            }

    @Test
    fun `like remote action is triggered for selected reader post`() =
            testWithUnlikedPost {
                // Act
                useCase.perform(readerPost, true).toList(mutableListOf())

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
            testWithUnlikedPost {
                // Act
                useCase.perform(readerPost, true).toList(mutableListOf())

                // Assert
                verify(readerPostActionsWrapper).bumpPageViewForPost(anyOrNull())
            }

    @Test
    fun `Post views NOT bumped when asking to unlike`() =
            testWithLikedPost {
                // Act
                useCase.perform(readerPost, false).toList(mutableListOf())

                // Assert
                verify(readerPostActionsWrapper, never()).bumpPageViewForPost(anyOrNull())
            }

    private fun <T> testWithUnlikedPost(block: suspend CoroutineScope.() -> T) {
        test {
            readerPost.isLikedByCurrentUser = false
            whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
            whenever(
                    readerPostActionsWrapper.performLikeActionLocal(
                            anyOrNull(),
                            anyBoolean(),
                            anyLong()
                    )
            ).thenReturn(true)
            whenever(
                    readerPostActionsWrapper.performLikeActionRemote(
                            anyOrNull(),
                            anyBoolean(),
                            anyLong(),
                            anyOrNull()
                    )
            )
                    .then {
                        (it.arguments[3] as ActionListener).onActionResult(true)
                    }

            block()
        }
    }

    private fun <T> testWithLikedPost(block: suspend CoroutineScope.() -> T) {
        test {
            readerPost.isLikedByCurrentUser = true
            whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
            whenever(
                    readerPostActionsWrapper.performLikeActionLocal(
                            anyOrNull(),
                            anyBoolean(),
                            anyLong()
                    )
            ).thenReturn(true)
            whenever(
                    readerPostActionsWrapper.performLikeActionRemote(
                            anyOrNull(),
                            anyBoolean(),
                            anyLong(),
                            anyOrNull()
                    )
            )
                    .then {
                        (it.arguments[3] as ActionListener).onActionResult(true)
                    }

            block()
        }
    }

    private fun <T> testWitFailurehUnlikedPost(block: suspend CoroutineScope.() -> T) {
        test {
            readerPost.isLikedByCurrentUser = false
            whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
            whenever(
                    readerPostActionsWrapper.performLikeActionLocal(
                            anyOrNull(),
                            anyBoolean(),
                            anyLong()
                    )
            ).thenReturn(true)
            whenever(
                    readerPostActionsWrapper.performLikeActionRemote(
                            anyOrNull(),
                            anyBoolean(),
                            anyLong(),
                            anyOrNull()
                    )
            )
                    .then {
                        (it.arguments[3] as ActionListener).onActionResult(false)
                    }

            block()
        }
    }

    private fun <T> testWithFailureLikedPost(block: suspend CoroutineScope.() -> T) {
        test {
            readerPost.isLikedByCurrentUser = true
            whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
            whenever(
                    readerPostActionsWrapper.performLikeActionLocal(
                            anyOrNull(),
                            anyBoolean(),
                            anyLong()
                    )
            ).thenReturn(true)
            whenever(
                    readerPostActionsWrapper.performLikeActionRemote(
                            anyOrNull(),
                            anyBoolean(),
                            anyLong(),
                            anyOrNull()
                    )
            )
                    .then {
                        (it.arguments[3] as ActionListener).onActionResult(false)
                    }

            block()
        }
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
