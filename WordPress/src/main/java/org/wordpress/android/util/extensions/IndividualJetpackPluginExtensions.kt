package org.wordpress.android.util.extensions

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.JetpackCPConnectedSiteModel

/**
 * @return a List of the active individual Jetpack plugins names
 * (e.g. [Jetpack Search, Jetpack VaultPress Backup]) or null if there are no active Jetpack connection plugins.
 */
fun SiteModel.activeIndividualJetpackPluginNames(): List<String>? =
    activeJetpackConnectionPluginValues()?.filterMapJetpackIndividualPluginValuesToNames()

/**
 * @return a List of the active individual Jetpack plugins names
 * (e.g. [Jetpack Search, Jetpack VaultPress Backup]) or null if there are no active Jetpack connection plugins.
 */
fun JetpackCPConnectedSiteModel.activeIndividualJetpackPluginNames(): List<String> =
    activeJetpackConnectionPlugins.filterMapJetpackIndividualPluginValuesToNames()

/**
 * @return true if the site has active Jetpack individual plugins connected and does not have the full Jetpack plugin
 */
fun SiteModel.isJetpackIndividualPluginConnectedWithoutFullPlugin(): Boolean =
    activeJetpackConnectionPluginValues()?.run {
        isNotEmpty() && none { it.isJetpackFullPlugin() } && any { it.isJetpackIndividualPlugin() }
    } ?: false

/**
 * @return true if the site has active Jetpack individual plugins connected and does not have the full Jetpack plugin
 */
fun JetpackCPConnectedSiteModel.isJetpackIndividualPluginConnectedWithoutFullPlugin(): Boolean =
    activeJetpackConnectionPlugins.run {
        isNotEmpty() && none { it.isJetpackFullPlugin() } && any { it.isJetpackIndividualPlugin() }
    }

private fun List<String>.filterMapJetpackIndividualPluginValuesToNames(): List<String> = this
    .filter { it.isJetpackIndividualPlugin() }
    .mapNotNull {
        when (it) {
            "jetpack-search" -> "Jetpack Search"
            "jetpack-backup" -> "Jetpack VaultPress Backup"
            "jetpack-protect" -> "Jetpack Protect"
            "jetpack-videopress" -> "Jetpack VideoPress"
            "jetpack-social" -> "Jetpack Social"
            "jetpack-boost" -> "Jetpack Boost"
            else -> null
        }
    }

private fun String.isJetpackIndividualPlugin(): Boolean = this.startsWith("jetpack-")

private fun String.isJetpackFullPlugin(): Boolean = this == "jetpack"
