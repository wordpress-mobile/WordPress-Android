package org.wordpress.android.networking

import com.android.volley.DefaultRetryPolicy
import com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
import com.android.volley.DefaultRetryPolicy.DEFAULT_TIMEOUT_MS
import com.android.volley.RequestQueue
import com.bumptech.glide.integration.volley.VolleyRequestFactory
import com.bumptech.glide.integration.volley.VolleyStreamFetcher
import com.bumptech.glide.integration.volley.VolleyUrlLoader
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import java.io.InputStream

private const val MAX_RETRIES = 10

/**
 * String URL wrapper used to customise the request via [GlideMShotsLoader]
 */
class MShot(val url: String)

/**
 * Implements a custom Glide [ModelLoader] that retries loading on http redirection (307)
 *
 * This is needed because the mshot backend service redirects to a loading gif image when the thumbnail is not ready.
 * This occurs when the thumbnail has not been accessed recently for the specific language and viewport size. When this
 * thumbnail is requested it is cached on the server and is readily available according to the server cache policy.
 */
class GlideMShotsLoader(noRedirectsRequestQueue: RequestQueue) : ModelLoader<MShot, InputStream> {
    private val requestFactory = VolleyRequestFactory { url, callback, priority, headers ->
        VolleyStreamFetcher.GlideRequest(url, callback, priority, headers).apply {
            retryPolicy = DefaultRetryPolicy(DEFAULT_TIMEOUT_MS, MAX_RETRIES, DEFAULT_BACKOFF_MULT)
        }
    }

    private val loader: VolleyUrlLoader = VolleyUrlLoader(noRedirectsRequestQueue, requestFactory)

    override fun handles(item: MShot) = true

    override fun buildLoadData(model: MShot, width: Int, height: Int, options: Options): LoadData<InputStream>? =
        loader.buildLoadData(GlideUrl(model.url), width, height, options)

    class Factory(private val noRedirectsRequestQueue: RequestQueue) : ModelLoaderFactory<MShot, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<MShot, InputStream> =
            GlideMShotsLoader(noRedirectsRequestQueue)

        override fun teardown() {}
    }
}
