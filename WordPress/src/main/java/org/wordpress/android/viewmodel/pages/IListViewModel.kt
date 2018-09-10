package org.wordpress.android.viewmodel.pages

import android.arch.lifecycle.LiveData
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.Page

interface IListViewModel {
    val pages: LiveData<List<PageItem>>

    fun onMenuAction(action: Action, pageItem: Page): Boolean
    fun onItemTapped(pageItem: Page)
}
