package org.wordpress.android.util.experiments

import org.wordpress.android.fluxc.model.experiments.Variation.Control
import javax.inject.Inject

class ABNTestExperiment
@Inject constructor(exPlat: ExPlat) : Experiment(EXPERIMENT_NAME, exPlat) {

    val isControl: Boolean
        get() = getVariation() == Control

    val isRedVariant: Boolean
        get() = getVariation().name == VARIANT_RED

    val isGreenVariant: Boolean
        get() = getVariation().name == VARIANT_GREEN

    val isBlueVariant: Boolean
        get() = getVariation().name == VARIANT_BLUE

    companion object {
        const val EXPERIMENT_NAME = "a_b_n_android_native_test"
        const val VARIANT_RED = "red_background"
        const val VARIANT_GREEN = "green_background"
        const val VARIANT_BLUE = "blue_background"
    }
}
