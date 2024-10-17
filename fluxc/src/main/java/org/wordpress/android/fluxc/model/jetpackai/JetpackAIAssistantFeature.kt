package org.wordpress.android.fluxc.model.jetpackai

data class UsagePeriod(
    val currentStart: String,
    val nextStart: String,
    val requestsCount: Int,
)

data class Tier(
    val slug: String,
    val limit: Int,
    val value: Int,
    val readableLimit: String?
)

data class JetpackAiLogoGenerator(
    val logo: Int
)

data class FeaturedPostImage(
    val image: Int
)

data class Costs(
    val jetpackAiLogoGenerator: JetpackAiLogoGenerator,
    val featuredPostImage: FeaturedPostImage
)

data class JetpackAIAssistantFeature(
    val hasFeature: Boolean,
    val isOverLimit: Boolean,
    val requestsCount: Int,
    val requestsLimit: Int,
    val usagePeriod: UsagePeriod?,
    val siteRequireUpgrade: Boolean,
    val upgradeType: String,
    val upgradeUrl: String?,
    val currentTier: Tier?,
    val nextTier: Tier?,
    val tierPlans: List<Tier>,
    val tierPlansEnabled: Boolean,
    val costs: Costs?
)
