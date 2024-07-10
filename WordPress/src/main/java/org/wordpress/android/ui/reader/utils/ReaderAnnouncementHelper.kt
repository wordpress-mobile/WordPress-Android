package org.wordpress.android.ui.reader.utils

import dagger.Reusable
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.views.compose.ReaderAnnouncementCardItemData
import org.wordpress.android.util.config.ReaderAnnouncementCardFeatureConfig
import org.wordpress.android.util.config.ReaderTagsFeedFeatureConfig
import javax.inject.Inject

@Reusable
class ReaderAnnouncementHelper @Inject constructor(
    private val readerAnnouncementCardFeatureConfig: ReaderAnnouncementCardFeatureConfig,
    private val readerTagsFeedFeatureConfig: ReaderTagsFeedFeatureConfig,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val readerTracker: ReaderTracker,
) {
    fun hasReaderAnnouncement(): Boolean {
        return readerAnnouncementCardFeatureConfig.isEnabled() && appPrefsWrapper.shouldShowReaderAnnouncementCard()
    }

    fun getReaderAnnouncementItems(): List<ReaderAnnouncementCardItemData> {
        if (!readerAnnouncementCardFeatureConfig.isEnabled() || !appPrefsWrapper.shouldShowReaderAnnouncementCard()) {
            return emptyList()
        }

        val items = mutableListOf<ReaderAnnouncementCardItemData>()

        if (readerTagsFeedFeatureConfig.isEnabled()) {
            items.add(
                ReaderAnnouncementCardItemData(
                    iconRes = R.drawable.ic_reader_tag,
                    titleRes = R.string.reader_announcement_card_tags_stream_title,
                    descriptionRes = R.string.reader_announcement_card_tags_stream_description,
                )
            )
        }

        items.add(
            ReaderAnnouncementCardItemData(
                iconRes = R.drawable.ic_reader_preferences,
                titleRes = R.string.reader_announcement_card_reading_preferences_title,
                descriptionRes = R.string.reader_announcement_card_reading_preferences_description,
            )
        )

        return items
    }

    fun dismissReaderAnnouncement() {
        readerTracker.track(AnalyticsTracker.Stat.READER_ANNOUNCEMENT_CARD_DISMISSED)
        appPrefsWrapper.setShouldShowReaderAnnouncementCard(false)
    }
}

