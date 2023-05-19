package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val SITE_EDITOR_MVP_REMOTE_FIELD = "site_editor_mvp"

@Feature(SITE_EDITOR_MVP_REMOTE_FIELD, true)
class SiteEditorMVPFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.SITE_EDITOR_MVP,
    SITE_EDITOR_MVP_REMOTE_FIELD
)
