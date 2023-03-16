package org.wordpress.android.util.experiments

import javax.inject.Inject

class SiteCreationDomainPurchasingExperiment
@Inject constructor(
    exPlat: ExPlat,
) : Experiment(
    NAME,
    exPlat
) {
    companion object {
        const val NAME = "jpandroid_site_creation_domain_purchasing_v1"
    }
}
