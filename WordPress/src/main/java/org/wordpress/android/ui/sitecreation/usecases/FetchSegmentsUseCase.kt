package org.wordpress.android.ui.sitecreation.usecases

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.BuildConfig
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.VerticalActionBuilder
import org.wordpress.android.fluxc.store.VerticalStore
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentsFetched
import javax.inject.Inject
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Transforms EventBus event to a coroutines.
 */
class FetchSegmentsUseCase @Inject constructor(
    val dispatcher: Dispatcher,
    @Suppress("unused") val verticalStore: VerticalStore
) {
    private var continuation: Continuation<OnSegmentsFetched>? = null

    suspend fun fetchCategories(): OnSegmentsFetched {
        if (continuation != null) {
            throw IllegalStateException("Fetch already in progress.")
        }
        return suspendCoroutine { cont ->
            continuation = cont
            dispatcher.dispatch(VerticalActionBuilder.newFetchSegmentsAction())
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onSiteCategoriesFetched(event: OnSegmentsFetched) {
        if (continuation != null) {
            continuation?.resume(event)
            continuation = null
        } else if (BuildConfig.DEBUG) {
            throw java.lang.IllegalStateException("onSiteCategoriesFetched received without a suspended coroutine.")
        }
    }
}
