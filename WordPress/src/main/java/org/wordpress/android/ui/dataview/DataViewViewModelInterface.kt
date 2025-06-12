package org.wordpress.android.ui.dataview

/**
 * ViewModels for [DataViewItem] screens should implement this interface.
 * This isn't enforced but adhering to it will lead to more consistency.
 */
interface DataViewViewModelInterface {
    fun onItemClick(item: DataViewItem)
    fun onFilterClick(filter: DataViewItemFilter?)
    fun onSearchQueryChange(query: String)

    fun getSupportedFilters(): List<DataViewItemFilter>

    fun fetchData()
    fun refreshData()
}
