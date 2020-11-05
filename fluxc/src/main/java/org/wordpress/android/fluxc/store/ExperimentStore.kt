package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.ExperimentAction
import org.wordpress.android.fluxc.action.ExperimentAction.FETCH_ASSIGNMENTS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.network.rest.wpcom.experiments.ExperimentRestClient
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExperimentStore @Inject constructor(
    private val experimentRestClient: ExperimentRestClient,
    private val coroutineEngine: CoroutineEngine,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? ExperimentAction ?: return
        when (actionType) {
            FETCH_ASSIGNMENTS -> coroutineEngine.launch(API, this, "FETCH_ASSIGNMENTS") {
                emitChange(fetchAssignments(action.payload as FetchAssignmentsPayload))
            }
        }
    }

    override fun onRegister() {
        AppLog.d(API, "${this.javaClass.simpleName}: onRegister")
    }

    private suspend fun fetchAssignments(fetchPayload: FetchAssignmentsPayload): OnAssignmentsFetched {
        val fetchedPayload = experimentRestClient.fetchAssignments(fetchPayload.platform, fetchPayload.anonId)
        return if (!fetchedPayload.isError) {
            // TODO: Persist locally
            OnAssignmentsFetched(variations = fetchedPayload.variations)
        } else {
            OnAssignmentsFetched(error = fetchedPayload.error)
        }
    }

    data class FetchAssignmentsPayload(
        val platform: String,
        val anonId: String? = null
    )

    data class FetchedAssignmentsPayload(
        val variations: Map<String, String?>,
        val ttl: Int
    ) : Payload<FetchAssignmentsError>() {
        constructor(error: FetchAssignmentsError) : this(emptyMap(), 0) {
            this.error = error
        }
    }

    data class OnAssignmentsFetched(
        val variations: Map<String, String?>
    ) : OnChanged<FetchAssignmentsError>() {
        constructor(error: FetchAssignmentsError) : this(emptyMap()) {
            this.error = error
        }
    }

    data class FetchAssignmentsError(
        val type: ExperimentErrorType,
        val message: String? = null
    ) : OnChangedError

    enum class ExperimentErrorType {
        GENERIC_ERROR
    }
}
