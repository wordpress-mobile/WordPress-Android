package org.wordpress.android.fluxc.store

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.ActivityLogAction
import org.wordpress.android.fluxc.action.PlansAction
import org.wordpress.android.fluxc.action.PlansAction.FETCH_PLANS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.plans.PlanModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.plans.PlansRestClient
import org.wordpress.android.fluxc.persistence.PlansSqlUtils
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindStatusError
import org.wordpress.android.fluxc.store.PlansStore.PlansErrorType.GENERIC_ERROR
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class PlansStore @Inject constructor(
    private val plansRestClient: PlansRestClient,
    private val plansSqlUtils: PlansSqlUtils,
    private val coroutineContext: CoroutineContext,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? PlansAction ?: return
        when (actionType) {
            FETCH_PLANS -> {
                GlobalScope.launch(coroutineContext) { emitChange(fetchPlans()) }
            }
        }
    }

    private suspend fun fetchPlans(): OnPlansFetched {
        val fetchedPlansPayload = plansRestClient.fetchPlans()

        return if (!fetchedPlansPayload.isError) {
            plansSqlUtils.storePlans(fetchedPlansPayload.plans!!)
            val onPlansFetched = OnPlansFetched(fetchedPlansPayload.plans)
            onPlansFetched
        } else {
            val errorPayload = OnPlansFetched(
                    getCachedPlans(),
                    PlansFetchError(GENERIC_ERROR, fetchedPlansPayload.error.message)
            )
            errorPayload
        }
    }

    fun getCachedPlans(): List<PlanModel> {
        return plansSqlUtils.getPlans()
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, PlansStore::class.java.simpleName + " onRegister")
    }

    class PlansFetchedPayload(val plans: List<PlanModel>? = null) : Payload<BaseRequest.BaseNetworkError>()

    data class OnPlansFetched(
        val plans: List<PlanModel>? = null,
        val plansError: PlansFetchError? = null
    ) : Store.OnChanged<PlansFetchError>() {
        init {
            // we allow setting error from constructor, so it will be a part of data class
            // and used when comparing this class, so we can test error/no error events
            this.error = plansError
        }
    }

    data class PlansFetchError(
        val type: PlansErrorType,
        val message: String = ""
    ) : OnChangedError

    enum class PlansErrorType {
        GENERIC_ERROR
    }
}
