package org.wordpress.android.fluxc.list

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.list.datastore.InternalPagedListDataStore
import org.wordpress.android.fluxc.model.list.datastore.ListItemDataStoreInterface
import kotlin.test.assertEquals

private const val NUMBER_OF_ITEMS = 71
private const val IS_LIST_FULLY_FETCHED = false
private val testListDescriptor = TestListDescriptor()
private val testStartAndEndPosition = Pair(5, 10)

class InternalPagedListDataStoreTest {
    private val remoteItemIds = mock<List<RemoteId>>()
    private val mockIdentifiers = mock<List<TestListIdentifier>>()
    private val mockItemDataStore = mock<ListItemDataStoreInterface<TestListDescriptor, TestListIdentifier, String>>()

    @Before
    fun setup() {
        whenever(remoteItemIds.size).thenReturn(NUMBER_OF_ITEMS)
        whenever(mockIdentifiers.size).thenReturn(NUMBER_OF_ITEMS)
        val mockSublist = mock<List<TestListIdentifier>>()
        whenever(mockIdentifiers.subList(any(), any())).thenReturn(mockSublist)

        whenever(
                mockItemDataStore.getItemIdentifiers(
                        listDescriptor = testListDescriptor,
                        remoteItemIds = remoteItemIds,
                        isListFullyFetched = IS_LIST_FULLY_FETCHED
                )
        ).thenReturn(mockIdentifiers)
    }

    /**
     * Tests that item identifiers are cached when a new instance of [InternalPagedListDataStore] is created.
     *
     * Caching the item identifiers is how we ensure that this component will provide consistent data to
     * `PositionalDataSource` so it's very important that we have this test. Since we don't have access to
     * `InternalPagedListDataStore.itemIdentifiers` private property, we have to test the internal implementation
     * which is more likely to break. However, in this specific case, we DO want the test to break if the internal
     * implementation changes.
     */
    @Test
    fun `init calls getItemIdentifiers`() {
        createInternalPagedListDataStore(mockItemDataStore)

        verify(mockItemDataStore).getItemIdentifiers(eq(testListDescriptor), any(), any())
    }

    @Test
    fun `total size uses getItemIdentifiers' size`() {
        val internalDataStore = createInternalPagedListDataStore(mockItemDataStore)
        assertEquals(
                NUMBER_OF_ITEMS, internalDataStore.totalSize, "InternalPagedListDataStore should not change the" +
                "number of items in a list and should propagate that to its ListItemDataStoreInterface"
        )
    }

    @Test
    fun `getItemsInRange creates the correct sublist of the identifiers`() {
        val internalDataStore = createInternalPagedListDataStore(mockItemDataStore)

        val (startPosition, endPosition) = testStartAndEndPosition
        internalDataStore.getItemsInRange(startPosition, endPosition)

        verify(mockIdentifiers).subList(startPosition, endPosition)
    }

    @Test
    fun `getItemsInRange propagates the call to getItemsAndFetchIfNecessary correctly`() {
        val internalDataStore = createInternalPagedListDataStore(dataStore = mockItemDataStore)

        val (startPosition, endPosition) = testStartAndEndPosition
        internalDataStore.getItemsInRange(startPosition, endPosition)

        verify(mockItemDataStore).getItemsAndFetchIfNecessary(eq(testListDescriptor), any())
    }

    private fun createInternalPagedListDataStore(
        dataStore: TestListItemDataStore
    ): TestInternalPagedListDataStore {
        return InternalPagedListDataStore(
                listDescriptor = testListDescriptor,
                remoteItemIds = remoteItemIds,
                isListFullyFetched = IS_LIST_FULLY_FETCHED,
                itemDataStore = dataStore
        )
    }
}
