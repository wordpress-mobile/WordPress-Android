package org.wordpress.android.ui.photopicker

import android.Manifest.permission
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.photo_picker_fragment.*
import kotlinx.android.synthetic.main.photo_picker_fragment.view.*
import kotlinx.android.synthetic.main.stats_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.media.MediaBrowserActivity
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.media.MediaPreviewActivity
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon.WP_MEDIA
import org.wordpress.android.ui.photopicker.PhotoPickerViewModel.BottomBarUiModel
import org.wordpress.android.ui.photopicker.PhotoPickerViewModel.BottomBarUiModel.BottomBar.INSERT_EDIT
import org.wordpress.android.ui.photopicker.PhotoPickerViewModel.BottomBarUiModel.BottomBar.MEDIA_SOURCE
import org.wordpress.android.ui.photopicker.PhotoPickerViewModel.BottomBarUiModel.BottomBar.NONE
import org.wordpress.android.ui.photopicker.PhotoPickerViewModel.FabUiModel
import org.wordpress.android.ui.photopicker.PhotoPickerViewModel.PermissionsRequested.CAMERA
import org.wordpress.android.ui.photopicker.PhotoPickerViewModel.PermissionsRequested.STORAGE
import org.wordpress.android.ui.photopicker.PhotoPickerViewModel.PhotoListUiModel
import org.wordpress.android.ui.photopicker.PhotoPickerViewModel.SoftAskViewUiModel
import org.wordpress.android.ui.photopicker.PhotoPickerViewModel.SoftAskViewUiModel.Hide
import org.wordpress.android.ui.photopicker.PhotoPickerViewModel.SoftAskViewUiModel.Show
import org.wordpress.android.util.AccessibilityUtils
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AniUtils.Duration.MEDIUM
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.POSTS
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.ViewWrapper
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

        val browserType = requireArguments().getSerializable(MediaBrowserActivity.ARG_BROWSER_TYPE) as MediaBrowserType
        val site = requireArguments().getSerializable(WordPress.SITE) as SiteModel
        var selectedIds: List<Long>? = null
        var lastTappedIcon: PhotoPickerIcon? = null
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

        viewModel.uiState.observe(viewLifecycleOwner, Observer {
            it?.let { uiState ->
                uiState.photoListUiModel?.let(this::setupPhotoList)
                uiState.bottomBarUiModel?.let(this::setupBottomBar)
                uiState.softAskViewUiModel?.let(this::setupSoftAskView)
                uiState.fabUiModel?.let(this::setupFab)
            }
        })

        viewModel.onShowActionMode.observe(viewLifecycleOwner, Observer {
            if (it?.peekContent() == true && !it.hasBeenHandled) {
                it.getContentIfNotHandled()

                (activity as AppCompatActivity).startSupportActionMode(
                        PhotoPickerActionModeCallback(
                                viewModel
                        )
                )
            }
        }
        )

        viewModel.onNavigateToPreview.observe(viewLifecycleOwner, Observer
        {
            it.getContentIfNotHandled()?.let { uri ->
                MediaPreviewActivity.showPreview(
                        requireContext(),
                        null,
                        uri.toString()
                )
                AccessibilityUtils.setActionModeDoneButtonContentDescription(activity, getString(R.string.cancel))
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

        viewModel.onShowPopupMenu.observe(viewLifecycleOwner, Observer {
            it?.getContentIfNotHandled()?.let { uiModel ->
                val popup = PopupMenu(activity, uiModel.view.view)
                for (popupMenuItem in uiModel.items) {
                    val item = popup.menu
                            .add(popupMenuItem.title.stringRes)
                    item.setOnMenuItemClickListener {
                        popupMenuItem.action()
                        true
                    }
                }
                popup.show()
            }
        })

        viewModel.onPermissionsRequested.observe(viewLifecycleOwner, Observer {
            it?.applyIfNotHandled {
                when (this) {
                    CAMERA -> requestCameraPermission()
                    STORAGE -> requestStoragePermission()
                }
            }
        })

        viewModel.start(selectedIds, browserType, lastTappedIcon, site)
    }

    private fun setupSoftAskView(uiModel: SoftAskViewUiModel?) {
        when (uiModel) {
            is Show -> {
                soft_ask_view.title.text = Html.fromHtml(uiModel.label)
                soft_ask_view.button.setText(uiModel.allowId.stringRes)
                soft_ask_view.button.setOnClickListener {
                    if (uiModel.isAlwaysDenied) {
                        WPPermissionUtils.showAppSettings(requireActivity())
                    } else {
                        requestStoragePermission()
                    }
                }
                soft_ask_view.visibility = View.VISIBLE
            }
            is Hide -> {
                if (soft_ask_view.visibility == View.VISIBLE) {
                    AniUtils.fadeOut(soft_ask_view, MEDIUM)
                }
            }
        }
    }

    private fun setupPhotoList(uiModel: PhotoListUiModel) {
        if (recycler.adapter == null) {
            recycler.adapter = PhotoPickerAdapter(
                    imageManager
            )
        }
        val adapter = recycler.adapter as PhotoPickerAdapter
        val recyclerViewState = recyclerView?.layoutManager?.onSaveInstanceState()
        adapter.loadData(uiModel.items)
        recyclerView?.layoutManager?.onRestoreInstanceState(recyclerViewState)
    }

    private fun setupFab(fabUiModel: FabUiModel) {
        if (fabUiModel.show) {
            wp_stories_take_picture.visibility = View.VISIBLE
            wp_stories_take_picture.setOnClickListener {
                fabUiModel.action()
            }
        } else {
            wp_stories_take_picture.visibility = View.GONE
        }
    }

    private fun setupBottomBar(uiModel: BottomBarUiModel) {
        if (!canShowMediaSourceBottomBar(uiModel.hideMediaBottomBarInPortrait)) {
            hideBottomBar(container_media_source_bar)
        } else {
            if (!uiModel.showCameraButton) {
                container_media_source_bar.icon_camera.visibility = View.GONE
            } else {
                container_media_source_bar.icon_camera.setOnClickListener {
                    viewModel.onCameraClicked(ViewWrapper(it))
                }
            }
            container_media_source_bar.icon_picker
                    ?.setOnClickListener {
                        uiModel.onIconPickerClicked(ViewWrapper(it))
                    }

            if (uiModel.showWPMediaIcon) {
                container_media_source_bar.icon_wpmedia.setOnClickListener {
                    viewModel.clickIcon(WP_MEDIA)
                }
            } else {
                container_media_source_bar.icon_wpmedia.visibility = View.GONE
            }
        }
        if (uiModel.canShowInsertEditBottomBar) {
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
            NONE -> {
                hideBottomBar(container_insert_edit_bar)
                hideBottomBar(container_media_source_bar)
            }
        }
    }

    private fun canShowMediaSourceBottomBar(
        hideMediaBottomBarInPortrait: Boolean
    ): Boolean {
        return !hideMediaBottomBarInPortrait || DisplayUtils.isLandscape(activity)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(
                KEY_LAST_TAPPED_ICON,
                viewModel.lastTappedIcon?.name
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

    fun performActionOrShowPopup(view: View) {
        viewModel.performActionOrShowPopup(ViewWrapper(view))
    }

    fun showCameraPopupMenu(view: View) {
        viewModel.showCameraPopupMenu(ViewWrapper(view))
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
        viewModel.refreshData(false)
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
        viewModel.checkStoragePermission(isStoragePermissionAlwaysDenied)
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
                viewModel.clickOnLastTappedIcon()
            }
        }
    }

    fun finishActionMode() {
        viewModel.finishActionMode()
    }

    fun doIconClicked(wpMedia: PhotoPickerIcon) {
        viewModel.clickIcon(wpMedia)
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
