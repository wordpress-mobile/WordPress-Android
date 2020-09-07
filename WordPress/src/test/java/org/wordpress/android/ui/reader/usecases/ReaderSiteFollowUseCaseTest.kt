package org.wordpress.android.ui.reader.usecases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.test
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener
import org.wordpress.android.ui.reader.actions.ReaderBlogActionsWrapper
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.Failed.NoNetwork
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.Failed.RequestFailed
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.PostFollowStatusChanged
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.Success
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderSiteFollowUseCaseTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    lateinit var useCase: ReaderSiteFollowUseCase
    @Mock lateinit var readerPostTableWrapper: ReaderPostTableWrapper
    @Mock lateinit var readerUtilsWrapper: ReaderUtilsWrapper
    @Mock lateinit var readerBlogActionsWrapper: ReaderBlogActionsWrapper
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    private val readerPost = createDummyReaderPost(1L)

    @Before
    fun setup() {
        useCase = ReaderSiteFollowUseCase(
                networkUtilsWrapper,
                readerBlogActionsWrapper,
                readerPostTableWrapper,
                readerUtilsWrapper
        )

        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(readerUtilsWrapper.isExternalFeed(anyLong(), anyLong())).thenReturn(false)
    }

    @Test
    fun `NoNetwork returned when no network found`() = test {
        // Given
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        // Act
        val result = useCase.toggleFollow(readerPost).toList(mutableListOf())

        // Then
        assertThat(result).contains(NoNetwork)
    }

    @Test
    fun `Success returned on success response`() =
            testWithFollowedSitePost {
                // Act
                val result = useCase.toggleFollow(readerPost).toList(mutableListOf())

                // Assert
                assertThat((result)).contains(Success)
            }

    @Test
    fun `RequestFailed returned on failed response`() =
            testWithFailedResponseForFollowedSitePost {
                // Act
                val result = useCase.toggleFollow(readerPost).toList(mutableListOf())

                // Assert
                assertThat((result)).contains(RequestFailed)
            }

    @Test
    fun `follow site action returns info to show enable notification`() =
            testWithUnFollowedSitePost {
                // Act
                val result = useCase.toggleFollow(readerPost).toList(mutableListOf())

                // Assert
                assertThat((result[0] as PostFollowStatusChanged).showEnableNotification).isTrue()
            }

    @Test
    fun `un-follow site action returns info to not show enable notification`() =
            testWithFollowedSitePost {
                // Act
                val result = useCase.toggleFollow(readerPost).toList(mutableListOf())

                // Assert
                assertThat((result[0] as PostFollowStatusChanged).showEnableNotification).isFalse()
            }

    @Test
    fun `follow site action returns info that site is followed`() =
            testWithUnFollowedSitePost {
                // Act
                val result = useCase.toggleFollow(readerPost).toList(mutableListOf())

                // Assert
                assertThat((result[0] as PostFollowStatusChanged).following).isTrue()
            }

    @Test
    fun `un-follow site action returns info that site is un-followed`() =
            testWithFollowedSitePost {
                // Act
                val result = useCase.toggleFollow(readerPost).toList(mutableListOf())

                // Assert
                assertThat((result[0] as PostFollowStatusChanged).following).isFalse()
            }

    @Test
    fun `follow site action returns info to not delete notification subscriptions`() =
            testWithUnFollowedSitePost {
                // Act
                val result = useCase.toggleFollow(readerPost).toList(mutableListOf())

                // Assert
                assertThat(result.size).isGreaterThanOrEqualTo(2)
                assertThat((result[1] as PostFollowStatusChanged).deleteNotificationSubscription).isFalse()
            }

    @Test
    fun `un-follow site action returns info to delete notification subscriptions`() =
            testWithFollowedSitePost {
                // Act
                val result = useCase.toggleFollow(readerPost).toList(mutableListOf())

                // Assert
                assertThat(result.size).isGreaterThanOrEqualTo(2)
                assertThat((result[1] as PostFollowStatusChanged).deleteNotificationSubscription).isTrue()
            }

    @Test
    fun `request failure on un-follow site returns info that site is followed`() =
            testWithFailedResponseForFollowedSitePost {
                // Act
                val result = useCase.toggleFollow(readerPost).toList(mutableListOf())

                // Assert
                assertThat(result.size).isGreaterThanOrEqualTo(2)
                assertThat((result[1] as PostFollowStatusChanged).following).isTrue()
            }

    @Test
    fun `request failure on follow site returns info that site is un-followed`() =
            testWithFailedResponseForFollowedSitePost {
                // Act
                val result = useCase.toggleFollow(readerPost).toList(mutableListOf())

                // Assert
                assertThat(result.size).isGreaterThanOrEqualTo(2)
                assertThat((result[1] as PostFollowStatusChanged).following).isTrue()
            }

    @Test
    fun `follow external feed post action returns info to not show enable notification`() =
            testWithUnFollowedSitePost {
                // Given
                whenever(readerUtilsWrapper.isExternalFeed(anyLong(), anyLong())).thenReturn(true)

                // Act
                val result = useCase.toggleFollow(readerPost).toList(mutableListOf())

                // Assert
                assertThat((result[0] as PostFollowStatusChanged).showEnableNotification).isFalse()
            }

    @Test
    fun `un-follow external feed post action returns info to not show enable notification`() =
            testWithFollowedSitePost {
                // Given
                whenever(readerUtilsWrapper.isExternalFeed(anyLong(), anyLong())).thenReturn(true)

                // Act
                val result = useCase.toggleFollow(readerPost).toList(mutableListOf())

                // Assert
                assertThat((result[0] as PostFollowStatusChanged).showEnableNotification).isFalse()
            }

    private fun createDummyReaderPost(id: Long): ReaderPost = ReaderPost().apply {
        this.postId = id
        this.blogId = id
        this.title = "DummyPost"
    }

    private fun <T> testWithFollowedSitePost(block: suspend CoroutineScope.() -> T) {
        test {
            readerPost.isFollowedByCurrentUser = true
            whenever(readerPostTableWrapper.isPostFollowed(readerPost)).thenReturn(true)
            whenever(readerBlogActionsWrapper.followBlogForPost(anyOrNull(), anyBoolean(), anyOrNull())).then {
                (it.arguments[2] as ActionListener).onActionResult(true)
                true
            }

            block()
        }
    }

    private fun <T> testWithUnFollowedSitePost(block: suspend CoroutineScope.() -> T) {
        test {
            readerPost.isFollowedByCurrentUser = false
            whenever(readerPostTableWrapper.isPostFollowed(readerPost)).thenReturn(false)
            whenever(readerBlogActionsWrapper.followBlogForPost(anyOrNull(), anyBoolean(), anyOrNull())).then {
                (it.arguments[2] as ActionListener).onActionResult(true)
                true
            }

            block()
        }
    }

    private fun <T> testWithFailedResponseForFollowedSitePost(block: suspend CoroutineScope.() -> T) {
        test {
            readerPost.isFollowedByCurrentUser = true
            whenever(readerPostTableWrapper.isPostFollowed(readerPost)).thenReturn(true)
            whenever(readerBlogActionsWrapper.followBlogForPost(anyOrNull(), anyBoolean(), anyOrNull())).then {
                (it.arguments[2] as ActionListener).onActionResult(false)
                false
            }

            block()
        }
    }

    private fun <T> testWithFailedResponseForUnFollowedSitePost(block: suspend CoroutineScope.() -> T) {
        test {
            readerPost.isFollowedByCurrentUser = false
            whenever(readerPostTableWrapper.isPostFollowed(readerPost)).thenReturn(false)
            whenever(readerBlogActionsWrapper.followBlogForPost(anyOrNull(), anyBoolean(), anyOrNull())).then {
                (it.arguments[2] as ActionListener).onActionResult(false)
                false
            }

            block()
        }
    }
}
