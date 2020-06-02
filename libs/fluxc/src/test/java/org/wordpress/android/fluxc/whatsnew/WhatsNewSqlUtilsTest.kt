package org.wordpress.android.fluxc.whatsnew

import com.yarolegovich.wellsql.WellSql
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.whatsnew.WhatsNewAnnouncementModel
import org.wordpress.android.fluxc.model.whatsnew.WhatsNewAnnouncementModel.WhatsNewAnnouncementFeature
import org.wordpress.android.fluxc.persistence.WhatsNewSqlUtils
import org.wordpress.android.fluxc.persistence.WhatsNewSqlUtils.WhatsNewAnnouncementBuilder
import org.wordpress.android.fluxc.persistence.WhatsNewSqlUtils.WhatsNewAnnouncementFeatureBuilder

@RunWith(RobolectricTestRunner::class)
class WhatsNewSqlUtilsTest {
    private lateinit var whatsNewSqlUtils: WhatsNewSqlUtils

    private val firstAnnouncement = WhatsNewAnnouncementModel(
            "15.0",
            1,
            "14.7",
            "14.9",
            "https://wordpress.org",
            true,
            "it",
            listOf(
                    WhatsNewAnnouncementFeature(
                            "first announcement feature 1",
                            "first announcement subtitle 1",
                            "",
                            "https://wordpress.org/icon1.png"
                    ),
                    WhatsNewAnnouncementFeature(
                            "first announcement feature 2",
                            "first announcement subtitle 2",
                            "<image data>",
                            ""
                    )
            )
    )

    private val secondAnnouncement = WhatsNewAnnouncementModel(
            "16.0",
            2,
            "14.9",
            "16.0",
            "https://wordpress.org/announcement2/",
            false,
            "en",
            listOf(
                    WhatsNewAnnouncementFeature(
                            "second announcement feature 1",
                            "second announcement subtitle 1",
                            "",
                            "https://wordpress.org/icon2.png"
                    ),
                    WhatsNewAnnouncementFeature(
                            "second announcement feature 2",
                            "first announcement subtitle 2",
                            "<second image data>",
                            ""
                    )
            )
    )

    private val testAnnouncements = listOf(firstAnnouncement, secondAnnouncement)

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(WhatsNewAnnouncementBuilder::class.java, WhatsNewAnnouncementFeatureBuilder::class.java), ""
        )
        WellSql.init(config)
        config.reset()

        whatsNewSqlUtils = WhatsNewSqlUtils()
    }

    @Test
    fun `announcements are stored and retrieved correctly`() {
        whatsNewSqlUtils.updateAnnouncementCache(testAnnouncements)

        val cachedAnnouncements = whatsNewSqlUtils.getAnnouncements()
        assertEquals(testAnnouncements, cachedAnnouncements)
    }

    @Test
    fun `hasCachedAnnouncements returns true when there are cached announcements`() {
        assertEquals(whatsNewSqlUtils.hasCachedAnnouncements(), false)
        whatsNewSqlUtils.updateAnnouncementCache(testAnnouncements)
        assertEquals(whatsNewSqlUtils.hasCachedAnnouncements(), true)
    }
}
