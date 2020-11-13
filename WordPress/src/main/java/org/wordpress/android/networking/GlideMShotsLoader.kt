package org.wordpress.android.networking

import android.content.Context
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.Volley
import com.bumptech.glide.integration.volley.VolleyUrlLoader
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import java.io.InputStream
import java.net.URL

data class MShot(val url: String)

class MShotModelLoader(context: Context, glideRequestFactory: GlideRequestFactory) : ModelLoader<MShot, InputStream> {
    var loader: VolleyUrlLoader = VolleyUrlLoader(noRedirectQueue(context), glideRequestFactory)

    override fun handles(item: MShot) = true

    override fun buildLoadData(model: MShot, width: Int, height: Int, options: Options): LoadData<InputStream>? {
        return loader.buildLoadData(GlideUrl(model.url), width, height, options)
    }

    private fun noRedirectQueue(context: Context) = Volley.newRequestQueue(context, object : HurlStack() {
        override fun createConnection(url: URL?) = super.createConnection(url).apply {
            instanceFollowRedirects = false
        }
    })
}

class MShotModelLoaderFactory(private val context: Context, private val requestFactory: GlideRequestFactory) :
        ModelLoaderFactory<MShot, InputStream> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<MShot, InputStream> =
            MShotModelLoader(context, requestFactory)

    override fun teardown() {}
}
