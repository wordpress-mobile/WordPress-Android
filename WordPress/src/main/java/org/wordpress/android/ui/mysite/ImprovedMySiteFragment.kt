package org.wordpress.android.ui.mysite

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.TooltipCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.snackbar.Snackbar
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCrop.Options
import com.yalantis.ucrop.UCropActivity
import kotlinx.android.synthetic.main.me_action_layout.*
import kotlinx.android.synthetic.main.media_picker_fragment.*
import kotlinx.android.synthetic.main.new_my_site_fragment.*
import org.wordpress.android.R
import org.wordpress.android.R.attr
import org.wordpress.android.WordPress
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.TextInputDialogFragment
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.main.utils.MeGravatarLoader
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenConnectJetpackForStatsScreen
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenCropActivity
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenMeScreen
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenMediaPicker
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenSite
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenSitePicker
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenStatsScreen
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.StartLoginForJetpackStats
import org.wordpress.android.ui.mysite.SiteIconUploadViewModel.ItemUploadedModel
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.photopicker.MediaPickerConstants
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource
import org.wordpress.android.ui.posts.BasicDialogViewModel
import org.wordpress.android.ui.posts.BasicDialogViewModel.BasicDialogModel
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MAIN
import org.wordpress.android.util.AppLog.T.UTILS
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarItem.Action
import org.wordpress.android.util.SnackbarItem.Info
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.getColorFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.USER
import java.io.File
import javax.inject.Inject

