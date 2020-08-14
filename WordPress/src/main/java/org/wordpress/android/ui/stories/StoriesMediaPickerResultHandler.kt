package org.wordpress.android.ui.stories

import android.app.Activity
import android.content.Intent
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.PagePostCreationSourcesDetail
import org.wordpress.android.ui.media.MediaBrowserActivity
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.photopicker.MediaPickerConstants
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.UTILS

class StoriesMediaPickerResultHandler {
    companion object {
        /* return true if MediaPickerResult was handled */
        fun handleMediaPickerResultForStories(
            data: Intent,
            activity: Activity?,
            selectedSite: SiteModel?
        ): Boolean {
            if (data.getBooleanExtra(MediaPickerConstants.EXTRA_LAUNCH_WPSTORIES_CAMERA_REQUESTED, false)) {
                ActivityLauncher.addNewStoryForResult(
                        activity,
                        selectedSite,
                        PagePostCreationSourcesDetail.STORY_FROM_MY_SITE
                )
                return true
            } else if (isWPStoriesMediaBrowserTypeResult(data)) {
                if (data.hasExtra(MediaBrowserActivity.RESULT_IDS)) {
                    ActivityLauncher.addNewStoryWithMediaIdsForResult(
                            activity,
                            selectedSite,
                            PagePostCreationSourcesDetail.STORY_FROM_MY_SITE,
                            data.getLongArrayExtra(
                                    MediaBrowserActivity.RESULT_IDS
                            )
                    )
                    return true
                } else {
                    val mediaUriStringsArray = data.getStringArrayExtra(
                            MediaPickerConstants.EXTRA_MEDIA_URIS
                    )
                    if (mediaUriStringsArray.isNullOrEmpty()) {
                        AppLog.e(
                                UTILS,
                                "Can't resolve picked or captured image"
                        )
                        return false
                    }
                    ActivityLauncher.addNewStoryWithMediaUrisForResult(
                            activity,
                            selectedSite,
                            PagePostCreationSourcesDetail.STORY_FROM_MY_SITE,
                            mediaUriStringsArray
                    )
                    return true
                }
            }
            return false
        }

        private fun isWPStoriesMediaBrowserTypeResult(data: Intent): Boolean {
            if (data.hasExtra(MediaBrowserActivity.ARG_BROWSER_TYPE)) {
                val browserType = data.getSerializableExtra(MediaBrowserActivity.ARG_BROWSER_TYPE)
                return (browserType as MediaBrowserType).isWPStoriesPicker
            }
            return false
        }
    }
}
