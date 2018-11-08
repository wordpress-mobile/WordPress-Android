package org.wordpress.android.fluxc.model.list

import android.arch.lifecycle.LiveData
import android.arch.paging.PagedList

class PagedListWrapper<T>(
    val data: LiveData<PagedList<PagedListItemType<T>>>,
    val refresh: () -> Unit,
    val invalidate: () -> Unit
)
