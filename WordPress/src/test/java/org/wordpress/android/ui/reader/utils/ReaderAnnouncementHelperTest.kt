package org.wordpress.android.ui.reader.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.util.config.ReaderAnnouncementCardFeatureConfig
import org.wordpress.android.util.config.ReaderTagsFeedFeatureConfig

@RunWith(MockitoJUnitRunner::class)
class ReaderAnnouncementHelperTest {
    @Mock
    private lateinit var readerAnnouncementCardFeatureConfig: ReaderAnnouncementCardFeatureConfig

    @Mock
    private lateinit var readerTagsFeedFeatureConfig: ReaderTagsFeedFeatureConfig

    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    private lateinit var readerTracker: ReaderTracker

    private lateinit var repository: ReaderAnnouncementHelper

    @Before
    fun setUp() {
        repository = ReaderAnnouncementHelper(
            readerAnnouncementCardFeatureConfig,
            readerTagsFeedFeatureConfig,
            appPrefsWrapper,
            readerTracker
        )
    }

    @Test
    fun `given feature config is off the hasReaderAnnouncement is false`() {
        // Given
        whenever(readerAnnouncementCardFeatureConfig.isEnabled()).thenReturn(false)

        // When
        val hasAnnouncement = repository.hasReaderAnnouncement()

        // Then
        assertThat(hasAnnouncement).isFalse()
    }

    @Test
    fun `given should show announcement in prefs is false the hasReaderAnnouncement is false`() {
        // Given
        whenever(readerAnnouncementCardFeatureConfig.isEnabled()).thenReturn(true)
        whenever(appPrefsWrapper.shouldShowReaderAnnouncementCard()).thenReturn(false)

        // When
        val hasAnnouncement = repository.hasReaderAnnouncement()

        // Then
        assertThat(hasAnnouncement).isFalse()
    }

    @Test
    fun `given feature config is on and should show announcement in prefs is true the hasReaderAnnouncement is true`() {
        // Given
        whenever(readerAnnouncementCardFeatureConfig.isEnabled()).thenReturn(true)
        whenever(appPrefsWrapper.shouldShowReaderAnnouncementCard()).thenReturn(true)

        // When
        val hasAnnouncement = repository.hasReaderAnnouncement()

        // Then
        assertThat(hasAnnouncement).isTrue()
    }

    @Test
    fun `given tags feed feature is off when getReaderAnnouncementItems then return single item`() {
        // Given
        whenever(readerAnnouncementCardFeatureConfig.isEnabled()).thenReturn(true)
        whenever(appPrefsWrapper.shouldShowReaderAnnouncementCard()).thenReturn(true)
        whenever(readerTagsFeedFeatureConfig.isEnabled()).thenReturn(false)

        // When
        val items = repository.getReaderAnnouncementItems()

        // Then
        assertThat(items).hasSize(1)

        val readerPreferencesItem = items[0]
        assertThat(readerPreferencesItem.iconRes).isEqualTo(R.drawable.ic_reader_preferences)
        assertThat(readerPreferencesItem.titleRes).isEqualTo(
            R.string.reader_announcement_card_reading_preferences_title
        )
        assertThat(readerPreferencesItem.descriptionRes).isEqualTo(
            R.string.reader_announcement_card_reading_preferences_description
        )
    }

    @Test
    fun `given tags feed feature is on when getReaderAnnouncementItems then return single item`() {
        // Given
        whenever(readerAnnouncementCardFeatureConfig.isEnabled()).thenReturn(true)
        whenever(appPrefsWrapper.shouldShowReaderAnnouncementCard()).thenReturn(true)
        whenever(readerTagsFeedFeatureConfig.isEnabled()).thenReturn(true)

        // When
        val items = repository.getReaderAnnouncementItems()

        // Then
        assertThat(items).hasSize(2)

        val tagsFeedItem = items[0]
        assertThat(tagsFeedItem.iconRes).isEqualTo(R.drawable.ic_reader_tag)
        assertThat(tagsFeedItem.titleRes).isEqualTo(R.string.reader_announcement_card_tags_stream_title)
        assertThat(tagsFeedItem.descriptionRes).isEqualTo(R.string.reader_announcement_card_tags_stream_description)

        val readerPreferencesItem = items[1]
        assertThat(readerPreferencesItem.iconRes).isEqualTo(R.drawable.ic_reader_preferences)
        assertThat(readerPreferencesItem.titleRes).isEqualTo(
            R.string.reader_announcement_card_reading_preferences_title
        )
        assertThat(readerPreferencesItem.descriptionRes).isEqualTo(
            R.string.reader_announcement_card_reading_preferences_description
        )
    }

    @Test
    fun `when dismissReaderAnnouncement then track`() {
        // When
        repository.dismissReaderAnnouncement()

        // Then
        verify(readerTracker).track(AnalyticsTracker.Stat.READER_ANNOUNCEMENT_CARD_DISMISSED)
    }

    @Test
    fun `when dismissReaderAnnouncement then set should show reader announcement card to false`() {
        // When
        repository.dismissReaderAnnouncement()

        // Then
        verify(appPrefsWrapper).setShouldShowReaderAnnouncementCard(false)
    }
}
