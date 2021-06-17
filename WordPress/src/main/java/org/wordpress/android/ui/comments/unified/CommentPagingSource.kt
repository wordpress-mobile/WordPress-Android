package org.wordpress.android.ui.comments.unified

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.delay
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.NetworkUtilsWrapper
import java.util.Date
import javax.inject.Inject

class CommentPagingSource : PagingSource<Int, CommentModel>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CommentModel> {
        val nextPageNumber = params.key ?: 0
        Log.v("Comments", "UC: Local Data Requested, page $nextPageNumber")
        delay(1500) // synthetic delay
        Log.v("Comments", "UC: Local Data Loaded")
        return LoadResult.Page(
                data = generateComments(params.loadSize, nextPageNumber),
                prevKey = null, // Only paging forward
                nextKey = if (nextPageNumber == 4) null else nextPageNumber + 1 // limit to 5 pages for now
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
    fun generateComments(loadSize: Int, page: Int): List<CommentModel> {
        val commentListItems = ArrayList<CommentModel>()
        val startIndex = loadSize * page
        var startTimestamp = System.currentTimeMillis() / 1000 - (30000 * startIndex)

        for (i in startIndex..startIndex + loadSize - 1) {
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
                commentModel.authorProfileImageUrl =
                        "https://0.gravatar.com/avatar/cec64efa352617c35743d8ed233ab410?s=96&d=identicon&r=G"
            } else {
                commentModel.status = CommentStatus.APPROVED.toString()
                commentModel.authorProfileImageUrl =
                        "https://0.gravatar.com/avatar/cdc72cf084621e5cf7e42913f3197c13?s=256&d=identicon&r=G"
            }

            commentListItems.add(commentModel)
        }
        return commentListItems
    }
}
