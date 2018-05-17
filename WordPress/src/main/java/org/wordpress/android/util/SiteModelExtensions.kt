package org.wordpress.android.util

import org.wordpress.android.fluxc.model.SiteModel

val SiteModel.logInformation: String
    get() {
        val usernameStr = if (username.isNullOrEmpty()) username else "NO"
        val extraStr = if (isWPCom) {
            " wp.com account: $usernameStr blogId: $siteId plan: $planShortName ($planId)"
        } else {
            " jetpack: $jetpackLogInformation"
        }
        return "<Blog Name: ${SiteUtils.getSiteNameOrHomeURL(this)} URL: $url XML-RPC: $xmlRpcUrl$extraStr>"
    }

val SiteModel.jetpackLogInformation: String
    get() {
        val jpVersionStr = if (jetpackVersion.isNullOrEmpty()) "unknown" else jetpackVersion
        val siteIdStr = if (siteId == 0L) "unknown" else "$siteId"
        return when {
            // We don't add the username of the user to the jetpack connected string (ex: connected as exampleUsername)
            // because we don't know whether the current user is the one that's connected with Jetpack.
            isJetpackConnected -> "🚀✅ Jetpack $jpVersionStr connected with site ID $siteIdStr"
            isJetpackInstalled -> "🚀❌ Jetpack $jpVersionStr not connected"
            else -> "🚀❔Jetpack not installed"
        }
    }

val SiteModel.stateLogInformation: String
    get() {
        val apiString = if (isUsingWpComRestApi) "REST" else "XML-RPC"
        return when {
            isWPCom -> "wpcom"
            isJetpackConnected -> "jetpack_connected - $apiString"
            isJetpackInstalled -> "jetpack_installed"
            else -> "self_hosted"
        }
    }
