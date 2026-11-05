package org.wordpress.android.ui.reader.repository.usecases.tags

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.NetworkUnavailable
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.RemoteRequestFailure
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Success
import org.wordpress.android.ui.reader.repository.ReaderTagsAndSitesUpdater
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

class FollowInterestTagsUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val accountStore: AccountStore,
    private val tagsAndSitesUpdater: ReaderTagsAndSitesUpdater
) {
    suspend fun followInterestTags(tags: List<ReaderTag>): ReaderRepositoryCommunication = when {
        // logged out users follow tags locally, so they don't need a connection
        accountStore.hasAccessToken() && !networkUtilsWrapper.isNetworkAvailable() -> NetworkUnavailable
        tagsAndSitesUpdater.followTags(tags) -> Success
        else -> RemoteRequestFailure
    }
}
