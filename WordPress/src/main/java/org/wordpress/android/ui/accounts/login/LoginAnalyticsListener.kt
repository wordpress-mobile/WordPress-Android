package org.wordpress.android.ui.accounts.login

interface LoginAnalyticsListener {
    fun trackAnalyticsSignIn(isWpcomLogin: Boolean)
    fun trackLoginAccessed()
    fun trackLoginAutofillCredentialsUpdated()
    fun trackLoginMagicLinkOpened()
    fun trackLoginMagicLinkSucceeded()
    fun trackUrlFormViewed()
    fun trackConnectedSiteInfoRequested(url: String?)
    fun trackConnectedSiteInfoFailed(
        url: String?,
        errorContext: String?,
        errorType: String?,
        errorDescription: String?
    )
    fun trackConnectedSiteInfoSucceeded(properties: Map<String, *>)
    fun trackFailure(message: String?)
    fun trackSubmitClicked()
    fun trackShowHelpClick()
    fun siteAddressFormScreenResumed()
}
