package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.flow.flow
import org.wordpress.android.R.string
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase.PostSeenState.Failure
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase.PostSeenState.PostSeenStateChanged
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase.PostSeenState.UserNotAuthenticated
import org.wordpress.android.ui.reader.utils.PostSeenStatusApiCallsProvider
import org.wordpress.android.ui.reader.utils.PostSeenStatusApiCallsProvider.SeenStatusToggleCallResult
import org.wordpress.android.ui.reader.utils.PostSeenStatusApiCallsProvider.SeenStatusToggleCallResult.Success
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

/**
 * This class handles reader blog follow click events.
 */
class ReaderSeenStatusToggleUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val apiCallsProvider: PostSeenStatusApiCallsProvider,
    private val accountStore: AccountStore,
    private val readerPostTableWrapper: ReaderPostTableWrapper
) {
    suspend fun toggleSeenStatus(post: ReaderPost) = flow {
        if (!accountStore.hasAccessToken()) {
            emit(UserNotAuthenticated)
        } else {
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                emit(Failure(UiStringRes(string.error_network_connection)))
            } else {
                val isAskingToMarkAsSeen = !readerPostTableWrapper.isPostSeen(post)

                val status = if (isAskingToMarkAsSeen) {
                    apiCallsProvider.markPostAsSeen(post)
                } else {
                    apiCallsProvider.markPostAsUnseen(post)
                }

                when (status) {
                    is Success -> {
                        readerPostTableWrapper.makPostAsSeenLocally(post, isAskingToMarkAsSeen)
                        emit(
                                PostSeenStateChanged(
                                        isAskingToMarkAsSeen,
                                        UiStringRes(
                                                if (isAskingToMarkAsSeen)
                                                    string.reader_marked_post_as_seen
                                                else
                                                    string.reader_marked_post_as_unseen
                                        )
                                )
                        )
                    }
                    is SeenStatusToggleCallResult.Failure -> {
                        emit(Failure(UiStringText(status.error)))
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
}
