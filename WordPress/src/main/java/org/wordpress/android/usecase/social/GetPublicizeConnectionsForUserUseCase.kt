package org.wordpress.android.usecase.social

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.datasets.wrappers.PublicizeTableWrapper
import org.wordpress.android.models.PublicizeConnection
import org.wordpress.android.models.PublicizeService
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.publicize.services.PublicizeUpdateServicesV2
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Returns a list of connections available for a user. See PublicizeConnectionList#getServiceConnectionsForUser
 */
class GetPublicizeConnectionsForUserUseCase @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val publicizeTableWrapper: PublicizeTableWrapper,
    private val publicizeUpdateServicesV2: PublicizeUpdateServicesV2,
) {
    suspend fun execute(siteId: Long, userId: Long, shouldForceUpdate: Boolean = true): List<PublicizeConnection> =
        withContext(ioDispatcher) {
            val allSupportedServices = if (shouldForceUpdate) {
                fetchServices(siteId)
            } else {
                publicizeTableWrapper.getServiceList().run {
                    ifEmpty { fetchServices(siteId) }
                }
            }.filter { it.status != PublicizeService.Status.UNSUPPORTED }
            val allConnectionsForSite = if (shouldForceUpdate) {
                fetchConnections(siteId)
            } else {
                publicizeTableWrapper.getConnectionsForSite(siteId).run {
                    ifEmpty { fetchConnections(siteId) }
                }
            }
            val connectionsForUser: List<PublicizeConnection> = allSupportedServices.map { service ->
                allConnectionsForSite.filter {
                    it.service.equals(service.id, true) && (it.isShared || it.userId.toLong() == userId)
                }
            }.flatten()
            connectionsForUser
        }

    private suspend fun fetchServices(siteId: Long): List<PublicizeService> =
        suspendCoroutine { cont ->
            publicizeUpdateServicesV2.updateServices(
                siteId = siteId,
                success = { services -> cont.resume(services) },
                failure = {
                    AppLog.e(AppLog.T.SHARING, "Error updating publicize services", it)
                    cont.resume(emptyList())
                }
            )
        }

    private suspend fun fetchConnections(siteId: Long): List<PublicizeConnection> =
        suspendCoroutine { cont ->
            publicizeUpdateServicesV2.updateConnections(
                siteId = siteId,
                success = { connections -> cont.resume(connections) },
                failure = {
                    AppLog.e(AppLog.T.SHARING, "Error updating publicize connections", it)
                    cont.resume(emptyList())
                }
            )
        }
}
