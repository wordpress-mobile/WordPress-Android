package org.wordpress.android.ui.publicize.services

import com.wordpress.rest.RestRequest
import org.wordpress.android.datasets.PublicizeTable
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.PublicizeConnectionList
import org.wordpress.android.models.PublicizeServiceList
import org.wordpress.android.ui.publicize.PublicizeEvents.ConnectionsChanged
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.RestClientProvider
import java.util.Locale
import javax.inject.Inject

class PublicizeUpdateServicesV2 @Inject constructor(
    private val restClientProvider: RestClientProvider,
    private val eventBusWrapper: EventBusWrapper,
) {
    suspend fun execute(siteModel: SiteModel) {
        AppLog.d(AppLog.T.SHARING, "${PublicizeUpdateServicesV2::class.simpleName}: updating services and connections")
        updateServices(siteModel.siteId)
        updateConnections(siteModel.siteId)
    }

    /*
     * update the list of publicize services
     */
    private fun updateServices(siteId: Long) {
        val listener = RestRequest.Listener { jsonObject ->
            val serverList = PublicizeServiceList.fromJson(jsonObject)
            val localList = PublicizeTable.getServiceList()
            if (!serverList.isSameAs(localList)) {
                PublicizeTable.setServiceList(serverList)
                eventBusWrapper.post(ConnectionsChanged())
            }
        }
        val errorListener = RestRequest.ErrorListener { volleyError -> AppLog.e(AppLog.T.SHARING, volleyError) }
        val path = "sites/$siteId/external-services?type=publicize"
        restClientProvider.getRestClientUtilsV2()[path, null, null, listener, errorListener]
    }

    /*
     * update the connections for the passed blog
     */
    private fun updateConnections(siteId: Long) {
        val listener = RestRequest.Listener { jsonObject ->
            val serverList = PublicizeConnectionList.fromJson(jsonObject)
            val localList = PublicizeTable.getConnectionsForSite(siteId)
            if (!serverList.isSameAs(localList)) {
                PublicizeTable.setConnectionsForSite(siteId, serverList)
                eventBusWrapper.post(ConnectionsChanged())
            }
        }
        val errorListener = RestRequest.ErrorListener { volleyError -> AppLog.e(AppLog.T.SHARING, volleyError) }
        val path = String.format(Locale.ROOT, "sites/%d/publicize-connections", siteId)
        restClientProvider.getRestClientUtilsV1_1()[path, null, null, listener, errorListener]
    }
}
