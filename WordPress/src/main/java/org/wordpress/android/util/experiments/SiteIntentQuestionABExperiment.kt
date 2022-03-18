package org.wordpress.android.util.experiments

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.util.experiments.SiteIntentQuestionABExperiment.Variation.CONTROL
import org.wordpress.android.util.experiments.SiteIntentQuestionABExperiment.Variation.TREATMENT
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SiteIntentQuestionABExperiment
@Inject constructor(private val accountStore: AccountStore) {
    enum class Variation {
        CONTROL,
        TREATMENT
    }

    private val default = CONTROL

    val variant: Variation
        get() = calculateVariant()

    private fun calculateVariant() = if (!accountStore.hasAccessToken()) {
        default
    } else accountStore.accessToken?.hashCode()?.let { hash ->
        if (hash % 2L == 0L) CONTROL else TREATMENT
    } ?: run {
        default
    }
}
