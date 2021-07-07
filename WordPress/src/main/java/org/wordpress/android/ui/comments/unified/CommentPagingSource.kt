package org.wordpress.android.ui.comments.unified

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.comments.CommentsStore
import org.wordpress.android.util.NetworkUtilsWrapper

// TODO for testing purposes only. Remove after attaching real data source.
@Suppress("MagicNumber")
class CommentPagingSource(
    private val commentFilter: CommentFilter,
    private val cacheStatuses: List<CommentStatus>,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val commentsStore: CommentsStore,
    private val site: SiteModel?
) : PagingSource<Int, CommentEntity>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CommentEntity> {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return LoadResult.Error(Error("Network Unavailable"))
        }
        if (site == null) {
            return LoadResult.Error(Error("No Site Selected"))
        }
        val nextPageNumber = params.key ?: 0
        val startIndex = params.loadSize * nextPageNumber // TODOD: better check this!

        val response = commentsStore.fetchComments(
                site = site,
                number = params.loadSize,
                offset = startIndex,
                networkStatusFilter = commentFilter.toCommentStatus(),
                cacheStatuses = cacheStatuses
        )

        if (response.isError) {
            return LoadResult.Error(Error(response.error.message))
        }

        // TODOD: evaluate to rescue data from db directly
        //val allComments = CommentSqlUtils.getCommentsForSite(
        //        site,
        //        SelectQuery.ORDER_DESCENDING,
        //        startIndex + params.loadSize, commentFilter.toCommentStatus()
        //)

        val commentsToDeliver = response.response ?: listOf()//allComments.takeLast(params.loadSize)
        return LoadResult.Page(
                data = commentsToDeliver,
                prevKey = null, // Only paging forward
                nextKey = if (commentsToDeliver.isEmpty()) null else nextPageNumber + 1 // limit to 5 pages for now
        )
    }

    override fun getRefreshKey(state: PagingState<Int, CommentEntity>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}
