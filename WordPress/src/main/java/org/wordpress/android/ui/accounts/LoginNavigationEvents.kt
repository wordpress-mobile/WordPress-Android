package org.wordpress.android.ui.accounts

const val INSTRUCTIONS_URL = "https://jetpack.com/support/getting-started-with-jetpack/"

sealed class LoginNavigationEvents {
    data class ShowSiteAddressError(val url: String) : LoginNavigationEvents()
    object ShowNoJetpackSites : LoginNavigationEvents()
    object ShowSignInForResultJetpackOnly : LoginNavigationEvents()
    data class ShowInstructions(val url: String = INSTRUCTIONS_URL) : LoginNavigationEvents()
    object ShowPostSignupInterstitialScreen : LoginNavigationEvents()
    data class SelectSite(val localId: Int) : LoginNavigationEvents()
    object CreateNewSite : LoginNavigationEvents()
    object CloseWithResultOk : LoginNavigationEvents()
    object ShowEmailLoginScreen : LoginNavigationEvents()
    object ShowLoginViaSiteAddressScreen : LoginNavigationEvents()
}
