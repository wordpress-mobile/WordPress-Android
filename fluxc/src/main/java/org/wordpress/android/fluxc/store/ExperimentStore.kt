package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExperimentStore @Inject constructor(
    dispatcher: Dispatcher
) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>?) {
        TODO("Not yet implemented")
    }

    override fun onRegister() {
        AppLog.d(API, "${this.javaClass.simpleName}: onRegister")
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
