package org.wordpress.android.ui.sitecreation.previews

import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.CreateSiteState

interface SitePreviewScreenListener {
    fun onSitePreviewScreenDismissed(createSiteState: CreateSiteState)
    fun onSiteCreationCompleted()
}
