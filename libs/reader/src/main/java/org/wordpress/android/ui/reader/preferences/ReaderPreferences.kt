package org.wordpress.android.ui.reader.preferences

import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.reader.tracker.ReaderTab

/**
 * Interface for reader-specific preferences, abstracting
 * away the app-layer AppPrefsWrapper dependency.
 */
interface ReaderPreferences {
    var readerTagsUpdatedTimestamp: Long
    var readerAnalyticsCountTagsTimestamp: Long
    var readerCssUpdatedTimestamp: Long
    var readerCardsPageHandle: String?
    var readerTopBarSelectedFeedItemId: String?
    var readerReadingPreferencesJson: String?

    fun getReaderActiveTab(): ReaderTab?
    fun setReaderActiveTab(selectedTab: ReaderTab?)

    fun getReaderTag(): ReaderTag?
    fun setReaderTag(selectedTag: ReaderTag?)

    fun getLastReaderKnownUserId(): Long
    fun setLastReaderKnownUserId(userId: Long)

    fun getLastReaderKnownAccessTokenStatus(): Boolean
    fun setLastReaderKnownAccessTokenStatus(
        lastKnownAccessTokenStatus: Boolean
    )

    fun shouldShowBookmarksSavedLocallyDialog(): Boolean
    fun setBookmarksSavedLocallyDialogShown()

    fun shouldUpdateBookmarkPostsPseudoIds(tag: ReaderTag?): Boolean
    fun setBookmarkPostsPseudoIdsUpdated()

    fun shouldShowReaderAnnouncementCard(): Boolean
    fun setShouldShowReaderAnnouncementCard(shouldShow: Boolean)

    fun getReaderCardsRefreshCounter(): Int
    fun incrementReaderCardsRefreshCounter()
}
