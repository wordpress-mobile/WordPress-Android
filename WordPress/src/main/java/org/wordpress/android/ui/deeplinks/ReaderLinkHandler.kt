package org.wordpress.android.ui.deeplinks

import android.content.Intent
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenInReader
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenReader
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.utils.IntentUtils
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class ReaderLinkHandler
@Inject constructor(private val intentUtils: IntentUtils) {
    /**
     * URIs supported by the Reader are already defined as intent filters in the manifest. Instead of replicating
     * that logic here, we simply check if we can resolve an [Intent] that uses [ReaderConstants.ACTION_VIEW_POST].
     * Since that's a custom action that is only handled by the Reader, we can then assume it supports this URI.
     * In addition we also handle the Reader app links `wordpress://read` here.
     */
    fun isReaderUrl(uri: UriWrapper) =
            DEEP_LINK_HOST_READ == uri.host || intentUtils.canResolveWith(ReaderConstants.ACTION_VIEW_POST, uri)

    fun buildOpenInReaderNavigateAction(uri: UriWrapper): NavigateAction {
        return when (uri.host) {
            DEEP_LINK_HOST_READ -> OpenReader
            else -> OpenInReader(uri)
        }
    }

    companion object {
        private const val DEEP_LINK_HOST_READ = "read"
    }
}
