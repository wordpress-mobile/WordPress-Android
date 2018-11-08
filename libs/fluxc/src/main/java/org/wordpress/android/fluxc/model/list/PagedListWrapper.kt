package org.wordpress.android.fluxc.model.list

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.OnLifecycleEvent
import android.arch.paging.PagedList
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.ListStore.ListError
import org.wordpress.android.fluxc.store.ListStore.OnListChanged
import org.wordpress.android.fluxc.store.ListStore.OnListItemsChanged
import org.wordpress.android.fluxc.store.ListStore.OnListStateChanged

class PagedListWrapper<T>(
    val dispatcher: Dispatcher,
    val listDescriptor: ListDescriptor,
    val data: LiveData<PagedList<PagedListItemType<T>>>,
    val lifecycle: Lifecycle,
    private val refresh: () -> Unit,
    private val invalidate: () -> Unit,
    private val isListEmpty: () -> Boolean
) : LifecycleObserver {
    private var isRegistered = true

    private val _isFetchingFirstPage = MutableLiveData<Boolean>()
    val isFetchingFirstPage: LiveData<Boolean> = _isFetchingFirstPage

    private val _isLoadingMore = MutableLiveData<Boolean>()
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _listError = MutableLiveData<ListError?>()
    val listError: LiveData<ListError?> = _listError

    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    init {
        dispatcher.register(this)
        lifecycle.addObserver(this)
    }

    fun fetchFirstPage() {
        refresh()
    }

    fun invalidateData() {
        invalidate()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        if (isRegistered) {
            lifecycle.removeObserver(this)
            dispatcher.unregister(this)
            isRegistered = false
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onListStateChanged(event: OnListStateChanged) {
        if (event.listDescriptor != listDescriptor) {
            return
        }
        _isFetchingFirstPage.postValue(event.newState.isFetchingFirstPage())
        _isLoadingMore.postValue(event.newState.isLoadingMore())
        _listError.postValue(event.error)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onListChanged(event: OnListChanged) {
        if (!event.listDescriptors.contains(listDescriptor)) {
            return
        }
        invalidateData()
        postIsEmpty()
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onListItemsChanged(event: OnListItemsChanged) {
        if (listDescriptor.typeIdentifier != event.type) {
            return
        }
        invalidateData()
        postIsEmpty()
    }

    private fun postIsEmpty() {
        _isEmpty.postValue(isListEmpty())
    }
}
