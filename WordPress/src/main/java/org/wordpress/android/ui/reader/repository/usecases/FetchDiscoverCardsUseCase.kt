package org.wordpress.android.ui.reader.repository.usecases

import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.NetworkUnavailable
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Started
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverServiceStarter
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class FetchDiscoverCardsUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val contextProvider: ContextProvider
) {
    fun fetch(discoverTask: DiscoverTasks): ReaderRepositoryCommunication {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return NetworkUnavailable
        }

        ReaderDiscoverServiceStarter.startService(contextProvider.getContext(), discoverTask)

        return Started
    }
}
