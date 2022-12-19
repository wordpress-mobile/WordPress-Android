package org.wordpress.android.ui.domains.usecases

import org.greenrobot.eventbus.Subscribe
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnPlansFetched
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Wraps an [OnPlansFetched] into a coroutine.
 */
class FetchPlansUseCase @Inject constructor(
    private val dispatcher: Dispatcher,
    @Suppress("unused") private val siteStore: SiteStore // needed for events to work
) {
    private var continuation: Continuation<OnPlansFetched>? = null

    init {
        dispatcher.register(this)
    }

    fun clear() {
        dispatcher.unregister(this)
    }

    @Suppress("UseCheckOrError")
    suspend fun execute(
        site: SiteModel
    ): OnPlansFetched {
        if (continuation != null) {
            throw IllegalStateException("FetchSite is already in progress!")
        }
        return suspendCoroutine {
            continuation = it
            dispatcher.dispatch(SiteActionBuilder.newFetchPlansAction(site))
        }
    }

    @Subscribe
    fun onPlansFetched(event: OnPlansFetched) {
        continuation?.resume(event)
        continuation = null
    }
}
