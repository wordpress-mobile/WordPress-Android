package org.wordpress.android.fluxc.network.rest.wpcom.site

data class SitesFeaturesRestResponse(
    val features: Map<Long, SiteFeatures>
)

data class SiteFeatures(
    val active: List<String>
)
