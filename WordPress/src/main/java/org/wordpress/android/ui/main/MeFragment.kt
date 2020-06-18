package org.wordpress.android.ui.main

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCrop.Options
import com.yalantis.ucrop.UCropActivity
import kotlinx.android.synthetic.main.me_fragment.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.ME_GRAVATAR_CROPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.ME_GRAVATAR_GALLERY_PICKED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.ME_GRAVATAR_SHOT_NEW
import org.wordpress.android.analytics.AnalyticsTracker.Stat.ME_GRAVATAR_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.ME_GRAVATAR_UPLOADED
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.networking.GravatarApi
import org.wordpress.android.networking.GravatarApi.GravatarUploadListener
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.accounts.HelpActivity.Origin.ME_SCREEN_HELP
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener
import org.wordpress.android.ui.main.utils.MeGravatarLoader
import org.wordpress.android.ui.media.MediaBrowserType.GRAVATAR_IMAGE_PICKER
import org.wordpress.android.ui.photopicker.PhotoPickerActivity
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource.ANDROID_CAMERA
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MAIN
import org.wordpress.android.util.AppLog.T.UTILS
import org.wordpress.android.util.FluxCUtils
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration.SHORT
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.image.ImageManager.RequestListener
import org.wordpress.android.util.image.ImageType.AVATAR_WITHOUT_BACKGROUND
import java.io.File
import javax.inject.Inject

