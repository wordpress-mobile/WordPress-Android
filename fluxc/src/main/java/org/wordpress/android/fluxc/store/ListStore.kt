package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.ListAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.list.LIST_STATE_TIMEOUT
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListItemDataSource
import org.wordpress.android.fluxc.model.list.ListItemModel
import org.wordpress.android.fluxc.model.list.ListManager
import org.wordpress.android.fluxc.model.list.ListModel
import org.wordpress.android.fluxc.model.list.ListState
import org.wordpress.android.fluxc.model.list.ListType
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.persistence.ListItemSqlUtils
import org.wordpress.android.fluxc.persistence.ListSqlUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

const val DEFAULT_LOAD_MORE_OFFSET = 10 // When we should load more data for a list

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
            ListAction.FETCH_LIST -> handleFetchList(action.payload as FetchListPayload)
            ListAction.FETCHED_LIST_ITEMS -> handleFetchedListItems(action.payload as FetchedListItemsPayload)
            ListAction.DELETE_LIST_ITEMS -> handleDeleteListItems(action.payload as DeleteListItemsPayload)
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, ListStore::class.java.simpleName + " onRegister")
    }

    /**
     * This is the function that'll be used to consume lists.
     *
     * @property listDescriptor List to be consumed
     * @property dataSource An interface that tells the [ListStore] how to get/fetch items. See [ListItemDataSource]
     * for more details.
     * @property loadMoreOffset Indicates when more data for a list should be fetched. It'll be passed to [ListManager].
     *
     * @return An immutable list manager that exposes enough information about a list to be used by adapters. See
     * [ListManager] for more details.
     */
    fun <T> getListManager(
        listDescriptor: ListDescriptor,
        dataSource: ListItemDataSource<T>,
        loadMoreOffset: Int = DEFAULT_LOAD_MORE_OFFSET
    ): ListManager<T> {
        val listModel = listSqlUtils.getList(listDescriptor)
        val listItems = if (listModel != null) {
            listItemSqlUtils.getListItems(listModel.id)
        } else emptyList()
        return ListManager(
                dispatcher = mDispatcher,
                listDescriptor = listDescriptor,
                items = listItems,
                dataSource = dataSource,
                loadMoreOffset = loadMoreOffset,
                isFetchingFirstPage = if (listModel != null) getListState(listModel).isFetchingFirstPage() else false,
                isLoadingMore = if (listModel != null) getListState(listModel).isLoadingMore() else false
        )
    }

    /**
     * Handles the [ListAction.FETCH_LIST] action.
     *
     * This acts as an intermediary action. It will update the state and emit the change. Afterwards, depending on
     * the type of the list, another action will be dispatched so the Store for that type can handle the fetch action
     * and later use the [ListAction.FETCHED_LIST_ITEMS] action to let the [ListStore] know about it.
     *
     * See [handleFetchedListItems] for what happens after items are fetched.
     */
    private fun handleFetchList(payload: FetchListPayload) {
        val newState = if (payload.loadMore) ListState.LOADING_MORE else ListState.FETCHING_FIRST_PAGE
        listSqlUtils.insertOrUpdateList(payload.listDescriptor, newState)
        emitChange(OnListChanged(listOf(payload.listDescriptor), null))

        when (payload.listDescriptor.type) {
            ListType.POST -> TODO()
            ListType.WOO_ORDER -> TODO()
        }
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
            val listModel = listSqlUtils.getList(payload.listDescriptor)
            if (listModel != null) { // Sanity check
                listItemSqlUtils.insertItemList(payload.remoteItemIds.map { remoteItemId ->
                    val listItemModel = ListItemModel()
                    listItemModel.listId = listModel.id
                    listItemModel.remoteItemId = remoteItemId
                    return@map listItemModel
                })
            }
        } else {
            listSqlUtils.insertOrUpdateList(payload.listDescriptor, ListState.ERROR)
        }
        emitChange(OnListChanged(listOf(payload.listDescriptor), payload.error))
    }

    /**
     * Handles the [ListAction.DELETE_LIST_ITEMS] action.
     *
     * It'll find every list for the given [ListDescriptor]s, remove the given items from each one and emit the changes.
     */
    private fun handleDeleteListItems(payload: DeleteListItemsPayload) {
        val lists = payload.listDescriptors.mapNotNull { listSqlUtils.getList(it) }
        listItemSqlUtils.deleteItemsFromLists(lists.map { it.id }, payload.remoteItemIds)
        emitChange(OnListChanged(payload.listDescriptors, error = null))
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
                /**
                 * If the state in the DB doesn't match any of the values, we want the app to crash so we can fix it.
                 * That also means if we change the ListState values, we should write a migration to reset lists.
                 */
                ListState.values().firstOrNull { it.value == listModel.stateDbValue }!!
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
     * The event to be emitted when there is a change to a [ListModel] or its items.
     */
    class OnListChanged(
        val listDescriptors: List<ListDescriptor>,
        error: FetchedListItemsError?
    ) : Store.OnChanged<FetchedListItemsError>() {
        init {
            this.error = error
        }
    }

    /**
     * This is the payload for [ListAction.DELETE_LIST_ITEMS]. When an item is deleted, we'll need to remove it from
     * several lists. It's the caller Stores responsibility to decide which lists an item should be deleted from.
     *
     * @property listDescriptors Lists to be deleted from.
     * @property remoteItemIds Items to delete.
     */
    class DeleteListItemsPayload(
        val listDescriptors: List<ListDescriptor>,
        val remoteItemIds: List<Long>
    ) : Payload<BaseNetworkError>()

    /**
     * This is the payload for [ListAction.FETCH_LIST].
     *
     * @property listDescriptor List to be fetched
     * @property loadMore Indicates whether the first page should be fetched or more data should be loaded.
     */
    class FetchListPayload(
        val listDescriptor: ListDescriptor,
        val loadMore: Boolean = false
    ) : Payload<BaseNetworkError>()

    /**
     * This is the payload for [ListAction.FETCHED_LIST_ITEMS].
     *
     * @property listDescriptor List descriptor will be provided when the action to fetch items will be dispatched
     * from other Stores. The same list descriptor will need to be used in this payload so [ListStore] can decide
     * which list to update.
     * @property remoteItemIds Fetched item ids
     * @property loadedMore Indicates whether the first page is fetched or loaded more data
     * @property canLoadMore Indicates whether there is more data to be loaded from the server. If it's false,
     * [ListStore] will not trigger any more actions to load more data.
     */
    class FetchedListItemsPayload(
        val listDescriptor: ListDescriptor,
        val remoteItemIds: List<Long>,
        val loadedMore: Boolean,
        val canLoadMore: Boolean,
        error: FetchedListItemsError?
    ) : Payload<FetchedListItemsError>() {
        init {
            this.error = error
        }
    }

    class FetchedListItemsError(
        val type: FetchedListItemsErrorType,
        val message: String? = null
    ) : Store.OnChangedError

    enum class FetchedListItemsErrorType {
        GENERIC_ERROR
    }
}
