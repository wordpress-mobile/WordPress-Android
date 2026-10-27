package org.wordpress.android.ui.jetpackoverlay

import android.content.SharedPreferences
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * These assertions deliberately hard-code the stored key. It is built from
 * [JETPACK_REMOVAL_TRACKING_NAME], which reads like a vestigial phase name and is an easy thing to
 * "tidy up" — but changing it re-shows the switch-to-Jetpack overlay to every user who has already
 * dismissed it, silently and with no other test failing.
 */
@RunWith(MockitoJUnitRunner::class)
class JetpackFeatureOverlayShownTrackerTest {
    @Mock
    private lateinit var sharedPrefs: SharedPreferences

    @Mock
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var tracker: JetpackFeatureOverlayShownTracker

    @Before
    fun setUp() {
        tracker = JetpackFeatureOverlayShownTracker(sharedPrefs)
    }

    @Test
    fun `the removal tracking name is part of stored keys and must not change`() {
        assertThat(JETPACK_REMOVAL_TRACKING_NAME).isEqualTo("self_hosted")
    }

    @Test
    fun `marking the overlay shown writes the phase-suffixed key`() {
        whenever(sharedPrefs.edit()).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenReturn(editor)

        tracker.setFeatureCollectionOverlayShown()

        verify(editor).putBoolean(eq("KEY_FEATURE_COLLECTION_OVERLAY_SHOWNself_hosted"), eq(true))
    }

    @Test
    fun `reading whether the overlay was shown uses the phase-suffixed key`() {
        whenever(sharedPrefs.getBoolean(eq("KEY_FEATURE_COLLECTION_OVERLAY_SHOWNself_hosted"), eq(false)))
            .thenReturn(true)

        assertThat(tracker.getFeatureCollectionOverlayShown()).isTrue
    }
}
