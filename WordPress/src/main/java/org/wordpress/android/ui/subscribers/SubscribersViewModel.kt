package org.wordpress.android.ui.subscribers

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.dataview.DataViewFieldType
import org.wordpress.android.ui.dataview.DataViewItem
import org.wordpress.android.ui.dataview.DataViewItemField
import org.wordpress.android.ui.dataview.DataViewItemFilter
import org.wordpress.android.ui.dataview.DataViewItemImage
import org.wordpress.android.ui.dataview.DataViewViewModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.ToastUtilsWrapper
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.Subscriber
import uniffi.wp_api.SubscribersListParams
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SubscribersViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    val appLogWrapper: AppLogWrapper,
    toastUtilsWrapper: ToastUtilsWrapper,
    networkUtilsWrapper: NetworkUtilsWrapper,
    selectedSiteRepository: SelectedSiteRepository
) : DataViewViewModel(
    mainDispatcher = mainDispatcher,
    ioDispatcher = ioDispatcher,
    appLogWrapper = appLogWrapper,
    toastUtilsWrapper = toastUtilsWrapper,
    networkUtilsWrapper = networkUtilsWrapper,
    selectedSiteRepository = selectedSiteRepository
) {
    override fun getSupportedFilters(): List<DataViewItemFilter> {
        return listOf(
            DataViewItemFilter(
                id = ID_FILTER_EMAIL,
                titleRes = R.string.subscribers_filter_email_subscription
            ),
            DataViewItemFilter(
                id = ID_FILTER__TYPE,
                titleRes = R.string.subscribers_filter_subscription_type
            )
        )
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun performNetworkRequest(
        page: Int,
        pageSize: Int,
        searchQuery: String,
        filter: DataViewItemFilter?
    ): List<DataViewItem> = withContext(ioDispatcher) {
        try {
            val params = SubscribersListParams(
                page = 0u,
                perPage = pageSize.toULong(),
                search = searchQuery
            )
            val request = apiClient.request { requestBuilder ->
                requestBuilder.subscribers().listSubscribers(
                    wpComSiteId = siteId().toULong(),
                    params = params
                )
            }
            when (request) {
                is WpRequestResult.Success -> {
                    val subscribers = request.response.data.subscribers
                    appLogWrapper.d(AppLog.T.MAIN, "Fetched ${subscribers.size} subscribers")
                    val items = ArrayList<DataViewItem>()
                    subscribers.forEach { subscriber ->
                        items.add(subscriberToDataViewItem(subscriber))
                    }
                    return@withContext items
                }

                else -> {
                    appLogWrapper.e(AppLog.T.MAIN, "Fetch subscribers failed: $request")
                    (request as? WpRequestResult.WpError)?.let {
                        showError(it.errorMessage)
                    } ?: run {
                        showError(R.string.error_generic_network)
                    }
                    return@withContext emptyList()
                }
            }
        } catch (e: Exception) {
            appLogWrapper.e(AppLog.T.MAIN, "Fetch subscribers failed: $e")
            e.message?.let {
                showError(it)
            } ?: run {
                showError(R.string.error_generic_network)
            }
            return@withContext emptyList()
        }
    }

    private fun subscriberToDataViewItem(subscriber: Subscriber): DataViewItem {
        return DataViewItem(
            id = subscriber.userId,
            image = DataViewItemImage(
                imageUrl = subscriber.avatar,
                fallbackImageRes = R.drawable.ic_user_placeholder_primary_24,
            ),
            fields = listOf(
                DataViewItemField(
                    value = subscriber.displayName.ifEmpty { subscriber.emailAddress },
                    valueType = DataViewFieldType.TITLE,
                    subValue = subscriber.subscriptionStatus,
                    subValueType = DataViewFieldType.SUBTITLE,
                    weight = .6f,
                ),
                DataViewItemField(
                    value = subscriber.dateSubscribed.toString(),
                    valueType = DataViewFieldType.DATE,
                    weight = .4f,
                ),
            )
        )
    }

    companion object {
        private const val ID_FILTER_EMAIL = 1L
        private const val ID_FILTER__TYPE = 2L
    }
}
