package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.reader.actions.ReaderBlogActions
import org.wordpress.android.ui.reader.actions.ReaderBlogActions.BlockedUserResult
import org.wordpress.android.ui.reader.actions.ReaderBlogActionsWrapper
import org.wordpress.android.ui.reader.repository.usecases.BlockUserState.Failed.AlreadyRunning
import org.wordpress.android.ui.reader.repository.usecases.BlockUserState.UserBlockedInLocalDb
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import javax.inject.Inject
import kotlin.coroutines.Continuation

class BlockUserUseCase @Inject constructor(
    private val readerTracker: ReaderTracker,
    private val readerBlogActionsWrapper: ReaderBlogActionsWrapper
) {
    private var continuation: Continuation<Boolean>? = null

    suspend fun blockUser(
        authorId: Long,
        feedId: Long
    ) = flow {
        // Blocking multiple users in parallel isn't supported as the user would lose the ability to undo the action
        if (continuation == null) {
            performAction(authorId, feedId)
        } else {
            emit(AlreadyRunning)
        }
    }

    fun undoBlockUser(blockedBlogData: BlockedUserResult) {
        ReaderBlogActions.undoBlockUserFromReader(blockedBlogData)
    }

    private suspend fun FlowCollector<BlockUserState>.performAction(
        authorId: Long,
        feedId: Long
    ) {
        // We want to track the action no matter the result
        readerTracker.trackBlog(
                AnalyticsTracker.Stat.READER_USER_BLOCKED,
                authorId,
                feedId
        )
        val blockedBlogData = readerBlogActionsWrapper.blockUserFromReaderLocal(authorId, feedId)
        emit(UserBlockedInLocalDb(blockedBlogData))
    }
}

sealed class BlockUserState {
    data class UserBlockedInLocalDb(val blockedUserData: BlockedUserResult) : BlockUserState()
    sealed class Failed : BlockUserState() {
        object AlreadyRunning : Failed()
    }
}
