package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.AnyFileUploadFeatureConfig.Companion.ANY_FILE_UPLOAD_REMOTE_FIELD
import javax.inject.Inject

@Feature(ANY_FILE_UPLOAD_REMOTE_FIELD)
class AnyFileUploadFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.ANY_FILE_UPLOAD,
        ANY_FILE_UPLOAD_REMOTE_FIELD
) {
    companion object {
        const val ANY_FILE_UPLOAD_REMOTE_FIELD = "any_file_upload_enabled"
    }
}
