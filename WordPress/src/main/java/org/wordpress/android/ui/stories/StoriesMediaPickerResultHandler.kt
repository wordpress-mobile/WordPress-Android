package org.wordpress.android.ui.stories

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.PagePostCreationSourcesDetail
import org.wordpress.android.ui.media.MediaBrowserActivity
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.SiteNavigationAction.AddNewStory
import org.wordpress.android.ui.mysite.SiteNavigationAction.AddNewStoryWithMediaIds
import org.wordpress.android.ui.mysite.SiteNavigationAction.AddNewStoryWithMediaUris
import org.wordpress.android.ui.photopicker.MediaPickerConstants
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.UTILS
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class StoriesMediaPickerResultHandler
@Inject constructor() {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation as LiveData<Event<SiteNavigationAction>>

    @Deprecated("Use rather the other handle method and the live data navigation.")
    fun handleMediaPickerResultForStories(
        data: Intent,
        activity: Activity?,
        selectedSite: SiteModel?,
        source: PagePostCreationSourcesDetail
    ): Boolean {
        if (selectedSite == null) {
            return false
        }
        when (val navigationAction = buildNavigationAction(data, selectedSite, source)) {
            is AddNewStory -> ActivityLauncher.addNewStoryForResult(
                activity,
                navigationAction.site,
                navigationAction.source
            )
            is AddNewStoryWithMediaIds -> ActivityLauncher.addNewStoryWithMediaIdsForResult(
                activity,
                navigationAction.site,
                navigationAction.source,
                navigationAction.mediaIds.toLongArray()
            )
            is AddNewStoryWithMediaUris -> ActivityLauncher.addNewStoryWithMediaUrisForResult(
                activity,
                navigationAction.site,
                navigationAction.source,
                navigationAction.mediaUris.toTypedArray()
            )
            else -> {
                return false
            }
        }

        return true
    }

    fun handleMediaPickerResultForStories(
        data: Intent,
        selectedSite: SiteModel,
        source: PagePostCreationSourcesDetail
    ): Boolean {
        val navigationAction = buildNavigationAction(data, selectedSite, source)
        return if (navigationAction != null) {
            _onNavigation.postValue(Event(navigationAction))
            true
        } else {
            false
        }
    }

    private fun buildNavigationAction(
        data: Intent,
        selectedSite: SiteModel,
        source: PagePostCreationSourcesDetail
    ): SiteNavigationAction? {
        if (data.getBooleanExtra(MediaPickerConstants.EXTRA_LAUNCH_WPSTORIES_CAMERA_REQUESTED, false)) {
            return AddNewStory(selectedSite, source)
        } else if (isWPStoriesMediaBrowserTypeResult(data)) {
            if (data.hasExtra(MediaBrowserActivity.RESULT_IDS)) {
                val mediaIds = data.getLongArrayExtra(MediaBrowserActivity.RESULT_IDS)?.asList() ?: listOf()
                return AddNewStoryWithMediaIds(selectedSite, source, mediaIds)
            } else {
                val mediaUriStringsArray = data.getStringArrayExtra(
                    MediaPickerConstants.EXTRA_MEDIA_URIS
                )
                if (mediaUriStringsArray.isNullOrEmpty()) {
                    AppLog.e(
                        UTILS,
                        "Can't resolve picked or captured image"
                    )
                    return null
                }
                val mediaUris = mediaUriStringsArray.asList()
                return AddNewStoryWithMediaUris(
                    selectedSite,
                    source,
                    mediaUris = mediaUris
                )
            }
        }
        return null
    }

    private fun isWPStoriesMediaBrowserTypeResult(data: Intent): Boolean {
        if (data.hasExtra(MediaBrowserActivity.ARG_BROWSER_TYPE)) {
            val browserType = data.getSerializableExtraCompat<MediaBrowserType>(MediaBrowserActivity.ARG_BROWSER_TYPE)
            return browserType?.isWPStoriesPicker ?: false
        }
        return false
    }
}
