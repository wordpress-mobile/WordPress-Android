package org.wordpress.android.ui.subscribers

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.models.wrappers.SimpleDateFormatWrapper
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.dataview.DataViewFieldType
import org.wordpress.android.ui.dataview.DataViewItem
import org.wordpress.android.ui.dataview.DataViewItemField
import org.wordpress.android.ui.dataview.DataViewDropdownItem
import org.wordpress.android.ui.dataview.DataViewItemImage
import org.wordpress.android.ui.dataview.DataViewViewModel
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.ListSubscribersSortField
import uniffi.wp_api.Subscriber
import uniffi.wp_api.SubscriberType
import uniffi.wp_api.SubscribersListParams
import uniffi.wp_api.WpApiParamOrder
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SubscribersViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val appLogWrapper: AppLogWrapper,
) : DataViewViewModel(
    mainDispatcher = mainDispatcher,
    appLogWrapper = appLogWrapper
) {
    @Inject
    lateinit var dateFormatWrapper: SimpleDateFormatWrapper

    override fun getSupportedFilters(): List<DataViewDropdownItem> {
        return listOf(
            DataViewDropdownItem(
                id = ID_FILTER_EMAIL,
                titleRes = R.string.subscribers_filter_email_subscription
            ),
            DataViewDropdownItem(
                id = ID_FILTER_READER,
                titleRes = R.string.subscribers_filter_reader_subscription
            )
        )
    }

    override fun getSupportedSorts(): List<DataViewDropdownItem> {
        return listOf(
            DataViewDropdownItem(
                id = ID_SORT_DATE,
                titleRes = R.string.subscribers_sort_date
            ),
            DataViewDropdownItem(
                id = ID_SORT_DISPLAY_NAME,
                titleRes = R.string.subscribers_sort_display_name
            ),
            DataViewDropdownItem(
                id = ID_SORT_EMAIL,
                titleRes = R.string.subscribers_sort_email
            ),
            DataViewDropdownItem(
                id = ID_SORT_PLAN,
                titleRes = R.string.subscribers_sort_plan
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
    ): List<DataViewItem> {
        val filterType = filter?.let {
            when (it.id) {
                ID_FILTER_EMAIL -> SubscriberType.EmailSubscriber
                ID_FILTER_READER -> SubscriberType.ReaderSubscriber
                else -> null
            }
        }

        val sortType = sortBy?.let {
            when (it.id) {
                ID_SORT_DATE -> ListSubscribersSortField.DATE_SUBSCRIBED
                ID_SORT_PLAN -> ListSubscribersSortField.PLAN
                ID_SORT_DISPLAY_NAME -> ListSubscribersSortField.DISPLAY_NAME
                ID_SORT_EMAIL -> ListSubscribersSortField.EMAIL_ADDRESS
                else -> null
            }
        }

        val params = SubscribersListParams(
            page = page.toULong(),
            perPage = PAGE_SIZE.toULong(),
            sortOrder = sortOrder,
            search = searchQuery,
            filter = filterType,
            sort = sortType
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
                return subscribers.map { subscriberToDataViewItem(it) }
            }

            else -> {
                appLogWrapper.e(AppLog.T.MAIN, "Fetch subscribers failed: $response")
                onError((response as? WpRequestResult.WpError)?.errorMessage)
                return emptyList()
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
                    value = subscriber.subscriptionStatus,
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

    /*
     * Returns the subscriber with the given ID, or null if not found. Note that this does NOT do a network call,
     * it simply returns the subscriber from the existing list of items.
     */
    fun getSubscriber(userId: Long): Subscriber? {
        val item = items.value.firstOrNull { it.id == userId }
        return item?.data as? Subscriber
    }

    /**
     * Called when an item in the list is clicked.
     */
    override fun onItemClick(item: DataViewItem) {
        (item.data as? Subscriber)?.let { subscriber ->
            appLogWrapper.d(AppLog.T.MAIN, "Clicked on subscriber ${subscriber.displayNameOrEmail()}")
        }
    }

    companion object {
        private const val ID_FILTER_EMAIL = 1L
        private const val ID_FILTER_READER = 2L

        private const val ID_SORT_DATE = 1L
        private const val ID_SORT_DISPLAY_NAME = 2L
        private const val ID_SORT_EMAIL = 3L
        private const val ID_SORT_PLAN = 4L

        fun Subscriber.displayNameOrEmail() = displayName.ifEmpty { emailAddress }
    }
}
