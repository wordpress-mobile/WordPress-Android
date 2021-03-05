package org.wordpress.android.fluxc.model.list

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import androidx.paging.PagedList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.ListStore.ListError
import org.wordpress.android.fluxc.store.ListStore.OnListChanged
import org.wordpress.android.fluxc.store.ListStore.OnListDataInvalidated
import org.wordpress.android.fluxc.store.ListStore.OnListRequiresRefresh
import org.wordpress.android.fluxc.store.ListStore.OnListStateChanged
import kotlin.coroutines.CoroutineContext

/**
 * This is a wrapper class to consume lists from `ListStore`.
 *
 * @property data A [LiveData] instance that provides the [PagedList] for the given item type [T].
 * @property isFetchingFirstPage A [LiveData] instance that tells the client whether the first page is currently being
 * fetched. It can be directly used in the UI.
 * @property isLoadingMore A [LiveData] instance that tells the client whether more data is currently being fetched. It
 * can be directly used in the UI.
 * @property listError A [LiveData] instance that tells whether the last fetch resulted in an error. It can be used
 * to either let the user know of each error or present the error in the empty view when it's visible.
 */
class PagedListWrapper<T>(
    val data: LiveData<PagedList<T>>,
    private val dispatcher: Dispatcher,
    private val listDescriptor: ListDescriptor,
    private val lifecycle: Lifecycle,
    private val refresh: () -> Unit,
    private val invalidate: () -> Unit,
    private val parentCoroutineContext: CoroutineContext
) : LifecycleObserver, CoroutineScope {
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = parentCoroutineContext + job

    private val _isFetchingFirstPage = MutableLiveData<Boolean>()
    val isFetchingFirstPage: LiveData<Boolean> = _isFetchingFirstPage

    private val _isLoadingMore = MutableLiveData<Boolean>()
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _listError = MutableLiveData<ListError?>()
    val listError: LiveData<ListError?> = _listError

    private val _isEmpty = MediatorLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    /**
     * Register the dispatcher so we can handle `ListStore` events and add an observer for the lifecycle so we can
     * cleanup properly in `onDestroy`.
     */
    init {
        _isEmpty.addSource(data) {
            _isEmpty.value = it?.isEmpty()
        }
        dispatcher.register(this)
        lifecycle.addObserver(this)
    }

    /**
     * Handles the [Lifecycle.Event.ON_DESTROY] event to cleanup the registration for dispatcher and removing the
     * observer for lifecycle.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
        lifecycle.removeObserver(this)
        dispatcher.unregister(this)
        job.cancel()
    }

    /**
     * A method to be used by clients to refresh the first page of a list from network.
     */
    fun fetchFirstPage() {
        launch {
            refresh()
        }
    }

    /**
     * A method to be used by clients to tell the data needs to be reloaded and recalculated since there was a change
     * to at least one of the objects in the list. In most cases this should be used for changes where the depending
     * data of an object changes, such as a change to the upload status of a post. Changes to the actual data
     * should be managed through `ListStore` and shouldn't be necessary to be handled by clients.
     */
    fun invalidateData() {
        invalidate()
    }

    /**
     * Handles the [OnListStateChanged] `ListStore` event. It'll update the state information related [LiveData]
     * instances.
     */
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

    /**
     * Handles the [OnListChanged] `ListStore` event. It'll invalidate the data, so it can be reloaded. It'll also
     * updates whether the list is empty or not.
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onListChanged(event: OnListChanged) {
        if (!event.listDescriptors.contains(listDescriptor)) {
            return
        }
        invalidateData()
    }

    /**
     * Handles the [OnListRequiresRefresh] `ListStore` event. It'll refresh the list if the type of this list matches
     * the type of list that requires a refresh.
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onListRequiresRefresh(event: OnListRequiresRefresh) {
        if (listDescriptor.typeIdentifier == event.type) {
            fetchFirstPage()
        }
    }

    /**
     * Handles the [OnListDataInvalidated] `ListStore` event. It'll invalidate the list if the type of this list matches
     * the type of list that is invalidated.
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onListDataInvalidated(event: OnListDataInvalidated) {
        if (listDescriptor.typeIdentifier == event.type) {
            invalidateData()
        }
    }
}
