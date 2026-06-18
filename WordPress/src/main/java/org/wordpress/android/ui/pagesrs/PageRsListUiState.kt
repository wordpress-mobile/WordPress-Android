package org.wordpress.android.ui.pagesrs

import androidx.annotation.DrawableRes
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

/** A destructive or status-changing action awaiting user confirmation in a dialog. */
internal sealed interface PageRsListConfirmation {
    data class Trash(val pageId: Long) : PageRsListConfirmation
    data class Delete(val pageId: Long, val pageTitle: String) : PageRsListConfirmation
    data class MoveToDraft(val pageId: Long) : PageRsListConfirmation
}

internal data class PageRsConfirmationDialogState(
    val pending: PageRsListConfirmation? = null,
    val onConfirm: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)

/** State for the "Set Parent" bottom sheet. [candidates] excludes the page and its descendants. */
internal data class PageRsParentPickerState(
    val pageId: Long,
    val currentParentId: Long,
    val candidates: List<PageRsParentCandidate>
)

internal data class PageRsParentCandidate(
    val id: Long,
    val title: String
)

internal data class PageTabUiState(
    val pages: List<PageRsListItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val error: String? = null,
    val isAuthError: Boolean = false
)

internal sealed interface PageRsListItem {
    val page: PageRsUiModel
    val stableKey: String
    val remotePageId: Long get() = page.remotePageId

    data class Real(
        override val page: PageRsUiModel,
        val indentLevel: Int = 0
    ) : PageRsListItem {
        override val stableKey: String get() = "real:${page.remotePageId}"
    }

    data class Virtual(
        val kind: Kind,
        override val page: PageRsUiModel
    ) : PageRsListItem {
        override val stableKey: String get() = "virtual:$kind"

        enum class Kind { HOMEPAGE, POSTS_PAGE }
    }
}

internal enum class PageRsDisplayState {
    NORMAL,
    FETCHING_WITH_DATA,
    FAILED_WITH_DATA,
    PLACEHOLDER,
    ERROR
}

internal data class PageRsUiModel(
    val remotePageId: Long,
    val parentId: Long = 0L,
    val title: String,
    val excerpt: String,
    val date: String,
    val lastModified: String = "",
    val link: String = "",
    val hasPassword: Boolean = false,
    val status: PostStatus? = null,
    @StringRes val statusLabelResId: Int = 0,
    val authorId: Long = 0L,
    val authorDisplayName: String? = null,
    val featuredImageId: Long = 0L,
    val featuredImageUrl: String? = null,
    val isTrashed: Boolean = false,
    val actions: List<PageRsMenuAction> = emptyList(),
    val badges: List<Int> = emptyList(),
    val displayState: PageRsDisplayState = PageRsDisplayState.NORMAL
)

internal enum class PageRsMenuAction(
    @StringRes val labelResId: Int,
    @DrawableRes val iconResId: Int,
    val isDestructive: Boolean = false
) {
    VIEW(R.string.pages_view, R.drawable.gb_ic_external),
    SET_PARENT(R.string.set_parent, R.drawable.gb_ic_pages_set_as_parent),
    SET_AS_HOMEPAGE(R.string.pages_set_as_homepage, R.drawable.gb_ic_home_page_24dp),
    SET_AS_POSTS_PAGE(R.string.pages_set_as_posts_page, R.drawable.ic_posts_white_24dp),
    PUBLISH_NOW(R.string.pages_publish_now, R.drawable.gb_ic_globe),
    MOVE_TO_DRAFT(R.string.pages_move_to_draft, R.drawable.gb_ic_move_to),
    DUPLICATE(R.string.button_copy, R.drawable.gb_ic_copy),
    SHARE(R.string.button_share, R.drawable.gb_ic_share),
    COPY_URL(R.string.page_rs_copy_url, R.drawable.ic_attachment_link),
    BLAZE(R.string.pages_promote_with_blaze, R.drawable.ic_blaze_flame_24dp),
    TRASH(R.string.pages_move_to_trash, R.drawable.gb_ic_trash, isDestructive = true),
    DELETE_PERMANENTLY(
        R.string.pages_delete_permanently,
        R.drawable.gb_ic_trash,
        isDestructive = true
    ),
}

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
        parentId = page.parent ?: 0L,
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
        link = page.link,
        hasPassword = !page.password.isNullOrEmpty(),
        status = page.status,
        statusLabelResId = if (showStatus) page.status.toLabel() else 0,
        authorId = page.author ?: 0L,
        featuredImageId = page.featuredMedia ?: 0L,
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
