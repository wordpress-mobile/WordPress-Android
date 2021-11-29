package org.wordpress.android.ui.mysite

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.annotation.StringRes
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCrop.Options
import com.yalantis.ucrop.UCropActivity
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.databinding.MySiteFragmentBinding
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.FullScreenDialogFragment
import org.wordpress.android.ui.FullScreenDialogFragment.Builder
import org.wordpress.android.ui.FullScreenDialogFragment.OnConfirmListener
import org.wordpress.android.ui.FullScreenDialogFragment.OnDismissListener
import org.wordpress.android.ui.PagePostCreationSourcesDetail
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.TextInputDialogFragment
import org.wordpress.android.ui.domains.DomainRegistrationActivity.Companion.RESULT_REGISTERED_DOMAIN_EMAIL
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.CTA_DOMAIN_CREDIT_REDEMPTION
import org.wordpress.android.ui.main.SitePickerActivity
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.main.utils.MeGravatarLoader
import org.wordpress.android.ui.mysite.MySiteViewModel.State
import org.wordpress.android.ui.mysite.SiteIconUploadHandler.ItemUploadedModel
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuFragment
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.photopicker.MediaPickerConstants
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource
import org.wordpress.android.ui.posts.BasicDialogViewModel
import org.wordpress.android.ui.posts.BasicDialogViewModel.BasicDialogModel
import org.wordpress.android.ui.posts.PostListType
import org.wordpress.android.ui.posts.QuickStartPromptDialogFragment
import org.wordpress.android.ui.posts.QuickStartPromptDialogFragment.QuickStartPromptClickInterface
import org.wordpress.android.ui.quickstart.QuickStartFullScreenDialogFragment
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MAIN
import org.wordpress.android.util.AppLog.T.UTILS
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarItem.Action
import org.wordpress.android.util.SnackbarItem.Info
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper
import org.wordpress.android.util.getColorFromAttribute
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.USER
import org.wordpress.android.util.setVisible
import org.wordpress.android.viewmodel.observeEvent
import java.io.File
import javax.inject.Inject

