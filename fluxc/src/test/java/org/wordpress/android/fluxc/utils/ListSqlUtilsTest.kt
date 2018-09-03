package org.wordpress.android.fluxc.utils

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListModel
import org.wordpress.android.fluxc.model.list.ListType.POST
import org.wordpress.android.fluxc.persistence.ListSqlUtils
import kotlin.test.assertEquals
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
    fun testInsertOrUpdateList() {
        val listDescriptor = ListDescriptor(POST, 333)

        /**
         * 1. Insert a new list for `testSite` and `listType`
         * 2. Verify that it's inserted
         * 3. Verify the `localSiteId` value
         * 4. Verify that `dateCreated` and `lastModified` are equal since this is the first time it's created
         */
        listSqlUtils.insertOrUpdateList(listDescriptor)
        val insertedList = listSqlUtils.getList(listDescriptor)
        assertNotNull(insertedList)
        assertEquals(listDescriptor, insertedList?.listDescriptor)
        assertEquals(insertedList?.dateCreated, insertedList?.lastModified)

        /**
         * 1. Wait 1 second before the update test to ensure `lastModified` value will be different
         * 2. Insert the same list which should update instead
         * 3. Verify that it's inserted
         * 4. Verify the `localSiteId` value
         * 5. Verify the `dateCreated` and `lastModified` values are different since this is an update. (See point 1)
         */
        Thread.sleep(1000)
        listSqlUtils.insertOrUpdateList(listDescriptor)
        val updatedList = listSqlUtils.getList(listDescriptor)
        assertNotNull(updatedList)
        assertEquals(listDescriptor, updatedList?.listDescriptor)
        assertNotEquals(updatedList?.dateCreated, updatedList?.lastModified)

        /**
         * Verify that initially created list and updated list has the same `dateCreated`
         */
        assertEquals(insertedList?.dateCreated, updatedList?.dateCreated)
    }

    @Test
    fun testDeleteList() {
        val listDescriptor = ListDescriptor(POST, 444)

        /**
         * 1. Insert a test list
         * 2. Verify that the list is inserted correctly
         * 3. Delete the inserted test list
         * 4. Verify that the list is deleted correctly
         */
        listSqlUtils.insertOrUpdateList(listDescriptor)
        assertNotNull(listSqlUtils.getList(listDescriptor))
        listSqlUtils.deleteList(listDescriptor)
        assertNull(listSqlUtils.getList(listDescriptor))
    }
}
