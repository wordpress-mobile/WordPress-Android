package org.wordpress.android.ui.subscribers

import android.content.SharedPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.models.wrappers.SimpleDateFormatWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.dataview.DataViewDropdownItem
import org.wordpress.android.ui.dataview.DataViewFieldType
import org.wordpress.android.ui.dataview.DataViewItem
import org.wordpress.android.ui.dataview.DataViewItemField
import org.wordpress.android.ui.dataview.DataViewItemImage
import org.wordpress.android.ui.dataview.DataViewViewModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.IndividualSubscriberStats
import uniffi.wp_api.IndividualSubscriberStatsParams
import uniffi.wp_api.ListSubscribersIncludeField
import uniffi.wp_api.ListSubscribersSortField
import uniffi.wp_api.Subscriber
import uniffi.wp_api.SubscriberType
import uniffi.wp_api.SubscribersListParams
import uniffi.wp_api.WpApiParamOrder
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SubscribersViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val appLogWrapper: AppLogWrapper,
    sharedPrefs: SharedPreferences,
    networkUtilsWrapper: NetworkUtilsWrapper,
    selectedSiteRepository: SelectedSiteRepository,
    accountStore: AccountStore,
    @Named(IO_THREAD) ioDispatcher: CoroutineDispatcher,
) : DataViewViewModel(
    mainDispatcher = mainDispatcher,
    appLogWrapper = appLogWrapper,
    sharedPrefs = sharedPrefs,
    networkUtilsWrapper = networkUtilsWrapper,
    selectedSiteRepository = selectedSiteRepository,
    accountStore = accountStore,
    ioDispatcher = ioDispatcher
) {
    private val _subscriberStats = MutableStateFlow<IndividualSubscriberStats?>(null)
    val subscriberStats = _subscriberStats.asStateFlow()

    private var statsJob: Job? = null

    @Inject
    lateinit var dateFormatWrapper: SimpleDateFormatWrapper

    sealed class UiEvent {
        data class ShowDeleteConfirmationDialog(val subscriber: Subscriber) : UiEvent()
        data object ShowDeleteSuccessDialog : UiEvent()
        data class ShowToast(val messageRes: Int) : UiEvent()
    }

    private val _uiEvent = MutableStateFlow<UiEvent?>(null)
    val uiEvent = _uiEvent

    override fun getSupportedFilters(): List<DataViewDropdownItem> {
        return listOf(
            DataViewDropdownItem(
                id = SubscriberFilterType.Email.id,
                titleRes = R.string.subscribers_filter_email_subscription
            ),
            DataViewDropdownItem(
                id = SubscriberFilterType.Reader.id,
                titleRes = R.string.subscribers_filter_reader_subscription
            )
        )
    }

    override fun getSupportedSorts(): List<DataViewDropdownItem> {
        return listOf(
            DataViewDropdownItem(
                id = SubscriberSortType.DateSubscribed.id,
                titleRes = R.string.subscribers_sort_date
            ),
            DataViewDropdownItem(
                id = SubscriberSortType.Name.id,
                titleRes = R.string.subscribers_sort_name
            ),
        )
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun performNetworkRequest(
        page: Int,
        searchQuery: String,
        filter: DataViewDropdownItem?,
        sortOrder: WpApiParamOrder,
        sortBy: DataViewDropdownItem?,
    ): List<DataViewItem> = withContext(ioDispatcher) {
        try {
            fetchSubscriberList(
                page = page,
                filter = filter,
                sortBy = sortBy,
                sortOrder = sortOrder,
                searchQuery = searchQuery
            )
        } catch (e: Exception) {
            appLogWrapper.e(AppLog.T.MAIN, "Fetch subscribers failed: $e")
            onError(e.message)
            emptyList()
        }
    }

    private suspend fun fetchSubscriberList(
        page: Int,
        filter: DataViewDropdownItem?,
        sortOrder: WpApiParamOrder,
        sortBy: DataViewDropdownItem?,
        searchQuery: String
    ): List<DataViewItem> = withContext(ioDispatcher) {
        if (USE_DUMMY_DATA) {
            val subscribers = DummySubscribers.getDummySubscribers()
            return@withContext subscribers.map { subscriberToDataViewItem(it) }
        }
        val filterType = filter?.let {
            when (it.id) {
                SubscriberFilterType.Email.id -> SubscriberType.EmailSubscriber
                SubscriberFilterType.Reader.id -> SubscriberType.ReaderSubscriber
                else -> null
            }
        }

        val sortType = sortBy?.let {
            when (it.id) {
                SubscriberSortType.DateSubscribed.id -> ListSubscribersSortField.DATE_SUBSCRIBED
                SubscriberSortType.Name.id -> ListSubscribersSortField.DISPLAY_NAME
                else -> null
            }
        }

        val params = SubscribersListParams(
            page = page.toULong(),
            perPage = PAGE_SIZE.toULong(),
            sortOrder = sortOrder,
            search = searchQuery,
            filter = filterType,
            sort = sortType,
            include = listOf(ListSubscribersIncludeField.COUNTRY)
        )

        val response = wpComApiClient.request { requestBuilder ->
            requestBuilder.subscribers().listSubscribers(
                wpComSiteId = siteId().toULong(),
                params = params
            )
        }
        when (response) {
            is WpRequestResult.Success -> {
                val subscribers = response.response.data.subscribers
                appLogWrapper.d(AppLog.T.MAIN, "Fetched ${subscribers.size} subscribers")
                return@withContext subscribers.map { subscriberToDataViewItem(it) }
            }

            else -> {
                appLogWrapper.e(AppLog.T.MAIN, "Fetch subscribers failed: $response")
                onError((response as? WpRequestResult.WpError)?.errorMessage)
                return@withContext emptyList()
            }
        }
    }

    private fun subscriberToDataViewItem(subscriber: Subscriber): DataViewItem {
        return DataViewItem(
            id = subscriber.userId,
            image = DataViewItemImage(
                imageUrl = subscriber.avatar,
                fallbackImageRes = R.drawable.ic_user_placeholder_primary_24,
            ),
            title = subscriber.displayNameOrEmail(),
            fields = listOf(
                DataViewItemField(
                    value = subscriber.subscriptionStatus ?: "",
                    valueType = DataViewFieldType.TEXT,
                    weight = .6f,
                ),
                DataViewItemField(
                    value = dateFormatWrapper.getDateInstance().format(subscriber.dateSubscribed),
                    valueType = DataViewFieldType.DATE,
                    weight = .4f,
                ),
            ),
            data = subscriber
        )
    }

    /**
     * Returns the subscriber with the given ID, or null if not found. Note that this does NOT do a network call,
     * it simply returns the subscriber from the existing list of items.
     */
    fun getSubscriber(userId: Long): Subscriber? {
        val item = uiState.value.items.firstOrNull { it.id == userId }
        return item?.data as? Subscriber
    }

    private suspend fun fetchSubscriberStats(subscriptionId: ULong): IndividualSubscriberStats? =
        withContext(ioDispatcher) {
            if (USE_DUMMY_DATA) {
                return@withContext DummySubscribers.getDummySubscriberStats()
            }

            val params = IndividualSubscriberStatsParams(
                subscriptionId = subscriptionId
            )

            val response = wpComApiClient.request { requestBuilder ->
                requestBuilder.subscribers().individualSubscriberStats(
                    wpComSiteId = siteId().toULong(),
                    params = params
                )
            }
            when (response) {
                is WpRequestResult.Success -> {
                    val stats = response.response.data
                    appLogWrapper.d(AppLog.T.MAIN, "Fetched subscriber stats: $stats")
                    return@withContext stats
                }

                else -> {
                    appLogWrapper.e(AppLog.T.MAIN, "Fetch subscribers failed: $response")
                    return@withContext null
                }
            }
        }

    private suspend fun deleteSubscriber(subscriber: Subscriber) = runCatching {
        withContext(ioDispatcher) {
            val response = if (subscriber.isEmailSubscriber) {
                wpComApiClient.request { requestBuilder ->
                    requestBuilder.followers().deleteEmailFollower(
                        wpComSiteId = siteId().toULong(),
                        subscriptionId = subscriber.subscriptionId
                    )
                }
            } else {
                wpComApiClient.request { requestBuilder ->
                    requestBuilder.followers().deleteFollower(
                        wpComSiteId = siteId().toULong(),
                        userId = subscriber.userId
                    )
                }
            }
            when (response) {
                is WpRequestResult.Success -> {
                    appLogWrapper.d(AppLog.T.MAIN, "Delete subscriber success")
                    Result.success(true)
                }

                else -> {
                    val error = (response as? WpRequestResult.WpError)?.errorMessage
                    appLogWrapper.e(AppLog.T.MAIN, "Delete subscriber failed: $error")
                    Result.failure(Exception(error))
                }
            }
        }
    }


    /**
     * Called when an item in the list is clicked. We use this to request stats for the clicked subscriber.
     */
    override fun onItemClick(item: DataViewItem) {
        (item.data as? Subscriber)?.let { subscriber ->
            appLogWrapper.d(AppLog.T.MAIN, "Clicked on subscriber ${subscriber.displayNameOrEmail()}")
            _subscriberStats.value = null
            statsJob?.cancel()
            statsJob = launch {
                val stats = fetchSubscriberStats(subscriber.subscriptionId)
                _subscriberStats.value = stats
            }
        }
    }

    /**
     * Trigger the delete confirmation dialog when the user taps the delete button for a subscriber
     */
    fun onDeleteSubscriberClick(subscriber: Subscriber) {
        appLogWrapper.d(AppLog.T.MAIN, "Clicked on delete subscriber ${subscriber.displayNameOrEmail()}")
        _uiEvent.value = UiEvent.ShowDeleteConfirmationDialog(subscriber)
        clearUiEvent()
    }

    /**
     * Subscriber deletion has been confirmed by the user so delete the subscriber
     */
    fun deleteSubscriberConfirmed(subscriber: Subscriber, onSuccess: () -> Unit) {
        launch(ioDispatcher) {
            val result = deleteSubscriber(subscriber = subscriber)

            withContext(mainDispatcher) {
                if (result.isSuccess) {
                    // note that it may take a few seconds for the subscriber to actually be deleted,
                    // which is why we only remove it locally instead of fetching the list again
                    removeItem(subscriber.userId)
                    _uiEvent.value = UiEvent.ShowDeleteSuccessDialog
                    onSuccess()
                } else {
                    _uiEvent.value = UiEvent.ShowToast(R.string.subscribers_delete_failed)
                }
                clearUiEvent()
            }
        }
    }

    private fun clearUiEvent() {
        _uiEvent.value = null
    }

    private enum class SubscriberSortType(val id: Long) {
        DateSubscribed(1L),
        Name(2L)
    }

    private enum class SubscriberFilterType(val id: Long) {
        Email(1L),
        Reader(2L)
    }

    companion object {
        private const val USE_DUMMY_DATA = false
        fun Subscriber.displayNameOrEmail() = displayName.ifEmpty { emailAddress }
    }
}
