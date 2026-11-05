package org.wordpress.android.ui.reader.repository.usecases.tags

import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.NetworkUnavailable
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.RemoteRequestFailure
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Success
import org.wordpress.android.ui.reader.repository.ReaderTagsAndSitesUpdater
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

class FetchFollowedTagsUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val tagsAndSitesUpdater: ReaderTagsAndSitesUpdater
) {
    suspend fun fetch(): ReaderRepositoryCommunication = when {
        !networkUtilsWrapper.isNetworkAvailable() -> NetworkUnavailable
        tagsAndSitesUpdater.updateTags() -> Success
        else -> RemoteRequestFailure
    }
}
