package org.wordpress.android.ui.main

import android.app.Activity
import android.content.Intent
import android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCrop.Options
import com.yalantis.ucrop.UCropActivity
import kotlinx.android.synthetic.main.me_action_layout.*
import kotlinx.android.synthetic.main.my_site_fragment.*
import kotlinx.android.synthetic.main.toolbar_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.R.attr
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
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_MIGRATION_DIALOG_POSITIVE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_MIGRATION_DIALOG_VIEWED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_REMOVE_DIALOG_NEGATIVE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_REMOVE_DIALOG_POSITIVE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_REQUEST_DIALOG_NEGATIVE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_REQUEST_DIALOG_NEUTRAL_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_REQUEST_DIALOG_POSITIVE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TASK_DIALOG_NEGATIVE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TASK_DIALOG_POSITIVE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TASK_DIALOG_VIEWED
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CHECK_STATS
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CHOOSE_THEME
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CREATE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CUSTOMIZE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.ENABLE_POST_SHARING
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.EXPLORE_PLANS
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPLOAD_SITE_ICON
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.VIEW_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.UNKNOWN
import org.wordpress.android.fluxc.store.SiteStore.OnPlansFetched
import org.wordpress.android.login.LoginMode.JETPACK_STATS
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.FullScreenDialogFragment
import org.wordpress.android.ui.FullScreenDialogFragment.Builder
import org.wordpress.android.ui.FullScreenDialogFragment.OnConfirmListener
import org.wordpress.android.ui.FullScreenDialogFragment.OnDismissListener
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.accounts.LoginActivity
import org.wordpress.android.ui.comments.CommentsListFragment.CommentStatusCriteria.ALL
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.CTA_DOMAIN_CREDIT_REDEMPTION
import org.wordpress.android.ui.domains.DomainRegistrationResultFragment
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener
import org.wordpress.android.ui.main.utils.MeGravatarLoader
import org.wordpress.android.ui.media.MediaBrowserType.SITE_ICON_PICKER
import org.wordpress.android.ui.photopicker.PhotoPickerActivity
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource.ANDROID_CAMERA
import org.wordpress.android.ui.plans.isDomainCreditAvailable
import org.wordpress.android.ui.plugins.PluginUtils
import org.wordpress.android.ui.posts.BasicFragmentDialog
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogNegativeClickInterface
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogOnDismissByOutsideTouchInterface
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface
import org.wordpress.android.ui.posts.PromoDialog
import org.wordpress.android.ui.posts.PromoDialog.PromoDialogClickInterface
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.prefs.SiteSettingsInterface
import org.wordpress.android.ui.prefs.SiteSettingsInterface.SiteSettingsListener
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.quickstart.QuickStartFullScreenDialogFragment
import org.wordpress.android.ui.quickstart.QuickStartMySitePrompts
import org.wordpress.android.ui.quickstart.QuickStartMySitePrompts.Companion.getPromptDetailsForTask
import org.wordpress.android.ui.quickstart.QuickStartMySitePrompts.Companion.isTargetingBottomNavBar
import org.wordpress.android.ui.quickstart.QuickStartNoticeDetails
import org.wordpress.android.ui.themes.ThemeBrowserActivity
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.uploads.UploadService.UploadErrorEvent
import org.wordpress.android.ui.uploads.UploadService.UploadMediaSuccessEvent
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.DOMAIN_REGISTRATION
import org.wordpress.android.util.AppLog.T.EDITOR
import org.wordpress.android.util.AppLog.T.MAIN
import org.wordpress.android.util.AppLog.T.UTILS
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.FluxCUtils
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.PhotonUtils.Quality.HIGH
import org.wordpress.android.util.QuickStartUtils.Companion.addQuickStartFocusPointAboveTheView
import org.wordpress.android.util.QuickStartUtils.Companion.completeTaskAndRemindNextOne
import org.wordpress.android.util.QuickStartUtils.Companion.getNextUncompletedQuickStartTask
import org.wordpress.android.util.QuickStartUtils.Companion.isQuickStartInProgress
import org.wordpress.android.util.QuickStartUtils.Companion.removeQuickStartFocusPoint
import org.wordpress.android.util.QuickStartUtils.Companion.stylizeQuickStartPrompt
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration.SHORT
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.util.getColorFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.BLAVATAR
import org.wordpress.android.util.image.ImageType.USER
import org.wordpress.android.util.requestEmailValidation
import org.wordpress.android.widgets.WPDialogSnackbar
import java.io.File
import java.util.GregorianCalendar
import java.util.TimeZone
import javax.inject.Inject

