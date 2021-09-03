package org.wordpress.android.ui.stats.refresh

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.JetpackStore.ActivateStatsModulePayload
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.stats.refresh.StatsModuleActivateRequestState.Failure.NetworkUnavailable
import org.wordpress.android.ui.stats.refresh.StatsModuleActivateRequestState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.stats.refresh.StatsModuleActivateRequestState.Success
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import javax.inject.Named

class StatsModuleActivateUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val jetpackStore: JetpackStore,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun postActivateStatsModule(site: SiteModel): StatsModuleActivateRequestState = withContext(ioDispatcher) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return@withContext NetworkUnavailable
        }
        val result = jetpackStore.activateStatsModule(ActivateStatsModulePayload(site))
        if (result.isError) {
            RemoteRequestFailure
        } else {
            Success
        }
    }
}
