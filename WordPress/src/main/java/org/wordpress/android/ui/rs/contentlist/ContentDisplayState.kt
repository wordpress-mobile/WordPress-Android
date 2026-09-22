package org.wordpress.android.ui.rs.contentlist

/**
 * How a row should present itself while the list is loading or syncing around it.
 *
 * NORMAL is the settled row. FETCHING_WITH_DATA and FAILED_WITH_DATA decorate a row that already
 * has content - a sync bar and a failure tint respectively - rather than replacing it. PLACEHOLDER
 * is a row the collection knows about but has not loaded yet, and ERROR is one whose load failed.
 */
enum class ContentDisplayState {
    NORMAL,
    FETCHING_WITH_DATA,
    FAILED_WITH_DATA,
    PLACEHOLDER,
    ERROR
}
