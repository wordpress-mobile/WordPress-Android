package org.wordpress.android.networking

import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import org.wordpress.android.fluxc.network.HTTPAuthManager
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken

import java.io.InputStream

class GlideHeaderLoaderFactory(
    private val accessToken: AccessToken,
    private val httpAuthManager: HTTPAuthManager,
    private val userAgent: UserAgent) : ModelLoaderFactory<GlideUrl, InputStream> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<GlideUrl, InputStream> {
        val loader = multiFactory.build(GlideUrl::class.java, InputStream::class.java)
        return GlideHeaderLoader(loader, accessToken, httpAuthManager, userAgent)
    }

    override fun teardown() {
        // noop
    }
}
