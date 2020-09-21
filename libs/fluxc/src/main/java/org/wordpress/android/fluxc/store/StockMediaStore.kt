package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.ASYNC
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.StockMediaAction
import org.wordpress.android.fluxc.action.StockMediaAction.FETCHED_STOCK_MEDIA
import org.wordpress.android.fluxc.action.StockMediaAction.FETCH_STOCK_MEDIA
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.StockMediaModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.stockmedia.StockMediaRestClient
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MEDIA
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockMediaStore @Inject constructor(
    dispatcher: Dispatcher?,
    private val stockMediaRestClient: StockMediaRestClient
) : Store(dispatcher) {
    /**
     * Actions: FETCH_MEDIA_LIST
     */
    data class FetchStockMediaListPayload(val searchTerm: String, val page: Int) : Payload<BaseNetworkError?>()

    /**
     * Actions: FETCHED_MEDIA_LIST
     */
    class FetchedStockMediaListPayload(
        val mediaList: List<StockMediaModel>,
        val searchTerm: String,
        val nextPage: Int,
        val canLoadMore: Boolean
    ) : Payload<StockMediaError?>() {
        constructor(error: StockMediaError, searchTerm: String) : this(listOf(), searchTerm, 0, false) {
            this.error = error
        }
    }

    class OnStockMediaListFetched(
        val mediaList: List<StockMediaModel>,
        val searchTerm: String,
        val nextPage: Int,
        val canLoadMore: Boolean
    ) : OnChanged<StockMediaError?>() {
        constructor(error: StockMediaError, searchTerm: String) : this(listOf(), searchTerm, 0, false) {
            this.error = error
        }
    }

    enum class StockMediaErrorType {
        GENERIC_ERROR;

        companion object {
            @JvmStatic fun fromBaseNetworkError(baseError: BaseNetworkError?): StockMediaErrorType {
                // endpoint returns an empty media list for any type of error, including timeouts, server error, etc.
                return GENERIC_ERROR
            }
        }
    }

    data class StockMediaError(val type: StockMediaErrorType, val message: String) : OnChangedError

    @Subscribe(threadMode = ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? StockMediaAction ?: return
        when (actionType) {
            FETCH_STOCK_MEDIA -> performFetchStockMediaList(action.payload as FetchStockMediaListPayload)
            FETCHED_STOCK_MEDIA -> handleStockMediaListFetched(action.payload as FetchedStockMediaListPayload)
        }
    }

    override fun onRegister() {
        AppLog.d(MEDIA, "StockMediaStore onRegister")
    }

    private fun performFetchStockMediaList(payload: FetchStockMediaListPayload) {
        stockMediaRestClient.searchStockMedia(payload.searchTerm, payload.page)
    }

    private fun handleStockMediaListFetched(payload: FetchedStockMediaListPayload) {
        val onStockMediaListFetched: OnStockMediaListFetched = if (payload.isError) {
            OnStockMediaListFetched(payload.error!!, payload.searchTerm)
        } else {
            OnStockMediaListFetched(
                    payload.mediaList,
                    payload.searchTerm,
                    payload.nextPage,
                    payload.canLoadMore
            )
        }
        emitChange(onStockMediaListFetched)
    }
}
