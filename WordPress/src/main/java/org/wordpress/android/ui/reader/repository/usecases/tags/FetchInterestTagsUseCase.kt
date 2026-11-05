package org.wordpress.android.ui.reader.repository.usecases.tags

import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.NetworkUnavailable
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.RemoteRequestFailure
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.SuccessWithData
import org.wordpress.android.ui.reader.repository.ReaderTagsAndSitesUpdater
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

class FetchInterestTagsUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val tagsAndSitesUpdater: ReaderTagsAndSitesUpdater
) {
    suspend fun fetch(): ReaderRepositoryCommunication {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return NetworkUnavailable
        }
        return tagsAndSitesUpdater.fetchInterestTags()?.let { SuccessWithData(it) } ?: RemoteRequestFailure
    }
}
