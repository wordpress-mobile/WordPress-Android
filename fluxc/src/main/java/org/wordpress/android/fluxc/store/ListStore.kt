package org.wordpress.android.fluxc.store

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.paging.PagedList.BoundaryCallback
import com.yarolegovich.wellsql.WellSql
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.ListAction
import org.wordpress.android.fluxc.action.ListAction.FETCHED_LIST_ITEMS
import org.wordpress.android.fluxc.action.ListAction.LIST_DATA_INVALIDATED
import org.wordpress.android.fluxc.action.ListAction.LIST_ITEMS_REMOVED
import org.wordpress.android.fluxc.action.ListAction.LIST_REQUIRES_REFRESH
import org.wordpress.android.fluxc.action.ListAction.REMOVE_ALL_LISTS
import org.wordpress.android.fluxc.action.ListAction.REMOVE_EXPIRED_LISTS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.list.LIST_STATE_TIMEOUT
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListDescriptorTypeIdentifier
import org.wordpress.android.fluxc.model.list.ListItemModel
import org.wordpress.android.fluxc.model.list.ListModel
import org.wordpress.android.fluxc.model.list.ListState
import org.wordpress.android.fluxc.model.list.ListState.FETCHED
import org.wordpress.android.fluxc.model.list.PagedListFactory
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.model.list.datasource.InternalPagedListDataSource
import org.wordpress.android.fluxc.model.list.datasource.ListItemDataSourceInterface
import org.wordpress.android.fluxc.persistence.ListItemSqlUtils
import org.wordpress.android.fluxc.persistence.ListSqlUtils
import org.wordpress.android.fluxc.store.ListStore.OnListChanged.CauseOfListChange
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

// How long a list should stay in DB if it hasn't been updated
const val DEFAULT_EXPIRATION_DURATION = 1000L * 60 * 60 * 24 * 7

/**
 * This Store is responsible for managing lists and their metadata. One of the designs goals for this Store is expose
 * as little as possible to the consumers and make sure the exposed parts are immutable. This not only moves the
 * responsibility of mutation to the Store but also makes it much easier to use the exposed data.
 */
