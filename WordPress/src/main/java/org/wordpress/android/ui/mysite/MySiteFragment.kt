package org.wordpress.android.ui.mysite

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.databinding.MySiteFragmentBinding
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.ActivityNavigator
import org.wordpress.android.ui.FullScreenDialogFragment
import org.wordpress.android.ui.PagePostCreationSourcesDetail
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.TextInputDialogFragment
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.accounts.LoginEpilogueActivity
import org.wordpress.android.ui.bloganuary.learnmore.BloganuaryNudgeLearnMoreOverlayFragment
import org.wordpress.android.ui.deeplinks.DeepLinkingIntentReceiverActivity
import org.wordpress.android.ui.domains.DomainRegistrationActivity
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureFullScreenOverlayFragment
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.jetpackoverlay.individualplugin.WPJetpackIndividualPluginFragment
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.onboarding.JetpackFullPluginInstallOnboardingDialogFragment
import org.wordpress.android.ui.main.AddSiteHandler
import org.wordpress.android.ui.main.ChooseSiteActivity
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationActivity
import org.wordpress.android.ui.main.utils.MeGravatarLoader
import org.wordpress.android.ui.mysite.MySiteViewModel.State
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptsCardAnalyticsTracker
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.photopicker.MediaPickerConstants
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.ui.posts.BasicDialogViewModel
import org.wordpress.android.ui.posts.EditPostActivityConstants
import org.wordpress.android.ui.posts.PostListType
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.posts.QuickStartPromptDialogFragment
import org.wordpress.android.ui.posts.QuickStartPromptDialogFragment.QuickStartPromptClickInterface
import org.wordpress.android.ui.quickstart.QuickStartFullScreenDialogFragment
import org.wordpress.android.ui.quickstart.QuickStartTracker
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.ui.stats.refresh.utils.StatsLaunchedFrom
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.ui.utils.TitleSubtitleSnackbarSpannable
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.HtmlCompatWrapper
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.PackageManagerWrapper
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.util.extensions.setVisible
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.viewmodel.main.WPMainActivityViewModel
import org.wordpress.android.viewmodel.observeEvent
import org.wordpress.android.viewmodel.pages.PageListViewModel
import java.io.File
import javax.inject.Inject

