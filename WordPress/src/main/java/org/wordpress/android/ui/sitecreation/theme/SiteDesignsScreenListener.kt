package org.wordpress.android.ui.sitecreation.theme

private const val defaultTemplate = "default"

interface SiteDesignsScreenListener {
    fun onSiteDesignSelected(siteDesign: String?)
    fun onDesignSelectionSkipped() = onSiteDesignSelected(defaultTemplate)
}
