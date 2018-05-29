package org.wordpress.android.util

import org.wordpress.android.fluxc.model.SiteModel

val SiteModel.logInformation: String
    get() {
        val typeLog = "Type: ($stateLogInformation)"
        val usernameLog = if (isUsingWpComRestApi) "" else username
        val urlLog = "${if (isUsingWpComRestApi) "REST" else "Self-hosted"} URL: $url"
        val planLog = if (isUsingWpComRestApi) "Plan: $planShortName ($planId)" else ""
        val jetpackVersionLog = if (isJetpackInstalled) "Jetpack-version: $jetpackVersion" else ""
        return listOf(typeLog, usernameLog, urlLog, planLog, jetpackVersionLog)
                .filter { it != "" }
                .joinToString(separator = " ", prefix = "<", postfix = ">")
    }

val SiteModel.stateLogInformation: String
    get() {
        val apiString = if (isUsingWpComRestApi) "REST" else "XML-RPC"
        return when {
            isWPCom -> "wpcom"
            isJetpackConnected -> "jetpack_connected - $apiString"
            isJetpackInstalled -> "self-hosted - jetpack_installed"
            else -> "self_hosted"
        }
    }
