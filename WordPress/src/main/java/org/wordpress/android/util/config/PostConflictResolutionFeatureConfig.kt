package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val POST_CONFLICT_RESOLUTION_FEATURE_REMOTE_FIELD = "sync_publishing"

@Feature(POST_CONFLICT_RESOLUTION_FEATURE_REMOTE_FIELD, false)
class PostConflictResolutionFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.SYNC_PUBLISHING,
    POST_CONFLICT_RESOLUTION_FEATURE_REMOTE_FIELD
)
