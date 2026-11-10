package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.flow.flow
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.reader.repository.BlockedUserResult
import org.wordpress.android.ui.reader.repository.ReaderBlogRepository
import org.wordpress.android.ui.reader.repository.usecases.BlockUserState.UserBlockedInLocalDb
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import javax.inject.Inject

class BlockUserUseCase @Inject constructor(
    private val readerTracker: ReaderTracker,
    private val readerBlogRepository: ReaderBlogRepository
) {
    fun blockUser(
        authorId: Long,
        feedId: Long
    ) = flow {
        // We want to track the action no matter the result
        readerTracker.trackBlog(
            AnalyticsTracker.Stat.READER_USER_BLOCKED,
            authorId,
            feedId
        )
        val blockedUserData = readerBlogRepository.blockUserLocal(authorId, feedId)
        emit(UserBlockedInLocalDb(blockedUserData))
    }

    fun undoBlockUser(blockedUserData: BlockedUserResult) {
        readerBlogRepository.undoBlockUser(blockedUserData)
    }
}

sealed class BlockUserState {
    data class UserBlockedInLocalDb(val blockedUserData: BlockedUserResult) : BlockUserState()
}
