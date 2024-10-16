package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.plans.full.Plan
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.plans.PlansRestClient
import org.wordpress.android.fluxc.store.Store.OnChanged
import org.wordpress.android.fluxc.store.Store.OnChangedError
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlansStore @Inject constructor(
    private val restClient: PlansRestClient,
    private val coroutineEngine: CoroutineEngine,
) {
    suspend fun fetchPlans(): OnPlansFetched =
        coroutineEngine.withDefaultContext(T.API, this, "Fetch plans") {
            return@withDefaultContext when (val response = restClient.fetchPlans()) {
                is Success -> {
                    OnPlansFetched(response.data.toList())
                }
                is Error -> {
                    OnPlansFetched(
                        FetchPlansError(response.error.volleyError.message ?: "Unknown error")
                    )
                }
            }
        }

    data class OnPlansFetched(val plans: List<Plan>? = null) : OnChanged<FetchPlansError>() {
        constructor(error: FetchPlansError) : this() {
            this.error = error
        }
    }

    data class FetchPlansError(
        val message: String,
    ) : OnChangedError
}
