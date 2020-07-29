package org.wordpress.android.ui.photopicker

import android.Manifest.permission
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.text.Html
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.ActionMode.Callback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnFlingListener
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import kotlinx.android.synthetic.main.photo_picker_fragment.*
import org.wordpress.android.R
import org.wordpress.android.R.layout
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_CAPTURE_MEDIA
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_DEVICE_LIBRARY
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_WP_MEDIA
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_WP_STORIES_CAPTURE
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_RECENT_MEDIA_SELECTED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.media.MediaBrowserActivity
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.media.MediaBrowserType.AZTEC_EDITOR_PICKER
import org.wordpress.android.ui.media.MediaBrowserType.GRAVATAR_IMAGE_PICKER
import org.wordpress.android.ui.media.MediaBrowserType.SITE_ICON_PICKER
import org.wordpress.android.ui.photopicker.PhotoPickerAdapter.PhotoPickerAdapterListener
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon.ANDROID_CAPTURE_PHOTO
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon.ANDROID_CAPTURE_VIDEO
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon.ANDROID_CHOOSE_PHOTO
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon.ANDROID_CHOOSE_VIDEO
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon.GIF
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon.STOCK_MEDIA
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon.WP_MEDIA
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon.WP_STORIES_CAPTURE
import org.wordpress.android.util.AccessibilityUtils
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AniUtils.Duration.MEDIUM
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MEDIA
import org.wordpress.android.util.AppLog.T.POSTS
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.WPPermissionUtils
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.util.config.TenorFeatureConfig
import java.util.ArrayList
import java.util.HashMap
import javax.inject.Inject

class PhotoPickerFragment : Fragment() {
    enum class PhotoPickerIcon(private val mRequiresUploadPermission: Boolean) {
        ANDROID_CHOOSE_PHOTO(true),
        ANDROID_CHOOSE_VIDEO(true),
        ANDROID_CAPTURE_PHOTO(true),
        ANDROID_CAPTURE_VIDEO(true),
        ANDROID_CHOOSE_PHOTO_OR_VIDEO(true),
        WP_MEDIA(false),
        STOCK_MEDIA(true),
        GIF(true),
        WP_STORIES_CAPTURE(true);

        fun requiresUploadPermission(): Boolean {
            return mRequiresUploadPermission
        }
    }

    /*
     * parent activity must implement this listener
     */
    interface PhotoPickerListener {
        fun onPhotoPickerMediaChosen(uriList: List<Uri?>)
        fun onPhotoPickerIconClicked(icon: PhotoPickerIcon, allowMultipleSelection: Boolean)
    }

    private var mAdapter: PhotoPickerAdapter? = null
    private var mInsertEditBottomBar: View? = null
    private var mSoftAskView: ActionableEmptyView? = null
    private var mActionMode: ActionMode? = null
    private var mGridManager: GridLayoutManager? = null
    private var mRestoreState: Parcelable? = null
    private var mListener: PhotoPickerListener? = null
    private var mLastTappedIcon: PhotoPickerIcon? = null
    private var mBrowserType: MediaBrowserType? = null
    private var mSite: SiteModel? = null
    private var mSelectedPositions: ArrayList<Int>? = null

    @Inject private lateinit var mTenorFeatureConfig: TenorFeatureConfig