class MySiteFragment : Fragment(),
        SiteSettingsListener,
        OnScrollToTopListener,
        BasicDialogPositiveClickInterface,
        BasicDialogNegativeClickInterface,
        BasicDialogOnDismissByOutsideTouchInterface,
        PromoDialogClickInterface,
        OnConfirmListener,
        OnDismissListener {
    private var siteSettings: SiteSettingsInterface? = null
    private var activeTutorialPrompt: QuickStartMySitePrompts? = null
    private val quickStartSnackBarHandler = Handler()
    private var blavatarSz = 0
    private var isDomainCreditAvailable = false
    private var isDomainCreditChecked = false

    @Inject lateinit var accountStore: AccountStore
    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var mediaStore: MediaStore
    @Inject lateinit var quickStartStore: QuickStartStore
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var uploadUtilsWrapper: UploadUtilsWrapper
    @Inject lateinit var meGravatarLoader: MeGravatarLoader
    val selectedSite: SiteModel?
        get() {
            return (activity as? WPMainActivity)?.selectedSite
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
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
        siteSettings?.clear()
        super.onDestroy()
    }

    private fun refreshMeGravatar() {
        val avatarUrl = meGravatarLoader.constructGravatarUrl(accountStore.account.avatarUrl)
        meGravatarLoader.load(
                false,
                avatarUrl,
                null,
                avatar,
                USER,
                null
        )
    }

    override fun onResume() {
        super.onResume()
        updateSiteSettingsIfNecessary()

        // Site details may have changed (e.g. via Settings and returning to this Fragment) so update the UI
        refreshSelectedSiteDetails(selectedSite)
        refreshMeGravatar()
        selectedSite?.let { site ->
            val isNotAdmin = !site.hasCapabilityManageOptions
            val isSelfHostedWithoutJetpack = !SiteUtils.isAccessedViaWPComRest(
                    site
            ) && !site.isJetpackConnected
            if (isNotAdmin || isSelfHostedWithoutJetpack) {
                row_activity_log.visibility = View.GONE
            } else {
                row_activity_log.visibility = View.VISIBLE
            }
        }
        updateQuickStartContainer()
        if (!AppPrefs.hasQuickStartMigrationDialogShown() && isQuickStartInProgress(quickStartStore)) {
            showQuickStartDialogMigration()
        }
        showQuickStartNoticeIfNecessary()
    }

    private fun showQuickStartNoticeIfNecessary() {
        if (!isQuickStartInProgress(quickStartStore) || !AppPrefs.isQuickStartNoticeRequired()) {
            return
        }
        val taskToPrompt = getNextUncompletedQuickStartTask(
                quickStartStore,
                AppPrefs.getSelectedSite().toLong(), CUSTOMIZE
        ) // CUSTOMIZE is default type
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
        // If the selected site is null, we can't update its site settings
        val selectedSite = selectedSite ?: return
        if (siteSettings != null && siteSettings!!.localSiteId != selectedSite.id) {
            // The site has changed, we can't use the previous site settings, force a refresh
            siteSettings = null
        }
        if (siteSettings == null) {
            siteSettings = SiteSettingsInterface.getInterface(activity, selectedSite, this)
            if (siteSettings != null) {
                siteSettings!!.init(true)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        clearActiveQuickStart()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.my_site_fragment, container, false) as ViewGroup
        blavatarSz = resources.getDimensionPixelSize(R.dimen.blavatar_sz_small)
        return rootView
    }

    private fun setupClickListeners() {
        site_info_container.setOnClickListener { viewSite() }
        switch_site.setOnClickListener { showSitePicker() }
        row_view_site.setOnClickListener { viewSite() }
        my_site_register_domain_cta.setOnClickListener { registerDomain() }
        quick_action_stats_button.setOnClickListener {
            AnalyticsTracker.track(QUICK_ACTION_STATS_TAPPED)
            viewStats()
        }
        row_stats.setOnClickListener { viewStats() }
        my_site_blavatar.setOnClickListener { updateBlavatar() }
        row_plan.setOnClickListener {
            completeQuickStarTask(EXPLORE_PLANS)
            ActivityLauncher.viewBlogPlans(activity, selectedSite)
        }
        quick_action_posts_button.setOnClickListener {
            AnalyticsTracker.track(QUICK_ACTION_POSTS_TAPPED)
            viewPosts()
        }
        row_blog_posts.setOnClickListener { viewPosts() }
        quick_action_media_button.setOnClickListener {
            AnalyticsTracker.track(QUICK_ACTION_MEDIA_TAPPED)
            viewMedia()
        }
        row_media.setOnClickListener { viewMedia() }
        quick_action_pages_button.setOnClickListener {
            AnalyticsTracker.track(QUICK_ACTION_PAGES_TAPPED)
            viewPages()
        }
        row_pages.setOnClickListener { viewPages() }
        row_comments.setOnClickListener {
            ActivityLauncher.viewCurrentBlogComments(
                    activity,
                    selectedSite
            )
        }
        row_themes.setOnClickListener {
            completeQuickStarTask(CHOOSE_THEME)
            if (isQuickStartTaskActive(CUSTOMIZE_SITE)) {
                requestNextStepOfActiveQuickStartTask()
            }
            ActivityLauncher.viewCurrentBlogThemes(activity, selectedSite)
        }
        row_people.setOnClickListener {
            ActivityLauncher.viewCurrentBlogPeople(
                    activity,
                    selectedSite
            )
        }
        row_plugins.setOnClickListener {
            ActivityLauncher.viewPluginBrowser(
                    activity,
                    selectedSite
            )
        }
        row_activity_log.setOnClickListener {
            ActivityLauncher.viewActivityLogList(
                    activity,
                    selectedSite
            )
        }
        row_settings.setOnClickListener {
            ActivityLauncher.viewBlogSettingsForResult(
                    activity,
                    selectedSite
            )
        }
        row_sharing.setOnClickListener {
            if (isQuickStartTaskActive(ENABLE_POST_SHARING)) {
                requestNextStepOfActiveQuickStartTask()
            }
            ActivityLauncher.viewBlogSharing(activity, selectedSite)
        }
        row_admin.setOnClickListener {
            ActivityLauncher.viewBlogAdmin(
                    activity,
                    selectedSite
            )
        }
        actionable_empty_view.button.setOnClickListener {
            SitePickerActivity.addSite(
                    activity,
                    accountStore.hasAccessToken()
            )
        }
        quick_start_customize.setOnClickListener {
            showQuickStartList(
                    CUSTOMIZE
            )
        }
        quick_start_grow.setOnClickListener {
            showQuickStartList(
                    GROW
            )
        }
        quick_start_more.setOnClickListener { showQuickStartCardMenu() }
    }

    private fun registerDomain() {
        AnalyticsUtils.trackWithSiteDetails(DOMAIN_CREDIT_REDEMPTION_TAPPED, selectedSite)
        ActivityLauncher.viewDomainRegistrationActivityForResult(
                activity, selectedSite,
                CTA_DOMAIN_CREDIT_REDEMPTION
        )
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
                completeQuickStarTask(UPLOAD_SITE_ICON)
            } else {
                val message = if (hasIcon) {
                    R.string.my_site_icon_dialog_change_requires_permission_message
                } else {
                    R.string.my_site_icon_dialog_add_requires_permission_message
                }
                showEditingSiteIconRequiresPermissionDialog(getString(message))
            }
        }
    }

    private fun viewPosts() {
        requestNextStepOfActiveQuickStartTask()
        val selectedSite = selectedSite
        if (selectedSite != null) {
            ActivityLauncher.viewCurrentBlogPosts(requireActivity(), selectedSite)
        } else {
            ToastUtils.showToast(activity, R.string.site_cannot_be_loaded)
        }
    }

    private fun viewPages() {
        requestNextStepOfActiveQuickStartTask()
        val selectedSite = selectedSite
        if (selectedSite != null) {
            ActivityLauncher.viewCurrentBlogPages(requireActivity(), selectedSite)
        } else {
            ToastUtils.showToast(activity, R.string.site_cannot_be_loaded)
        }
    }

    private fun viewStats() {
        val selectedSite = selectedSite
        if (selectedSite != null) {
            completeQuickStarTask(CHECK_STATS)
            if (!accountStore.hasAccessToken() && selectedSite.isJetpackConnected) {
                // If the user is not connected to WordPress.com, ask him to connect first.
                startWPComLoginForJetpackStats()
            } else if (selectedSite.isWPCom || selectedSite.isJetpackInstalled && selectedSite
                            .isJetpackConnected) {
                ActivityLauncher.viewBlogStats(activity, selectedSite)
            } else {
                ActivityLauncher.viewConnectJetpackForStats(activity, selectedSite)
            }
        }
    }

    private fun viewSite() {
        completeQuickStarTask(VIEW_SITE)
        ActivityLauncher.viewCurrentSite(activity, selectedSite, true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        toolbar_main.setTitle(R.string.my_site_section_screen_title)
        toolbar_main.inflateMenu(R.menu.my_site_menu)
        val meMenu = toolbar_main.menu.findItem(R.id.me_item)
        val actionView = meMenu.actionView
        actionView.setOnClickListener {
            ActivityLauncher.viewMeActivityForResult(
                    activity
            )
        }
        actionView.let {
            TooltipCompat.setTooltipText(it, meMenu.title)
        }
        if (activeTutorialPrompt != null) {
            showQuickStartFocusPoint()
        }
    }

    private fun updateQuickStartContainer() {
        if (!isAdded) {
            return
        }
        if (isQuickStartInProgress(quickStartStore)) {
            val site = AppPrefs.getSelectedSite()
            val countCustomizeCompleted = quickStartStore.getCompletedTasksByType(
                    site.toLong(),
                    CUSTOMIZE
            ).size
            val countCustomizeUncompleted = quickStartStore.getUncompletedTasksByType(
                    site.toLong(),
                    CUSTOMIZE
            ).size
            val countGrowCompleted = quickStartStore.getCompletedTasksByType(
                    site.toLong(),
                    GROW
            ).size
            val countGrowUncompleted = quickStartStore.getUncompletedTasksByType(
                    site.toLong(),
                    GROW
            ).size
            if (countCustomizeUncompleted > 0) {
                quick_start_customize_icon.isEnabled = true
                quick_start_customize_title.isEnabled = true
                val updatedPaintFlags = quick_start_customize_title.paintFlags and STRIKE_THRU_TEXT_FLAG.inv()
                quick_start_customize_title.paintFlags = updatedPaintFlags
            } else {
                quick_start_customize_icon.isEnabled = false
                quick_start_customize_title.isEnabled = false
                quick_start_customize_title.paintFlags = quick_start_customize_title.paintFlags or STRIKE_THRU_TEXT_FLAG
            }
            quick_start_customize_subtitle.text = getString(
                    R.string.quick_start_sites_type_subtitle,
                    countCustomizeCompleted, countCustomizeCompleted + countCustomizeUncompleted
            )
            if (countGrowUncompleted > 0) {
                quick_start_grow_icon.setBackgroundResource(R.drawable.bg_oval_pink_50_multiple_users_white_40dp)
                quick_start_grow_title.isEnabled = true
                quick_start_grow_title.paintFlags = quick_start_grow_title.paintFlags and STRIKE_THRU_TEXT_FLAG.inv()
            } else {
                quick_start_grow_icon.setBackgroundResource(R.drawable.bg_oval_neutral_30_multiple_users_white_40dp)
                quick_start_grow_title.isEnabled = false
                quick_start_grow_title.paintFlags = quick_start_grow_title.paintFlags or STRIKE_THRU_TEXT_FLAG
            }
            quick_start_grow_subtitle.text = getString(
                    R.string.quick_start_sites_type_subtitle,
                    countGrowCompleted, countGrowCompleted + countGrowUncompleted
            )
            quick_start.visibility = View.VISIBLE
        } else {
            quick_start.visibility = View.GONE
        }
    }

    private fun showQuickStartCardMenu() {
        val quickStartPopupMenu = PopupMenu(
                requireContext(),
                quick_start_more
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

    private fun showEditingSiteIconRequiresPermissionDialog(message: String) {
        val dialog = BasicFragmentDialog()
        val tag = TAG_EDIT_SITE_ICON_PERMISSIONS_DIALOG
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
                // reset comments status filter
                AppPrefs.setCommentsStatusFilter(ALL)
                // reset domain credit flag - it will be checked in onSiteChanged
                isDomainCreditAvailable = false
            }
            RequestCodes.PHOTO_PICKER -> if (resultCode == Activity.RESULT_OK && data != null) {
                if (data.hasExtra(PhotoPickerActivity.EXTRA_MEDIA_ID)) {
                    val mediaId = data.getLongExtra(PhotoPickerActivity.EXTRA_MEDIA_ID, 0).toInt()
                    showSiteIconProgressBar(true)
                    updateSiteIconMediaId(mediaId)
                } else {
                    val mediaUriStringsArray = data.getStringArrayExtra(
                            PhotoPickerActivity.EXTRA_MEDIA_URIS
                    )
                    if (mediaUriStringsArray.isNullOrEmpty()) {
                        AppLog.e(
                                UTILS,
                                "Can't resolve picked or captured image"
                        )
                        return
                    }
                    val source = PhotoPickerMediaSource.fromString(
                            data.getStringExtra(PhotoPickerActivity.EXTRA_MEDIA_SOURCE)
                    )
                    val stat = if (source == ANDROID_CAMERA) MY_SITE_ICON_SHOT_NEW else MY_SITE_ICON_GALLERY_PICKED
                    AnalyticsTracker.track(stat)
                    val imageUri = Uri.parse(mediaUriStringsArray[0])
                    if (imageUri != null) {
                        val didGoWell = WPMediaUtils.fetchMediaAndDoNext(
                                activity, imageUri
                        ) { uri: Uri ->
                            showSiteIconProgressBar(true)
                            startCropActivity(uri)
                        }
                        if (!didGoWell) {
                            AppLog.e(
                                    UTILS,
                                    "Can't download picked or captured image"
                            )
                        }
                    }
                }
            }
            UCrop.REQUEST_CROP -> if (resultCode == Activity.RESULT_OK) {
                AnalyticsTracker.track(MY_SITE_ICON_CROPPED)
                WPMediaUtils.fetchMediaAndDoNext(
                        activity, UCrop.getOutput(data!!)
                ) { uri: Uri? ->
                    startSiteIconUpload(
                            MediaUtils.getRealPathFromURI(activity, uri)
                    )
                }
            } else if (resultCode == UCrop.RESULT_ERROR) {
                AppLog.e(
                        MAIN,
                        "Image cropping failed!",
                        UCrop.getError(data!!)
                )
                ToastUtils.showToast(
                        activity,
                        R.string.error_cropping_image,
                        SHORT
                )
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
        updateQuickStartContainer()
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

    private fun showSiteIconProgressBar(isVisible: Boolean) {
        if (my_site_icon_progress != null && my_site_blavatar != null) {
            if (isVisible) {
                my_site_icon_progress.visibility = View.VISIBLE
                my_site_blavatar.visibility = View.INVISIBLE
            } else {
                my_site_icon_progress.visibility = View.GONE
                my_site_blavatar.visibility = View.VISIBLE
            }
        }
    }

    private val isMediaUploadInProgress: Boolean
        get() = my_site_icon_progress.visibility == View.VISIBLE

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
        options.setToolbarWidgetColor(context.getColorFromAttribute(attr.colorOnPrimarySurface))
        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.NONE, UCropActivity.NONE)
        options.setHideBottomControls(true)
        UCrop.of(uri, Uri.fromFile(File(context.cacheDir, "cropped_for_site_icon.jpg")))
                .withAspectRatio(1f, 1f)
                .withOptions(options)
                .start(requireActivity(), this)
    }

    private fun refreshSelectedSiteDetails(site: SiteModel?) {
        if (!isAdded || view == null) {
            return
        }
        if (site == null) {
            scroll_view.visibility = View.GONE
            actionable_empty_view.visibility = View.VISIBLE

            // Hide actionable empty view image when screen height is under 600 pixels.
            if (DisplayUtils.getDisplayPixelHeight(activity) >= 600) {
                actionable_empty_view.image.visibility = View.VISIBLE
            } else {
                actionable_empty_view.image.visibility = View.GONE
            }
            return
        }
        if (SiteUtils.onFreePlan(site) || SiteUtils.hasCustomDomain(
                        site
                )) {
            isDomainCreditAvailable = false
            toggleDomainRegistrationCtaVisibility()
        } else if (!isDomainCreditChecked) {
            fetchSitePlans(site)
        } else {
            toggleDomainRegistrationCtaVisibility()
        }
        scroll_view.visibility = View.VISIBLE
        actionable_empty_view.visibility = View.GONE
        toggleAdminVisibility(site)
        val themesVisibility = if (ThemeBrowserActivity.isAccessible(site)) View.VISIBLE else View.GONE
        my_site_look_and_feel_header.visibility = themesVisibility
        row_themes.visibility = themesVisibility

        // sharing is only exposed for sites accessed via the WPCOM REST API (wpcom or Jetpack)
        val sharingVisibility = if (SiteUtils.isAccessedViaWPComRest(site)) View.VISIBLE else View.GONE
        row_sharing.visibility = sharingVisibility

        // show settings for all self-hosted to expose Delete Site
        val isAdminOrSelfHosted = site.hasCapabilityManageOptions || !SiteUtils.isAccessedViaWPComRest(
                site
        )
        row_settings.visibility = if (isAdminOrSelfHosted) View.VISIBLE else View.GONE
        row_people.visibility = if (site.hasCapabilityListUsers) View.VISIBLE else View.GONE
        row_plugins.visibility = if (PluginUtils.isPluginFeatureAvailable(site)) View.VISIBLE else View.GONE

        // if either people or settings is visible, configuration header should be visible
        val settingsVisibility = if (isAdminOrSelfHosted || site.hasCapabilityListUsers) View.VISIBLE else View.GONE
        my_site_configuration_header.visibility = settingsVisibility
        imageManager.load(
                my_site_blavatar,
                BLAVATAR,
                SiteUtils.getSiteIconUrl(site, blavatarSz)
        )
        val homeUrl = SiteUtils.getHomeURLOrHostName(site)
        val blogTitle = SiteUtils.getSiteNameOrHomeURL(site)
        my_site_title_label.text = blogTitle
        my_site_subtitle_label.text = homeUrl

        // Hide the Plan item if the Plans feature is not available for this blog
        val planShortName = site.planShortName
        if (!TextUtils.isEmpty(planShortName) && site.hasCapabilityManageOptions) {
            if (site.isWPCom || site.isAutomatedTransfer) {
                my_site_current_plan_text_view.text = planShortName
                row_plan.visibility = View.VISIBLE
            } else {
                // TODO: Support Jetpack plans
                row_plan.visibility = View.GONE
            }
        } else {
            row_plan.visibility = View.GONE
        }

        // Do not show pages menu item to Collaborators.
        val pageVisibility = if (site.isSelfHostedAdmin || site.hasCapabilityEditPages) View.VISIBLE else View.GONE
        row_pages.visibility = pageVisibility
        quick_action_pages_container.visibility = pageVisibility
        if (pageVisibility == View.VISIBLE) {
            quick_action_buttons_container.weightSum = 100f
        } else {
            quick_action_buttons_container.weightSum = 75f
        }
    }

    private fun toggleAdminVisibility(site: SiteModel?) {
        if (site == null) {
            return
        }
        if (shouldHideWPAdmin(site)) {
            row_admin.visibility = View.GONE
        } else {
            row_admin.visibility = View.VISIBLE
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
            calendar.timeZone = TimeZone.getTimeZone(HIDE_WP_ADMIN_GMT_TIME_ZONE)
            dateCreated != null && dateCreated.after(calendar.time)
        }
    }

    override fun onScrollToTop() {
        if (isAdded) {
            scroll_view.smoothScrollTo(0, 0)
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
    fun onSiteChanged(site: SiteModel?) {
        // whenever site changes we hide CTA and check for credit in refreshSelectedSiteDetails()
        isDomainCreditChecked = false
        refreshSelectedSiteDetails(site)
        showSiteIconProgressBar(false)
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: UploadErrorEvent) {
        AnalyticsTracker.track(MY_SITE_ICON_UPLOAD_UNSUCCESSFUL)
        EventBus.getDefault().removeStickyEvent(event)
        if (isMediaUploadInProgress) {
            showSiteIconProgressBar(false)
        }
        val site = selectedSite
        if (site != null && event.post != null) {
            if (event.post.localSiteId == site.id) {
                uploadUtilsWrapper.onPostUploadedSnackbarHandler(
                        activity,
                        requireActivity().findViewById(R.id.coordinator), true,
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
            if (isMediaUploadInProgress) {
                if (event.mediaModelList.size > 0) {
                    val media = event.mediaModelList[0]
                    imageManager.load(
                            my_site_blavatar,
                            BLAVATAR,
                            PhotonUtils
                                    .getPhotonImageUrl(
                                            media.url,
                                            blavatarSz,
                                            blavatarSz,
                                            HIGH,
                                            site.isPrivateWPComAtomic
                                    )
                    )
                    updateSiteIconMediaId(media.mediaId.toInt())
                } else {
                    AppLog.w(
                            MAIN,
                            "Site icon upload completed, but mediaList is empty."
                    )
                }
                showSiteIconProgressBar(false)
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
    }

    override fun onPositiveClicked(instanceTag: String) {
        when (instanceTag) {
            TAG_ADD_SITE_ICON_DIALOG, TAG_CHANGE_SITE_ICON_DIALOG -> ActivityLauncher.showPhotoPickerForResult(
                    activity,
                    SITE_ICON_PICKER, selectedSite, null
            )
            TAG_EDIT_SITE_ICON_PERMISSIONS_DIALOG -> {
            }
            TAG_QUICK_START_DIALOG -> {
                startQuickStart()
                AnalyticsTracker.track(QUICK_START_REQUEST_DIALOG_POSITIVE_TAPPED)
            }
            TAG_QUICK_START_MIGRATION_DIALOG -> AnalyticsTracker.track(QUICK_START_MIGRATION_DIALOG_POSITIVE_TAPPED)
            TAG_REMOVE_NEXT_STEPS_DIALOG -> {
                AnalyticsTracker.track(QUICK_START_REMOVE_DIALOG_POSITIVE_TAPPED)
                skipQuickStart()
                updateQuickStartContainer()
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
        val siteId = AppPrefs.getSelectedSite()
        for (quickStartTask in QuickStartTask.values()) {
            quickStartStore.setDoneTask(siteId.toLong(), quickStartTask, true)
        }
        quickStartStore.setQuickStartCompleted(siteId.toLong(), true)
        // skipping all tasks means no achievement notification, so we mark it as received
        quickStartStore.setQuickStartNotificationReceived(siteId.toLong(), true)
    }

    private fun startQuickStart() {
        quickStartStore.setDoneTask(AppPrefs.getSelectedSite().toLong(), CREATE_SITE, true)
        updateQuickStartContainer()
    }

    private fun toggleDomainRegistrationCtaVisibility() {
        if (isDomainCreditAvailable) {
            // we nest this check because of some weirdness with ui state and race conditions
            if (my_site_register_domain_cta.visibility != View.VISIBLE) {
                AnalyticsTracker.track(DOMAIN_CREDIT_PROMPT_SHOWN)
                my_site_register_domain_cta.visibility = View.VISIBLE
            }
        } else {
            my_site_register_domain_cta.visibility = View.GONE
        }
    }

    override fun onNegativeClicked(instanceTag: String) {
        when (instanceTag) {
            TAG_ADD_SITE_ICON_DIALOG -> showQuickStartNoticeIfNecessary()
            TAG_CHANGE_SITE_ICON_DIALOG -> {
                AnalyticsTracker.track(MY_SITE_ICON_REMOVED)
                showSiteIconProgressBar(true)
                updateSiteIconMediaId(0)
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
            TAG_EDIT_SITE_ICON_PERMISSIONS_DIALOG,
            TAG_QUICK_START_DIALOG,
            TAG_QUICK_START_MIGRATION_DIALOG,
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

    override fun onLinkClicked(instanceTag: String) {}
    override fun onSettingsSaved() {
        // refresh the site after site icon change
        val site = selectedSite
        if (site != null) {
            dispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(site))
        }
    }

    override fun onSaveError(error: Exception) {
        showSiteIconProgressBar(false)
    }

    override fun onFetchError(error: Exception) {
        showSiteIconProgressBar(false)
    }

    override fun onSettingsUpdated() {}
    override fun onCredentialsValidated(error: Exception?) {}
    private fun fetchSitePlans(site: SiteModel?) {
        dispatcher.dispatch(SiteActionBuilder.newFetchPlansAction(site))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlansFetched(event: OnPlansFetched) {
        if (AppPrefs.getSelectedSite() != event.site.id) {
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
            toggleDomainRegistrationCtaVisibility()
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
        if (isTargetingBottomNavBar(activeTutorialPrompt!!.task)) {
            horizontalOffset = quickStartTarget.width / 2 - focusPointSize + resources
                    .getDimensionPixelOffset(R.dimen.quick_start_focus_point_bottom_nav_offset)
            verticalOffset = 0
        } else if (activeTutorialPrompt!!.task == UPLOAD_SITE_ICON) {
            horizontalOffset = focusPointSize
            verticalOffset = -focusPointSize / 2
        } else {
            horizontalOffset = resources.getDimensionPixelOffset(R.dimen.quick_start_focus_point_my_site_right_offset)
            verticalOffset = (quickStartTarget.height - focusPointSize) / 2
        }
        addQuickStartFocusPointAboveTheView(
                parentView, quickStartTarget, horizontalOffset,
                verticalOffset
        )

        // highlight MySite row and scroll to it
        if (!isTargetingBottomNavBar(activeTutorialPrompt!!.task)) {
            scroll_view.post { scroll_view.smoothScrollTo(0, quickStartTarget.top) }
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
        removeQuickStartFocusPoint(requireActivity().findViewById(R.id.root_view_main))
    }

    fun isQuickStartTaskActive(task: QuickStartTask): Boolean {
        return hasActiveQuickStartTask() && activeTutorialPrompt!!.task == task
    }

    private fun completeQuickStarTask(quickStartTask: QuickStartTask) {
        selectedSite?.let { site ->
            // we need to process notices for tasks that are completed at MySite fragment
            AppPrefs.setQuickStartNoticeRequired(
                    !quickStartStore.hasDoneTask(
                            AppPrefs.getSelectedSite().toLong(), quickStartTask
                    ) &&
                            activeTutorialPrompt != null &&
                            activeTutorialPrompt!!.task == quickStartTask
            )
            completeTaskAndRemindNextOne(
                    quickStartStore, quickStartTask, dispatcher,
                    site, context = requireContext()
            )
            // We update completed tasks counter onResume, but UPLOAD_SITE_ICON can be completed without navigating
            // away from the activity, so we are updating counter here
            if (quickStartTask == UPLOAD_SITE_ICON) {
                updateQuickStartContainer()
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

    fun requestNextStepOfActiveQuickStartTask() {
        if (!hasActiveQuickStartTask()) {
            return
        }
        removeQuickStartFocusPoint()
        EventBus.getDefault().postSticky(QuickStartEvent(activeTutorialPrompt!!.task))
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
        val shortQuickStartMessage = stylizeQuickStartPrompt(
                requireActivity(),
                activeTutorialPrompt!!.shortMessagePrompt,
                activeTutorialPrompt!!.iconId
        )
        val promptSnackbar = WPDialogSnackbar.make(
                requireActivity().findViewById(R.id.coordinator),
                shortQuickStartMessage, resources.getInteger(R.integer.quick_start_snackbar_duration_ms)
        )
        (requireActivity() as WPMainActivity).showQuickStartSnackBar(promptSnackbar)
    }

    private fun showQuickStartDialogMigration() {
        val promoDialog = PromoDialog()
        promoDialog.initialize(
                TAG_QUICK_START_MIGRATION_DIALOG,
                getString(R.string.quick_start_dialog_migration_title),
                getString(R.string.quick_start_dialog_migration_message),
                getString(android.R.string.ok),
                R.drawable.img_illustration_checkmark_280dp,
                "",
                "",
                ""
        )
        if (fragmentManager != null) {
            promoDialog.show(requireFragmentManager(), TAG_QUICK_START_MIGRATION_DIALOG)
            AppPrefs.setQuickStartMigrationDialogShown(true)
            AnalyticsTracker.track(QUICK_START_MIGRATION_DIALOG_VIEWED)
        }
    }

    private fun updateSiteIconMediaId(mediaId: Int) {
        siteSettings?.let {
            it.setSiteIconMediaId(mediaId)
            it.saveSettings()
        }
    }

    companion object {
        const val HIDE_WP_ADMIN_YEAR = 2015
        const val HIDE_WP_ADMIN_MONTH = 9
        const val HIDE_WP_ADMIN_DAY = 7
        const val HIDE_WP_ADMIN_GMT_TIME_ZONE = "GMT"
        const val ARG_QUICK_START_TASK = "ARG_QUICK_START_TASK"
        const val TAG_ADD_SITE_ICON_DIALOG = "TAG_ADD_SITE_ICON_DIALOG"
        const val TAG_REMOVE_NEXT_STEPS_DIALOG = "TAG_REMOVE_NEXT_STEPS_DIALOG"
        const val TAG_CHANGE_SITE_ICON_DIALOG = "TAG_CHANGE_SITE_ICON_DIALOG"
        const val TAG_EDIT_SITE_ICON_PERMISSIONS_DIALOG = "TAG_EDIT_SITE_ICON_PERMISSIONS_DIALOG"
        const val TAG_QUICK_START_DIALOG = "TAG_QUICK_START_DIALOG"
        const val TAG_QUICK_START_MIGRATION_DIALOG = "TAG_QUICK_START_MIGRATION_DIALOG"
        const val AUTO_QUICK_START_SNACKBAR_DELAY_MS = 1000
        const val KEY_IS_DOMAIN_CREDIT_AVAILABLE = "KEY_IS_DOMAIN_CREDIT_AVAILABLE"
        const val KEY_DOMAIN_CREDIT_CHECKED = "KEY_DOMAIN_CREDIT_CHECKED"
        fun newInstance(): MySiteFragment {
            return MySiteFragment()
        }
    }
}
