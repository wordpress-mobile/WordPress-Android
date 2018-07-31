package org.wordpress.android.fluxc.utils

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.model.ListModel
import org.wordpress.android.fluxc.model.ListModel.ListType
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.ListSqlUtils
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.site.SiteUtils
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ListSqlUtilsTest {
    private lateinit var listSqlUtils: ListSqlUtils

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = WellSqlConfig(appContext)
        WellSql.init(config)
        config.reset()

        listSqlUtils = ListSqlUtils()
    }

    @Test
    fun testInsertOrUpdateList() {
        val testSite = generateAndInsertSelfHostedNonJPTestSite()
        val listType = ListType.POSTS_ALL

        /**
         * 1. Insert a new list for `testSite` and `listType`
         * 2. Verify that it's inserted
         * 3. Verify the `localSiteId` value
         * 4. Verify that `dateCreated` and `lastModified` are equal since this is the first time it's created
         */
        listSqlUtils.insertOrUpdateList(testSite.id, listType)
        val insertedList = listSqlUtils.getList(testSite.id, listType)
        assertNotNull(insertedList)
        assertEquals(testSite.id, insertedList?.localSiteId)
        assertEquals(insertedList?.dateCreated, insertedList?.lastModified)

        /**
         * 1. Wait 1 second before the update test to ensure `lastModified` value will be different
         * 2. Insert the same list which should update instead
         * 3. Verify that it's inserted
         * 4. Verify the `localSiteId` value
         * 5. Verify the `dateCreated` and `lastModified` values are different since this is an update. (See point 1)
         */
        Thread.sleep(1000)
        listSqlUtils.insertOrUpdateList(testSite.id, listType)
        val updatedList = listSqlUtils.getList(testSite.id, listType)
        assertNotNull(updatedList)
        assertEquals(testSite.id, updatedList?.localSiteId)
        assertNotEquals(updatedList?.dateCreated, updatedList?.lastModified)

        /**
         * Verify that initially created list and updated list has the same `dateCreated`
         */
        assertEquals(insertedList?.dateCreated, updatedList?.dateCreated)
    }

    @Test
    fun testDeleteList() {
        val testSite = generateAndInsertSelfHostedNonJPTestSite()
        val listType = ListType.POSTS_ALL

        /**
         * 1. Insert a test list
         * 2. Verify that the list is inserted correctly
         */
        listSqlUtils.insertOrUpdateList(testSite.id, listType)
        assertNotNull(listSqlUtils.getList(testSite.id, listType))

        /**
         * 1. Delete the inserted test list
         * 2. Verify that the list is deleted correctly
         */
        listSqlUtils.deleteList(testSite.id, listType)
        assertNull(listSqlUtils.getList(testSite.id, listType))
    }

    @Test
    fun testLocalSiteIdForeignKeyCascadeDelete() {
        val listType1 = ListType.POSTS_ALL
        val listType2 = ListType.POSTS_SCHEDULED
        /**
         * 1. Generate and insert a self-hosted test site
         * 2. Verify that the site is inserted
         * 3. Insert a 2 different [ListModel]s for that test site
         * 4. Verify that lists are inserted
         */
        val testSite = generateAndInsertSelfHostedNonJPTestSite()
        assertFalse(SiteSqlUtils.getSitesAccessedViaXMLRPC().asModel.isEmpty())
        listSqlUtils.insertOrUpdateList(testSite.id, listType1)
        listSqlUtils.insertOrUpdateList(testSite.id, listType2)
        assertNotNull(listSqlUtils.getList(testSite.id, listType1))
        assertNotNull(listSqlUtils.getList(testSite.id, listType2))

        /**
         * 1. Delete the test site
         * 2. Verify that test site is deleted
         * 3. Verify that both lists are deleted as a result of deleting the site
         */
        SiteSqlUtils.deleteSite(testSite)
        assertTrue(SiteSqlUtils.getSitesAccessedViaXMLRPC().asModel.isEmpty())
        assertNull(listSqlUtils.getList(testSite.id, listType1))
        assertNull(listSqlUtils.getList(testSite.id, listType2))
    }

    /**
     * Helper function that generates a self-hosted test site and inserts it into the DB. Since we have a FK restriction
     * for [ListModel.localSiteId] we need to do this before we can insert [ListModel] instances.
     */
    private fun generateAndInsertSelfHostedNonJPTestSite(): SiteModel {
        val site = SiteUtils.generateSelfHostedNonJPSite()
        SiteSqlUtils.insertOrUpdateSite(site)
        return site
    }
}
