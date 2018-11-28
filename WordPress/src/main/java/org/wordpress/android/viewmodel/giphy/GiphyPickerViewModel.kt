package org.wordpress.android.viewmodel.giphy

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList

/**
 * Holds the data for [GiphyPickerViewModel]
 *
 * This creates a [PagedList] which can be bound to by a [PagedListAdapter].
 */
class GiphyPickerViewModel : ViewModel() {
    private val dataSourceFactory = GiphyPickerDataSourceFactory()

    val mediaViewModelPagedList: LiveData<PagedList<GiphyMediaViewModel>> by lazy {
        val pagedListConfig = PagedList.Config.Builder().setEnablePlaceholders(true).setPageSize(30).build()
        LivePagedListBuilder(dataSourceFactory, pagedListConfig).build()
    }

    fun search(searchQuery: String) = dataSourceFactory.setSearchQuery(searchQuery)
}
