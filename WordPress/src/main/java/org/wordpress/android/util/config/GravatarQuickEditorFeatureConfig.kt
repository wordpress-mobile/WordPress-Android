package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

@Feature(GravatarQuickEditorFeatureConfig.GRAVATAR_QUICK_EDITOR_REMOTE_FIELD, true)
class GravatarQuickEditorFeatureConfig @Inject constructor(appConfig: AppConfig) : FeatureConfig(
    appConfig,
    BuildConfig.GRAVATAR_QUICK_EDITOR,
    GRAVATAR_QUICK_EDITOR_REMOTE_FIELD
) {
    override fun isEnabled(): Boolean {
        return super.isEnabled() && BuildConfig.GRAVATAR_QUICK_EDITOR
    }

    companion object {
        const val GRAVATAR_QUICK_EDITOR_REMOTE_FIELD = "gravatar_quick_editor"
    }
}
