package org.wordpress.android.fluxc.list

import androidx.paging.PagingSource.LoadParams.Refresh
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.list.PagedListFactory

internal class PagedListFactoryTest {
    @Test
    fun `first load triggers create data source`() = runBlocking<Unit> {
        val mockCreateDataSource = mock<() -> TestInternalPagedListDataSource>()
        whenever(mockCreateDataSource.invoke()).thenReturn(mock())
        val pagedListFactory = PagedListFactory(mockCreateDataSource)

        pagedListFactory.create().load(Refresh(key = null, loadSize = 1, placeholdersEnabled = true))

        verify(mockCreateDataSource, times(1)).invoke()
    }

    @Test
    fun `invalidate invalidates the current paging source`() {
        val mockCreateDataSource = mock<() -> TestInternalPagedListDataSource>()
        val invalidatedCallback = mock<() -> Unit>()

        val pagedListFactory = PagedListFactory(mockCreateDataSource)
        val currentSource = pagedListFactory.create()
        currentSource.registerInvalidatedCallback(invalidatedCallback)

        pagedListFactory.invalidate()

        verify(invalidatedCallback, times(1)).invoke()
    }
}
