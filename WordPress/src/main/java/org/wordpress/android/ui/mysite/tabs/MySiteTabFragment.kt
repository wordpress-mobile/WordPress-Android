@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.mysite.tabs

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCrop.Options
import com.yalantis.ucrop.UCropActivity
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.databinding.MySiteTabFragmentBinding
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.FullScreenDialogFragment
import org.wordpress.android.ui.FullScreenDialogFragment.Builder
import org.wordpress.android.ui.FullScreenDialogFragment.OnConfirmListener
import org.wordpress.android.ui.FullScreenDialogFragment.OnDismissListener
import org.wordpress.android.ui.PagePostCreationSourcesDetail
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.TextInputDialogFragment
import org.wordpress.android.ui.accounts.LoginEpilogueActivity
import org.wordpress.android.ui.domains.DomainRegistrationActivity.Companion.RESULT_REGISTERED_DOMAIN_EMAIL
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.CTA_DOMAIN_CREDIT_REDEMPTION
import org.wordpress.android.ui.main.SitePickerActivity
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationActivity
import org.wordpress.android.ui.mysite.MySiteAdapter
import org.wordpress.android.ui.mysite.MySiteCardAndItemDecoration
import org.wordpress.android.ui.mysite.MySiteViewModel
import org.wordpress.android.ui.mysite.MySiteViewModel.MySiteTrackWithTabSource
import org.wordpress.android.ui.mysite.MySiteViewModel.State
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteIconUploadHandler.ItemUploadedModel
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptsCardAnalyticsTracker
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuFragment
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.photopicker.MediaPickerConstants
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.ui.photopicker.PhotoPickerActivity
import org.wordpress.android.ui.posts.BasicDialogViewModel
import org.wordpress.android.ui.posts.BasicDialogViewModel.BasicDialogModel
import org.wordpress.android.ui.posts.EditPostActivity.EXTRA_IS_LANDING_EDITOR_OPENED_FOR_NEW_SITE
import org.wordpress.android.ui.posts.PostListType
import org.wordpress.android.ui.posts.PostUtils.EntryPoint
import org.wordpress.android.ui.posts.QuickStartPromptDialogFragment
import org.wordpress.android.ui.posts.QuickStartPromptDialogFragment.QuickStartPromptClickInterface
import org.wordpress.android.ui.quickstart.QuickStartFullScreenDialogFragment
import org.wordpress.android.ui.quickstart.QuickStartTracker
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MAIN
import org.wordpress.android.util.AppLog.T.UTILS
import org.wordpress.android.util.HtmlCompatWrapper
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarItem.Action
import org.wordpress.android.util.SnackbarItem.Info
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.extensions.setVisible
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.viewmodel.observeEvent
import java.io.File
import javax.inject.Inject

