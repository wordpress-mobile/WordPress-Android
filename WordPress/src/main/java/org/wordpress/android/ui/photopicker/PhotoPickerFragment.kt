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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.photo_picker_fragment.*
import kotlinx.android.synthetic.main.photo_picker_fragment.view.*
import kotlinx.android.synthetic.main.stats_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
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
import org.wordpress.android.ui.photopicker.PhotoPickerViewModel.PhotoPickerUiModel.BottomBarUiModel
import org.wordpress.android.ui.photopicker.PhotoPickerViewModel.PhotoPickerUiModel.BottomBarUiModel.BottomBar.INSERT_EDIT
import org.wordpress.android.ui.photopicker.PhotoPickerViewModel.PhotoPickerUiModel.BottomBarUiModel.BottomBar.MEDIA_SOURCE
import org.wordpress.android.ui.photopicker.PhotoPickerViewModel.PhotoPickerUiModel.BottomBarUiModel.BottomBar.NONE
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AccessibilityUtils
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AniUtils.Duration.MEDIUM
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MEDIA
import org.wordpress.android.util.AppLog.T.POSTS
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.WPPermissionUtils
import org.wordpress.android.util.config.TenorFeatureConfig
import org.wordpress.android.util.image.ImageManager
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
        fun onPhotoPickerMediaChosen(uriList: List<Uri>)
        fun onPhotoPickerIconClicked(icon: PhotoPickerIcon, allowMultipleSelection: Boolean)
    }

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
        recycler.setEmptyView(actionable_empty_view)
        recycler.setHasFixedSize(true)

        val layoutManager = GridLayoutManager(
                activity,
                NUM_COLUMNS
        )

        savedInstanceState?.getParcelable<Parcelable>(KEY_LIST_STATE)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        recycler.layoutManager = layoutManager

        viewModel.data.observe(viewLifecycleOwner, Observer { uiModel ->
            if (uiModel != null) {
                if (recycler.adapter == null) {
                    recycler.adapter = PhotoPickerAdapter(
                            imageManager
                    )
                }
                val adapter = recycler.adapter as PhotoPickerAdapter
                val recyclerViewState = recyclerView?.layoutManager?.onSaveInstanceState()
                adapter.loadData(uiModel.items)
                recyclerView?.layoutManager?.onRestoreInstanceState(recyclerViewState)
                setupBottomBar(uiModel.bottomBarUiModel)

                if (uiModel.showStoriesTakePicture) {
                    wp_stories_take_picture.visibility = View.VISIBLE
                    wp_stories_take_picture.setOnClickListener { doIconClicked(WP_STORIES_CAPTURE) }
                } else {
                    wp_stories_take_picture.visibility = View.GONE
                }
            }
        })

        viewModel.showActionMode.observe(viewLifecycleOwner, Observer {
            if (it?.peekContent() == true) {
                it.getContentIfNotHandled()

                (activity as AppCompatActivity).startSupportActionMode(ActionModeCallback(viewModel))
                hideBottomBar(container_media_source_bar)
            }
        }
        )

        viewModel.navigateToPreview.observe(viewLifecycleOwner, Observer
        {
            it.getContentIfNotHandled()?.let { uri ->
                MediaPreviewActivity.showPreview(
                        requireContext(),
                        null,
                        uri.toString()
                )
                AccessibilityUtils.setActionModeDoneButtonContentDescription(activity, getString(string.cancel))
            }
        })

        viewModel.onInsert.observe(viewLifecycleOwner, Observer
        { event ->
            event.getContentIfNotHandled()?.let { selectedUris ->
                listener?.onPhotoPickerMediaChosen(selectedUris.map { it.uri })
            }
        })

        viewModel.onIconClicked.observe(viewLifecycleOwner, Observer {
            it?.getContentIfNotHandled()?.let { (icon, allowMultipleSelection) ->
                listener?.onPhotoPickerIconClicked(icon, allowMultipleSelection)
            }
        })

        viewModel.start(selectedIds, browserType)
    }

    private fun setupBottomBar(uiModel: BottomBarUiModel) {
        if (!canShowMediaSourceBottomBar(uiModel.hideMediaBottomBarInPortrait)) {
            container_media_source_bar.visibility = View.GONE
        } else {
            if (!uiModel.showCameraButton) {
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
            container_insert_edit_bar.text_insert.setOnClickListener { viewModel.performInsertAction() }
        }
        val editTextVisible = if (uiModel.insertEditTextBarVisible) View.VISIBLE else View.GONE
        container_insert_edit_bar.text_edit.visibility = editTextVisible
        when (uiModel.type) {
            INSERT_EDIT -> {
                hideBottomBar(container_media_source_bar)
                showBottomBar(container_insert_edit_bar)
            }
            MEDIA_SOURCE -> {
                if (canShowMediaSourceBottomBar(uiModel.hideMediaBottomBarInPortrait)) {
                    showBottomBar(container_media_source_bar)
                } else {
                    hideBottomBar(container_media_source_bar)
                }
                hideBottomBar(container_insert_edit_bar)
            }
            NONE -> TODO()
        }
    }

    private fun canShowMediaSourceBottomBar(
        hideMediaBottomBarInPortrait: Boolean
    ): Boolean {
        return !hideMediaBottomBarInPortrait || DisplayUtils.isLandscape(activity)
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
        if (icon == ANDROID_CAPTURE_PHOTO || icon == ANDROID_CAPTURE_VIDEO || icon == WP_STORIES_CAPTURE) {
            if (ContextCompat.checkSelfPermission(
                            requireActivity(), permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED || !hasStoragePermission()) {
                requestCameraPermission()
                return
            }
        }
        viewModel.clickIcon(icon)
    }

    fun performActionOrShowPopup(view: View) {
        val popup = PopupMenu(activity, view)
        if (browserType.isImagePicker) {
            val itemPhoto = popup.menu
                    .add(string.photo_picker_choose_photo)
            itemPhoto.setOnMenuItemClickListener {
                doIconClicked(ANDROID_CHOOSE_PHOTO)
                true
            }
        }
        if (browserType.isVideoPicker) {
            val itemVideo = popup.menu
                    .add(string.photo_picker_choose_video)
            itemVideo.setOnMenuItemClickListener {
                doIconClicked(ANDROID_CHOOSE_VIDEO)
                true
            }
        }
        if (site != null && !browserType.isGutenbergPicker) {
            val itemStock = popup.menu
                    .add(string.photo_picker_stock_media)
            itemStock.setOnMenuItemClickListener {
                doIconClicked(STOCK_MEDIA)
                true
            }

            // only show GIF picker from Tenor if this is NOT the WPStories picker
            if (tenorFeatureConfig.isEnabled() && !browserType.isWPStoriesPicker) {
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

    private class ActionModeCallback(private val viewModel: PhotoPickerViewModel) : Callback, LifecycleOwner {
        private lateinit var lifecycleRegistry: LifecycleRegistry
        override fun onCreateActionMode(
            actionMode: ActionMode,
            menu: Menu
        ): Boolean {
            lifecycleRegistry = LifecycleRegistry(this)
            lifecycleRegistry.handleLifecycleEvent(ON_START)
            viewModel.actionModeUiModel.observe(this, Observer { uiModel ->
                if (uiModel.showInsertEditBottomBar) {
                    viewModel.showInsertEditBottomBar()
                } else if (menu.size() == 0) {
                    val inflater = actionMode.menuInflater
                    inflater.inflate(R.menu.photo_picker_action_mode, menu)
                }

                if (uiModel.actionModeTitle is UiStringText) {
                    actionMode.title = uiModel.actionModeTitle.text
                } else if (uiModel.actionModeTitle is UiStringRes) {
                    actionMode.setTitle(uiModel.actionModeTitle.stringRes)
                }
            })
            viewModel.showActionMode.observe(this, Observer {
                if (it?.peekContent() == false) {
                    it.getContentIfNotHandled()
                    actionMode.finish()
                }
            })
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(
            mode: ActionMode,
            item: MenuItem
        ): Boolean {
            if (item.itemId == R.id.mnu_confirm_selection) {
                viewModel.performInsertAction()
                return true
            }
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            viewModel.showMediaSourceBottomBar()
            viewModel.clearSelection()

            lifecycleRegistry.handleLifecycleEvent(ON_STOP)
        }

        override fun getLifecycle(): Lifecycle = lifecycleRegistry
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
            val appName = "<strong>${getString(string.app_name)}</strong>"
            val label: String
            label = if (isAlwaysDenied) {
                val permissionName = ("<strong>${WPPermissionUtils.getPermissionName(
                        requireActivity(),
                        permission.WRITE_EXTERNAL_STORAGE
                )}</strong>")
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
            soft_ask_view.title.text = Html.fromHtml(label)

            // when the user taps Allow, request the required permissions unless the user already
            // denied them permanently, in which case take them to the device settings for this
            // app so the user can change permissions there
            val allowId = if (isAlwaysDenied) string.button_edit_permissions else string.photo_picker_soft_ask_allow
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
            if (canShowMediaSourceBottomBar(browserType == AZTEC_EDITOR_PICKER)) {
                showBottomBar(container_media_source_bar)
            }
        }
    }

    fun finishActionMode() {
        viewModel.finishActionMode()
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
