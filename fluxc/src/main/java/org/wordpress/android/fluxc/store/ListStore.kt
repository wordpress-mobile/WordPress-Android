package org.wordpress.android.fluxc.store

import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.ListAction
import org.wordpress.android.fluxc.action.ListAction.FETCHED_LIST_ITEMS
import org.wordpress.android.fluxc.action.ListAction.FETCH_LIST
import org.wordpress.android.fluxc.action.ListAction.LIST_ITEMS_CHANGED
import org.wordpress.android.fluxc.action.ListAction.LIST_ITEMS_REMOVED
import org.wordpress.android.fluxc.action.ListAction.REMOVE_ALL_LISTS
import org.wordpress.android.fluxc.action.ListAction.REMOVE_EXPIRED_LISTS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.list.LIST_STATE_TIMEOUT
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListDescriptorTypeIdentifier
import org.wordpress.android.fluxc.model.list.ListItemDataSource
import org.wordpress.android.fluxc.model.list.ListItemModel
import org.wordpress.android.fluxc.model.list.ListManager
import org.wordpress.android.fluxc.model.list.ListManagerItem
import org.wordpress.android.fluxc.model.list.ListManagerItem.LocalItem
import org.wordpress.android.fluxc.model.list.ListManagerItem.RemoteItem
import org.wordpress.android.fluxc.model.list.ListModel
import org.wordpress.android.fluxc.model.list.ListState
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.persistence.ListItemSqlUtils
import org.wordpress.android.fluxc.persistence.ListSqlUtils
import org.wordpress.android.fluxc.store.ListStore.OnListChanged.CauseOfListChange
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

// How long a list should stay in DB if it hasn't been updated
const val DEFAULT_EXPIRATION_DURATION = 1000L * 60 * 60 * 24 * 7
// When we should load more data for a list
const val DEFAULT_LOAD_MORE_OFFSET = 10

/**
 * This Store is responsible for managing lists and their metadata. One of the designs goals for this Store is expose
 * as little as possible to the consumers and make sure the exposed parts are immutable. This not only moves the
 * responsibility of mutation to the Store but also makes it much easier to use the exposed data.
 */
