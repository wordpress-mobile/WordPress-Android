package org.wordpress.android.ui.main

import android.app.Activity
import android.content.Intent
import android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.TooltipCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import com.wordpress.stories.compose.frame.FrameSaveNotifier.Companion.buildSnackbarErrorMessage
import com.wordpress.stories.compose.frame.StorySaveEvents.Companion.allErrorsInResult
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveProcessStart
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveResult
import com.wordpress.stories.compose.story.StoryRepository.getStoryAtIndex
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCrop.Options
import com.yalantis.ucrop.UCropActivity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_CREDIT_PROMPT_SHOWN
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_CREDIT_REDEMPTION_SUCCESS
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_CREDIT_REDEMPTION_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_CROPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_GALLERY_PICKED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_REMOVED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_SHOT_NEW
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_UPLOADED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_UPLOAD_UNSUCCESSFUL
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_ACTION_MEDIA_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_ACTION_PAGES_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_ACTION_POSTS_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_ACTION_STATS_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_REMOVE_DIALOG_NEGATIVE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_REMOVE_DIALOG_POSITIVE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_REQUEST_DIALOG_NEGATIVE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_REQUEST_DIALOG_NEUTRAL_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_REQUEST_DIALOG_POSITIVE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TASK_DIALOG_NEGATIVE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TASK_DIALOG_POSITIVE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TASK_DIALOG_VIEWED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STORY_SAVE_ERROR_SNACKBAR_MANAGE_TAPPED
import org.wordpress.android.databinding.MySiteFragmentBinding
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CHECK_STATS
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CREATE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.EDIT_HOMEPAGE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.ENABLE_POST_SHARING
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.EXPLORE_PLANS
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.REVIEW_PAGES
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPDATE_SITE_TITLE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPLOAD_SITE_ICON
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.VIEW_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.UNKNOWN
import org.wordpress.android.fluxc.store.SiteStore.OnPlansFetched
import org.wordpress.android.login.LoginMode.JETPACK_STATS
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.FullScreenDialogFragment
import org.wordpress.android.ui.FullScreenDialogFragment.Builder
import org.wordpress.android.ui.FullScreenDialogFragment.OnConfirmListener
import org.wordpress.android.ui.FullScreenDialogFragment.OnDismissListener
import org.wordpress.android.ui.PagePostCreationSourcesDetail.STORY_FROM_MY_SITE
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.TextInputDialogFragment
import org.wordpress.android.ui.accounts.LoginActivity
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.CTA_DOMAIN_CREDIT_REDEMPTION
import org.wordpress.android.ui.domains.DomainRegistrationResultFragment
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase.JetpackPurchasedProducts
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener
import org.wordpress.android.ui.main.utils.MeGravatarLoader
import org.wordpress.android.ui.mysite.MySiteViewModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.photopicker.MediaPickerConstants
import org.wordpress.android.ui.photopicker.MediaPickerConstants.EXTRA_MEDIA_SOURCE
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource.ANDROID_CAMERA
import org.wordpress.android.ui.plans.isDomainCreditAvailable
import org.wordpress.android.ui.plugins.PluginUtils
import org.wordpress.android.ui.posts.BasicFragmentDialog
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogNegativeClickInterface
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogOnDismissByOutsideTouchInterface
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface
import org.wordpress.android.ui.posts.QuickStartPromptDialogFragment.QuickStartPromptClickInterface
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.quickstart.QuickStartFullScreenDialogFragment
import org.wordpress.android.ui.quickstart.QuickStartMySitePrompts
import org.wordpress.android.ui.quickstart.QuickStartMySitePrompts.Companion.getPromptDetailsForTask
import org.wordpress.android.ui.quickstart.QuickStartMySitePrompts.Companion.isTargetingBottomNavBar
import org.wordpress.android.ui.quickstart.QuickStartNoticeDetails
import org.wordpress.android.ui.stories.StoriesMediaPickerResultHandler
import org.wordpress.android.ui.stories.StoriesTrackerHelper
import org.wordpress.android.ui.themes.ThemeBrowserUtils
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.uploads.UploadService.UploadErrorEvent
import org.wordpress.android.ui.uploads.UploadService.UploadMediaSuccessEvent
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.AccessibilityUtils.getSnackbarDuration
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.DOMAIN_REGISTRATION
import org.wordpress.android.util.AppLog.T.EDITOR
import org.wordpress.android.util.AppLog.T.MAIN
import org.wordpress.android.util.AppLog.T.UTILS
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.FluxCUtils
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.PhotonUtils.Quality.HIGH
import org.wordpress.android.util.QuickStartUtils
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration.SHORT
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.util.config.OnboardingImprovementsFeatureConfig
import org.wordpress.android.util.config.UnifiedCommentsListFeatureConfig
import org.wordpress.android.util.getColorFromAttribute
import org.wordpress.android.util.image.BlavatarShape.SQUARE
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.BLAVATAR
import org.wordpress.android.util.image.ImageType.USER
import org.wordpress.android.util.requestEmailValidation
import org.wordpress.android.util.setVisible
import org.wordpress.android.widgets.WPDialogSnackbar
import org.wordpress.android.widgets.WPSnackbar
import java.io.File
import java.util.GregorianCalendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Named

