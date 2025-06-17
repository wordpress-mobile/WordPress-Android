package org.wordpress.android.ui.subscribers

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.dataview.DataViewItem
import org.wordpress.android.ui.dataview.DataViewItemFilter
import org.wordpress.android.ui.dataview.DataViewViewModel
import org.wordpress.android.ui.dataview.DummyDataViewItems.getDummyDataViewItems
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.ToastUtilsWrapper
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.SubscribersListParams
import uniffi.wp_api.WpAuthenticationProvider
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SubscribersViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    val appLogWrapper: AppLogWrapper,
    toastUtilsWrapper: ToastUtilsWrapper,
    networkUtilsWrapper: NetworkUtilsWrapper
) : DataViewViewModel(
    mainDispatcher = mainDispatcher,
    ioDispatcher = ioDispatcher,
    appLogWrapper = appLogWrapper,
    toastUtilsWrapper = toastUtilsWrapper,
    networkUtilsWrapper = networkUtilsWrapper
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

    override suspend fun performNetworkRequest(
        page: Int,
        pageSize: Int,
        searchQuery: String,
        filter: DataViewItemFilter?
    ): List<DataViewItem> = withContext(ioDispatcher) {
        // These credentials are from a dummy site, so it's safe to check them in during testing
        val authProvider = WpAuthenticationProvider.staticWithUsernameAndPassword(
            username = "demo", password = "FKnT 3P5E aIUs xCIz vb6T 20Ni"
        )

        val client = WpComApiClient(authProvider)
        val params = SubscribersListParams(
            page = 0u,
            perPage = pageSize.toULong(),
            search = searchQuery
        )
        try {
            val request = client.request { requestBuilder ->
                requestBuilder.subscribers().listSubscribers(
                    wpComSiteId = siteId.toULong(),
                    params = params
                )
            }
            when (request) {
                is WpRequestResult.Success -> {
                    val subscribers = request.response.data.subscribers
                    appLogWrapper.d(AppLog.T.MAIN, "Fetched ${subscribers.size} subscribers")
                }

                else -> {
                    appLogWrapper.e(AppLog.T.MAIN, "Fetch subscribers failed: $request")
                    (request as? WpRequestResult.WpError)?.let{
                        showToast(it.errorMessage)
                    } ?: run {
                        showToast(R.string.error_generic_network)
                    }
                }
            }

            val offset = page * pageSize
            getDummyDataViewItems(offset)
        } catch (e: Exception) {
            appLogWrapper.e(AppLog.T.MAIN, "Fetch subscribers failed: $e")
            e.message?.let {
                showToast(it)
            } ?: run {
                showToast(R.string.error_generic_network)
            }
            return@withContext emptyList()
        }
    }

    companion object {
        private const val ID_FILTER_EMAIL = 1L
        private const val ID_FILTER__TYPE = 2L
    }
}
