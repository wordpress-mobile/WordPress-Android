package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import javax.inject.Inject

class AnyFileUploadFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(appConfig, BuildConfig.ANY_FILE_UPLOAD)
