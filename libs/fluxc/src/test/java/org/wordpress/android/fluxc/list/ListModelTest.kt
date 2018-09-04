package org.wordpress.android.fluxc.list

import org.junit.Assert.assertNull
import org.junit.Test
import org.wordpress.android.fluxc.model.list.BasicListOrder
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListFilter
import org.wordpress.android.fluxc.model.list.ListModel
import org.wordpress.android.fluxc.model.list.ListOrder
import org.wordpress.android.fluxc.model.list.ListType
import org.wordpress.android.fluxc.model.list.ListType.POST
import org.wordpress.android.fluxc.model.list.ListType.WOO_ORDER
import org.wordpress.android.fluxc.model.list.PostListFilter
import org.wordpress.android.fluxc.model.list.WooOrderListFilter
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ListModelTest {
    /**
     * Tests [ListType.fromValue] static function. Since changing the value of a list type requires a DB
     * migration, having this kind of test can help make sure of it.
     */
    @Test
    fun testListTypes() {
        assertEquals(POST, ListType.fromValue(100))
        assertEquals(WOO_ORDER, ListType.fromValue(101))
    }

    /**
     * Tests [ListFilter.fromValue] static function. It tries to ensure that [ListType]s and [ListFilter]s are
     * associated correctly.
     */
    @Test
    fun testListFilters() {
        assertEquals(PostListFilter.ALL, ListFilter.fromValue(POST, "all"))
        assertEquals(WooOrderListFilter.ALL, ListFilter.fromValue(WOO_ORDER, "all"))
        assertNull(ListFilter.fromValue(POST, "doesn't exist"))
    }

    /**
     * Tests [ListOrder.fromValue] for [BasicListOrder].
     */
    @Test
    fun testBasicListOrder() {
        assertEquals(BasicListOrder.ASC, ListOrder.fromValue(POST, "asc"))
        assertEquals(BasicListOrder.DESC, ListOrder.fromValue(WOO_ORDER, "desc"))
    }

    /**
     * Tests [ListDescriptor.equals]. We are using a data class for [ListDescriptor], so we normally shouldn't need
     * this test. However, a lot of our list management logic depends on this and we need to ensure that the equality
     * works as expected. It's mostly added to prevent changing the `ListDescriptor` to regular class without
     * implementing the equality correctly.
     *
     * Note that the test is non-exhaustive both covers quite a bit of different cases.
     */
    @Test
    fun testListDescriptorEquals() {
        assertEquals(ListDescriptor(POST), ListDescriptor(POST))
        assertNotEquals(ListDescriptor(POST), ListDescriptor(WOO_ORDER))
        assertEquals(ListDescriptor(POST, 5), ListDescriptor(POST, 5))
        assertNotEquals(ListDescriptor(POST, 5), ListDescriptor(POST))
        assertEquals(ListDescriptor(POST, 5, PostListFilter.ALL), ListDescriptor(POST, 5, PostListFilter.ALL))
        assertNotEquals(ListDescriptor(POST, 5, PostListFilter.ALL), ListDescriptor(POST, 5))
        assertEquals(ListDescriptor(POST, 5, PostListFilter.ALL, BasicListOrder.ASC),
                ListDescriptor(POST, 5, PostListFilter.ALL, BasicListOrder.ASC))
        assertNotEquals(ListDescriptor(POST, 5, PostListFilter.ALL, BasicListOrder.ASC),
                ListDescriptor(POST, 5, PostListFilter.ALL))
        assertEquals(ListDescriptor(POST, 5, order = BasicListOrder.ASC),
                ListDescriptor(POST, 5, order = BasicListOrder.ASC))
    }

    /**
     * Tests [ListModel.setListDescriptor]. Since values of the enums are saved and then converted back in the getter
     * this acts as a good test to cover the basic scenarios.
     */
    @Test
    fun testListModelSetListDescriptor() {
        assertSetListDescriptor(ListDescriptor(POST, 555, PostListFilter.ALL, BasicListOrder.ASC))
        assertSetListDescriptor(ListDescriptor(POST, filter =  PostListFilter.ALL))
        assertSetListDescriptor(ListDescriptor(POST, order =  BasicListOrder.ASC))
        assertSetListDescriptor(ListDescriptor(POST, filter =  PostListFilter.ALL, order = BasicListOrder.ASC))
    }

    private fun assertSetListDescriptor(listDescriptor: ListDescriptor) {
        val listModel = ListModel()
        listModel.setListDescriptor(listDescriptor)
        assertEquals(listDescriptor, listModel.listDescriptor)
    }
}
