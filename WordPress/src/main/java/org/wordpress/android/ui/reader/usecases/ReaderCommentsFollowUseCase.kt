package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.reader.FollowConversationStatusFlags
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.AnalyticsFollowCommentsAction.DISABLE_PUSH_NOTIFICATION
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.AnalyticsFollowCommentsAction.ENABLE_PUSH_NOTIFICATION
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.AnalyticsFollowCommentsAction.FOLLOW_COMMENTS
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.AnalyticsFollowCommentsAction.UNFOLLOW_COMMENTS
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.AnalyticsFollowCommentsActionResult.ERROR
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.AnalyticsFollowCommentsActionResult.SUCCEEDED
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.AnalyticsFollowCommentsGenericError.NO_NETWORK
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState.UserNotAuthenticated
import org.wordpress.android.ui.reader.utils.PostSubscribersApiCallsProvider
import org.wordpress.android.ui.reader.utils.PostSubscribersApiCallsProvider.PostSubscribersCallResult.Failure
import org.wordpress.android.ui.reader.utils.PostSubscribersApiCallsProvider.PostSubscribersCallResult.Success
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

class ReaderCommentsFollowUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val postSubscribersApiCallsProvider: PostSubscribersApiCallsProvider,
    private val accountStore: AccountStore,
    private val readerTracker: ReaderTracker,
    private val readerPostTableWrapper: ReaderPostTableWrapper
) {
    suspend fun getMySubscriptionToPost(blogId: Long, postId: Long, isInit: Boolean) = flow {
        if (!accountStore.hasAccessToken()) {
            emit(UserNotAuthenticated)
        } else {
            emit(FollowCommentsState.Loading)

            if (!networkUtilsWrapper.isNetworkAvailable()) {
                emit(FollowCommentsState.Failure(blogId, postId, UiStringRes(R.string.error_network_connection)))
            } else {
                val canFollowComments = postSubscribersApiCallsProvider.getCanFollowComments(blogId)

                if (!canFollowComments) {
                    emit(FollowCommentsState.FollowCommentsNotAllowed)
                } else {
                    val status = postSubscribersApiCallsProvider.getMySubscriptionToPost(blogId, postId)

                    when (status) {
                        is Success -> {
                            emit(
                                FollowCommentsState.FollowStateChanged(
                                    blogId = blogId,
                                    postId = postId,
                                    isFollowing = status.isFollowing,
                                    isReceivingNotifications = status.isReceivingNotifications,
                                    isInit = isInit
                                )
                            )
                        }
                        is Failure -> {
                            emit(FollowCommentsState.Failure(blogId, postId, UiStringText(status.error)))
                        }
                    }
                }
            }
        }
    }

    suspend fun setMySubscriptionToPost(
        blogId: Long,
        postId: Long,
        subscribe: Boolean,
        source: ThreadedCommentsActionSource
    ): Flow<FollowCommentsState> = flow {
        val properties = mutableMapOf<String, Any?>()

        properties.addFollowAction(subscribe)
        properties.addFollowActionSource(source)

        emit(FollowCommentsState.Loading)

        if (!networkUtilsWrapper.isNetworkAvailable()) {
            emit(FollowCommentsState.Failure(blogId, postId, UiStringRes(R.string.error_network_connection)))
            properties.addFollowActionResult(ERROR, NO_NETWORK.errorMessage)
        } else {
            val status = if (subscribe) {
                postSubscribersApiCallsProvider.subscribeMeToPost(blogId, postId)
            } else {
                postSubscribersApiCallsProvider.unsubscribeMeFromPost(blogId, postId)
            }

            when (status) {
                is Success -> {
                    emit(
                        FollowCommentsState.FollowStateChanged(
                            blogId = blogId,
                            postId = postId,
                            isFollowing = status.isFollowing,
                            isReceivingNotifications = status.isReceivingNotifications,
                            false,
                            userMessage = UiStringRes(
                                if (status.isFollowing) {
                                    R.string.reader_follow_comments_subscribe_success_enable_push
                                } else {
                                    R.string.reader_follow_comments_unsubscribe_from_all_success
                                }
                            )
                        )
                    )
                    properties.addFollowActionResult(SUCCEEDED)
                }
                is Failure -> {
                    emit(FollowCommentsState.Failure(blogId, postId, UiStringText(status.error)))
                    properties.addFollowActionResult(ERROR, status.error)
                }
            }
        }

        val post = readerPostTableWrapper.getBlogPost(blogId, postId, true)

        readerTracker.trackPostComments(
            Stat.COMMENT_FOLLOW_CONVERSATION,
            blogId,
            postId,
            post,
            properties
        )
    }

    @Suppress("LongMethod")
    suspend fun setEnableByPushNotifications(
        blogId: Long,
        postId: Long,
        enable: Boolean,
        source: ThreadedCommentsActionSource
    ): Flow<FollowCommentsState> = flow {
        val properties = mutableMapOf<String, Any?>()
        properties.addEnablePushNotificationAction(enable)
        properties.addFollowActionSource(source)

        if (!networkUtilsWrapper.isNetworkAvailable()) {
            emit(
                FollowCommentsState.FollowStateChanged(
                    blogId = blogId, postId = postId,
                    isFollowing = true,
                    isReceivingNotifications = !enable,
                    false,
                    userMessage = UiStringRes(R.string.error_network_connection),
                    true
                )
            )
            properties.addFollowActionResult(ERROR, NO_NETWORK.errorMessage)
        } else {
            when (val status = postSubscribersApiCallsProvider.managePushNotificationsForPost(blogId, postId, enable)) {
                is Success -> {
                    emit(
                        FollowCommentsState.FollowStateChanged(
                            blogId = blogId,
                            postId = postId,
                            isFollowing = status.isFollowing,
                            isReceivingNotifications = status.isReceivingNotifications,
                            false,
                            userMessage = UiStringRes(
                                if (enable) {
                                    R.string.reader_follow_comments_subscribe_to_push_success
                                } else {
                                    R.string.reader_follow_comments_unsubscribe_from_push_success
                                }
                            ),
                            false
                        )
                    )
                    properties.addFollowActionResult(SUCCEEDED)
                }
                is Failure -> {
                    emit(
                        FollowCommentsState.FollowStateChanged(
                            blogId = blogId,
                            postId = postId,
                            isFollowing = true,
                            isReceivingNotifications = !enable,
                            false,
                            userMessage = UiStringRes(
                                if (enable) {
                                    R.string.reader_follow_comments_could_not_subscribe_to_push_error
                                } else {
                                    R.string.reader_follow_comments_could_not_unsubscribe_from_push_error
                                }
                            ),
                            true
                        )
                    )
                    properties.addFollowActionResult(ERROR, status.error)
                }
            }
        }
        readerTracker.trackPostComments(
            Stat.COMMENT_FOLLOW_CONVERSATION,
            blogId,
            postId,
            readerPostTableWrapper.getBlogPost(blogId, postId, true),
            properties
        )
    }

    sealed class FollowCommentsState(open val forcePushNotificationsUpdate: Boolean = false) {
        object Loading : FollowCommentsState()

        data class FollowStateChanged(
            val blogId: Long,
            val postId: Long,
            val isFollowing: Boolean,
            val isReceivingNotifications: Boolean,
            val isInit: Boolean = false,
            val userMessage: UiString? = null,
            override val forcePushNotificationsUpdate: Boolean = false
        ) : FollowCommentsState()

        data class Failure(
            val blogId: Long,
            val postId: Long,
            val error: UiString
        ) : FollowCommentsState()

        object FollowCommentsNotAllowed : FollowCommentsState()

        object UserNotAuthenticated : FollowCommentsState()

        data class FlagsMappedState(
            val flags: FollowConversationStatusFlags
        ) : FollowCommentsState()
    }

    private enum class AnalyticsFollowCommentsAction(val action: String) {
        FOLLOW_COMMENTS("followed"),
        UNFOLLOW_COMMENTS("unfollowed"),
        ENABLE_PUSH_NOTIFICATION("enable_push_notifications"),
        DISABLE_PUSH_NOTIFICATION("disable_push_notifications")
    }

    private enum class AnalyticsFollowCommentsActionResult(val actionResult: String) {
        SUCCEEDED("succeeded"),
        ERROR("error")
    }

    private enum class AnalyticsFollowCommentsGenericError(val errorMessage: String) {
        NO_NETWORK("no_network")
    }

    private fun MutableMap<String, Any?>.addFollowAction(subscribe: Boolean): MutableMap<String, Any?> {
        this[FOLLOW_COMMENT_ACTION] = if (subscribe) {
            FOLLOW_COMMENTS.action
        } else {
            UNFOLLOW_COMMENTS.action
        }
        return this
    }

    private fun MutableMap<String, Any?>.addEnablePushNotificationAction(enable: Boolean): MutableMap<String, Any?> {
        this[FOLLOW_COMMENT_ACTION] = if (enable) {
            ENABLE_PUSH_NOTIFICATION.action
        } else {
            DISABLE_PUSH_NOTIFICATION.action
        }
        return this
    }

    private fun MutableMap<String, Any?>.addFollowActionResult(
        result: AnalyticsFollowCommentsActionResult,
        errorMessage: String? = null
    ): MutableMap<String, Any?> {
        this[FOLLOW_COMMENT_ACTION_RESULT] = result.actionResult
        errorMessage?.also {
            this[FOLLOW_COMMENT_ACTION_ERROR] = errorMessage
        }
        return this
    }

    private fun MutableMap<String, Any?>.addFollowActionSource(
        source: ThreadedCommentsActionSource
    ): MutableMap<String, Any?> {
        this[FOLLOW_COMMENT_ACTION_SOURCE] = source.sourceDescription
        return this
    }

    companion object {
        private const val FOLLOW_COMMENT_ACTION = "follow_action"
        private const val FOLLOW_COMMENT_ACTION_RESULT = "follow_action_result"
        private const val FOLLOW_COMMENT_ACTION_ERROR = "follow_action_error"
        private const val FOLLOW_COMMENT_ACTION_SOURCE = "source"
    }
}
