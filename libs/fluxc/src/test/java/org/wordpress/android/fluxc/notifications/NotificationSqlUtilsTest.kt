package org.wordpress.android.fluxc.notifications

import com.google.gson.Gson
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.notification.NoteIdSet
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationApiResponse
import org.wordpress.android.fluxc.persistence.NotificationSqlUtils
import org.wordpress.android.fluxc.persistence.NotificationSqlUtils.NotificationModelBuilder
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.tools.FormattableContentMapper
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class NotificationSqlUtilsTest {
    // All notifications require a matching [SiteModel] in the database in order to save
    // to the database due to a foreign key constraint
    private val site = SiteModel().apply {
        name = "Unit Test Site"
        url = "https://www.mytestsite.com"
    }

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(NotificationModelBuilder::class.java, SiteModel::class.java),
                "")
        WellSql.init(config)
        config.reset()

        // Save the test [SiteModel] to the db
        saveSiteToDb(site)
    }

    @Test
    fun testInsertOrUpdateNotifications() {
        // Test inserting notifications
        val notificationSqlUtils = NotificationSqlUtils(FormattableContentMapper(Gson()))
        generateAndSaveTestNotificationsToDb(notificationSqlUtils, site)
        val notesList = notificationSqlUtils.getNotifications()

        // Test updating notifications
        val newNote = notesList[0].copy(noteId = -1, remoteNoteId = 333)
        val dbList = notificationSqlUtils.getNotifications().toMutableList()
        dbList.add(newNote)
        val updated = dbList.sumBy { notificationSqlUtils.insertOrUpdateNotification(it) }
        assertEquals(7, updated)
        val updatedList = notificationSqlUtils.getNotifications()
        assertEquals(7, updatedList.size)
    }

    @Test
    fun testGetNotifications() {
        // Insert notifications
        val notificationSqlUtils = NotificationSqlUtils(FormattableContentMapper(Gson()))
        generateAndSaveTestNotificationsToDb(notificationSqlUtils, site)

        // Get notifications
        val notifications = notificationSqlUtils.getNotifications(SelectQuery.ORDER_DESCENDING)
        assertEquals(6, notifications.size)
    }

    @Test
    fun testGetNotificationsForSite() {
        // Insert notifications
        val notificationSqlUtils = NotificationSqlUtils(FormattableContentMapper(Gson()))
        generateAndSaveTestNotificationsToDb(notificationSqlUtils, site)

        // Get notifications
        val notifications = notificationSqlUtils.getNotificationsForSite(site, SelectQuery.ORDER_DESCENDING)
        assertEquals(6, notifications.size)
    }

    @Test
    fun testGetNotificationsForSite_storeOrder() {
        // Insert notification
        val notificationSqlUtils = NotificationSqlUtils(FormattableContentMapper(Gson()))
        val jsonString = UnitTestUtils
                .getStringFromResourceFile(this.javaClass, "notifications/store-order-notification.json")
        val apiResponse = NotificationTestUtils.parseNotificationApiResponseFromJsonString(jsonString)
        val notesList = listOf(NotificationApiResponse.notificationResponseToNotificationModel(apiResponse, site.id))
        val inserted = notesList.sumBy { notificationSqlUtils.insertOrUpdateNotification(it) }
        assertEquals(1, inserted)

        // Get notification from database
        val notifications = notificationSqlUtils.getNotificationsForSite(site, SelectQuery.ORDER_DESCENDING)
        assertEquals(1, notifications.size)

        // Verify properties
        val note = notifications[0]
        assertEquals(note.title, "New Order")
        assertEquals(note.noteHash, 2064099309)
        assertEquals(note.localSiteId, site.id)
        assertEquals(note.remoteNoteId, 3604874081)
        assertEquals(note.type, NotificationModel.Kind.STORE_ORDER)
        assertEquals(note.read, true)
        assertEquals(note.subtype, NotificationModel.Subkind.UNKNOWN)
        assertEquals(note.timestamp, "2018-10-22T21:08:11+00:00")
        assertEquals(note.icon, "https://s.wp.com/wp-content/mu-plugins/notes/images/update-payment-2x.png")
        assertEquals(note.url, "https://wordpress.com/store/order/droidtester2018.shop/88")
        assertNotNull(note.subject)
        note.subject?.let {
            assertEquals(it[0].text, "\ud83c\udf89 You have a new order!")
            assertEquals(it[1].text, "Someone placed a $18.00 order from Woo Test Store")
            assertNotNull(it[1].ranges)
            assertEquals(it[1].ranges!!.size, 1)
            assertEquals(it[1].ranges!![0].type, "site")
            assertEquals(it[1].ranges!![0].indices!!.size, 2)
        }
        assertNotNull(note.body)
        note.body?.let { body ->
            assertEquals(body.size, 4)
            with(body[0]) {
                assertEquals(text, "")
                assertNotNull(media)
                media?.let { media ->
                    assertEquals(media[0].type, "image")
                    assertEquals(media.size, 1)
                    assertEquals(media[0].height, "48")
                    assertEquals(media[0].width, "48")
                    assertEquals(
                            media[0].url,
                            "https://s.wp.com/wp-content/mu-plugins/notes/images/store-cart-icon.png")
                }
            }
            with(body[1]) {
                assertEquals(text, "Order Number: 88\nDate: October 22, 2018\nTotal: " +
                        "$18.00\nPayment Method: Credit Card (Stripe)")
            }
            with(body[2]) {
                assertEquals(text, "Products:\n\nBeanie \u00d7 1\n")
            }
            with(body[3]) {
                assertEquals(text, "\u2139\ufe0f View Order")
                assertNotNull(ranges)
                assertEquals(ranges!!.size, 1)
            }
        }
        assertNotNull(note.meta)
        with(note.meta!!) {
            // verify ids
            assertNotNull(ids)
            assertEquals(ids!!.site, 141286411)
            assertEquals(ids!!.order, 88)

            // verify links
            assertNotNull(links)
            assertEquals(links!!.site, "https://public-api.wordpress.com/rest/v1/sites/141286411")
            assertEquals(links!!.order, "https://public-api.wordpress.com/rest/v1/orders/88")
        }
    }

    @Test
    fun testGetNotificationsForSite_storeReview() {
        // Insert notification
        val notificationSqlUtils = NotificationSqlUtils(FormattableContentMapper(Gson()))
        val jsonString = UnitTestUtils
                .getStringFromResourceFile(this.javaClass, "notifications/store-review-notification.json")
        val apiResponse = NotificationTestUtils.parseNotificationApiResponseFromJsonString(jsonString)
        val notesList = listOf(NotificationApiResponse.notificationResponseToNotificationModel(apiResponse, site.id))
        val inserted = notesList.sumBy { notificationSqlUtils.insertOrUpdateNotification(it) }
        assertEquals(1, inserted)

        // Get notification from database
        val notifications = notificationSqlUtils.getNotificationsForSite(site, SelectQuery.ORDER_DESCENDING)
        assertEquals(1, notifications.size)

        // Verify properties
        // Since we do a full test of all properties in the previous test method, only check high-level properties
        // here.
        val note = notifications[0]
        assertEquals(note.noteHash, 1543255567)
        assertEquals(note.title, "Product Review")
        assertEquals(note.localSiteId, site.id)
        assertEquals(note.remoteNoteId, 3617558725)
        assertEquals(note.type, NotificationModel.Kind.COMMENT)
        assertEquals(note.read, true)
        assertEquals(note.subtype, NotificationModel.Subkind.STORE_REVIEW)
        assertEquals(note.timestamp, "2018-10-30T16:22:11+00:00")
        assertEquals(note.icon, "https://2.gravatar.com/avatar/" +
                "ebab642c3eb6022e6986f9dcf3147c1e?s=256&d=https%3A%2F%2Fsecure.gravatar.com%2F" +
                "avatar%2Fad516503a11cd5ca435acc9bb6523536%3Fs%3D256&r=G")
        assertEquals(note.url, "https://testwooshop.mystagingwebsite.com/product/ninja-hoodie/#comment-2716")
        assertNotNull(note.subject)
        assertEquals(note.subject!!.size, 2)
        assertEquals(note.body!!.size, 3)
    }

    @Test
    fun testGetNotifications_filterBy() {
        // Insert notifications
        val notificationSqlUtils = NotificationSqlUtils(FormattableContentMapper(Gson()))
        generateAndSaveTestNotificationsToDb(notificationSqlUtils, site)

        // Get notifications of type "store_order"
        val newOrderNotifications = notificationSqlUtils.getNotifications(
                filterByType = listOf(NotificationModel.Kind.STORE_ORDER.toString()))
        assertEquals(2, newOrderNotifications.size)

        // Get notifications of subtype "store_review"
        val storeReviewNotifications = notificationSqlUtils.getNotifications(
                filterBySubtype = listOf(NotificationModel.Subkind.STORE_REVIEW.toString()))
        assertEquals(2, storeReviewNotifications.size)

        // Get notifications of type "store_order" or subtype "store_review"
        val combinedNotifications = notificationSqlUtils.getNotifications(
                filterByType = listOf(NotificationModel.Kind.STORE_ORDER.toString()),
                filterBySubtype = listOf(NotificationModel.Subkind.STORE_REVIEW.toString()))
        assertEquals(4, combinedNotifications.size)
    }

    @Test
    fun testGetNotificationsForSite_filterBy() {
        // Insert notifications for main test site
        val notificationSqlUtils = NotificationSqlUtils(FormattableContentMapper(Gson()))
        generateAndSaveTestNotificationsToDb(notificationSqlUtils, site)

        // Insert notifications for additional test site
        val site2 = SiteModel().apply {
            name = "Test Site 2"
            url = "https://someothertestsite.com"
        }
        saveSiteToDb(site2)
        generateAndSaveTestNotificationsToDb(notificationSqlUtils, site2)

        // Verify we have 12 total notifications in the db
        val allNotifs = notificationSqlUtils.getNotifications()
        assertEquals(12, allNotifs.size)

        // Get notifications of type "store_order".
        //
        // Note: FOUR store_order notifications were inserted into the db, but they belong to two
        // different sites so only 2 should be returned.
        val newOrderNotifications = notificationSqlUtils.getNotificationsForSite(
                site2,
                filterByType = listOf(NotificationModel.Kind.STORE_ORDER.toString()))
        assertEquals(2, newOrderNotifications.size)

        // Get notifications of subtype "store_review"
        val storeReviewNotifications = notificationSqlUtils.getNotificationsForSite(
                site2,
                filterBySubtype = listOf(NotificationModel.Subkind.STORE_REVIEW.toString()))
        assertEquals(2, storeReviewNotifications.size)

        // Get notifications of type "store_order" or subtype "store_review"
        val combinedNotifications = notificationSqlUtils.getNotificationsForSite(
                site2,
                filterByType = listOf(NotificationModel.Kind.STORE_ORDER.toString()),
                filterBySubtype = listOf(NotificationModel.Subkind.STORE_REVIEW.toString()))
        assertEquals(4, combinedNotifications.size)
    }

    @Test
    fun testGetNotificationByIdSet() {
        val noteId = 3616322875

        // Insert notifications
        val notificationSqlUtils = NotificationSqlUtils(FormattableContentMapper(Gson()))
        generateAndSaveTestNotificationsToDb(notificationSqlUtils, site)

        // Fetch a single notification using the noteIdSet
        val idSet = NoteIdSet(-1, noteId, site.id)
        val notification = notificationSqlUtils.getNotificationByIdSet(idSet)
        assertNotNull(notification)

        with(notification) {
            assertEquals(remoteNoteId, noteId)
            assertEquals(localSiteId, site.id)
        }
    }

    @Test
    fun testGetNotificationByRemoteId() {
        val noteId = 3616322875

        // Insert notifications
        val notificationSqlUtils = NotificationSqlUtils(FormattableContentMapper(Gson()))
        generateAndSaveTestNotificationsToDb(notificationSqlUtils, site)

        // Fetch a single notification using the remoteNoteId
        val notification = notificationSqlUtils.getNotificationByRemoteId(noteId)
        assertNotNull(notification)

        with(notification) {
            assertEquals(remoteNoteId, noteId)
            assertEquals(localSiteId, site.id)
        }
    }

    @Test
    fun testGetNotificationCount() {
        // Insert notifications
        val notificationSqlUtils = NotificationSqlUtils(FormattableContentMapper(Gson()))
        generateAndSaveTestNotificationsToDb(notificationSqlUtils, site)

        // Get notifications
        val count = notificationSqlUtils.getNotificationsCount()
        assertEquals(6, count)
    }

    @Test
    fun testHasUnreadNotifications() {
        val notificationSqlUtils = NotificationSqlUtils(FormattableContentMapper(Gson()))
        generateAndSaveTestNotificationsToDb(notificationSqlUtils, site)

        val hasUnread = notificationSqlUtils.hasUnreadNotificationsForSite(site)
        assertEquals(hasUnread, true)
    }

    @Test
    fun testDeleteNotificationByRemoteId() {
        val noteId = 3616322875

        // Insert notifications
        val notificationSqlUtils = NotificationSqlUtils(FormattableContentMapper(Gson()))
        generateAndSaveTestNotificationsToDb(notificationSqlUtils, site)

        // Fetch a single notification
        val notification = notificationSqlUtils.getNotificationByRemoteId(noteId)
        assertNotNull(notification)

        // Delete notification by remoteNoteId
        val rowsAffected = notificationSqlUtils.deleteNotificationByRemoteId(noteId)
        assertEquals(rowsAffected, 1)

        // Verify notification not in database
        assertNull(notificationSqlUtils.getNotificationByRemoteId(noteId))
    }

    @Test
    fun testDeleteSiteCascadesToNotificationTable() {
        // Assemble:
        // - Insert notifications for main test site
        // - Insert notifications for additional test site
        // - Verify we have 12 total notifications in the db
        val notificationSqlUtils = NotificationSqlUtils(FormattableContentMapper(Gson()))
        generateAndSaveTestNotificationsToDb(notificationSqlUtils, site)

        val site2 = SiteModel().apply {
            name = "Test Site 2"
            url = "https://someothertestsite.com"
        }
        saveSiteToDb(site2)
        generateAndSaveTestNotificationsToDb(notificationSqlUtils, site2)

        val allNotifs = notificationSqlUtils.getNotifications()
        assertEquals(12, allNotifs.size)

        // Act:
        // - Delete the site from the database
        SiteSqlUtils.deleteSite(site2)

        // Assert:
        // - Verify all the notifications for site2 have also been deleted from the database
        val savedNotifs = notificationSqlUtils.getNotifications()
        assertEquals(6, savedNotifs.size)
    }

    private fun generateAndSaveTestNotificationsToDb(sqlUtils: NotificationSqlUtils, siteModel: SiteModel) {
        val jsonString = UnitTestUtils
                .getStringFromResourceFile(this.javaClass, "notifications/notifications-api-response.json")
        val apiResponse = NotificationTestUtils.parseNotificationsApiResponseFromJsonString(jsonString)
        val notesList = apiResponse.notes?.map {
            NotificationApiResponse.notificationResponseToNotificationModel(it, siteModel.id)
        } ?: emptyList()
        val inserted = notesList.sumBy { sqlUtils.insertOrUpdateNotification(it) }
        assertEquals(6, inserted)
    }

    private fun saveSiteToDb(siteModel: SiteModel) {
        val siteRowsAffected = SiteSqlUtils.insertOrUpdateSite(siteModel)
        assertEquals(1, siteRowsAffected)
        val savedSite = SiteSqlUtils.getSitesByNameOrUrlMatching(siteModel.name).firstOrNull()
        assertNotNull(savedSite)
        assertEquals(siteModel.name, savedSite.name)
    }
}
