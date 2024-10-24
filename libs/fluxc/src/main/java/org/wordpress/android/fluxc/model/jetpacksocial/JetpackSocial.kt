package org.wordpress.android.fluxc.model.jetpacksocial

data class JetpackSocial(
    val isShareLimitEnabled: Boolean,
    val toBePublicizedCount: Int,
    val shareLimit: Int,
    val publicizedCount: Int,
    val sharedPostsCount: Int,
    val sharesRemaining: Int,
    val isEnhancedPublishingEnabled: Boolean,
    val isSocialImageGeneratorEnabled: Boolean,
)
