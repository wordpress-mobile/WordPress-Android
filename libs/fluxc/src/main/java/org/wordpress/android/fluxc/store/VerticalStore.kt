package org.wordpress.android.fluxc.store

import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.VerticalAction
import org.wordpress.android.fluxc.action.VerticalAction.FETCH_SEGMENT_PROMPT
import org.wordpress.android.fluxc.action.VerticalAction.FETCH_VERTICALS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.vertical.SegmentPromptModel
import org.wordpress.android.fluxc.model.vertical.VerticalModel
import org.wordpress.android.fluxc.model.vertical.VerticalSegmentModel
import org.wordpress.android.fluxc.network.rest.wpcom.vertical.VerticalRestClient
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.experimental.CoroutineContext

private const val DEFAULT_FETCH_VERTICAL_LIMIT = 5

@Singleton
class VerticalStore @Inject constructor(
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
                FETCH_SEGMENT_PROMPT -> fetchSegmentPrompt(action.payload as FetchSegmentPromptPayload)
            }
            emitChange(onChanged)
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, ListStore::class.java.simpleName + " onRegister")
    }

    private suspend fun fetchSegments(): OnSegmentsFetched {
        val fetchedSegmentsPayload = verticalRestClient.fetchSegments()
        return OnSegmentsFetched(fetchedSegmentsPayload.segmentList, fetchedSegmentsPayload.error)
    }

    private suspend fun fetchSegmentPrompt(payload: FetchSegmentPromptPayload): OnSegmentPromptFetched {
        val fetchedSegmentPromptPayload = verticalRestClient.fetchSegmentPrompt(payload.segmentId)
        return OnSegmentPromptFetched(
                segmentId = payload.segmentId,
                prompt = fetchedSegmentPromptPayload.prompt,
                error = fetchedSegmentPromptPayload.error
        )
    }

    private suspend fun fetchVerticals(payload: FetchVerticalsPayload): OnVerticalsFetched {
        val fetchedVerticalsPayload = verticalRestClient.fetchVerticals(payload.searchQuery, payload.limit)
        return OnVerticalsFetched(
                searchQuery = payload.searchQuery,
                verticalList = fetchedVerticalsPayload.verticalList,
                error = fetchedVerticalsPayload.error
        )
    }

    class OnSegmentsFetched(
        val segmentList: List<VerticalSegmentModel>,
        error: FetchSegmentsError? = null
    ) : Store.OnChanged<FetchSegmentsError>() {
        init {
            this.error = error
        }
    }

    class OnVerticalsFetched(
        val searchQuery: String,
        val verticalList: List<VerticalModel>,
        error: FetchVerticalsError?
    ) : Store.OnChanged<FetchVerticalsError>() {
        init {
            this.error = error
        }
    }

    class OnSegmentPromptFetched(
        val segmentId: Long,
        val prompt: SegmentPromptModel?,
        error: FetchSegmentPromptError?
    ) : Store.OnChanged<FetchSegmentPromptError>() {
        init {
            this.error = error
        }
    }

    class FetchVerticalsPayload(val searchQuery: String, val limit: Int = DEFAULT_FETCH_VERTICAL_LIMIT)
    class FetchSegmentPromptPayload(val segmentId: Long)

    class FetchedSegmentsPayload(val segmentList: List<VerticalSegmentModel>) : Payload<FetchSegmentsError>()
    class FetchedSegmentPromptPayload(val prompt: SegmentPromptModel?) : Payload<FetchSegmentPromptError>()
    class FetchedVerticalsPayload(val verticalList: List<VerticalModel>) : Payload<FetchVerticalsError>()

    class FetchSegmentsError(val type: VerticalErrorType, val message: String? = null) : Store.OnChangedError
    class FetchVerticalsError(val type: VerticalErrorType, val message: String? = null) : Store.OnChangedError
    class FetchSegmentPromptError(val type: VerticalErrorType, val message: String? = null) : Store.OnChangedError
    enum class VerticalErrorType {
        GENERIC_ERROR
    }
}
