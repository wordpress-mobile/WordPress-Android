package org.wordpress.android.datasets.wrappers

import org.wordpress.android.datasets.PublicizeTable
import org.wordpress.android.models.PublicizeConnection
import org.wordpress.android.models.PublicizeService
import javax.inject.Inject

class PublicizeTableWrapper @Inject constructor() {
    fun getConnectionsForSite(siteId: Long): List<PublicizeConnection> =
        PublicizeTable.getConnectionsForSite(siteId)

    fun getServiceList(): List<PublicizeService> =
        PublicizeTable.getServiceList()
}