@Suppress("TooManyFunctions")
class MySiteFragment : Fragment(R.layout.my_site_fragment),
        TextInputDialogFragment.Callback,
        QuickStartPromptClickInterface,
        OnConfirmListener,
        OnDismissListener {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var snackbarSequencer: SnackbarSequencer
    @Inject lateinit var meGravatarLoader: MeGravatarLoader
    @Inject lateinit var mediaPickerLauncher: MediaPickerLauncher
    @Inject lateinit var uploadUtilsWrapper: UploadUtilsWrapper
    @Inject lateinit var quickStartUtils: QuickStartUtilsWrapper
    private lateinit var viewModel: MySiteViewModel
    private lateinit var dialogViewModel: BasicDialogViewModel
    private lateinit var dynamicCardMenuViewModel: DynamicCardMenuViewModel
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper

    private var binding: MySiteFragmentBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The following prevents the soft keyboard from leaving a white space when dismissed.
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        (requireActivity().application as WordPress).component().inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory).get(MySiteViewModel::class.java)
        dialogViewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(BasicDialogViewModel::class.java)
        dynamicCardMenuViewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(DynamicCardMenuViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = MySiteFragmentBinding.bind(view).apply {
            setupToolbar()
            setupContentViews(savedInstanceState)
            setupObservers()
        }
    }

    private fun MySiteFragmentBinding.setupToolbar() {
        toolbarMain.let { toolbar ->
            toolbar.inflateMenu(R.menu.my_site_menu)
            toolbar.menu.findItem(R.id.me_item)?.let { meMenu ->
                meMenu.actionView.let { actionView ->
                    actionView.setOnClickListener { viewModel.onAvatarPressed() }
                    TooltipCompat.setTooltipText(actionView, meMenu.title)
                }
            }
        }

        val avatar = root.findViewById<ImageView>(R.id.avatar)

        appbarMain.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val maxOffset = appBarLayout.totalScrollRange
            val currentOffset = maxOffset + verticalOffset

            val percentage = ((currentOffset.toFloat() / maxOffset.toFloat()) * 100).toInt()
            avatar?.let { avatar ->
                val minSize = avatar.minimumHeight
                val maxSize = avatar.maxHeight
                val modifierPx = (minSize.toFloat() - maxSize.toFloat()) * (percentage.toFloat() / 100) * -1
                val modifierPercentage = modifierPx / minSize
                val newScale = 1 + modifierPercentage

                avatar.scaleX = newScale
                avatar.scaleY = newScale
            }
        })
    }

    private fun MySiteFragmentBinding.setupContentViews(savedInstanceState: Bundle?) {
        actionableEmptyView.button.setOnClickListener { viewModel.onAddSitePressed() }

        val layoutManager = LinearLayoutManager(activity)

        savedInstanceState?.getParcelable<Parcelable>(KEY_LIST_STATE)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        recyclerView.layoutManager = layoutManager
        recyclerView.addItemDecoration(
                MySiteCardAndItemDecoration(
                        horizontalMargin = resources.getDimensionPixelSize(R.dimen.margin_extra_large),
                        verticalMargin = resources.getDimensionPixelSize(R.dimen.margin_medium)
                )
        )

        val adapter = MySiteAdapter(imageManager, uiHelpers)

        savedInstanceState?.getBundle(KEY_NESTED_LISTS_STATES)?.let {
            adapter.onRestoreInstanceState(it)
        }

        recyclerView.adapter = adapter

        swipeToRefreshHelper = buildSwipeToRefreshHelper(swipeRefreshLayout) {
            if (NetworkUtils.checkConnection(requireActivity())) {
                viewModel.onPullToRefresh()
            } else {
                swipeToRefreshHelper.isRefreshing = false
            }
        }
    }

    @Suppress("LongMethod")
    private fun MySiteFragmentBinding.setupObservers() {
        viewModel.uiModel.observe(viewLifecycleOwner, { uiModel ->
            loadGravatar(uiModel.accountAvatarUrl)
            when (val state = uiModel.state) {
                is State.SiteSelected -> loadData(state.cardAndItems)
                is State.NoSites -> loadEmptyView(state.shouldShowImage)
            }
        })
        viewModel.onScrollTo.observeEvent(viewLifecycleOwner, {
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(it, 0)
        })
        viewModel.onBasicDialogShown.observeEvent(viewLifecycleOwner, { model ->
            dialogViewModel.showDialog(requireActivity().supportFragmentManager,
                    BasicDialogModel(
                            model.tag,
                            getString(model.title),
                            getString(model.message),
                            getString(model.positiveButtonLabel),
                            model.negativeButtonLabel?.let { label -> getString(label) },
                            model.cancelButtonLabel?.let { label -> getString(label) }
                    ))
        })
        viewModel.onTextInputDialogShown.observeEvent(viewLifecycleOwner, { model ->
            val inputDialog = TextInputDialogFragment.newInstance(
                    getString(model.title),
                    model.initialText,
                    getString(model.hint),
                    model.isMultiline,
                    model.isInputEnabled,
                    model.callbackId
            )
            inputDialog.setTargetFragment(this@MySiteFragment, 0)
            inputDialog.show(parentFragmentManager, TextInputDialogFragment.TAG)
        })
        viewModel.onDynamicCardMenuShown.observeEvent(viewLifecycleOwner, { dynamicCardMenuModel ->
            ((parentFragmentManager.findFragmentByTag(dynamicCardMenuModel.id) as? DynamicCardMenuFragment)
                    ?: DynamicCardMenuFragment.newInstance(
                            dynamicCardMenuModel.cardType,
                            dynamicCardMenuModel.isPinned
                    ))
                    .show(parentFragmentManager, dynamicCardMenuModel.id)
        })
        viewModel.onNavigation.observeEvent(viewLifecycleOwner, { handleNavigationAction(it) })
        viewModel.onSnackbarMessage.observeEvent(viewLifecycleOwner, { showSnackbar(it) })
        viewModel.onQuickStartMySitePrompts.observeEvent(viewLifecycleOwner, { activeTutorialPrompt ->
            val message = quickStartUtils.stylizeThemedQuickStartPrompt(
                    requireContext(),
                    activeTutorialPrompt.shortMessagePrompt,
                    activeTutorialPrompt.iconId
            )
            showSnackbar(SnackbarMessageHolder(UiStringText(message)))
        })
        viewModel.onMediaUpload.observeEvent(viewLifecycleOwner, { UploadService.uploadMedia(requireActivity(), it) })
        dialogViewModel.onInteraction.observeEvent(viewLifecycleOwner, { viewModel.onDialogInteraction(it) })
        dynamicCardMenuViewModel.onInteraction.observeEvent(viewLifecycleOwner, { interaction ->
            viewModel.onQuickStartMenuInteraction(interaction)
        })
        viewModel.onUploadedItem.observeEvent(viewLifecycleOwner, { handleUploadedItem(it) })
        viewModel.onShowSwipeRefreshLayout.observeEvent(viewLifecycleOwner, { showSwipeToRefreshLayout(it) })
    }

    @Suppress("ComplexMethod", "LongMethod")
    private fun handleNavigationAction(action: SiteNavigationAction) = when (action) {
        is SiteNavigationAction.OpenMeScreen -> ActivityLauncher.viewMeActivityForResult(activity)
        is SiteNavigationAction.OpenSitePicker -> ActivityLauncher.showSitePickerForResult(activity, action.site)
        is SiteNavigationAction.OpenSite -> ActivityLauncher.viewCurrentSite(activity, action.site, true)
        is SiteNavigationAction.OpenMediaPicker ->
            mediaPickerLauncher.showSiteIconPicker(this@MySiteFragment, action.site)
        is SiteNavigationAction.OpenCropActivity -> startCropActivity(action.imageUri)
        is SiteNavigationAction.OpenActivityLog -> ActivityLauncher.viewActivityLogList(activity, action.site)
        is SiteNavigationAction.OpenBackup -> ActivityLauncher.viewBackupList(activity, action.site)
        is SiteNavigationAction.OpenScan -> ActivityLauncher.viewScan(activity, action.site)
        is SiteNavigationAction.OpenPlan -> ActivityLauncher.viewBlogPlans(activity, action.site)
        is SiteNavigationAction.OpenPosts -> ActivityLauncher.viewCurrentBlogPosts(requireActivity(), action.site)
        is SiteNavigationAction.OpenPages -> ActivityLauncher.viewCurrentBlogPages(requireActivity(), action.site)
        is SiteNavigationAction.OpenAdmin -> ActivityLauncher.viewBlogAdmin(activity, action.site)
        is SiteNavigationAction.OpenPeople -> ActivityLauncher.viewCurrentBlogPeople(activity, action.site)
        is SiteNavigationAction.OpenSharing -> ActivityLauncher.viewBlogSharing(activity, action.site)
        is SiteNavigationAction.OpenSiteSettings -> ActivityLauncher.viewBlogSettingsForResult(activity, action.site)
        is SiteNavigationAction.OpenThemes -> ActivityLauncher.viewCurrentBlogThemes(activity, action.site)
        is SiteNavigationAction.OpenPlugins -> ActivityLauncher.viewPluginBrowser(activity, action.site)
        is SiteNavigationAction.OpenMedia -> ActivityLauncher.viewCurrentBlogMedia(activity, action.site)
        is SiteNavigationAction.OpenComments -> ActivityLauncher.viewCurrentBlogComments(activity, action.site)
        is SiteNavigationAction.OpenUnifiedComments -> ActivityLauncher.viewUnifiedComments(activity, action.site)
        is SiteNavigationAction.OpenStats -> ActivityLauncher.viewBlogStats(activity, action.site)
        is SiteNavigationAction.ConnectJetpackForStats ->
            ActivityLauncher.viewConnectJetpackForStats(activity, action.site)
        is SiteNavigationAction.StartWPComLoginForJetpackStats ->
            ActivityLauncher.loginForJetpackStats(this@MySiteFragment)
        is SiteNavigationAction.OpenJetpackSettings ->
            ActivityLauncher.viewJetpackSecuritySettings(activity, action.site)
        is SiteNavigationAction.OpenStories -> ActivityLauncher.viewStories(activity, action.site, action.event)
        is SiteNavigationAction.AddNewStory ->
            ActivityLauncher.addNewStoryForResult(activity, action.site, action.source)
        is SiteNavigationAction.AddNewStoryWithMediaIds -> ActivityLauncher.addNewStoryWithMediaIdsForResult(
                activity,
                action.site,
                action.source,
                action.mediaIds.toLongArray()
        )
        is SiteNavigationAction.AddNewStoryWithMediaUris -> ActivityLauncher.addNewStoryWithMediaUrisForResult(
                activity,
                action.site,
                action.source,
                action.mediaUris.toTypedArray()
        )
        is SiteNavigationAction.OpenDomains -> ActivityLauncher.viewDomainsDashboardActivity(
                activity,
                action.site
        )
        is SiteNavigationAction.OpenDomainRegistration -> ActivityLauncher.viewDomainRegistrationActivityForResult(
                activity,
                action.site,
                CTA_DOMAIN_CREDIT_REDEMPTION
        )
        is SiteNavigationAction.AddNewSite -> SitePickerActivity.addSite(activity, action.isSignedInWpCom)
        is SiteNavigationAction.ShowQuickStartDialog -> showQuickStartDialog(
                action.title,
                action.message,
                action.positiveButtonLabel,
                action.negativeButtonLabel
        )
        is SiteNavigationAction.OpenQuickStartFullScreenDialog -> openQuickStartFullScreenDialog(action)
        is SiteNavigationAction.OpenDraftsPosts ->
            ActivityLauncher.viewCurrentBlogPostsOfType(requireActivity(), action.site, PostListType.DRAFTS)
        is SiteNavigationAction.OpenScheduledPosts ->
            ActivityLauncher.viewCurrentBlogPostsOfType(requireActivity(), action.site, PostListType.SCHEDULED)
        is SiteNavigationAction.OpenEditorToCreateNewPost ->
            ActivityLauncher.addNewPostForResult(
                    requireActivity(),
                    action.site,
                    false,
                    PagePostCreationSourcesDetail.POST_FROM_MY_SITE
            )
        // TODO: ashiagr this is unhandled right now as mocked post is being used which cannot be opened in the editor
        is SiteNavigationAction.EditPost -> Unit
    }

    private fun openQuickStartFullScreenDialog(action: SiteNavigationAction.OpenQuickStartFullScreenDialog) {
        val bundle = QuickStartFullScreenDialogFragment.newBundle(action.type)
        Builder(requireContext())
                .setTitle(action.title)
                .setOnConfirmListener(this)
                .setOnDismissListener(this)
                .setContent(QuickStartFullScreenDialogFragment::class.java, bundle)
                .build()
                .show(requireActivity().supportFragmentManager, FullScreenDialogFragment.TAG)
    }

    private fun handleUploadedItem(itemUploadedModel: ItemUploadedModel) = when (itemUploadedModel) {
        is ItemUploadedModel.PostUploaded -> {
            uploadUtilsWrapper.onPostUploadedSnackbarHandler(
                    activity,
                    requireActivity().findViewById(R.id.coordinator),
                    isError = true,
                    isFirstTimePublish = false,
                    post = itemUploadedModel.post,
                    errorMessage = itemUploadedModel.errorMessage,
                    site = itemUploadedModel.site
            )
        }
        is ItemUploadedModel.MediaUploaded -> {
            uploadUtilsWrapper.onMediaUploadedSnackbarHandler(
                    activity,
                    requireActivity().findViewById(R.id.coordinator),
                    isError = true,
                    mediaList = itemUploadedModel.media,
                    site = itemUploadedModel.site,
                    messageForUser = itemUploadedModel.errorMessage
            )
        }
    }

    private fun startCropActivity(imageUri: UriWrapper) {
        val context = activity ?: return
        val options = Options()
        options.setShowCropGrid(false)
        options.setStatusBarColor(context.getColorFromAttribute(android.R.attr.statusBarColor))
        options.setToolbarColor(context.getColorFromAttribute(R.attr.wpColorAppBar))
        options.setToolbarWidgetColor(context.getColorFromAttribute(R.attr.colorOnSurface))
        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.NONE, UCropActivity.NONE)
        options.setHideBottomControls(true)
        UCrop.of(imageUri.uri, Uri.fromFile(File(context.cacheDir, "cropped_for_site_icon.jpg")))
                .withAspectRatio(1f, 1f)
                .withOptions(options)
                .start(requireActivity(), this)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
        viewModel.checkAndShowQuickStartNotice()
    }

    override fun onPause() {
        super.onPause()
        activity?.let {
            if (!it.isChangingConfigurations) {
                viewModel.clearActiveQuickStartTask()
                viewModel.dismissQuickStartNotice()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding?.recyclerView?.layoutManager?.let {
            outState.putParcelable(KEY_LIST_STATE, it.onSaveInstanceState())
        }
        (binding?.recyclerView?.adapter as? MySiteAdapter)?.let {
            outState.putBundle(KEY_NESTED_LISTS_STATES, it.onSaveInstanceState())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun MySiteFragmentBinding.loadGravatar(avatarUrl: String) =
            root.findViewById<ImageView>(R.id.avatar)?.let {
                meGravatarLoader.load(
                        false,
                        meGravatarLoader.constructGravatarUrl(avatarUrl),
                        null,
                        it,
                        USER,
                        null
                )
            }

    @Suppress("ReturnCount", "LongMethod", "ComplexMethod")
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
                        val mediaId = data.getLongExtra(MediaPickerConstants.EXTRA_MEDIA_ID, 0)
                        viewModel.handleSelectedSiteIcon(mediaId)
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
            RequestCodes.STORIES_PHOTO_PICKER,
            RequestCodes.PHOTO_PICKER -> if (resultCode == Activity.RESULT_OK) {
                viewModel.handleStoriesPhotoPickerResult(data)
            }
            UCrop.REQUEST_CROP -> {
                if (resultCode == UCrop.RESULT_ERROR) {
                    AppLog.e(
                            MAIN,
                            "Image cropping failed!",
                            UCrop.getError(data)
                    )
                }
                viewModel.handleCropResult(UCrop.getOutput(data), resultCode == Activity.RESULT_OK)
            }
            RequestCodes.DOMAIN_REGISTRATION -> if (resultCode == Activity.RESULT_OK) {
                viewModel.handleSuccessfulDomainRegistrationResult(data.getStringExtra(RESULT_REGISTERED_DOMAIN_EMAIL))
            }
            RequestCodes.LOGIN_EPILOGUE,
            RequestCodes.CREATE_SITE -> {
                viewModel.checkAndStartQuickStart(
                        data.getIntExtra(
                                SitePickerActivity.KEY_SITE_LOCAL_ID,
                                SelectedSiteRepository.UNAVAILABLE
                        )
                )
            }
            RequestCodes.SITE_PICKER -> {
                if (data.getIntExtra(WPMainActivity.ARG_CREATE_SITE, 0) == RequestCodes.CREATE_SITE) {
                    viewModel.checkAndStartQuickStart(
                            data.getIntExtra(
                                    SitePickerActivity.KEY_SITE_LOCAL_ID,
                                    SelectedSiteRepository.UNAVAILABLE
                            )
                    )
                }
            }
        }
    }

    private fun showQuickStartDialog(
        @StringRes title: Int,
        @StringRes message: Int,
        @StringRes positiveButtonLabel: Int,
        @StringRes negativeButtonLabel: Int
    ) {
        val tag = TAG_QUICK_START_DIALOG
        val quickStartPromptDialogFragment = QuickStartPromptDialogFragment()
        quickStartPromptDialogFragment.initialize(
                tag,
                getString(title),
                getString(message),
                getString(positiveButtonLabel),
                R.drawable.img_illustration_site_about_280dp,
                getString(negativeButtonLabel)
        )
        quickStartPromptDialogFragment.show(parentFragmentManager, tag)
        AnalyticsTracker.track(AnalyticsTracker.Stat.QUICK_START_REQUEST_VIEWED)
    }

    private fun MySiteFragmentBinding.loadData(cardAndItems: List<MySiteCardAndItem>) {
        recyclerView.setVisible(true)
        viewModel.setActionableEmptyViewGone(actionableEmptyView.isVisible) {
            actionableEmptyView.setVisible(false)
        }
        swipeToRefreshHelper.isRefreshing = false
        (recyclerView.adapter as? MySiteAdapter)?.loadData(cardAndItems)
    }

    private fun MySiteFragmentBinding.loadEmptyView(shouldShowEmptyViewImage: Boolean) {
        recyclerView.setVisible(false)
        viewModel.setActionableEmptyViewVisible(actionableEmptyView.isVisible) {
            actionableEmptyView.setVisible(true)
            actionableEmptyView.image.setVisible(shouldShowEmptyViewImage)
        }
        swipeToRefreshHelper.isRefreshing = false
        actionableEmptyView.image.setVisible(shouldShowEmptyViewImage)
    }

    private fun showSnackbar(holder: SnackbarMessageHolder) {
        activity?.let { parent ->
            snackbarSequencer.enqueue(
                    SnackbarItem(
                            info = Info(
                                    view = parent.findViewById(R.id.coordinator),
                                    textRes = holder.message,
                                    duration = holder.duration,
                                    isImportant = holder.isImportant
                            ),
                            action = holder.buttonTitle?.let {
                                Action(
                                        textRes = holder.buttonTitle,
                                        clickListener = { holder.buttonAction() }
                                )
                            },
                            dismissCallback = { _, event -> holder.onDismissAction(event) }
                    )
            )
        }
    }

    private fun showSwipeToRefreshLayout(isEnabled: Boolean) {
        swipeToRefreshHelper.setEnabled(isEnabled)
    }

    companion object {
        private const val KEY_LIST_STATE = "key_list_state"
        private const val KEY_NESTED_LISTS_STATES = "key_nested_lists_states"
        private const val TAG_QUICK_START_DIALOG = "TAG_QUICK_START_DIALOG"
        fun newInstance(): MySiteFragment {
            return MySiteFragment()
        }
    }

    override fun onSuccessfulInput(input: String, callbackId: Int) {
        viewModel.onSiteNameChosen(input)
    }

    override fun onTextInputDialogDismissed(callbackId: Int) {
        viewModel.onSiteNameChooserDismissed()
    }

    override fun onPositiveClicked(instanceTag: String) {
        viewModel.startQuickStart()
    }

    override fun onNegativeClicked(instanceTag: String) {
        viewModel.ignoreQuickStart()
    }

    override fun onConfirm(result: Bundle?) {
        val task = result?.getSerializable(QuickStartFullScreenDialogFragment.RESULT_TASK) as? QuickStartTask
        task?.let { viewModel.onQuickStartTaskCardClick(it) }
    }

    override fun onDismiss() {
        viewModel.onQuickStartFullScreenDialogDismiss()
    }
}
