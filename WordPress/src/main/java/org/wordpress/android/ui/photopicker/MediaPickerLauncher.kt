package org.wordpress.android.ui.photopicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.media.MediaBrowserActivity
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.photopicker.mediapicker.MediaPickerActivity
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
            val intent = createShowPhotoPickerIntent(
                    activity,
                    browserType,
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
            val intent = createShowPhotoPickerIntent(
                    fragment.requireContext(),
                    browserType,
                    site,
                    localPostId
            )
            fragment.startActivityForResult(intent, RequestCodes.PHOTO_PICKER)
        } else {
            ActivityLauncher.showPhotoPickerForResult(fragment, browserType, site, localPostId)
        }
    }

    private fun createShowPhotoPickerIntent(
        context: Context,
        browserType: MediaBrowserType,
        site: SiteModel?,
        localPostId: Int?
    ): Intent? {
        val intent = Intent(context, MediaPickerActivity::class.java)
        intent.putExtra(MediaBrowserActivity.ARG_BROWSER_TYPE, browserType)
        if (site != null) {
            intent.putExtra(WordPress.SITE, site)
        }
        if (localPostId != null) {
            intent.putExtra(MediaPickerActivity.LOCAL_POST_ID, localPostId.toInt())
        }
        return intent
    }
}
