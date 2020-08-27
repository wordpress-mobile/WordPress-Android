package org.wordpress.android.ui.reader.repository.usecases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.test
import org.wordpress.android.ui.reader.actions.ReaderPostActionsWrapper
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.NetworkUnavailable
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.NetworkUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
class PostLikeUseCaseTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Mock private lateinit var accountStore: AccountStore
    @Mock private lateinit var eventBusWrapper: EventBusWrapper
    @Mock private lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock private lateinit var readerPostActionsWrapper: ReaderPostActionsWrapper

    private lateinit var useCase: PostLikeUseCase

    @Before
    fun setUp() {
        useCase = PostLikeUseCase(
                eventBusWrapper,
                readerPostActionsWrapper,
                accountStore,
                networkUtilsWrapper
        )
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        val account = AccountModel()
        account.userId = 100
        whenever(accountStore.account).thenReturn(account)
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

    private fun createDummyReaderPost(
        id: Long = 1,
        isLikedByCurrentUser: Boolean = false
    ): ReaderPost =
            ReaderPost().apply {
                this.postId = id
                this.blogId = id
                this.isLikedByCurrentUser = isLikedByCurrentUser
            }
}
