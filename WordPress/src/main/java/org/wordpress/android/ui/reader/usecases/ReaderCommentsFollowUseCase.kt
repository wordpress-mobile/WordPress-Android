package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.R
import org.wordpress.android.ui.reader.utils.PostSubscribersApiCallsProvider
import org.wordpress.android.ui.reader.utils.PostSubscribersApiCallsProvider.PostSubscribersCallResult
import org.wordpress.android.ui.reader.utils.PostSubscribersApiCallsProvider.PostSubscribersCallResult.Failure
import org.wordpress.android.ui.reader.utils.PostSubscribersApiCallsProvider.PostSubscribersCallResult.Success
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

class ReaderCommentsFollowUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val postSubscribersApiCallsProvider: PostSubscribersApiCallsProvider,
) {
    suspend fun getMySubscriptionToPost(blogId: Long, postId: Long, isInit: Boolean) = flow {
        emit(FollowCommentsState.Loading)

        if (!networkUtilsWrapper.isNetworkAvailable()) {
            emit(FollowCommentsState.NoNetwork(true, UiStringRes(R.string.error_network_connection)))
        } else {
            val canFollowComments: Boolean = suspendCoroutine { continuation ->
                postSubscribersApiCallsProvider.getCanFollowComments(blogId, continuation)
            }

            if (!(canFollowComments)) {
                emit(FollowCommentsState.FollowCommentsNotAllowed)
            } else {
                val status: PostSubscribersCallResult = suspendCoroutine { continuation ->
                    postSubscribersApiCallsProvider.getMySubscriptionToPost(blogId, postId, continuation)
                }

                when(status) {
                    is Success -> {
                        emit(
                                FollowCommentsState.FollowStateChanged(
                                        blogId,
                                        postId,
                                        status.isFollowing,
                                        isInit
                                )
                        )
                    }
                    is Failure -> {
                        emit(FollowCommentsState.Failure(blogId, postId, true, UiStringText(status.error)))
                    }
                }
            }
        }
    }

    suspend fun setMySubscriptionToPost(blogId: Long, postId: Long, subscribe: Boolean) : Flow<FollowCommentsState> = flow {
        emit(FollowCommentsState.Loading)

        if (!networkUtilsWrapper.isNetworkAvailable()) {
            emit(FollowCommentsState.NoNetwork(false, UiStringRes(R.string.error_network_connection)))
        } else {
            val status: PostSubscribersCallResult = suspendCoroutine { continuation ->
                if (subscribe) {
                    postSubscribersApiCallsProvider.subscribeMeToPost(blogId, postId, continuation)
                } else {
                    postSubscribersApiCallsProvider.unsubscribeMeFromPost(blogId, postId, continuation)
                }
            }

            when(status) {
                is Success -> {
                    emit(
                            FollowCommentsState.FollowStateChanged(
                                    blogId,
                                    postId,
                                    status.isFollowing,
                                    false,
                                    UiStringRes(
                                        if (status.isFollowing)
                                            R.string.reader_follow_comments_subscribe_success
                                        else
                                            R.string.reader_follow_comments_unsubscribe_success
                                    )
                            )
                    )
                }
                is Failure -> {
                    emit(FollowCommentsState.Failure(blogId, postId, false, UiStringText(status.error)))
                }
            }
        }
    }

    sealed class FollowCommentsState {
        object Loading : FollowCommentsState()

        data class FollowStateChanged(
            val blogId: Long,
            val postId: Long,
            val isFollowing: Boolean,
            val isInit: Boolean = false,
            val userMessage: UiString? = null
        ) : FollowCommentsState()

        data class NoNetwork(
            val isGetStatus: Boolean,
            val error: UiString
        ) : FollowCommentsState()

        data class Failure(
            val blogId: Long,
            val postId: Long,
            val isGetStatus: Boolean,
            val error: UiString
        ) : FollowCommentsState()

        object FollowCommentsNotAllowed : FollowCommentsState()
    }
}
