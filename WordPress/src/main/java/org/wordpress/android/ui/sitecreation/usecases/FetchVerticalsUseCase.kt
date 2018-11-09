package org.wordpress.android.ui.sitecreation.usecases

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.VerticalActionBuilder
import org.wordpress.android.fluxc.store.VerticalStore
import org.wordpress.android.fluxc.store.VerticalStore.FetchVerticalsPayload
import org.wordpress.android.fluxc.store.VerticalStore.OnVerticalsFetched
import javax.inject.Inject
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Transforms EventBus event to a coroutines.
 */
class FetchVerticalsUseCase @Inject constructor(
    val dispatcher: Dispatcher,
    @Suppress("unused") val verticalStore: VerticalStore
) {
    private var continuation: Continuation<OnVerticalsFetched>? = null

    suspend fun fetchVerticals(query: String): OnVerticalsFetched {
        if (continuation != null) {
            throw IllegalStateException("Fetch already in progress.")
        }
        return suspendCoroutine { cont ->
            continuation = cont
            dispatcher.dispatch(VerticalActionBuilder.newFetchVerticalsAction(FetchVerticalsPayload(query)))
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onVerticalsFetched(event: OnVerticalsFetched) {
        checkNotNull(continuation) { "onVerticalsFetched received without a suspended coroutine." }
                .resume(event)
        continuation = null
    }
}
