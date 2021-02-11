package org.wordpress.android.util.experiments

import org.wordpress.android.fluxc.model.experiments.Variation

abstract class Experiment(
    val name: String,
    private val exPlat: ExPlat
) {
    @JvmOverloads fun getVariation(shouldFetchIfStale: Boolean = false): Variation {
        return exPlat.getVariation(this, shouldFetchIfStale)
    }
}
