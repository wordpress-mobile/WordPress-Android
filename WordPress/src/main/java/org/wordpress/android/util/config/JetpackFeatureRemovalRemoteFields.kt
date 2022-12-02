package org.wordpress.android.util.config

import org.wordpress.android.annotation.RemoteFieldDefaultGenerater
import javax.inject.Inject

const val JP_DEADLINE_REMOTE_FIELD = "jp_deadline"
const val JP_DEADLINE_DEFAULT = ""

@RemoteFieldDefaultGenerater(remoteField = JP_DEADLINE_REMOTE_FIELD, defaultValue = JP_DEADLINE_DEFAULT)
class JPDeadlineConfig @Inject constructor(appConfig: AppConfig) :
        RemoteConfigField<String>(
                appConfig,
                JP_DEADLINE_REMOTE_FIELD,
                JP_DEADLINE_DEFAULT
        )

const val PHASE_TWO_BLOG_POST_LINK_REMOTE_FIELD = "phase_two_blog_post"
const val PHASE_TWO_BLOG_POST_LINK_DEFAULT = ""

@RemoteFieldDefaultGenerater(remoteField = PHASE_TWO_BLOG_POST_LINK_REMOTE_FIELD, PHASE_TWO_BLOG_POST_LINK_DEFAULT)
class PhaseTwoBlogPostLinkConfig @Inject constructor(appConfig: AppConfig) :
        RemoteConfigField<String>(
                appConfig,
                PHASE_TWO_BLOG_POST_LINK_REMOTE_FIELD,
                PHASE_TWO_BLOG_POST_LINK_DEFAULT
        )

const val PHASE_THREE_BLOG_POST_LINK_REMOTE_FIELD = "phase_three_blog_post"
const val PHASE_THREE_BLOG_POST_LINK_DEFAULT_VALUE = ""

@RemoteFieldDefaultGenerater(
        remoteField = PHASE_THREE_BLOG_POST_LINK_REMOTE_FIELD,
        defaultValue = PHASE_THREE_BLOG_POST_LINK_DEFAULT_VALUE
)
class PhaseThreeBlogPostLinkConfig @Inject constructor(appConfig: AppConfig) :
        RemoteConfigField<String>(
                appConfig,
                PHASE_THREE_BLOG_POST_LINK_REMOTE_FIELD,
                PHASE_THREE_BLOG_POST_LINK_DEFAULT_VALUE
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
                PHASE_FOUR_BLOG_POST_LINK_REMOTE_FIELD,
                PHASE_FOUR_BLOG_POST_LINK_DEFAULT_VALUE
        )

const val PHASE_FIVE_BLOG_POST_LINK_REMOTE_FIELD = "phase_five_blog_post"
const val PHASE_FIVE_BLOG_POST_LINK_DEFAULT_VALUE = ""

@RemoteFieldDefaultGenerater(
        remoteField = PHASE_FIVE_BLOG_POST_LINK_REMOTE_FIELD,
        defaultValue = PHASE_FIVE_BLOG_POST_LINK_DEFAULT_VALUE
)
class PhaseNewUsersBlogPostLinkConfig @Inject constructor(appConfig: AppConfig) :
        RemoteConfigField<String>(
                appConfig,
                PHASE_FIVE_BLOG_POST_LINK_REMOTE_FIELD,
                PHASE_FIVE_BLOG_POST_LINK_DEFAULT_VALUE
        )

const val PHASE_SIX_BLOG_POST_LINK_REMOTE_FIELD = "phase_six_blog_post"
const val PHASE_SIX_BLOG_POST_LINK_DEFAULT_VALUE = ""

@RemoteFieldDefaultGenerater(
        remoteField = PHASE_SIX_BLOG_POST_LINK_REMOTE_FIELD,
        defaultValue = PHASE_SIX_BLOG_POST_LINK_DEFAULT_VALUE
)
class PhaseSelfHostedPostLinkConfig @Inject constructor(appConfig: AppConfig) :
        RemoteConfigField<String>(
                appConfig,
                PHASE_SIX_BLOG_POST_LINK_REMOTE_FIELD,
                PHASE_SIX_BLOG_POST_LINK_DEFAULT_VALUE
        )

