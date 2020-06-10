package org.wordpress.android.ui.main

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCrop.Options
import com.yalantis.ucrop.UCropActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.R.color
import org.wordpress.android.R.layout
import org.wordpress.android.R.string
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
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MAIN
import org.wordpress.android.util.AppLog.T.UTILS
import org.wordpress.android.util.FluxCUtils
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration.SHORT
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageManager.RequestListener
import org.wordpress.android.util.image.ImageType.AVATAR_WITHOUT_BACKGROUND
import java.io.File
import java.lang.ref.WeakReference
import javax.inject.Inject

class MeFragment : Fragment(), OnScrollToTopListener {
    private var mAvatarCard: ViewGroup? = null
    private var mProgressBar: View? = null
    private var mAvatarImageView: ImageView? = null
    private var mDisplayNameTextView: TextView? = null
    private var mUsernameTextView: TextView? = null
    private var mLoginLogoutTextView: TextView? = null
    private var mMyProfileView: View? = null
    private var mAccountSettingsView: View? = null
    private var mDisconnectProgressDialog: ProgressDialog? = null
    private var mScrollView: ScrollView? = null
    private var mToolbar: Toolbar? = null
    private val mToolbarTitle: String? = null
    private var mIsUpdatingGravatar = false

    @JvmField @Inject
    var mDispatcher: Dispatcher? = null

    @JvmField @Inject
    var mAccountStore: AccountStore? = null

    @JvmField @Inject
    var mSiteStore: SiteStore? = null

    @JvmField @Inject
    var mImageManager: ImageManager? = null

    @JvmField @Inject
    var mAppPrefsWrapper: AppPrefsWrapper? = null

    @JvmField @Inject
    var mPostStore: PostStore? = null

