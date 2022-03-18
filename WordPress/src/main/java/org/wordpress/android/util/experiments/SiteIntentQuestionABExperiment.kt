package org.wordpress.android.util.experiments

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.util.experiments.SiteIntentQuestionABExperiment.Variation.CONTROL
import org.wordpress.android.util.experiments.SiteIntentQuestionABExperiment.Variation.TREATMENT
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SiteIntentQuestionABExperiment
@Inject constructor(private val accountStore: AccountStore, private val tracker: SiteCreationTracker) {
    enum class Variation(val key: String) {
        CONTROL("control"),
        TREATMENT("treatment")
    }

    private val default = CONTROL

    val variant: Variation
        get() = calculateVariant()

    private fun calculateVariant(): Variation {
        val variation = if (!accountStore.hasAccessToken()) {
            default
        } else accountStore.accessToken?.hashCode()?.let { hash ->
            if (hash % 2L == 0L) CONTROL else TREATMENT
        } ?: run {
            default
        }
        tracker.trackSiteIntentQuestionExperimentVariation(variation)
        return variation
    }
}
