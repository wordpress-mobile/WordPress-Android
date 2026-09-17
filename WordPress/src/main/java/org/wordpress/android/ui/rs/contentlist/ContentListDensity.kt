package org.wordpress.android.ui.rs.contentlist

/**
 * How much of each row the redesigned posts/pages list shows.
 *
 * [CONDENSED] drops the excerpt, the metrics and the lead row's hero image, which roughly halves the
 * height of a row. The excerpt is the part that matters: without it the other two save nothing after
 * the first row, since the date still occupies the metadata line and the overflow button sets its
 * height.
 *
 * Dropping the metrics also stops them being fetched at all, so a condensed list makes no per-post
 * stats requests.
 */
enum class ContentListDensity {
    COMFORTABLE,
    CONDENSED;

    val isCondensed: Boolean get() = this == CONDENSED

    companion object {
        fun of(isCondensed: Boolean) = if (isCondensed) CONDENSED else COMFORTABLE
    }
}