@Singleton
class ListStore @Inject constructor(
    private val listSqlUtils: ListSqlUtils,
    private val listItemSqlUtils: ListItemSqlUtils,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? ListAction ?: return

        when (actionType) {
            FETCH_LIST -> handleFetchList(action.payload as FetchListPayload)
            FETCHED_LIST_ITEMS -> handleFetchedListItems(action.payload as FetchedListItemsPayload)
            LIST_ITEMS_CHANGED -> handleListItemsChanged(action.payload as ListItemsChangedPayload)
            LIST_ITEMS_REMOVED -> handleListItemsRemoved(action.payload as ListItemsRemovedPayload)
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
     * It'll add the [ListItemDataSource.localItems], [ListItemDataSource.remoteItemIdsToInclude] and the list items
     * from the DB one after another and then filter out [ListItemDataSource.remoteItemsToHide] and finally
     * pass it to [ListManager].
     *
     * @param listDescriptor List to be consumed
     * @param dataSource An interface that tells the [ListStore] how to get/fetch items. See [ListItemDataSource]
     * for more details.
     * @param loadMoreOffset Indicates when more data for a list should be fetched. It'll be passed to [ListManager].
     *
     * @return An immutable list manager that exposes enough information about a list to be used by adapters. See
     * [ListManager] for more details.
     */
    suspend fun <T> getListManager(
        listDescriptor: ListDescriptor,
        dataSource: ListItemDataSource<T>,
        loadMoreOffset: Int = DEFAULT_LOAD_MORE_OFFSET
    ): ListManager<T> = withContext(Dispatchers.Default) {
        val listModel = listSqlUtils.getList(listDescriptor)
        // Get the remote ids of items for the list from the DB
        val remoteIdsFromDb = if (listModel != null) {
            listItemSqlUtils.getListItems(listModel.id).map { it.remoteItemId }
        } else emptyList()
        // Get the remote ids that client asks for to be included if they are not already in the remote ids from DB
        val remoteItemIdsToInclude = dataSource.remoteItemIdsToInclude(listDescriptor)?.let { remoteIdsToInclude ->
            remoteIdsToInclude.filter { !remoteIdsFromDb.contains(it) }
        } ?: emptyList()
        // Add the remote ids together and filter out the ids the client asks to be hidden
        val remoteIds = remoteItemIdsToInclude.asSequence().plus(remoteIdsFromDb).filter {
            dataSource.remoteItemsToHide(listDescriptor)?.contains(it) != true
        }.toList()

        val listState = if (listModel != null) getListState(listModel) else null
        val listData = dataSource.getItems(listDescriptor, remoteIds)
        val localItems = dataSource.localItems(listDescriptor) ?: emptyList()
        // Add the local items and remote items in one list of `ListManagerItem`s
        val allItems: List<ListManagerItem<T>> = localItems.asSequence().map { LocalItem(it) }
                .plus(remoteIds.map { RemoteItem(it, listData[it]) }).toList()
        return@withContext ListManager(
                dispatcher = mDispatcher,
                listDescriptor = listDescriptor,
                items = allItems,
                loadMoreOffset = loadMoreOffset,
                isFetchingFirstPage = listState?.isFetchingFirstPage() ?: false,
                isLoadingMore = listState?.isLoadingMore() ?: false,
                canLoadMore = listState?.canLoadMore() ?: false,
                fetchItem = { remoteItemId ->
                    dataSource.fetchItem(listDescriptor, remoteItemId)
                },
                fetchList = dataSource::fetchList
        )
    }

    /**
     * Handles the [ListAction.FETCH_LIST] action.
     *
     * This acts as an intermediary action. It will first check the current state and ignore unnecessary actions.
     * It will then update the state and emit the change. Afterwards, [FetchListPayload.fetchList] will be used to do
     * the actual fetching. [ListAction.FETCHED_LIST_ITEMS] action should be used to let the [ListStore] know the
     * results of the fetch.
     *
     * See [handleFetchedListItems] for what happens after items are fetched.
     */
    private fun handleFetchList(payload: FetchListPayload) {
        listSqlUtils.getList(payload.listDescriptor)?.let { listModel ->
            val currentState = getListState(listModel)
            if (!payload.loadMore && currentState.isFetchingFirstPage()) {
                // already fetching the first page
                return
            } else if (payload.loadMore && !currentState.canLoadMore()) {
                // we can only load more if there is more data to be loaded
                return
            }
        }
        val newState = if (payload.loadMore) ListState.LOADING_MORE else ListState.FETCHING_FIRST_PAGE
        listSqlUtils.insertOrUpdateList(payload.listDescriptor, newState)
        emitChange(OnListChanged(listOf(payload.listDescriptor), CauseOfListChange.STATE_CHANGED, null))

        val listModel = requireNotNull(listSqlUtils.getList(payload.listDescriptor)) {
            "The `ListModel` can never be `null` here since either a new list is inserted or existing one updated"
        }
        val offset = if (payload.loadMore) listItemSqlUtils.getListItems(listModel.id).size else 0
        payload.fetchList(payload.listDescriptor, offset)
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
        if (!payload.isError) {
            if (!payload.loadedMore) {
                deleteListItems(payload.listDescriptor)
            }
            val state = if (payload.canLoadMore) ListState.CAN_LOAD_MORE else ListState.FETCHED
            listSqlUtils.insertOrUpdateList(payload.listDescriptor, state)
            val listModel = requireNotNull(listSqlUtils.getList(payload.listDescriptor)) {
                "The `ListModel` can never be `null` here since either a new list is inserted or existing one updated"
            }
            listItemSqlUtils.insertItemList(payload.remoteItemIds.map { remoteItemId ->
                val listItemModel = ListItemModel()
                listItemModel.listId = listModel.id
                listItemModel.remoteItemId = remoteItemId
                return@map listItemModel
            })
        } else {
            listSqlUtils.insertOrUpdateList(payload.listDescriptor, ListState.ERROR)
        }
        val causeOfChange = if (payload.isError) {
            CauseOfListChange.ERROR
        } else {
            if (payload.loadedMore) CauseOfListChange.LOADED_MORE else CauseOfListChange.FIRST_PAGE_FETCHED
        }
        emitChange(OnListChanged(listOf(payload.listDescriptor), causeOfChange, payload.error))
    }

    /**
     * Handles the [ListAction.LIST_ITEMS_CHANGED] action.
     *
     * Whenever an item of a list is changed, we'll emit the [OnListItemsChanged] event so the consumer of the lists can
     * update themselves.
     */
    private fun handleListItemsChanged(payload: ListItemsChangedPayload) {
        emitChange(OnListItemsChanged(payload.type, error = null))
    }

    /**
     * Handles the [ListAction.LIST_ITEMS_REMOVED] action.
     *
     * Items in [ListItemsRemovedPayload.remoteItemIds] will be removed from lists with
     * [ListDescriptorTypeIdentifier] after which [OnListItemsChanged] event will be emitted.
     */
    private fun handleListItemsRemoved(payload: ListItemsRemovedPayload) {
        val lists = listSqlUtils.getListsWithTypeIdentifier(payload.type)
        listItemSqlUtils.deleteItemsFromLists(lists.map { it.id }, payload.remoteItemIds)
        emitChange(OnListItemsChanged(payload.type, error = null))
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

    private fun getListState(listModel: ListModel): ListState =
            if (isListStateOutdated(listModel)) {
                ListState.defaultState
            } else {
                requireNotNull(ListState.values().firstOrNull { it.value == listModel.stateDbValue }) {
                    "The stateDbValue of the ListModel didn't match any of the `ListState`s. This likely happened " +
                            "because the ListState values were altered without a DB migration."
                }
            }

    /**
     * A helper function to returns whether it has been more than a certain time has passed since it's `lastModified`.
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
            ERROR, FIRST_PAGE_FETCHED, LOADED_MORE, STATE_CHANGED
        }
        init {
            this.error = error
        }
    }

    /**
     * The event to be emitted when there is a change to items for a specific [ListDescriptorTypeIdentifier].
     */
    class OnListItemsChanged(
        val type: ListDescriptorTypeIdentifier,
        error: ListError?
    ) : Store.OnChanged<ListError>() {
        init {
            this.error = error
        }
    }

    /**
     * This is the payload for [ListAction.LIST_ITEMS_CHANGED].
     *
     * @property type [ListDescriptorTypeIdentifier] which will tell [ListStore] and the clients which
     * [ListDescriptor]s are updated.
     */
    class ListItemsChangedPayload(val type: ListDescriptorTypeIdentifier)

    /**
     * This is the payload for [ListAction.LIST_ITEMS_REMOVED].
     *
     * @property type [ListDescriptorTypeIdentifier] which will tell [ListStore] and the clients which
     * [ListDescriptor]s are updated.
     * @property remoteItemIds Remote item ids to be removed from the lists matching the [ListDescriptorTypeIdentifier].
     */
    class ListItemsRemovedPayload(val type: ListDescriptorTypeIdentifier, val remoteItemIds: List<Long>)

    /**
     * This is the payload for [ListAction.FETCH_LIST].
     *
     * @property listDescriptor List to be fetched
     * @property loadMore Indicates whether the first page should be fetched or more data should be loaded.
     * @property fetchList A function that tells ListStore how to fetch a list given the [ListDescriptor] and the offset
     * calculated by [ListStore]. Please check [ListItemDataSource.fetchList] as that's how the fetch function is
     * initially passed to [ListStore] in [getListManager].
     */
    class FetchListPayload(
        val listDescriptor: ListDescriptor,
        val loadMore: Boolean = false,
        val fetchList: (ListDescriptor, Int) -> Unit
    ) : Payload<BaseNetworkError>()

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
    ) : Store.OnChangedError

    enum class ListErrorType {
        GENERIC_ERROR,
        PERMISSION_ERROR
    }
}
