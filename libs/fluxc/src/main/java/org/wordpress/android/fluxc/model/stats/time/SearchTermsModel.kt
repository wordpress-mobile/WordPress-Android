package org.wordpress.android.fluxc.model.stats.time

data class SearchTermsModel(
    val otherSearchTerms: Int,
    val totalSearchTerms: Int,
    val list: List<SearchTerm>,
    val hasMore: Boolean
) {
    data class SearchTerm(val text: String, val views: Int)
}
