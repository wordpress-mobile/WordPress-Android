package org.wordpress.android.ui.sitecreation.creation

import org.wordpress.android.ui.sitecreation.NewSitePreviewViewModel.CreateSiteState

interface SitePreviewScreenListener {
    fun onSitePreviewScreenDismissed(createSiteState: CreateSiteState)
}