    @Inject private lateinit var mDeviceMediaListBuilder: DeviceMediaListBuilder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        mBrowserType = requireArguments().getSerializable(MediaBrowserActivity.ARG_BROWSER_TYPE) as MediaBrowserType
        mSite = requireArguments().getSerializable(WordPress.SITE) as SiteModel
        if (savedInstanceState != null) {
            val savedLastTappedIconName = savedInstanceState.getString(KEY_LAST_TAPPED_ICON)
            mLastTappedIcon = if (savedLastTappedIconName == null) null else PhotoPickerIcon.valueOf(
                    savedLastTappedIconName
            )
            if (savedInstanceState.containsKey(KEY_SELECTED_POSITIONS)) {
                mSelectedPositions = savedInstanceState.getIntegerArrayList(KEY_SELECTED_POSITIONS)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
                layout.photo_picker_fragment,
                container,
                false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (mBrowserType!!.isWPStoriesPicker) {
            wp_stories_take_picture.visibility = View.VISIBLE
            wp_stories_take_picture.setOnClickListener { doIconClicked(WP_STORIES_CAPTURE) }
        } else {
            wp_stories_take_picture.visibility = View.GONE
        }
        recycler.setEmptyView(view.findViewById(R.id.actionable_empty_view))
        recycler.setHasFixedSize(true)

        // disable thumbnail loading during a fling to conserve memory
        val minDistance = WPMediaUtils.getFlingDistanceToDisableThumbLoading(requireActivity())
        recycler.onFlingListener = object : OnFlingListener() {
            override fun onFling(velocityX: Int, velocityY: Int): Boolean {
                if (Math.abs(velocityY) > minDistance) {
                    adapter.setLoadThumbnails(false)
                }
                return false
            }
        }
        recycler.addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(
                recyclerView: RecyclerView,
                newState: Int
            ) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    adapter.setLoadThumbnails(true)
                }
            }
        })
        mInsertEditBottomBar = view.findViewById(R.id.container_insert_edit_bar)
        if (!canShowMediaSourceBottomBar()) {
            container_media_source_bar.visibility = View.GONE
        } else {
            val camera = container_media_source_bar.findViewById<View>(R.id.icon_camera)
            if (mBrowserType!!.isGutenbergPicker || mBrowserType!!.isWPStoriesPicker) {
                camera?.visibility = View.GONE
            } else {
                camera?.setOnClickListener { v ->
                    if (mBrowserType!!.isImagePicker && mBrowserType!!.isVideoPicker) {
                        showCameraPopupMenu(v)
                    } else if (mBrowserType!!.isImagePicker) {
                        doIconClicked(ANDROID_CAPTURE_PHOTO)
                    } else if (mBrowserType!!.isVideoPicker) {
                        doIconClicked(ANDROID_CAPTURE_VIDEO)
                    } else {
                        AppLog.e(
                                MEDIA,
                                "This code should be unreachable. If you see this message one of "
                                        + "the MediaBrowserTypes isn't setup correctly."
                        )
                    }
                }
            }
            container_media_source_bar.findViewById<View>(R.id.icon_picker)
                    ?.setOnClickListener { v ->
                        if (mBrowserType == GRAVATAR_IMAGE_PICKER
                                || mBrowserType == SITE_ICON_PICKER) {
                            doIconClicked(ANDROID_CHOOSE_PHOTO)
                        } else {
                            performActionOrShowPopup(v)
                        }
                    }

            // choosing from WP media requires a site and should be hidden in gutenberg picker
            val wpMedia = container_media_source_bar.findViewById<View>(R.id.icon_wpmedia)
            if (mSite == null || mBrowserType!!.isGutenbergPicker) {
                wpMedia?.visibility = View.GONE
            } else {
                wpMedia?.setOnClickListener { doIconClicked(WP_MEDIA) }
            }
        }
        if (canShowInsertEditBottomBar()) {
            mInsertEditBottomBar?.findViewById<View>(R.id.text_edit)
                    ?.setOnClickListener {
                        val inputData = WPMediaUtils.createListOfEditImageInputData(
                                requireContext(),
                                adapter.selectedURIs
                        )
                        ActivityLauncher.openImageEditor(activity, inputData)
                    }
            mInsertEditBottomBar?.findViewById<View>(R.id.text_insert)
                    ?.setOnClickListener { performInsertAction() }
        }
        mSoftAskView = view.findViewById(R.id.soft_ask_view)
    }

    private fun canShowMediaSourceBottomBar(): Boolean {
        if (mBrowserType == AZTEC_EDITOR_PICKER && DisplayUtils.isLandscape(
                        activity
                )) {
            return true
        } else if (mBrowserType == AZTEC_EDITOR_PICKER) {
            return false
        }
        return true
    }

    private fun canShowInsertEditBottomBar(): Boolean {
        return mBrowserType!!.isGutenbergPicker
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(
                KEY_LAST_TAPPED_ICON,
                if (mLastTappedIcon == null) null else mLastTappedIcon!!.name
        )
        if (hasAdapter() && adapter.numSelected > 0) {
            val selectedItems = adapter.selectedPositions
            outState.putIntegerArrayList(KEY_SELECTED_POSITIONS, selectedItems)
        }
    }

    override fun onResume() {
        super.onResume()
        checkStoragePermission()
    }

    fun doIconClicked(icon: PhotoPickerIcon) {
        mLastTappedIcon = icon
        if (icon == ANDROID_CAPTURE_PHOTO || icon == ANDROID_CAPTURE_VIDEO || icon == WP_STORIES_CAPTURE) {
            if (ContextCompat.checkSelfPermission(
                            requireActivity(), permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED || !hasStoragePermission()) {
                requestCameraPermission()
                return
            }
        }
        when (icon) {
            ANDROID_CAPTURE_PHOTO -> trackSelectedOtherSourceEvents(
                    MEDIA_PICKER_OPEN_CAPTURE_MEDIA,
                    false
            )
            ANDROID_CAPTURE_VIDEO -> trackSelectedOtherSourceEvents(
                    MEDIA_PICKER_OPEN_CAPTURE_MEDIA,
                    true
            )
            ANDROID_CHOOSE_PHOTO -> trackSelectedOtherSourceEvents(
                    MEDIA_PICKER_OPEN_DEVICE_LIBRARY,
                    false
            )
            ANDROID_CHOOSE_VIDEO -> trackSelectedOtherSourceEvents(
                    MEDIA_PICKER_OPEN_DEVICE_LIBRARY,
                    true
            )
            WP_MEDIA -> AnalyticsTracker.track(MEDIA_PICKER_OPEN_WP_MEDIA)
            STOCK_MEDIA -> {
            }
            GIF -> {
            }
            WP_STORIES_CAPTURE -> AnalyticsTracker.track(MEDIA_PICKER_OPEN_WP_STORIES_CAPTURE)
        }
        if (mListener != null) {
            mListener!!.onPhotoPickerIconClicked(icon, mBrowserType!!.canMultiselect())
        }
    }

    fun performActionOrShowPopup(view: View) {
        val popup = PopupMenu(activity, view)
        if (mBrowserType!!.isImagePicker) {
            val itemPhoto = popup.menu
                    .add(string.photo_picker_choose_photo)
            itemPhoto.setOnMenuItemClickListener {
                doIconClicked(ANDROID_CHOOSE_PHOTO)
                true
            }
        }
        if (mBrowserType!!.isVideoPicker) {
            val itemVideo = popup.menu
                    .add(string.photo_picker_choose_video)
            itemVideo.setOnMenuItemClickListener {
                doIconClicked(ANDROID_CHOOSE_VIDEO)
                true
            }
        }
        if (mSite != null && !mBrowserType!!.isGutenbergPicker) {
            val itemStock = popup.menu
                    .add(string.photo_picker_stock_media)
            itemStock.setOnMenuItemClickListener {
                doIconClicked(STOCK_MEDIA)
                true
            }

            // only show GIF picker from Tenor if this is NOT the WPStories picker
            if (mTenorFeatureConfig!!.isEnabled() && !mBrowserType!!.isWPStoriesPicker) {
                val itemGif = popup.menu
                        .add(string.photo_picker_gif)
                itemGif.setOnMenuItemClickListener { item: MenuItem? ->
                    doIconClicked(GIF)
                    true
                }
            }
        }

        // if the menu has a single item, perform the action right away
        if (popup.menu.size() == 1) {
            popup.menu.performIdentifierAction(popup.menu.getItem(0).itemId, 0)
        } else {
            popup.show()
        }
    }

    fun showCameraPopupMenu(view: View) {
        val popup = PopupMenu(activity, view)
        val itemPhoto = popup.menu
                .add(string.photo_picker_capture_photo)
        itemPhoto.setOnMenuItemClickListener {
            doIconClicked(ANDROID_CAPTURE_PHOTO)
            true
        }
        val itemVideo = popup.menu
                .add(string.photo_picker_capture_video)
        itemVideo.setOnMenuItemClickListener {
            doIconClicked(ANDROID_CAPTURE_VIDEO)
            true
        }
        popup.show()
    }

    fun setPhotoPickerListener(listener: PhotoPickerListener?) {
        mListener = listener
    }

    private fun showBottomBar(bottomBar: View?) {
        if (!isBottomBarShowing(bottomBar)) {
            AniUtils.animateBottomBar(bottomBar, true)
        }
    }

    private fun hideBottomBar(bottomBar: View?) {
        if (isBottomBarShowing(bottomBar)) {
            AniUtils.animateBottomBar(bottomBar, false)
        }
    }

    private fun isBottomBarShowing(bottomBar: View?): Boolean {
        return bottomBar!!.visibility == View.VISIBLE
    }

    private val mAdapterListener: PhotoPickerAdapterListener = object : PhotoPickerAdapterListener {
        override fun onSelectedCountChanged(count: Int) {
            if (count == 0) {
                finishActionMode()
            } else {
                val activity = activity ?: return
                if (canShowInsertEditBottomBar()) {
                    val editView = mInsertEditBottomBar!!.findViewById<TextView>(
                            R.id.text_edit
                    )
                    val isVideoFileSelected = adapter.isVideoFileSelected
                    editView.visibility = if (isVideoFileSelected) View.GONE else View.VISIBLE
                }
                if (mActionMode == null) {
                    (activity as AppCompatActivity).startSupportActionMode(ActionModeCallback())
                }
                updateActionModeTitle()
            }
        }

        override fun onAdapterLoaded(isEmpty: Boolean) {
            // restore previous selection
            if (mSelectedPositions != null) {
                adapter.selectedPositions = mSelectedPositions!!
                mSelectedPositions = null
            }
            // restore previous state
            if (mRestoreState != null) {
                mGridManager!!.onRestoreInstanceState(mRestoreState)
                mRestoreState = null
            }
        }
    }

    private fun hasAdapter(): Boolean {
        return mAdapter != null
    }

    private val adapter: PhotoPickerAdapter
        private get() {
            if (mAdapter == null) {
                mAdapter = PhotoPickerAdapter(activity, mBrowserType, mAdapterListener, mDeviceMediaListBuilder)
            }
            return mAdapter!!
        }

    /*
     * populates the adapter with media stored on the device
     */
    fun reload() {
        if (!isAdded) {
            AppLog.w(
                    POSTS,
                    "Photo picker > can't reload when not added"
            )
            return
        }
        if (!hasStoragePermission()) {
            return
        }

        // save the current state so we can restore it after loading
        if (mGridManager != null) {
            mRestoreState = mGridManager!!.onSaveInstanceState()
        }
        mGridManager = GridLayoutManager(
                activity,
                NUM_COLUMNS
        )
        recycler.layoutManager = mGridManager
        recycler.adapter = adapter
        adapter.refresh(true)
    }

    /*
     * similar to the above but only repopulates if changes are detected
     */
    fun refresh() {
        if (!isAdded) {
            AppLog.w(
                    POSTS,
                    "Photo picker > can't refresh when not added"
            )
            return
        }
        if (!hasStoragePermission()) {
            return
        }
        if (mGridManager == null || mAdapter == null) {
            reload()
        } else {
            adapter.refresh(false)
        }
    }

    fun finishActionMode() {
        if (mActionMode != null) {
            mActionMode!!.finish()
        }
    }

    private fun updateActionModeTitle() {
        if (mActionMode == null) {
            return
        }
        val title: String
        if (mBrowserType!!.canMultiselect()) {
            val numSelected = adapter.numSelected
            title = String.format(getString(string.cab_selected), numSelected)
            mActionMode!!.title = title
        } else {
            if (mBrowserType!!.isImagePicker && mBrowserType!!.isVideoPicker) {
                mActionMode!!.setTitle(string.photo_picker_use_media)
            } else if (mBrowserType!!.isVideoPicker) {
                mActionMode!!.setTitle(string.photo_picker_use_video)
            } else {
                mActionMode!!.setTitle(string.photo_picker_use_photo)
            }
        }
    }

    private inner class ActionModeCallback : Callback {
        override fun onCreateActionMode(
            actionMode: ActionMode,
            menu: Menu
        ): Boolean {
            mActionMode = actionMode
            if (canShowInsertEditBottomBar()) {
                showBottomBar(mInsertEditBottomBar)
            } else {
                val inflater = actionMode.menuInflater
                inflater.inflate(R.menu.photo_picker_action_mode, menu)
            }
            hideBottomBar(container_media_source_bar)
            return true
        }

        override fun onPrepareActionMode(
            mode: ActionMode,
            menu: Menu
        ): Boolean {
            AccessibilityUtils.setActionModeDoneButtonContentDescription(
                    activity,
                    getString(string.cancel)
            )
            return false
        }

        override fun onActionItemClicked(
            mode: ActionMode,
            item: MenuItem
        ): Boolean {
            if (item.itemId == R.id.mnu_confirm_selection && mListener != null) {
                performInsertAction()
                return true
            }
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            mActionMode = null
            if (canShowMediaSourceBottomBar()) {
                showBottomBar(container_media_source_bar)
            }
            hideBottomBar(mInsertEditBottomBar)
            adapter.clearSelection()
        }
    }

    private fun performInsertAction() {
        val uriList = adapter.selectedURIs
        mListener!!.onPhotoPickerMediaChosen(uriList)
        trackAddRecentMediaEvent(uriList)
    }

    private fun hasStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
                requireActivity(), permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private val isStoragePermissionAlwaysDenied: Boolean
        private get() = WPPermissionUtils.isPermissionAlwaysDenied(
                requireActivity(), permission.WRITE_EXTERNAL_STORAGE
        )

    /*
     * load the photos if we have the necessary permission, otherwise show the "soft ask" view
     * which asks the user to allow the permission
     */
    private fun checkStoragePermission() {
        if (!isAdded) {
            return
        }
        if (hasStoragePermission()) {
            showSoftAskView(false)
            if (!hasAdapter()) {
                reload()
            }
        } else {
            showSoftAskView(true)
        }
    }

    private fun requestStoragePermission() {
        val permissions = arrayOf(permission.WRITE_EXTERNAL_STORAGE)
        requestPermissions(
                permissions, WPPermissionUtils.PHOTO_PICKER_STORAGE_PERMISSION_REQUEST_CODE
        )
    }

    private fun requestCameraPermission() {
        // in addition to CAMERA permission we also need a storage permission, to store media from the camera
        val permissions = arrayOf(
                permission.CAMERA,
                permission.WRITE_EXTERNAL_STORAGE
        )
        requestPermissions(permissions, WPPermissionUtils.PHOTO_PICKER_CAMERA_PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        val checkForAlwaysDenied = requestCode == WPPermissionUtils.PHOTO_PICKER_CAMERA_PERMISSION_REQUEST_CODE
        val allGranted = WPPermissionUtils.setPermissionListAsked(
                requireActivity(), requestCode, permissions, grantResults, checkForAlwaysDenied
        )
        when (requestCode) {
            WPPermissionUtils.PHOTO_PICKER_STORAGE_PERMISSION_REQUEST_CODE -> checkStoragePermission()
            WPPermissionUtils.PHOTO_PICKER_CAMERA_PERMISSION_REQUEST_CODE -> if (allGranted) {
                doIconClicked(mLastTappedIcon!!)
            }
        }
    }

    /*
     * shows the "soft ask" view which should appear when storage permission hasn't been granted
     */
    private fun showSoftAskView(show: Boolean) {
        if (!isAdded) {
            return
        }
        val isAlwaysDenied = isStoragePermissionAlwaysDenied
        if (show) {
            val appName = "<strong>" + getString(string.app_name) + "</strong>"
            val label: String
            label = if (isAlwaysDenied) {
                val permissionName = ("<strong>"
                        + WPPermissionUtils.getPermissionName(
                        requireActivity(),
                        permission.WRITE_EXTERNAL_STORAGE
                )
                        + "</strong>")
                String.format(
                        getString(string.photo_picker_soft_ask_permissions_denied), appName,
                        permissionName
                )
            } else {
                String.format(
                        getString(string.photo_picker_soft_ask_label),
                        appName
                )
            }
            mSoftAskView!!.title.text = Html.fromHtml(label)

            // when the user taps Allow, request the required permissions unless the user already
            // denied them permanently, in which case take them to the device settings for this
            // app so the user can change permissions there
            val allowId = if (isAlwaysDenied) string.button_edit_permissions else string.photo_picker_soft_ask_allow
            mSoftAskView!!.button.setText(allowId)
            mSoftAskView!!.button.setOnClickListener {
                if (isStoragePermissionAlwaysDenied) {
                    WPPermissionUtils.showAppSettings(requireActivity())
                } else {
                    requestStoragePermission()
                }
            }
            mSoftAskView!!.visibility = View.VISIBLE
            hideBottomBar(container_media_source_bar)
        } else if (mSoftAskView!!.visibility == View.VISIBLE) {
            AniUtils.fadeOut(mSoftAskView, MEDIUM)
            if (canShowMediaSourceBottomBar()) {
                showBottomBar(container_media_source_bar)
            }
        }
    }

    private fun trackAddRecentMediaEvent(uriList: List<Uri?>?) {
        if (uriList == null) {
            AppLog.e(
                    MEDIA,
                    "Cannot track new media events if uriList is null!!"
            )
            return
        }
        val isMultiselection = uriList.size > 1
        for (mediaUri in uriList) {
            if (mediaUri != null) {
                val isVideo = MediaUtils.isVideo(mediaUri.toString())
                val properties = AnalyticsUtils.getMediaProperties(
                        activity,
                        isVideo,
                        mediaUri,
                        null
                )
                properties["is_part_of_multiselection"] = isMultiselection
                if (isMultiselection) {
                    properties["number_of_media_selected"] = uriList.size
                }
                AnalyticsTracker.track(MEDIA_PICKER_RECENT_MEDIA_SELECTED, properties)
            }
        }
    }

    private fun trackSelectedOtherSourceEvents(stat: Stat, isVideo: Boolean) {
        val properties: MutableMap<String, Any?> = HashMap()
        properties["is_video"] = isVideo
        AnalyticsTracker.track(stat, properties)
    }

    companion object {
        private const val KEY_LAST_TAPPED_ICON = "last_tapped_icon"
        private const val KEY_SELECTED_POSITIONS = "selected_positions"
        const val NUM_COLUMNS = 3
        @JvmStatic fun newInstance(
            listener: PhotoPickerListener,
            browserType: MediaBrowserType,
            site: SiteModel?
        ): PhotoPickerFragment {
            val args = Bundle()
            args.putSerializable(MediaBrowserActivity.ARG_BROWSER_TYPE, browserType)
            if (site != null) {
                args.putSerializable(WordPress.SITE, site)
            }
            val fragment = PhotoPickerFragment()
            fragment.setPhotoPickerListener(listener)
            fragment.arguments = args
            return fragment
        }
    }
}
