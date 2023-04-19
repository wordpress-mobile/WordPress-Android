package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val BLOGGING_PROMPTS_SOCIAL_REMOTE_FIELD = "blogging_prompts_social_enabled"

@Feature(BLOGGING_PROMPTS_SOCIAL_REMOTE_FIELD, true)
class BloggingPromptsSocialFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
    appConfig,
    BuildConfig.BLOGGING_PROMPTS_SOCIAL,
    BLOGGING_PROMPTS_SOCIAL_REMOTE_FIELD,
) {
    override fun isEnabled(): Boolean {
        return super.isEnabled() && BuildConfig.IS_JETPACK_APP
    }
}
