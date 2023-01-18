@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.main

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.View.OnClickListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCrop.Options
import com.yalantis.ucrop.UCropActivity
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.R.attr
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.ME_GRAVATAR_CROPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.ME_GRAVATAR_GALLERY_PICKED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.ME_GRAVATAR_SHOT_NEW
import org.wordpress.android.analytics.AnalyticsTracker.Stat.ME_GRAVATAR_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.ME_GRAVATAR_UPLOADED
import org.wordpress.android.databinding.MeFragmentBinding
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.networking.GravatarApi
import org.wordpress.android.networking.GravatarApi.GravatarUploadListener
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.about.UnifiedAboutActivity
import org.wordpress.android.ui.accounts.HelpActivity.Origin.ME_SCREEN_HELP
import org.wordpress.android.ui.deeplinks.DeepLinkOpenWebLinksWithJetpackHelper
import org.wordpress.android.ui.main.MeViewModel.RecommendAppUiState
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener
import org.wordpress.android.ui.main.utils.MeGravatarLoader
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment
import org.wordpress.android.ui.notifications.utils.NotificationsUtils
import org.wordpress.android.ui.photopicker.MediaPickerConstants
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.ui.photopicker.PhotoPickerActivity
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MAIN
import org.wordpress.android.util.AppLog.T.UTILS
import org.wordpress.android.util.FluxCUtils
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.JetpackBrandedScreenData
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.PackageManagerWrapper
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarItem.Info
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration.SHORT
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.config.QRCodeAuthFlowFeatureConfig
import org.wordpress.android.util.config.RecommendTheAppFeatureConfig
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.image.ImageManager.RequestListener
import org.wordpress.android.util.image.ImageType.AVATAR_WITHOUT_BACKGROUND
import org.wordpress.android.viewmodel.observeEvent
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
@Suppress("TooManyFunctions")
class MeFragment : Fragment(R.layout.me_fragment), OnScrollToTopListener {
    @Suppress("DEPRECATION")
    private var disconnectProgressDialog: ProgressDialog? = null
    private var isUpdatingGravatar = false
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
    lateinit var mediaPickerLauncher: MediaPickerLauncher

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

    private val viewModel: MeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        if (savedInstanceState != null) {
            isUpdatingGravatar = savedInstanceState.getBoolean(IS_UPDATING_GRAVATAR)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = MeFragmentBinding.bind(view).apply {
            setupViews()
            setupObservers(savedInstanceState)
        }
    }

    private fun MeFragmentBinding.setupViews() {
        with(requireActivity() as AppCompatActivity) {
            setSupportActionBar(toolbarMain)
            supportActionBar?.apply {
                setHomeButtonEnabled(true)
                setDisplayHomeAsUpEnabled(true)
                // We need to set the title this way so it can be updated on locale change
                setTitle(packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA).labelRes)
            }
        }

        addJetpackBadgeIfNeeded()

        val showPickerListener = OnClickListener {
            AnalyticsTracker.track(ME_GRAVATAR_TAPPED)
            showPhotoPickerForGravatar()
        }
        avatarContainer.setOnClickListener(showPickerListener)
        changePhoto.setOnClickListener(showPickerListener)
        rowMyProfile.setOnClickListener {
            ActivityLauncher.viewMyProfile(activity)
        }
        rowAccountSettings.setOnClickListener {
            ActivityLauncher.viewAccountSettings(activity)
        }
        rowAppSettings.setOnClickListener {
            ActivityLauncher.viewAppSettingsForResult(activity)
        }
        rowSupport.setOnClickListener {
            ActivityLauncher.viewHelpAndSupport(requireContext(), ME_SCREEN_HELP, viewModel.getSite(), null)
        }

        if (BuildConfig.IS_JETPACK_APP) meAboutIcon.setImageResource(R.drawable.ic_jetpack_logo_white_24dp)

        rowAboutTheApp.setOnClickListener {
            viewModel.showUnifiedAbout()
        }

        if (shouldShowQrCodeLogin()) {
            rowScanLoginCode.isVisible = true

            rowScanLoginCode.setOnClickListener {
                viewModel.showScanLoginCode()
            }
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
    }

