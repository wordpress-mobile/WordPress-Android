package org.wordpress.android.util.config

import org.wordpress.android.annotation.RemoteFieldDefaultGenerater
import javax.inject.Inject

const val PHASE_THREE_BLOG_POST_LINK_REMOTE_FIELD = "phase_three_blog_post"
const val PHASE_THREE_BLOG_POST_LINK_DEFAULT_VALUE = ""

@RemoteFieldDefaultGenerater(
    remoteField = PHASE_THREE_BLOG_POST_LINK_REMOTE_FIELD,
    defaultValue = PHASE_THREE_BLOG_POST_LINK_DEFAULT_VALUE
)
class PhaseThreeBlogPostLinkConfig @Inject constructor(appConfig: AppConfig) :
    RemoteConfigField<String>(
        appConfig,
        PHASE_THREE_BLOG_POST_LINK_REMOTE_FIELD
    )

const val PHASE_FOUR_BLOG_POST_LINK_REMOTE_FIELD = "phase_four_blog_post"
const val PHASE_FOUR_BLOG_POST_LINK_DEFAULT_VALUE = ""

@RemoteFieldDefaultGenerater(
    remoteField = PHASE_FOUR_BLOG_POST_LINK_REMOTE_FIELD,
    defaultValue = PHASE_FOUR_BLOG_POST_LINK_DEFAULT_VALUE
)
class PhaseFourBlogPostLinkConfig @Inject constructor(appConfig: AppConfig) :
    RemoteConfigField<String>(
        appConfig,
        PHASE_FOUR_BLOG_POST_LINK_REMOTE_FIELD
    )

const val PHASE_FOUR_OVERLAY_FREQUENCY_IN_DAYS_REMOTE_FIELD = "phase_four_overlay_frequency_in_days"
const val PHASE_FOUR_OVERLAY_FREQUENCY_IN_DAYS_DEFAULT_VALUE = "-1"

@RemoteFieldDefaultGenerater(
    remoteField = PHASE_FOUR_OVERLAY_FREQUENCY_IN_DAYS_REMOTE_FIELD,
    defaultValue = PHASE_FOUR_OVERLAY_FREQUENCY_IN_DAYS_DEFAULT_VALUE
)
class PhaseFourOverlayFrequencyConfig @Inject constructor(appConfig: AppConfig) :
    RemoteConfigField<Int>(
        appConfig,
        PHASE_FOUR_OVERLAY_FREQUENCY_IN_DAYS_REMOTE_FIELD
    )
