package org.wordpress.android.ui.pagesrs

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.ui.rs.contentlist.ContentListDensity
import org.wordpress.android.ui.rs.contentlist.ContentListRowUiState
import org.wordpress.android.ui.rs.data.FeaturedImageUrls
import org.wordpress.android.ui.rs.RsDateFormatter
import org.wordpress.android.ui.rs.toLabel
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

/** A request to select [tab] and scroll to [remotePageId] within it. */
internal data class PageRsReveal(
    val tab: PageRsListTab,
    val remotePageId: Long
)

internal data class PageRsConfirmationDialogState(
    val pending: PageRsListConfirmation? = null,
    val onConfirm: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)

/**
 * State for the "Set Parent" bottom sheet. [candidates] is a paged, optionally search-filtered
 * list of eligible published pages, excluding the page itself and its known descendants.
 */
internal data class PageRsParentPickerState(
    val pageId: Long,
    val currentParentId: Long,
    val candidates: List<PageRsParentCandidate>,
    val query: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val error: String? = null
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

        // HOMEPAGE / POSTS_PAGE wrap a real assigned static page; SITE_EDITOR is the block-theme
        // homepage, which has no backing page and opens the Site Editor web view on tap.
        enum class Kind { HOMEPAGE, POSTS_PAGE, SITE_EDITOR }
    }
}

/**
 * Sentinel [PageRsUiModel.remotePageId] for the synthetic SITE_EDITOR virtual row, which has no real
 * page behind it. Real remote page ids are always positive, so a negative value can't collide.
 */
internal const val SITE_EDITOR_PAGE_ID = -1L

/**
 * Whether the tab holds anything the site actually owns.
 *
 * A block-theme site gets the synthetic SITE_EDITOR row prepended even when it has no pages at
 * all, so row count alone can't answer "is this tab empty?" - and treating that row as content
 * would suppress the loading placeholders and the full-screen error state. Every other row,
 * including the HOMEPAGE and POSTS_PAGE virtuals, wraps a real page.
 */
internal val List<PageRsListItem>.hasRealPages: Boolean
    get() = any { it.remotePageId != SITE_EDITOR_PAGE_ID }

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
    val featuredImage: FeaturedImageUrls? = null,
    /** True when the media lookup answered without a URL, so the row should stop waiting for one. */
    val isFeaturedImageUnresolvable: Boolean = false,
    /** All-time views, or null when stats are unavailable or not fetched yet. */
    val viewCount: Long? = null,
    /** True while this row's view count is expected but has not arrived, so it shows a skeleton. */
    val areMetricsPending: Boolean = false,
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
    nowLabel: String,
    showStatus: Boolean = false
): PageRsUiModel = when (this) {
    is PostItemState.Fresh -> data.toPageUiModel(showStatus, nowLabel)
    is PostItemState.Stale -> data.toPageUiModel(showStatus, nowLabel)
    is PostItemState.FetchingWithData ->
        data.toPageUiModel(showStatus, nowLabel, PageRsDisplayState.FETCHING_WITH_DATA)
    is PostItemState.FailedWithData ->
        data.toPageUiModel(showStatus, nowLabel, PageRsDisplayState.FAILED_WITH_DATA)
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
    nowLabel: String,
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
        date = RsDateFormatter.format(page.dateGmt, nowLabel, isScheduled = page.status is PostStatus.Future),
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

/**
 * The label the Homepage and Posts page rows carry.
 *
 * SITE_EDITOR renders its own title from string resources, so it never shows this alongside one.
 */
@StringRes
internal fun PageRsListItem.Virtual.Kind.labelResId(): Int = when (this) {
    PageRsListItem.Virtual.Kind.HOMEPAGE -> R.string.site_settings_homepage
    PageRsListItem.Virtual.Kind.POSTS_PAGE -> R.string.site_settings_posts_page
    PageRsListItem.Virtual.Kind.SITE_EDITOR -> R.string.virtual_homepage_title
}

/** Replaces the row's page, keeping whichever kind of row it is. */
internal fun PageRsListItem.withPage(page: PageRsUiModel): PageRsListItem = when (this) {
    is PageRsListItem.Real -> copy(page = page)
    is PageRsListItem.Virtual -> copy(page = page)
}

/**
 * Projects a row onto the shared model the redesigned list renders.
 *
 * The SITE_EDITOR row has no backing page, so its text comes from the caller's already-resolved
 * strings. Everything the pre-redesign row spelled out in its own coloured header line - the
 * Homepage / Posts page label, and the status shown while searching - rides along as a badge
 * instead, which is where the redesigned card puts short qualifiers.
 */
internal fun PageRsListItem.toContentListRowUiState(
    siteEditorTitle: String,
    siteEditorSubtitle: String
): ContentListRowUiState {
    val kind = (this as? PageRsListItem.Virtual)?.kind
    val isSiteEditor = kind == PageRsListItem.Virtual.Kind.SITE_EDITOR
    return ContentListRowUiState(
        id = page.remotePageId,
        title = if (isSiteEditor) siteEditorTitle else page.title,
        excerpt = if (isSiteEditor) siteEditorSubtitle else page.excerpt,
        dateLabel = page.date,
        thumbnailImageUrl = page.featuredImage?.thumbnail,
        heroImageUrl = page.featuredImage?.hero,
        isImagePending = page.featuredImageId != 0L &&
            page.featuredImage == null &&
            !page.isFeaturedImageUnresolvable,
        viewCount = page.viewCount,
        areMetricsPending = page.areMetricsPending,
        badges = buildList {
            if (kind != null && !isSiteEditor) add(kind.labelResId())
            page.statusLabelResId.takeIf { it != 0 }?.let { add(it) }
            addAll(page.badges)
        },
        isSyncing = page.displayState == PageRsDisplayState.FETCHING_WITH_DATA,
        hasSyncFailed = page.displayState == PageRsDisplayState.FAILED_WITH_DATA
    )
}

/**
 * Whether the row is drawn image-led rather than compact.
 *
 * Top-level rows only. The published tab is a tree, and an indented full-width image reads as a
 * broken card rather than a lead item; virtual rows are excluded by the same token, since they are
 * a fixed block at the top of the list carrying a leading icon.
 *
 * Keyed off the featured image *id*, which is present as soon as the page loads, rather than the
 * resolved URL, which arrives a network call later - keying off the URL would pop rows from compact
 * to hero as their images resolved. A page whose media could not be resolved falls back to the
 * compact shape, since an image-led card with no image is just a compact card with wrong padding.
 */
internal fun PageRsListItem.isHeroRow(density: ContentListDensity): Boolean =
    this is PageRsListItem.Real &&
        indentLevel == 0 &&
        !density.isCondensed &&
        page.featuredImageId != 0L &&
        !page.isFeaturedImageUnresolvable
