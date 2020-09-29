package org.wordpress.android.ui.photopicker

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment
import org.wordpress.android.R
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
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.WP_LIBRARY
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
                    buildLocalMediaPickerSetup(browserType),
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
                    buildLocalMediaPickerSetup(browserType),
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
                    buildLocalMediaPickerSetup(GRAVATAR_IMAGE_PICKER)
            )
        } else {
            Intent(fragment.requireContext(), PhotoPickerActivity::class.java).apply {
                this.putExtra(MediaBrowserActivity.ARG_BROWSER_TYPE, GRAVATAR_IMAGE_PICKER)
            }
        }
        fragment.startActivityForResult(intent, RequestCodes.PHOTO_PICKER)
    }

    fun showFilePicker(activity: Activity) {
        if (consolidatedMediaPickerFeatureConfig.isEnabled()) {
            val allowedTypes = mutableSetOf(IMAGE, VIDEO, AUDIO, DOCUMENT)
            val mediaPickerSetup = MediaPickerSetup(
                    dataSource = DEVICE,
                    canMultiselect = true,
                    requiresStoragePermissions = true,
                    allowedTypes = allowedTypes,
                    cameraEnabled = false,
                    systemPickerEnabled = true,
                    editingEnabled = true,
                    title = R.string.photo_picker_choose_file
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

    fun viewWPMediaLibraryPickerForResult(activity: Activity, site: SiteModel, browserType: MediaBrowserType) {
        if (consolidatedMediaPickerFeatureConfig.isEnabled()) {
            val intent = MediaPickerActivity.buildIntent(
                    activity,
                    browserType,
                    buildWPMediaLibraryPickerSetup(browserType),
                    site
            )
            val requestCode: Int = if (browserType.canMultiselect()) {
                RequestCodes.MULTI_SELECT_MEDIA_PICKER
            } else {
                RequestCodes.SINGLE_SELECT_MEDIA_PICKER
            }
            activity.startActivityForResult(intent, requestCode)
        } else {
            ActivityLauncher.viewMediaPickerForResult(activity, site, browserType)
        }
    }

    private fun buildLocalMediaPickerSetup(browserType: MediaBrowserType): MediaPickerSetup {
        val allowedTypes = mutableSetOf<MediaType>()
        if (browserType.isImagePicker) {
            allowedTypes.add(IMAGE)
        }
        if (browserType.isVideoPicker) {
            allowedTypes.add(VIDEO)
        }
        val title = if (browserType.isImagePicker && browserType.isVideoPicker) {
            R.string.photo_picker_photo_or_video_title
        } else if (browserType.isVideoPicker) {
            R.string.photo_picker_video_title
        } else {
            R.string.photo_picker_title
        }
        return MediaPickerSetup(
                dataSource = DEVICE,
                canMultiselect = browserType.canMultiselect(),
                requiresStoragePermissions = true,
                allowedTypes = allowedTypes,
                cameraEnabled = browserType.isWPStoriesPicker,
                systemPickerEnabled = true,
                editingEnabled = browserType.isImagePicker,
                title = title
        )
    }

    private fun buildWPMediaLibraryPickerSetup(browserType: MediaBrowserType): MediaPickerSetup {
        val allowedTypes = mutableSetOf<MediaType>()
        if (browserType.isImagePicker) {
            allowedTypes.add(IMAGE)
        }
        if (browserType.isVideoPicker) {
            allowedTypes.add(VIDEO)
        }
        return MediaPickerSetup(
                dataSource = WP_LIBRARY,
                canMultiselect = browserType.canMultiselect(),
                requiresStoragePermissions = false,
                allowedTypes = allowedTypes,
                cameraEnabled = browserType.isWPStoriesPicker,
                systemPickerEnabled = false,
                editingEnabled = false,
                title = R.string.wp_media_title
        )
    }
}
