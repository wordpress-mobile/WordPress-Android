package org.wordpress.android.fluxc.list

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListModel
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForRestSite
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForXmlRpcSite
import org.wordpress.android.fluxc.persistence.ListSqlUtils
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class ListSqlUtilsTest {
    private lateinit var listSqlUtils: ListSqlUtils

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(appContext, ListModel::class.java)
        WellSql.init(config)
        config.reset()

        listSqlUtils = ListSqlUtils()
    }

    @Test
    fun testInsertAndUpdateList() {
        val listDescriptor = PostListDescriptorForRestSite(testSite())
        /**
         * 1. Insert a test list
         * 2. Wait 1 second before the update to ensure `lastModified` value will be different
         * 3. Insert the same list which should update instead
         * 4. Verify the `lastModified` values are different
         */
        val insertedList = insertOrUpdateAndThenAssertList(listDescriptor)
        Thread.sleep(1000)
        val updatedList = insertOrUpdateAndThenAssertList(listDescriptor)
        assertNotEquals(insertedList.lastModified, updatedList.lastModified)
    }

    /**
     * Inserts several different lists to test different combinations for [ListDescriptor]
     */
    @Test
    fun testInsertSeveralDifferentLists() {
        insertOrUpdateAndThenAssertList(PostListDescriptorForRestSite(testSite()))
        insertOrUpdateAndThenAssertList(PostListDescriptorForXmlRpcSite(testSite()))
    }

    @Test
    fun testDeleteList() {
        val listDescriptor = PostListDescriptorForRestSite(testSite())
        /**
         * 1. Insert a test list
         * 2. Delete it
         * 3. Verify that it is deleted correctly
         */
        insertOrUpdateAndThenAssertList(listDescriptor)
        listSqlUtils.deleteList(listDescriptor)
        assertNull(listSqlUtils.getList(listDescriptor))
    }

    @Test
    fun testDeleteAllLists() {
        val listDescriptors = (1..10).map { PostListDescriptorForRestSite(testSite(it)) }
        /**
         * 1. Insert 10 different lists
         * 2. Delete all lists
         * 3. Verify that all of them are deleted correctly
         */
        listDescriptors.forEach { insertOrUpdateAndThenAssertList(it) }
        listSqlUtils.deleteAllLists()
        listDescriptors.forEach { assertNull(listSqlUtils.getList(it)) }
    }

    /**
     * Inserts or updates the list for the listDescriptor and asserts that it's inserted correctly
     */
    private fun insertOrUpdateAndThenAssertList(listDescriptor: ListDescriptor): ListModel {
        listSqlUtils.insertOrUpdateList(listDescriptor)
        val listModel = listSqlUtils.getList(listDescriptor)
        assertNotNull(listModel)
        return listModel!!
    }

    private fun testSite(localSiteId: Int = 123): SiteModel {
        val site = SiteModel()
        site.id = localSiteId
        return site
    }
}
