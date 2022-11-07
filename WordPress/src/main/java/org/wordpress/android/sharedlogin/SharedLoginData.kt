package org.wordpress.android.sharedlogin

import org.wordpress.android.fluxc.model.SiteModel

data class SharedLoginData(
    val token: String?,
    val sites: List<SiteModel>?
)
