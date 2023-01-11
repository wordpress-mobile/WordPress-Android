package org.wordpress.android.ui.reader.repository.usecases

import org.wordpress.android.ui.reader.repository.ReaderDiscoverCommunication
import org.wordpress.android.ui.reader.repository.ReaderDiscoverCommunication.Error.NetworkUnavailable
import org.wordpress.android.ui.reader.repository.ReaderDiscoverCommunication.Error.ServiceNotStarted
import org.wordpress.android.ui.reader.repository.ReaderDiscoverCommunication.Started
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverServiceStarter
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class FetchDiscoverCardsUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val contextProvider: ContextProvider
) {
    fun fetch(discoverTask: DiscoverTasks): ReaderDiscoverCommunication {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return NetworkUnavailable(discoverTask)
        }

        val isStarted =
            ReaderDiscoverServiceStarter.startService(contextProvider.getContext(), discoverTask)

        return if (isStarted)
            Started(discoverTask)
        else
            ServiceNotStarted(discoverTask)
    }
}
