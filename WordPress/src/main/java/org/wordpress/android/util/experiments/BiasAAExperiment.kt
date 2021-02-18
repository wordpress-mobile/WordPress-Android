package org.wordpress.android.util.experiments

import javax.inject.Inject

class BiasAAExperiment
@Inject constructor(
    exPlat: ExPlat
) : Experiment(
        name = "explat_test_aa_weekly_wpandroid_2021_week_06",
        exPlat
)
