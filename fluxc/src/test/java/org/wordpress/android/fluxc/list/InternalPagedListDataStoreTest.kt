package org.wordpress.android.fluxc.list

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Test
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.list.datastore.InternalPagedListDataStore
import org.wordpress.android.fluxc.model.list.datastore.ListItemDataStoreInterface
import org.wordpress.android.fluxc.model.list.datastore.ListItemIdentifier
import kotlin.test.assertEquals

private const val NUMBER_OF_ITEMS = 71

class InternalPagedListDataStoreTest {
    @Test
    fun `init calls getItemIdentifiers`() {
        val mockItemDataStore = createMockedItemDataStore()

        createInternalPagedListDataStore(mockItemDataStore)

        verify(mockItemDataStore).getItemIdentifiers(any(), any())
    }

    @Test
    fun `total size uses getItemIdentifiers' size`() {
        val numberOfItemsToTest = NUMBER_OF_ITEMS
        val mockItemDataStore = createMockedItemDataStore(identifiers = createMockedIdentifiers(numberOfItemsToTest))

        val internalDataStore = createInternalPagedListDataStore(mockItemDataStore)
        assertEquals(
                numberOfItemsToTest, internalDataStore.totalSize, "InternalPagedListDataStore should not change the" +
                "number of items in a list and should propagate that to its ListItemDataStoreInterface"
        )
    }

    @Test
    fun `getItemsInRange creates the correct sublist of the identifiers`() {
        val mockIdentifiers = createMockedIdentifiers()
        val mockItemDataStore = createMockedItemDataStore(identifiers = mockIdentifiers)
        val internalDataStore = createInternalPagedListDataStore(mockItemDataStore)

        val (startPosition, endPosition) = Pair(5, 10)
        internalDataStore.getItemsInRange(startPosition, endPosition)

        verify(mockIdentifiers).subList(startPosition, endPosition)
    }

    @Test
    fun `getItemsInRange propagates the call to getItemsAndFetchIfNecessary correctly`() {
        val mockIdentifiers = createMockedIdentifiers()
        val mockSublist = mock<List<ListItemIdentifier>>()
        whenever(mockIdentifiers.subList(any(), any())).thenReturn(mockSublist)
        val mockItemDataStore = createMockedItemDataStore(identifiers = mockIdentifiers)
        val internalDataStore = createInternalPagedListDataStore(dataStore = mockItemDataStore)

        internalDataStore.getItemsInRange(any(), any())

        verify(mockItemDataStore).getItemsAndFetchIfNecessary(mockSublist)
    }

    private fun <T> createInternalPagedListDataStore(
        dataStore: ListItemDataStoreInterface<T>
    ): InternalPagedListDataStore<T> {
        val remoteIds = mock<List<RemoteId>>()
        val isListFullyFetched = false // should not matter
        return InternalPagedListDataStore(remoteIds, isListFullyFetched, dataStore)
    }

    private fun createMockedItemDataStore(
        identifiers: List<ListItemIdentifier>? = null
    ): ListItemDataStoreInterface<String> {
        val mockItemDataStore = mock<ListItemDataStoreInterface<String>>()
        identifiers?.let { whenever(mockItemDataStore.getItemIdentifiers(any(), any())).thenReturn(it) }
        return mockItemDataStore
    }

    private fun createMockedIdentifiers(numberOfItems: Int = NUMBER_OF_ITEMS): List<ListItemIdentifier> {
        val mockIdentifiers = mock<List<ListItemIdentifier>>()
        whenever(mockIdentifiers.size).thenReturn(numberOfItems)
        return mockIdentifiers
    }
}
