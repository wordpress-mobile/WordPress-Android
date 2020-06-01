package org.wordpress.android.util.config

/**
 * This class represents an abstract experiment configuration. An experiment has a list of variants. In this case the
 * To add an experiment don't forget to update the `remote_config_defaults.xml` file.
 * @param appConfig class that loads the feature configuration
 * @param remoteField is the key of the feature flag in the remote config file
 */
abstract class ExperimentConfig(
    private val appConfig: AppConfig,
    val remoteField: String
) {
    /**
     * List of all the variants in an experiment
     */
    abstract val variants: List<Variant>

    /**
     * This class represents a single variant of the experiment
     */
    data class Variant(val value: String)

    /**
     * Gets the current variant for the experiment
     */
    fun getVariant(): Variant {
        return appConfig.getCurrentVariant(this)
    }

    /**
     * Returns true if the variant is the current variant for the experiment
     */
    fun isInVariant(variant: Variant): Boolean {
        return getVariant() == variant
    }
}