class ImprovedMySiteFragment : Fragment(),
        TextInputDialogFragment.Callback {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var snackbarSequencer: SnackbarSequencer
    @Inject lateinit var meGravatarLoader: MeGravatarLoader
    @Inject lateinit var mediaPickerLauncher: MediaPickerLauncher
    @Inject lateinit var selectedSiteRepository: SelectedSiteRepository
    @Inject lateinit var uploadUtilsWrapper: UploadUtilsWrapper
    private lateinit var viewModel: MySiteViewModel
    private lateinit var siteIconUploadViewModel: SiteIconUploadViewModel
    private lateinit var dialogViewModel: BasicDialogViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(MySiteViewModel::class.java)
        siteIconUploadViewModel = ViewModelProviders.of(this, viewModelFactory).get(SiteIconUploadViewModel::class.java)
        dialogViewModel = ViewModelProviders.of(requireActivity(), viewModelFactory)
                .get(BasicDialogViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
                R.layout.new_my_site_fragment,
                container,
                false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appbar_main.addOnOffsetChangedListener(OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val maxOffset = appBarLayout.totalScrollRange
            val currentOffset = maxOffset + verticalOffset

            val percentage = ((currentOffset.toFloat() / maxOffset.toFloat()) * 100).toInt()
            avatar?.let {
                val minSize = avatar.minimumHeight
                val maxSize = avatar.maxHeight
                val modifierPx = (minSize.toFloat() - maxSize.toFloat()) * (percentage.toFloat() / 100) * -1
                val modifierPercentage = modifierPx / minSize
                val newScale = 1 + modifierPercentage

                avatar.scaleX = newScale
                avatar.scaleY = newScale
            }
        })

        toolbar_main.let { toolbar ->
            toolbar.inflateMenu(R.menu.my_site_menu)
            toolbar.menu.findItem(R.id.me_item)?.let { meMenu ->
                meMenu.actionView.let { actionView ->
                    actionView.setOnClickListener { viewModel.onAvatarPressed() }
                    TooltipCompat.setTooltipText(actionView, meMenu.title)
                }
            }
        }

        val layoutManager = LinearLayoutManager(activity)

        savedInstanceState?.getParcelable<Parcelable>(KEY_LIST_STATE)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        recycler_view.layoutManager = layoutManager

        viewModel.uiModel.observe(viewLifecycleOwner, {
            it?.let { uiModel ->
                loadGravatar(uiModel.accountAvatarUrl)
                loadData(uiModel.items)
            }
        })
        viewModel.onBasicDialogShown.observe(viewLifecycleOwner, {
            it?.getContentIfNotHandled()?.let { model ->
                dialogViewModel.showDialog(requireActivity().supportFragmentManager,
                        BasicDialogModel(
                                model.tag,
                                getString(model.title),
                                getString(model.message),
                                getString(model.positiveButtonLabel),
                                model.negativeButtonLabel?.let { label -> getString(label) },
                                model.cancelButtonLabel?.let { label -> getString(label) }
                        ))
            }
        })
        viewModel.onTextInputDialogShown.observe(viewLifecycleOwner, {
            it?.getContentIfNotHandled()?.let { model ->
                val inputDialog = TextInputDialogFragment.newInstance(
                        getString(model.title),
                        model.initialText,
                        getString(model.hint),
                        model.isMultiline,
                        model.isInputEnabled,
                        model.callbackId
                )
                inputDialog.setTargetFragment(this, 0)
                inputDialog.show(parentFragmentManager, TextInputDialogFragment.TAG)
            }
        })
        viewModel.onNavigation.observe(viewLifecycleOwner, {
            it?.getContentIfNotHandled()?.let { action ->
                when (action) {
                    is OpenMeScreen -> {
                        ActivityLauncher.viewMeActivityForResult(activity)
                    }
                    is OpenSitePicker -> {
                        ActivityLauncher.showSitePickerForResult(activity, action.site)
                    }
                    is OpenSite -> {
                        ActivityLauncher.viewCurrentSite(activity, action.site, true)
                    }
                    is OpenMediaPicker -> {
                        mediaPickerLauncher.showSiteIconPicker(
                                requireActivity(),
                                action.site,
                                RequestCodes.SITE_ICON_PICKER
                        )
                    }
                    is OpenCropActivity -> {
                        startCropActivity(action.imageUri)
                    }
                    is OpenStatsScreen -> {
                        ActivityLauncher.viewBlogStats(activity, action.site)
                    }
                    is OpenConnectJetpackForStatsScreen -> {
                        ActivityLauncher.viewConnectJetpackForStats(activity, action.site)
                    }
                    is StartLoginForJetpackStats -> {
                        ActivityLauncher.loginForJetpackStats(this)
                    }
                }
            }
        })
        viewModel.onSnackbarMessage.observe(viewLifecycleOwner, {
            it?.getContentIfNotHandled()?.let { messageHolder ->
                showSnackbar(messageHolder)
            }
        })
        viewModel.onMediaUpload.observe(viewLifecycleOwner, {
            it?.getContentIfNotHandled()?.let { mediaModel ->
                UploadService.uploadMedia(requireActivity(), mediaModel)
            }
        })
        dialogViewModel.onInteraction.observe(viewLifecycleOwner, {
            it?.getContentIfNotHandled()?.let { interaction -> viewModel.onDialogInteraction(interaction) }
        })
        siteIconUploadViewModel.onUploadedItem.observe(viewLifecycleOwner, {
            it?.getContentIfNotHandled()?.let { itemUploadedModel ->
                when (itemUploadedModel) {
                    is ItemUploadedModel.PostUploaded -> {
                        uploadUtilsWrapper.onPostUploadedSnackbarHandler(
                                activity,
                                requireActivity().findViewById(R.id.coordinator), true,
                                itemUploadedModel.post, itemUploadedModel.errorMessage, itemUploadedModel.site
                        )
                    }
                    is ItemUploadedModel.MediaUploaded -> {
                        uploadUtilsWrapper.onMediaUploadedSnackbarHandler(
                                activity,
                                requireActivity().findViewById(R.id.coordinator), true,
                                itemUploadedModel.media, itemUploadedModel.site, itemUploadedModel.errorMessage
                        )
                    }
                }
            }
        })
    }

    private fun startCropActivity(imageUri: UriWrapper) {
        val context = activity ?: return
        val options = Options()
        options.setShowCropGrid(false)
        options.setStatusBarColor(context.getColorFromAttribute(android.R.attr.statusBarColor))
        options.setToolbarColor(context.getColorFromAttribute(R.attr.wpColorAppBar))
        options.setToolbarWidgetColor(context.getColorFromAttribute(attr.colorOnSurface))
        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.NONE, UCropActivity.NONE)
        options.setHideBottomControls(true)
        UCrop.of(imageUri.uri, Uri.fromFile(File(context.cacheDir, "cropped_for_site_icon.jpg")))
                .withAspectRatio(1f, 1f)
                .withOptions(options)
                .start(requireActivity(), this)
    }

    override fun onResume() {
        super.onResume()
        selectedSiteRepository.updateSite((activity as? WPMainActivity)?.selectedSite)
        selectedSiteRepository.updateSiteSettingsIfNecessary()
        viewModel.refreshAccountAvatarUrl()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        recycler_view.layoutManager?.let {
            outState.putParcelable(KEY_LIST_STATE, it.onSaveInstanceState())
        }
    }

    private fun loadGravatar(avatarUrl: String) = avatar?.let {
        meGravatarLoader.load(
                false,
                meGravatarLoader.constructGravatarUrl(avatarUrl),
                null,
                it,
                USER,
                null
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) {
            return
        }
        when (requestCode) {
            RequestCodes.DO_LOGIN -> if (resultCode == Activity.RESULT_OK) {
                viewModel.handleSuccessfulLoginResult()
            }
            RequestCodes.SITE_ICON_PICKER -> {
                if (resultCode != Activity.RESULT_OK) {
                    return
                }
                when {
                    data.hasExtra(MediaPickerConstants.EXTRA_MEDIA_ID) -> {
                        val mediaId = data.getLongExtra(MediaPickerConstants.EXTRA_MEDIA_ID, 0).toInt()
                        selectedSiteRepository.updateSiteIconMediaId(mediaId, true)
                    }
                    data.hasExtra(MediaPickerConstants.EXTRA_MEDIA_URIS) -> {
                        val mediaUriStringsArray = data.getStringArrayExtra(
                                MediaPickerConstants.EXTRA_MEDIA_URIS
                        ) ?: return

                        val source = PhotoPickerMediaSource.fromString(
                                data.getStringExtra(MediaPickerConstants.EXTRA_MEDIA_SOURCE)
                        )
                        val iconUrl = mediaUriStringsArray.getOrNull(0) ?: return
                        viewModel.handleTakenSiteIcon(iconUrl, source)
                    }
                    else -> {
                        AppLog.e(
                                UTILS,
                                "Can't resolve picked or captured image"
                        )
                    }
                }
            }
            UCrop.REQUEST_CROP -> {
                if (resultCode == UCrop.RESULT_ERROR) {
                    AppLog.e(
                            MAIN,
                            "Image cropping failed!",
                            UCrop.getError(data!!)
                    )
                }
                viewModel.handleCropResult(UCrop.getOutput(data), resultCode == Activity.RESULT_OK)
            }
        }
    }

    private fun loadData(items: List<MySiteItem>) {
        if (recycler_view.adapter == null) {
            recycler_view.adapter = MySiteAdapter(imageManager)
        }
        val adapter = recycler_view.adapter as MySiteAdapter
        adapter.loadData(items)
    }

    private fun showSnackbar(holder: SnackbarMessageHolder) {
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
                                    clickListener = { holder.buttonAction() }
                            )
                        },
                        dismissCallback = { _, _ -> holder.onDismissAction() }
                )
        )
    }

    companion object {
        private const val KEY_LIST_STATE = "key_list_state"
        fun newInstance(): ImprovedMySiteFragment {
            return ImprovedMySiteFragment()
        }
    }

    override fun onSuccessfulInput(input: String, callbackId: Int) {
        viewModel.onSiteNameChosen(input)
    }

    override fun onTextInputDialogDismissed(callbackId: Int) {
        viewModel.onSiteNameChooserDismissed()
    }
}
