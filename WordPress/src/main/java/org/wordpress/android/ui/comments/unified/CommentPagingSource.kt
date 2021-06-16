package org.wordpress.android.ui.comments.unified

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus

class CommentPagingSource : PagingSource<Int, CommentModel>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CommentModel> {
        return LoadResult.Page(
                data = generateComments(params.loadSize),
                prevKey = null, // Only paging forward
                nextKey = null // Only one page for now
        )
    }

    override fun getRefreshKey(state: PagingState<Int, CommentModel>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    // TODO for testing purposes only. Remove after attaching real data source.
    @Suppress("MagicNumber")
    fun generateComments(num: Int): List<CommentModel> {
        val commentListItems = ArrayList<CommentModel>()

        for (i in 0..num) {
            val commentModel = CommentModel()
            commentModel.id = i
            commentModel.remoteCommentId = i.toLong()
            commentModel.postTitle = "Post $i"
            commentModel.authorName = "Author $i"
            commentModel.authorEmail = "authors_email$i@wordpress.org"
            commentModel.content = "Generated Comment Content for Comment with remote ID $i"
            commentModel.authorProfileImageUrl = ""
            if (i % 3 == 0) {
                commentModel.status = CommentStatus.UNAPPROVED.toString()
            } else {
                commentModel.status = CommentStatus.APPROVED.toString()
            }

            commentListItems.add(commentModel)
        }
        return commentListItems
    }
}
