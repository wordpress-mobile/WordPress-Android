package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.BloggingPromptsEnhancementsFeatureConfig.Companion.BLOGGING_PROMPTS_ENHANCEMENTS_REMOTE_FIELD
import javax.inject.Inject

@Feature(BLOGGING_PROMPTS_ENHANCEMENTS_REMOTE_FIELD, true)
class BloggingPromptsEnhancementsFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
    appConfig,
    BuildConfig.BLOGGING_PROMPTS_ENHANCEMENTS,
    BLOGGING_PROMPTS_ENHANCEMENTS_REMOTE_FIELD,
) {
    override fun isEnabled(): Boolean {
        return super.isEnabled() && BuildConfig.IS_JETPACK_APP
    }

    companion object {
        const val BLOGGING_PROMPTS_ENHANCEMENTS_REMOTE_FIELD = "blogging_prompts_enhancements_enabled"
    }
}
