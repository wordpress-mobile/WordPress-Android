package org.wordpress.android.ui.news

import javax.inject.Inject

/**
 * Helper to prevent tracking duplicate events when the view has been just recycled or after a config change.
 */
class NewsTrackerHelper @Inject constructor() {
    private val trackedItems: ArrayList<Int> = ArrayList()

    fun shouldTrackNewsCardShown(itemVersion: Int): Boolean = !trackedItems.contains(itemVersion)

    fun itemTracked(itemVersion: Int) {
        trackedItems.add(itemVersion)
    }

    fun reset() {
        trackedItems.clear()
    }
}