@Deprecated(
        "This class is being refactored, if you implement any change, please also update " +
                "{@link org.wordpress.android.ui.mysite.ImprovedMySiteFragment}"
)
@Suppress("LargeClass", "TooManyFunctions")
class MySiteFragment : Fragment(R.layout.my_site_fragment),
        OnScrollToTopListener,
        BasicDialogPositiveClickInterface,
        BasicDialogNegativeClickInterface,
        BasicDialogOnDismissByOutsideTouchInterface,
        QuickStartPromptClickInterface,
        OnConfirmListener,
        OnDismissListener,
        TextInputDialogFragment.Callback {
    private var binding: MySiteFragmentBinding? = null
    private var activeTutorialPrompt: QuickStartMySitePrompts? = null
    private val quickStartSnackBarHandler = Handler()
    private var blavatarSz = 0
    private var isDomainCreditAvailable = false
    private var isDomainCreditChecked = false
    private val job = Job()

    @Inject lateinit var accountStore: AccountStore
    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var mediaStore: MediaStore
    @Inject lateinit var quickStartStore: QuickStartStore
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var uploadUtilsWrapper: UploadUtilsWrapper
    @Inject lateinit var meGravatarLoader: MeGravatarLoader
    @Inject lateinit var storiesTrackerHelper: StoriesTrackerHelper
    @Inject lateinit var mediaPickerLauncher: MediaPickerLauncher
    @Inject lateinit var storiesMediaPickerResultHandler: StoriesMediaPickerResultHandler
    @Inject lateinit var selectedSiteRepository: SelectedSiteRepository
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var themeBrowserUtils: ThemeBrowserUtils
    @Inject lateinit var jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase
    @Inject lateinit var siteUtilsWrapper: SiteUtilsWrapper
    @Inject lateinit var quickStartUtilsWrapper: QuickStartUtilsWrapper
    @Inject lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Inject lateinit var buildConfigWrapper: BuildConfigWrapper
    @Inject lateinit var unifiedCommentsListFeatureConfig: UnifiedCommentsListFeatureConfig
    @Inject lateinit var onboardingImprovementsFeatureConfig: OnboardingImprovementsFeatureConfig
    @Inject @Named(UI_THREAD) lateinit var uiDispatcher: CoroutineDispatcher
    @Inject @Named(BG_THREAD) lateinit var bgDispatcher: CoroutineDispatcher
    lateinit var uiScope: CoroutineScope

    private val selectedSite: SiteModel?
        get() {
            return selectedSiteRepository.getSelectedSite()
        }
    private val selectedSiteLocalId: Int
        get() {
            return selectedSiteRepository.getSelectedSiteLocalId()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        (requireActivity().application as WordPress).component().inject(this)
        uiScope = CoroutineScope(uiDispatcher + job)
        if (savedInstanceState != null) {
            activeTutorialPrompt = savedInstanceState
                    .getSerializable(QuickStartMySitePrompts.KEY) as? QuickStartMySitePrompts
            isDomainCreditAvailable = savedInstanceState.getBoolean(
                    KEY_IS_DOMAIN_CREDIT_AVAILABLE,
                    false
            )
            isDomainCreditChecked = savedInstanceState.getBoolean(
                    KEY_DOMAIN_CREDIT_CHECKED,
                    false
            )
        }
    }

    override fun onDestroy() {
        selectedSiteRepository.clear()
        job.cancel()
        jetpackCapabilitiesUseCase.clear()
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onResume() {
        super.onResume()
        updateSiteSettingsIfNecessary()
        completeQuickStartStepsIfNeeded()
        // Site details may have changed (e.g. via Settings and returning to this Fragment) so update the UI
        binding?.apply {
            refreshSelectedSiteDetails(selectedSite)
            selectedSite?.let { site ->
                updateScanAndBackup(site)

                val isNotAdmin = !site.hasCapabilityManageOptions
                val isSelfHostedWithoutJetpack = !SiteUtils.isAccessedViaWPComRest(
                        site
                ) && !site.isJetpackConnected
                if (isNotAdmin || isSelfHostedWithoutJetpack || site.isWpForTeamsSite) {
                    rowActivityLog.visibility = View.GONE
                } else {
                    rowActivityLog.visibility = View.VISIBLE
                }

                siteInfoContainer.title.isClickable = SiteUtils.isAccessedViaWPComRest(site)
            }
            updateQuickStartContainer()
            showQuickStartNoticeIfNecessary()
        }
    }

    private fun MySiteFragmentBinding.updateScanAndBackup(site: SiteModel) {
        // Make sure that we load the cached value synchronously as we want to suppress the default animation
        updateScanAndBackupVisibility(
                site = site,
                products = jetpackCapabilitiesUseCase.getCachedJetpackPurchasedProducts(site.siteId)
        )
        uiScope.launch {
            val products = jetpackCapabilitiesUseCase.fetchJetpackPurchasedProducts(site.siteId)
            view?.let {
                updateScanAndBackupVisibility(
                        site = site,
                        products = products
                )
            }
        }
    }

    private fun MySiteFragmentBinding.updateScanAndBackupVisibility(
        site: SiteModel,
        products: JetpackPurchasedProducts
    ) {
        rowScan.setVisible(SiteUtils.isScanEnabled(products.scan, site))
        rowBackup.setVisible(products.backup)
    }

    private fun completeQuickStartStepsIfNeeded() {
        selectedSite?.let {
            if (it.showOnFront == ShowOnFront.POSTS.value) {
                quickStartUtilsWrapper.completeTaskAndRemindNextOne(
                        EDIT_HOMEPAGE,
                        it,
                        null,
                        requireContext()
                )
            }
        }
    }

    private fun showQuickStartNoticeIfNecessary() {
        if (!quickStartUtilsWrapper.isQuickStartInProgress(selectedSiteLocalId) ||
                !AppPrefs.isQuickStartNoticeRequired()) {
            return
        }
        val taskToPrompt = QuickStartUtils.getNextUncompletedQuickStartTask(
                quickStartStore,
                AppPrefs.getSelectedSite().toLong()
        )
        if (taskToPrompt != null) {
            quickStartSnackBarHandler.removeCallbacksAndMessages(null)
            quickStartSnackBarHandler.postDelayed({
                if (!isAdded || view == null || activity !is WPMainActivity) {
                    return@postDelayed
                }
                val noticeDetails = QuickStartNoticeDetails.getNoticeForTask(taskToPrompt) ?: return@postDelayed
                val noticeTitle = getString(noticeDetails.titleResId)
                val noticeMessage = getString(noticeDetails.messageResId)
                val quickStartNoticeSnackBar = WPDialogSnackbar.make(
                        requireActivity().findViewById(R.id.coordinator),
                        noticeMessage,
                        resources.getInteger(R.integer.quick_start_snackbar_duration_ms)
                )
                quickStartNoticeSnackBar.setTitle(noticeTitle)
                quickStartNoticeSnackBar.setPositiveButton(
                        getString(R.string.quick_start_button_positive)
                ) {
                    AnalyticsTracker.track(QUICK_START_TASK_DIALOG_POSITIVE_TAPPED)
                    activeTutorialPrompt = getPromptDetailsForTask(taskToPrompt)
                    showActiveQuickStartTutorial()
                }
                quickStartNoticeSnackBar
                        .setNegativeButton(
                                getString(R.string.quick_start_button_negative)
                        ) {
                            AppPrefs.setLastSkippedQuickStartTask(taskToPrompt)
                            AnalyticsTracker.track(
                                    QUICK_START_TASK_DIALOG_NEGATIVE_TAPPED
                            )
                        }
                (requireActivity() as WPMainActivity).showQuickStartSnackBar(quickStartNoticeSnackBar)
                AnalyticsTracker.track(QUICK_START_TASK_DIALOG_VIEWED)
                AppPrefs.setQuickStartNoticeRequired(false)
            }, AUTO_QUICK_START_SNACKBAR_DELAY_MS.toLong())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(QuickStartMySitePrompts.KEY, activeTutorialPrompt)
        outState.putBoolean(KEY_IS_DOMAIN_CREDIT_AVAILABLE, isDomainCreditAvailable)
        outState.putBoolean(KEY_DOMAIN_CREDIT_CHECKED, isDomainCreditChecked)
    }

    private fun updateSiteSettingsIfNecessary() {
        selectedSiteRepository.updateSiteSettingsIfNecessary()
    }

    override fun onPause() {
        super.onPause()
        clearActiveQuickStart()
    }

    private fun MySiteFragmentBinding.setupClickListeners() {
        siteInfoContainer.title.setOnClickListener {
            completeQuickStartTask(UPDATE_SITE_TITLE)
            showTitleChangerDialog()
        }
        siteInfoContainer.subtitle.setOnClickListener { viewSite() }
        switchSite.setOnClickListener { showSitePicker() }
        rowViewSite.setOnClickListener { viewSite() }
        mySiteRegisterDomainCta.setOnClickListener { registerDomain() }
        rowStats.setOnClickListener { viewStats() }
        mySiteBlavatar.setOnClickListener { updateBlavatar() }
        rowPlan.setOnClickListener {
            completeQuickStartTask(EXPLORE_PLANS)
            ActivityLauncher.viewBlogPlans(activity, selectedSite)
        }
        rowJetpackSettings.setOnClickListener { ActivityLauncher.viewJetpackSecuritySettings(activity, selectedSite) }
        rowBlogPosts.setOnClickListener { viewPosts() }
        rowMedia.setOnClickListener { viewMedia() }
        rowPages.setOnClickListener { viewPages() }
        rowComments.setOnClickListener {
            if (unifiedCommentsListFeatureConfig.isEnabled()) {
                ActivityLauncher.viewUnifiedComments(activity, selectedSite)
            } else {
                ActivityLauncher.viewCurrentBlogComments(activity, selectedSite)
            }
        }
        rowThemes.setOnClickListener {
            if (themeBrowserUtils.isAccessible(selectedSite)) {
                ActivityLauncher.viewCurrentBlogThemes(activity, selectedSite)
            }
        }
        rowPeople.setOnClickListener { ActivityLauncher.viewCurrentBlogPeople(activity, selectedSite) }
        rowPlugins.setOnClickListener { ActivityLauncher.viewPluginBrowser(activity, selectedSite) }
        rowActivityLog.setOnClickListener { ActivityLauncher.viewActivityLogList(activity, selectedSite) }
        rowBackup.setOnClickListener { ActivityLauncher.viewBackupList(activity, selectedSite) }
        rowScan.setOnClickListener { ActivityLauncher.viewScan(activity, selectedSite) }
        rowSettings.setOnClickListener { ActivityLauncher.viewBlogSettingsForResult(activity, selectedSite) }
        rowSharing.setOnClickListener {
            if (isQuickStartTaskActive(ENABLE_POST_SHARING)) requestNextStepOfActiveQuickStartTask()
            ActivityLauncher.viewBlogSharing(activity, selectedSite)
        }
        rowAdmin.setOnClickListener { ActivityLauncher.viewBlogAdmin(activity, selectedSite) }
        actionableEmptyView.button.setOnClickListener {
            SitePickerActivity.addSite(activity, accountStore.hasAccessToken())
        }
        quickStartCustomize.setOnClickListener { showQuickStartList(CUSTOMIZE) }
        quickStartGrow.setOnClickListener { showQuickStartList(GROW) }
        mySiteCardToolbarMore.setOnClickListener { showQuickStartCardMenu() }
    }

    private fun MySiteFragmentBinding.setupQuickActionsIfNecessary() {
        if (buildConfigWrapper.isJetpackApp) {
            quickActionButtonsContainer.visibility = View.GONE
            return
        }

        quickActionStatsButton.setOnClickListener {
            AnalyticsTracker.track(QUICK_ACTION_STATS_TAPPED)
            viewStats()
        }
        quickActionPostsButton.setOnClickListener {
            AnalyticsTracker.track(QUICK_ACTION_POSTS_TAPPED)
            viewPosts()
        }
        quickActionMediaButton.setOnClickListener {
            AnalyticsTracker.track(QUICK_ACTION_MEDIA_TAPPED)
            viewMedia()
        }
        quickActionPagesButton.setOnClickListener {
            AnalyticsTracker.track(QUICK_ACTION_PAGES_TAPPED)
            viewPages()
        }
    }

    private fun registerDomain() {
        AnalyticsUtils.trackWithSiteDetails(DOMAIN_CREDIT_REDEMPTION_TAPPED, selectedSite)
        ActivityLauncher.viewDomainRegistrationActivityForResult(activity, selectedSite, CTA_DOMAIN_CREDIT_REDEMPTION)
    }

    private fun viewMedia() {
        ActivityLauncher.viewCurrentBlogMedia(activity, selectedSite)
    }

    private fun updateBlavatar() {
        AnalyticsTracker.track(MY_SITE_ICON_TAPPED)
        val site = selectedSite
        if (site != null) {
            val hasIcon = site.iconUrl != null
            if (site.hasCapabilityManageOptions && site.hasCapabilityUploadFiles) {
                if (hasIcon) {
                    showChangeSiteIconDialog()
                } else {
                    showAddSiteIconDialog()
                }
                completeQuickStartTask(UPLOAD_SITE_ICON)
            } else {
                val message = when {
                    !site.isUsingWpComRestApi -> {
                        R.string.my_site_icon_dialog_change_requires_jetpack_message
                    }
                    hasIcon -> {
                        R.string.my_site_icon_dialog_change_requires_permission_message
                    }
                    else -> {
                        R.string.my_site_icon_dialog_add_requires_permission_message
                    }
                }
                showEditingSiteIconNotAllowedDialog(getString(message))
            }
        }
    }

    private fun viewPosts() {
        requestNextStepOfActiveQuickStartTask()
        val site = selectedSite
        if (site != null) {
            ActivityLauncher.viewCurrentBlogPosts(requireActivity(), site)
        } else {
            ToastUtils.showToast(activity, R.string.site_cannot_be_loaded)
        }
    }

    private fun viewPages() {
        if (activeTutorialPrompt != null && activeTutorialPrompt == QuickStartMySitePrompts.EDIT_HOMEPAGE) {
            requestNextStepOfActiveQuickStartTask()
        } else {
            completeQuickStartTask(REVIEW_PAGES)
        }
        val site = selectedSite
        if (site != null) {
            ActivityLauncher.viewCurrentBlogPages(requireActivity(), site)
        } else {
            ToastUtils.showToast(activity, R.string.site_cannot_be_loaded)
        }
    }

    private fun viewStats() {
        val site = selectedSite
        if (site != null) {
            completeQuickStartTask(CHECK_STATS)
            if (!accountStore.hasAccessToken() && site.isJetpackConnected) {
                // If the user is not connected to WordPress.com, ask him to connect first.
                startWPComLoginForJetpackStats()
            } else if (site.isWPCom || site.isJetpackInstalled && site
                            .isJetpackConnected) {
                ActivityLauncher.viewBlogStats(activity, site)
            } else {
                ActivityLauncher.viewConnectJetpackForStats(activity, site)
            }
        }
    }

    private fun MySiteFragmentBinding.showTitleChangerDialog() {
        if (!NetworkUtils.isNetworkAvailable(activity)) {
            WPSnackbar.make(
                    requireActivity().findViewById(R.id.coordinator),
                    R.string.error_network_connection,
                    getSnackbarDuration(activity, Snackbar.LENGTH_SHORT)
            ).show()
            return
        }

        val canEditTitle = SiteUtils.isAccessedViaWPComRest(selectedSite!!) &&
                selectedSite?.hasCapabilityManageOptions!!
        val hint = if (canEditTitle) {
            getString(R.string.my_site_title_changer_dialog_hint)
        } else {
            getString(R.string.my_site_title_changer_dialog_not_allowed_hint)
        }

        val inputDialog = TextInputDialogFragment.newInstance(
                getString(R.string.my_site_title_changer_dialog_title),
                selectedSite?.name,
                hint,
                false,
                canEditTitle,
                siteInfoContainer.title.id
        )
        inputDialog.setTargetFragment(this@MySiteFragment, 0)
        inputDialog.show(parentFragmentManager, TextInputDialogFragment.TAG)
    }

    private fun viewSite() {
        completeQuickStartTask(VIEW_SITE)
        ActivityLauncher.viewCurrentSite(activity, selectedSite, true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        blavatarSz = resources.getDimensionPixelSize(R.dimen.blavatar_sz_small)
        binding = MySiteFragmentBinding.bind(view).apply { onBind() }
    }

    private fun MySiteFragmentBinding.onBind() {
        setupQuickActionsIfNecessary()
        setupClickListeners()
        collapsingToolbar.title = getString(R.string.my_site_section_screen_title)

        val avatar = root.findViewById<ImageView>(R.id.avatar)

        appbarMain.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
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
        (activity as AppCompatActivity).setSupportActionBar(toolbarMain)
        if (activeTutorialPrompt != null) {
            showQuickStartFocusPoint()
        }
        selectedSiteRepository.selectedSiteChange.observe(viewLifecycleOwner, {
            onSiteChanged(it)
        })
        selectedSiteRepository.showSiteIconProgressBar.observe(viewLifecycleOwner, {
            showSiteIconProgressBar(it == true)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.my_site_menu, menu)
        binding?.apply {
            val meMenu = toolbarMain.menu.findItem(R.id.me_item)
            val actionView = meMenu.actionView
            actionView.setOnClickListener {
                ActivityLauncher.viewMeActivityForResult(
                        activity
                )
            }
            actionView.let {
                TooltipCompat.setTooltipText(it, meMenu.title)
            }

            refreshMeGravatar(actionView.findViewById(R.id.avatar))
        }
    }

    private fun refreshMeGravatar(gravatarImageView: ImageView) {
        val avatarUrl = meGravatarLoader.constructGravatarUrl(accountStore.account.avatarUrl)
        meGravatarLoader.load(
                false,
                avatarUrl,
                null,
                gravatarImageView,
                USER,
                null
        )
    }

    private fun MySiteFragmentBinding.updateQuickStartContainer() {
        if (!isAdded) {
            return
        }
        if (quickStartUtilsWrapper.isQuickStartInProgress(selectedSiteLocalId)) {
            val selectedSiteLocalId: Long = selectedSiteRepository.getSelectedSiteLocalId().toLong()
            val countCustomizeCompleted = quickStartStore.getCompletedTasksByType(
                    selectedSiteLocalId,
                    CUSTOMIZE
            ).size
            val countCustomizeUncompleted = quickStartStore.getUncompletedTasksByType(
                    selectedSiteLocalId,
                    CUSTOMIZE
            ).size
            val countGrowCompleted = quickStartStore.getCompletedTasksByType(
                    selectedSiteLocalId,
                    GROW
            ).size
            val countGrowUncompleted = quickStartStore.getUncompletedTasksByType(
                    selectedSiteLocalId,
                    GROW
            ).size
            if (countCustomizeUncompleted > 0) {
                quickStartCustomizeIcon.isEnabled = true
                quickStartCustomizeTitle.isEnabled = true
                val updatedPaintFlags = quickStartCustomizeTitle.paintFlags and STRIKE_THRU_TEXT_FLAG.inv()
                quickStartCustomizeTitle.paintFlags = updatedPaintFlags
            } else {
                quickStartCustomizeIcon.isEnabled = false
                quickStartCustomizeTitle.isEnabled = false
                quickStartCustomizeTitle.paintFlags = quickStartCustomizeTitle.paintFlags or STRIKE_THRU_TEXT_FLAG
            }
            quickStartCustomizeSubtitle.text = getString(
                    R.string.quick_start_sites_type_subtitle,
                    countCustomizeCompleted, countCustomizeCompleted + countCustomizeUncompleted
            )
            if (countGrowUncompleted > 0) {
                quickStartGrowIcon.setBackgroundResource(R.drawable.bg_oval_blue_50_multiple_users_white_40dp)
                quickStartGrowTitle.isEnabled = true
                quickStartGrowTitle.paintFlags = quickStartGrowTitle.paintFlags and STRIKE_THRU_TEXT_FLAG.inv()
            } else {
                quickStartGrowIcon.setBackgroundResource(R.drawable.bg_oval_neutral_30_multiple_users_white_40dp)
                quickStartGrowTitle.isEnabled = false
                quickStartGrowTitle.paintFlags = quickStartGrowTitle.paintFlags or STRIKE_THRU_TEXT_FLAG
            }
            quickStartGrowSubtitle.text = getString(
                    R.string.quick_start_sites_type_subtitle,
                    countGrowCompleted, countGrowCompleted + countGrowUncompleted
            )
            quickStart.visibility = View.VISIBLE
        } else {
            quickStart.visibility = View.GONE
        }
    }

    private fun MySiteFragmentBinding.showQuickStartCardMenu() {
        val quickStartPopupMenu = PopupMenu(
                requireContext(),
                mySiteCardToolbarMore
        )
        quickStartPopupMenu.setOnMenuItemClickListener { item: MenuItem ->
            if (item.itemId == R.id.quick_start_card_menu_remove) {
                showRemoveNextStepsDialog()
                return@setOnMenuItemClickListener true
            }
            false
        }
        quickStartPopupMenu.inflate(R.menu.quick_start_card_menu)
        quickStartPopupMenu.show()
    }

    private fun showQuickStartList(type: QuickStartTaskType) {
        clearActiveQuickStart()
        val bundle = QuickStartFullScreenDialogFragment.newBundle(type)
        when (type) {
            CUSTOMIZE -> Builder(requireContext())
                    .setTitle(R.string.quick_start_sites_type_customize)
                    .setOnConfirmListener(this)
                    .setOnDismissListener(this)
                    .setContent(QuickStartFullScreenDialogFragment::class.java, bundle)
                    .build()
                    .show(requireActivity().supportFragmentManager, FullScreenDialogFragment.TAG)
            GROW -> Builder(requireContext())
                    .setTitle(R.string.quick_start_sites_type_grow)
                    .setOnConfirmListener(this)
                    .setOnDismissListener(this)
                    .setContent(QuickStartFullScreenDialogFragment::class.java, bundle)
                    .build()
                    .show(requireActivity().supportFragmentManager, FullScreenDialogFragment.TAG)
            UNKNOWN -> {
            }
        }
    }

    private fun showAddSiteIconDialog() {
        val dialog = BasicFragmentDialog()
        val tag = TAG_ADD_SITE_ICON_DIALOG
        dialog.initialize(
                tag, getString(R.string.my_site_icon_dialog_title),
                getString(R.string.my_site_icon_dialog_add_message),
                getString(R.string.yes),
                getString(R.string.no),
                null
        )
        dialog.show(requireActivity().supportFragmentManager, tag)
    }

    private fun showChangeSiteIconDialog() {
        val dialog = BasicFragmentDialog()
        val tag = TAG_CHANGE_SITE_ICON_DIALOG
        dialog.initialize(
                tag, getString(R.string.my_site_icon_dialog_title),
                getString(R.string.my_site_icon_dialog_change_message),
                getString(R.string.my_site_icon_dialog_change_button),
                getString(R.string.my_site_icon_dialog_remove_button),
                getString(R.string.my_site_icon_dialog_cancel_button)
        )
        dialog.show(requireActivity().supportFragmentManager, tag)
    }

    private fun showEditingSiteIconNotAllowedDialog(message: String) {
        val dialog = BasicFragmentDialog()
        val tag = TAG_EDIT_SITE_ICON_NOT_ALLOWED_DIALOG
        dialog.initialize(
                tag, getString(R.string.my_site_icon_dialog_title),
                message,
                getString(R.string.dialog_button_ok),
                null,
                null
        )
        dialog.show(requireActivity().supportFragmentManager, tag)
    }

    private fun showRemoveNextStepsDialog() {
        val dialog = BasicFragmentDialog()
        val tag = TAG_REMOVE_NEXT_STEPS_DIALOG
        dialog.initialize(
                tag, getString(R.string.quick_start_dialog_remove_next_steps_title),
                getString(R.string.quick_start_dialog_remove_next_steps_message),
                getString(R.string.remove),
                getString(R.string.cancel),
                null
        )
        dialog.show(requireActivity().supportFragmentManager, tag)
    }

    private fun startWPComLoginForJetpackStats() {
        val loginIntent = Intent(activity, LoginActivity::class.java)
        JETPACK_STATS.putInto(loginIntent)
        startActivityForResult(loginIntent, RequestCodes.DO_LOGIN)
    }

    private fun showSitePicker() {
        if (isAdded) {
            ActivityLauncher.showSitePickerForResult(activity, selectedSite)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RequestCodes.DO_LOGIN -> if (resultCode == Activity.RESULT_OK) {
                ActivityLauncher.viewBlogStats(activity, selectedSite)
            }
            RequestCodes.SITE_PICKER -> if (resultCode == Activity.RESULT_OK) {
                // reset domain credit flag - it will be checked in onSiteChanged
                isDomainCreditAvailable = false
            }
            RequestCodes.PHOTO_PICKER -> if (resultCode == Activity.RESULT_OK && data != null) {
                if (!storiesMediaPickerResultHandler.handleMediaPickerResultForStories(
                                data, activity, selectedSite, STORY_FROM_MY_SITE
                        )
                ) {
                    if (data.hasExtra(MediaPickerConstants.EXTRA_MEDIA_ID)) {
                        val mediaId = data.getLongExtra(MediaPickerConstants.EXTRA_MEDIA_ID, 0).toInt()
                        updateSiteIconMediaId(mediaId, true)
                    } else {
                        val mediaUriStringsArray = data.getStringArrayExtra(MediaPickerConstants.EXTRA_MEDIA_URIS)
                        if (mediaUriStringsArray.isNullOrEmpty()) {
                            AppLog.e(UTILS, "Can't resolve picked or captured image")
                            return
                        }

                        val source = PhotoPickerMediaSource.fromString(data.getStringExtra(EXTRA_MEDIA_SOURCE))
                        val stat = if (source == ANDROID_CAMERA) MY_SITE_ICON_SHOT_NEW else MY_SITE_ICON_GALLERY_PICKED
                        AnalyticsTracker.track(stat)
                        val imageUri = Uri.parse(mediaUriStringsArray[0])
                        if (imageUri != null) {
                            val didGoWell = WPMediaUtils.fetchMediaAndDoNext(activity, imageUri) { uri: Uri ->
                                selectedSiteRepository.showSiteIconProgressBar(true)
                                startCropActivity(uri)
                            }
                            if (!didGoWell) {
                                AppLog.e(UTILS, "Can't download picked or captured image")
                            }
                        }
                    }
                }
            }
            RequestCodes.STORIES_PHOTO_PICKER -> if (resultCode == Activity.RESULT_OK && data != null) {
                storiesMediaPickerResultHandler.handleMediaPickerResultForStories(
                        data,
                        activity,
                        selectedSite,
                        STORY_FROM_MY_SITE
                )
            }
            UCrop.REQUEST_CROP -> if (resultCode == Activity.RESULT_OK) {
                AnalyticsTracker.track(MY_SITE_ICON_CROPPED)
                WPMediaUtils.fetchMediaAndDoNext(activity, UCrop.getOutput(data!!)) { uri: Uri? ->
                    startSiteIconUpload(MediaUtils.getRealPathFromURI(activity, uri))
                }
            } else if (resultCode == UCrop.RESULT_ERROR) {
                AppLog.e(MAIN, "Image cropping failed!", UCrop.getError(data!!))
                ToastUtils.showToast(activity, R.string.error_cropping_image, SHORT)
            }
            RequestCodes.DOMAIN_REGISTRATION -> if (resultCode == Activity.RESULT_OK && isAdded && data != null) {
                AnalyticsTracker.track(DOMAIN_CREDIT_REDEMPTION_SUCCESS)
                val email = data.getStringExtra(DomainRegistrationResultFragment.RESULT_REGISTERED_DOMAIN_EMAIL)
                requestEmailValidation(requireContext(), email)
            }
        }
    }

    override fun onConfirm(result: Bundle?) {
        if (result != null) {
            val task = result.getSerializable(QuickStartFullScreenDialogFragment.RESULT_TASK) as? QuickStartTask
            if (task == null || task == CREATE_SITE) {
                return
            }

            // Remove existing quick start indicator, if necessary.
            if (activeTutorialPrompt != null) {
                removeQuickStartFocusPoint()
            }
            activeTutorialPrompt = getPromptDetailsForTask(task)
            showActiveQuickStartTutorial()
        }
    }

    override fun onDismiss() {
        binding?.updateQuickStartContainer()
    }

    private fun startSiteIconUpload(filePath: String) {
        if (TextUtils.isEmpty(filePath)) {
            ToastUtils.showToast(
                    activity,
                    R.string.error_locating_image,
                    SHORT
            )
            return
        }
        val file = File(filePath)
        if (!file.exists()) {
            ToastUtils.showToast(
                    activity,
                    R.string.file_error_create,
                    SHORT
            )
            return
        }
        val site = selectedSite
        if (site != null) {
            val media = buildMediaModel(file, site)
            if (media == null) {
                ToastUtils.showToast(
                        activity,
                        R.string.file_not_found,
                        SHORT
                )
                return
            }
            UploadService.uploadMedia(activity, media)
        } else {
            ToastUtils.showToast(
                    activity,
                    R.string.error_generic,
                    SHORT
            )
            AppLog.e(
                    MAIN,
                    "Unexpected error - Site icon upload failed, because there wasn't any site selected."
            )
        }
    }

    private fun MySiteFragmentBinding.showSiteIconProgressBar(isVisible: Boolean) {
        if (isVisible) {
            mySiteIconProgress.visibility = View.VISIBLE
            mySiteBlavatar.visibility = View.INVISIBLE
        } else {
            mySiteIconProgress.visibility = View.GONE
            mySiteBlavatar.visibility = View.VISIBLE
        }
    }

    private val isMediaUploadInProgress: Boolean
        get() = binding?.mySiteIconProgress?.visibility == View.VISIBLE

    private fun buildMediaModel(file: File, site: SiteModel): MediaModel? {
        val uri = Uri.Builder().path(file.path).build()
        val mimeType = requireActivity().contentResolver.getType(uri)
        return FluxCUtils.mediaModelFromLocalUri(requireActivity(), uri, mimeType, mediaStore, site.id)
    }

    private fun startCropActivity(uri: Uri) {
        val context = activity ?: return
        val options = Options()
        options.setShowCropGrid(false)
        options.setStatusBarColor(context.getColorFromAttribute(android.R.attr.statusBarColor))
        options.setToolbarColor(context.getColorFromAttribute(R.attr.wpColorAppBar))
        options.setToolbarWidgetColor(context.getColorFromAttribute(R.attr.colorOnSurface))
        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.NONE, UCropActivity.NONE)
        options.setHideBottomControls(true)
        UCrop.of(uri, Uri.fromFile(File(context.cacheDir, "cropped_for_site_icon.jpg")))
                .withAspectRatio(1f, 1f)
                .withOptions(options)
                .start(requireActivity(), this)
    }

    private fun MySiteFragmentBinding.refreshSelectedSiteDetails(site: SiteModel?) {
        if (!isAdded || view == null) {
            return
        }
        if (site == null) {
            showEmptyView()
        } else {
            showContent(site)
        }
    }

    private fun MySiteFragmentBinding.showEmptyView() {
        scrollView.visibility = View.GONE
        actionableEmptyView.visibility = View.VISIBLE

        // Hide actionable empty view image when screen height is under 600 pixels.
        if (DisplayUtils.getDisplayPixelHeight(activity) >= 600) {
            actionableEmptyView.image.visibility = View.VISIBLE
        } else {
            actionableEmptyView.image.visibility = View.GONE
        }
    }

    private fun MySiteFragmentBinding.showContent(site: SiteModel) {
        showDomainRegistrationIfNeeded(site)

        scrollView.visibility = View.VISIBLE
        actionableEmptyView.visibility = View.GONE
        toggleAdminVisibility(site)
        val themesVisibility = if (themeBrowserUtils.isAccessible(site)) View.VISIBLE else View.GONE
        mySiteLookAndFeelHeader.visibility = themesVisibility
        rowThemes.visibility = themesVisibility

        // sharing is only exposed for sites accessed via the WPCOM REST API (wpcom or Jetpack)
        val sharingVisibility = if (site.supportsSharing()) View.VISIBLE else View.GONE
        rowSharing.visibility = sharingVisibility

        // show settings for all self-hosted to expose Delete Site
        val isAdminOrSelfHosted = site.hasCapabilityManageOptions || !SiteUtils.isAccessedViaWPComRest(site)
        rowSettings.visibility = if (isAdminOrSelfHosted) View.VISIBLE else View.GONE
        rowPeople.visibility = if (site.hasCapabilityListUsers) View.VISIBLE else View.GONE
        rowPlugins.visibility = if (PluginUtils.isPluginFeatureAvailable(site)) View.VISIBLE else View.GONE

        // if either people or settings is visible, configuration header should be visible
        val settingsVisibility = if (isAdminOrSelfHosted || site.hasCapabilityListUsers) View.VISIBLE else View.GONE
        mySiteConfigurationHeader.visibility = settingsVisibility
        imageManager.load(
                mySiteBlavatar,
                SiteUtils.getSiteImageType(site.isWpForTeamsSite, SQUARE),
                SiteUtils.getSiteIconUrl(site, blavatarSz)
        )
        val homeUrl = SiteUtils.getHomeURLOrHostName(site)
        val blogTitle = SiteUtils.getSiteNameOrHomeURL(site)
        siteInfoContainer.title.text = blogTitle
        siteInfoContainer.subtitle.text = homeUrl

        // Hide the Plan item if the Plans feature is not available for this blog
        showPlanIfNeeded(site)

        val jetpackSectionVisible = site.isJetpackConnected && // jetpack is installed and connected
                !site.isWPComAtomic // isn't atomic site

        val jetpackSettingsVisible = jetpackSectionVisible &&
                SiteUtils.isAccessedViaWPComRest(site) && // is using .com login
                site.hasCapabilityManageOptions // has permissions to manage the site

        uiHelpers.updateVisibility(rowLabelJetpack, jetpackSectionVisible)
        uiHelpers.updateVisibility(rowJetpackSettings, jetpackSettingsVisible)

        // Do not show pages menu item to Collaborators.
        val pageVisibility = if (site.isSelfHostedAdmin || site.hasCapabilityEditPages) View.VISIBLE else View.GONE
        rowPages.visibility = pageVisibility
        quickActionPagesContainer.visibility = pageVisibility
        middleQuickActionSpacing.visibility = pageVisibility
    }

    private fun MySiteFragmentBinding.showDomainRegistrationIfNeeded(site: SiteModel) {
        if (SiteUtils.onFreePlan(site) || SiteUtils.hasCustomDomain(site)) {
            isDomainCreditAvailable = false
            toggleDomainRegistrationCtaVisibility()
        } else if (!isDomainCreditChecked) {
            fetchSitePlans(site)
        } else {
            toggleDomainRegistrationCtaVisibility()
        }
    }

    private fun MySiteFragmentBinding.showPlanIfNeeded(site: SiteModel) {
        val planShortName = site.planShortName
        if (!TextUtils.isEmpty(planShortName) && site.hasCapabilityManageOptions && !site.isWpForTeamsSite) {
            if (site.isWPCom || site.isAutomatedTransfer) {
                mySiteCurrentPlanTextView.text = planShortName
                rowPlan.visibility = View.VISIBLE
            } else {
                // TODO: Support Jetpack plans
                rowPlan.visibility = View.GONE
            }
        } else {
            rowPlan.visibility = View.GONE
        }
    }

    private fun MySiteFragmentBinding.toggleAdminVisibility(site: SiteModel?) {
        if (site == null) {
            return
        }
        if (shouldHideWPAdmin(site)) {
            rowAdmin.visibility = View.GONE
        } else {
            rowAdmin.visibility = View.VISIBLE
        }
    }

    private fun shouldHideWPAdmin(site: SiteModel?): Boolean {
        if (site == null) {
            return false
        }
        return if (!site.isWPCom) {
            false
        } else {
            val dateCreated = DateTimeUtils.dateFromIso8601(
                    accountStore.account
                            .date
            )
            val calendar = GregorianCalendar(
                    HIDE_WP_ADMIN_YEAR, HIDE_WP_ADMIN_MONTH,
                    HIDE_WP_ADMIN_DAY
            )
            calendar.timeZone = TimeZone.getTimeZone(MySiteViewModel.HIDE_WP_ADMIN_GMT_TIME_ZONE)
            dateCreated != null && dateCreated.after(calendar.time)
        }
    }

    override fun onScrollToTop() {
        if (isAdded) {
            binding?.scrollView?.smoothScrollTo(0, 0)
        }
    }

    override fun onStop() {
        dispatcher.unregister(this)
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
        EventBus.getDefault().register(this)
    }

    /**
     * We can't just use fluxc OnSiteChanged event, as the order of events is not guaranteed -> getSelectedSite()
     * method might return an out of date SiteModel, if the OnSiteChanged event handler in the WPMainActivity wasn't
     * called yet.
     */
    private fun MySiteFragmentBinding.onSiteChanged(site: SiteModel?) {
        // whenever site changes we hide CTA and check for credit in refreshSelectedSiteDetails()
        isDomainCreditChecked = false
        refreshSelectedSiteDetails(site)
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: UploadErrorEvent) {
        AnalyticsTracker.track(MY_SITE_ICON_UPLOAD_UNSUCCESSFUL)
        EventBus.getDefault().removeStickyEvent(event)
        if (isMediaUploadInProgress) {
            selectedSiteRepository.showSiteIconProgressBar(false)
        }
        val site = selectedSite
        if (site != null && event.post != null) {
            if (event.post.localSiteId == site.id) {
                uploadUtilsWrapper.onPostUploadedSnackbarHandler(
                        activity,
                        requireActivity().findViewById(R.id.coordinator), true, false,
                        event.post, event.errorMessage, site
                )
            }
        } else if (event.mediaModelList != null && event.mediaModelList.isNotEmpty()) {
            uploadUtilsWrapper.onMediaUploadedSnackbarHandler(
                    activity,
                    requireActivity().findViewById(R.id.coordinator), true,
                    event.mediaModelList, site, event.errorMessage
            )
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: UploadMediaSuccessEvent) {
        AnalyticsTracker.track(MY_SITE_ICON_UPLOADED)
        EventBus.getDefault().removeStickyEvent(event)
        val site = selectedSite
        if (site != null) {
            binding?.handleUploadMediaSuccessEvent(site, event)
        }
    }

    private fun MySiteFragmentBinding.handleUploadMediaSuccessEvent(site: SiteModel, event: UploadMediaSuccessEvent) {
        if (isMediaUploadInProgress) {
            if (event.mediaModelList.size > 0) {
                val media = event.mediaModelList[0]
                imageManager.load(
                        mySiteBlavatar,
                        BLAVATAR,
                        PhotonUtils.getPhotonImageUrl(
                                media.url,
                                blavatarSz,
                                blavatarSz,
                                HIGH,
                                site.isPrivateWPComAtomic
                        )
                )
                updateSiteIconMediaId(media.mediaId.toInt(), false)
            } else {
                AppLog.w(MAIN, "Site icon upload completed, but mediaList is empty.")
            }
            selectedSiteRepository.showSiteIconProgressBar(false)
        } else {
            if (event.mediaModelList != null && event.mediaModelList.isNotEmpty()) {
                uploadUtilsWrapper.onMediaUploadedSnackbarHandler(
                        activity,
                        requireActivity().findViewById(R.id.coordinator), false,
                        event.mediaModelList, site, event.successMessage
                )
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: StorySaveResult) {
        EventBus.getDefault().removeStickyEvent(event)
        if (!event.isSuccess()) {
            // note: no tracking added here as we'll perform tracking in StoryMediaSaveUploadBridge
            val errorText = String.format(
                    getString(R.string.story_saving_snackbar_finished_with_error),
                    getStoryAtIndex(event.storyIndex).title
            )
            val snackbarMessage = buildSnackbarErrorMessage(
                    requireActivity(),
                    allErrorsInResult(event.frameSaveResult).size,
                    errorText
            )
            uploadUtilsWrapper.showSnackbarError(
                    requireActivity().findViewById<View>(R.id.coordinator),
                    snackbarMessage,
                    R.string.story_saving_failed_quick_action_manage
            ) {
                // TODO WPSTORIES add TRACKS: the putExtra described here below for NOTIFICATION_TYPE
                // is meant to be used for tracking purposes. Use it!
                // TODO add NotificationType.MEDIA_SAVE_ERROR param later when integrating with WPAndroid
                //        val notificationType = NotificationType.MEDIA_SAVE_ERROR
                //        notificationIntent.putExtra(ARG_NOTIFICATION_TYPE, notificationType)

                storiesTrackerHelper.trackStorySaveResultEvent(
                        event,
                        STORY_SAVE_ERROR_SNACKBAR_MANAGE_TAPPED

                )
                ActivityLauncher.viewStories(requireActivity(), selectedSite, event)
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onStorySaveStart(event: StorySaveProcessStart) {
        EventBus.getDefault().removeStickyEvent(event)
        val snackbarMessage = String.format(
                getString(R.string.story_saving_snackbar_started),
                getStoryAtIndex(event.storyIndex).title
        )
        uploadUtilsWrapper.showSnackbar(
                requireActivity().findViewById<View>(R.id.coordinator),
                snackbarMessage
        )
    }

    override fun onPositiveClicked(instanceTag: String) {
        when (instanceTag) {
            TAG_ADD_SITE_ICON_DIALOG, TAG_CHANGE_SITE_ICON_DIALOG -> mediaPickerLauncher.showSiteIconPicker(
                    requireActivity(),
                    selectedSite
            )
            TAG_EDIT_SITE_ICON_NOT_ALLOWED_DIALOG -> {
            }
            TAG_QUICK_START_DIALOG -> {
                startQuickStart()
                AnalyticsTracker.track(QUICK_START_REQUEST_DIALOG_POSITIVE_TAPPED)
            }
            TAG_REMOVE_NEXT_STEPS_DIALOG -> {
                AnalyticsTracker.track(QUICK_START_REMOVE_DIALOG_POSITIVE_TAPPED)
                skipQuickStart()
                binding?.updateQuickStartContainer()
                clearActiveQuickStart()
            }
            else -> {
                AppLog.e(
                        EDITOR,
                        "Dialog instanceTag is not recognized"
                )
                throw UnsupportedOperationException("Dialog instanceTag is not recognized")
            }
        }
    }

    private fun skipQuickStart() {
        val selectedSiteLocalId: Long = selectedSiteRepository.getSelectedSiteLocalId().toLong()
        for (quickStartTask in QuickStartTask.values()) {
            quickStartStore.setDoneTask(
                    selectedSiteLocalId,
                    quickStartTask,
                    true
            )
        }
        quickStartStore.setQuickStartCompleted(selectedSiteLocalId, true)
        // skipping all tasks means no achievement notification, so we mark it as received
        quickStartStore.setQuickStartNotificationReceived(selectedSiteLocalId, true)
    }

    private fun startQuickStart() {
        quickStartUtilsWrapper.startQuickStart(selectedSiteLocalId)
        binding?.updateQuickStartContainer()
    }

    private fun MySiteFragmentBinding.toggleDomainRegistrationCtaVisibility() {
        if (isDomainCreditAvailable) {
            // we nest this check because of some weirdness with ui state and race conditions
            if (mySiteRegisterDomainCta.visibility != View.VISIBLE) {
                AnalyticsTracker.track(DOMAIN_CREDIT_PROMPT_SHOWN)
                mySiteRegisterDomainCta.visibility = View.VISIBLE
            }
        } else {
            mySiteRegisterDomainCta.visibility = View.GONE
        }
    }

    override fun onNegativeClicked(instanceTag: String) {
        when (instanceTag) {
            TAG_ADD_SITE_ICON_DIALOG -> showQuickStartNoticeIfNecessary()
            TAG_CHANGE_SITE_ICON_DIALOG -> {
                AnalyticsTracker.track(MY_SITE_ICON_REMOVED)
                updateSiteIconMediaId(0, true)
            }
            TAG_QUICK_START_DIALOG -> AnalyticsTracker.track(QUICK_START_REQUEST_DIALOG_NEGATIVE_TAPPED)
            TAG_REMOVE_NEXT_STEPS_DIALOG -> AnalyticsTracker.track(QUICK_START_REMOVE_DIALOG_NEGATIVE_TAPPED)
            else -> {
                AppLog.e(
                        EDITOR,
                        "Dialog instanceTag '$instanceTag' is not recognized"
                )
                throw UnsupportedOperationException("Dialog instanceTag is not recognized")
            }
        }
    }

    override fun onNeutralClicked(instanceTag: String) {
        if (onboardingImprovementsFeatureConfig.isEnabled()) return

        if (TAG_QUICK_START_DIALOG == instanceTag) {
            AppPrefs.setQuickStartDisabled(true)
            AnalyticsTracker.track(QUICK_START_REQUEST_DIALOG_NEUTRAL_TAPPED)
        } else {
            AppLog.e(
                    EDITOR,
                    "Dialog instanceTag '$instanceTag' is not recognized"
            )
            throw UnsupportedOperationException("Dialog instanceTag is not recognized")
        }
    }

    override fun onDismissByOutsideTouch(instanceTag: String) {
        when (instanceTag) {
            TAG_ADD_SITE_ICON_DIALOG -> showQuickStartNoticeIfNecessary()
            TAG_CHANGE_SITE_ICON_DIALOG,
            TAG_EDIT_SITE_ICON_NOT_ALLOWED_DIALOG,
            TAG_QUICK_START_DIALOG,
            TAG_REMOVE_NEXT_STEPS_DIALOG -> {
            }
            else -> {
                AppLog.e(
                        EDITOR,
                        "Dialog instanceTag '$instanceTag' is not recognized"
                )
                throw UnsupportedOperationException("Dialog instanceTag is not recognized")
            }
        }
    }

    private fun fetchSitePlans(site: SiteModel?) {
        dispatcher.dispatch(SiteActionBuilder.newFetchPlansAction(site))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlansFetched(event: OnPlansFetched) {
        if (selectedSite?.id != event.site.id) {
            return
        }
        if (event.isError) {
            AppLog.e(
                    DOMAIN_REGISTRATION,
                    "An error occurred while fetching plans : " + event.error.message
            )
        } else {
            isDomainCreditChecked = true
            isDomainCreditAvailable = isDomainCreditAvailable(event.plans)
            binding?.toggleDomainRegistrationCtaVisibility()
        }
    }

    private val mAddQuickStartFocusPointTask = Runnable { // technically there is no situation (yet) where fragment is not added but we need to show focus point
        if (!isAdded) {
            return@Runnable
        }
        val parentView = requireActivity().findViewById<ViewGroup>(activeTutorialPrompt!!.parentContainerId)
        val quickStartTarget = requireActivity().findViewById<View>(
                activeTutorialPrompt!!.focusedContainerId
        )
        if (quickStartTarget == null || parentView == null) {
            return@Runnable
        }
        val focusPointSize = resources.getDimensionPixelOffset(R.dimen.quick_start_focus_point_size)
        val horizontalOffset: Int
        val verticalOffset: Int
        when {
            isTargetingBottomNavBar(activeTutorialPrompt!!.task) -> {
                horizontalOffset = quickStartTarget.width / 2 - focusPointSize + resources
                        .getDimensionPixelOffset(R.dimen.quick_start_focus_point_bottom_nav_offset)
                verticalOffset = 0
            }
            activeTutorialPrompt!!.task == UPLOAD_SITE_ICON -> {
                horizontalOffset = focusPointSize
                verticalOffset = -focusPointSize / 2
            }
            activeTutorialPrompt!!.task == CHECK_STATS ||
                    activeTutorialPrompt!!.task == REVIEW_PAGES ||
                    activeTutorialPrompt!!.task == EDIT_HOMEPAGE -> {
                horizontalOffset = -focusPointSize / 4
                verticalOffset = -focusPointSize / 4
            }
            activeTutorialPrompt!!.task == VIEW_SITE -> { // focus point might be hidden behind FAB
                horizontalOffset = (focusPointSize / 0.5).toInt()
                verticalOffset = (quickStartTarget.height - focusPointSize) / 2
            }
            activeTutorialPrompt!!.task == UPDATE_SITE_TITLE -> {
                horizontalOffset = -(focusPointSize / 2)
                verticalOffset = (quickStartTarget.height - focusPointSize) / 2
            }
            else -> {
                horizontalOffset = resources.getDimensionPixelOffset(
                        R.dimen.quick_start_focus_point_my_site_right_offset
                )
                verticalOffset = (quickStartTarget.height - focusPointSize) / 2
            }
        }
        QuickStartUtils.addQuickStartFocusPointAboveTheView(
                parentView, quickStartTarget, horizontalOffset,
                verticalOffset
        )

        // highlight MySite row and scroll to it
        if (!isTargetingBottomNavBar(activeTutorialPrompt!!.task)) {
            binding?.scrollView?.post { binding?.scrollView?.smoothScrollTo(0, quickStartTarget.top) }
        }
    }

    private fun showQuickStartFocusPoint() {
        if (view == null || !hasActiveQuickStartTask()) {
            return
        }
        requireView().post(mAddQuickStartFocusPointTask)
    }

    private fun removeQuickStartFocusPoint() {
        if (view == null || !isAdded) {
            return
        }
        requireView().removeCallbacks(mAddQuickStartFocusPointTask)
        QuickStartUtils.removeQuickStartFocusPoint(requireActivity().findViewById(R.id.root_view_main))
    }

    fun isQuickStartTaskActive(task: QuickStartTask): Boolean {
        return hasActiveQuickStartTask() && activeTutorialPrompt!!.task == task
    }

    private fun completeQuickStartTask(quickStartTask: QuickStartTask) {
        selectedSite?.let { site ->
            // we need to process notices for tasks that are completed at MySite fragment
            AppPrefs.setQuickStartNoticeRequired(
                    !quickStartStore.hasDoneTask(site.id.toLong(), quickStartTask) &&
                            activeTutorialPrompt != null &&
                            activeTutorialPrompt!!.task == quickStartTask
            )
            quickStartUtilsWrapper.completeTaskAndRemindNextOne(quickStartTask, site, context = requireContext())
            // We update completed tasks counter onResume, but UPLOAD_SITE_ICON can be completed without navigating
            // away from the activity, so we are updating counter here
            if (quickStartTask == UPLOAD_SITE_ICON) {
                binding?.updateQuickStartContainer()
            }
            if (activeTutorialPrompt != null && activeTutorialPrompt!!.task == quickStartTask) {
                removeQuickStartFocusPoint()
                clearActiveQuickStartTask()
            }
        }
    }

    private fun clearActiveQuickStart() {
        // Clear pressed row.
        if (activeTutorialPrompt != null && !isTargetingBottomNavBar(activeTutorialPrompt!!.task)) {
            requireActivity().findViewById<View>(activeTutorialPrompt!!.focusedContainerId).isPressed = false
        }
        if (activity != null && !requireActivity().isChangingConfigurations) {
            clearActiveQuickStartTask()
            removeQuickStartFocusPoint()
        }
        quickStartSnackBarHandler.removeCallbacksAndMessages(null)
    }

    @JvmOverloads
    fun requestNextStepOfActiveQuickStartTask(fireQuickStartEvent: Boolean = true) {
        if (!hasActiveQuickStartTask()) {
            return
        }
        removeQuickStartFocusPoint()
        if (fireQuickStartEvent) {
            EventBus.getDefault().postSticky(QuickStartEvent(activeTutorialPrompt!!.task))
        }
        clearActiveQuickStartTask()
    }

    private fun clearActiveQuickStartTask() {
        activeTutorialPrompt = null
    }

    private fun hasActiveQuickStartTask(): Boolean {
        return activeTutorialPrompt != null
    }

    private fun showActiveQuickStartTutorial() {
        if (!hasActiveQuickStartTask() || !isAdded || activity !is WPMainActivity) {
            return
        }
        showQuickStartFocusPoint()
        val shortQuickStartMessage =
                if (activeTutorialPrompt?.task == UPDATE_SITE_TITLE) {
                    HtmlCompat.fromHtml(
                            getString(
                                    R.string.quick_start_dialog_update_site_title_message_short,
                                    binding?.siteInfoContainer?.title?.text.toString()
                            ), HtmlCompat.FROM_HTML_MODE_COMPACT
                    )
                } else {
                    quickStartUtilsWrapper.stylizeQuickStartPrompt(
                            requireContext(),
                            activeTutorialPrompt!!.shortMessagePrompt,
                            activeTutorialPrompt!!.iconId
                    )
                }
        val promptSnackbar = WPDialogSnackbar.make(
                requireActivity().findViewById(R.id.coordinator),
                shortQuickStartMessage, resources.getInteger(R.integer.quick_start_snackbar_duration_ms)
        )
        (requireActivity() as WPMainActivity).showQuickStartSnackBar(promptSnackbar)
    }

    private fun updateSiteIconMediaId(mediaId: Int, showProgressBar: Boolean) {
        selectedSiteRepository.updateSiteIconMediaId(mediaId, showProgressBar)
    }

    companion object {
        const val HIDE_WP_ADMIN_YEAR = 2015
        const val HIDE_WP_ADMIN_MONTH = 9
        const val HIDE_WP_ADMIN_DAY = 7
        const val TAG_ADD_SITE_ICON_DIALOG = "TAG_ADD_SITE_ICON_DIALOG"
        const val TAG_REMOVE_NEXT_STEPS_DIALOG = "TAG_REMOVE_NEXT_STEPS_DIALOG"
        const val TAG_CHANGE_SITE_ICON_DIALOG = "TAG_CHANGE_SITE_ICON_DIALOG"
        const val TAG_EDIT_SITE_ICON_NOT_ALLOWED_DIALOG = "TAG_EDIT_SITE_ICON_NOT_ALLOWED_DIALOG"
        const val TAG_QUICK_START_DIALOG = "TAG_QUICK_START_DIALOG"
        const val AUTO_QUICK_START_SNACKBAR_DELAY_MS = 1000
        const val KEY_IS_DOMAIN_CREDIT_AVAILABLE = "KEY_IS_DOMAIN_CREDIT_AVAILABLE"
        const val KEY_DOMAIN_CREDIT_CHECKED = "KEY_DOMAIN_CREDIT_CHECKED"
        fun newInstance(): MySiteFragment {
            return MySiteFragment()
        }
    }

    override fun onSuccessfulInput(input: String, callbackId: Int) {
        binding?.apply {
            if (callbackId == siteInfoContainer.title.id && selectedSite != null) {
                if (!NetworkUtils.isNetworkAvailable(activity)) {
                    WPSnackbar.make(
                            requireActivity().findViewById(R.id.coordinator),
                            R.string.error_update_site_title_network,
                            getSnackbarDuration(activity, Snackbar.LENGTH_SHORT)
                    ).show()
                    return
                }

                siteInfoContainer.title.text = input

                selectedSiteRepository.updateTitle(input)
            }
        }
    }

    override fun onTextInputDialogDismissed(callbackId: Int) {
        binding?.apply {
            if (callbackId == siteInfoContainer.title.id) {
                showQuickStartNoticeIfNecessary()
                updateQuickStartContainer()
            }
        }
    }
}
