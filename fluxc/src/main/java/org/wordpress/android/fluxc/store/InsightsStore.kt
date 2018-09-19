package org.wordpress.android.fluxc.store

import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.StatsRestClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.experimental.CoroutineContext

@Singleton
class InsightsStore
@Inject constructor(private val statsRestClient: StatsRestClient, private val coroutineContext: CoroutineContext)  {
    suspend fun fetchAllTimeInsights(site: SiteModel) = withContext(coroutineContext) {
        return@withContext statsRestClient.fetchAllTimeInsights(site)
    }

    data class FetchAllTimeInsightsPayload(
        val model: InsightsAllTimeModel? = null
    ) : Payload<StatsError>() {
        constructor(error: StatsError) : this() {
            this.error = error
        }
    }

    enum class StatsErrorType {
        GENERIC_ERROR,
        API_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE
    }
    class StatsError(var type: StatsErrorType, var message: String? = null) : Store.OnChangedError
}
