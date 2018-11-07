package org.wordpress.android.fluxc.model.list

import android.arch.lifecycle.LiveData
import android.arch.paging.PagedList

class PagedListWrapper<T>(val liveData: LiveData<PagedList<PagedListItemType<T>>>, val invalidate: () -> Unit)
