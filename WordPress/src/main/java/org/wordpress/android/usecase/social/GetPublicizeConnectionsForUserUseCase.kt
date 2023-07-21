package org.wordpress.android.usecase.social

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.datasets.wrappers.PublicizeTableWrapper
import org.wordpress.android.models.PublicizeConnection
import org.wordpress.android.models.PublicizeService
import org.wordpress.android.modules.IO_THREAD
import javax.inject.Inject
import javax.inject.Named

/**
 * Returns a list of connections available for a user. See PublicizeConnectionList#getServiceConnectionsForUser
 */
class GetPublicizeConnectionsForUserUseCase @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val publicizeTableWrapper: PublicizeTableWrapper,
) {
    suspend fun execute(siteId: Long, userId: Long): List<PublicizeConnection> =
        withContext(ioDispatcher) {
            val allSupportedServices = publicizeTableWrapper.getServiceList()
                .filter { it.status != PublicizeService.Status.UNSUPPORTED }
            val allConnectionsForSite = publicizeTableWrapper.getConnectionsForSite(siteId)
            val connectionsForUser: List<PublicizeConnection> = allSupportedServices.map { service ->
                allConnectionsForSite.filter {
                    it.service.equals(service.id, true) && (it.isShared || it.userId.toLong() == userId)
                }
            }.flatten()
            connectionsForUser
        }
}
