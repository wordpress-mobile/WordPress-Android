package org.wordpress.android.ui.reader.repository.usecases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
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
import org.wordpress.android.ui.reader.actions.ReaderPostActionsWrapper
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

    private lateinit var useCase: PostLikeUseCase

    @Before
    fun setUp() {
        useCase = PostLikeUseCase(
                readerPostActionsWrapper,
                analyticsUtilsWrapper,
                accountStore,
                networkUtilsWrapper,
                TEST_DISPATCHER
        )
//        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
//        val account = AccountModel()
//        account.userId = 100
//        whenever(accountStore.account).thenReturn(account)
//        whenever(readerPostActionsWrapper.performLikeActionLocal(anyOrNull(), anyBoolean(), anyLong())).thenReturn(true)
//        whenever(readerPostActionsWrapper.performLikeActionRemote(anyOrNull(), anyBoolean(), anyLong(), any()))
//                .thenReturn(any())
    }

    @Test
    fun foo() {
    }

//    @Test
//    fun `NoNetwork returned when no network found`() = test {
//        // Given
//        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
//
//        // When
//        val result = useCase.perform(createDummyReaderPost(), true)
//
//        // Then
//        Assertions.assertThat(result).isEqualTo(NoNetwork(anyOrNull()))
//    }
//
//    @Test
//    fun `Post views bumped when asking to like`() = test {
//        // Arrange
//        val isAskingToLike = true
//        // Act
//        useCase.perform(createDummyReaderPost(), isAskingToLike)
//        // Assert
//        verify(readerPostActionsWrapper).bumpPageViewForPost(anyOrNull())
//    }
//
//    @Test
//    fun `Post views NOT bumped when asking to unlike`() = test {
//        // Arrange
//        val isAskingToLike = false
//        // Act
//        useCase.perform(createDummyReaderPost(), isAskingToLike)
//        // Assert
//        verify(readerPostActionsWrapper, never()).bumpPageViewForPost(anyOrNull())
//    }
//
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
