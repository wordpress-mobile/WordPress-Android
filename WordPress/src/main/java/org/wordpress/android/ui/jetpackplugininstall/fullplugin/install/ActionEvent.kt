package org.wordpress.android.ui.jetpackplugininstall.fullplugin.install

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.accounts.HelpActivity

sealed class ActionEvent {
    data class ContactSupport(
        val origin: HelpActivity.Origin,
        val selectedSite: SiteModel?,
    ) : ActionEvent()

    object Dismiss : ActionEvent()
}
