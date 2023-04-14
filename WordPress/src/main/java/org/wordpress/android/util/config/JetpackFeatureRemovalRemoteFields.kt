package org.wordpress.android.util.config

import org.wordpress.android.annotation.RemoteFieldDefaultGenerater
import javax.inject.Inject

const val JP_DEADLINE_REMOTE_FIELD = "jp_deadline"
const val JP_DEADLINE_DEFAULT = ""

@RemoteFieldDefaultGenerater(remoteField = JP_DEADLINE_REMOTE_FIELD, defaultValue = JP_DEADLINE_DEFAULT)
class JPDeadlineConfig @Inject constructor(appConfig: AppConfig) :
    RemoteConfigField<String>(
        appConfig,
        JP_DEADLINE_REMOTE_FIELD
    )

const val PHASE_TWO_BLOG_POST_REMOTE_FIELD = "phase_two_blog_post"
const val PHASE_TWO_BLOG_POST_DEFAULT = ""

@RemoteFieldDefaultGenerater(remoteField = PHASE_TWO_BLOG_POST_REMOTE_FIELD, PHASE_TWO_BLOG_POST_DEFAULT)
class PhaseTwoBlogPostLinkConfig @Inject constructor(appConfig: AppConfig) :
    RemoteConfigField<String>(
        appConfig,
        PHASE_TWO_BLOG_POST_REMOTE_FIELD
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

const val PHASE_NEW_USERS_BLOG_POST_LINK = "phase_new_users_blog_post"
const val PHASE_NEW_USERS_BLOG_POST_LINK_DEFAULT_VALUE = ""

@RemoteFieldDefaultGenerater(
    remoteField = PHASE_NEW_USERS_BLOG_POST_LINK,
    defaultValue = PHASE_NEW_USERS_BLOG_POST_LINK_DEFAULT_VALUE
)
class PhaseNewUsersBlogPostLinkConfig @Inject constructor(appConfig: AppConfig) :
    RemoteConfigField<String>(
        appConfig,
        PHASE_NEW_USERS_BLOG_POST_LINK
    )

const val PHASE_SELF_HOSTED_BLOG_POST_LINK_REMOTE_FIELD = "phase_self_hosted_blog_post"
const val PHASE_SELF_HOSTED_BLOG_POST_LINK_DEFAULT_VALUE = ""

@RemoteFieldDefaultGenerater(
    remoteField = PHASE_SELF_HOSTED_BLOG_POST_LINK_REMOTE_FIELD,
    defaultValue = PHASE_SELF_HOSTED_BLOG_POST_LINK_DEFAULT_VALUE
)
class PhaseSelfHostedPostLinkConfig @Inject constructor(appConfig: AppConfig) :
    RemoteConfigField<String>(
        appConfig,
        PHASE_SELF_HOSTED_BLOG_POST_LINK_REMOTE_FIELD
    )
