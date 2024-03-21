package org.wordpress.android.ui.publicize.services

import com.wordpress.rest.RestRequest
import org.wordpress.android.datasets.PublicizeTable
import org.wordpress.android.models.PublicizeConnectionList
import org.wordpress.android.models.PublicizeServiceList
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.RestClientProvider
import java.util.Locale
import javax.inject.Inject

class PublicizeUpdateServicesV2 @Inject constructor(
    private val restClientProvider: RestClientProvider,
) {
    /*
     * Updates the list of publicize services
     */
    fun updateServices(siteId: Long, success: (PublicizeServiceList) -> Unit, failure: (Throwable) -> Unit) {
        AppLog.d(AppLog.T.SHARING, "${PublicizeUpdateServicesV2::class.simpleName}: updating services")
        val listener = RestRequest.Listener { jsonObject ->
            val serverList = PublicizeServiceList.fromJson(jsonObject)
            val localList = PublicizeTable.getServiceList()
            if (!serverList.isSameAs(localList)) {
                PublicizeTable.setServiceList(serverList)
            }
            success(serverList)
        }
        val errorListener = RestRequest.ErrorListener { volleyError -> failure(volleyError) }
        val path = "sites/$siteId/external-services?type=publicize"
        restClientProvider.getRestClientUtilsV2().getWithLocale(path, listener, errorListener)
    }

    /*
     * Updates the connections for a site
     */
    fun updateConnections(siteId: Long, success: (PublicizeConnectionList) -> Unit, failure: (Throwable) -> Unit) {
        AppLog.d(AppLog.T.SHARING, "${PublicizeUpdateServicesV2::class.simpleName}: updating connections")
        val listener = RestRequest.Listener { jsonObject ->
            val serverList = PublicizeConnectionList.fromJson(jsonObject)
            val localList = PublicizeTable.getConnectionsForSite(siteId)
            if (!serverList.isSameAs(localList)) {
                PublicizeTable.setConnectionsForSite(siteId, serverList)
            }
            success(serverList)
        }
        val errorListener = RestRequest.ErrorListener { volleyError -> failure(volleyError) }
        val path = String.format(Locale.ROOT, "sites/%d/publicize-connections", siteId)
        restClientProvider.getRestClientUtilsV1_1().getWithLocale(path, listener, errorListener)
    }
}
