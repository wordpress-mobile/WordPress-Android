package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.ExperimentAction
import org.wordpress.android.fluxc.action.ExperimentAction.FETCH_ASSIGNMENTS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.experiments.Assignments
import org.wordpress.android.fluxc.model.experiments.AssignmentsModel
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
            OnAssignmentsFetched(assignments = Assignments.fromModel(fetchedPayload.assignments))
        } else {
            OnAssignmentsFetched(error = fetchedPayload.error)
        }
    }

    data class FetchAssignmentsPayload(
        val platform: Platform,
        val anonId: String? = null
    )

    data class FetchedAssignmentsPayload(
        val assignments: AssignmentsModel
    ) : Payload<FetchAssignmentsError>() {
        constructor(error: FetchAssignmentsError) : this(AssignmentsModel()) {
            this.error = error
        }
    }

    data class OnAssignmentsFetched(
        val assignments: Assignments
    ) : OnChanged<FetchAssignmentsError>() {
        constructor(error: FetchAssignmentsError) : this(Assignments()) {
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

    enum class Platform(val value: String) {
        WORDPRESS_COM("wpcom"),
        CALYPSO("calypso"),
        JETPACK("jetpack"),
        WOOCOMMERCE("woocommerce"),
        WORDPRESS_IOS("wpios"),
        WORDPRESS_ANDROID("wpandroid"),
        WOOCOMMERCE_IOS("woocommerceios"),
        WOOCOMMERCE_ANDROID("woocommerceandroid");

        companion object {
            fun fromValue(value: String): Platform? {
                return values().firstOrNull { it.value == value }
            }
        }
    }
}
