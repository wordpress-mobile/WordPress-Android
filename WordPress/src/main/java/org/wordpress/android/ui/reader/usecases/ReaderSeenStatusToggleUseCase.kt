package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.flow.flow
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase.PostSeenState.Failure
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase.PostSeenState.PostSeenStateChanged
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase.PostSeenState.UserNotAuthenticated
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase.ReaderPostSeenToggleSource.READER_POST_CARD
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase.ReaderPostSeenToggleSource.READER_POST_DETAILS
import org.wordpress.android.ui.reader.utils.PostSeenStatusApiCallsProvider
import org.wordpress.android.ui.reader.utils.PostSeenStatusApiCallsProvider.SeenStatusToggleCallResult
import org.wordpress.android.ui.reader.utils.PostSeenStatusApiCallsProvider.SeenStatusToggleCallResult.Success
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import javax.inject.Inject

/**
 * This class handles reader blog follow click events.
 */
class ReaderSeenStatusToggleUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val apiCallsProvider: PostSeenStatusApiCallsProvider,
    private val accountStore: AccountStore,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
    private val readerPostTableWrapper: ReaderPostTableWrapper
) {
    companion object {
        const val ACTION_SOURCE_PARAM_NAME = "source"
    }

    /**
     * Convenience method for toggling seen status based on the current state in local DB
     */
    suspend fun toggleSeenStatus(post: ReaderPost) = flow {
        val isAskingToMarkAsSeen = !readerPostTableWrapper.isPostSeen(post)
        val status = if (isAskingToMarkAsSeen) {
            analyticsUtilsWrapper.trackWithReaderPostDetails(
                    AnalyticsTracker.Stat.READER_POST_MARKED_AS_SEEN,
                    post,
                    mapOf(ACTION_SOURCE_PARAM_NAME to READER_POST_CARD.toString())
            )
            markPostAsSeen(post)
        } else {
            analyticsUtilsWrapper.trackWithReaderPostDetails(
                    AnalyticsTracker.Stat.READER_POST_MARKED_AS_UNSEEN,
                    post,
                    mapOf(ACTION_SOURCE_PARAM_NAME to READER_POST_CARD.toString())
            )
            markPostAsUnseen(post)
        }

        emit(status)
    }

    suspend fun markPostAsSeenIfNecessary(post: ReaderPost) = flow {
        if (!readerPostTableWrapper.isPostSeen(post)) {
            val status = markPostAsSeen(post)
            emit(status)
            analyticsUtilsWrapper.trackWithReaderPostDetails(
                    AnalyticsTracker.Stat.READER_POST_MARKED_AS_UNSEEN,
                    post,
                    mapOf(ACTION_SOURCE_PARAM_NAME to READER_POST_DETAILS.toString())
            )
        }
    }

    private suspend fun markPostAsSeen(post: ReaderPost): PostSeenState {
        if (!accountStore.hasAccessToken()) {
            return UserNotAuthenticated
        } else {
            return if (!networkUtilsWrapper.isNetworkAvailable()) {
                Failure(UiStringRes(string.error_network_connection))
            } else {
                when (val status = apiCallsProvider.markPostAsSeen(post)) {
                    is Success -> {
                        readerPostTableWrapper.togglePostSeenStatusLocally(post, true)
                        PostSeenStateChanged(true, UiStringRes(string.reader_marked_post_as_seen))
                    }
                    is SeenStatusToggleCallResult.Failure -> {
                        Failure(UiStringText(status.error))
                    }
                }
            }
        }
    }

    private suspend fun markPostAsUnseen(post: ReaderPost): PostSeenState {
        if (!accountStore.hasAccessToken()) {
            return UserNotAuthenticated
        } else {
            return if (!networkUtilsWrapper.isNetworkAvailable()) {
                Failure(UiStringRes(string.error_network_connection))
            } else {
                when (val status = apiCallsProvider.markPostAsUnseen(post)) {
                    is Success -> {
                        readerPostTableWrapper.togglePostSeenStatusLocally(post, false)
                        PostSeenStateChanged(false, UiStringRes(string.reader_marked_post_as_unseen))
                    }
                    is SeenStatusToggleCallResult.Failure -> {
                        Failure(UiStringText(status.error))
                    }
                }
            }
        }
    }

    sealed class PostSeenState {
        data class PostSeenStateChanged(
            val isSeen: Boolean,
            val userMessage: UiString? = null
        ) : PostSeenState()

        data class Failure(
            val error: UiString
        ) : PostSeenState()

        object UserNotAuthenticated : PostSeenState()
    }

    enum class ReaderPostSeenToggleSource {
        READER_POST_CARD {
            override fun toString(): String {
                return "post_card"
            }
        },
        READER_POST_DETAILS {
            override fun toString(): String {
                return "post_details"
            }
        }
    }
}
