package org.wordpress.android.ui.reader.discover

/**
 * Use for filtering posts in Discover tab
 *
 * Example : https://public-api.wordpress.com/wpcom/v2/read/tags/cards?tags=dogs&sort=date
 *
 * If [NONE] is used, the parameter value will not be added to api call.
 *
 *  @property [sortedBy] : value used in api call
 *
 */
enum class DiscoverSortingType(val sortedBy: String) {
    NONE("none"),
    POPULARITY("popularity"),
    TIME("date")
}
