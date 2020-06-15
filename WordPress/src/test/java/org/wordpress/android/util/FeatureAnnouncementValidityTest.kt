package org.wordpress.android.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.whatsnew.FeatureAnnouncement
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementItem

class FeatureAnnouncementValidityTest {
    private val testFeatures = listOf(
            FeatureAnnouncementItem(
                    "Test Feature 1",
                    "Test Description 1",
                    "",
                    "https://wordpress.org/icon1.png"
            ),
            FeatureAnnouncementItem(
                    "Test Feature 2",
                    "Test Description 1",
                    "",
                    "https://wordpress.org/icon2.png"
            ),
            FeatureAnnouncementItem(
                    "Test Feature 3",
                    "Test Description 3",
                    "",
                    "https://wordpress.org/icon3.png"
            )
    )

    private val validAnnouncement = FeatureAnnouncement(
            "14.7",
            1,
            "14.5",
            "14.7",
            "https://wordpress.org/",
            true,
            testFeatures
    )

    private val nonLocalizedAnnouncement = FeatureAnnouncement(
            "14.7",
            1,
            "14.5",
            "14.7",
            "https://wordpress.org/",
            false,
            testFeatures
    )

    private val noFeaturesAnnouncement = FeatureAnnouncement(
            "14.7",
            1,
            "14.5",
            "14.7",
            "https://wordpress.org/",
            true,
            emptyList()
    )

    private val outOfRangeAnnouncement = FeatureAnnouncement(
            "14.2",
            1,
            "14.0",
            "14.2",
            "https://wordpress.org/",
            true,
            testFeatures
    )

    private val noMinVersionAnnouncement = FeatureAnnouncement(
            "14.7",
            1,
            "-1",
            "14.8",
            "https://wordpress.org/",
            true,
            testFeatures
    )

    private val noMaxVersionAnnouncement = FeatureAnnouncement(
            "14.7",
            1,
            "14.5",
            "-1",
            "https://wordpress.org/",
            true,
            testFeatures
    )

    @Test
    fun `canBeDisplayedOnAppUpgrade returns correct value based on announcement`() {
        assertThat(nonLocalizedAnnouncement.canBeDisplayedOnAppUpgrade("14.6")).isFalse()
        assertThat(noFeaturesAnnouncement.canBeDisplayedOnAppUpgrade("14.6")).isFalse()
        assertThat(outOfRangeAnnouncement.canBeDisplayedOnAppUpgrade("14.6")).isFalse()
        assertThat(validAnnouncement.canBeDisplayedOnAppUpgrade("14.7")).isTrue()

        assertThat(noMinVersionAnnouncement.canBeDisplayedOnAppUpgrade("14.7")).isTrue()
        assertThat(noMinVersionAnnouncement.canBeDisplayedOnAppUpgrade("14.8")).isTrue()
        assertThat(noMinVersionAnnouncement.canBeDisplayedOnAppUpgrade("14.9")).isFalse()

        assertThat(noMaxVersionAnnouncement.canBeDisplayedOnAppUpgrade("14.4")).isFalse()
        assertThat(noMaxVersionAnnouncement.canBeDisplayedOnAppUpgrade("14.5")).isTrue()
        assertThat(noMaxVersionAnnouncement.canBeDisplayedOnAppUpgrade("14.6")).isTrue()
    }
}
