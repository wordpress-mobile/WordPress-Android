package org.wordpress.android.ui.media

import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.C.ContentType
import com.google.android.exoplayer2.offline.FilteringManifestParser
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource.Factory
import com.google.android.exoplayer2.source.dash.manifest.DashManifestParser
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.hls.playlist.DefaultHlsPlaylistParserFactory
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.manifest.SsManifestParser
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import dagger.Reusable
import org.wordpress.android.WordPress
import org.wordpress.android.ui.utils.AuthenticationUtils
import javax.inject.Inject

@Reusable
class ExoPlayerUtils
@Inject constructor(
    private val authenticationUtils: AuthenticationUtils
) {
    private var httpDataSourceFactory: DefaultHttpDataSourceFactory? = null

    fun buildHttpDataSourceFactory(url: String): DefaultHttpDataSourceFactory {
        if (httpDataSourceFactory == null) {
            httpDataSourceFactory = DefaultHttpDataSourceFactory(
                    Util.getUserAgent(WordPress.getContext(), USER_AGENT)
            )
        }
        httpDataSourceFactory?.defaultRequestProperties?.set(authenticationUtils.getAuthHeaders(url))
        return httpDataSourceFactory as DefaultHttpDataSourceFactory
    }

    fun buildMediaSource(uri: Uri): MediaSource? {
        val httpDataSourceFactory = buildHttpDataSourceFactory(uri.toString())
        return when (@ContentType val type = Util.inferContentType(uri)) {
            C.TYPE_DASH -> Factory(httpDataSourceFactory)
                    .setManifestParser(FilteringManifestParser(DashManifestParser(), null))
                    .createMediaSource(uri)
            C.TYPE_SS -> SsMediaSource.Factory(httpDataSourceFactory)
                    .setManifestParser(FilteringManifestParser(SsManifestParser(), null))
                    .createMediaSource(uri)
            C.TYPE_HLS -> HlsMediaSource.Factory(httpDataSourceFactory)
                    .setPlaylistParserFactory(DefaultHlsPlaylistParserFactory())
                    .createMediaSource(uri)
            C.TYPE_OTHER -> ExtractorMediaSource.Factory(httpDataSourceFactory).createMediaSource(uri)
            else -> {
                throw IllegalStateException("$UNSUPPORTED_TYPE $type")
            }
        }
    }

    companion object {
        private const val USER_AGENT = "WordPress ExoPlayer Android"
        private const val UNSUPPORTED_TYPE = "Unsupported type"
    }
}
