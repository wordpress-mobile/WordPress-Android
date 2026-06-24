package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

/**
 * Configuration of the taxonomies REST API migration (self-hosted wp-rs taxonomy path).
 */
@Feature(TaxonomiesRestApiMigrationFeatureConfig.TAXONOMIES_REST_API_MIGRATION_REMOTE_FIELD, false)
class TaxonomiesRestApiMigrationFeatureConfig
@Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.TAXONOMIES_REST_API_MIGRATION,
    TAXONOMIES_REST_API_MIGRATION_REMOTE_FIELD
) {
    companion object {
        const val TAXONOMIES_REST_API_MIGRATION_REMOTE_FIELD = "taxonomies_rest_api_migration"
    }
}
