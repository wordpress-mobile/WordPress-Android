package org.wordpress.android.ui.reader.repository.usecases

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.reader.repository.BlockedUserResult
import org.wordpress.android.ui.reader.repository.ReaderBlogRepository
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import javax.inject.Inject

class BlockUserUseCase @Inject constructor(
    private val readerTracker: ReaderTracker,
    private val readerBlogRepository: ReaderBlogRepository
) {
    /**
     * Blocks the user locally and returns what's needed to undo it
     */
    fun blockUser(authorId: Long, feedId: Long): BlockedUserResult {
        readerTracker.trackBlog(AnalyticsTracker.Stat.READER_USER_BLOCKED, authorId, feedId)
        return readerBlogRepository.blockUserLocal(authorId, feedId)
    }
}
