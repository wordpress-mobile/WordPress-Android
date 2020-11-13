package org.wordpress.android.networking

import android.content.Context
import com.android.volley.DefaultRetryPolicy
import com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
import com.android.volley.Request
import com.android.volley.Request.Priority
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.Volley
import com.bumptech.glide.integration.volley.VolleyRequestFactory
import com.bumptech.glide.integration.volley.VolleyStreamFetcher
import com.bumptech.glide.integration.volley.VolleyUrlLoader
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher.DataCallback
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import java.io.InputStream
import java.net.URL

private const val retryInterval = 1000
private const val maxRetries = 10

data class MShot(val url: String)

/**
 * Implements a custom Glide [ModelLoader] that prevents following redirects and applies a [DefaultRetryPolicy] setting
 * a specific [retryInterval] and [maxRetries] number.
 *
 * This is needed because the mshot backend service redirects to a loading gif image when the thumbnail is not ready.
 * This occurs when the thumbnail has not been accessed recently for the specific language and viewport size. When this
 * thumbnail is requested it is cached on the server and is readily available according to the server cache policy.
 */
class GlideMShotsLoader(context: Context) : ModelLoader<MShot, InputStream> {
    private val requestFactory = RequestFactory()
    var loader: VolleyUrlLoader = VolleyUrlLoader(noRedirectQueue(context), requestFactory)

    override fun handles(item: MShot) = true

    override fun buildLoadData(model: MShot, width: Int, height: Int, options: Options): LoadData<InputStream>? =
            loader.buildLoadData(GlideUrl(model.url), width, height, options)

    private fun noRedirectQueue(context: Context) = Volley.newRequestQueue(context, object : HurlStack() {
        override fun createConnection(url: URL?) = super.createConnection(url).apply {
            instanceFollowRedirects = false
        }
    })

    class RequestFactory : VolleyRequestFactory {
        override fun create(
            url: String,
            callback: DataCallback<in InputStream>,
            priority: Priority,
            headers: Map<String, String>
        ): Request<ByteArray>? = VolleyStreamFetcher.GlideRequest(url, callback, priority, headers).apply {
            retryPolicy = DefaultRetryPolicy(retryInterval, maxRetries, DEFAULT_BACKOFF_MULT)
        }
    }

    class Factory(private val context: Context) :
            ModelLoaderFactory<MShot, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<MShot, InputStream> =
                GlideMShotsLoader(context)

        override fun teardown() {}
    }
}
