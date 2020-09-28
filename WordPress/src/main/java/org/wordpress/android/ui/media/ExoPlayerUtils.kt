package org.wordpress.android.ui.media

import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
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

    fun buildMediaSourceFactory(httpDataSourceFactory: DefaultHttpDataSourceFactory): MediaSourceFactory {
        return DefaultMediaSourceFactory(DefaultDataSourceFactory(WordPress.getContext(), httpDataSourceFactory))
    }

    fun buildHttpDataSourceFactory(url: String): DefaultHttpDataSourceFactory {
        if (httpDataSourceFactory == null) {
            httpDataSourceFactory = DefaultHttpDataSourceFactory()
        }
        httpDataSourceFactory?.defaultRequestProperties?.set(authenticationUtils.getAuthHeaders(url))
        return httpDataSourceFactory as DefaultHttpDataSourceFactory
    }
}
