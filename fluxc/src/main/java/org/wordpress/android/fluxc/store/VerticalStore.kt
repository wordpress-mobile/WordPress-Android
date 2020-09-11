package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.VerticalAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.vertical.VerticalSegmentModel
import org.wordpress.android.fluxc.network.rest.wpcom.vertical.VerticalRestClient
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VerticalStore @Inject constructor(
    private val verticalRestClient: VerticalRestClient,
    private val coroutineEngine: CoroutineEngine,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? VerticalAction ?: return

        when (actionType) {
            VerticalAction.FETCH_SEGMENTS -> {
                coroutineEngine.launch(AppLog.T.API, this, "FETCH_SEGMENTS") {
                    emitChange(fetchSegments())
                }
            }
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, VerticalStore::class.java.simpleName + " onRegister")
    }

    private suspend fun fetchSegments(): OnSegmentsFetched {
        val fetchedSegmentsPayload = verticalRestClient.fetchSegments()
        return OnSegmentsFetched(fetchedSegmentsPayload.segmentList, fetchedSegmentsPayload.error)
    }

    class OnSegmentsFetched(
        val segmentList: List<VerticalSegmentModel>,
        error: FetchSegmentsError? = null
    ) : Store.OnChanged<FetchSegmentsError>() {
        init {
            this.error = error
        }
    }

    class FetchSegmentPromptPayload(val segmentId: Long)

    class FetchedSegmentsPayload(val segmentList: List<VerticalSegmentModel>) : Payload<FetchSegmentsError>() {
        constructor(error: FetchSegmentsError) : this(emptyList()) {
            this.error = error
        }
    }

    class FetchSegmentsError(val type: VerticalErrorType, val message: String? = null) : Store.OnChangedError
    enum class VerticalErrorType {
        GENERIC_ERROR
    }
}
