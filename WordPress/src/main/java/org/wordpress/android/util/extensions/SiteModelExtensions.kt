package org.wordpress.android.util.extensions

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

/**
 * @return a List of the active Jetpack connection plugins values
 * (e.g. [jetpack-search, jetpack-backup]) or null if there are no active Jetpack connection plugins.
 */
fun SiteModel.activeJetpackConnectionPluginValues(): List<String>? =
    activeJetpackConnectionPlugins?.split(",")

/**
 * @return true if the Jetpack app has anything to offer this site. The Jetpack-powered features live
 * behind WordPress.com, so a self-hosted site without Jetpack gets nothing out of switching apps —
 * and telling its owner that their site has the Jetpack plugin is simply wrong.
 */
fun SiteModel?.canUseJetpackApp(): Boolean =
    this != null && (isWPCom || isJetpackInstalled || isJetpackConnected)
