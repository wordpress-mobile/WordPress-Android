package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.ASYNC
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.StockMediaAction
import org.wordpress.android.fluxc.action.StockMediaAction.FETCH_STOCK_MEDIA
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.StockMediaModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.stockmedia.StockMediaRestClient
import org.wordpress.android.fluxc.persistence.StockMediaSqlUtils
import org.wordpress.android.fluxc.store.MediaStore.OnStockMediaUploaded
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MEDIA
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockMediaStore
@Inject constructor(
    dispatcher: Dispatcher?,
    private val restClient: StockMediaRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val sqlUtils: StockMediaSqlUtils,
    private val mediaStore: MediaStore
) : Store(dispatcher) {
    /**
     * Actions: FETCH_MEDIA_LIST
     */
    data class FetchStockMediaListPayload(val searchTerm: String, val page: Int) : Payload<BaseNetworkError?>()

    /**
     * Actions: FETCHED_MEDIA_LIST
     */
    class FetchedStockMediaListPayload(
        @JvmField val mediaList: List<StockMediaModel>,
        @JvmField val searchTerm: String,
        @JvmField val nextPage: Int,
        @JvmField val canLoadMore: Boolean
    ) : Payload<StockMediaError?>() {
        constructor(error: StockMediaError, searchTerm: String) : this(listOf(), searchTerm, 0, false) {
            this.error = error
        }
    }

    class OnStockMediaListFetched(
        @JvmField val mediaList: List<StockMediaModel>,
        @JvmField val searchTerm: String,
        @JvmField val nextPage: Int,
        @JvmField val canLoadMore: Boolean
    ) : OnChanged<StockMediaError?>() {
        constructor(error: StockMediaError, searchTerm: String) : this(listOf(), searchTerm, 0, false) {
            this.error = error
        }
    }

    enum class StockMediaErrorType {
        GENERIC_ERROR;

        companion object {
            // endpoint returns an empty media list for any type of error, including timeouts, server error, etc.
            fun fromBaseNetworkError() = GENERIC_ERROR
        }
    }

    data class StockMediaError(val type: StockMediaErrorType, val message: String) : OnChangedError

    @Subscribe(threadMode = ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? StockMediaAction ?: return
        when (actionType) {
            FETCH_STOCK_MEDIA -> performFetchStockMediaList(action.payload as FetchStockMediaListPayload)
        }
    }

    override fun onRegister() {
        AppLog.d(MEDIA, "StockMediaStore onRegister")
    }

    private fun performFetchStockMediaList(payload: FetchStockMediaListPayload) {
        coroutineEngine.launch(MEDIA, this, "Fetching stock media") {
            val mediaListPayload = restClient.searchStockMedia(
                    payload.searchTerm,
                    payload.page,
                    PAGE_SIZE
            )
            handleStockMediaListFetched(mediaListPayload)
        }
    }

    suspend fun fetchStockMedia(filter: String, loadMore: Boolean): OnStockMediaListFetched {
        return coroutineEngine.withDefaultContext(MEDIA, this, "Fetching stock media") {
            val loadedPage = if (loadMore) {
                sqlUtils.getNextPage() ?: 0
            } else {
                0
            }
            if (loadedPage == 0) {
                sqlUtils.clear()
            }

            val payload = restClient.searchStockMedia(filter, loadedPage, PAGE_SIZE)
            if (payload.isError) {
                OnStockMediaListFetched(requireNotNull(payload.error), filter)
            } else {
                sqlUtils.insert(
                        loadedPage,
                        if (payload.canLoadMore) payload.nextPage else null,
                        payload.mediaList.map {
                            StockMediaItem(it.id, it.name, it.title, it.url, it.date, it.thumbnail)
                        })
                OnStockMediaListFetched(payload.mediaList, filter, payload.nextPage, payload.canLoadMore)
            }
        }
    }

    suspend fun getStockMedia(): List<StockMediaItem> {
        return coroutineEngine.withDefaultContext(MEDIA, this, "Getting stock media") {
            sqlUtils.selectAll()
        }
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

    suspend fun performUploadStockMedia(site: SiteModel, stockMedia: List<StockMediaUploadItem>): OnStockMediaUploaded {
        return coroutineEngine.withDefaultContext(MEDIA, this, "Upload stock media") {
            val payload = restClient.uploadStockMedia(site, stockMedia)
            if (payload.isError) {
                OnStockMediaUploaded(payload.site, payload.error!!)
            } else {
                // add uploaded media to the store
                for (media in payload.mediaList) {
                    mediaStore.updateMedia(media, false)
                }
                OnStockMediaUploaded(payload.site, payload.mediaList)
            }
        }
    }

    companion object {
        // this should be a multiple of both 3 and 4 since WPAndroid shows either 3 or 4 pics per row
        const val PAGE_SIZE = 36
    }
}
