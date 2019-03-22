package org.wordpress.android.fluxc.list

import android.arch.paging.DataSource.InvalidatedCallback
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Test
import org.wordpress.android.fluxc.model.list.PagedListFactory
import org.wordpress.android.fluxc.model.list.datastore.InternalPagedListDataStore

internal class PagedListFactoryTest {
    @Test
    fun `create factory triggers create data store`() {
        val mockCreateDataStore = mock<() -> InternalPagedListDataStore<String>>()
        whenever(mockCreateDataStore.invoke()).thenReturn(mock())
        val pagedListFactory = PagedListFactory(mockCreateDataStore)

        pagedListFactory.create()

        verify(mockCreateDataStore, times(1)).invoke()
    }

    @Test
    fun `invalidate triggers create data store`() {
        val mockCreateDataStore = mock<() -> InternalPagedListDataStore<String>>()
        whenever(mockCreateDataStore.invoke()).thenReturn(mock())
        val invalidatedCallback = mock<InvalidatedCallback>()

        val pagedListFactory = PagedListFactory(mockCreateDataStore)
        val currentSource = pagedListFactory.create()
        currentSource.addInvalidatedCallback(invalidatedCallback)

        pagedListFactory.invalidate()

        verify(invalidatedCallback, times(1)).onInvalidated()
    }
}
