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
 * @return a List of the active Jetpack connection plugins names
 * (e.g. [Jetpack Search, Jetpack VaultPress Backup]) or null if there are no active Jetpack connection plugins.
 */
fun SiteModel.activeJetpackConnectionPluginNames(): List<String>? =
    activeJetpackConnectionPluginValues()?.filter { it.startsWith("jetpack-") }
        ?.map {
            when (it) {
                "jetpack-search" -> "Jetpack Search"
                "jetpack-backup" -> "Jetpack VaultPress Backup"
                "jetpack-protect" -> "Jetpack Protect"
                "jetpack-videopress" -> "Jetpack VideoPress"
                "jetpack-social" -> "Jetpack Social"
                "jetpack-boost" -> "Jetpack Boost"
                else -> ""
            }
        }
        ?.filter { it.isNotEmpty() }


fun SiteModel.isJetpackConnectedWithoutFullPlugin(): Boolean =
    activeJetpackConnectionPluginValues()?.run {
        isNotEmpty() && !contains("jetpack") && firstOrNull { it.startsWith("jetpack-") } != null
    } ?: false
