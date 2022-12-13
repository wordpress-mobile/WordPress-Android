package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener
import org.wordpress.android.ui.reader.actions.ReaderBlogActionsWrapper
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.Failed.NoNetwork
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.Failed.RequestFailed
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.FollowStatusChanged
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.Success
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper

private const val FOLLOW_BLOG_ACTION_LISTENER_PARAM_POSITION = 3
private const val SOURCE = "source"

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderSiteFollowUseCaseTest : BaseUnitTest() {
    lateinit var useCase: ReaderSiteFollowUseCase
    @Mock lateinit var readerBlogTableWrapper: ReaderBlogTableWrapper
    @Mock lateinit var readerUtilsWrapper: ReaderUtilsWrapper
    @Mock lateinit var readerBlogActionsWrapper: ReaderBlogActionsWrapper
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var readerTracker: ReaderTracker

    private val useCaseParam = ReaderSiteFollowUseCase.Param(11L, 13L, "FakeBlogName")

    @Before
    fun setup() {
        useCase = ReaderSiteFollowUseCase(
                networkUtilsWrapper,
                readerBlogActionsWrapper,
                readerBlogTableWrapper,
                readerUtilsWrapper,
                readerTracker
        )

        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(readerUtilsWrapper.isExternalFeed(anyLong(), anyLong())).thenReturn(false)
    }

    @Test
    fun `NoNetwork returned when no network found`() = test {
        // Given
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        // Act
        val result = useCase.toggleFollow(useCaseParam, SOURCE).toList(mutableListOf())

        // Then
        assertThat(result).contains(NoNetwork)
    }

    @Test
    fun `Success returned on success response`() =
            testWithFollowedSitePost {
                // Act
                val result = useCase.toggleFollow(useCaseParam, SOURCE)
                        .toList(mutableListOf())

                // Assert
                assertThat((result)).contains(Success)
            }

    @Test
    fun `RequestFailed returned on failed response`() =
            testWithFailedResponseForFollowedSitePost {
                // Act
                val result = useCase.toggleFollow(useCaseParam, SOURCE)
                        .toList(mutableListOf())

                // Assert
                assertThat((result)).contains(RequestFailed)
            }

    @Test
    fun `follow site action returns info to show enable notification`() =
            testWithUnFollowedSitePost {
                // Act
                val result = useCase.toggleFollow(useCaseParam, SOURCE)
                        .toList(mutableListOf())

                // Assert
                assertThat((result[0] as FollowStatusChanged).showEnableNotification).isTrue
            }

    @Test
    fun `un-follow site action returns info to not show enable notification`() =
            testWithFollowedSitePost {
                // Act
                val result = useCase.toggleFollow(useCaseParam, SOURCE)
                        .toList(mutableListOf())

                // Assert
                assertThat((result[0] as FollowStatusChanged).showEnableNotification).isFalse
            }

    @Test
    fun `follow site action returns info that site is followed`() =
            testWithUnFollowedSitePost {
                // Act
                val result = useCase.toggleFollow(useCaseParam, SOURCE)
                        .toList(mutableListOf())

                // Assert
                assertThat((result[0] as FollowStatusChanged).following).isTrue
            }

    @Test
    fun `un-follow site action returns info that site is un-followed`() =
            testWithFollowedSitePost {
                // Act
                val result = useCase.toggleFollow(useCaseParam, SOURCE)
                        .toList(mutableListOf())

                // Assert
                assertThat((result[0] as FollowStatusChanged).following).isFalse
            }

    @Test
    fun `follow site action returns info to not delete notification subscriptions`() =
            testWithUnFollowedSitePost {
                // Act
                val result = useCase.toggleFollow(useCaseParam, SOURCE)
                        .toList(mutableListOf())

                // Assert
                assertThat(result.size).isGreaterThanOrEqualTo(2)
                assertThat((result[1] as FollowStatusChanged).deleteNotificationSubscription).isFalse
            }

    @Test
    fun `un-follow site action returns info to delete notification subscriptions`() =
            testWithFollowedSitePost {
                // Act
                val result = useCase.toggleFollow(useCaseParam, SOURCE)
                        .toList(mutableListOf())

                // Assert
                assertThat(result.size).isGreaterThanOrEqualTo(2)
                assertThat((result[1] as FollowStatusChanged).deleteNotificationSubscription).isTrue
            }

    @Test
    fun `request failure on un-follow site action returns info that site is followed`() =
            testWithFailedResponseForFollowedSitePost {
                // Act
                val result = useCase.toggleFollow(useCaseParam, SOURCE)
                        .toList(mutableListOf())

                // Assert
                assertThat(result.size).isGreaterThanOrEqualTo(2)
                assertThat((result[1] as FollowStatusChanged).following).isTrue
            }

    @Test
    fun `request failure on follow site action returns info that site is not followed`() =
            testWithFailedResponseForUnFollowedSitePost {
                // Act
                val result = useCase.toggleFollow(useCaseParam, SOURCE)
                        .toList(mutableListOf())

                // Assert
                assertThat(result.size).isGreaterThanOrEqualTo(2)
                assertThat((result[1] as FollowStatusChanged).following).isFalse
            }

    @Test
    fun `follow external feed post action returns info to not show enable notification`() =
            testWithUnFollowedSitePost {
                // Given
                whenever(readerUtilsWrapper.isExternalFeed(anyLong(), anyLong())).thenReturn(true)

                // Act
                val result = useCase.toggleFollow(useCaseParam, SOURCE)
                        .toList(mutableListOf())

                // Assert
                assertThat((result[0] as FollowStatusChanged).showEnableNotification).isFalse
            }

    @Test
    fun `un-follow external feed post action returns info to not show enable notification`() =
            testWithFollowedSitePost {
                // Given
                whenever(readerUtilsWrapper.isExternalFeed(anyLong(), anyLong())).thenReturn(true)

                // Act
                val result = useCase.toggleFollow(useCaseParam, SOURCE)
                        .toList(mutableListOf())

                // Assert
                assertThat((result[0] as FollowStatusChanged).showEnableNotification).isFalse
            }

    @Test
    fun `toggling follow for un-followed site post initiates follow blog (or site) action`() =
            testWithUnFollowedSitePost {
                // Act
                useCase.toggleFollow(useCaseParam, SOURCE).toList(mutableListOf())

                // Assert
                verify(readerBlogActionsWrapper).followBlog(
                        anyLong(),
                        anyLong(),
                        eq(true),
                        anyOrNull(),
                        eq(SOURCE),
                        eq(readerTracker)
                )
            }

    @Test
    fun `toggling follow for followed site post initiates un-follow blog (or site) action`() =
            testWithFollowedSitePost {
                // Act
                useCase.toggleFollow(useCaseParam, SOURCE).toList(mutableListOf())

                // Assert
                verify(readerBlogActionsWrapper).followBlog(
                        anyLong(),
                        anyLong(),
                        eq(false),
                        anyOrNull(),
                        eq(SOURCE),
                        eq(readerTracker)
                )
            }

    @Test
    fun `follow blog (or site) action is triggered for selected reader post`() =
            testWithFollowedSitePost {
                // Act
                useCase.toggleFollow(useCaseParam, SOURCE).toList(mutableListOf())

                // Assert
                verify(readerBlogActionsWrapper).followBlog(
                        eq(useCaseParam.blogId),
                        eq(useCaseParam.feedId),
                        anyBoolean(),
                        anyOrNull(),
                        eq(SOURCE),
                        eq(readerTracker)
                )
            }

    private fun <T> testWithFollowedSitePost(block: suspend CoroutineScope.() -> T) {
        test {
            whenever(readerBlogTableWrapper.isSiteFollowed(useCaseParam.blogId, useCaseParam.feedId)).thenReturn(true)
            whenever(
                    readerBlogActionsWrapper.followBlog(
                            anyLong(),
                            anyLong(),
                            anyBoolean(),
                            anyOrNull(),
                            anyString(),
                            eq(readerTracker)
                    )
            ).then {
                (it.arguments[FOLLOW_BLOG_ACTION_LISTENER_PARAM_POSITION] as ActionListener).onActionResult(true)
                true
            }

            block()
        }
    }

    private fun <T> testWithUnFollowedSitePost(block: suspend CoroutineScope.() -> T) {
        test {
            whenever(readerBlogTableWrapper.isSiteFollowed(useCaseParam.blogId, useCaseParam.feedId)).thenReturn(false)
            whenever(
                    readerBlogActionsWrapper.followBlog(
                            anyLong(),
                            anyLong(),
                            anyBoolean(),
                            anyOrNull(),
                            anyString(),
                            eq(readerTracker)
                    )
            ).then {
                (it.arguments[FOLLOW_BLOG_ACTION_LISTENER_PARAM_POSITION] as ActionListener).onActionResult(true)
                true
            }

            block()
        }
    }

    private fun <T> testWithFailedResponseForFollowedSitePost(block: suspend CoroutineScope.() -> T) {
        test {
            whenever(readerBlogTableWrapper.isSiteFollowed(useCaseParam.blogId, useCaseParam.feedId)).thenReturn(true)
            whenever(
                    readerBlogActionsWrapper.followBlog(
                            anyLong(),
                            anyLong(),
                            anyBoolean(),
                            anyOrNull(),
                            anyString(),
                            eq(readerTracker)
                    )
            ).then {
                (it.arguments[FOLLOW_BLOG_ACTION_LISTENER_PARAM_POSITION] as ActionListener).onActionResult(false)
                true
            }

            block()
        }
    }

    private fun <T> testWithFailedResponseForUnFollowedSitePost(block: suspend CoroutineScope.() -> T) {
        test {
            whenever(readerBlogTableWrapper.isSiteFollowed(useCaseParam.blogId, useCaseParam.feedId)).thenReturn(false)
            whenever(
                    readerBlogActionsWrapper.followBlog(
                            anyLong(),
                            anyLong(),
                            anyBoolean(),
                            anyOrNull(),
                            anyString(),
                            eq(readerTracker)
                    )
            ).then {
                (it.arguments[FOLLOW_BLOG_ACTION_LISTENER_PARAM_POSITION] as ActionListener).onActionResult(false)
                true
            }

            block()
        }
    }
}
