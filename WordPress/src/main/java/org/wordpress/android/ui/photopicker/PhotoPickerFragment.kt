package org.wordpress.android.ui.photopicker

import android.Manifest.permission
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.Builder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.PhotoPickerFragmentBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.media.MediaBrowserActivity
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.media.MediaPreviewActivity
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.ProgressDialogUiModel
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.ProgressDialogUiModel.Visible
import org.wordpress.android.util.AccessibilityUtils
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AniUtils.Duration.MEDIUM
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.POSTS
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.ViewWrapper
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.WPPermissionUtils
import org.wordpress.android.util.extensions.getParcelableCompat
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

@Deprecated(
    "This class is being refactored, if you implement any change, please also update " +
            "{@link org.wordpress.android.ui.mediapicker.MediaPickerFragment}"
)
class PhotoPickerFragment : Fragment(R.layout.photo_picker_fragment) {
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

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Suppress("DEPRECATION")
    private lateinit var viewModel: PhotoPickerViewModel
    private var binding: PhotoPickerFragmentBinding? = null

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory).get(PhotoPickerViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val browserType =
            requireArguments().getSerializableCompat<MediaBrowserType>(MediaBrowserActivity.ARG_BROWSER_TYPE)
        val site = requireArguments().getSerializableCompat<SiteModel>(WordPress.SITE)
        var selectedIds: List<Long>? = null
        var lastTappedIcon: PhotoPickerIcon? = null
        if (savedInstanceState != null) {
            val savedLastTappedIconName = savedInstanceState.getString(KEY_LAST_TAPPED_ICON)
            lastTappedIcon = savedLastTappedIconName?.let { PhotoPickerIcon.valueOf(it) }
            if (savedInstanceState.containsKey(KEY_SELECTED_POSITIONS)) {
                selectedIds = savedInstanceState.getLongArray(KEY_SELECTED_POSITIONS)?.toList()
            }
        }
        with(PhotoPickerFragmentBinding.bind(view)) {
            binding = this
            recycler.setEmptyView(actionableEmptyView)
            recycler.setHasFixedSize(true)

            val layoutManager = GridLayoutManager(
                activity,
                NUM_COLUMNS
            )

            savedInstanceState?.getParcelableCompat<Parcelable>(KEY_LIST_STATE)?.let {
                layoutManager.onRestoreInstanceState(it)
            }

            recycler.layoutManager = layoutManager

            observeUIState()

            observeOnNavigateToPreview()

            observeOnInsert()

            observeOnIconClicked()

            observeOnShowPopupMenu()

            observeOnPermissionsRequested()

            setupProgressDialog()

            browserType?.let { viewModel.start(selectedIds, it, lastTappedIcon, site) }
        }
    }

    @Suppress("DEPRECATION")
    private fun observeOnPermissionsRequested() {
        viewModel.onPermissionsRequested.observeEvent(viewLifecycleOwner) {
            when (it) {
                PhotoPickerViewModel.PermissionsRequested.CAMERA -> requestCameraPermission()
                PhotoPickerViewModel.PermissionsRequested.STORAGE -> requestStoragePermission()
            }
        }
    }

    private fun observeOnShowPopupMenu() {
        viewModel.onShowPopupMenu.observeEvent(viewLifecycleOwner) { uiModel ->
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
    }

    private fun observeOnIconClicked() {
        viewModel.onIconClicked.observeEvent(viewLifecycleOwner) { (icon, allowMultipleSelection) ->
            listener?.onPhotoPickerIconClicked(icon, allowMultipleSelection)
        }
    }

    private fun observeOnInsert() {
        viewModel.onInsert.observeEvent(viewLifecycleOwner) { selectedUris ->
            listener?.onPhotoPickerMediaChosen(selectedUris.map { it.uri })
        }
    }

    private fun observeOnNavigateToPreview() {
        viewModel.onNavigateToPreview.observeEvent(viewLifecycleOwner) { uri ->
            MediaPreviewActivity.showPreview(
                requireContext(),
                null,
                uri.toString()
            )
            AccessibilityUtils.setActionModeDoneButtonContentDescription(activity, getString(R.string.cancel))
        }
    }

    @Suppress("DEPRECATION")
    private fun PhotoPickerFragmentBinding.observeUIState() {
        var isShowingActionMode = false
        viewModel.uiState.observe(viewLifecycleOwner) {
            it?.let { uiState ->
                setupPhotoList(uiState.photoListUiModel)
                setupBottomBar(uiState.bottomBarUiModel)
                setupSoftAskView(uiState.softAskViewUiModel)
                if (uiState.actionModeUiModel is PhotoPickerViewModel.ActionModeUiModel.Visible &&
                    !isShowingActionMode
                ) {
                    isShowingActionMode = true
                    (activity as AppCompatActivity).startSupportActionMode(PhotoPickerActionModeCallback(viewModel))
                } else if (uiState.actionModeUiModel is PhotoPickerViewModel.ActionModeUiModel.Hidden &&
                    isShowingActionMode
                ) {
                    isShowingActionMode = false
                }
                setupFab(uiState.fabUiModel)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    @Suppress("DEPRECATION")
    private fun PhotoPickerFragmentBinding.setupSoftAskView(uiModel: PhotoPickerViewModel.SoftAskViewUiModel) {
        when (uiModel) {
            is PhotoPickerViewModel.SoftAskViewUiModel.Visible -> {
                softAskView.title.text = HtmlCompat.fromHtml(uiModel.label, HtmlCompat.FROM_HTML_MODE_LEGACY)
                softAskView.button.setText(uiModel.allowId.stringRes)
                softAskView.button.setOnClickListener {
                    if (uiModel.isAlwaysDenied) {
                        WPPermissionUtils.showAppSettings(requireActivity())
                    } else {
                        requestStoragePermission()
                    }
                }
                softAskView.visibility = View.VISIBLE
            }
            is PhotoPickerViewModel.SoftAskViewUiModel.Hidden -> {
                if (softAskView.visibility == View.VISIBLE) {
                    AniUtils.fadeOut(softAskView, MEDIUM)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun PhotoPickerFragmentBinding.setupPhotoList(uiModel: PhotoPickerViewModel.PhotoListUiModel) {
        if (uiModel is PhotoPickerViewModel.PhotoListUiModel.Data) {
            if (recycler.adapter == null) {
                recycler.adapter = PhotoPickerAdapter(
                    imageManager,
                    viewModel.viewModelScope
                )
            }
            val adapter = recycler.adapter as PhotoPickerAdapter
            val recyclerViewState = recycler.layoutManager?.onSaveInstanceState()
            adapter.loadData(uiModel.items)
            recycler.layoutManager?.onRestoreInstanceState(recyclerViewState)
        }
    }

    @Suppress("DEPRECATION")
    private fun PhotoPickerFragmentBinding.setupFab(fabUiModel: PhotoPickerViewModel.FabUiModel) {
        if (fabUiModel.show) {
            wpStoriesTakePicture.visibility = View.VISIBLE
            wpStoriesTakePicture.setOnClickListener {
                fabUiModel.action()
            }
        } else {
            wpStoriesTakePicture.visibility = View.GONE
        }
    }

    @Suppress("DEPRECATION")
    private fun PhotoPickerFragmentBinding.setupBottomBar(uiModel: PhotoPickerViewModel.BottomBarUiModel) {
        if (!canShowMediaSourceBottomBar(uiModel.hideMediaBottomBarInPortrait)) {
            hideBottomBar(containerMediaSourceBar)
        } else {
            if (!uiModel.showCameraButton) {
                iconCamera.visibility = View.GONE
            } else {
                iconCamera.setOnClickListener {
                    viewModel.onCameraClicked(ViewWrapper(it))
                }
            }
            iconPicker.setOnClickListener {
                uiModel.onIconPickerClicked(ViewWrapper(it))
            }

            if (uiModel.showWPMediaIcon) {
                iconWpmedia.setOnClickListener {
                    viewModel.clickIcon(PhotoPickerIcon.WP_MEDIA)
                }
            } else {
                iconWpmedia.visibility = View.GONE
            }
        }
        if (uiModel.canShowInsertEditBottomBar) {
            textEdit
                .setOnClickListener {
                    val inputData = WPMediaUtils.createListOfEditImageInputData(
                        requireContext(),
                        viewModel.selectedURIs().map { it.uri }
                    )
                    ActivityLauncher.openImageEditor(activity, inputData)
                }
            textInsert.setOnClickListener { viewModel.performInsertAction() }
        }
        val editTextVisible = if (uiModel.insertEditTextBarVisible) View.VISIBLE else View.GONE
        textEdit.visibility = editTextVisible
        when (uiModel.type) {
            PhotoPickerViewModel.BottomBarUiModel.BottomBar.INSERT_EDIT -> {
                hideBottomBar(containerMediaSourceBar)
                showBottomBar(containerInsertEditBar)
            }
            PhotoPickerViewModel.BottomBarUiModel.BottomBar.MEDIA_SOURCE -> {
                if (canShowMediaSourceBottomBar(uiModel.hideMediaBottomBarInPortrait)) {
                    showBottomBar(containerMediaSourceBar)
                } else {
                    hideBottomBar(containerMediaSourceBar)
                }
                hideBottomBar(containerInsertEditBar)
            }
            PhotoPickerViewModel.BottomBarUiModel.BottomBar.NONE -> {
                hideBottomBar(containerInsertEditBar)
                hideBottomBar(containerMediaSourceBar)
            }
        }
    }

    private fun setupProgressDialog() {
        var progressDialog: AlertDialog? = null
        viewModel.uiState.observe(viewLifecycleOwner) {
            it?.progressDialogUiModel?.apply {
                when (this) {
                    is Visible -> {
                        if (progressDialog == null || progressDialog?.isShowing == false) {
                            val builder: Builder = MaterialAlertDialogBuilder(requireContext())
                            builder.setTitle(this.title)
                            builder.setView(R.layout.media_picker_progress_dialog)
                            builder.setNegativeButton(
                                R.string.cancel
                            ) { _, _ -> this.cancelAction() }
                            builder.setCancelable(false)
                            progressDialog = builder.show()
                        }
                    }
                    ProgressDialogUiModel.Hidden -> {
                        progressDialog?.let { dialog ->
                            if (dialog.isShowing) {
                                dialog.dismiss()
                            }
                        }
                    }
                }
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
        binding!!.recycler.layoutManager?.let {
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

    @Suppress("DEPRECATION")
    private fun requestStoragePermission() {
        val permissions = arrayOf(permission.WRITE_EXTERNAL_STORAGE)
        requestPermissions(
            permissions, WPPermissionUtils.PHOTO_PICKER_STORAGE_PERMISSION_REQUEST_CODE
        )
    }

    @Suppress("DEPRECATION")
    private fun requestCameraPermission() {
        // in addition to CAMERA permission we also need a storage permission, to store media from the camera
        val permissions = arrayOf(
            permission.CAMERA,
            permission.WRITE_EXTERNAL_STORAGE
        )
        requestPermissions(permissions, WPPermissionUtils.PHOTO_PICKER_CAMERA_PERMISSION_REQUEST_CODE)
    }

    @Suppress("OVERRIDE_DEPRECATION")
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
        viewModel.clearSelection()
    }

    fun doIconClicked(wpMedia: PhotoPickerIcon) {
        viewModel.clickIcon(wpMedia)
    }

    fun urisSelectedFromSystemPicker(uris: List<Uri>) {
        viewModel.urisSelectedFromSystemPicker(uris.map { UriWrapper(it) })
    }

    fun mediaIdsSelectedFromWPMediaPicker(mediaIds: List<Long>) {
        viewModel.mediaIdsSelectedFromWPMediaPicker(mediaIds)
    }

    companion object {
        private const val KEY_LAST_TAPPED_ICON = "last_tapped_icon"
        private const val KEY_SELECTED_POSITIONS = "selected_positions"
        private const val KEY_LIST_STATE = "list_state"
        const val NUM_COLUMNS = 3

        @Suppress("DEPRECATION")
        @JvmStatic
        fun newInstance(
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
