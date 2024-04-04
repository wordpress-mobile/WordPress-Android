package org.wordpress.android.ui.reader.tracker

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import org.wordpress.android.ui.reader.tracker.ReaderReadingPreferencesTracker.Source
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@RunWith(MockitoJUnitRunner::class)
class ReaderReadingPreferencesTrackerTest {
    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private lateinit var tracker: ReaderReadingPreferencesTracker

    @Before
    fun setUp() {
        tracker = ReaderReadingPreferencesTracker(analyticsTrackerWrapper)
    }

    @Test
    fun `when trackScreenOpened is called, then track event`() {
        Source.values().forEach { source ->
            tracker.trackScreenOpened(source)

            val expectedSource = when (source) {
                Source.POST_DETAIL_TOOLBAR -> "post_detail_toolbar"
                Source.POST_DETAIL_MORE_MENU -> "post_detail_more_menu"
            }

            verify(analyticsTrackerWrapper).track(
                Stat.READER_READING_PREFERENCES_OPENED,
                mapOf("source" to expectedSource)
            )
        }
    }

    @Test
    fun `when trackScreenClosed is called, then track event`() {
        tracker.trackScreenClosed()

        verify(analyticsTrackerWrapper).track(Stat.READER_READING_PREFERENCES_CLOSED)
    }

    @Test
    fun `when trackFeedbackTapped is called, then track event`() {
        tracker.trackFeedbackTapped()

        verify(analyticsTrackerWrapper).track(Stat.READER_READING_PREFERENCES_FEEDBACK_TAPPED)
    }

    @Test
    fun `when trackItemTapped is called with theme, then track event`() {
        ReaderReadingPreferences.Theme.values().forEach { theme ->
            tracker.trackItemTapped(theme)

            verify(analyticsTrackerWrapper).track(
                Stat.READER_READING_PREFERENCES_ITEM_TAPPED,
                mapOf(
                    "type" to "color_scheme",
                    "value" to propValueFor(theme)
                )
            )
        }
    }

    @Test
    fun `when trackItemTapped is called with font family, then track event`() {
        ReaderReadingPreferences.FontFamily.values().forEach { fontFamily ->
            tracker.trackItemTapped(fontFamily)

            verify(analyticsTrackerWrapper).track(
                Stat.READER_READING_PREFERENCES_ITEM_TAPPED,
                mapOf(
                    "type" to "font",
                    "value" to propValueFor(fontFamily)
                )
            )
        }
    }

    @Test
    fun `when trackItemTapped is called with font size, then track event`() {
        ReaderReadingPreferences.FontSize.values().forEach { fontSize ->
            tracker.trackItemTapped(fontSize)

            verify(analyticsTrackerWrapper).track(
                Stat.READER_READING_PREFERENCES_ITEM_TAPPED,
                mapOf(
                    "type" to "font_size",
                    "value" to propValueFor(fontSize)
                )
            )
        }
    }

    @Test
    fun `given default preferences, when trackSaved is called, then track event`() {
        val preferences = ReaderReadingPreferences()

        tracker.trackSaved(preferences)

        verify(analyticsTrackerWrapper).track(
            Stat.READER_READING_PREFERENCES_SAVED,
            mapOf(
                "is_default" to true,
                "color_scheme" to propValueFor(preferences.theme),
                "font" to propValueFor(preferences.fontFamily),
                "font_size" to propValueFor(preferences.fontSize)
            )
        )
    }

    @Test
    fun `given custom preferences, when trackSaved is called, then track event`() {
        val preferences = ReaderReadingPreferences(
            theme = ReaderReadingPreferences.Theme.SOFT,
            fontFamily = ReaderReadingPreferences.FontFamily.SERIF,
            fontSize = ReaderReadingPreferences.FontSize.LARGE
        )

        tracker.trackSaved(preferences)

        verify(analyticsTrackerWrapper).track(
            Stat.READER_READING_PREFERENCES_SAVED,
            mapOf(
                "is_default" to false,
                "color_scheme" to propValueFor(preferences.theme),
                "font" to propValueFor(preferences.fontFamily),
                "font_size" to propValueFor(preferences.fontSize)
            )
        )
    }

    @Test
    fun `given all possible combinations, when getPropertiesForPreferences is called, return expected properties`() {
        val defaultPreferences = ReaderReadingPreferences()

        ReaderReadingPreferences.Theme.values().forEach { theme ->
            ReaderReadingPreferences.FontFamily.values().forEach { fontFamily ->
                ReaderReadingPreferences.FontSize.values().forEach { fontSize ->
                    val preferences = ReaderReadingPreferences(theme, fontFamily, fontSize)
                    val expectedProperties = mapOf(
                        "is_default" to (preferences == defaultPreferences),
                        "color_scheme" to propValueFor(theme),
                        "font" to propValueFor(fontFamily),
                        "font_size" to propValueFor(fontSize)
                    )

                    val result = tracker.getPropertiesForPreferences(preferences)

                    assertThat(result).isEqualTo(expectedProperties)
                }
            }
        }
    }

    @Test
    fun `given a prefix, when getPropertiesForPreferences is called, return expected properties with prefix`() {
        val prefix = "my_prefix"

        val preferences = ReaderReadingPreferences()
        val expectedProperties = mapOf(
            "my_prefix_is_default" to true,
            "my_prefix_color_scheme" to propValueFor(preferences.theme),
            "my_prefix_font" to propValueFor(preferences.fontFamily),
            "my_prefix_font_size" to propValueFor(preferences.fontSize)
        )

        val result = tracker.getPropertiesForPreferences(preferences, prefix)

        assertThat(result).isEqualTo(expectedProperties)
    }

    // region helper methods (note: they match the implementation but they are duplicated here for reliable testing)
    private fun propValueFor(theme: ReaderReadingPreferences.Theme) = when (theme) {
        ReaderReadingPreferences.Theme.SYSTEM -> "default"
        ReaderReadingPreferences.Theme.SOFT -> "soft"
        ReaderReadingPreferences.Theme.SEPIA -> "sepia"
        ReaderReadingPreferences.Theme.EVENING -> "evening"
        ReaderReadingPreferences.Theme.OLED -> "oled"
        ReaderReadingPreferences.Theme.H4X0R -> "h4x0r"
        ReaderReadingPreferences.Theme.CANDY -> "candy"
    }

    private fun propValueFor(fontFamily: ReaderReadingPreferences.FontFamily) = when (fontFamily) {
        ReaderReadingPreferences.FontFamily.SANS -> "sans"
        ReaderReadingPreferences.FontFamily.SERIF -> "serif"
        ReaderReadingPreferences.FontFamily.MONO -> "mono"
    }

    private fun propValueFor(fontSize: ReaderReadingPreferences.FontSize) = when (fontSize) {
        ReaderReadingPreferences.FontSize.EXTRA_SMALL -> "extra_small"
        ReaderReadingPreferences.FontSize.SMALL -> "small"
        ReaderReadingPreferences.FontSize.NORMAL -> "normal"
        ReaderReadingPreferences.FontSize.LARGE -> "large"
        ReaderReadingPreferences.FontSize.EXTRA_LARGE -> "extra_large"
    }
    // endregion
}
