package org.wordpress.android.ui.reader.repository

enum class UpdateAction {
    REQUEST_NEWER, // request the newest posts for this tag/blog/feed
    REQUEST_REFRESH, // request fresh data and get rid of the rest
    REQUEST_OLDER, // request posts older than the oldest existing one for this tag/blog/feed
    REQUEST_OLDER_THAN_GAP // request posts older than the one with the gap marker for this tag
                           // (not supported for blog/feed)
}