class MeFragment : Fragment(), OnScrollToTopListener {
    private var disconnectProgressDialog: ProgressDialog? = null
    private var isUpdatingGravatar = false

    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var accountStore: AccountStore
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var postStore: PostStore
    @Inject lateinit var meGravatarLoader: MeGravatarLoader
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: MeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        if (savedInstanceState != null) {
            isUpdatingGravatar = savedInstanceState.getBoolean(IS_UPDATING_GRAVATAR)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.me_fragment, container, false) as ViewGroup
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val showPickerListener = OnClickListener {
            AnalyticsTracker.track(ME_GRAVATAR_TAPPED)
            showPhotoPickerForGravatar()
        }
        avatar_container.setOnClickListener(showPickerListener)
        change_photo.setOnClickListener(showPickerListener)
        row_my_profile.setOnClickListener {
            ActivityLauncher.viewMyProfile(
                    activity
            )
        }
        row_account_settings.setOnClickListener {
            ActivityLauncher.viewAccountSettings(
                    activity
            )
        }
        row_app_settings.setOnClickListener {
            ActivityLauncher.viewAppSettingsForResult(
                    activity
            )
        }
        row_support.setOnClickListener {
            ActivityLauncher
                    .viewHelpAndSupport(
                            requireContext(),
                            ME_SCREEN_HELP,
                            selectedSite,
                            null
                    )
        }
        row_logout.setOnClickListener {
            if (accountStore.hasAccessToken()) {
                signOutWordPressComWithConfirmation()
            } else {
                ActivityLauncher.showSignInForResult(activity)
            }
        }
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(IS_DISCONNECTING, false)) {
                viewModel.openDisconnectDialog()
            }
            if (savedInstanceState.getBoolean(IS_UPDATING_GRAVATAR, false)) {
                showGravatarProgressBar(true)
            }
        }

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(MeViewModel::class.java)
        viewModel.showDisconnectDialog.observe(viewLifecycleOwner, Observer {
            it.applyIfNotHandled {
                when (this) {
                    true -> showDisconnectDialog()
                    false -> hideDisconnectDialog()
                }
            }
        })
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
            scroll_view.smoothScrollTo(0, 0)
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
        refreshAccountDetails()
    }

    override fun onDestroy() {
        disconnectProgressDialog?.dismiss()
        disconnectProgressDialog = null
        super.onDestroy()
    }

    private fun refreshAccountDetails() {
        if (!FluxCUtils.isSignedInWPComOrHasWPOrgSite(accountStore, siteStore)) {
            return
        }
        // we only want to show user details for WordPress.com users
        if (accountStore.hasAccessToken()) {
            val defaultAccount = accountStore.account
            me_display_name.visibility = View.VISIBLE
            me_username.visibility = View.VISIBLE
            card_avatar.visibility = View.VISIBLE
            row_my_profile.visibility = View.VISIBLE
            loadAvatar(null)
            me_username.text = getString(R.string.at_username, defaultAccount.userName)
            me_login_logout_text_view.setText(R.string.me_disconnect_from_wordpress_com)
            val displayName = defaultAccount.displayName
            if (!TextUtils.isEmpty(displayName)) {
                me_display_name.text = displayName
            } else {
                me_display_name.text = defaultAccount.userName
            }
        } else {
            me_display_name.visibility = View.GONE
            me_username.visibility = View.GONE
            card_avatar.visibility = View.GONE
            avatar_progress.visibility = View.GONE
            row_my_profile.visibility = View.GONE
            row_account_settings.visibility = View.GONE
            me_login_logout_text_view.setText(R.string.me_connect_to_wordpress_com)
        }
    }

    private fun showGravatarProgressBar(isUpdating: Boolean) {
        avatar_progress.visibility = if (isUpdating) View.VISIBLE else View.GONE
        isUpdatingGravatar = isUpdating
    }

    private fun loadAvatar(injectFilePath: String?) {
        val newAvatarUploaded = injectFilePath != null && injectFilePath.isNotEmpty()
        val avatarUrl = meGravatarLoader.constructGravatarUrl(accountStore.account.avatarUrl)
        meGravatarLoader.load(
                newAvatarUploaded,
                avatarUrl,
                injectFilePath,
                me_avatar,
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
        MaterialAlertDialogBuilder(activity)
                .setMessage(message)
                .setPositiveButton(
                        R.string.signout
                ) { _, _ -> signOutWordPressCom() }
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true)
                .create().show()
    }

    private fun signOutWordPressCom() {
        viewModel.signOutWordPress(requireActivity().application as WordPress)
    }

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // If the fragment is not attached to the activity, we can't start the crop activity or upload the
        // cropped image.
        if (!isAdded) {
            return
        }
        when (requestCode) {
            RequestCodes.PHOTO_PICKER -> if (resultCode == Activity.RESULT_OK && data != null) {
                val mediaUriStringsArray = data.getStringArrayExtra(PhotoPickerActivity.EXTRA_MEDIA_URIS)
                if (mediaUriStringsArray == null || mediaUriStringsArray.size == 0) {
                    AppLog.e(
                            UTILS,
                            "Can't resolve picked or captured image"
                    )
                    return
                }
                val source = PhotoPickerMediaSource.fromString(
                        data.getStringExtra(PhotoPickerActivity.EXTRA_MEDIA_SOURCE)
                )
                val stat = if (source == ANDROID_CAMERA) ME_GRAVATAR_SHOT_NEW else ME_GRAVATAR_GALLERY_PICKED
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
        ActivityLauncher.showPhotoPickerForResult(this, GRAVATAR_IMAGE_PICKER, null, null)
    }

    private fun startCropActivity(uri: Uri) {
        val context = activity ?: return
        val options = Options()
        options.setShowCropGrid(false)
        options.setStatusBarColor(ContextCompat.getColor(context, R.color.status_bar))
        options.setToolbarColor(ContextCompat.getColor(context, R.color.primary))
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
        showGravatarProgressBar(true)
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
        showGravatarProgressBar(false)
        if (event.success) {
            AnalyticsTracker.track(ME_GRAVATAR_UPLOADED)
            loadAvatar(event.filePath)
        } else {
            ToastUtils.showToast(
                    activity,
                    R.string.error_updating_gravatar,
                    SHORT
            )
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged?) {
        refreshAccountDetails()
    }

    private val selectedSite: SiteModel?
        get() {
            return (activity as? WPMainActivity)?.selectedSite
        }

    companion object {
        private const val IS_DISCONNECTING = "IS_DISCONNECTING"
        private const val IS_UPDATING_GRAVATAR = "IS_UPDATING_GRAVATAR"
        fun newInstance(): MeFragment {
            return MeFragment()
        }
    }
}
