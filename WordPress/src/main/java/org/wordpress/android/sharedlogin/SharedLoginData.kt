package org.wordpress.android.sharedlogin

import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel

data class SharedLoginData(
    val token: String?,
    val accounts: List<AccountModel>?,
    val sites: List<SiteModel>?
)
