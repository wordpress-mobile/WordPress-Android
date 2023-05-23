package org.wordpress.android.ui.publicize

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.publicize.services.PublicizeUpdateServicesV2
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class PublicizeListViewModel @Inject constructor(
    private val publicizeUpdateServicesV2: PublicizeUpdateServicesV2,
    private val eventBusWrapper: EventBusWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) : ScopedViewModel(bgDispatcher) {
    fun onSiteAvailable(siteModel: SiteModel) {
        val siteId = siteModel.siteId
        updateServices(siteId)
        updateConnections(siteId)
    }

    private fun updateServices(siteId: Long) {
        publicizeUpdateServicesV2.updateServices(
            siteId = siteId,
            success = { eventBusWrapper.post(PublicizeEvents.ConnectionsChanged()) },
            failure = { AppLog.e(AppLog.T.SHARING, "Error updating publicize services", it) }
        )
    }

    private fun updateConnections(siteId: Long) {
        publicizeUpdateServicesV2.updateConnections(
            siteId = siteId,
            success = { eventBusWrapper.post(PublicizeEvents.ConnectionsChanged()) },
            failure = { AppLog.e(AppLog.T.SHARING, "Error updating publicize connections", it) }
        )
    }
}
