package org.wordpress.android.util.experiments

import javax.inject.Inject

class SiteNameABExperiment
@Inject constructor(exPlat: ExPlat) : Experiment(name = "wpandroid_site_name_v1", exPlat)
