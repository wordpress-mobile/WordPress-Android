package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.reader.actions.ReaderPostActionsWrapper
import javax.inject.Inject
import javax.inject.Named

class PostBookmarkActionUseCase @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val readerPostActionsWrapper: ReaderPostActionsWrapper
) {
    suspend fun perform(post: ReaderPost, isAskingToBookmark: Boolean) {
        withContext(ioDispatcher) {
            if (isAskingToBookmark)
                readerPostActionsWrapper.addToBookmarked(post)
            else
                readerPostActionsWrapper.removeFromBookmarked(post)
        }
    }
}