    private fun MeFragmentBinding.addJetpackBadgeIfNeeded() {
        if (jetpackBrandingUtils.shouldShowJetpackBranding()) {
            val screen = JetpackBrandedScreenData.ME
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
            if (savedInstanceState.getBoolean(IS_UPDATING_GRAVATAR, false)) {
                showGravatarProgressBar(true)
            }
        }

        viewModel.showUnifiedAbout.observeEvent(viewLifecycleOwner, {
            startActivity(Intent(activity, UnifiedAboutActivity::class.java))
        })

        viewModel.showDisconnectDialog.observeEvent(viewLifecycleOwner, {
            when (it) {
                true -> showDisconnectDialog()
                false -> hideDisconnectDialog()
            }
        })

        viewModel.recommendUiState.observeEvent(viewLifecycleOwner, {
            if (!isAdded) return@observeEvent

            manageRecommendUiState(it)
        })

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
        outState.putBoolean(IS_UPDATING_GRAVATAR, isUpdatingGravatar)
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
            loadAvatar(null)
            meUsername.text = getString(R.string.at_username, defaultAccount.userName)
            meLoginLogoutTextView.setText(R.string.me_disconnect_from_wordpress_com)
            meDisplayName.text = defaultAccount.displayName.ifEmpty { defaultAccount.userName }
        } else {
            meDisplayName.visibility = View.GONE
            meUsername.visibility = View.GONE
            cardAvatar.visibility = View.GONE
            avatarProgress.visibility = View.GONE
            rowMyProfile.visibility = View.GONE
            rowAccountSettings.visibility = View.GONE
            meLoginLogoutTextView.setText(R.string.me_connect_to_wordpress_com)
        }
    }

    private fun MeFragmentBinding.showGravatarProgressBar(isUpdating: Boolean) {
        avatarProgress.visibility = if (isUpdating) View.VISIBLE else View.GONE
        isUpdatingGravatar = isUpdating
    }

    private fun MeFragmentBinding.loadAvatar(injectFilePath: String?) {
        val newAvatarUploaded = injectFilePath != null && injectFilePath.isNotEmpty()
        val avatarUrl = meGravatarLoader.constructGravatarUrl(accountStore.account.avatarUrl)
        meGravatarLoader.load(
            newAvatarUploaded,
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
                    if (newAvatarUploaded && resource is BitmapDrawable) {
                        var bitmap = resource.bitmap
                        // create a copy since the original bitmap may by automatically recycled
                        bitmap = bitmap.copy(bitmap.config, true)
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

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION", "LongMethod", "NestedBlockDepth")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // If the fragment is not attached to the activity, we can't start the crop activity or upload the
        // cropped image.
        if (!isAdded) {
            return
        }
        when (requestCode) {
            RequestCodes.PHOTO_PICKER -> if (resultCode == Activity.RESULT_OK && data != null) {
                val mediaUriStringsArray = data.getStringArrayExtra(MediaPickerConstants.EXTRA_MEDIA_URIS)
                if (mediaUriStringsArray == null || mediaUriStringsArray.size == 0) {
                    AppLog.e(
                        UTILS,
                        "Can't resolve picked or captured image"
                    )
                    return
                }
                val source = PhotoPickerActivity.PhotoPickerMediaSource.fromString(
                    data.getStringExtra(MediaPickerConstants.EXTRA_MEDIA_SOURCE)
                )
                val stat = if (source == PhotoPickerActivity.PhotoPickerMediaSource.ANDROID_CAMERA) {
                    ME_GRAVATAR_SHOT_NEW
                } else {
                    ME_GRAVATAR_GALLERY_PICKED
                }
                AnalyticsTracker.track(stat)
                val imageUri = Uri.parse(mediaUriStringsArray[0])
                if (imageUri != null) {
                    val didGoWell = WPMediaUtils.fetchMediaAndDoNext(
                        activity,
                        imageUri
                    ) { uri: Uri -> startCropActivity(uri) }
                    if (!didGoWell) {
                        AppLog.e(
                            UTILS,
                            "Can't download picked or captured image"
                        )
                    }
                }
            }
            UCrop.REQUEST_CROP -> {
                AnalyticsTracker.track(ME_GRAVATAR_CROPPED)
                if (resultCode == Activity.RESULT_OK) {
                    WPMediaUtils.fetchMediaAndDoNext(
                        activity, UCrop.getOutput(data!!)
                    ) { uri: Uri? ->
                        startGravatarUpload(
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
            }
        }
    }

    private fun showPhotoPickerForGravatar() {
        mediaPickerLauncher.showGravatarPicker(this)
    }

    private fun startCropActivity(uri: Uri) {
        val context = activity ?: return
        val options = Options()
        options.setShowCropGrid(false)
        options.setStatusBarColor(context.getColorFromAttribute(android.R.attr.statusBarColor))
        options.setToolbarColor(context.getColorFromAttribute(R.attr.wpColorAppBar))
        options.setToolbarWidgetColor(context.getColorFromAttribute(attr.colorOnSurface))
        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.NONE, UCropActivity.NONE)
        options.setHideBottomControls(true)
        UCrop.of(uri, Uri.fromFile(File(context.cacheDir, "cropped_for_gravatar.jpg")))
            .withAspectRatio(1f, 1f)
            .withOptions(options)
            .start(requireActivity(), this)
    }

    private fun startGravatarUpload(filePath: String) {
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
                R.string.error_locating_image,
                SHORT
            )
            return
        }
        binding?.showGravatarProgressBar(true)
        GravatarApi.uploadGravatar(file, accountStore.account.email, accountStore.accessToken,
            object : GravatarUploadListener {
                override fun onSuccess() {
                    EventBus.getDefault().post(GravatarUploadFinished(filePath, true))
                }

                override fun onError() {
                    EventBus.getDefault().post(GravatarUploadFinished(filePath, false))
                }
            })
    }

    class GravatarUploadFinished internal constructor(val filePath: String, val success: Boolean)

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: GravatarUploadFinished) {
        binding?.showGravatarProgressBar(false)
        if (event.success) {
            AnalyticsTracker.track(ME_GRAVATAR_UPLOADED)
            binding?.loadAvatar(event.filePath)
        } else {
            ToastUtils.showToast(
                activity,
                R.string.error_updating_gravatar,
                SHORT
            )
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged?) {
        binding?.refreshAccountDetails()
    }

    companion object {
        private const val IS_DISCONNECTING = "IS_DISCONNECTING"
        private const val IS_UPDATING_GRAVATAR = "IS_UPDATING_GRAVATAR"
        fun newInstance(): MeFragment {
            return MeFragment()
        }
    }
}
