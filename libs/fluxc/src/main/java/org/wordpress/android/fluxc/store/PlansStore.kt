package org.wordpress.android.fluxc.store

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.PlansAction
import org.wordpress.android.fluxc.action.PlansAction.FETCH_PLANS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.plans.PlanModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.plans.PlansRestClient
import org.wordpress.android.fluxc.persistence.PlansSqlUtils
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
                GlobalScope.launch(coroutineContext) { fetchPlans() }
            }
        }
    }

    private suspend fun fetchPlans() = withContext(coroutineContext) {
        val fetchedPlansPayload = plansRestClient.fetchPlans()


        return@withContext if (!fetchedPlansPayload.isError) {
            plansSqlUtils.storePlans(fetchedPlansPayload.plans!!)
            val onPlansFetched = OnPlansFetched(fetchedPlansPayload.plans)
            emitChange(onPlansFetched)
            onPlansFetched
        } else {
            val errorPayload = OnPlansFetched()
            errorPayload.error = PlansFetchError(GENERIC_ERROR, fetchedPlansPayload.error.message)
            emitChange(errorPayload)
            errorPayload
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, PlansStore::class.java.simpleName + " onRegister")
    }

    class PlansFetchedPayload(val plans: List<PlanModel>? = null) : Payload<BaseRequest.BaseNetworkError>()

    class OnPlansFetched internal constructor(val plans: List<PlanModel>? = null
    ) : Store.OnChanged<PlansFetchError>()

    class PlansFetchError(
        val type: PlansErrorType,
        val message: String = ""
    ) : OnChangedError

    enum class PlansErrorType {
        GENERIC_ERROR
    }
}
