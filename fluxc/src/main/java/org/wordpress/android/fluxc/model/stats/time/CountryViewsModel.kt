package org.wordpress.android.fluxc.model.stats.time

data class CountryViewsModel(
    val otherViews: Int,
    val totalViews: Int,
    val countries: List<Country>,
    val hasMore: Boolean
) {
    data class Country(
        val countryCode: String,
        val fullName: String,
        val views: Int,
        val flagIconUrl: String?,
        val flatFlagIconUrl: String?
    )
}
