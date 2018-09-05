package org.wordpress.android.fluxc.list

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.list.BasicListOrder
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListModel
import org.wordpress.android.fluxc.model.list.ListType.POST
import org.wordpress.android.fluxc.model.list.ListType.WOO_ORDER
import org.wordpress.android.fluxc.model.list.PostListFilter
import org.wordpress.android.fluxc.model.list.WooOrderListFilter
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
    fun testInsertAndUpdateList() {
        val listDescriptor = ListDescriptor(POST, 333)
        /**
         * 1. Insert a test list
         * 2. Wait 1 second before the update to ensure `lastModified` value will be different
         * 3. Insert the same list which should update instead
         * 4. Verify the listDescriptors are the same
         * 5. Verify the `lastModified` values are different
         */
        val insertedList = insertOrUpdateAndThenAssertList(listDescriptor)
        Thread.sleep(1000)
        val updatedList = insertOrUpdateAndThenAssertList(listDescriptor)
        assertEquals(insertedList.listDescriptor, updatedList.listDescriptor)
        assertNotEquals(insertedList.lastModified, updatedList.lastModified)
    }

    @Test
    fun testInsertSeveralDifferentLists() {
        insertOrUpdateAndThenAssertList(ListDescriptor(POST))
        insertOrUpdateAndThenAssertList(ListDescriptor(WOO_ORDER))
        insertOrUpdateAndThenAssertList(ListDescriptor(POST, 123))
        insertOrUpdateAndThenAssertList(ListDescriptor(POST, 123, PostListFilter.ALL))
        insertOrUpdateAndThenAssertList(ListDescriptor(WOO_ORDER, 123, WooOrderListFilter.ALL, BasicListOrder.ASC))
        insertOrUpdateAndThenAssertList(ListDescriptor(POST, filter = PostListFilter.ALL))
        insertOrUpdateAndThenAssertList(ListDescriptor(POST, order = BasicListOrder.DESC))
        insertOrUpdateAndThenAssertList(
                ListDescriptor(WOO_ORDER, filter = WooOrderListFilter.ALL, order = BasicListOrder.DESC))
    }

    @Test
    fun testDeleteList() {
        val listDescriptor = ListDescriptor(POST, 444)
        /**
         * 1. Insert a test list
         * 2. Delete it
         * 3. Verify that it is deleted correctly
         */
        insertOrUpdateAndThenAssertList(listDescriptor)
        listSqlUtils.deleteList(listDescriptor)
        assertNull(listSqlUtils.getList(listDescriptor))
    }

    /**
     * Inserts or updates the list for the listDescriptor and asserts that it's inserted correctly
     */
    private fun insertOrUpdateAndThenAssertList(listDescriptor: ListDescriptor): ListModel {
        listSqlUtils.insertOrUpdateList(listDescriptor)
        val listModel = listSqlUtils.getList(listDescriptor)
        assertNotNull(listModel)
        assertEquals(listDescriptor, listModel?.listDescriptor)
        return listModel!!
    }
}
