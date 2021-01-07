package org.wordpress.android.ui.photopicker

import android.app.Activity
import android.content.Intent
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.media.MediaBrowserActivity
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.media.MediaBrowserType.FEATURED_IMAGE_PICKER
import org.wordpress.android.ui.media.MediaBrowserType.GRAVATAR_IMAGE_PICKER
import org.wordpress.android.ui.media.MediaBrowserType.SITE_ICON_PICKER
import org.wordpress.android.ui.media.MediaBrowserType.WP_STORIES_MEDIA_PICKER
import org.wordpress.android.ui.mediapicker.MediaPickerActivity
import org.wordpress.android.ui.mediapicker.MediaPickerSetup
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.CameraSetup.ENABLED
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.CameraSetup.HIDDEN
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.CameraSetup.STORIES
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.DEVICE
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.GIF_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.STOCK_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.WP_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaType
import org.wordpress.android.ui.mediapicker.MediaType.AUDIO
import org.wordpress.android.ui.mediapicker.MediaType.DOCUMENT
import org.wordpress.android.ui.mediapicker.MediaType.IMAGE
import org.wordpress.android.ui.mediapicker.MediaType.VIDEO
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.ConsolidatedMediaPickerFeatureConfig
import javax.inject.Inject

class MediaPickerLauncher @Inject constructor(
    private val consolidatedMediaPickerFeatureConfig: ConsolidatedMediaPickerFeatureConfig,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun showFeaturedImagePicker(
        activity: Activity,
        site: SiteModel?,
        localPostId: Int
    ) {
        if (consolidatedMediaPickerFeatureConfig.isEnabled()) {
            val availableDataSources = if (site != null && site.isUsingWpComRestApi) {
                setOf(WP_LIBRARY, STOCK_LIBRARY, GIF_LIBRARY)
            } else {
                setOf(WP_LIBRARY, GIF_LIBRARY)
            }

            val mediaPickerSetup = MediaPickerSetup(
                    primaryDataSource = DEVICE,
                    availableDataSources = availableDataSources,
                    canMultiselect = false,
                    requiresStoragePermissions = true,
                    allowedTypes = setOf(IMAGE),
                    cameraSetup = ENABLED,
                    systemPickerEnabled = true,
                    editingEnabled = true,
                    queueResults = true,
                    defaultSearchView = false,
                    title = R.string.photo_picker_title
            )
            val intent = MediaPickerActivity.buildIntent(
                    activity,
                    mediaPickerSetup,
                    site,
                    localPostId
            )
            activity.startActivityForResult(intent, RequestCodes.PHOTO_PICKER)
        } else {
            ActivityLauncher.showPhotoPickerForResult(activity, FEATURED_IMAGE_PICKER, site, localPostId)
        }
    }

    fun showSiteIconPicker(
        activity: Activity,
        site: SiteModel?
    ) {
        if (consolidatedMediaPickerFeatureConfig.isEnabled()) {
            val intent = buildSitePickerIntent(activity, site)
            activity.startActivityForResult(intent, RequestCodes.PHOTO_PICKER)
        } else {
            ActivityLauncher.showPhotoPickerForResult(activity, SITE_ICON_PICKER, site, null)
        }
    }

    fun showSiteIconPicker(
        fragment: Fragment,
        site: SiteModel?
    ) {
        if (consolidatedMediaPickerFeatureConfig.isEnabled()) {
            val intent = buildSitePickerIntent(fragment.requireActivity(), site)
            fragment.startActivityForResult(intent, RequestCodes.SITE_ICON_PICKER)
        } else {
            ActivityLauncher.showPhotoPickerForResult(fragment.requireActivity(), SITE_ICON_PICKER, site, null)
        }
    }

    private fun buildSitePickerIntent(
        activity: Activity,
        site: SiteModel?
    ): Intent {
        val mediaPickerSetup = MediaPickerSetup(
                primaryDataSource = DEVICE,
                availableDataSources = setOf(WP_LIBRARY),
                canMultiselect = false,
                requiresStoragePermissions = true,
                allowedTypes = setOf(IMAGE),
                cameraSetup = ENABLED,
                systemPickerEnabled = true,
                editingEnabled = true,
                queueResults = false,
                defaultSearchView = false,
                title = string.photo_picker_title
        )
        val intent = MediaPickerActivity.buildIntent(
                activity,
                mediaPickerSetup,
                site,
                null
        )
        return intent
    }

    fun showPhotoPickerForResult(
        activity: Activity,
        browserType: MediaBrowserType,
        site: SiteModel?,
        localPostId: Int?
    ) {
        if (consolidatedMediaPickerFeatureConfig.isEnabled()) {
            val intent = MediaPickerActivity.buildIntent(
                    activity,
                    buildLocalMediaPickerSetup(browserType),
                    site,
                    localPostId
            )
            activity.startActivityForResult(intent, RequestCodes.PHOTO_PICKER)
        } else {
            ActivityLauncher.showPhotoPickerForResult(activity, browserType, site, localPostId)
        }
    }

    fun showStoriesPhotoPickerForResultAndTrack(activity: Activity, site: SiteModel?) {
        analyticsTrackerWrapper.track(Stat.MEDIA_PICKER_OPEN_FOR_STORIES)
        showStoriesPhotoPickerForResult(activity, site)
    }

    fun showStoriesPhotoPickerForResult(
        activity: Activity,
        site: SiteModel?
    ) {
        ActivityLauncher.showPhotoPickerForResult(activity, WP_STORIES_MEDIA_PICKER, site, null)
    }

    fun showGravatarPicker(fragment: Fragment) {
        val intent = if (consolidatedMediaPickerFeatureConfig.isEnabled()) {
            val mediaPickerSetup = MediaPickerSetup(
                    primaryDataSource = DEVICE,
                    availableDataSources = setOf(),
                    canMultiselect = false,
                    requiresStoragePermissions = true,
                    allowedTypes = setOf(IMAGE),
                    cameraSetup = ENABLED,
                    systemPickerEnabled = true,
                    editingEnabled = true,
                    queueResults = false,
                    defaultSearchView = false,
                    title = R.string.photo_picker_title
            )
            MediaPickerActivity.buildIntent(
                    fragment.requireContext(),
                    mediaPickerSetup
            )
        } else {
            Intent(fragment.requireContext(), PhotoPickerActivity::class.java).apply {
                this.putExtra(MediaBrowserActivity.ARG_BROWSER_TYPE, GRAVATAR_IMAGE_PICKER)
            }
        }
        fragment.startActivityForResult(intent, RequestCodes.PHOTO_PICKER)
    }

    fun showFilePicker(activity: Activity, canMultiselect: Boolean = true) {
        showMediaPicker(
                activity,
                canMultiselect,
                mutableSetOf(IMAGE, VIDEO, AUDIO, DOCUMENT),
                R.string.photo_picker_choose_file,
                RequestCodes.FILE_LIBRARY
        )
    }

    fun showAudioFilePicker(activity: Activity, canMultiselect: Boolean = false) {
        showMediaPicker(
            activity,
            canMultiselect,
                mutableSetOf(AUDIO),
                R.string.photo_picker_choose_file,
                RequestCodes.AUDIO_LIBRARY
        )
    }

    private fun showMediaPicker(
        activity: Activity,
        canMultiselect: Boolean = false,
        allowedTypes: Set<MediaType>,
        @StringRes title: Int,
        requestCode: Int
    ) {
        if (consolidatedMediaPickerFeatureConfig.isEnabled()) {
            val mediaPickerSetup = MediaPickerSetup(
                    primaryDataSource = DEVICE,
                    availableDataSources = setOf(),
                    canMultiselect = canMultiselect,
                    requiresStoragePermissions = true,
                    allowedTypes = allowedTypes,
                    cameraSetup = HIDDEN,
                    systemPickerEnabled = true,
                    editingEnabled = true,
                    queueResults = false,
                    defaultSearchView = false,
                    title = title
            )
            val intent = MediaPickerActivity.buildIntent(
                    activity,
                    mediaPickerSetup
            )
            activity.startActivityForResult(
                    intent,
                    requestCode
            )
        } else {
            if (requestCode == RequestCodes.FILE_LIBRARY) {
                WPMediaUtils.launchFileLibrary(activity, canMultiselect)
            } else {
                WPMediaUtils.launchAudioFileLibrary(activity, canMultiselect)
            }
        }
    }

    fun viewWPMediaLibraryPickerForResult(activity: Activity, site: SiteModel, browserType: MediaBrowserType) {
        if (consolidatedMediaPickerFeatureConfig.isEnabled()) {
            val intent = MediaPickerActivity.buildIntent(
                    activity,
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

    fun showStockMediaPickerForResult(
        activity: Activity,
        site: SiteModel,
        requestCode: Int,
        allowMultipleSelection: Boolean
    ) {
        if (consolidatedMediaPickerFeatureConfig.isEnabled()) {
            val mediaPickerSetup = MediaPickerSetup(
                    primaryDataSource = STOCK_LIBRARY,
                    availableDataSources = setOf(),
                    canMultiselect = allowMultipleSelection,
                    requiresStoragePermissions = false,
                    allowedTypes = setOf(IMAGE),
                    cameraSetup = HIDDEN,
                    systemPickerEnabled = false,
                    editingEnabled = false,
                    queueResults = false,
                    defaultSearchView = true,
                    title = R.string.photo_picker_stock_media
            )
            val intent = MediaPickerActivity.buildIntent(
                    activity,
                    mediaPickerSetup,
                    site
            )
            activity.startActivityForResult(intent, requestCode)
        } else {
            ActivityLauncher.showStockMediaPickerForResult(activity, site, requestCode)
        }
    }

    fun showGifPickerForResult(
        activity: Activity,
        site: SiteModel,
        allowMultipleSelection: Boolean
    ) {
        val requestCode = if (allowMultipleSelection) {
            RequestCodes.GIF_PICKER_MULTI_SELECT
        } else {
            RequestCodes.GIF_PICKER_SINGLE_SELECT
        }
        if (consolidatedMediaPickerFeatureConfig.isEnabled()) {
            val mediaPickerSetup = MediaPickerSetup(
                    primaryDataSource = GIF_LIBRARY,
                    availableDataSources = setOf(),
                    canMultiselect = allowMultipleSelection,
                    requiresStoragePermissions = false,
                    allowedTypes = setOf(IMAGE),
                    cameraSetup = HIDDEN,
                    systemPickerEnabled = false,
                    editingEnabled = false,
                    queueResults = false,
                    defaultSearchView = true,
                    title = R.string.photo_picker_gif
            )
            val intent = MediaPickerActivity.buildIntent(
                    activity,
                    mediaPickerSetup,
                    site
            )
            activity.startActivityForResult(intent, requestCode)
        } else {
            ActivityLauncher.showGifPickerForResult(activity, site, requestCode)
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
                primaryDataSource = DEVICE,
                availableDataSources = if (browserType.isWPStoriesPicker) setOf(WP_LIBRARY) else setOf(),
                canMultiselect = browserType.canMultiselect(),
                requiresStoragePermissions = true,
                allowedTypes = allowedTypes,
                cameraSetup = if (browserType.isWPStoriesPicker) STORIES else HIDDEN,
                systemPickerEnabled = true,
                editingEnabled = browserType.isImagePicker,
                queueResults = browserType == FEATURED_IMAGE_PICKER,
                defaultSearchView = false,
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

        if (browserType.isAudioPicker) {
            allowedTypes.add(AUDIO)
        }

        if (browserType.isDocumentPicker) {
            allowedTypes.add(DOCUMENT)
        }

        return MediaPickerSetup(
                primaryDataSource = WP_LIBRARY,
                availableDataSources = setOf(),
                canMultiselect = browserType.canMultiselect(),
                requiresStoragePermissions = false,
                allowedTypes = allowedTypes,
                cameraSetup = if (browserType.isWPStoriesPicker) STORIES else HIDDEN,
                systemPickerEnabled = false,
                editingEnabled = false,
                queueResults = false,
                defaultSearchView = false,
                title = R.string.wp_media_title
        )
    }
}
