package org.wordpress.android.util.experiments

import javax.inject.Inject

class LandOnTheEditorABExperiment
@Inject constructor(
    exPlat: ExPlat
) : Experiment(
        name = "wpandroid_land_in_the_editor_phase1_v3",
        exPlat
)
