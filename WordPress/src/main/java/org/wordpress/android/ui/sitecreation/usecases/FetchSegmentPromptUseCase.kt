package org.wordpress.android.ui.sitecreation.usecases

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.BuildConfig
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.VerticalActionBuilder
import org.wordpress.android.fluxc.store.VerticalStore
import org.wordpress.android.fluxc.store.VerticalStore.FetchSegmentPromptPayload
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentPromptFetched
import javax.inject.Inject
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

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
        if (continuation != null) {
            continuation?.resume(event)
            continuation = null
        } else if (BuildConfig.DEBUG) {
            throw java.lang.IllegalStateException("onSiteCategoriesFetched received without a suspended coroutine.")
        }
    }
}
