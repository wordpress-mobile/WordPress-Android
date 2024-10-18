package org.wordpress.android.fluxc.list

import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.list.datasource.InternalPagedListDataSource
import org.wordpress.android.fluxc.model.list.datasource.ListItemDataSourceInterface
import kotlin.test.assertEquals

private const val NUMBER_OF_ITEMS = 71
private const val IS_LIST_FULLY_FETCHED = false
private val testListDescriptor = TestListDescriptor()
private val testStartAndEndPosition = Pair(5, 10)

internal class InternalPagedListDataSourceTest {
    private val remoteItemIds = mock<List<RemoteId>>()
    private val mockIdentifiers = mock<List<TestListIdentifier>>()
    private val mockItemDataSource = mock<ListItemDataSourceInterface<TestListDescriptor, TestListIdentifier, String>>()

    @Before
    fun setup() {
        whenever(remoteItemIds.size).thenReturn(NUMBER_OF_ITEMS)
        whenever(mockIdentifiers.size).thenReturn(NUMBER_OF_ITEMS)
        val mockSublist = mock<List<TestListIdentifier>>()
        whenever(mockIdentifiers.subList(any(), any())).thenReturn(mockSublist)

        whenever(
                mockItemDataSource.getItemIdentifiers(
                        listDescriptor = testListDescriptor,
                        remoteItemIds = remoteItemIds,
                        isListFullyFetched = IS_LIST_FULLY_FETCHED
                )
        ).thenReturn(mockIdentifiers)
    }

    /**
     * Tests that item identifiers are cached when a new instance of [InternalPagedListDataSource] is created.
     *
     * Caching the item identifiers is how we ensure that this component will provide consistent data to
     * `PositionalDataSource` so it's very important that we have this test. Since we don't have access to
     * `InternalPagedListDataSource.itemIdentifiers` private property, we have to test the internal implementation
     * which is more likely to break. However, in this specific case, we DO want the test to break if the internal
     * implementation changes.
     */
    @Test
    fun `init calls getItemIdentifiers`() {
        createInternalPagedListDataSource(mockItemDataSource)

        verify(mockItemDataSource).getItemIdentifiers(eq(testListDescriptor), any(), any())
    }

    @Test
    fun `total size uses getItemIdentifiers' size`() {
        val internalDataSource = createInternalPagedListDataSource(mockItemDataSource)
        assertEquals(
                NUMBER_OF_ITEMS, internalDataSource.totalSize, "InternalPagedListDataSource should not change the" +
                "number of items in a list and should propagate that to its ListItemDataSourceInterface"
        )
    }

    @Test
    fun `getItemsInRange creates the correct sublist of the identifiers`() {
        val internalDataSource = createInternalPagedListDataSource(mockItemDataSource)

        val (startPosition, endPosition) = testStartAndEndPosition
        internalDataSource.getItemsInRange(startPosition, endPosition)

        verify(mockIdentifiers).subList(startPosition, endPosition)
    }

    @Test
    fun `getItemsInRange propagates the call to getItemsAndFetchIfNecessary correctly`() {
        val internalDataSource = createInternalPagedListDataSource(dataSource = mockItemDataSource)

        val (startPosition, endPosition) = testStartAndEndPosition
        internalDataSource.getItemsInRange(startPosition, endPosition)

        verify(mockItemDataSource).getItemsAndFetchIfNecessary(eq(testListDescriptor), any())
    }

    private fun createInternalPagedListDataSource(
        dataSource: TestListItemDataSource
    ): TestInternalPagedListDataSource {
        return InternalPagedListDataSource(
                listDescriptor = testListDescriptor,
                remoteItemIds = remoteItemIds,
                isListFullyFetched = IS_LIST_FULLY_FETCHED,
                itemDataSource = dataSource
        )
    }
}
