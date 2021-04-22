package org.wordpress.android.ui

import android.content.Intent
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenInReader
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
     */
    fun isReaderUrl(uri: UriWrapper) = intentUtils.canResolveWith(ReaderConstants.ACTION_VIEW_POST, uri)

    fun buildOpenInReaderNavigateAction(uri: UriWrapper) = OpenInReader(uri)
}
