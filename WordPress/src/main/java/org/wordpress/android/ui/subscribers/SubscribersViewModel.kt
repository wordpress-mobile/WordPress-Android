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
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.ParsedUrl
import uniffi.wp_api.SubscribersListParams
import uniffi.wp_api.WpAuthenticationProvider
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SubscribersViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    val appLogWrapper: AppLogWrapper,
    networkUtilsWrapper: NetworkUtilsWrapper
) : DataViewViewModel(
    mainDispatcher = mainDispatcher,
    ioDispatcher = ioDispatcher,
    appLogWrapper = appLogWrapper,
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
        // This is the url from api discovery process and should be stored somewhere
        val apiRootUrl = ParsedUrl.parse("https://remote-wildfowl-pollan.jurassic.ninja/wp-json")

        val client = WpComApiClient(authProvider)
        val params = SubscribersListParams(
            page = 0u,
            perPage = pageSize.toULong(),
            search = searchQuery
        )
        val request = client.request { requestBuilder ->
            val response = requestBuilder.subscribers().listSubscribers(
                wpComSiteId = getSiteId().toULong(),
                params = params
            )
            val subscribers = response.data.subscribers
        }

        when (request) {
            is WpRequestResult.Success -> {
                appLogWrapper.d(AppLog.T.MAIN, "Fetch subscribers success")
            }

            else -> {
                appLogWrapper.e(AppLog.T.MAIN, "Fetch subscribers failed: ${request}")
            }
        }

        val offset = page * pageSize
        getDummyDataViewItems(offset)
    }

    companion object {
        private const val ID_FILTER_EMAIL = 1L
        private const val ID_FILTER__TYPE = 2L
    }
}
