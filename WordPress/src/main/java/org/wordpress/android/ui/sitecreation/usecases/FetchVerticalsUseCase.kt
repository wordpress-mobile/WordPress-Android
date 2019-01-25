package org.wordpress.android.ui.sitecreation.usecases

import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.VerticalActionBuilder
import org.wordpress.android.fluxc.store.VerticalStore
import org.wordpress.android.fluxc.store.VerticalStore.FetchVerticalsPayload
import org.wordpress.android.fluxc.store.VerticalStore.OnVerticalsFetched
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

/**
 * Transforms OnVerticalsFetched EventBus event to a coroutine.
 *
 * The client may dispatch multiple requests, but we want to accept only the latest one and ignore all others.
 * We can't rely just on job.cancel() as the FetchVerticalsPayload may have already been dispatched and FluxC will
 * return a result.
 */
class FetchVerticalsUseCase @Inject constructor(
    val dispatcher: Dispatcher,
    @Suppress("unused") val verticalStore: VerticalStore
) {
    /**
     * Query - Continuation pair
     */
    private var pair: Pair<String, Continuation<OnVerticalsFetched>>? = null

    suspend fun fetchVerticals(query: String): OnVerticalsFetched {
        return suspendCancellableCoroutine { cont ->
            pair = Pair(query, cont)
            dispatcher.dispatch(VerticalActionBuilder.newFetchVerticalsAction(FetchVerticalsPayload(query)))
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onVerticalsFetched(event: OnVerticalsFetched) {
        pair?.let {
            if (event.searchQuery == it.first) {
                it.second.resume(event)
                pair = null
            }
        }
    }
}
