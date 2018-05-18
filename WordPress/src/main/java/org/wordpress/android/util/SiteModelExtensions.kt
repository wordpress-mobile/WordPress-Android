package org.wordpress.android.util

import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel

fun SiteModel.logInformation(account: AccountModel?): String {
    val usernameStr = if (username.isNullOrEmpty()) username else "NO"
    val extraStr = if (isWPCom) {
        " wp.com account: $usernameStr blogId: $siteId plan: $planShortName ($planId)"
    } else {
        " jetpack: ${jetpackLogInformation(account)}"
    }
    return "<Blog Name: ${SiteUtils.getSiteNameOrHomeURL(this)} URL: $url XML-RPC: $xmlRpcUrl$extraStr>"
}

/**
 * This method currently doesn't handle an edge case where the site is connected with Jetpack but the current user
 * is not the one that's connected, so it's still a self-hosted site.
 *
 * TODO: Figure out how we can improve the Jetpack logging to include the said case
 */
fun SiteModel.jetpackLogInformation(account: AccountModel?): String? {
    val usernameStr = if (account == null) "UNKNOWN" else account.userName
    val jpVersionStr = if (jetpackVersion.isNullOrEmpty()) "unknown" else jetpackVersion
    val siteIdStr = if (siteId == 0L) "unknown" else "$siteId"
    return when {
        isJetpackConnected -> "🚀✅ Jetpack $jpVersionStr connected as $usernameStr with site ID $siteIdStr"
        isJetpackInstalled -> "🚀❌ Jetpack $jpVersionStr not connected"
        else -> "🚀❔Jetpack not installed"
    }
}
