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
import org.wordpress.android.ui.dataview.DataViewItemFilter
import org.wordpress.android.ui.dataview.DataViewItemImage
import org.wordpress.android.ui.dataview.DataViewViewModel
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpRequestResult
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

    override fun getSupportedFilters(): List<DataViewItemFilter> {
        return listOf(
            DataViewItemFilter(
                id = ID_FILTER_EMAIL,
                titleRes = R.string.subscribers_filter_email_subscription
            ),
            DataViewItemFilter(
                id = ID_FILTER_READER,
                titleRes = R.string.subscribers_filter_reader_subscription
            )
        )
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun performNetworkRequest(
        page: Int,
        sortOrder: WpApiParamOrder,
        searchQuery: String,
        filter: DataViewItemFilter?
    ): List<DataViewItem> = withContext(ioDispatcher) {
        try {
            requestSubscribers(filter, page, sortOrder, searchQuery)
        } catch (e: Exception) {
            appLogWrapper.e(AppLog.T.MAIN, "Fetch subscribers failed: $e")
            onError(e.message)
            emptyList()
        }
    }

    private suspend fun requestSubscribers(
        filter: DataViewItemFilter?,
        page: Int,
        sortOrder: WpApiParamOrder,
        searchQuery: String
    ): List<DataViewItem> {
        val filterType = filter?.let {
            when (it.id) {
                ID_FILTER_EMAIL -> SubscriberType.EmailSubscriber
                ID_FILTER_READER -> SubscriberType.ReaderSubscriber
                else -> null
            }
        }

        val params = SubscribersListParams(
            page = page.toULong(),
            perPage = PAGE_SIZE.toULong(),
            sortOrder = sortOrder,
            search = searchQuery,
            filter = filterType,
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

    fun getSubscriber(userId: Long): Subscriber? {
        val item = items.value.firstOrNull { it.id == userId }
        return item?.data as? Subscriber
    }

    override fun onItemClick(item: DataViewItem) {
        (item.data as? Subscriber)?.let { subscriber ->
            appLogWrapper.d(AppLog.T.MAIN, "Clicked on subscriber ${subscriber.displayNameOrEmail()}")
        }
    }

    companion object {
        private const val ID_FILTER_EMAIL = 1L
        private const val ID_FILTER_READER = 2L

        fun Subscriber.displayNameOrEmail() = displayName.ifEmpty { emailAddress }
    }
}
