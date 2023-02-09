@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.media

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.C.ContentType
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.offline.FilteringManifestParser
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.manifest.DashManifestParser
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.hls.playlist.DefaultHlsPlaylistParserFactory
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.manifest.SsManifestParser
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import dagger.Reusable
import org.wordpress.android.WordPress
import org.wordpress.android.ui.utils.AuthenticationUtils
import javax.inject.Inject

@Reusable
@Suppress("DEPRECATION")
class ExoPlayerUtils @Inject constructor(
    private val authenticationUtils: AuthenticationUtils,
    private val appContext: Context
) {
    private var httpDataSourceFactory: DefaultHttpDataSource.Factory? = null

    private fun buildHttpDataSourceFactory(url: String): DefaultHttpDataSource.Factory {
        if (httpDataSourceFactory == null) {
            httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(WordPress.getUserAgent())
        }
        httpDataSourceFactory?.setDefaultRequestProperties(authenticationUtils.getAuthHeaders(url))
        return httpDataSourceFactory as DefaultHttpDataSource.Factory
    }

    private fun buildDefaultDataSourceFactory(httpDataSourceFactory: DefaultHttpDataSource.Factory) =
        DefaultDataSourceFactory(appContext, httpDataSourceFactory)

    @Suppress("UseCheckOrError")
    fun buildMediaSource(uri: Uri): MediaSource {
        val mediaItem = MediaItem.Builder().setUri(uri).build()
        val httpDataSourceFactory = buildHttpDataSourceFactory(uri.toString())
        val defaultDataSourceFactory = buildDefaultDataSourceFactory(httpDataSourceFactory)
        return when (@ContentType val type = Util.inferContentType(uri)) {
            C.TYPE_DASH -> DashMediaSource.Factory(defaultDataSourceFactory)
                .setManifestParser(FilteringManifestParser(DashManifestParser(), null))
                .createMediaSource(mediaItem)
            C.TYPE_SS -> SsMediaSource.Factory(defaultDataSourceFactory)
                .setManifestParser(FilteringManifestParser(SsManifestParser(), null))
                .createMediaSource(mediaItem)
            C.TYPE_HLS -> HlsMediaSource.Factory(defaultDataSourceFactory)
                .setPlaylistParserFactory(DefaultHlsPlaylistParserFactory())
                .createMediaSource(mediaItem)
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(defaultDataSourceFactory)
                .createMediaSource(mediaItem)
            else -> {
                throw IllegalStateException("$UNSUPPORTED_TYPE $type")
            }
        }
    }

    companion object {
        private const val UNSUPPORTED_TYPE = "Unsupported type"
    }
}
