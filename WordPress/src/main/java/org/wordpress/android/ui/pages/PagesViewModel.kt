package org.wordpress.android.ui.pages

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.Action.PUBLISH_NOW
import org.wordpress.android.ui.pages.PageItem.Action.VIEW_PAGE
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.Page
import javax.inject.Inject

class PagesViewModel
@Inject constructor() : ViewModel() {
    private val mutableSearchExpanded: MutableLiveData<Boolean> = MutableLiveData()
    private val mutableSearchResult: MutableLiveData<List<PageItem>> = MutableLiveData()
    val searchExpanded: LiveData<Boolean> = mutableSearchExpanded
    val searchResult: LiveData<List<PageItem>> = mutableSearchResult

    fun onSearchTextSubmit(query: String?): Boolean {
        val listOf = mockResult(query)
        mutableSearchResult.postValue(listOf)
        return true
    }

    fun onSearchTextChange(newText: String?): Boolean {
        val listOf = mockResult(newText)
        mutableSearchResult.postValue(listOf)
        return true
    }

    fun searchExpanded(): Boolean {
        mutableSearchExpanded.postValue(true)
        return true
    }

    fun searchCollapsed(): Boolean {
        mutableSearchExpanded.postValue(false)
        return true
    }

    fun refresh() {
    }

    fun onAction(action: Action, pageItem: PageItem): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun mockResult(query: String?): List<PageItem> {
    // TODO remove with real data
    return listOf(
            Divider(1, "Data for $query"),
            Page(1, "item 1", null, 0, setOf(VIEW_PAGE)),
            Page(2, "item 2", null, 1),
            Page(3, "item 3", null, 2, setOf(VIEW_PAGE, PUBLISH_NOW)),
            Divider(2, "Divider 2"),
            Page(4, "item 4", null, 0)
    )
}
