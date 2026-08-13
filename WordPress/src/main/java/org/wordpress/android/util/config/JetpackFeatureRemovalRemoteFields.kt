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

