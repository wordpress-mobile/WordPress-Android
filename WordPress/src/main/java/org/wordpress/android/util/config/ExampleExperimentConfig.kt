package org.wordpress.android.util.config

import javax.inject.Inject

/**
 * An example of how to create and use an experiment.
 * The experiment defines a list of variants.
 */
@SuppressWarnings("Unused")
class ExampleExperimentConfig
@Inject constructor(appConfig: AppConfig) : ExperimentConfig(appConfig, "testing_experiment") {
    val VariantA = Variant("variant A")
    val VariantB = Variant("variant B")
    override val variants: List<Variant>
        get() = listOf(VariantA, VariantB)
    /**
     * Define the methods you need
     */
    fun isVariantA() = isInVariant(VariantA)
    fun isVariantB() = isInVariant(VariantB)
}