    @JvmField @Inject
    var mMeGravatarLoader: MeGravatarLoader? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity!!.application as WordPress).component().inject(this)
        if (savedInstanceState != null) {
            mIsUpdatingGravatar = savedInstanceState.getBoolean(IS_UPDATING_GRAVATAR)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(layout.me_fragment, container, false) as ViewGroup
        mAvatarCard = rootView.findViewById(R.id.card_avatar)
        val avatarContainer = rootView.findViewById<ViewGroup>(R.id.avatar_container)
        mAvatarImageView = rootView.findViewById(R.id.me_avatar)
        mProgressBar = rootView.findViewById(R.id.avatar_progress)
        mDisplayNameTextView = rootView.findViewById(R.id.me_display_name)
        mUsernameTextView = rootView.findViewById(R.id.me_username)
        mLoginLogoutTextView = rootView.findViewById(R.id.me_login_logout_text_view)
        mMyProfileView = rootView.findViewById(R.id.row_my_profile)
        mAccountSettingsView = rootView.findViewById(R.id.row_account_settings)
        mScrollView = rootView.findViewById(R.id.scroll_view)
        val showPickerListener = OnClickListener { v: View? ->
            AnalyticsTracker.track(ME_GRAVATAR_TAPPED)
            showPhotoPickerForGravatar()
        }
        avatarContainer.setOnClickListener(showPickerListener)
        rootView.findViewById<View>(R.id.change_photo).setOnClickListener(showPickerListener)
        mMyProfileView.setOnClickListener(OnClickListener { v: View? ->
            ActivityLauncher.viewMyProfile(
                    activity
            )
        })
        mAccountSettingsView.setOnClickListener(OnClickListener { v: View? ->
            ActivityLauncher.viewAccountSettings(
                    activity
            )
        })
        rootView.findViewById<View>(R.id.row_app_settings).setOnClickListener { v: View? ->
            ActivityLauncher.viewAppSettingsForResult(
                    activity
            )
        }
        rootView.findViewById<View>(R.id.row_support).setOnClickListener { v: View? ->
            ActivityLauncher
                    .viewHelpAndSupport(
                            activity!!,
                            ME_SCREEN_HELP,
                            selectedSite,
                            null
                    )
        }
        rootView.findViewById<View>(R.id.row_logout)
                .setOnClickListener { v: View? ->
                    if (mAccountStore!!.hasAccessToken()) {
                        signOutWordPressComWithConfirmation()
                    } else {
                        ActivityLauncher.showSignInForResult(activity)
                    }
                }
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(IS_DISCONNECTING, false)) {
                showDisconnectDialog(activity)
            }
            if (savedInstanceState.getBoolean(IS_UPDATING_GRAVATAR, false)) {
                showGravatarProgressBar(true)
            }
        }
        mToolbar = rootView.findViewById(R.id.toolbar_main)
        mToolbar.setTitle(mToolbarTitle)
        return rootView
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (mDisconnectProgressDialog != null) {
            outState.putBoolean(IS_DISCONNECTING, true)
        }
        outState.putBoolean(IS_UPDATING_GRAVATAR, mIsUpdatingGravatar)
        super.onSaveInstanceState(outState)
    }

    override fun onScrollToTop() {
        if (isAdded) {
            mScrollView!!.smoothScrollTo(0, 0)
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
        mDispatcher!!.register(this)
    }

    override fun onStop() {
        mDispatcher!!.unregister(this)
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        refreshAccountDetails()
    }

    override fun onDestroy() {
        if (mDisconnectProgressDialog != null) {
            mDisconnectProgressDialog!!.dismiss()
            mDisconnectProgressDialog = null
        }
        super.onDestroy()
    }

    private fun refreshAccountDetails() {
        if (!FluxCUtils.isSignedInWPComOrHasWPOrgSite(mAccountStore, mSiteStore)) {
            return
        }
        // we only want to show user details for WordPress.com users
        if (mAccountStore!!.hasAccessToken()) {
            val defaultAccount = mAccountStore!!.account
            mDisplayNameTextView!!.visibility = View.VISIBLE
            mUsernameTextView!!.visibility = View.VISIBLE
            mAvatarCard!!.visibility = View.VISIBLE
            mMyProfileView!!.visibility = View.VISIBLE
            loadAvatar(null)
            mUsernameTextView!!.text = getString(string.at_username, defaultAccount.userName)
            mLoginLogoutTextView!!.setText(string.me_disconnect_from_wordpress_com)
            val displayName = defaultAccount.displayName
            if (!TextUtils.isEmpty(displayName)) {
                mDisplayNameTextView!!.text = displayName
            } else {
                mDisplayNameTextView!!.text = defaultAccount.userName
            }
        } else {
            mDisplayNameTextView!!.visibility = View.GONE
            mUsernameTextView!!.visibility = View.GONE
            mAvatarCard!!.visibility = View.GONE
            mProgressBar!!.visibility = View.GONE
            mMyProfileView!!.visibility = View.GONE
            mAccountSettingsView!!.visibility = View.GONE
            mLoginLogoutTextView!!.setText(string.me_connect_to_wordpress_com)
        }
    }

    private fun showGravatarProgressBar(isUpdating: Boolean) {
        mProgressBar!!.visibility = if (isUpdating) View.VISIBLE else View.GONE
        mIsUpdatingGravatar = isUpdating
    }

    private fun loadAvatar(injectFilePath: String?) {
        val newAvatarUploaded = injectFilePath != null && !injectFilePath.isEmpty()
        val avatarUrl = mMeGravatarLoader!!.constructGravatarUrl(mAccountStore!!.account.avatarUrl)
        mMeGravatarLoader!!.load(
                newAvatarUploaded,
                avatarUrl,
                injectFilePath,
                mAvatarImageView!!,
                AVATAR_WITHOUT_BACKGROUND,
                object : RequestListener<Drawable?> {
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
                                    activity, string.error_refreshing_gravatar,
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
        val message: String
        message = if (mPostStore!!.numLocalChanges > 0) {
            getString(string.sign_out_wpcom_confirm_with_changes)
        } else {
            getString(string.sign_out_wpcom_confirm_with_no_changes)
        }
        MaterialAlertDialogBuilder(activity)
                .setMessage(message)
                .setPositiveButton(
                        string.signout
                ) { dialog: DialogInterface?, whichButton: Int -> signOutWordPressCom() }
                .setNegativeButton(string.cancel, null)
                .setCancelable(true)
                .create().show()
    }

    private fun signOutWordPressCom() {
        // note that signing out sends a CoreEvents.UserSignedOutWordPressCom EventBus event,
        // which will cause the main activity to recreate this fragment
        SignOutWordPressComAsync(activity).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun showDisconnectDialog(context: Context?) {
        mDisconnectProgressDialog = ProgressDialog.show(context, null, context!!.getText(string.signing_out), false)
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
                            string.error_cropping_image,
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
        options.setStatusBarColor(ContextCompat.getColor(context, color.status_bar))
        options.setToolbarColor(ContextCompat.getColor(context, color.primary))
        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.NONE, UCropActivity.NONE)
        options.setHideBottomControls(true)
        UCrop.of(uri, Uri.fromFile(File(context.cacheDir, "cropped_for_gravatar.jpg")))
                .withAspectRatio(1f, 1f)
                .withOptions(options)
                .start(activity!!, this)
    }

    private fun startGravatarUpload(filePath: String) {
        if (TextUtils.isEmpty(filePath)) {
            ToastUtils.showToast(
                    activity,
                    string.error_locating_image,
                    SHORT
            )
            return
        }
        val file = File(filePath)
        if (!file.exists()) {
            ToastUtils.showToast(
                    activity,
                    string.error_locating_image,
                    SHORT
            )
            return
        }
        showGravatarProgressBar(true)
        GravatarApi.uploadGravatar(file, mAccountStore!!.account.email, mAccountStore!!.accessToken,
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
                    string.error_updating_gravatar,
                    SHORT
            )
        }
    }

    private inner class SignOutWordPressComAsync internal constructor(context: Context?) : AsyncTask<Void?, Void?, Void?>() {
        var mWeakContext: WeakReference<Context?>
        override fun onPreExecute() {
            super.onPreExecute()
            val context = mWeakContext.get()
            context?.let { showDisconnectDialog(it) }
        }

        protected override fun doInBackground(vararg params: Void): Void? {
            val context = mWeakContext.get()
            if (context != null) {
                (activity!!.application as WordPress).wordPressComSignOut()
            }
            return null
        }

        override fun onPostExecute(aVoid: Void?) {
            super.onPostExecute(aVoid)
            if (mDisconnectProgressDialog != null && mDisconnectProgressDialog!!.isShowing) {
                mDisconnectProgressDialog!!.dismiss()
            }
            mDisconnectProgressDialog = null
        }

        init {
            mWeakContext = WeakReference(context)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged?) {
        refreshAccountDetails()
    }

    private val selectedSite: SiteModel?
        private get() {
            if (activity is WPMainActivity) {
                val mainActivity = activity as WPMainActivity?
                return mainActivity!!.selectedSite
            }
            return null
        }

    companion object {
        private const val IS_DISCONNECTING = "IS_DISCONNECTING"
        private const val IS_UPDATING_GRAVATAR = "IS_UPDATING_GRAVATAR"
        fun newInstance(): MeFragment {
            return MeFragment()
        }
    }
}