package org.wordpress.android.ui.activitylog

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import javax.inject.Inject

/**
 * Determines which activity log groups should be hidden from the user for a given site.
 *
 * WordPress.com backs up Simple sites internally and writes those events to the activity log regardless of plan,
 * so the activity endpoint can return backup ("rewind") and scan activities for sites whose plan does not include
 * those products. The web Activity Log hides these groups (see getActivityLogHiddenGroups in wp-calypso), and the
 * apps need to do the same to stay consistent with it.
 *
 * The groups stay visible when the site has the "backups-self-serve" plan feature (the same check the web makes).
 * The plan feature list is not guaranteed to reflect standalone product purchases on self-hosted Jetpack sites,
 * so each group also stays visible when the matching Jetpack product was purchased.
 */
class GetActivityLogHiddenGroupsUseCase @Inject constructor(
    private val jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase
) {
    fun getHiddenGroups(site: SiteModel): List<String> {
        if (hasBackupsSelfServeFeature(site)) {
            return emptyList()
        }
        val purchasedProducts = jetpackCapabilitiesUseCase.getCachedJetpackPurchasedProducts(site.siteId)
        return listOfNotNull(
            REWIND_GROUP.takeUnless { purchasedProducts.backup },
            SCAN_GROUP.takeUnless { purchasedProducts.scan }
        )
    }

    private fun hasBackupsSelfServeFeature(site: SiteModel) =
        site.planActiveFeatures?.split(",")?.contains(BACKUPS_SELF_SERVE_FEATURE) == true
}

private const val BACKUPS_SELF_SERVE_FEATURE = "backups-self-serve"
private const val REWIND_GROUP = "rewind"
private const val SCAN_GROUP = "scan"
