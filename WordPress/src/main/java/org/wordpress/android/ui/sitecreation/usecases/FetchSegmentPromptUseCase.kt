package org.wordpress.android.ui.sitecreation.usecases

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.VerticalActionBuilder
import org.wordpress.android.fluxc.store.VerticalStore
import org.wordpress.android.fluxc.store.VerticalStore.FetchSegmentPromptPayload
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentPromptFetched
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Transforms newFetchSegmentPromptAction EventBus event to a coroutine.
 */
class FetchSegmentPromptUseCase @Inject constructor(
    val dispatcher: Dispatcher,
    @Suppress("unused") val verticalStore: VerticalStore
) {
    private var continuation: Continuation<OnSegmentPromptFetched>? = null

    suspend fun fetchSegmentsPrompt(segmentId: Long): OnSegmentPromptFetched {
        if (continuation != null) {
            throw IllegalStateException("Fetch already in progress.")
        }
        return suspendCoroutine { cont ->
            continuation = cont
            dispatcher.dispatch(
                    VerticalActionBuilder.newFetchSegmentPromptAction(
                            FetchSegmentPromptPayload(segmentId)
                    )
            )
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onSegmentPromptFetched(event: OnSegmentPromptFetched) {
        continuation?.resume(event)
        continuation = null
    }
}
