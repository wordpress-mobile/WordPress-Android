package org.wordpress.android.fluxc.utils

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.model.list.ListItemModel
import org.wordpress.android.fluxc.model.list.ListModel
import org.wordpress.android.fluxc.model.list.ListModel.ListType
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.ListItemSqlUtils
import org.wordpress.android.fluxc.persistence.ListSqlUtils
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.site.SiteUtils.generateSelfHostedNonJPSite
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class ListItemSqlUtilsTest {
    private lateinit var listSqlUtils: ListSqlUtils
    private lateinit var listItemSqlUtils: ListItemSqlUtils

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = WellSqlConfig(appContext)
        WellSql.init(config)
        config.reset()

        listSqlUtils = ListSqlUtils()
        listItemSqlUtils = ListItemSqlUtils()
    }

    @Test
    fun testInsertOrUpdateItemList() {
        val testSite = generateAndInsertSelfHostedNonJPTestSite()
        val count = 20 // value doesn't matter
        val listType = ListType.POSTS_ALL

        /**
         * 1. Since a [ListItemModel] requires a [ListModel] in the DB due to the foreign key restriction, a test list
         * will be inserted in the DB.
         * 2. A list of test [ListItemModel]s will be generated and inserted in the DB
         * 3. Verify that the [ListItemModel] instances are inserted correctly
         */
        val testList = insertTestList(testSite.id, listType)
        val itemList = generateItemList(testList, count)
        listItemSqlUtils.insertItemList(itemList)
        assertEquals(count, listItemSqlUtils.getListItems(testList.id).size)
    }

    @Test
    fun testListIdForeignKeyCascadeDelete() {
        val testSite = generateAndInsertSelfHostedNonJPTestSite()
        val count = 20 // value doesn't matter
        val listType = ListType.POSTS_ALL

        /**
         * 1. Since a [ListItemModel] requires a [ListModel] in the DB due to the foreign key restriction, a test list
         * will be inserted in the DB.
         * 2. A list of test [ListItemModel]s will be generated and inserted in the DB
         * 3. Verify that the [ListItemModel] instances are inserted correctly
         */
        val testList = insertTestList(testSite.id, listType)
        val itemList = generateItemList(testList, count)
        listItemSqlUtils.insertItemList(itemList)
        assertEquals(count, listItemSqlUtils.getListItems(testList.id).size)

        /**
         * 1. Delete the inserted list
         * 2. Verify that deleting the list also deletes the inserted [ListItemModel]s due to foreign key restriction
         */
        listSqlUtils.deleteList(testSite.id, listType)
        assertEquals(0, listItemSqlUtils.getListItems(testList.id).size)
    }

    @Test
    fun testDeleteItem() {
        val testSite = generateAndInsertSelfHostedNonJPTestSite()
        val testRemoteItemId = 1245L // value doesn't matter

        /**
         * 1. Insert a test list for every list type. There should at least be 2 list types for a good test.
         * 2. Generate a [ListItemModel] for every list type with the same id and insert it
         * 3. Verify that the [ListItemModel] was inserted correctly
         */
        val testLists = ListType.values().map { insertTestList(testSite.id, it) }
        val itemList = testLists.map { generateListItemModel(it.id, testRemoteItemId) }
        listItemSqlUtils.insertItemList(itemList)
        testLists.forEach { list ->
            assertEquals(1, listItemSqlUtils.getListItems(list.id).size)
        }

        /**
         * 1. Delete [ListItemModel]s for which [ListItemModel.remoteItemId] == `testRemoteItemId`
         * 2. Verify that [ListItemModel]s from every list is deleted
         */
        listItemSqlUtils.deleteItem(testLists.map { it.id }, testRemoteItemId)
        testLists.forEach {
            assertEquals(0, listItemSqlUtils.getListItems(it.id).size)
        }
    }

    @Test
    fun insertDuplicateListItemModel() {
        val testSite = generateAndInsertSelfHostedNonJPTestSite()
        val testRemoteItemId = 1245L // value doesn't matter
        val listType = ListType.POSTS_ALL

        /**
         * 1. Since a [ListItemModel] requires a [ListModel] in the DB due to the foreign key restriction, a test list
         * will be inserted in the DB.
         * 2. Generate 2 [ListItemModel]s with the exact same values and insert the first one in the DB
         * 3. Verify that first [ListItemModel] is inserted correctly
         */
        val testList = insertTestList(testSite.id, listType)
        val listItemModel = generateListItemModel(testList.id, testRemoteItemId)
        val listItemModel2 = generateListItemModel(testList.id, testRemoteItemId)
        listItemSqlUtils.insertItemList(listOf(listItemModel))
        val insertedItemList = listItemSqlUtils.getListItems(testList.id)
        assertEquals(1, insertedItemList.size)

        /**
         * 1. Insert the second [ListItemModel] in the DB
         * 2. Verify that no new record is created and the list size is the same.
         * 3. Verify that the [ListItemModel.id] has not changed
         */
        listItemSqlUtils.insertItemList(listOf(listItemModel2))
        val updatedItemList = listItemSqlUtils.getListItems(testList.id)
        assertEquals(1, updatedItemList.size)
        assertEquals(insertedItemList[0].id, updatedItemList[0].id)
    }

    /**
     * Creates and inserts a [ListModel] for the given site. It also verifies that the list is inserted correctly.
     */
    private fun insertTestList(localSiteId: Int, listType: ListType): ListModel {
        listSqlUtils.insertOrUpdateList(localSiteId, listType)
        val list = listSqlUtils.getList(localSiteId, listType)
        assertNotNull(list)
        return list!!
    }

    /**
     * Helper function that creates a list of [ListItemModel] to be used in tests.
     */
    private fun generateItemList(listModel: ListModel, count: Int): List<ListItemModel> =
            (1..count).map { generateListItemModel(listModel.id, it.toLong()) }

    /**
     * Helper function that generates a [ListItemModel] instance.
     */
    private fun generateListItemModel(
        listId: Int,
        remoteItemId: Long
    ): ListItemModel {
        val listItemModel = ListItemModel()
        listItemModel.listId = listId
        listItemModel.remoteItemId = remoteItemId
        return listItemModel
    }

    /**
     * Helper function that generates a self-hosted test site and inserts it into the DB. Since we have a FK restriction
     * for [ListModel.localSiteId] we need to do this before we can insert [ListModel] instances.
     */
    private fun generateAndInsertSelfHostedNonJPTestSite(): SiteModel {
        val site = generateSelfHostedNonJPSite()
        SiteSqlUtils.insertOrUpdateSite(site)
        return site
    }
}
