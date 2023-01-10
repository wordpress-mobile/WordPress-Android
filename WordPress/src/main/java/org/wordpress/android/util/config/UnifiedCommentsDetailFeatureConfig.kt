package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.UnifiedCommentsDetailFeatureConfig.Companion.UNIFIED_COMMENTS_DETAILS_REMOTE_FIELD
import javax.inject.Inject

/**
 * Configuration of the Unified Comments list improvements
 */
@Feature(UNIFIED_COMMENTS_DETAILS_REMOTE_FIELD, false)
class UnifiedCommentsDetailFeatureConfig
@Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.UNIFIED_COMMENTS_DETAILS,
    UNIFIED_COMMENTS_DETAILS_REMOTE_FIELD
) {
    companion object {
        const val UNIFIED_COMMENTS_DETAILS_REMOTE_FIELD = "unified_comments_detail_remote_field"
    }
}
