package org.wordpress.android.fluxc.network.rest.wpcom.experiments

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.experiments.AssignmentsModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.ExperimentStore.ExperimentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.ExperimentStore.FetchAssignmentsError
import org.wordpress.android.fluxc.store.ExperimentStore.FetchedAssignmentsPayload
import org.wordpress.android.fluxc.store.ExperimentStore.Platform
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ExperimentRestClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    dispatcher: Dispatcher,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchAssignments(
        platform: Platform,
        experimentNames: List<String>,
        anonymousId: String? = null,
        version: String = DEFAULT_VERSION
    ): FetchedAssignmentsPayload {
        val url = WPCOMV2.experiments.version(version).assignments.platform(platform.value).url
        val params = mapOf(
                "experiment_names" to experimentNames.joinToString(","),
                "anon_id" to anonymousId.orEmpty()
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                FetchAssignmentsResponse::class.java,
                enableCaching = false,
                forced = true
        )
        return when (response) {
            is Success -> FetchedAssignmentsPayload(response.data.let { AssignmentsModel(it.variations, it.ttl) })
            is Error -> FetchedAssignmentsPayload(FetchAssignmentsError(GENERIC_ERROR, response.error.message))
        }
    }

    data class FetchAssignmentsResponse(
        val variations: Map<String, String?>,
        val ttl: Int
    )

    companion object {
        const val DEFAULT_VERSION = "0.1.0"
    }
}
