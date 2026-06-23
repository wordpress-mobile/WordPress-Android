package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

/**
 * Configuration of the categories REST API migration (self-hosted wp-rs taxonomy path).
 */
@Feature(CategoriesRestApiMigrationFeatureConfig.CATEGORIES_REST_API_MIGRATION_REMOTE_FIELD, false)
class CategoriesRestApiMigrationFeatureConfig
@Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.CATEGORIES_REST_API_MIGRATION,
    CATEGORIES_REST_API_MIGRATION_REMOTE_FIELD
) {
    companion object {
        const val CATEGORIES_REST_API_MIGRATION_REMOTE_FIELD = "categories_rest_api_migration"
    }
}
