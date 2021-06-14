package org.wordpress.android.ui.comments.unified

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.util.DateTimeUtils
import java.util.Date

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
        var startTimestamp = System.currentTimeMillis() / 1000 - (30000 * num)

        for (i in 0..num) {
            val commentModel = CommentModel()
            commentModel.id = i
            commentModel.remoteCommentId = i.toLong()
            commentModel.postTitle = "Post $i"
            commentModel.authorName = "Author $i"
            commentModel.authorEmail = "authors_email$i@wordpress.org"
            commentModel.content = "Generated <b>Comment</b> <i>Content</i> for Comment with remote ID $i"
            startTimestamp -= 30000
            commentModel.publishedTimestamp = startTimestamp
            commentModel.datePublished = DateTimeUtils.iso8601FromDate(Date(startTimestamp * 1000))
            commentModel.authorProfileImageUrl = ""
            if (i % 3 == 0) {
                commentModel.status = CommentStatus.UNAPPROVED.toString()
                commentModel.authorProfileImageUrl = "https://0.gravatar.com/avatar/cec64efa352617c35743d8ed233ab410?s=96&d=identicon&r=G"
            } else {
                commentModel.status = CommentStatus.APPROVED.toString()
                commentModel.authorProfileImageUrl = "https://0.gravatar.com/avatar/cdc72cf084621e5cf7e42913f3197c13?s=256&d=identicon&r=G"
            }

            commentListItems.add(commentModel)
        }
        return commentListItems
    }
}
