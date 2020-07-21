package org.wordpress.android.util.config

import org.wordpress.android.annotation.Experiment
import org.wordpress.android.util.config.ExampleExperimentConfig.Companion.CONTROL_GROUP
import org.wordpress.android.util.config.ExampleExperimentConfig.Companion.EXAMPLE_EXPERIMENT_REMOTE_FIELD
import javax.inject.Inject

/**
 * An example of how to create and use an experiment.
 * The experiment defines a list of variants.
 */
@SuppressWarnings("Unused")
@Experiment(remoteField = EXAMPLE_EXPERIMENT_REMOTE_FIELD, defaultVariant = CONTROL_GROUP)
class ExampleExperimentConfig
@Inject constructor(appConfig: AppConfig) : ExperimentConfig(appConfig, EXAMPLE_EXPERIMENT_REMOTE_FIELD) {
    private val variantA = Variant(VARIANT_A)
    private val variantB = Variant(VARIANT_B)
    private val controlGroup = Variant(CONTROL_GROUP)
    override val variants: List<Variant>
        get() = listOf(variantA, variantB, controlGroup)
    /**
     * Define the methods you need
     */
    fun isVariantA() = isInVariant(variantA)
    fun isVariantB() = isInVariant(variantB)

    companion object {
        const val EXAMPLE_EXPERIMENT_REMOTE_FIELD = "example_experiment_remote_field"
        const val VARIANT_A = "variant_A"
        const val VARIANT_B = "variant_B"
        const val CONTROL_GROUP = "control_group"
    }
}
