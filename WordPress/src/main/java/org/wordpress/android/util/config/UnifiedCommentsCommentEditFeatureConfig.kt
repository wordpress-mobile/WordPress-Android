package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

@FeatureInDevelopment
class UnifiedCommentsCommentEditFeatureConfig @Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.UNIFIED_COMMENTS_COMMENT_EDIT
)
