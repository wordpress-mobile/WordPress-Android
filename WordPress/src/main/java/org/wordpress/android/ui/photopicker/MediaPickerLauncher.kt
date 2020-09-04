package org.wordpress.android.ui.photopicker

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.media.MediaBrowserActivity
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.media.MediaBrowserType.BROWSER
import org.wordpress.android.ui.media.MediaBrowserType.GRAVATAR_IMAGE_PICKER
import org.wordpress.android.ui.mediapicker.MediaPickerActivity
import org.wordpress.android.ui.mediapicker.MediaPickerSetup
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.DEVICE
import org.wordpress.android.ui.mediapicker.MediaType
import org.wordpress.android.ui.mediapicker.MediaType.AUDIO
import org.wordpress.android.ui.mediapicker.MediaType.DOCUMENT
import org.wordpress.android.ui.mediapicker.MediaType.IMAGE
import org.wordpress.android.ui.mediapicker.MediaType.VIDEO
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.config.ConsolidatedMediaPickerFeatureConfig
import javax.inject.Inject

class MediaPickerLauncher
@Inject constructor(private val consolidatedMediaPickerFeatureConfig: ConsolidatedMediaPickerFeatureConfig) {
    fun showPhotoPickerForResult(
        activity: Activity,
        browserType: MediaBrowserType,
        site: SiteModel?,
        localPostId: Int?
    ) {
        if (consolidatedMediaPickerFeatureConfig.isEnabled()) {
            val intent = MediaPickerActivity.buildIntent(
                    activity,
                    browserType,
                    buildMediaPickerSetup(browserType),
                    site,
                    localPostId
            )
            activity.startActivityForResult(intent, RequestCodes.PHOTO_PICKER)
        } else {
            ActivityLauncher.showPhotoPickerForResult(activity, browserType, site, localPostId)
        }
    }

    fun showPhotoPickerForResult(
        fragment: Fragment,
        browserType: MediaBrowserType,
        site: SiteModel?,
        localPostId: Int?
    ) {
        if (consolidatedMediaPickerFeatureConfig.isEnabled()) {
            val intent = MediaPickerActivity.buildIntent(
                    fragment.requireContext(),
                    browserType,
                    buildMediaPickerSetup(browserType),
                    site,
                    localPostId
            )
            fragment.startActivityForResult(intent, RequestCodes.PHOTO_PICKER)
        } else {
            ActivityLauncher.showPhotoPickerForResult(fragment, browserType, site, localPostId)
        }
    }

    fun showGravatarPicker(fragment: Fragment) {
        val intent = if (consolidatedMediaPickerFeatureConfig.isEnabled()) {
            MediaPickerActivity.buildIntent(
                    fragment.requireContext(),
                    GRAVATAR_IMAGE_PICKER,
                    buildMediaPickerSetup(GRAVATAR_IMAGE_PICKER)
            )
        } else {
            Intent(fragment.requireContext(), PhotoPickerActivity::class.java).apply {
                this.putExtra(MediaBrowserActivity.ARG_BROWSER_TYPE, GRAVATAR_IMAGE_PICKER)
            }
        }
        fragment.startActivityForResult(intent, RequestCodes.PHOTO_PICKER)
    }

    private fun buildMediaPickerSetup(browserType: MediaBrowserType): MediaPickerSetup {
        val allowedTypes = mutableSetOf<MediaType>()
        if (browserType.isImagePicker) {
            allowedTypes.add(IMAGE)
        }
        if (browserType.isVideoPicker) {
            allowedTypes.add(VIDEO)
        }
        return MediaPickerSetup(DEVICE, browserType.canMultiselect(), allowedTypes, browserType.isWPStoriesPicker)
    }

    fun showFilePicker(activity: Activity) {
        if (consolidatedMediaPickerFeatureConfig.isEnabled()) {
            val allowedTypes = mutableSetOf(IMAGE, VIDEO, AUDIO, DOCUMENT)
            val mediaPickerSetup = MediaPickerSetup(
                    DEVICE,
                    true,
                    allowedTypes,
                    false
            )
            val intent = MediaPickerActivity.buildIntent(
                    activity,
                    BROWSER,
                    mediaPickerSetup
            )
            activity.startActivityForResult(
                    intent,
                    RequestCodes.FILE_LIBRARY
            )
        } else {
            WPMediaUtils.launchFileLibrary(activity, true)
        }
    }
}
