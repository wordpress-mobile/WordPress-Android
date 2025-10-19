@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.main

import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.gravatar.quickeditor.GravatarQuickEditor
import com.gravatar.quickeditor.ui.editor.AboutEditorResult
import com.gravatar.quickeditor.ui.editor.AboutInputField
import com.gravatar.quickeditor.ui.editor.AuthenticationMethod
import com.gravatar.quickeditor.ui.editor.AvatarPickerAndAboutEditorConfiguration
import com.gravatar.quickeditor.ui.editor.AvatarPickerResult
import com.gravatar.quickeditor.ui.editor.GravatarQuickEditorParams
import com.gravatar.quickeditor.ui.editor.QuickEditorScopeOption
import com.gravatar.types.Email
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.ME_GRAVATAR_TAPPED
import org.wordpress.android.databinding.MeFragmentBinding
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.about.UnifiedAboutActivity
import org.wordpress.android.ui.accounts.HelpActivity.Origin.ME_SCREEN_HELP
import org.wordpress.android.ui.debug.DebugSettingsActivity
import org.wordpress.android.ui.deeplinks.DeepLinkOpenWebLinksWithJetpackHelper
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.main.MeViewModel.RecommendAppUiState
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener
import org.wordpress.android.ui.main.emailverificationbanner.EmailVerificationBanner
import org.wordpress.android.ui.main.emailverificationbanner.EmailVerificationViewModel
import org.wordpress.android.ui.main.utils.MeGravatarLoader
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment
import org.wordpress.android.ui.notifications.utils.NotificationsUtils
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MAIN
import org.wordpress.android.util.FluxCUtils
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.PackageManagerWrapper
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarItem.Info
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration.SHORT
import org.wordpress.android.util.config.DomainManagementFeatureConfig
import org.wordpress.android.util.config.QRCodeAuthFlowFeatureConfig
import org.wordpress.android.util.config.RecommendTheAppFeatureConfig
import org.wordpress.android.util.image.ImageManager.RequestListener
import org.wordpress.android.util.image.ImageType.AVATAR_WITHOUT_BACKGROUND
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

@AndroidEntryPoint
@Suppress("TooManyFunctions")
class MeFragment : Fragment(R.layout.me_fragment), OnScrollToTopListener {
    @Suppress("DEPRECATION")
    private var disconnectProgressDialog: ProgressDialog? = null
    private var binding: MeFragmentBinding? = null

    @Inject
    lateinit var dispatcher: Dispatcher

    @Inject
    lateinit var accountStore: AccountStore

    @Inject
    lateinit var siteStore: SiteStore

    @Inject
    lateinit var postStore: PostStore

    @Inject
    lateinit var meGravatarLoader: MeGravatarLoader

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var recommendTheAppFeatureConfig: RecommendTheAppFeatureConfig

    @Inject
    lateinit var sequencer: SnackbarSequencer

    @Inject
    lateinit var qrCodeAuthFlowFeatureConfig: QRCodeAuthFlowFeatureConfig

    @Inject
    lateinit var jetpackBrandingUtils: JetpackBrandingUtils

    @Inject
    lateinit var packageManagerWrapper: PackageManagerWrapper

    @Inject
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var jetpackFeatureRemovalUtils: JetpackFeatureRemovalOverlayUtil

    @Inject
    lateinit var domainManagementFeatureConfig: DomainManagementFeatureConfig

    @Inject
    lateinit var experimentalFeatures: ExperimentalFeatures

    @Inject
    lateinit var appLogWrapper: AppLogWrapper

    private val viewModel: MeViewModel by viewModels()
    private val emailVerificationViewModel: EmailVerificationViewModel by viewModels()