@Suppress("LargeClass")
class MySiteFragment : Fragment(R.layout.my_site_fragment),
    TextInputDialogFragment.Callback,
    QuickStartPromptClickInterface,
    FullScreenDialogFragment.OnConfirmListener,
    FullScreenDialogFragment.OnDismissListener,
    OnScrollToTopListener {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var meGravatarLoader: MeGravatarLoader

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var accountStore: AccountStore

    @Inject
    lateinit var bloggingPromptsCardAnalyticsTracker: BloggingPromptsCardAnalyticsTracker

    @Inject
    lateinit var snackbarSequencer: SnackbarSequencer

    @Inject
    lateinit var readerTracker: ReaderTracker

    @Inject
    lateinit var quickStartTracker: QuickStartTracker

    @Inject
    lateinit var quickStartUtils: QuickStartUtilsWrapper

    @Inject
    lateinit var htmlCompatWrapper: HtmlCompatWrapper

    @Inject
    lateinit var mediaPickerLauncher: MediaPickerLauncher

    @Inject
    lateinit var uploadUtilsWrapper: UploadUtilsWrapper

    @Inject
    lateinit var activityNavigator: ActivityNavigator

    @Inject
    lateinit var packageManagerWrapper: PackageManagerWrapper

    private lateinit var viewModel: MySiteViewModel
    private lateinit var dialogViewModel: BasicDialogViewModel
    private lateinit var wpMainActivityViewModel: WPMainActivityViewModel
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper

    private var binding: MySiteFragmentBinding? = null
    private var siteTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSoftKeyboard()
        initDagger()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        binding = MySiteFragmentBinding.bind(view).apply {
            setupContentViews(savedInstanceState)
            setupObservers()
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

    override fun onPause() {
        super.onPause()
        activity?.let {
            if (!it.isChangingConfigurations) {
                viewModel.clearActiveQuickStartTask()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
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
        val task = result?.getSerializableCompat(
            QuickStartFullScreenDialogFragment.RESULT_TASK
        ) as? QuickStartStore.QuickStartTask
        task?.let { viewModel.onQuickStartTaskCardClick(it) }
    }

    override fun onDismiss() {
        viewModel.onQuickStartFullScreenDialogDismiss()
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

                        val source =
                            org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource.fromString(
                                data.getStringExtra(MediaPickerConstants.EXTRA_MEDIA_SOURCE)
                            )
                        val iconUrl = mediaUriStringsArray.getOrNull(0) ?: return
                        viewModel.handleTakenSiteIcon(iconUrl, source)
                    }
                    else -> {
                        AppLog.e(
                            AppLog.T.UTILS,
                            "Can't resolve picked or captured image"
                        )
                    }
                }
            }
            RequestCodes.STORIES_PHOTO_PICKER,
            UCrop.REQUEST_CROP -> {
                if (resultCode == UCrop.RESULT_ERROR) {
                    AppLog.e(
                        AppLog.T.MAIN,
                        "Image cropping failed!",
                        UCrop.getError(data)
                    )
                }
                viewModel.handleCropResult(UCrop.getOutput(data), resultCode == Activity.RESULT_OK)
            }
            RequestCodes.DOMAIN_REGISTRATION -> if (resultCode == Activity.RESULT_OK) {
                viewModel.handleSuccessfulDomainRegistrationResult(
                    data.getStringExtra(DomainRegistrationActivity.RESULT_REGISTERED_DOMAIN_EMAIL)
                )
            }
            RequestCodes.LOGIN_EPILOGUE,
            RequestCodes.CREATE_SITE -> {
                val isNewSite = requestCode == RequestCodes.CREATE_SITE ||
                        data.getBooleanExtra(LoginEpilogueActivity.KEY_SITE_CREATED_FROM_LOGIN_EPILOGUE, false)
                viewModel.performFirstStepAfterSiteCreation(
                    data.getBooleanExtra(ChooseSiteActivity.KEY_SITE_TITLE_TASK_COMPLETED, false),
                    isNewSite = isNewSite
                )
            }
            RequestCodes.SITE_PICKER -> {
                if (data.getIntExtra(WPMainActivity.ARG_CREATE_SITE, 0) == RequestCodes.CREATE_SITE) {
                    viewModel.performFirstStepAfterSiteCreation(
                        data.getBooleanExtra(ChooseSiteActivity.KEY_SITE_TITLE_TASK_COMPLETED, false),
                        isNewSite = true
                    )
                } else {
                    viewModel.onSitePicked()
                }
            }
            RequestCodes.EDIT_LANDING_PAGE -> {
                viewModel.checkAndStartQuickStart(
                    data.getBooleanExtra(ChooseSiteActivity.KEY_SITE_TITLE_TASK_COMPLETED, false),
                    isNewSite = data.getBooleanExtra(
                        EditPostActivityConstants.EXTRA_IS_LANDING_EDITOR_OPENED_FOR_NEW_SITE, false
                    )
                )
            }
        }
    }

    private fun initSoftKeyboard() {
        // The following prevents the soft keyboard from leaving a white space when dismissed.
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component().inject(this)
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this, viewModelFactory).get(MySiteViewModel::class.java)
        wpMainActivityViewModel = ViewModelProvider(requireActivity(), viewModelFactory)
            .get(WPMainActivityViewModel::class.java)
        dialogViewModel = ViewModelProvider(requireActivity(), viewModelFactory)
            .get(BasicDialogViewModel::class.java)
    }

    private fun MySiteFragmentBinding.setupContentViews(savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(activity)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            savedInstanceState?.getParcelable(KEY_LIST_STATE, Parcelable::class.java)?.let {
                layoutManager.onRestoreInstanceState(it)
            }
        } else {
            @Suppress("DEPRECATION")
            savedInstanceState?.getParcelable<Parcelable>(KEY_LIST_STATE)?.let {
                layoutManager.onRestoreInstanceState(it)
            }
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
            accountStore,
            meGravatarLoader,
            bloggingPromptsCardAnalyticsTracker,
            htmlCompatWrapper,
            { viewModel.onBloggingPromptsLearnMoreClicked() },
            { viewModel.onBloggingPromptsAttributionClicked() }
        )

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                if (positionStart == FIRST_ITEM) {
                    recyclerView.smoothScrollToPosition(0)
                }
            }
        })

        savedInstanceState?.getBundle(KEY_NESTED_LISTS_STATES)?.let {
            adapter.onRestoreInstanceState(it)
        }

        recyclerView.adapter = adapter

        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(swipeRefreshLayout) {
            if (NetworkUtils.checkConnection(requireActivity())) {
                viewModel.refresh(isPullToRefresh = true)
            } else {
                swipeToRefreshHelper.isRefreshing = false
            }
        }

        setupActionableEmptyView()
    }

    private fun MySiteFragmentBinding.setupActionableEmptyView() {
        noSitesView.actionableEmptyView.button.setOnClickListener { viewModel.onAddSitePressed() }
    }

    @Suppress("LongMethod")
    private fun MySiteFragmentBinding.setupObservers() {
        viewModel.uiModel.observe(viewLifecycleOwner) { uiModel ->
            when (uiModel) {
                is State.SiteSelected -> loadData(uiModel)
                is State.NoSites -> loadEmptyView(uiModel)
            }
        }
        viewModel.onBasicDialogShown.observeEvent(viewLifecycleOwner) { model ->
            dialogViewModel.showDialog(requireActivity().supportFragmentManager,
                BasicDialogViewModel.BasicDialogModel(
                    model.tag,
                    getString(model.title),
                    getString(model.message),
                    getString(model.positiveButtonLabel),
                    model.negativeButtonLabel?.let { label -> getString(label) },
                    model.cancelButtonLabel?.let { label -> getString(label) }
                ))
        }
        viewModel.onTextInputDialogShown.observeEvent(viewLifecycleOwner) { model ->
            val inputDialog = TextInputDialogFragment.newInstance(
                getString(model.title),
                model.initialText,
                getString(model.hint),
                model.isMultiline,
                model.isInputEnabled,
                model.callbackId
            )
            inputDialog.show(childFragmentManager, TextInputDialogFragment.TAG)
        }
        viewModel.onNavigation.observeEvent(viewLifecycleOwner) { handleNavigationAction(it) }
        viewModel.onSnackbarMessage.observeEvent(viewLifecycleOwner) { showSnackbar(it) }
        viewModel.onQuickStartMySitePrompts.observeEvent(viewLifecycleOwner) { activeTutorialPrompt ->
            val message = quickStartUtils.stylizeQuickStartPrompt(
                requireContext(),
                activeTutorialPrompt.shortMessagePrompt,
                activeTutorialPrompt.iconId
            )
            showSnackbar(SnackbarMessageHolder(UiString.UiStringText(message)))
        }
        viewModel.onMediaUpload.observeEvent(viewLifecycleOwner) {
            UploadService.uploadMedia(requireActivity(), it, "MySiteFragment onMediaUpload")
        }
        dialogViewModel.onInteraction.observeEvent(viewLifecycleOwner) { viewModel.onDialogInteraction(it) }
        viewModel.onUploadedItem.observeEvent(viewLifecycleOwner) { handleUploadedItem(it) }
        viewModel.onOpenJetpackInstallFullPluginOnboarding.observeEvent(viewLifecycleOwner) {
            JetpackFullPluginInstallOnboardingDialogFragment.newInstance().show(
                requireActivity().supportFragmentManager,
                JetpackFullPluginInstallOnboardingDialogFragment.TAG
            )
        }

        viewModel.refresh.observe(viewLifecycleOwner) {
            viewModel.refresh()
        }

        viewModel.onShowJetpackIndividualPluginOverlay.observeEvent(viewLifecycleOwner) {
            WPJetpackIndividualPluginFragment.show(requireActivity().supportFragmentManager)
        }

        viewModel.onScrollTo.observeEvent(viewLifecycleOwner) {
            var quickStartScrollPosition = it
            if (quickStartScrollPosition == -1) {
                quickStartScrollPosition = 0
            }
            recyclerView.scrollToPosition(quickStartScrollPosition)
        }

        wpMainActivityViewModel.mySiteDashboardRefreshRequested.observeEvent(viewLifecycleOwner) {
            viewModel.refresh()
        }

        viewModel.isRefreshingOrLoading.observe(viewLifecycleOwner) {
            swipeToRefreshHelper.isRefreshing = it
        }
    }

    private fun showSnackbar(holder: SnackbarMessageHolder) {
        activity?.let { parent ->
            snackbarSequencer.enqueue(
                SnackbarItem(
                    info = SnackbarItem.Info(
                        view = parent.findViewById(R.id.coordinator),
                        textRes = holder.message,
                        duration = holder.duration,
                        isImportant = holder.isImportant
                    ),
                    action = holder.buttonTitle?.let {
                        SnackbarItem.Action(
                            textRes = holder.buttonTitle,
                            clickListener = { holder.buttonAction() }
                        )
                    },
                    dismissCallback = { _, event -> holder.onDismissAction(event) }
                )
            )
        }
    }

    private fun handleUploadedItem(
        itemUploadedModel: SiteIconUploadHandler.ItemUploadedModel
    ) = when (itemUploadedModel) {
        is SiteIconUploadHandler.ItemUploadedModel.PostUploaded -> {
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
        is SiteIconUploadHandler.ItemUploadedModel.MediaUploaded -> {
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

    private fun MySiteFragmentBinding.loadData(state: State.SiteSelected) {
        recyclerView.setVisible(true)
        (recyclerView.adapter as? MySiteAdapter)?.submitList(state.dashboardData)

        if (noSitesView.actionableEmptyView.isVisible) {
            noSitesView.actionableEmptyView.setVisible(false)
        }

        if(noSitesView.avatarAccountSettings.isVisible){
            noSitesView.avatarAccountSettings.setVisible(false)
        }
    }

    private fun MySiteFragmentBinding.loadEmptyView(state: State.NoSites) {
        recyclerView.setVisible(false)

        if (!noSitesView.actionableEmptyView.isVisible) {
            noSitesView.actionableEmptyView.setVisible(true)
            viewModel.onActionableEmptyViewVisible()
        }
        showAvatarSettingsView(state)
        siteTitle = getString(R.string.my_site_section_screen_title)
    }

    private fun MySiteFragmentBinding.showAvatarSettingsView(state: State.NoSites) {
        // For a newly created account, avatar may be null
        if (state.accountName != null || state.avatarUrl != null){
            noSitesView.actionableEmptyView.image.setVisible(true)
            noSitesView.avatarAccountSettings.visibility = View.VISIBLE
            noSitesView.meDisplayName.text = state.accountName
            if (state.accountName.isNullOrEmpty()) {
                noSitesView.meDisplayName.visibility = View.GONE
            } else {
                noSitesView.meDisplayName.visibility = View.VISIBLE
            }
            loadGravatar(state.avatarUrl)
            noSitesView.avatarAccountSettings.setOnClickListener { viewModel.onAvatarPressed() }
        } else noSitesView.avatarAccountSettings.visibility = View.GONE
    }

    private fun MySiteFragmentBinding.loadGravatar(avatarUrl: String?) =
        avatarUrl?.let {
            noSitesView.meAvatar.let {
                meGravatarLoader.load(
                    false,
                    meGravatarLoader.constructGravatarUrl(avatarUrl),
                    null,
                    it,
                    ImageType.USER,
                    null
                )
            }
        }

    @Suppress("ComplexMethod", "LongMethod")
    fun handleNavigationAction(action: SiteNavigationAction) = when (action) {
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
        is SiteNavigationAction.OpenMore -> activityNavigator.openUnifiedMySiteMenu(
            requireActivity(),
            action.quickStartEvent
        )
        is SiteNavigationAction.OpenUnifiedComments -> ActivityLauncher.viewUnifiedComments(activity, action.site)
        is SiteNavigationAction.OpenStats -> ActivityLauncher.viewBlogStats(
            activity,
            action.site,
            StatsLaunchedFrom.QUICK_ACTIONS
        )

        is SiteNavigationAction.ConnectJetpackForStats ->
            ActivityLauncher.viewConnectJetpackForStats(activity, action.site)
        is SiteNavigationAction.StartWPComLoginForJetpackStats ->
            ActivityLauncher.loginForJetpackStats(this@MySiteFragment)
        is SiteNavigationAction.OpenDomains -> ActivityLauncher.viewDomainsDashboardActivity(
            activity,
            action.site
        )
        is SiteNavigationAction.OpenDomainRegistration -> ActivityLauncher.viewDomainRegistrationActivityForResult(
            activity,
            action.site,
            DomainRegistrationActivity.DomainRegistrationPurpose.CTA_DOMAIN_CREDIT_REDEMPTION
        )
        is SiteNavigationAction.OpenFreeDomainSearch ->
            ActivityLauncher.viewPlanWithFreeDomainRegistrationActivityForResult(
                this,
                action.site,
                DomainRegistrationActivity.DomainRegistrationPurpose.FREE_DOMAIN_WITH_ANNUAL_PLAN
            )
        is SiteNavigationAction.OpenPaidDomainSearch -> ActivityLauncher.viewDomainRegistrationActivityForResult(
            this,
            action.site,
            DomainRegistrationActivity.DomainRegistrationPurpose.DOMAIN_PURCHASE
        )

        is SiteNavigationAction.AddNewSite ->
            AddSiteHandler.addSite(requireActivity(), action.hasAccessToken, action.source)
        is SiteNavigationAction.ShowQuickStartDialog -> showQuickStartDialog(
            action.title,
            action.message,
            action.positiveButtonLabel,
            action.negativeButtonLabel,
            action.isNewSite
        )
        is SiteNavigationAction.OpenQuickStartFullScreenDialog -> openQuickStartFullScreenDialog(action)
        is SiteNavigationAction.OpenDraftsPosts ->
            ActivityLauncher.viewCurrentBlogPostsOfType(requireActivity(), action.site, PostListType.DRAFTS)
        is SiteNavigationAction.OpenScheduledPosts ->
            ActivityLauncher.viewCurrentBlogPostsOfType(requireActivity(), action.site, PostListType.SCHEDULED)
        // The below navigation is temporary and as such not utilizing the 'action.postId' in order to navigate to the
        // 'Edit Post' screen. Instead, it fallbacks to navigating to the 'Posts' screen and targeting a specific tab.
        is SiteNavigationAction.EditDraftPost ->
            ActivityLauncher.viewCurrentBlogPostsOfType(requireActivity(), action.site, PostListType.DRAFTS)
        is SiteNavigationAction.EditScheduledPost ->
            ActivityLauncher.viewCurrentBlogPostsOfType(requireActivity(), action.site, PostListType.SCHEDULED)

        is SiteNavigationAction.OpenStatsByDay -> ActivityLauncher.viewBlogStatsForTimeframe(
            requireActivity(),
            action.site,
            StatsTimeframe.DAY,
            StatsLaunchedFrom.TODAY_STATS_CARD
        )

        is SiteNavigationAction.OpenExternalUrl ->
            ActivityLauncher.openUrlExternal(requireActivity(), action.url)
        is SiteNavigationAction.OpenUrlInWebView ->
            WPWebViewActivity.openURL(requireActivity(), action.url)
        is SiteNavigationAction.OpenDeepLink ->
            DeepLinkingIntentReceiverActivity.openDeepLinkUrl(requireActivity(), action.url)
        is SiteNavigationAction.OpenJetpackPoweredBottomSheet -> showJetpackPoweredBottomSheet()
        is SiteNavigationAction.OpenJetpackMigrationDeleteWP -> showJetpackMigrationDeleteWP()
        is SiteNavigationAction.OpenJetpackFeatureOverlay -> showJetpackFeatureOverlay(action.source)
        is SiteNavigationAction.OpenPromoteWithBlazeOverlay -> activityNavigator.openPromoteWithBlaze(
            requireActivity(),
            action.source,
            action.shouldShowBlazeOverlay
        )
        is SiteNavigationAction.ShowJetpackRemovalStaticPostersView -> {
            ActivityLauncher.showJetpackStaticPoster(requireActivity())
        }
        is SiteNavigationAction.OpenActivityLogDetail -> ActivityLauncher.viewActivityLogDetailFromDashboardCard(
            activity,
            action.site,
            action.activityId,
            action.isRewindable
        )
        is SiteNavigationAction.TriggerCreatePageFlow -> wpMainActivityViewModel.triggerCreatePageFlow()
        is SiteNavigationAction.OpenPagesDraftsTab -> ActivityLauncher.viewCurrentBlogPagesOfType(
            requireActivity(),
            action.site,
            PageListViewModel.PageListType.DRAFTS
        )
        is SiteNavigationAction.OpenPagesScheduledTab -> ActivityLauncher.viewCurrentBlogPagesOfType(
            requireActivity(),
            action.site,
            PageListViewModel.PageListType.SCHEDULED
        )

        is SiteNavigationAction.OpenCampaignListingPage -> activityNavigator.navigateToCampaignListingPage(
            requireActivity(),
            action.campaignListingPageSource
        )

        is SiteNavigationAction.OpenCampaignDetailPage -> activityNavigator.navigateToCampaignDetailPage(
            requireActivity(),
            action.campaignId,
            action.campaignDetailPageSource
        )

        is BloggingPromptCardNavigationAction -> handleNavigation(action)

        is SiteNavigationAction.OpenDashboardPersonalization -> activityNavigator.openDashboardPersonalization(
            requireActivity()
        )

        is SiteNavigationAction.OpenBloganuaryNudgeOverlay -> {
            BloganuaryNudgeLearnMoreOverlayFragment
                .newInstance(action.isPromptsEnabled)
                .show(requireActivity().supportFragmentManager, BloganuaryNudgeLearnMoreOverlayFragment.TAG)
        }

        is SiteNavigationAction.OpenSiteMonitoring -> activityNavigator.navigateToSiteMonitoring(
            requireActivity(),
            action.site
        )
    }

    private fun handleNavigation(action: BloggingPromptCardNavigationAction) {
        when (action) {
            is BloggingPromptCardNavigationAction.SharePrompt -> shareMessage(action.message)
            is BloggingPromptCardNavigationAction.AnswerPrompt -> {
                ActivityLauncher.addNewPostForResult(
                    activity,
                    action.selectedSite,
                    false,
                    PagePostCreationSourcesDetail.POST_FROM_MY_SITE,
                    action.promptId,
                    PostUtils.EntryPoint.MY_SITE_CARD_ANSWER_PROMPT
                )
            }
            is BloggingPromptCardNavigationAction.ViewAnswers -> {
                ReaderActivityLauncher.showReaderTagPreview(
                    activity,
                    action.readerTag,
                    ReaderTracker.SOURCE_BLOGGING_PROMPTS_VIEW_ANSWERS,
                    readerTracker,
                )
            }
            BloggingPromptCardNavigationAction.LearnMore ->
                (activity as? BloggingPromptsOnboardingListener)?.onShowBloggingPromptsOnboarding()
            is BloggingPromptCardNavigationAction.CardRemoved ->
                showBloggingPromptCardRemoveConfirmation(action.undoClick)
            BloggingPromptCardNavigationAction.ViewMore ->
                ActivityLauncher.showBloggingPromptsListActivity(activity)
        }
    }

    private fun showBloggingPromptCardRemoveConfirmation(undoClick: () -> Unit) {
        context?.run {
            val title = getString(R.string.my_site_blogging_prompt_card_removed_snackbar_title)
            val subtitle = HtmlCompat.fromHtml(
                getString(R.string.my_site_blogging_prompt_card_removed_snackbar_subtitle),
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
            val message = TitleSubtitleSnackbarSpannable.create(this, title, subtitle)

            val snackbarContent = SnackbarMessageHolder(
                message = UiString.UiStringText(message),
                buttonTitle = UiString.UiStringRes(R.string.undo),
                buttonAction = { undoClick() },
                isImportant = true
            )
            showSnackbar(snackbarContent)
        }
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

    private fun showJetpackFeatureOverlay(
        source: JetpackFeatureRemovalOverlayUtil.JetpackFeatureCollectionOverlaySource
    ) {
        JetpackFeatureFullScreenOverlayFragment
            .newInstance(
                isFeatureCollectionOverlay = true,
                featureCollectionOverlaySource = source
            )
            .show(requireActivity().supportFragmentManager, JetpackFeatureFullScreenOverlayFragment.TAG)
    }

    private fun openQuickStartFullScreenDialog(action: SiteNavigationAction.OpenQuickStartFullScreenDialog) {
        val bundle = QuickStartFullScreenDialogFragment.newBundle(action.type)
        FullScreenDialogFragment.Builder(requireContext())
            .setOnConfirmListener(this)
            .setOnDismissListener(this)
            .setContent(QuickStartFullScreenDialogFragment::class.java, bundle)
            .build()
            .show(requireActivity().supportFragmentManager, FullScreenDialogFragment.TAG)
    }

    private fun startCropActivity(imageUri: UriWrapper) {
        val context = activity ?: return
        val options = UCrop.Options()
        options.setShowCropGrid(false)
        options.setStatusBarColor(context.getColorFromAttribute(android.R.attr.statusBarColor))
        options.setToolbarColor(context.getColorFromAttribute(R.attr.wpColorAppBar))
        options.setToolbarWidgetColor(context.getColorFromAttribute(com.google.android.material.R.attr.colorOnSurface))
        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.NONE, UCropActivity.NONE)
        options.setHideBottomControls(true)
        UCrop.of(imageUri.uri, Uri.fromFile(File(context.cacheDir, "cropped_for_site_icon.jpg")))
            .withAspectRatio(1f, 1f)
            .withOptions(options)
            .start(requireActivity(), this)
    }

    private fun showQuickStartDialog(
        @StringRes title: Int,
        @StringRes message: Int,
        @StringRes positiveButtonLabel: Int,
        @StringRes negativeButtonLabel: Int,
        isNewSite: Boolean
    ) {
        val tag = TAG_QUICK_START_DIALOG
        val quickStartPromptDialogFragment = QuickStartPromptDialogFragment()
        quickStartPromptDialogFragment.initialize(
            tag,
            getString(title),
            getString(message),
            getString(positiveButtonLabel),
            R.drawable.img_illustration_site_about_280dp,
            getString(negativeButtonLabel),
            isNewSite
        )
        quickStartPromptDialogFragment.show(parentFragmentManager, tag)
        quickStartTracker.track(AnalyticsTracker.Stat.QUICK_START_REQUEST_VIEWED)
    }

    companion object {
        @JvmField
        var TAG: String = MySiteFragment::class.java.simpleName
        private const val KEY_LIST_STATE = "key_list_state"
        private const val KEY_NESTED_LISTS_STATES = "key_nested_lists_states"
        private const val TAG_QUICK_START_DIALOG = "TAG_QUICK_START_DIALOG"
        private const val FIRST_ITEM = 0
        fun newInstance(): MySiteFragment {
            return MySiteFragment()
        }
    }

    override fun onScrollToTop() {
        binding?.recyclerView?.smoothScrollToPosition(0)
    }
}
