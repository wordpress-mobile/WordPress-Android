package org.wordpress.android.viewmodel.giphy

import android.content.Context
import android.net.Uri
import org.wordpress.android.util.WPMediaUtils
import javax.inject.Inject

class MediaFetcher @Inject constructor(private val context: Context) {
    fun fetch(uri: Uri) {
        WPMediaUtils.fetchMediaAndDoNext(context, uri) { downloadedUri ->

        }
    }
}
