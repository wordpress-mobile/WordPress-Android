package org.wordpress.android.ui.reader.repository.usecases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.test
import org.wordpress.android.ui.reader.actions.ReaderPostActionsWrapper
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.NetworkUnavailable
import org.wordpress.android.ui.reader.repository.ReaderRepositoryEvent.PostLikeEnded.PostLikeSuccess
import org.wordpress.android.util.EventBusWrapper
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
    @Mock private lateinit var eventBusWrapper: EventBusWrapper
    @Mock private lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock private lateinit var readerPostActionsWrapper: ReaderPostActionsWrapper
    @Mock private lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper

    private lateinit var useCase: PostLikeUseCase

    @Before
    fun setUp() {
        useCase = PostLikeUseCase(
                eventBusWrapper,
                readerPostActionsWrapper,
                analyticsUtilsWrapper,
                accountStore,
                networkUtilsWrapper,
                TEST_DISPATCHER
        )
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        val account = AccountModel()
        account.userId = 100
        whenever(accountStore.account).thenReturn(account)
        whenever(readerPostActionsWrapper.performLikeAction(anyOrNull(), anyBoolean(), anyLong())).then {
            useCase.onPerformPostLikeEnded(
                    PostLikeSuccess(
                            POST_AND_BLOG_ID,
                            POST_AND_BLOG_ID,
                            it.arguments[1] as Boolean,
                            it.arguments[2] as Long
                    )
            )
            true
        }
    }

    @Test
    fun `NetworkUnavailable returned when no network found`() = test {
        // Given
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        // When
        val result = useCase.perform(createDummyReaderPost(), true)

        // Then
        Assertions.assertThat(result).isEqualTo(NetworkUnavailable)
    }

    @Test
    fun `Post views bumped when asking to like`() = test {
        // Arrange
        val isAskingToLike = true
        // Act
        useCase.perform(createDummyReaderPost(), isAskingToLike)
        // Assert
        verify(readerPostActionsWrapper).bumpPageViewForPost(anyOrNull())
    }

    @Test
    fun `Post views NOT bumped when asking to unlike`() = test {
        // Arrange
        val isAskingToLike = false
        // Act
        useCase.perform(createDummyReaderPost(), isAskingToLike)
        // Assert
        verify(readerPostActionsWrapper, never()).bumpPageViewForPost(anyOrNull())
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
