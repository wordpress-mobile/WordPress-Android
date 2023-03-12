package org.wordpress.android.ui.sitecreation.previews

import org.wordpress.android.ui.sitecreation.SiteCreationResult

interface SitePreviewScreenListener {
    fun onPreviewScreenClosed(result: SiteCreationResult)
}
