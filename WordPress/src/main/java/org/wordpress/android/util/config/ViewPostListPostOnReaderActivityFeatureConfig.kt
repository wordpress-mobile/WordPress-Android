package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val VIEW_POST_LIST_POST_ON_READER_ACTIVITY_REMOTE_FIELD = "view_post_list_post_on_activity";

@Feature(VIEW_POST_LIST_POST_ON_READER_ACTIVITY_REMOTE_FIELD, false)
class ViewPostListPostOnReaderActivityFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.VIEW_POST_LIST_POST_ON_READER_ACTIVITY,
    VIEW_POST_LIST_POST_ON_READER_ACTIVITY_REMOTE_FIELD
)