class MySiteTabFragment : Fragment(R.layout.my_site_tab_fragment),
        TextInputDialogFragment.Callback,
        QuickStartPromptClickInterface,
        OnConfirmListener,
        OnDismissListener {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var bloggingPromptsCardAnalyticsTracker: BloggingPromptsCardAnalyticsTracker
    @Inject lateinit var snackbarSequencer: SnackbarSequencer
    @Inject lateinit var mediaPickerLauncher: MediaPickerLauncher
    @Inject lateinit var uploadUtilsWrapper: UploadUtilsWrapper
    @Inject lateinit var quickStartUtils: QuickStartUtilsWrapper
    @Inject lateinit var quickStartTracker: QuickStartTracker
    @Inject lateinit var htmlCompatWrapper: HtmlCompatWrapper
    private lateinit var viewModel: MySiteViewModel
    private lateinit var dialogViewModel: BasicDialogViewModel
    private lateinit var dynamicCardMenuViewModel: DynamicCardMenuViewModel
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper
    private lateinit var mySiteTabType: MySiteTabType

    private var binding: MySiteTabFragmentBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSoftKeyboard()
        initDagger()
        initViewModels()
    }

    private fun initSoftKeyboard() {
        // The following prevents the soft keyboard from leaving a white space when dismissed.
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTabType()
        binding = MySiteTabFragmentBinding.bind(view).apply {
            setupContentViews(savedInstanceState)
            setupObservers()
            swipeToRefreshHelper.isRefreshing = true
        }
    }

    private fun initViewModels() {
        viewModel = ViewModelProvider(requireParentFragment(), viewModelFactory).get(MySiteViewModel::class.java)
        dialogViewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(BasicDialogViewModel::class.java)
        dynamicCardMenuViewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(DynamicCardMenuViewModel::class.java)
    }

    private fun initTabType() {
        mySiteTabType = if (viewModel.isMySiteTabsEnabled) {
            MySiteTabType.fromString(
                    this.arguments?.getString(KEY_MY_SITE_TAB_TYPE, MySiteTabType.SITE_MENU.label)
                            ?: MySiteTabType.SITE_MENU.label
            )
        } else {
            MySiteTabType.ALL
        }
    }

    private fun MySiteTabFragmentBinding.setupContentViews(savedInstanceState: Bundle?) {
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

        val adapter = MySiteAdapter(
                imageManager,
                uiHelpers,
                bloggingPromptsCardAnalyticsTracker,
                htmlCompatWrapper
        ) { viewModel.onBloggingPromptsLearnMoreClicked() }

        adapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                if (itemCount == ONE_ITEM && positionStart == FIRST_ITEM) {
                    recyclerView.smoothScrollToPosition(0)
                }
            }
        })

        savedInstanceState?.getBundle(KEY_NESTED_LISTS_STATES)?.let {
            adapter.onRestoreInstanceState(it)
        }

        recyclerView.adapter = adapter

        swipeToRefreshHelper = buildSwipeToRefreshHelper(swipeRefreshLayout) {
            if (NetworkUtils.checkConnection(requireActivity())) {
                viewModel.refresh(isPullToRefresh = true)
            } else {
                swipeToRefreshHelper.isRefreshing = false
            }
        }
    }

    @Suppress("DEPRECATION", "LongMethod")
    private fun MySiteTabFragmentBinding.setupObservers() {
        viewModel.uiModel.observe(viewLifecycleOwner, { uiModel ->
            hideRefreshIndicatorIfNeeded()
            when (val state = uiModel.state) {
                is State.SiteSelected -> loadData(state)
                is State.NoSites -> loadEmptyView()
            }
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
            inputDialog.setTargetFragment(this@MySiteTabFragment, 0)
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
            val message = quickStartUtils.stylizeQuickStartPrompt(
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
        viewModel.onShare.observeEvent(viewLifecycleOwner) { shareMessage(it) }
        viewModel.onAnswerBloggingPrompt.observeEvent(viewLifecycleOwner) {
            val site = it.first
            val bloggingPromptId = it.second
            ActivityLauncher.addNewPostForResult(
                    activity,
                    site,
                    false,
                    PagePostCreationSourcesDetail.POST_FROM_MY_SITE,
                    bloggingPromptId,
                    EntryPoint.MY_SITE_CARD_ANSWER_PROMPT
            )
        }
        viewModel.onBloggingPromptsLearnMore.observeEvent(viewLifecycleOwner) {
            (activity as? BloggingPromptsOnboardingListener)?.onShowBloggingPromptsOnboarding()
        }
        viewModel.onBloggingPromptsViewMore.observeEvent(viewLifecycleOwner) {
            ActivityLauncher.showBloggingPromptsListActivity(activity)
        }
    }

    @Suppress("ComplexMethod", "LongMethod")
    fun handleNavigationAction(action: SiteNavigationAction) = when (action) {
        is SiteNavigationAction.OpenMeScreen -> ActivityLauncher.viewMeActivityForResult(activity)
        is SiteNavigationAction.OpenSitePicker -> ActivityLauncher.showSitePickerForResult(activity, action.site)
        is SiteNavigationAction.OpenSite -> ActivityLauncher.viewCurrentSite(activity, action.site, true)
        is SiteNavigationAction.OpenMediaPicker ->
            mediaPickerLauncher.showSiteIconPicker(this@MySiteTabFragment, action.site)
        is SiteNavigationAction.OpenCropActivity -> startCropActivity(action.imageUri)
        is SiteNavigationAction.OpenActivityLog -> ActivityLauncher.viewActivityLogList(activity, action.site)
        is SiteNavigationAction.OpenBackup -> ActivityLauncher.viewBackupList(activity, action.site)
        is SiteNavigationAction.OpenScan -> ActivityLauncher.viewScan(activity, action.site)
        is SiteNavigationAction.OpenPlan -> ActivityLauncher.viewBlogPlans(activity, action.site)
        is SiteNavigationAction.OpenPosts -> ActivityLauncher.viewCurrentBlogPosts(requireActivity(), action.site)
        is SiteNavigationAction.OpenPages -> ActivityLauncher.viewCurrentBlogPages(requireActivity(), action.site)
        is SiteNavigationAction.OpenHomepage -> ActivityLauncher.editLandingPageForResult(
                this,
                action.site,
                action.homepageLocalId,
                action.isNewSite
        )
        is SiteNavigationAction.OpenAdmin -> ActivityLauncher.viewBlogAdmin(activity, action.site)
        is SiteNavigationAction.OpenPeople -> ActivityLauncher.viewCurrentBlogPeople(activity, action.site)
        is SiteNavigationAction.OpenSharing -> ActivityLauncher.viewBlogSharing(activity, action.site)
        is SiteNavigationAction.OpenSiteSettings -> ActivityLauncher.viewBlogSettingsForResult(activity, action.site)
        is SiteNavigationAction.OpenThemes -> ActivityLauncher.viewCurrentBlogThemes(activity, action.site)
        is SiteNavigationAction.OpenPlugins -> ActivityLauncher.viewPluginBrowser(activity, action.site)
        is SiteNavigationAction.OpenMedia -> ActivityLauncher.viewCurrentBlogMedia(activity, action.site)
        is SiteNavigationAction.OpenUnifiedComments -> ActivityLauncher.viewUnifiedComments(activity, action.site)
        is SiteNavigationAction.OpenStats -> ActivityLauncher.viewBlogStats(activity, action.site)
        is SiteNavigationAction.ConnectJetpackForStats ->
            ActivityLauncher.viewConnectJetpackForStats(activity, action.site)
        is SiteNavigationAction.StartWPComLoginForJetpackStats ->
            ActivityLauncher.loginForJetpackStats(this@MySiteTabFragment)
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
        is SiteNavigationAction.AddNewSite -> SitePickerActivity.addSite(activity, action.hasAccessToken, action.source)
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
                PagePostCreationSourcesDetail.POST_FROM_MY_SITE,
                -1,
                null
            )
        // The below navigation is temporary and as such not utilizing the 'action.postId' in order to navigate to the
        // 'Edit Post' screen. Instead, it fallbacks to navigating to the 'Posts' screen and targeting a specific tab.
        is SiteNavigationAction.EditDraftPost ->
            ActivityLauncher.viewCurrentBlogPostsOfType(requireActivity(), action.site, PostListType.DRAFTS)
        is SiteNavigationAction.EditScheduledPost ->
            ActivityLauncher.viewCurrentBlogPostsOfType(requireActivity(), action.site, PostListType.SCHEDULED)
        is SiteNavigationAction.OpenStatsInsights ->
            ActivityLauncher.viewBlogStatsForTimeframe(requireActivity(), action.site, StatsTimeframe.INSIGHTS)
        is SiteNavigationAction.OpenTodaysStatsGetMoreViewsExternalUrl ->
            ActivityLauncher.openUrlExternal(requireActivity(), action.url)
        is SiteNavigationAction.OpenJetpackPoweredBottomSheet -> showJetpackPoweredBottomSheet()
        is SiteNavigationAction.OpenJetpackMigrationDeleteWP -> showJetpackMigrationDeleteWP()
    }

    private fun showJetpackPoweredBottomSheet() {
        JetpackPoweredBottomSheetFragment
                .newInstance()
                .show(requireActivity().supportFragmentManager, JetpackPoweredBottomSheetFragment.TAG)
    }

    private fun showJetpackMigrationDeleteWP() {
        val intent = JetpackMigrationActivity.createIntent(
                context = requireActivity(),
                showDeleteWpState = true
        )
        startActivity(intent)
    }

    private fun openQuickStartFullScreenDialog(action: SiteNavigationAction.OpenQuickStartFullScreenDialog) {
        val bundle = QuickStartFullScreenDialogFragment.newBundle(action.type)
        Builder(requireContext())
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
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        activity?.let {
            if (!it.isChangingConfigurations) {
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

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION", "ReturnCount", "LongMethod", "ComplexMethod")
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

                        val source = PhotoPickerActivity.PhotoPickerMediaSource.fromString(
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
                val isNewSite = requestCode == RequestCodes.CREATE_SITE ||
                        data.getBooleanExtra(LoginEpilogueActivity.KEY_SITE_CREATED_FROM_LOGIN_EPILOGUE, false)
                viewModel.onCreateSiteResult()
                viewModel.performFirstStepAfterSiteCreation(
                        data.getIntExtra(SitePickerActivity.KEY_SITE_LOCAL_ID, SelectedSiteRepository.UNAVAILABLE),
                        data.getBooleanExtra(SitePickerActivity.KEY_SITE_TITLE_TASK_COMPLETED, false),
                        isNewSite = isNewSite
                )
            }
            RequestCodes.SITE_PICKER -> {
                if (data.getIntExtra(WPMainActivity.ARG_CREATE_SITE, 0) == RequestCodes.CREATE_SITE) {
                    viewModel.onCreateSiteResult()
                    viewModel.performFirstStepAfterSiteCreation(
                            data.getIntExtra(SitePickerActivity.KEY_SITE_LOCAL_ID, SelectedSiteRepository.UNAVAILABLE),
                            data.getBooleanExtra(SitePickerActivity.KEY_SITE_TITLE_TASK_COMPLETED, false),
                            isNewSite = true
                    )
                } else {
                    viewModel.onSitePicked()
                }
            }
            RequestCodes.EDIT_LANDING_PAGE -> {
                viewModel.checkAndStartQuickStart(
                        data.getIntExtra(SitePickerActivity.KEY_SITE_LOCAL_ID, SelectedSiteRepository.UNAVAILABLE),
                        data.getBooleanExtra(SitePickerActivity.KEY_SITE_TITLE_TASK_COMPLETED, false),
                        isNewSite = data.getBooleanExtra(EXTRA_IS_LANDING_EDITOR_OPENED_FOR_NEW_SITE, false)
                )
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
        quickStartTracker.track(AnalyticsTracker.Stat.QUICK_START_REQUEST_VIEWED)
    }

    private fun MySiteTabFragmentBinding.loadData(state: State.SiteSelected) {
        recyclerView.setVisible(true)
        val cardAndItems = when (mySiteTabType) {
            MySiteTabType.SITE_MENU -> state.siteMenuCardsAndItems
            MySiteTabType.DASHBOARD -> state.dashboardCardsAndItems
            else -> state.cardAndItems
        }
        (recyclerView.adapter as? MySiteAdapter)?.submitList(cardAndItems)
    }

    private fun MySiteTabFragmentBinding.loadEmptyView() {
        recyclerView.setVisible(false)
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

    private fun MySiteTabFragmentBinding.hideRefreshIndicatorIfNeeded() {
        swipeRefreshLayout.postDelayed({
            swipeToRefreshHelper.isRefreshing = viewModel.isRefreshing()
        }, CHECK_REFRESH_DELAY)
    }

    private fun shareMessage(message: String) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, message)

        startActivity(
                Intent.createChooser(
                        shareIntent,
                        resources.getString(R.string.my_site_blogging_prompt_card_share_chooser_title)
                )
        )
    }

    companion object {
        private const val KEY_LIST_STATE = "key_list_state"
        private const val KEY_NESTED_LISTS_STATES = "key_nested_lists_states"
        private const val TAG_QUICK_START_DIALOG = "TAG_QUICK_START_DIALOG"
        private const val KEY_MY_SITE_TAB_TYPE = "key_my_site_tab_type"
        private const val CHECK_REFRESH_DELAY = 300L
        private const val ONE_ITEM = 1
        private const val FIRST_ITEM = 0

        @JvmStatic
        fun newInstance(mySiteTabType: MySiteTabType) = MySiteTabFragment().apply {
            arguments = Bundle().apply {
                putString(KEY_MY_SITE_TAB_TYPE, mySiteTabType.label)
            }
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

    fun handleScrollTo(scrollTo: Int) {
        (binding?.recyclerView?.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(scrollTo, 0)
    }

    fun onTrackWithTabSource(event: MySiteTrackWithTabSource) {
        viewModel.trackWithTabSource(event = event.copy(currentTab = mySiteTabType))
    }
}

interface BloggingPromptsOnboardingListener {
    fun onShowBloggingPromptsOnboarding()
}
