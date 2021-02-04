package org.wordpress.android.util.experiments

import org.wordpress.android.fluxc.model.experiments.Variation.Treatment
import javax.inject.Inject

class BiasAAExperiment
@Inject constructor(
    exPlat: ExPlat
) : Experiment(
        id = 20085,
        name = "explat_test_aa_weekly_wpandroid_2021_week_06",
        exPlat
) {
    fun getEventProperties() = mapOf(
            "experiment_id" to id,
            "experiment_variation" to getVariation().let { if (it is Treatment) it.name else "control" }
    )
}
