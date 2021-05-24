package org.wordpress.android.ui.jetpack.restore

sealed class RestoreNavigationEvents {
    data class VisitSite(val url: String) : RestoreNavigationEvents()
}
