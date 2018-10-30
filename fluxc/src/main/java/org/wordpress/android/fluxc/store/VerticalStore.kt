package org.wordpress.android.fluxc.store

import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.VerticalAction
import org.wordpress.android.fluxc.action.VerticalAction.FETCH_VERTICALS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.vertical.VerticalModel
import org.wordpress.android.fluxc.model.vertical.VerticalSegmentModel
import org.wordpress.android.fluxc.network.rest.wpcom.vertical.VerticalRestClient
import org.wordpress.android.fluxc.persistence.VerticalSqlUtils
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.experimental.CoroutineContext

@Singleton
class VerticalStore @Inject constructor(
    private val verticalSqlUtils: VerticalSqlUtils,
    private val verticalRestClient: VerticalRestClient,
    private val coroutineContext: CoroutineContext,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? VerticalAction ?: return

        GlobalScope.launch(coroutineContext) {
            val onChanged = when (actionType) {
                VerticalAction.FETCH_SEGMENTS -> fetchSegments()
                FETCH_VERTICALS -> fetchVerticals(action.payload as FetchVerticalsPayload)
            }
            emitChange(onChanged)
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, ListStore::class.java.simpleName + " onRegister")
    }

    // TODO: Do we have pagination and if so, how complicated is it? Do we need all the data at once or can we
    // TODO: use the ListStore to paginate?
    private suspend fun fetchSegments(): OnSegmentsFetched {
        val fetchedSegmentsPayload = verticalRestClient.fetchSegments()
        val onSegmentsFetched = OnSegmentsFetched(fetchedSegmentsPayload.error)
        if (!fetchedSegmentsPayload.isError) {
            // TODO: If there is pagination and everything is kept in this Store, we need to delete the current
            // TODO: verticals before we insert the new list
            verticalSqlUtils.insertSegments(fetchedSegmentsPayload.segmentList)
        }
        return onSegmentsFetched
    }

    // TODO: Do we have pagination and if so, how complicated is it? Do we need all the data at once or can we
    // TODO: use the ListStore to paginate?
    private suspend fun fetchVerticals(payload: FetchVerticalsPayload): OnVerticalsFetched {
        val fetchedVerticalsPayload = verticalRestClient.fetchVerticals(payload.searchQuery)
        val onVerticalsFetched = OnVerticalsFetched(payload.searchQuery, fetchedVerticalsPayload.error)
        if (!fetchedVerticalsPayload.isError) {
            // TODO: If there is pagination and everything is kept in this Store, we need to delete the current
            // TODO: verticals before we insert the new list
            verticalSqlUtils.insertVerticals(fetchedVerticalsPayload.verticalList)
        }
        return onVerticalsFetched
    }

    class OnSegmentsFetched(error: FetchSegmentsError? = null) : Store.OnChanged<FetchSegmentsError>() {
        init {
            this.error = error
        }
    }

    class OnVerticalsFetched(
        val searchQuery: String,
        error: FetchVerticalsError
    ) : Store.OnChanged<FetchVerticalsError>() {
        init {
            this.error = error
        }
    }

    class FetchVerticalsPayload(val searchQuery: String)

    class FetchedSegmentsPayload(val segmentList: List<VerticalSegmentModel>) : Payload<FetchSegmentsError>()
    class FetchedVerticalsPayload(val verticalList: List<VerticalModel>) : Payload<FetchVerticalsError>()

    class FetchSegmentsError(val type: VerticalErrorType, val message: String? = null) : Store.OnChangedError
    class FetchVerticalsError(val type: VerticalErrorType, val message: String? = null) : Store.OnChangedError
    enum class VerticalErrorType {
        GENERIC_ERROR
    }
}
