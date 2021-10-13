package org.wordpress.android.ui.domains

data class DomainSuggestionItem(
    val domainName: String,
    val cost: String,
    val isFree: Boolean,
    val supportsPrivacy: Boolean,
    val productId: Int,
    val productSlug: String?,
    val vendor: String?,
    val relevance: Float,
    val isSelected: Boolean,
    val isCostVisible: Boolean,
    val isFreeWithCredits: Boolean
)
