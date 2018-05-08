package org.wordpress.android.util

import org.wordpress.android.fluxc.model.SiteModel

fun SiteModel.logInformation(username: String?): String {
    val usernameStr = if (username.isNullOrEmpty()) username else "NO"
    val extraStr = if (isWPCom) {
        " wp.com account: $usernameStr blogId: $siteId plan: $planShortName ($planId)"
    } else {
        " jetpack: ${jetpackLogInformation(username)}"
    }
    return "<Blog Name: ${SiteUtils.getSiteNameOrHomeURL(this)} URL: $url XML-RPC: $xmlRpcUrl$extraStr>"
}

fun SiteModel.jetpackLogInformation(username: String?): String? {
    val usernameStr = if (username.isNullOrEmpty()) "UNKNOWN" else username
    val jpVersionStr = if (jetpackVersion.isNullOrEmpty()) "unknown" else jetpackVersion
    val siteIdStr = if (siteId == 0L) "unknown" else "$siteId"
    return when {
        isJetpackConnected -> "🚀✅ Jetpack $jpVersionStr connected as $usernameStr with site ID $siteIdStr"
        isJetpackInstalled -> "🚀❌ Jetpack $jpVersionStr not connected"
        else -> "🚀❔Jetpack not installed"
    }
}
