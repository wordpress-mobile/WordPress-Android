package org.wordpress.android.ui.mediapicker

import android.Manifest.permission
import android.app.Activity
import android.content.Intent.ACTION_GET_CONTENT
import android.content.Intent.ACTION_OPEN_DOCUMENT
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.text.Html
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.Builder
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.MediaPickerFragmentBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.media.MediaPreviewActivity
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier
import org.wordpress.android.ui.mediapicker.MediaNavigationEvent.EditMedia
import org.wordpress.android.ui.mediapicker.MediaNavigationEvent.Exit
import org.wordpress.android.ui.mediapicker.MediaNavigationEvent.IconClickEvent
import org.wordpress.android.ui.mediapicker.MediaNavigationEvent.InsertMedia
import org.wordpress.android.ui.mediapicker.MediaNavigationEvent.PreviewMedia
import org.wordpress.android.ui.mediapicker.MediaNavigationEvent.PreviewUrl
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerIconType.ANDROID_CHOOSE_FROM_DEVICE
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerIconType.CAPTURE_PHOTO
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerIconType.SWITCH_SOURCE
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerIconType.WP_STORIES_CAPTURE
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.ActionModeUiModel
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.BrowseMenuUiModel.BrowseAction.DEVICE
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.BrowseMenuUiModel.BrowseAction.GIF_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.BrowseMenuUiModel.BrowseAction.STOCK_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.BrowseMenuUiModel.BrowseAction.SYSTEM_PICKER
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.BrowseMenuUiModel.BrowseAction.WP_MEDIA_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.FabUiModel
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.PermissionsRequested.CAMERA
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.PermissionsRequested.STORAGE
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.PhotoListUiModel
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.ProgressDialogUiModel
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.ProgressDialogUiModel.Visible
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.SearchUiModel
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.SoftAskViewUiModel
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AccessibilityUtils
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AniUtils.Duration.MEDIUM
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarItem.Action
import org.wordpress.android.util.SnackbarItem.Info
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.WPLinkMovementMethod
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.WPPermissionUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class MediaPickerFragment : Fragment() {
    enum class MediaPickerIconType {
        ANDROID_CHOOSE_FROM_DEVICE,
        SWITCH_SOURCE,
        WP_STORIES_CAPTURE,
        CAPTURE_PHOTO;

        companion object {
            @JvmStatic
            fun fromNameString(iconTypeName: String): MediaPickerIconType {
                return values().firstOrNull { it.name == iconTypeName }
                        ?: throw IllegalArgumentException("MediaPickerIconType not found with name $iconTypeName")
            }
        }
    }

    enum class ChooserContext(
        val intentAction: String,
        val title: UiStringRes,
        val mediaTypeFilter: String
    ) {
        PHOTO(ACTION_GET_CONTENT, UiStringRes(R.string.pick_photo), "image/*"),
        VIDEO(ACTION_GET_CONTENT, UiStringRes(R.string.pick_video), "video/*"),
        PHOTO_OR_VIDEO(ACTION_GET_CONTENT, UiStringRes(R.string.pick_media), "*/*"),
        AUDIO(ACTION_GET_CONTENT, UiStringRes(R.string.pick_audio), "*/*"),
        MEDIA_FILE(ACTION_OPEN_DOCUMENT, UiStringRes(R.string.pick_file), "*/*");
    }

    sealed class MediaPickerAction {
        data class OpenSystemPicker(
            val chooserContext: ChooserContext,
            val mimeTypes: List<String>,
            val allowMultipleSelection: Boolean
        ) : MediaPickerAction()

        data class OpenCameraForWPStories(val allowMultipleSelection: Boolean) : MediaPickerAction()
        object OpenCameraForPhotos : MediaPickerAction()
        data class SwitchMediaPicker(val mediaPickerSetup: MediaPickerSetup) : MediaPickerAction()
    }

    sealed class MediaPickerIcon(val type: MediaPickerIconType) {
        data class ChooseFromAndroidDevice(
            val allowedTypes: Set<MediaType>
        ) : MediaPickerIcon(ANDROID_CHOOSE_FROM_DEVICE)

        data class SwitchSource(val dataSource: DataSource) : MediaPickerIcon(SWITCH_SOURCE)

        object WpStoriesCapture : MediaPickerIcon(WP_STORIES_CAPTURE)
        object CapturePhoto : MediaPickerIcon(CAPTURE_PHOTO)

        fun toBundle(bundle: Bundle) {
            bundle.putString(KEY_LAST_TAPPED_ICON, type.name)
            when (this) {
                is ChooseFromAndroidDevice -> {
                    bundle.putStringArrayList(
                            KEY_LAST_TAPPED_ICON_ALLOWED_TYPES,
                            ArrayList(allowedTypes.map { it.name })
                    )
                }
                is SwitchSource -> {
                    bundle.putInt(KEY_LAST_TAPPED_ICON_DATA_SOURCE, this.dataSource.ordinal)
                }
            }
        }

        companion object {
            @JvmStatic
            fun fromBundle(bundle: Bundle): MediaPickerIcon? {
                val iconTypeName = bundle.getString(KEY_LAST_TAPPED_ICON) ?: return null

                return when (iconTypeName.let { MediaPickerIconType.fromNameString(iconTypeName) }) {
                    ANDROID_CHOOSE_FROM_DEVICE -> {
                        val allowedTypes = (bundle.getStringArrayList(KEY_LAST_TAPPED_ICON_ALLOWED_TYPES)
                                ?: listOf<String>()).map {
                            MediaType.valueOf(
                                    it
                            )
                        }.toSet()
                        ChooseFromAndroidDevice(allowedTypes)
                    }
                    WP_STORIES_CAPTURE -> WpStoriesCapture
                    CAPTURE_PHOTO -> CapturePhoto
                    SWITCH_SOURCE -> {
                        val ordinal = bundle.getInt(KEY_LAST_TAPPED_ICON_DATA_SOURCE, -1)
                        if (ordinal != -1) {
                            val dataSource = DataSource.values()[ordinal]
                            SwitchSource(dataSource)
                        } else {
                            null
                        }
                    }
                }
            }
        }
    }

    /*
     * parent activity must implement this listener
     */
    interface MediaPickerListener {
        fun onItemsChosen(identifiers: List<Identifier>)
        fun onIconClicked(action: MediaPickerAction)
    }

    private var listener: MediaPickerListener? = null

    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var snackbarSequencer: SnackbarSequencer
    @Inject lateinit var uiHelpers: UiHelpers
    private lateinit var viewModel: MediaPickerViewModel
    private var binding: MediaPickerFragmentBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory).get(MediaPickerViewModel::class.java)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
                R.layout.media_picker_fragment,
                container,
                false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mediaPickerSetup = MediaPickerSetup.fromBundle(requireArguments())
        val site = requireArguments().getSerializable(WordPress.SITE) as? SiteModel
        var selectedIds: List<Identifier>? = null
        var lastTappedIcon: MediaPickerIcon? = null
        if (savedInstanceState != null) {
            lastTappedIcon = MediaPickerIcon.fromBundle(savedInstanceState)
            if (savedInstanceState.containsKey(KEY_SELECTED_IDS)) {
                selectedIds = savedInstanceState.getParcelableArrayList<Identifier>(KEY_SELECTED_IDS)?.map { it }
            }
        }

        val layoutManager = GridLayoutManager(
                activity,
                NUM_COLUMNS
        )

        savedInstanceState?.getParcelable<Parcelable>(KEY_LIST_STATE)?.let {
            layoutManager.onRestoreInstanceState(it)
        }
        with(MediaPickerFragmentBinding.bind(view)) {
            binding = this
            recycler.layoutManager = layoutManager
            recycler.setEmptyView(actionableEmptyView)
            recycler.setHasFixedSize(true)

            val swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(pullToRefresh) {
                viewModel.onPullToRefresh()
            }

            var isShowingActionMode = false
            viewModel.uiState.observe(viewLifecycleOwner, Observer {
                it?.let { uiState ->
                    setupPhotoList(uiState.photoListUiModel)
                    setupSoftAskView(uiState.softAskViewUiModel)
                    if (uiState.actionModeUiModel is ActionModeUiModel.Visible && !isShowingActionMode) {
                        isShowingActionMode = true
                        (activity as AppCompatActivity).startSupportActionMode(
                                MediaPickerActionModeCallback(
                                        viewModel
                                )
                        )
                    } else if (uiState.actionModeUiModel is ActionModeUiModel.Hidden && isShowingActionMode) {
                        isShowingActionMode = false
                    }
                    setupFab(uiState.fabUiModel)
                    swipeToRefreshHelper.isRefreshing = uiState.isRefreshing
                }
            })

            viewModel.onNavigate.observeEvent(viewLifecycleOwner,
                    { navigationEvent ->
                        when (navigationEvent) {
                            is PreviewUrl -> {
                                MediaPreviewActivity.showPreview(
                                        requireContext(),
                                        null,
                                        navigationEvent.url
                                )
                                AccessibilityUtils.setActionModeDoneButtonContentDescription(
                                        activity,
                                        getString(R.string.cancel)
                                )
                            }
                            is PreviewMedia -> MediaPreviewActivity.showPreview(
                                    requireContext(),
                                    null,
                                    navigationEvent.media,
                                    null
                            )
                            is EditMedia -> {
                                val inputData = WPMediaUtils.createListOfEditImageInputData(
                                        requireContext(),
                                        navigationEvent.uris.map { wrapper -> wrapper.uri }
                                )
                                ActivityLauncher.openImageEditor(activity, inputData)
                            }
                            is InsertMedia -> listener?.onItemsChosen(navigationEvent.identifiers)
                            is IconClickEvent -> listener?.onIconClicked(navigationEvent.action)
                            Exit -> {
                                val activity = requireActivity()
                                activity.setResult(Activity.RESULT_CANCELED)
                                activity.finish()
                            }
                        }
                    })

            viewModel.onPermissionsRequested.observeEvent(viewLifecycleOwner, {
                when (it) {
                    CAMERA -> requestCameraPermission()
                    STORAGE -> requestStoragePermission()
                }
            })
            viewModel.onSnackbarMessage.observeEvent(viewLifecycleOwner, { messageHolder ->
                showSnackbar(messageHolder)
            })

            setupProgressDialog()

            viewModel.start(selectedIds, mediaPickerSetup, lastTappedIcon, site)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_media_picker, menu)

        val searchMenuItem = checkNotNull(menu.findItem(R.id.action_search)) {
            "Menu does not contain mandatory search item"
        }
        val browseMenuItem = checkNotNull(menu.findItem(R.id.mnu_browse_item)) {
            "Menu does not contain mandatory browse item"
        }
        val mediaLibraryMenuItem = checkNotNull(menu.findItem(R.id.mnu_choose_from_media_library)) {
            "Menu does not contain mandatory media library item"
        }
        val deviceMenuItem = checkNotNull(menu.findItem(R.id.mnu_choose_from_device)) {
            "Menu does not contain device library item"
        }
        val stockLibraryMenuItem = checkNotNull(menu.findItem(R.id.mnu_choose_from_stock_library)) {
            "Menu does not contain mandatory stock library item"
        }
        val tenorLibraryMenuItem = checkNotNull(menu.findItem(R.id.mnu_choose_from_tenor_library)) {
            "Menu does not contain mandatory tenor library item"
        }

        initializeSearchView(searchMenuItem)
        viewModel.uiState.observe(viewLifecycleOwner, Observer { uiState ->
            val searchView = searchMenuItem.actionView as SearchView

            if (uiState.searchUiModel is SearchUiModel.Expanded && !searchMenuItem.isActionViewExpanded) {
                searchMenuItem.expandActionView()
                searchView.maxWidth = Integer.MAX_VALUE
                searchView.setQuery(uiState.searchUiModel.filter, true)
                searchView.setOnCloseListener { !uiState.searchUiModel.closeable }
            } else if (uiState.searchUiModel is SearchUiModel.Collapsed && searchMenuItem.isActionViewExpanded) {
                searchMenuItem.collapseActionView()
            }

            searchMenuItem.isVisible = uiState.searchUiModel !is SearchUiModel.Hidden

            val shownActions = uiState.browseMenuUiModel.shownActions
            browseMenuItem.isVisible = shownActions.contains(SYSTEM_PICKER)
            mediaLibraryMenuItem.isVisible = shownActions.contains(WP_MEDIA_LIBRARY)
            deviceMenuItem.isVisible = shownActions.contains(DEVICE)
            stockLibraryMenuItem.isVisible = shownActions.contains(STOCK_LIBRARY)
            tenorLibraryMenuItem.isVisible = shownActions.contains(GIF_LIBRARY)
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mnu_browse_item -> {
                viewModel.onMenuItemClicked(SYSTEM_PICKER)
            }
            R.id.mnu_choose_from_media_library -> {
                viewModel.onMenuItemClicked(WP_MEDIA_LIBRARY)
            }
            R.id.mnu_choose_from_device -> {
                viewModel.onMenuItemClicked(DEVICE)
            }
            R.id.mnu_choose_from_stock_library -> {
                viewModel.onMenuItemClicked(STOCK_LIBRARY)
            }
            R.id.mnu_choose_from_tenor_library -> {
                viewModel.onMenuItemClicked(GIF_LIBRARY)
            }
        }
        return true
    }

    fun urisSelectedFromSystemPicker(uris: List<Uri>) {
        viewModel.urisSelectedFromSystemPicker(uris.map { UriWrapper(it) })
    }

    private fun initializeSearchView(actionMenuItem: MenuItem) {
        var isExpanding = false
        actionMenuItem.setOnActionExpandListener(object : OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                viewModel.onSearchExpanded()
                isExpanding = true
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                viewModel.onSearchCollapsed()
                return true
            }
        })
        val searchView = actionMenuItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                viewModel.onSearch(query)
                return true
            }

            override fun onQueryTextChange(query: String): Boolean {
                if (!isExpanding) {
                    viewModel.onSearch(query)
                }
                isExpanding = false
                return true
            }
        })
    }

    private fun MediaPickerFragmentBinding.setupSoftAskView(uiModel: SoftAskViewUiModel) {
        when (uiModel) {
            is SoftAskViewUiModel.Visible -> {
                softAskView.title.text = Html.fromHtml(uiModel.label)
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
            is SoftAskViewUiModel.Hidden -> {
                if (softAskView.visibility == View.VISIBLE) {
                    AniUtils.fadeOut(softAskView, MEDIUM)
                }
            }
        }
    }

    private fun MediaPickerFragmentBinding.setupPhotoList(uiModel: PhotoListUiModel) {
        loadingView.visibility = if (uiModel == PhotoListUiModel.Loading) View.VISIBLE else View.GONE
        actionableEmptyView.visibility = if (uiModel is PhotoListUiModel.Empty) View.VISIBLE else View.GONE
        recycler.visibility = if (uiModel is PhotoListUiModel.Data) View.VISIBLE else View.INVISIBLE
        when (uiModel) {
            is PhotoListUiModel.Data -> {
                setupAdapter(uiModel.items)
            }
            is PhotoListUiModel.Empty -> {
                setupAdapter(listOf())
                actionableEmptyView.updateLayoutForSearch(uiModel.isSearching, 0)
                actionableEmptyView.title.text = uiHelpers.getTextOfUiString(requireContext(), uiModel.title)

                actionableEmptyView.subtitle.applyOrHide(uiModel.htmlSubtitle) { htmlSubtitle ->
                    actionableEmptyView.subtitle.text = Html.fromHtml(
                            uiHelpers.getTextOfUiString(
                                    requireContext(),
                                    htmlSubtitle
                            ).toString()
                    )
                    actionableEmptyView.subtitle.movementMethod = WPLinkMovementMethod.getInstance()
                }
                actionableEmptyView.image.applyOrHide(uiModel.image) { image ->
                    this.setImageResource(image)
                }
                actionableEmptyView.bottomImage.applyOrHide(uiModel.bottomImage) { bottomImage ->
                    this.setImageResource(bottomImage)
                    if (uiModel.bottomImageDescription != null) {
                        this.contentDescription = uiHelpers.getTextOfUiString(
                                requireContext(),
                                uiModel.bottomImageDescription
                        ).toString()
                    }
                }
                actionableEmptyView.button.applyOrHide(uiModel.retryAction) { action ->
                    this.setOnClickListener {
                        action()
                    }
                }
            }
        }
    }

    private fun <T, U : View> U.applyOrHide(item: T?, action: U.(T) -> Unit) {
        if (item != null) {
            this.visibility = View.VISIBLE
            this.action(item)
        } else {
            this.visibility = View.GONE
        }
    }

    private fun MediaPickerFragmentBinding.setupAdapter(items: List<MediaPickerUiItem>) {
        if (recycler.adapter == null) {
            recycler.adapter = MediaPickerAdapter(
                    imageManager,
                    viewModel.viewModelScope
            )
        }
        val adapter = recycler.adapter as MediaPickerAdapter

        (recycler.layoutManager as? GridLayoutManager)?.spanSizeLookup =
                object : SpanSizeLookup() {
                    override fun getSpanSize(position: Int) = if (items[position].fullWidthItem) {
                        NUM_COLUMNS
                    } else {
                        1
                    }
                }
        val recyclerViewState = recycler.layoutManager?.onSaveInstanceState()
        adapter.loadData(items)
        recycler.layoutManager?.onRestoreInstanceState(recyclerViewState)
    }

    private fun MediaPickerFragmentBinding.setupFab(fabUiModel: FabUiModel) {
        if (fabUiModel.show) {
            fabTakePicture.show()
            fabTakePicture.setOnClickListener {
                fabUiModel.action()
            }
        } else {
            fabTakePicture.hide()
        }
    }

    private fun setupProgressDialog() {
        var progressDialog: AlertDialog? = null
        viewModel.uiState.observe(viewLifecycleOwner, Observer {
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
                            builder.setOnCancelListener { this.cancelAction() }
                            builder.setCancelable(true)
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
        })
    }

    private fun MediaPickerFragmentBinding.showSnackbar(holder: SnackbarMessageHolder) {
        snackbarSequencer.enqueue(
                SnackbarItem(
                        Info(
                                view = coordinator,
                                textRes = holder.message,
                                duration = Snackbar.LENGTH_LONG
                        ),
                        holder.buttonTitle?.let {
                            Action(
                                    textRes = holder.buttonTitle,
                                    clickListener = View.OnClickListener { holder.buttonAction() }
                            )
                        },
                        dismissCallback = { _, event -> holder.onDismissAction(event) }
                )
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.lastTappedIcon?.toBundle(outState)
        val selectedIds = viewModel.selectedIdentifiers()
        if (selectedIds.isNotEmpty()) {
            outState.putParcelableArrayList(KEY_SELECTED_IDS, ArrayList<Identifier>(selectedIds))
        }
        binding!!.recycler.layoutManager?.let {
            outState.putParcelable(KEY_LIST_STATE, it.onSaveInstanceState())
        }
    }

    override fun onResume() {
        super.onResume()
        checkStoragePermission()
    }

    fun setMediaPickerListener(listener: MediaPickerListener?) {
        this.listener = listener
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
        val permissions = arrayOf(permission.WRITE_EXTERNAL_STORAGE, permission.READ_EXTERNAL_STORAGE)
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

    companion object {
        private const val KEY_LAST_TAPPED_ICON = "last_tapped_icon"
        private const val KEY_LAST_TAPPED_ICON_ALLOWED_TYPES = "last_tapped_icon_allowed_types"
        private const val KEY_LAST_TAPPED_ICON_DATA_SOURCE = "last_tapped_icon_data_source"
        private const val KEY_SELECTED_IDS = "selected_ids"
        private const val KEY_LIST_STATE = "list_state"
        const val NUM_COLUMNS = 3
        @JvmStatic fun newInstance(
            listener: MediaPickerListener,
            mediaPickerSetup: MediaPickerSetup,
            site: SiteModel?
        ): MediaPickerFragment {
            val args = Bundle()
            mediaPickerSetup.toBundle(args)
            if (site != null) {
                args.putSerializable(WordPress.SITE, site)
            }
            val fragment = MediaPickerFragment()
            fragment.setMediaPickerListener(listener)
            fragment.arguments = args
            return fragment
        }
    }
}