    private val shouldShowDomainButton
        get() = BuildConfig.IS_JETPACK_APP && domainManagementFeatureConfig.isEnabled() && accountStore.hasAccessToken()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = MeFragmentBinding.bind(view).apply {
            setupViews()
            setupObservers(savedInstanceState)
        }
    }

    @Suppress("LongMethod")
    private fun MeFragmentBinding.setupViews() {
        if (!BuildConfig.IS_JETPACK_APP && jetpackFeatureRemovalUtils.shouldHideJetpackFeatures()) {
            with(requireActivity() as AppCompatActivity) {
                setSupportActionBar(toolbarMain)
                supportActionBar?.apply {
                    setHomeButtonEnabled(true)
                    setDisplayHomeAsUpEnabled(true)
                    // We need to set the title this way so it can be updated on locale change
                    setTitle(packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA).labelRes)
                }
            }
        } else {
            appbarMain.visibility = View.GONE
        }

        addJetpackBadgeIfNeeded()

        val showPickerListener = OnClickListener {
            AnalyticsTracker.track(ME_GRAVATAR_TAPPED)
            showAvatarPicker()
        }
        avatarContainer.setOnClickListener(showPickerListener)
        rowMyProfile.setOnClickListener {
            AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_MY_PROFILE)
            showProfileEditor()
        }
        rowAccountSettings.setOnClickListener {
            ActivityLauncher.viewAccountSettings(activity)
        }
        rowAppSettings.setOnClickListener {
            ActivityLauncher.viewAppSettingsForResult(activity)
        }
        rowSupport.setOnClickListener {
            ActivityLauncher.viewHelp(requireContext(), ME_SCREEN_HELP, viewModel.getSite(), null, experimentalFeatures)
        }
        learnMoreAtGravatar.setOnClickListener {
            ActivityLauncher.openUrlExternal(activity, GRAVATAR_URL)
        }

        if (BuildConfig.IS_JETPACK_APP) meAboutIcon.setImageResource(R.drawable.ic_jetpack_logo_white_24dp)

        rowExperimentalFeaturesSettings.setOnClickListener {
            context?.let { context -> ActivityLauncher.viewExperimentalFeatures(context) }
        }

        if (BuildConfig.DEBUG && BuildConfig.ENABLE_DEBUG_SETTINGS) {
            rowDebugSettings.isVisible = true
            debugSettingsDivider.isVisible = true
            rowDebugSettings.setOnClickListener {
                requireContext().startActivity(Intent(requireContext(), DebugSettingsActivity::class.java))
            }
        }

        rowAboutTheApp.setOnClickListener {
            viewModel.showUnifiedAbout()
        }

        initRecommendUiState()

        rowLogout.setOnClickListener {
            if (accountStore.hasAccessToken()) {
                signOutWordPressComWithConfirmation()
            } else {
                if (BuildConfig.IS_JETPACK_APP) {
                    ActivityLauncher.showSignInForResultJetpackOnly(activity)
                } else {
                    ActivityLauncher.showSignInForResultWpComOnly(activity)
                }
            }
        }

        refreshWPCOMLoggedInOnlyButtonsVisibility()

        meEmailVerificationComposeView.setContent {
            EmailVerificationBanner(
                verificationState = emailVerificationViewModel.verificationState.collectAsState(),
                emailAddress = emailVerificationViewModel.emailAddress.collectAsState(),
                errorMessage = emailVerificationViewModel.errorMessage.collectAsState(),
                onSendLinkClick = {
                    emailVerificationViewModel.onSendVerificationLinkClick()
                }
            )
        }
    }

    private fun MeFragmentBinding.showAvatarPicker() {
        appLogWrapper.d(AppLog.T.MAIN, "Gravatar: Opening avatar editor")
        withVeryfiedEmail(unverifiedEmailMessageRes = R.string.avatar_update_email_unverified) {
            showQuickEditor(page = AvatarPickerAndAboutEditorConfiguration.Page.AvatarPicker)
        }
    }

    private fun MeFragmentBinding.showProfileEditor() {
        appLogWrapper.d(AppLog.T.MAIN, "Gravatar: Opening profile editor")
        withVeryfiedEmail(unverifiedEmailMessageRes = R.string.about_update_email_unverified) {
            showQuickEditor(page = AvatarPickerAndAboutEditorConfiguration.Page.AboutEditor)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun MeFragmentBinding.showQuickEditor(
        page: AvatarPickerAndAboutEditorConfiguration.Page
    ) {
        try {
            appLogWrapper.d(AppLog.T.MAIN, "Gravatar: Showing Gravatar quick editor: ${page.value}")
            GravatarQuickEditor.show(
                fragment = this@MeFragment,
                gravatarQuickEditorParams = GravatarQuickEditorParams {
                    email = Email(accountStore.account.email)
                    scopeOption = QuickEditorScopeOption.avatarAndAbout {
                        initialPage = page
                        fields = setOf(
                            AboutInputField.FirstName,
                            AboutInputField.LastName,
                            AboutInputField.DisplayName,
                            AboutInputField.AboutMe
                        )
                    }
                },
                authenticationMethod = AuthenticationMethod.Bearer(accountStore.accessToken.orEmpty()),
                updateHandler = { event ->
                    when (event) {
                        AvatarPickerResult -> {
                            loadAvatar(null, true)
                        }

                        is AboutEditorResult -> {
                            meDisplayName.text = event.profile.displayName.ifEmpty { accountStore.account.displayName }
                        }

                        else -> Unit
                    }
                },
                onDismiss = {
                    appLogWrapper.d(AppLog.T.MAIN, "Gravatar: Gravatar quick editor dismissed: ${page.value}")
                }
            )
        } catch (exception: Exception) {
            appLogWrapper.d(AppLog.T.MAIN, "Gravatar: Error opening Gravatar quick editor: ${exception.message}")
        }
    }

    private fun withVeryfiedEmail(
        @StringRes unverifiedEmailMessageRes: Int,
        action: () -> Unit,
    ) {
        if (accountStore.account.emailVerified) {
            appLogWrapper.d(AppLog.T.MAIN, "Email verified")
            action()
        } else {
            appLogWrapper.d(AppLog.T.MAIN, "Email not verified")
            view?.let { view ->
                sequencer.enqueue(
                    SnackbarItem(
                        Info(
                            view,
                            UiString.UiStringRes(unverifiedEmailMessageRes),
                            Snackbar.LENGTH_LONG
                        ),
                        null,
                        null
                    )
                )
            }
        }
    }

    private fun MeFragmentBinding.refreshWPCOMLoggedInOnlyButtonsVisibility() {
        if (shouldShowQrCodeLogin()) {
            rowScanLoginCode.isVisible = true
            scanLoginCodeDivider.isVisible = true

            rowScanLoginCode.setOnClickListener {
                viewModel.showScanLoginCode()
            }
        } else {
            rowScanLoginCode.isVisible = false
            scanLoginCodeDivider.isVisible = false
        }

        if (shouldShowDomainButton) {
            domainManagementContainer.visibility = View.VISIBLE
            domainManagementContainer.setOnClickListener {
                context?.let {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.DOMAIN_MANAGEMENT_ME_DOMAINS_TAPPED)
                    ActivityLauncher.openDomainManagement(it)
                }
            }
        } else {
            domainManagementContainer.visibility = View.GONE
        }
    }

    private fun MeFragmentBinding.addJetpackBadgeIfNeeded() {
        if (jetpackBrandingUtils.shouldShowJetpackBranding()) {
            val screen = JetpackPoweredScreen.WithStaticText.ME
            jetpackBadge.isVisible = true
            jetpackBadge.text = uiHelpers.getTextOfUiString(
                requireContext(),
                jetpackBrandingUtils.getBrandingTextForScreen(screen)
            )
            if (jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) {
                jetpackBadge.setOnClickListener {
                    jetpackBrandingUtils.trackBadgeTapped(screen)
                    viewModel.showJetpackPoweredBottomSheet()
                }
            }
        }
    }

    private fun MeFragmentBinding.setupObservers(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(IS_DISCONNECTING, false)) {
                viewModel.openDisconnectDialog()
            }
        }

        viewModel.showUnifiedAbout.observeEvent(viewLifecycleOwner) {
            startActivity(Intent(activity, UnifiedAboutActivity::class.java))
        }

        viewModel.showDisconnectDialog.observeEvent(viewLifecycleOwner) {
            when (it) {
                true -> showDisconnectDialog()
                false -> {
                    hideDisconnectDialog()
                    refreshWPCOMLoggedInOnlyButtonsVisibility()
                }
            }
        }

        viewModel.recommendUiState.observeEvent(viewLifecycleOwner) {
            if (!isAdded) return@observeEvent

            manageRecommendUiState(it)
        }

        viewModel.showScanLoginCode.observeEvent(viewLifecycleOwner) {
            ActivityLauncher.startQRCodeAuthFlow(requireContext())
        }

        viewModel.showJetpackPoweredBottomSheet.observeEvent(viewLifecycleOwner) {
            JetpackPoweredBottomSheetFragment
                .newInstance()
                .show(childFragmentManager, JetpackPoweredBottomSheetFragment.TAG)
        }
    }

    private fun shouldShowQrCodeLogin(): Boolean {
        return qrCodeAuthFlowFeatureConfig.isEnabled() &&
                accountStore.hasAccessToken() &&
                accountStore.account?.twoStepEnabled != true
    }

    private fun MeFragmentBinding.setRecommendLoadingState(startShimmer: Boolean) {
        recommendTheAppShimmer.let {
            it.isEnabled = !startShimmer

            if (startShimmer) {
                if (it.isShimmerVisible) {
                    it.startShimmer()
                } else {
                    it.showShimmer(true)
                }
            } else {
                it.hideShimmer()
            }
        }
    }

    private fun MeFragmentBinding.initRecommendUiState() {
        if (recommendTheAppFeatureConfig.isEnabled()) {
            setRecommendLoadingState(false)
            recommendTheAppContainer.visibility = View.VISIBLE
            rowRecommendTheApp.setOnClickListener {
                viewModel.onRecommendTheApp()
            }
        } else {
            recommendTheAppContainer.visibility = View.GONE
        }
    }

    private fun manageRecommendUiState(state: RecommendAppUiState) {
        binding?.setRecommendLoadingState(state.showLoading)

        if (!state.showLoading) {
            if (state.isError()) {
                view?.let { view ->
                    sequencer.enqueue(
                        SnackbarItem(
                            Info(view, UiStringText(state.error!!), Snackbar.LENGTH_LONG),
                            null,
                            null
                        )
                    )
                }
            } else {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_TEXT, "${state.message}\n${state.link}")
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, resources.getString(R.string.recommend_app_subject))

                startActivity(Intent.createChooser(shareIntent, resources.getString(R.string.share_link)))
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (disconnectProgressDialog != null) {
            outState.putBoolean(IS_DISCONNECTING, true)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onScrollToTop() {
        if (isAdded) {
            binding?.scrollView?.smoothScrollTo(0, 0)
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
        dispatcher.register(this)
    }

    override fun onStop() {
        dispatcher.unregister(this)
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        binding?.refreshAccountDetails()
    }

    override fun onDestroy() {
        disconnectProgressDialog?.dismiss()
        disconnectProgressDialog = null
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun MeFragmentBinding.refreshAccountDetails() {
        if (!FluxCUtils.isSignedInWPComOrHasWPOrgSite(accountStore, siteStore)) {
            return
        }
        // we only want to show user details for WordPress.com users
        if (accountStore.hasAccessToken()) {
            val defaultAccount = accountStore.account
            meDisplayName.visibility = View.VISIBLE
            meUsername.visibility = View.VISIBLE
            cardAvatar.visibility = View.VISIBLE
            rowMyProfile.visibility = View.VISIBLE
            myProfileDivider.visibility = View.VISIBLE
            accountSettingsDivider.visibility = View.VISIBLE
            loadAvatar(null)
            meUsername.text = getString(R.string.at_username, defaultAccount.userName)
            meLoginLogoutTextView.setText(R.string.me_disconnect_from_wordpress_com)
            meDisplayName.text = defaultAccount.displayName.ifEmpty { defaultAccount.userName }
        } else {
            meDisplayName.visibility = View.GONE
            meUsername.visibility = View.GONE
            cardAvatar.visibility = View.GONE
            rowMyProfile.visibility = View.GONE
            myProfileDivider.visibility = View.GONE
            rowAccountSettings.visibility = View.GONE
            accountSettingsDivider.visibility = View.GONE
            meLoginLogoutTextView.setText(R.string.me_connect_to_wordpress_com)
        }
    }

    private fun MeFragmentBinding.loadAvatar(injectFilePath: String?, forceRefresh: Boolean = false) {
        val newAvatarUploaded = !injectFilePath.isNullOrEmpty()
        val avatarUrl = meGravatarLoader.constructGravatarUrl(accountStore.account.avatarUrl)
        val newAvatarSelected = newAvatarUploaded || forceRefresh
        meGravatarLoader.load(
            newAvatarSelected,
            avatarUrl,
            injectFilePath,
            meAvatar,
            AVATAR_WITHOUT_BACKGROUND,
            object : RequestListener<Drawable> {
                override fun onLoadFailed(e: Exception?, model: Any?) {
                    val appLogMessage = "onLoadFailed while loading Gravatar image!"
                    if (e == null) {
                        AppLog.e(
                            MAIN,
                            "$appLogMessage e == null"
                        )
                    } else {
                        AppLog.e(
                            MAIN,
                            appLogMessage,
                            e
                        )
                    }

                    // For some reason, the Activity can be null so, guard for it. See #8590.
                    if (activity != null) {
                        ToastUtils.showToast(
                            activity, R.string.error_refreshing_gravatar,
                            SHORT
                        )
                    }
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any?
                ) {
                    if (newAvatarSelected && resource is BitmapDrawable) {
                        var bitmap = resource.bitmap
                        // create a copy since the original bitmap may by automatically recycled
                        bitmap.config?.let { config ->
                            bitmap = bitmap.copy(config, true)
                        }
                        WordPress.getBitmapCache().put(
                            avatarUrl,
                            bitmap
                        )
                    }
                }
            })
    }

    private fun signOutWordPressComWithConfirmation() {
        // if there are local changes we need to let the user know they'll be lost if they logout, otherwise
        // we use a simpler (less scary!) confirmation
        val message: String = if (postStore.numLocalChanges > 0) {
            getString(R.string.sign_out_wpcom_confirm_with_changes)
        } else {
            getString(R.string.sign_out_wpcom_confirm_with_no_changes)
        }
        MaterialAlertDialogBuilder(requireActivity())
            .setMessage(message)
            .setPositiveButton(
                R.string.signout
            ) { _, _ ->
                clearNotifications()
                enableDeepLinkComponents()
                signOutWordPressCom()
            }
            .setNegativeButton(R.string.cancel, null)
            .setCancelable(true)
            .create().show()
    }

    private fun signOutWordPressCom() {
        viewModel.signOutWordPress(requireActivity().application as WordPress)
    }

    private fun clearNotifications() {
        NotificationsUtils.cancelAllNotifications(requireActivity())
    }

    private fun enableDeepLinkComponents() {
        packageManagerWrapper.enableReaderDeeplinks()
        packageManagerWrapper.enableComponentEnabledSetting(
            DeepLinkOpenWebLinksWithJetpackHelper.WEB_LINKS_DEEPLINK_ACTIVITY_ALIAS
        )
        appPrefsWrapper.setOpenWebLinksWithJetpackOverlayLastShownTimestamp(0L)
        appPrefsWrapper.setIsOpenWebLinksWithJetpack(false)
    }

    @Suppress("DEPRECATION")
    private fun showDisconnectDialog() {
        disconnectProgressDialog = ProgressDialog.show(
            requireContext(),
            null,
            requireContext().getText(R.string.signing_out),
            false
        )
    }

    private fun hideDisconnectDialog() {
        if (disconnectProgressDialog?.isShowing == true) {
            disconnectProgressDialog?.dismiss()
        }
        disconnectProgressDialog = null
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged?) {
        binding?.refreshAccountDetails()
    }

    companion object {
        private const val IS_DISCONNECTING = "IS_DISCONNECTING"
        private const val GRAVATAR_URL = "https://www.gravatar.com"
        fun newInstance(): MeFragment {
            return MeFragment()
        }
    }
}
