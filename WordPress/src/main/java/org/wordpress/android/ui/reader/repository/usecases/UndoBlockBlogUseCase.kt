package org.wordpress.android.ui.reader.repository.usecases

import org.wordpress.android.ui.reader.repository.BlockedBlogResult
import org.wordpress.android.ui.reader.repository.ReaderBlogRepository
import javax.inject.Inject

class UndoBlockBlogUseCase @Inject constructor(
    private val readerBlogRepository: ReaderBlogRepository
) {
    suspend fun undoBlockBlog(blockedBlogData: BlockedBlogResult, source: String) {
        readerBlogRepository.undoBlockBlog(blockedBlogData, source)
    }
}
