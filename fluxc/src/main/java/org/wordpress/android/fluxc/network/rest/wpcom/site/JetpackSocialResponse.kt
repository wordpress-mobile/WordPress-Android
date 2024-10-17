package org.wordpress.android.fluxc.network.rest.wpcom.site

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response

data class JetpackSocialResponse(
    @SerializedName("is_share_limit_enabled") val isShareLimitEnabled: Boolean?,
    @SerializedName("to_be_publicized_count") val toBePublicizedCount: Int?,
    @SerializedName("share_limit") val shareLimit: Int?,
    @SerializedName("publicized_count") val publicizedCount: Int?,
    @SerializedName("shared_posts_count") val sharedPostsCount: Int?,
    @SerializedName("shares_remaining") val sharesRemaining: Int?,
    @SerializedName("is_enhanced_publishing_enabled") val isEnhancedPublishingEnabled: Boolean?,
    @SerializedName("is_social_image_generator_enabled") val isSocialImageGeneratorEnabled: Boolean?,
) : Response
