package org.wordpress.android.ui.pagesrs

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.ui.rs.contentlist.ContentDisplayState
import org.wordpress.android.ui.rs.contentlist.ContentItemUiModel
import org.wordpress.android.ui.rs.contentlist.ContentListDensity
import org.wordpress.android.ui.rs.contentlist.ContentListRowUiState
import org.wordpress.android.ui.rs.contentlist.RsMenuAction

/** A page as the rs list renders it, carrying the pages menu's own actions. */
internal typealias PageRsUiModel = ContentItemUiModel<PageRsMenuAction>

/** A destructive or status-changing action awaiting user confirmation in a dialog. */
internal sealed interface PageRsListConfirmation {
    data class Trash(val pageId: Long) : PageRsListConfirmation
    data class Delete(val pageId: Long, val pageTitle: String) : PageRsListConfirmation
    data class MoveToDraft(val pageId: Long) : PageRsListConfirmation
}

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

internal sealed interface PageRsListItem {
    val page: PageRsUiModel
    val stableKey: String
    val remotePageId: Long get() = page.remoteId

    data class Real(
        override val page: PageRsUiModel,
        val indentLevel: Int = 0
    ) : PageRsListItem {
        override val stableKey: String get() = "real:${page.remoteId}"
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
 * Sentinel [PageRsListItem.remotePageId] for the synthetic SITE_EDITOR virtual row, which has no real
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

internal enum class PageRsMenuAction(
    @StringRes override val labelResId: Int,
    @DrawableRes override val iconResId: Int,
    override val isDestructive: Boolean = false
) : RsMenuAction {
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
        id = page.remoteId,
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
        isSyncing = page.displayState == ContentDisplayState.FETCHING_WITH_DATA,
        hasSyncFailed = page.displayState == ContentDisplayState.FAILED_WITH_DATA
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
