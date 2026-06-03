package org.wordpress.android.ui.pagesrs

import org.wordpress.android.R
import org.wordpress.android.ui.postsrs.PostRsDateFormatter
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.HtmlUtils
import uniffi.wp_api.AnyPostWithEditContext
import uniffi.wp_api.PostStatus
import uniffi.wp_mobile.FullEntityAnyPostWithEditContext
import uniffi.wp_mobile.PostItemState

internal data class PageTabUiState(
    val pages: List<PageRsUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val error: String? = null,
    val isAuthError: Boolean = false
)

internal enum class PageRsDisplayState {
    NORMAL,
    FETCHING_WITH_DATA,
    FAILED_WITH_DATA,
    PLACEHOLDER,
    ERROR
}

internal data class PageRsUiModel(
    val remotePageId: Long,
    val title: String,
    val excerpt: String,
    val date: String,
    val lastModified: String = "",
    val badges: List<Int> = emptyList(),
    val displayState: PageRsDisplayState = PageRsDisplayState.NORMAL
)

internal fun PostItemState.toPageUiModel(pageId: Long): PageRsUiModel = when (this) {
    is PostItemState.Fresh -> data.toPageUiModel()
    is PostItemState.Stale -> data.toPageUiModel()
    is PostItemState.FetchingWithData ->
        data.toPageUiModel(PageRsDisplayState.FETCHING_WITH_DATA)
    is PostItemState.FailedWithData ->
        data.toPageUiModel(PageRsDisplayState.FAILED_WITH_DATA)
    is PostItemState.Missing,
    is PostItemState.Fetching -> PageRsUiModel(
        remotePageId = pageId,
        title = "",
        excerpt = "",
        date = "",
        displayState = PageRsDisplayState.PLACEHOLDER
    )
    is PostItemState.Failed -> PageRsUiModel(
        remotePageId = pageId,
        title = "",
        excerpt = "",
        date = "",
        displayState = PageRsDisplayState.ERROR
    )
}

private fun FullEntityAnyPostWithEditContext.toPageUiModel(
    displayState: PageRsDisplayState = PageRsDisplayState.NORMAL
): PageRsUiModel {
    val page: AnyPostWithEditContext = data
    return PageRsUiModel(
        remotePageId = page.id,
        title = page.title?.raw?.takeIf { it.isNotBlank() }
            ?: page.title?.rendered
            ?: "",
        excerpt = (
            page.excerpt?.raw?.takeIf { it.isNotBlank() }
                ?: page.excerpt?.rendered
                ?: ""
            ).let { HtmlUtils.fastStripHtml(it).trim() },
        date = PostRsDateFormatter.format(page.dateGmt, page.status),
        lastModified = DateTimeUtils.iso8601UTCFromDate(page.modifiedGmt),
        badges = buildList {
            if (page.status is PostStatus.Private) {
                add(R.string.post_status_post_private)
            }
            if (page.status is PostStatus.Pending) {
                add(R.string.post_status_pending_review)
            }
        },
        displayState = displayState
    )
}
