package org.wordpress.android.ui.rs.contentlist

/** How a row presents itself while the list loads or syncs around it. */
enum class ContentDisplayState {
    NORMAL,
    FETCHING_WITH_DATA,
    FAILED_WITH_DATA,
    PLACEHOLDER,
    ERROR
}
