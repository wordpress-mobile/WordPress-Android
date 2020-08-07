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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.ActionMode.Callback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnFlingListener
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import kotlinx.android.synthetic.main.photo_picker_fragment.*
import kotlinx.android.synthetic.main.photo_picker_fragment.view.*
import kotlinx.android.synthetic.main.stats_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_CAPTURE_MEDIA
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_DEVICE_LIBRARY
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_WP_MEDIA
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_WP_STORIES_CAPTURE
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_RECENT_MEDIA_SELECTED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.media.MediaBrowserActivity
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.media.MediaBrowserType.AZTEC_EDITOR_PICKER
import org.wordpress.android.ui.media.MediaBrowserType.GRAVATAR_IMAGE_PICKER
import org.wordpress.android.ui.media.MediaBrowserType.SITE_ICON_PICKER
import org.wordpress.android.ui.media.MediaPreviewActivity
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
import org.wordpress.android.util.image.ImageManager
import java.util.HashMap
import javax.inject.Inject
import kotlin.math.abs

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
        fun onPhotoPickerMediaChosen(uriList: List<Uri>)
        fun onPhotoPickerIconClicked(icon: PhotoPickerIcon, allowMultipleSelection: Boolean)
    }

    private var actionMode: ActionMode? = null
    private var listener: PhotoPickerListener? = null
    private var lastTappedIcon: PhotoPickerIcon? = null
    private var site: SiteModel? = null
    private lateinit var browserType: MediaBrowserType

    @Inject lateinit var tenorFeatureConfig: TenorFeatureConfig
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PhotoPickerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(PhotoPickerViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
                R.layout.photo_picker_fragment,
                container,
                false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        browserType = requireArguments().getSerializable(MediaBrowserActivity.ARG_BROWSER_TYPE) as MediaBrowserType
        site = requireArguments().getSerializable(WordPress.SITE) as SiteModel
        var selectedIds: List<Long>? = null
        if (savedInstanceState != null) {
            val savedLastTappedIconName = savedInstanceState.getString(KEY_LAST_TAPPED_ICON)
            lastTappedIcon = savedLastTappedIconName?.let { PhotoPickerIcon.valueOf(it) }
            if (savedInstanceState.containsKey(KEY_SELECTED_POSITIONS)) {
                selectedIds = savedInstanceState.getLongArray(KEY_SELECTED_POSITIONS)?.toList()
            }
        }

        if (browserType.isWPStoriesPicker) {
            wp_stories_take_picture.visibility = View.VISIBLE
            wp_stories_take_picture.setOnClickListener { doIconClicked(WP_STORIES_CAPTURE) }
        } else {
            wp_stories_take_picture.visibility = View.GONE
        }
        recycler.setEmptyView(actionable_empty_view)
        recycler.setHasFixedSize(true)

        // disable thumbnail loading during a fling to conserve memory
        val minDistance = WPMediaUtils.getFlingDistanceToDisableThumbLoading(requireActivity())
        recycler.onFlingListener = object : OnFlingListener() {
            override fun onFling(velocityX: Int, velocityY: Int): Boolean {
                if (abs(velocityY) > minDistance) {
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
        if (!canShowMediaSourceBottomBar()) {
            container_media_source_bar.visibility = View.GONE
        } else {
            if (browserType.isGutenbergPicker || browserType.isWPStoriesPicker) {
                container_media_source_bar.icon_camera.visibility = View.GONE
            } else {
                container_media_source_bar.icon_camera.setOnClickListener { v ->
                    if (browserType.isImagePicker && browserType.isVideoPicker) {
                        showCameraPopupMenu(v)
                    } else if (browserType.isImagePicker) {
                        doIconClicked(ANDROID_CAPTURE_PHOTO)
                    } else if (browserType.isVideoPicker) {
                        doIconClicked(ANDROID_CAPTURE_VIDEO)
                    } else {
                        AppLog.e(
                                MEDIA,
                                "This code should be unreachable. If you see this message one of " +
                                        "the MediaBrowserTypes isn't setup correctly."
                        )
                    }
                }
            }
            container_media_source_bar.icon_picker
                    ?.setOnClickListener { v ->
                        if (browserType == GRAVATAR_IMAGE_PICKER || browserType == SITE_ICON_PICKER) {
                            doIconClicked(ANDROID_CHOOSE_PHOTO)
                        } else {
                            performActionOrShowPopup(v)
                        }
                    }

            // choosing from WP media requires a site and should be hidden in gutenberg picker
            if (site == null || browserType.isGutenbergPicker) {
                container_media_source_bar.icon_wpmedia.visibility = View.GONE
            } else {
                container_media_source_bar.icon_wpmedia.setOnClickListener { doIconClicked(WP_MEDIA) }
            }
        }
        if (canShowInsertEditBottomBar()) {
            container_insert_edit_bar.text_edit
                    .setOnClickListener {
                        val inputData = WPMediaUtils.createListOfEditImageInputData(
                                requireContext(),
                                viewModel.selectedURIs().map { it.uri }
                        )
                        ActivityLauncher.openImageEditor(activity, inputData)
                    }
            container_insert_edit_bar.text_insert
                    .setOnClickListener { performInsertAction() }
        }

        val layoutManager = GridLayoutManager(
                activity,
                NUM_COLUMNS
        )

        savedInstanceState?.getParcelable<Parcelable>(KEY_LIST_STATE)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        recycler.layoutManager = layoutManager

        viewModel.data.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                if (recycler.adapter == null) {
                    recycler.adapter = PhotoPickerAdapter(
                            imageManager
                    )
                }
                val adapter = recycler.adapter as PhotoPickerAdapter
                val recyclerViewState = recyclerView?.layoutManager?.onSaveInstanceState()
                adapter.loadData(it.items)
                recyclerView?.layoutManager?.onRestoreInstanceState(recyclerViewState)
                if (it.count == 0) {
                    finishActionMode()
                } else {
                    val activity = activity ?: return@Observer
                    if (canShowInsertEditBottomBar()) {
                        val isVideoFileSelected = it.isVideoSelected
                        val editTextVisible = if (isVideoFileSelected) View.GONE else View.VISIBLE
                        container_insert_edit_bar.text_edit.visibility = editTextVisible
                    }
                    if (actionMode == null) {
                        (activity as AppCompatActivity).startSupportActionMode(ActionModeCallback())
                    }
                    updateActionModeTitle()
                }
            }
        })

        viewModel.navigateToPreview.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { uri ->
                MediaPreviewActivity.showPreview(
                        requireContext(),
                        null,
                        uri.toString()
                )
            }
        })

        viewModel.start(selectedIds, browserType)
    }

    private fun canShowMediaSourceBottomBar(): Boolean {
        if (browserType == AZTEC_EDITOR_PICKER && DisplayUtils.isLandscape(
                        activity
                )) {
            return true
        } else if (browserType == AZTEC_EDITOR_PICKER) {
            return false
        }
        return true
    }

    private fun canShowInsertEditBottomBar(): Boolean {
        return browserType.isGutenbergPicker
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(
                KEY_LAST_TAPPED_ICON,
                lastTappedIcon?.name
        )
        val selectedIds = viewModel.selectedIds.value
        if (selectedIds != null && selectedIds.isNotEmpty()) {
            outState.putLongArray(KEY_SELECTED_POSITIONS, selectedIds.toLongArray())
        }
        recycler.layoutManager?.let {
            outState.putParcelable(KEY_LIST_STATE, it.onSaveInstanceState())
        }
    }

    override fun onResume() {
        super.onResume()
        checkStoragePermission()
    }

    fun doIconClicked(icon: PhotoPickerIcon) {
        lastTappedIcon = icon
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
        listener?.onPhotoPickerIconClicked(icon, browserType.canMultiselect())
    }

    fun performActionOrShowPopup(view: View) {
        val popup = PopupMenu(activity, view)
        if (browserType.isImagePicker) {
            val itemPhoto = popup.menu
                    .add(R.string.photo_picker_choose_photo)
            itemPhoto.setOnMenuItemClickListener {
                doIconClicked(ANDROID_CHOOSE_PHOTO)
                true
            }
        }
        if (browserType.isVideoPicker) {
            val itemVideo = popup.menu
                    .add(R.string.photo_picker_choose_video)
            itemVideo.setOnMenuItemClickListener {
                doIconClicked(ANDROID_CHOOSE_VIDEO)
                true
            }
        }
        if (site != null && !browserType.isGutenbergPicker) {
            val itemStock = popup.menu
                    .add(R.string.photo_picker_stock_media)
            itemStock.setOnMenuItemClickListener {
                doIconClicked(STOCK_MEDIA)
                true
            }

            // only show GIF picker from Tenor if this is NOT the WPStories picker
            if (tenorFeatureConfig.isEnabled() && !browserType.isWPStoriesPicker) {
                val itemGif = popup.menu
                        .add(R.string.photo_picker_gif)
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
                .add(R.string.photo_picker_capture_photo)
        itemPhoto.setOnMenuItemClickListener {
            doIconClicked(ANDROID_CAPTURE_PHOTO)
            true
        }
        val itemVideo = popup.menu
                .add(R.string.photo_picker_capture_video)
        itemVideo.setOnMenuItemClickListener {
            doIconClicked(ANDROID_CAPTURE_VIDEO)
            true
        }
        popup.show()
    }

    fun setPhotoPickerListener(listener: PhotoPickerListener?) {
        this.listener = listener
    }

    private fun showBottomBar(bottomBar: View) {
        if (!isBottomBarShowing(bottomBar)) {
            AniUtils.animateBottomBar(bottomBar, true)
        }
    }

    private fun hideBottomBar(bottomBar: View) {
        if (isBottomBarShowing(bottomBar)) {
            AniUtils.animateBottomBar(bottomBar, false)
        }
    }

    private fun isBottomBarShowing(bottomBar: View): Boolean {
        return bottomBar.visibility == View.VISIBLE
    }

    private fun hasAdapter(): Boolean {
        return recycler.adapter != null
    }

    private val adapter: PhotoPickerAdapter
        get() {
            if (recycler.adapter == null) {
                recycler.adapter = PhotoPickerAdapter(
                        imageManager
                )
            }
            return recycler.adapter as PhotoPickerAdapter
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
        viewModel.refreshData(browserType, false)
    }

    fun finishActionMode() {
        actionMode?.finish()
    }

    private fun updateActionModeTitle() {
        actionMode?.let { actionMode ->
            val title: String
            if (browserType.canMultiselect()) {
                val numSelected = viewModel.numSelected()
                title = String.format(getString(R.string.cab_selected), numSelected)
                actionMode.title = title
            } else {
                if (browserType.isImagePicker && browserType.isVideoPicker) {
                    actionMode.setTitle(R.string.photo_picker_use_media)
                } else if (browserType.isVideoPicker) {
                    actionMode.setTitle(R.string.photo_picker_use_video)
                } else {
                    actionMode.setTitle(R.string.photo_picker_use_photo)
                }
            }
        }
    }

    private inner class ActionModeCallback : Callback {
        override fun onCreateActionMode(
            actionMode: ActionMode,
            menu: Menu
        ): Boolean {
            this@PhotoPickerFragment.actionMode = actionMode
            if (canShowInsertEditBottomBar()) {
                showBottomBar(container_insert_edit_bar)
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
                    getString(R.string.cancel)
            )
            return false
        }

        override fun onActionItemClicked(
            mode: ActionMode,
            item: MenuItem
        ): Boolean {
            if (item.itemId == R.id.mnu_confirm_selection && listener != null) {
                performInsertAction()
                return true
            }
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null
            if (canShowMediaSourceBottomBar()) {
                showBottomBar(container_media_source_bar)
            }
            hideBottomBar(container_insert_edit_bar)
            viewModel.clearSelection()
        }
    }

    private fun performInsertAction() {
        val uriList = viewModel.selectedURIs().map { it.uri }
        listener!!.onPhotoPickerMediaChosen(uriList)
        trackAddRecentMediaEvent(uriList)
    }

    private fun hasStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
                requireActivity(), permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private val isStoragePermissionAlwaysDenied: Boolean
        get() = WPPermissionUtils.isPermissionAlwaysDenied(
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
                refresh()
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
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        val checkForAlwaysDenied = requestCode == WPPermissionUtils.PHOTO_PICKER_CAMERA_PERMISSION_REQUEST_CODE
        val allGranted = WPPermissionUtils.setPermissionListAsked(
                requireActivity(), requestCode, permissions, grantResults, checkForAlwaysDenied
        )
        when (requestCode) {
            WPPermissionUtils.PHOTO_PICKER_STORAGE_PERMISSION_REQUEST_CODE -> checkStoragePermission()
            WPPermissionUtils.PHOTO_PICKER_CAMERA_PERMISSION_REQUEST_CODE -> if (allGranted) {
                doIconClicked(lastTappedIcon!!)
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
            val appName = "<strong>${getString(R.string.app_name)}</strong>"
            val label: String
            label = if (isAlwaysDenied) {
                val permissionName = ("<strong>${WPPermissionUtils.getPermissionName(
                        requireActivity(),
                        permission.WRITE_EXTERNAL_STORAGE
                )}</strong>")
                String.format(
                        getString(R.string.photo_picker_soft_ask_permissions_denied), appName,
                        permissionName
                )
            } else {
                String.format(
                        getString(R.string.photo_picker_soft_ask_label),
                        appName
                )
            }
            soft_ask_view.title.text = Html.fromHtml(label)

            // when the user taps Allow, request the required permissions unless the user already
            // denied them permanently, in which case take them to the device settings for this
            // app so the user can change permissions there
            val allowId = if (isAlwaysDenied) R.string.button_edit_permissions else R.string.photo_picker_soft_ask_allow
            soft_ask_view.button.setText(allowId)
            soft_ask_view.button.setOnClickListener {
                if (isStoragePermissionAlwaysDenied) {
                    WPPermissionUtils.showAppSettings(requireActivity())
                } else {
                    requestStoragePermission()
                }
            }
            soft_ask_view.visibility = View.VISIBLE
            hideBottomBar(container_media_source_bar)
        } else if (soft_ask_view.visibility == View.VISIBLE) {
            AniUtils.fadeOut(soft_ask_view, MEDIUM)
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
        private const val KEY_LIST_STATE = "list_state"
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
