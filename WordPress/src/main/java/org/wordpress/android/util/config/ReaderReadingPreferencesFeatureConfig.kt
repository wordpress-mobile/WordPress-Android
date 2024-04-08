package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val READING_PREFERENCES_REMOTE_FIELD = "reading_preferences"

@Feature(READING_PREFERENCES_REMOTE_FIELD, true)
class ReaderReadingPreferencesFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
    appConfig,
    BuildConfig.READER_READING_PREFERENCES,
    READING_PREFERENCES_REMOTE_FIELD,
) {
    override fun isEnabled(): Boolean {
        return super.isEnabled() && BuildConfig.IS_JETPACK_APP
    }
}
