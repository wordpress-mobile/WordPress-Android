package org.wordpress.android.fluxc.network.rest.wpapi.taxonomy

/**
 * Bridge that lets the app gate the self-hosted (wp-rs) taxonomy REST path behind a feature flag.
 * Implemented in the app module against the `taxonomies_rest_api_migration` remote feature flag.
 */
fun interface TaxonomiesRestApiMigrationConfig {
    fun isEnabled(): Boolean
}
