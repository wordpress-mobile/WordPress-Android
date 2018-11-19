package org.wordpress.android.ui.sitecreation.usecases

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.Store
import org.wordpress.android.fluxc.store.VerticalStore
import org.wordpress.android.fluxc.store.VerticalStore.VerticalErrorType
import javax.inject.Inject
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Transforms EventBus event to a coroutines.
 */
class FetchVerticalsHeaderInfoUseCase @Inject constructor(
    val dispatcher: Dispatcher,
    @Suppress("unused") val verticalStore: VerticalStore
) {
    private var continuation: Continuation<DummyOnVerticalsHeaderInfoFetched>? = null

    suspend fun fetchVerticalHeaderInfo(): DummyOnVerticalsHeaderInfoFetched {
        if (continuation != null) {
            throw IllegalStateException("Fetch already in progress.")
        }
        return suspendCoroutine { cont ->
            continuation = cont
            // TODO dispatcher.dispatch()
            onVerticalHeaderInfoFetched(
                    DummyOnVerticalsHeaderInfoFetched(
                            DummyVerticalsHeaderInfoModel(
                                    "What’s the focus of your business?",
                                    "We’ll use your answer to add sections to your website.",
                                    "eg. Landscaping, Consulting… etc"
                            ),
                            null
                    )
            )
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onVerticalHeaderInfoFetched(event: DummyOnVerticalsHeaderInfoFetched) {
        checkNotNull(continuation) { "onVerticalHeaderInfoFetched received without a suspended coroutine." }
                .resume(event)
        continuation = null
    }
}

class DummyOnVerticalsHeaderInfoFetched(
    val headerInfo: DummyVerticalsHeaderInfoModel?,
    error: DummyFetchVerticalHeaderInfoError?
) : Store.OnChanged<DummyFetchVerticalHeaderInfoError>() {
    init {
        this.error = error
    }
}

class DummyFetchVerticalHeaderInfoError(val type: VerticalErrorType, val message: String? = null) : Store.OnChangedError

data class DummyVerticalsHeaderInfoModel(val title: String, val subtitle: String, val inputHint: String)
