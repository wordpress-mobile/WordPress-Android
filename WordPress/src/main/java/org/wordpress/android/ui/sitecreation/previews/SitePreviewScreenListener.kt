package org.wordpress.android.ui.sitecreation.previews

import org.wordpress.android.ui.sitecreation.misc.CreateSiteState

interface SitePreviewScreenListener {
    fun onPreviewScreenDismissed(state: CreateSiteState)
}
