package org.wordpress.android.ui.sitecreation.usecases

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PostAction
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.ui.sitecreation.OnSiteCategoriesFetchedDummy
import javax.inject.Inject
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Transforms EventBus event to a coroutines.
 */
class FetchCategoriesUseCase @Inject constructor(val dispatcher: Dispatcher) {
    private var continuation: Continuation<OnSiteCategoriesFetchedDummy>? = null

    suspend fun fetchCategories(): OnSiteCategoriesFetchedDummy {
        if (continuation != null) {
            throw IllegalStateException("Fetch already in progress.")
        }
        return suspendCoroutine { cont ->
            continuation = cont
            dispatchEvent()
        }
    }

    private fun dispatchEvent() {
        // TODO create payload and dispatch fetchSiteCategories event event
        dispatcher.dispatch(SiteActionBuilder.generateNoPayloadAction(PostAction.FETCH_POST))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onSiteCategoriesFetched(event: OnSiteCategoriesFetchedDummy) {
        checkNotNull(continuation) { "onSiteCategoriesFetched received without a suspended coroutine." }
        continuation!!.resume(event)
        continuation = null
    }
}
