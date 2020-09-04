package org.wordpress.android.ui.mediapicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.FragmentTransaction
import kotlinx.android.synthetic.main.toolbar_main.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.imageeditor.preview.PreviewImageFragment
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.RequestCodes.IMAGE_EDITOR_EDIT_IMAGE
import org.wordpress.android.ui.RequestCodes.MULTI_SELECT_MEDIA_PICKER
import org.wordpress.android.ui.RequestCodes.PICTURE_LIBRARY
import org.wordpress.android.ui.RequestCodes.SINGLE_SELECT_MEDIA_PICKER
import org.wordpress.android.ui.RequestCodes.STOCK_MEDIA_PICKER_SINGLE_SELECT
import org.wordpress.android.ui.RequestCodes.TAKE_PHOTO
import org.wordpress.android.ui.RequestCodes.VIDEO_LIBRARY
import org.wordpress.android.ui.media.MediaBrowserActivity
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.media.MediaBrowserType.FEATURED_IMAGE_PICKER
import org.wordpress.android.ui.media.MediaBrowserType.WP_STORIES_MEDIA_PICKER
import org.wordpress.android.ui.mediapicker.MediaPickerActivity.MediaPickerMediaSource.ANDROID_CAMERA
import org.wordpress.android.ui.mediapicker.MediaPickerActivity.MediaPickerMediaSource.ANDROID_PICKER
import org.wordpress.android.ui.mediapicker.MediaPickerActivity.MediaPickerMediaSource.APP_PICKER
import org.wordpress.android.ui.mediapicker.MediaPickerActivity.MediaPickerMediaSource.STOCK_MEDIA_PICKER
import org.wordpress.android.ui.mediapicker.MediaPickerActivity.MediaPickerMediaSource.WP_MEDIA_PICKER
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.Companion.newInstance
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerIcon
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerIcon.WP_STORIES_CAPTURE
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerListener
import org.wordpress.android.ui.photopicker.MediaPickerConstants.EXTRA_LAUNCH_WPSTORIES_CAMERA_REQUESTED
import org.wordpress.android.ui.photopicker.MediaPickerConstants.EXTRA_MEDIA_ID
import org.wordpress.android.ui.photopicker.MediaPickerConstants.EXTRA_MEDIA_QUEUED
import org.wordpress.android.ui.photopicker.MediaPickerConstants.EXTRA_MEDIA_SOURCE
import org.wordpress.android.ui.photopicker.MediaPickerConstants.EXTRA_MEDIA_URIS
import org.wordpress.android.ui.photopicker.MediaPickerConstants.LOCAL_POST_ID
import org.wordpress.android.ui.posts.EMPTY_LOCAL_POST_ID
import org.wordpress.android.ui.posts.FeaturedImageHelper
import org.wordpress.android.ui.posts.FeaturedImageHelper.EnqueueFeaturedImageResult.FILE_NOT_FOUND
import org.wordpress.android.ui.posts.FeaturedImageHelper.EnqueueFeaturedImageResult.INVALID_POST_ID
import org.wordpress.android.ui.posts.FeaturedImageHelper.EnqueueFeaturedImageResult.SUCCESS
import org.wordpress.android.ui.posts.FeaturedImageHelper.TrackableEvent.IMAGE_PICKED
import org.wordpress.android.ui.posts.editor.ImageEditorTracker
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MEDIA
import org.wordpress.android.util.ListUtils
import org.wordpress.android.util.WPMediaUtils
import java.io.File
import java.util.ArrayList
import javax.inject.Inject

class MediaPickerActivity : LocaleAwareActivity(), MediaPickerListener {
    private var mediaCapturePath: String? = null
    private lateinit var mediaPickerSetup: MediaPickerSetup
    private lateinit var browserType: MediaBrowserType

    // note that the site isn't required and may be null
    private var site: SiteModel? = null

    // note that the local post id isn't required (default value is EMPTY_LOCAL_POST_ID)
    private var localPostId: Int = EMPTY_LOCAL_POST_ID

    @Inject lateinit var dispatcher: Dispatcher

    @Inject lateinit var mediaStore: MediaStore

    @Inject lateinit var featuredImageHelper: FeaturedImageHelper

    @Inject lateinit var imageEditorTracker: ImageEditorTracker

    enum class MediaPickerMediaSource {
        ANDROID_CAMERA, ANDROID_PICKER, APP_PICKER, WP_MEDIA_PICKER, STOCK_MEDIA_PICKER;

