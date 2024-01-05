package org.wordpress.android.fluxc.store.dashboard

import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.CardsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.FetchCardsPayload
import org.wordpress.android.fluxc.persistence.dashboard.CardsDao
import org.wordpress.android.fluxc.store.Store
import org.wordpress.android.fluxc.store.Store.OnChangedError
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardsStore @Inject constructor(
    private val restClient: CardsRestClient,
    private val cardsDao: CardsDao,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchCards(
        fetchCardsPayload: FetchCardsPayload
    ) = coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchCards") {
        val payload = restClient.fetchCards(fetchCardsPayload)
        return@withDefaultContext storeCards(fetchCardsPayload.site, payload)
    }

    private suspend fun storeCards(
        site: SiteModel,
        payload: CardsPayload<CardsResponse>
    ): CardsResult<List<CardModel>> = when {
        payload.isError -> handlePayloadError(payload.error)
        payload.response != null -> handlePayloadResponse(site, payload.response)
        else -> CardsResult(CardsError(CardsErrorType.INVALID_RESPONSE))
    }

    private fun handlePayloadError(
        error: CardsError
    ): CardsResult<List<CardModel>> = when (error.type) {
        CardsErrorType.AUTHORIZATION_REQUIRED -> {
            cardsDao.clear()
            CardsResult()
        }
        else -> CardsResult(error)
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun handlePayloadResponse(
        site: SiteModel,
        response: CardsResponse
    ): CardsResult<List<CardModel>> = try {
        cardsDao.insertWithDate(site.id, response.toCards())
        CardsResult()
    } catch (e: Exception) {
        CardsResult(CardsError(CardsErrorType.GENERIC_ERROR))
    }

    fun getCards(
        site: SiteModel,
    ) = cardsDao.get(site.id).map { cards ->
            cards.map { it.toCard() }}
        .map { CardsResult(it) }

    /* PAYLOADS */

    data class CardsPayload<T>(
        val response: T? = null
    ) : Payload<CardsError>() {
        constructor(error: CardsError) : this() {
            this.error = error
        }
    }

    /* ACTIONS */

    data class CardsResult<T>(
        val model: T? = null,
        val cached: Boolean = false
    ) : Store.OnChanged<CardsError>() {
        constructor(error: CardsError) : this() {
            this.error = error
        }
    }

    /* ERRORS */

    enum class TodaysStatsCardErrorType {
        JETPACK_DISCONNECTED,
        JETPACK_DISABLED,
        UNAUTHORIZED,
        GENERIC_ERROR
    }

    class TodaysStatsCardError(
        val type: TodaysStatsCardErrorType,
        val message: String? = null
    ) : OnChangedError

    enum class PostCardErrorType {
        UNAUTHORIZED,
        GENERIC_ERROR
    }

    class PostCardError(
        val type: PostCardErrorType,
        val message: String? = null
    ) : OnChangedError

    enum class ActivityCardErrorType {
        UNAUTHORIZED,
        GENERIC_ERROR
    }
    class ActivityCardError(
        val type: ActivityCardErrorType,
        val message: String? = null
    ) : OnChangedError

    enum class CardsErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE,
        API_ERROR,
        TIMEOUT
    }

    class CardsError(
        val type: CardsErrorType,
        val message: String? = null
    ) : OnChangedError
}
