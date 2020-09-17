package org.wordpress.android.ui.media

import android.net.Uri
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import dagger.Reusable
import org.wordpress.android.ui.utils.AuthenticationUtils
import javax.inject.Inject

@Reusable
class ExoPlayerUtils
@Inject constructor(
    private val authenticationUtils: AuthenticationUtils
) {
    private var httpDataSourceFactory: DefaultHttpDataSourceFactory? = null

    fun getHttpDataSourceFactory(uri: Uri): DefaultHttpDataSourceFactory {
        if (httpDataSourceFactory == null) {
            httpDataSourceFactory = DefaultHttpDataSourceFactory()
        }
        httpDataSourceFactory?.defaultRequestProperties?.set(authenticationUtils.getAuthHeaders(uri.toString()))
        return httpDataSourceFactory as DefaultHttpDataSourceFactory
    }
}
