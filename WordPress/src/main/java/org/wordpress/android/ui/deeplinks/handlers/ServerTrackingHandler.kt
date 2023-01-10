package org.wordpress.android.ui.deeplinks.handlers

import com.android.volley.Request.Method
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.AppLog.T.API
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

class ServerTrackingHandler
@Inject constructor(
    private val appLogWrapper: AppLogWrapper,
    contextProvider: ContextProvider,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : CoroutineScope {
    private val queue: RequestQueue = Volley.newRequestQueue(contextProvider.getContext())
    private val job = Job()
    fun request(uri: UriWrapper) {
        launch {
            queue.add(
                StringRequest(
                    Method.GET, uri.toString(),
                    { appLogWrapper.d(API, "DeepLink tracking URI successfully requested") },
                    { error -> appLogWrapper.e(API, "DeepLink tracking URI request failed: $error") })
            )
        }
    }

    fun clear() {
        job.cancel()
    }

    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job
}
