package org.wordpress.android.ui.accounts

sealed class LoginNavigationEvents {
    data class ShowSiteAddressError(val url: String, val errorMessage: String) : LoginNavigationEvents()
    data class ShowNoJetpackSitesError(val errorMessage: String) : LoginNavigationEvents()
    object ShowSignInForResultJetpackOnly : LoginNavigationEvents()
    data class ShowInstructions(val url: String) : LoginNavigationEvents()
}
