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
import org.wordpress.android.util.NetworkUtilsWrapper
import rs.wordpress.api.kotlin.WpApiClient
import uniffi.wp_api.ParsedUrl
import uniffi.wp_api.SubscribersListParams
import uniffi.wp_api.SubscribersRequestExecutor
import uniffi.wp_api.WpAuthenticationProvider
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SubscribersViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    appLogWrapper: AppLogWrapper,
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
    ): List<DataViewItem> {
        withContext(ioDispatcher) {
            // These credentials are from a dummy site, so it's safe to check them in during testing
            val authProvider = WpAuthenticationProvider.staticWithUsernameAndPassword(
                username = "demo", password = "FKnT 3P5E aIUs xCIz vb6T 20Ni"
            )
            // This is the url from api discovery process and should be stored somewhere
            val apiRootUrl = ParsedUrl.parse("https://remote-wildfowl-pollan.jurassic.ninja/wp-json")

            val client = WpApiClient(apiRootUrl, authProvider)
            val params = SubscribersListParams(
                page = 0u,
                perPage = pageSize.toULong(),
                search = searchQuery
            )
            client.request { requestBuilder ->
                SubscribersRequestExecutor.listSubscribers
                requestBuilder.
            }
            // getDummyDataViewItems(offset)
        }
    }

    companion object {
        private const val ID_FILTER_EMAIL = 1L
        private const val ID_FILTER__TYPE = 2L
    }
}
