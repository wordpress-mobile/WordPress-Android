package org.wordpress.android.ui.newstats.poststats

import org.wordpress.android.ui.stats.StatsConstants

/** What tapping the per-post screen's header should do. Exactly one of these -- see [postHeaderAction]. */
internal sealed interface PostHeaderAction {
    /**
     * The header is inert: the site's home page comes back with no post, and an item can come
     * back without a permalink.
     */
    data object None : PostHeaderAction

    /**
     * There is something to open. Each case carries the item's URL, so a caller can never reach a
     * state where the header is clickable but has nothing to open -- the Reader case needs it too,
     * as the fallback for when the blog id cannot be resolved.
     */
    sealed interface Open : PostHeaderAction {
        val url: String
    }

    /** A post, which reads in the Reader rather than in a browser. */
    data class OpenInReader(override val url: String) : Open

    /** A page or an attachment, which has no Reader representation and opens at its own URL. */
    data class OpenInBrowser(override val url: String) : Open
}

/**
 * Decides what the per-post header does when tapped, from the two fields the `stats/post/<id>`
 * response supplies for the item.
 *
 * This is a testable function rather than inline in the composable because the type is a routing
 * key: [StatsConstants.ITEM_TYPE_POST] is the only value that goes to the Reader, and WordPress.com
 * can return any registered post type here.
 *
 * [url] is absent for the home page and may be blank for an item whose permalink the API does not
 * supply, and both mean the same thing as far as the header is concerned.
 */
internal fun postHeaderAction(url: String?, type: String?): PostHeaderAction = when {
    url.isNullOrBlank() || type == null -> PostHeaderAction.None
    type == StatsConstants.ITEM_TYPE_POST -> PostHeaderAction.OpenInReader(url)
    else -> PostHeaderAction.OpenInBrowser(url)
}