        companion object {
            fun fromString(strSource: String?): MediaPickerMediaSource? {
                if (strSource != null) {
                    for (source in values()) {
                        if (source.name.equals(strSource, ignoreCase = true)) {
                            return source
                        }
                    }
                }
                return null
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        setContentView(R.layout.photo_picker_activity)
        toolbar_main.setNavigationIcon(R.drawable.ic_close_white_24dp)
        setSupportActionBar(toolbar_main)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowTitleEnabled(true)
        }
        if (savedInstanceState == null) {
            browserType = intent.getSerializableExtra(MediaBrowserActivity.ARG_BROWSER_TYPE) as MediaBrowserType
            mediaPickerSetup = MediaPickerSetup.fromIntent(intent)
            site = intent.getSerializableExtra(WordPress.SITE) as? SiteModel
            localPostId = intent.getIntExtra(LOCAL_POST_ID, EMPTY_LOCAL_POST_ID)
        } else {
            browserType = savedInstanceState.getSerializable(MediaBrowserActivity.ARG_BROWSER_TYPE) as MediaBrowserType
            mediaPickerSetup = MediaPickerSetup.fromBundle(savedInstanceState)
            site = savedInstanceState.getSerializable(WordPress.SITE) as? SiteModel
            localPostId = savedInstanceState.getInt(LOCAL_POST_ID, EMPTY_LOCAL_POST_ID)
        }
        var fragment = pickerFragment
        if (fragment == null) {
            fragment = newInstance(this, mediaPickerSetup, site)
            supportFragmentManager.beginTransaction()
                    .replace(
                            R.id.fragment_container,
                            fragment,
                            PICKER_FRAGMENT_TAG
                    )
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commitAllowingStateLoss()
        } else {
            fragment.setMediaPickerListener(this)
        }
        updateTitle(mediaPickerSetup, requireNotNull(actionBar))
    }

    private fun updateTitle(mediaPickerSetup: MediaPickerSetup, actionBar: ActionBar) {
        val isImagePicker = mediaPickerSetup.allowedTypes.contains(MediaType.IMAGE)
        val isVideoPicker = mediaPickerSetup.allowedTypes.contains(MediaType.VIDEO)
        if (isImagePicker && isVideoPicker) {
            actionBar.setTitle(R.string.photo_picker_photo_or_video_title)
        } else if (isVideoPicker) {
            actionBar.setTitle(R.string.photo_picker_video_title)
        } else {
            actionBar.setTitle(R.string.photo_picker_title)
        }
    }

    private val pickerFragment: MediaPickerFragment?
        get() {
            val fragment = supportFragmentManager.findFragmentByTag(
                    PICKER_FRAGMENT_TAG
            )
            return if (fragment != null) {
                fragment as MediaPickerFragment
            } else null
        }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(MediaBrowserActivity.ARG_BROWSER_TYPE, browserType)
        mediaPickerSetup.toBundle(outState)
        outState.putInt(LOCAL_POST_ID, localPostId)
        if (site != null) {
            outState.putSerializable(WordPress.SITE, site)
        }
        if (!TextUtils.isEmpty(mediaCapturePath)) {
            outState.putString(KEY_MEDIA_CAPTURE_PATH, mediaCapturePath)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mediaCapturePath = savedInstanceState.getString(KEY_MEDIA_CAPTURE_PATH)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        when (requestCode) {
            PICTURE_LIBRARY, VIDEO_LIBRARY -> if (data != null) {
                doMediaUrisSelected(WPMediaUtils.retrieveMediaUris(data), ANDROID_PICKER)
            }
            TAKE_PHOTO -> try {
                WPMediaUtils.scanMediaFile(this, mediaCapturePath!!)
                val f = File(mediaCapturePath)
                val capturedImageUri = listOf(
                        Uri.fromFile(
                                f
                        )
                )
                doMediaUrisSelected(capturedImageUri, ANDROID_CAMERA)
            } catch (e: RuntimeException) {
                AppLog.e(MEDIA, e)
            }
            MULTI_SELECT_MEDIA_PICKER, SINGLE_SELECT_MEDIA_PICKER -> if (data!!.hasExtra(
                            MediaBrowserActivity.RESULT_IDS
                    )) {
                val ids = ListUtils.fromLongArray(
                        data.getLongArrayExtra(
                                MediaBrowserActivity.RESULT_IDS
                        )
                )
                doMediaIdsSelected(ids, WP_MEDIA_PICKER)
            }
            STOCK_MEDIA_PICKER_SINGLE_SELECT -> if (data != null && data.hasExtra(EXTRA_MEDIA_ID)) {
                val mediaId = data.getLongExtra(EXTRA_MEDIA_ID, 0)
                val ids = ArrayList<Long>()
                ids.add(mediaId)
                doMediaIdsSelected(ids, STOCK_MEDIA_PICKER)
            }
            IMAGE_EDITOR_EDIT_IMAGE -> if (data != null && data.hasExtra(PreviewImageFragment.ARG_EDIT_IMAGE_DATA)) {
                val uris = WPMediaUtils.retrieveImageEditorResult(data)
                doMediaUrisSelected(uris, APP_PICKER)
            }
        }
    }

