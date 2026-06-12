package org.wordpress.android.ui.pagesrs

import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.ui.postsrs.PostRsDateFormatter
import org.wordpress.android.ui.postsrs.toLabel
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
    @StringRes val statusLabelResId: Int = 0,
    val authorId: Long = 0L,
    val authorDisplayName: String? = null,
    val isTrashed: Boolean = false,
    val badges: List<Int> = emptyList(),
    val displayState: PageRsDisplayState = PageRsDisplayState.NORMAL
)

internal fun PostItemState.toPageUiModel(
    pageId: Long,
    showStatus: Boolean = false
): PageRsUiModel = when (this) {
    is PostItemState.Fresh -> data.toPageUiModel(showStatus)
    is PostItemState.Stale -> data.toPageUiModel(showStatus)
    is PostItemState.FetchingWithData ->
        data.toPageUiModel(showStatus, PageRsDisplayState.FETCHING_WITH_DATA)
    is PostItemState.FailedWithData ->
        data.toPageUiModel(showStatus, PageRsDisplayState.FAILED_WITH_DATA)
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
    showStatus: Boolean,
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
        statusLabelResId = if (showStatus) page.status.toLabel() else 0,
        authorId = page.author ?: 0L,
        isTrashed = page.status is PostStatus.Trash,
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
