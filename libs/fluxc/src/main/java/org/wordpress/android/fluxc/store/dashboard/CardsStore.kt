package org.wordpress.android.fluxc.store.dashboard

import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.dashboard.CardsMapper
import org.wordpress.android.fluxc.model.dashboard.CardsModel
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.CardsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.PostsResponse
import org.wordpress.android.fluxc.store.Store
import org.wordpress.android.fluxc.store.Store.OnChangedError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardsStore @Inject constructor(
    private val mapper: CardsMapper,
    private val coroutineEngine: CoroutineEngine
) {
    @Suppress("unused", "UNUSED_PARAMETER")
    suspend fun fetchCards(
        site: SiteModel
    ) = coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchCards") {
        // TODO: Fetch from rest.
        return@withDefaultContext storeCards(
                FetchedCardsPayload(
                        CardsResponse(
                                posts = PostsResponse(
                                        hasPublished = false,
                                        draft = listOf(),
                                        scheduled = listOf()
                                )
                        )
                )
        )
    }

    private fun storeCards(
        payload: FetchedCardsPayload<CardsResponse>
    ): OnCardsFetched<CardsModel> {
        return when {
            payload.isError -> OnCardsFetched(payload.error)
            payload.response != null -> {
                // TODO: Store in db.
                OnCardsFetched(mapper.map(payload.response))
            }
            else -> OnCardsFetched(CardsError(INVALID_RESPONSE))
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    fun getCards(
        site: SiteModel
    ) = coroutineEngine.run(AppLog.T.API, this, "getCards") {
        // TODO: Get from db.
    }

    /* PAYLOADS */

    data class FetchedCardsPayload<T>(
        val response: T? = null
    ) : Payload<CardsError>() {
        @Suppress("unused")
        constructor(error: CardsError) : this() {
            this.error = error
        }
    }

    /* ACTIONS */

    data class OnCardsFetched<T>(
        val model: T? = null,
        val cached: Boolean = false
    ) : Store.OnChanged<CardsError>() {
        constructor(error: CardsError) : this() {
            this.error = error
        }
    }

    /* ERRORS */

    enum class CardsErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE,
        API_ERROR,
        TIMEOUT
    }

    class CardsError(var type: CardsErrorType, var message: String? = null) : OnChangedError
}