    private fun launchWPStoriesCamera() {
        val intent = Intent()
                .putExtra(EXTRA_LAUNCH_WPSTORIES_CAMERA_REQUESTED, true)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun doMediaUrisSelected(
        mediaUris: List<Uri>,
        source: MediaPickerMediaSource
    ) {
        // if user chose a featured image, we need to upload it and return the uploaded media object
        if (browserType == FEATURED_IMAGE_PICKER) {
            val mediaUri = mediaUris[0]
            val mimeType = contentResolver.getType(mediaUri)
            featuredImageHelper.trackFeaturedImageEvent(
                    IMAGE_PICKED,
                    localPostId
            )
            WPMediaUtils.fetchMediaAndDoNext(
                    this, mediaUri
            ) { uri ->
                val queueImageResult = featuredImageHelper
                        .queueFeaturedImageForUpload(
                                localPostId, site!!, uri,
                                mimeType
                        )
                when (queueImageResult) {
                    FILE_NOT_FOUND -> Toast.makeText(
                            applicationContext,
                            R.string.file_not_found, Toast.LENGTH_SHORT
                    )
                            .show()
                    INVALID_POST_ID -> Toast.makeText(
                            applicationContext,
                            R.string.error_generic, Toast.LENGTH_SHORT
                    )
                            .show()
                    SUCCESS -> {
                    }
                }
                val intent = Intent()
                        .putExtra(EXTRA_MEDIA_QUEUED, true)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        } else {
            val intent = Intent()
                    .putExtra(EXTRA_MEDIA_URIS, convertUrisListToStringArray(mediaUris))
                    .putExtra(
                            EXTRA_MEDIA_SOURCE,
                            source.name
                    ) // set the browserType in the result, so caller can distinguish and handle things as needed
                    .putExtra(MediaBrowserActivity.ARG_BROWSER_TYPE, browserType)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private fun doMediaIdsSelected(
        mediaIds: ArrayList<Long>?,
        source: MediaPickerMediaSource
    ) {
        if (mediaIds != null && mediaIds.size > 0) {
            if (browserType == WP_STORIES_MEDIA_PICKER) {
                // TODO WPSTORIES add TRACKS (see how it's tracked below? maybe do along the same lines)
                val data = Intent()
                        .putExtra(
                                MediaBrowserActivity.RESULT_IDS,
                                ListUtils.toLongArray(mediaIds)
                        )
                        .putExtra(MediaBrowserActivity.ARG_BROWSER_TYPE, browserType)
                        .putExtra(EXTRA_MEDIA_SOURCE, source.name)
                setResult(Activity.RESULT_OK, data)
                finish()
            } else {
                // if user chose a featured image, track image picked event
                if (browserType == FEATURED_IMAGE_PICKER) {
                    featuredImageHelper.trackFeaturedImageEvent(
                            IMAGE_PICKED,
                            localPostId
                    )
                }
                val data = Intent()
                        .putExtra(EXTRA_MEDIA_ID, mediaIds[0])
                        .putExtra(EXTRA_MEDIA_SOURCE, source.name)
                setResult(Activity.RESULT_OK, data)
                finish()
            }
        } else {
            throw IllegalArgumentException("call to doMediaIdsSelected with null or empty mediaIds array")
        }
    }

    override fun onMediaChosen(uriList: List<Uri>) {
        if (uriList.isNotEmpty()) {
            doMediaUrisSelected(uriList, APP_PICKER)
        }
    }

    override fun onIconClicked(icon: MediaPickerIcon, allowMultipleSelection: Boolean) {
        when (icon) {
            WP_STORIES_CAPTURE -> launchWPStoriesCamera()
        }
    }

    private fun convertUrisListToStringArray(uris: List<Uri>): Array<String?> {
        val stringUris = arrayOfNulls<String>(uris.size)
        for (i in uris.indices) {
            stringUris[i] = uris[i].toString()
        }
        return stringUris
    }

    companion object {
        private const val PICKER_FRAGMENT_TAG = "picker_fragment_tag"
        private const val KEY_MEDIA_CAPTURE_PATH = "media_capture_path"

        fun buildIntent(
            context: Context,
            browserType: MediaBrowserType,
            mediaPickerSetup: MediaPickerSetup,
            site: SiteModel? = null,
            localPostId: Int? = null
        ): Intent {
            val intent = Intent(context, MediaPickerActivity::class.java)
            intent.putExtra(MediaBrowserActivity.ARG_BROWSER_TYPE, browserType)
            mediaPickerSetup.toIntent(intent)
            if (site != null) {
                intent.putExtra(WordPress.SITE, site)
            }
            if (localPostId != null) {
                intent.putExtra(LOCAL_POST_ID, localPostId)
            }
            return intent
        }
    }
}