@Singleton
class ListStore @Inject constructor(
    private val listSqlUtils: ListSqlUtils,
    private val listItemSqlUtils: ListItemSqlUtils,
    private val coroutineContext: CoroutineContext,
    private val coroutineEngine: CoroutineEngine,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? ListAction ?: return

        when (actionType) {
            FETCHED_LIST_ITEMS -> handleFetchedListItems(action.payload as FetchedListItemsPayload)
            LIST_ITEMS_REMOVED -> handleListItemsRemoved(action.payload as ListItemsRemovedPayload)
            LIST_REQUIRES_REFRESH -> handleListRequiresRefresh(action.payload as ListDescriptorTypeIdentifier)
            LIST_DATA_INVALIDATED -> handleListDataInvalidated(action.payload as ListDescriptorTypeIdentifier)
            REMOVE_EXPIRED_LISTS -> handleRemoveExpiredLists(action.payload as RemoveExpiredListsPayload)
            REMOVE_ALL_LISTS -> handleRemoveAllLists()
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, ListStore::class.java.simpleName + " onRegister")
    }

    /**
     * This is the function that'll be used to consume lists.
     *
     * @param listDescriptor Describes which list will be consumed
     * @param dataSource Describes how to take certain actions such as fetching a list for the item type [LIST_ITEM].
     * @param lifecycle The lifecycle of the client that'll be consuming this list. It's used to make sure everything
     * is cleaned up properly once the client is destroyed.
     *
     * @return A [PagedListWrapper] that provides all the necessary information to consume a list such as its data,
     * whether the first page is being fetched, whether there are any errors etc. in `LiveData` format.
     */
    fun <LIST_DESCRIPTOR : ListDescriptor, ITEM_IDENTIFIER, LIST_ITEM> getList(
        listDescriptor: LIST_DESCRIPTOR,
        dataSource: ListItemDataSourceInterface<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM>,
        lifecycle: Lifecycle
    ): PagedListWrapper<LIST_ITEM> {
        val factory = createPagedListFactory(listDescriptor, dataSource)
        val pagedListData = createPagedListLiveData(
                listDescriptor = listDescriptor,
                dataSource = dataSource,
                pagedListFactory = factory
        )
        return PagedListWrapper(
                data = pagedListData,
                dispatcher = mDispatcher,
                listDescriptor = listDescriptor,
                lifecycle = lifecycle,
                refresh = {
                    handleFetchList(listDescriptor, loadMore = false) { offset ->
                        dataSource.fetchList(listDescriptor, offset)
                    }
                },
                invalidate = factory::invalidate,
                parentCoroutineContext = coroutineContext
        )
    }

    /**
     * A helper function that creates a [PagedList] [LiveData] for the given [LIST_DESCRIPTOR], [dataSource] and the
     * [PagedListFactory].
     */
    private fun <LIST_DESCRIPTOR : ListDescriptor, ITEM_IDENTIFIER, LIST_ITEM> createPagedListLiveData(
        listDescriptor: LIST_DESCRIPTOR,
        dataSource: ListItemDataSourceInterface<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM>,
        pagedListFactory: PagedListFactory<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM>
    ): LiveData<PagedList<LIST_ITEM>> {
        val pagedListConfig = PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setInitialLoadSizeHint(listDescriptor.config.initialLoadSize)
                .setPageSize(listDescriptor.config.dbPageSize)
                .build()
        val boundaryCallback = object : BoundaryCallback<LIST_ITEM>() {
            override fun onItemAtEndLoaded(itemAtEnd: LIST_ITEM) {
                // Load more items if we are near the end of list
                coroutineEngine.launch(AppLog.T.API, this, "ListStore: Loading next page") {
                    handleFetchList(listDescriptor, loadMore = true) { offset ->
                        dataSource.fetchList(listDescriptor, offset)
                    }
                }
                super.onItemAtEndLoaded(itemAtEnd)
            }
        }
        return LivePagedListBuilder<Int, LIST_ITEM>(pagedListFactory, pagedListConfig)
                .setBoundaryCallback(boundaryCallback).build()
    }

    /**
     * A helper function that creates a [PagedListFactory] for the given [LIST_DESCRIPTOR] and [dataSource].
     */
    private fun <LIST_DESCRIPTOR : ListDescriptor, ITEM_IDENTIFIER, LIST_ITEM> createPagedListFactory(
        listDescriptor: LIST_DESCRIPTOR,
        dataSource: ListItemDataSourceInterface<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM>
    ): PagedListFactory<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM> {
        val getRemoteItemIds = { getListItems(listDescriptor).map { RemoteId(value = it) } }
        val getIsListFullyFetched = { getListState(listDescriptor) == FETCHED }
        return PagedListFactory(
                createDataSource = {
                    InternalPagedListDataSource(
                            listDescriptor = listDescriptor,
                            remoteItemIds = getRemoteItemIds(),
                            isListFullyFetched = getIsListFullyFetched(),
                            itemDataSource = dataSource
                    )
                })
    }

    /**
     * A helper function that returns the list items for the given [ListDescriptor].
     */
    private fun getListItems(listDescriptor: ListDescriptor): List<Long> {
        val listModel = listSqlUtils.getList(listDescriptor)
        return if (listModel != null) {
            listItemSqlUtils.getListItems(listModel.id).map { it.remoteItemId }
        } else emptyList()
    }

    /**
     * A helper function that initiates the fetch from remote for the given [ListDescriptor].
     *
     * Before fetching the list, it'll first check if this is a valid fetch depending on the list's state. Then, it'll
     * update the list's state and emit that change. Finally, it'll calculate the offset and initiate the fetch with
     * the given [fetchList] function.
     */
    private fun handleFetchList(
        listDescriptor: ListDescriptor,
        loadMore: Boolean,
        fetchList: (Long) -> Unit
    ) {
        val currentState = getListState(listDescriptor)
        if (!loadMore && currentState.isFetchingFirstPage()) {
            // already fetching the first page
            return
        } else if (loadMore && !currentState.canLoadMore()) {
            // we can only load more if there is more data to be loaded
            return
        }

        val newState = if (loadMore) ListState.LOADING_MORE else ListState.FETCHING_FIRST_PAGE
        listSqlUtils.insertOrUpdateList(listDescriptor, newState)
        handleListStateChange(listDescriptor, newState)

        val listModel = requireNotNull(listSqlUtils.getList(listDescriptor)) {
            "The `ListModel` can never be `null` here since either a new list is inserted or existing one updated"
        }
        val offset = if (loadMore) listItemSqlUtils.getListItemsCount(listModel.id) else 0L
        fetchList(offset)
    }

    /**
     * A helper function that emits the latest [ListState] for the given [ListDescriptor].
     */
    private fun handleListStateChange(listDescriptor: ListDescriptor, newState: ListState, error: ListError? = null) {
        emitChange(OnListStateChanged(listDescriptor, newState, error))
    }

    /**
     * Handles the [ListAction.FETCHED_LIST_ITEMS] action.
     *
     * Here is how it works:
     * 1. If there was an error, update the list's state and emit the change. Otherwise:
     * 2. If the first page is fetched, delete the existing [ListItemModel]s.
     * 3. Update the [ListModel]'s state depending on whether there is more data to be fetched
     * 4. Insert the [ListItemModel]s and emit the change
     *
     * See [handleFetchList] to see how items are fetched.
     */
    private fun handleFetchedListItems(payload: FetchedListItemsPayload) {
        val newState = when {
            payload.isError -> ListState.ERROR
            payload.canLoadMore -> ListState.CAN_LOAD_MORE
            else -> FETCHED
        }
        listSqlUtils.insertOrUpdateList(payload.listDescriptor, newState)

        if (!payload.isError) {
            val db = WellSql.giveMeWritableDb()
            db.beginTransaction()
            try {
                if (!payload.loadedMore) {
                    deleteListItems(payload.listDescriptor)
                }
                val listModel = requireNotNull(listSqlUtils.getList(payload.listDescriptor)) {
                    "The `ListModel` can never be `null` here since either a new list is inserted or existing one " +
                            "updated"
                }
                listItemSqlUtils.insertItemList(payload.remoteItemIds.map { remoteItemId ->
                    val listItemModel = ListItemModel()
                    listItemModel.listId = listModel.id
                    listItemModel.remoteItemId = remoteItemId
                    return@map listItemModel
                })
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
        val causeOfChange = if (payload.isError) {
            CauseOfListChange.ERROR
        } else {
            if (payload.loadedMore) CauseOfListChange.LOADED_MORE else CauseOfListChange.FIRST_PAGE_FETCHED
        }
        emitChange(OnListChanged(listOf(payload.listDescriptor), causeOfChange, payload.error))
        handleListStateChange(payload.listDescriptor, newState, payload.error)
    }

    /**
     * Handles the [ListAction.LIST_ITEMS_REMOVED] action.
     *
     * Items in [ListItemsRemovedPayload.remoteItemIds] will be removed from lists with
     * [ListDescriptorTypeIdentifier] after which [OnListDataInvalidated] event will be emitted.
     */
    private fun handleListItemsRemoved(payload: ListItemsRemovedPayload) {
        val lists = listSqlUtils.getListsWithTypeIdentifier(payload.type)
        listItemSqlUtils.deleteItemsFromLists(lists.map { it.id }, payload.remoteItemIds)
        emitChange(OnListDataInvalidated(payload.type))
    }

    /**
     * Handles the [ListAction.LIST_REQUIRES_REFRESH] action.
     *
     * Whenever a type of list needs to be refreshed, [OnListRequiresRefresh] event will be emitted so the listening
     * lists can refresh themselves.
     */
    private fun handleListRequiresRefresh(typeIdentifier: ListDescriptorTypeIdentifier) {
        emitChange(OnListRequiresRefresh(type = typeIdentifier))
    }

    /**
     * Handles the [ListAction.LIST_DATA_INVALIDATED] action.
     *
     * Whenever the data of a list is invalidated, [OnListDataInvalidated] event will be emitted so the listening
     * lists can invalidate their data.
     */
    private fun handleListDataInvalidated(typeIdentifier: ListDescriptorTypeIdentifier) {
        emitChange(OnListDataInvalidated(type = typeIdentifier))
    }

    /**
     * Handles the [ListAction.REMOVE_EXPIRED_LISTS] action.
     *
     * It deletes [ListModel]s that hasn't been updated for the given [RemoveExpiredListsPayload.expirationDuration].
     */
    private fun handleRemoveExpiredLists(payload: RemoveExpiredListsPayload) {
        listSqlUtils.deleteExpiredLists(payload.expirationDuration)
    }

    /**
     * Handles the [ListAction.REMOVE_ALL_LISTS] action.
     *
     * It simply deletes every [ListModel] in the DB.
     */
    private fun handleRemoveAllLists() {
        listSqlUtils.deleteAllLists()
    }

    /**
     * Deletes all the items for the given [ListDescriptor].
     */
    private fun deleteListItems(listDescriptor: ListDescriptor) {
        listSqlUtils.getList(listDescriptor)?.let {
            listItemSqlUtils.deleteItems(it.id)
        }
    }

    /**
     * A helper function that returns the [ListState] for the given [ListDescriptor].
     */
    private fun getListState(listDescriptor: ListDescriptor): ListState {
        val listModel = listSqlUtils.getList(listDescriptor)
        return if (listModel != null && !isListStateOutdated(listModel)) {
            requireNotNull(ListState.values().firstOrNull { it.value == listModel.stateDbValue }) {
                "The stateDbValue of the ListModel didn't match any of the `ListState`s. This likely happened " +
                        "because the ListState values were altered without a DB migration."
            }
        } else ListState.defaultState
    }

    /**
     * A helper function that returns whether it has been more than a certain time has passed since it's `lastModified`.
     *
     * Since we keep the state in the DB, in the case of application being closed during a fetch, it'll carry
     * over to the next session. To prevent such cases, we use a timeout approach. If it has been more than a
     * certain time since the list is last updated, we should ignore the state.
     */
    private fun isListStateOutdated(listModel: ListModel): Boolean {
        listModel.lastModified?.let {
            val lastModified = DateTimeUtils.dateUTCFromIso8601(it)
            val timePassed = (Date().time - lastModified.time)
            return timePassed > LIST_STATE_TIMEOUT
        }
        // If a list is null, it means we have never fetched it before, so it can't be outdated
        return false
    }

    /**
     * The event to be emitted when there is a change to a [ListModel].
     */
    class OnListChanged(
        val listDescriptors: List<ListDescriptor>,
        val causeOfChange: CauseOfListChange,
        error: ListError?
    ) : Store.OnChanged<ListError>() {
        enum class CauseOfListChange {
            ERROR, FIRST_PAGE_FETCHED, LOADED_MORE
        }

        init {
            this.error = error
        }
    }

    /**
     * The event to be emitted whenever there is a change to the [ListState]
     */
    class OnListStateChanged(
        val listDescriptor: ListDescriptor,
        val newState: ListState,
        error: ListError?
    ) : Store.OnChanged<ListError>() {
        init {
            this.error = error
        }
    }

    /**
     * The event to be emitted when a list needs to be refresh for a specific [ListDescriptorTypeIdentifier].
     */
    class OnListRequiresRefresh(val type: ListDescriptorTypeIdentifier) : Store.OnChanged<ListError>()

    /**
     * The event to be emitted when a list's data is invalidated for a specific [ListDescriptorTypeIdentifier].
     */
    class OnListDataInvalidated(val type: ListDescriptorTypeIdentifier) : Store.OnChanged<ListError>()

    /**
     * This is the payload for [ListAction.LIST_ITEMS_REMOVED].
     *
     * @property type [ListDescriptorTypeIdentifier] which will tell [ListStore] and the clients which
     * [ListDescriptor]s are updated.
     * @property remoteItemIds Remote item ids to be removed from the lists matching the [ListDescriptorTypeIdentifier].
     */
    class ListItemsRemovedPayload(val type: ListDescriptorTypeIdentifier, val remoteItemIds: List<Long>)

    /**
     * This is the payload for [ListAction.FETCHED_LIST_ITEMS].
     *
     * @property listDescriptor List descriptor will be provided when the action to fetch items will be dispatched
     * from other Stores. The same list descriptor will need to be used in this payload so [ListStore] can decide
     * which list to update.
     * @property remoteItemIds Fetched item ids
     * @property loadedMore Indicates whether the first page is fetched or we loaded more data
     * @property canLoadMore Indicates whether there is more data to be loaded from the server.
     */
    class FetchedListItemsPayload(
        val listDescriptor: ListDescriptor,
        val remoteItemIds: List<Long>,
        val loadedMore: Boolean,
        val canLoadMore: Boolean,
        error: ListError?
    ) : Payload<ListError>() {
        init {
            this.error = error
        }
    }

    /**
     * This is the payload for [ListAction.REMOVE_EXPIRED_LISTS].
     *
     * @property expirationDuration Tells how long a list should be kept in the DB if it hasn't been updated
     */
    class RemoveExpiredListsPayload(val expirationDuration: Long = DEFAULT_EXPIRATION_DURATION)

    class ListError(
        val type: ListErrorType,
        val message: String? = null
    ) : OnChangedError

    enum class ListErrorType {
        GENERIC_ERROR,
        PERMISSION_ERROR
    }
}
