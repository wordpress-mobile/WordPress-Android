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
import org.wordpress.android.util.NetworkUtilsWrapper
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
        offset: Int,
        searchQuery: String,
        filter: DataViewItemFilter?
    ): List<DataViewItem> = withContext(ioDispatcher) {
        getDummyDataViewItems(offset)
    }

    companion object {
        private const val ID_FILTER_EMAIL = 1L
        private const val ID_FILTER__TYPE = 2L
    }
}
