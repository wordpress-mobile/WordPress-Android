package org.wordpress.android.ui.localcontent

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R

/** The kinds of working copy the local content screens list, each on a screen of its own. */
enum class WorkingCopyKind(
    @StringRes val titleResId: Int,
    @StringRes val emptyMessageResId: Int,
    @DrawableRes val iconResId: Int
) {
    POST(R.string.posts, R.string.local_content_no_post_working_copies, R.drawable.ic_posts_white_24dp),
    PAGE(R.string.pages, R.string.local_content_no_page_working_copies, R.drawable.ic_pages_white_24dp),
}
