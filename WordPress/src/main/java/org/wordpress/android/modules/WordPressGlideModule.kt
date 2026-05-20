package org.wordpress.android.modules

import android.content.Context
import com.android.volley.RequestQueue
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import okhttp3.OkHttpClient
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.module.OkHttpClientQualifiers
import org.wordpress.android.networking.GlideAuthInterceptor
import org.wordpress.android.networking.GlideMShotsLoader
import org.wordpress.android.networking.MShot
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Named

// Backs Glide's GlideUrl→InputStream loader with OkHttp so responses stream into the downsampler
// instead of being buffered into a byte[] by Volley — this is what prevents OOM on huge images.
// MShot stays on Volley because GlideMShotsLoader implements a manual retry-on-307 that the
// OkHttp loader doesn't replicate.
@GlideModule
class WordPressGlideModule : AppGlideModule() {
    @Inject
    @Named(OkHttpClientQualifiers.CUSTOM_SSL_CUSTOM_REDIRECTS)
    lateinit var baseOkHttpClient: OkHttpClient

    @Inject
    @Named(OkHttpClientQualifiers.NO_REDIRECTS)
    lateinit var noRedirectsRequestQueue: RequestQueue

    @Inject
    lateinit var glideAuthInterceptor: GlideAuthInterceptor

    override fun applyOptions(context: Context, builder: GlideBuilder) {}

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        (context as WordPress).component().inject(this)
        val glideOkHttpClient = baseOkHttpClient.newBuilder()
            .addInterceptor(glideAuthInterceptor)
            .build()
        registry.replace(
            GlideUrl::class.java, InputStream::class.java,
            OkHttpUrlLoader.Factory(glideOkHttpClient)
        )
        registry.prepend(MShot::class.java, InputStream::class.java, GlideMShotsLoader.Factory(noRedirectsRequestQueue))
    }
}
