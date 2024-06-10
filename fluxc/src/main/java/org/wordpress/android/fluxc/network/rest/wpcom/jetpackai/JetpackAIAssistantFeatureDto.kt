package org.wordpress.android.fluxc.network.rest.wpcom.jetpackai

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.jetpackai.Costs
import org.wordpress.android.fluxc.model.jetpackai.FeaturedPostImage
import org.wordpress.android.fluxc.model.jetpackai.JetpackAIAssistantFeature
import org.wordpress.android.fluxc.model.jetpackai.JetpackAiLogoGenerator
import org.wordpress.android.fluxc.model.jetpackai.Tier
import org.wordpress.android.fluxc.model.jetpackai.UsagePeriod

data class UsagePeriodDto(
    @SerializedName("current-start")
    val currentStart: String?,
    @SerializedName("next-start")
    val nextStart: String?,
    @SerializedName("requests-count")
    val requestsCount: Int?,
) {
    fun toUsagePeriod(): UsagePeriod {
        return UsagePeriod(
            currentStart = currentStart.orEmpty(),
            nextStart = nextStart.orEmpty(),
            requestsCount = requestsCount ?: 0
        )
    }
}

data class TierDto(
    @SerializedName("slug")
    val slug: String?,
    @SerializedName("limit")
    val limit: Int?,
    @SerializedName("value")
    val value: Int?,
    @SerializedName("readable-limit")
    val readableLimit: String?
) {
    fun toTier(): Tier {
        return Tier(
            slug = slug.orEmpty(),
            limit = limit ?: 0,
            value = value ?: 0,
            readableLimit = readableLimit
        )
    }
}

data class JetpackAiLogoGeneratorDto(
    @SerializedName("logo") val logo: Int
) {
    fun toJetpackAiLogoGenerator(): JetpackAiLogoGenerator {
        return JetpackAiLogoGenerator(
            logo = logo
        )
    }
}

data class FeaturedPostImageDto(
    @SerializedName("image") val image: Int
) {
    fun toFeaturedPostImage(): FeaturedPostImage {
        return FeaturedPostImage(
            image = image
        )
    }
}

data class CostsDto(
    @SerializedName("jetpack-ai-logo-generator")
    val jetpackAiLogoGenerator: JetpackAiLogoGeneratorDto,
    @SerializedName("featured-post-image")
    val featuredPostImage: FeaturedPostImageDto
) {
    fun toCosts(): Costs {
        return Costs(
            jetpackAiLogoGenerator = jetpackAiLogoGenerator.toJetpackAiLogoGenerator(),
            featuredPostImage = featuredPostImage.toFeaturedPostImage()
        )
    }
}

data class JetpackAIAssistantFeatureDto(
    @SerializedName("has-feature")
    val hasFeature: Boolean?,
    @SerializedName("is-over-limit")
    val isOverLimit: Boolean?,
    @SerializedName("requests-count")
    val requestsCount: Int?,
    @SerializedName("requests-limit")
    val requestsLimit: Int?,
    @SerializedName("usage-period")
    val usagePeriod: UsagePeriodDto?,
    @SerializedName("site-require-upgrade")
    val siteRequireUpgrade: Boolean?,
    @SerializedName("upgrade-type")
    val upgradeType: String?,
    @SerializedName("upgrade-url")
    val upgradeUrl: String?,
    @SerializedName("current-tier")
    val currentTier: TierDto?,
    @SerializedName("next-tier")
    val nextTier: TierDto?,
    @SerializedName("tier-plans")
    val tierPlans: List<TierDto>?,
    @SerializedName("tier-plans-enabled")
    val tierPlansEnabled: Boolean?,
    @SerializedName("costs")
    val costs: CostsDto?
) {
    fun toJetpackAIAssistantFeature(): JetpackAIAssistantFeature {
        return JetpackAIAssistantFeature(
            hasFeature = hasFeature ?: false,
            isOverLimit = isOverLimit ?: false,
            requestsCount = requestsCount ?: 0,
            requestsLimit = requestsLimit ?: 0,
            usagePeriod = usagePeriod?.toUsagePeriod(),
            siteRequireUpgrade = siteRequireUpgrade ?: false,
            upgradeType = upgradeType.orEmpty(),
            upgradeUrl = upgradeUrl, // Can be null
            currentTier = currentTier?.toTier(),
            nextTier = nextTier?.toTier(),
            tierPlans = tierPlans?.map { it.toTier() } ?: emptyList(),
            tierPlansEnabled = tierPlansEnabled ?: false,
            costs = costs?.toCosts()
        )
    }
}
