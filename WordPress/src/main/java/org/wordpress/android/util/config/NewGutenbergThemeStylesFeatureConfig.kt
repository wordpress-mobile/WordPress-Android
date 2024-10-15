package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val NEW_GUTENBERG_THEME_STYLES_FEATURE_REMOTE_FIELD = "experimental_block_editor_theme_styles"

@Feature(NEW_GUTENBERG_THEME_STYLES_FEATURE_REMOTE_FIELD, false)
class NewGutenbergThemeStylesFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.ENABLE_NEW_GUTENBERG_THEME_STYLES,
    NEW_GUTENBERG_THEME_STYLES_FEATURE_REMOTE_FIELD
)
