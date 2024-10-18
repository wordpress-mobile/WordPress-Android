@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.accounts.signup

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.gravatar.services.AvatarService
import com.gravatar.services.GravatarResult
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.WordPress.Companion.getBitmapCache
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AccountUsernameActionType
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnUsernameChanged
import org.wordpress.android.fluxc.store.AccountStore.PushAccountSettingsPayload
import org.wordpress.android.fluxc.store.AccountStore.PushUsernamePayload
import org.wordpress.android.login.LoginBaseFormFragment
import org.wordpress.android.login.widgets.WPLoginInputRow
import org.wordpress.android.ui.FullScreenDialogFragment
import org.wordpress.android.ui.FullScreenDialogFragment.OnShownListener
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.accounts.UnifiedLoginTracker
import org.wordpress.android.ui.photopicker.MediaPickerConstants
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.WPAvatarUtils
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.extensions.redirectContextClickToLongPressListener
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.widgets.WPTextView
import java.io.File
import java.net.URI
import java.net.URISyntaxException
import java.util.EnumSet
import java.util.Locale
import javax.inject.Inject

@Suppress("LargeClass")
class SignupEpilogueFragment : LoginBaseFormFragment<SignupEpilogueListener?>(),
    FullScreenDialogFragment.OnConfirmListener, FullScreenDialogFragment.OnDismissListener,
    OnShownListener {
    private lateinit var mEditTextDisplayName: EditText
    private lateinit var mEditTextUsername: EditText
    private var mDialog: FullScreenDialogFragment? = null
    private var mSignupEpilogueListener: SignupEpilogueListener? = null

    private lateinit var mHeaderAvatarAdd: ImageView
    private var mDisplayName: String? = null
    private lateinit var mEmailAddress: String
    private lateinit var mPhotoUrl: String
    private var mUsername: String? = null
    private lateinit var mInputPassword: WPLoginInputRow
    private lateinit var mHeaderAvatar: ImageView
    private lateinit var mHeaderDisplayName: WPTextView
    private lateinit var mHeaderEmailAddress: WPTextView
    private var mBottomShadow: View? = null
    private lateinit var mScrollView: NestedScrollView
    private var mIsAvatarAdded: Boolean = false
    private var mIsEmailSignup: Boolean = false

    private var mIsUpdatingDisplayName = false
    private var mIsUpdatingPassword = false
    private var mHasUpdatedPassword = false
    private var mHasMadeUpdates = false

    @Inject
    lateinit var mAccount: AccountStore

    @Inject
    lateinit var dispatcher: Dispatcher

    @Inject
    lateinit var mImageManager: ImageManager

    @Inject
    lateinit var mAppPrefsWrapper: AppPrefsWrapper

    @Inject
    lateinit var mUnifiedLoginTracker: UnifiedLoginTracker

    @Inject
    lateinit var mSignupUtils: SignupUtils

    @Inject
    lateinit var mMediaPickerLauncher: MediaPickerLauncher

    @Inject
    lateinit var mAvatarService: AvatarService

    @LayoutRes
    override fun getContentLayout(): Int {
        return 0 // no content layout; entire view is inflated in createMainView
    }

    @LayoutRes
    override fun getProgressBarText(): Int {
        return R.string.signup_updating_account
    }

    override fun setupLabel(label: TextView) {
        // no label in this screen
    }

    override fun createMainView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): ViewGroup {
        return inflater.inflate(R.layout.signup_epilogue, container, false) as ViewGroup
    }

    @Suppress("LongMethod")
    override fun setupContent(rootView: ViewGroup) {
        val headerAvatarLayout =
            rootView.findViewById<FrameLayout>(R.id.login_epilogue_header_avatar_layout)
        headerAvatarLayout.isEnabled = mIsEmailSignup
        headerAvatarLayout.setOnClickListener {
            mUnifiedLoginTracker.trackClick(UnifiedLoginTracker.Click.SELECT_AVATAR)
            mMediaPickerLauncher.showGravatarPicker(this@SignupEpilogueFragment)
        }
        headerAvatarLayout.setOnLongClickListener {
            ToastUtils.showToast(
                activity, getString(R.string.content_description_add_avatar),
                ToastUtils.Duration.SHORT
            )
            true
        }
        headerAvatarLayout.redirectContextClickToLongPressListener()
        mHeaderAvatarAdd = rootView.findViewById(R.id.login_epilogue_header_avatar_add)
        mHeaderAvatarAdd.setVisibility(if (mIsEmailSignup) View.VISIBLE else View.GONE)
        mHeaderAvatar = rootView.findViewById(R.id.login_epilogue_header_avatar)
        mHeaderDisplayName = rootView.findViewById(R.id.login_epilogue_header_title)
        mHeaderDisplayName.text = mDisplayName
        mHeaderEmailAddress = rootView.findViewById(R.id.login_epilogue_header_subtitle)
        mHeaderEmailAddress.text = mEmailAddress
        val inputDisplayName =
            rootView.findViewById<WPLoginInputRow>(R.id.signup_epilogue_input_display)
        mEditTextDisplayName = inputDisplayName.editText
        mEditTextDisplayName.setText(mDisplayName)
        mEditTextDisplayName.addTextChangedListener(object : TextWatcher {
            @Suppress("EmptyFunctionBlock")
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            @Suppress("EmptyFunctionBlock")
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                mDisplayName = s.toString()
                mHeaderDisplayName.text = mDisplayName
            }
        })
        val inputUsername =
            rootView.findViewById<WPLoginInputRow>(R.id.signup_epilogue_input_username)
        mEditTextUsername = inputUsername.editText
        mEditTextUsername.setText(mUsername)
        mEditTextUsername.setOnClickListener {
            mUnifiedLoginTracker.trackClick(UnifiedLoginTracker.Click.EDIT_USERNAME)
            launchDialog()
        }
        mEditTextUsername.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                launchDialog()
            }
        }
        mEditTextUsername.setOnKeyListener { _, keyCode, _ ->
            // Consume keyboard events except for Enter (i.e. click/tap) and Tab (i.e. focus/navigation).
            // The onKey method returns true if the listener has consumed the event and false otherwise
            // allowing hardware keyboard users to tap and navigate, but not input text as expected.
            // This allows the username changer to launch using the keyboard.
            !(keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_TAB)
        }
        mInputPassword = rootView.findViewById(R.id.signup_epilogue_input_password)
        mInputPassword.visibility = if (mIsEmailSignup) View.VISIBLE else View.GONE
        val passwordDetail =
            rootView.findViewById<WPTextView>(R.id.signup_epilogue_input_password_detail)
        passwordDetail.visibility = if (mIsEmailSignup) View.VISIBLE else View.GONE

        // Set focus on static text field to avoid showing keyboard on start.
        mHeaderEmailAddress.requestFocus()

        mBottomShadow = rootView.findViewById(R.id.bottom_shadow)
        mScrollView = rootView.findViewById(R.id.scroll_view)
        mScrollView.setOnScrollChangeListener { _: NestedScrollView?, _: Int, _: Int, _: Int, _: Int ->
            showBottomShadowIfNeeded()
        }
        // We must use onGlobalLayout here otherwise canScrollVertically will always return false
        mScrollView.getViewTreeObserver()
            .addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mScrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                    showBottomShadowIfNeeded()
                }
            })
    }

    private fun showBottomShadowIfNeeded() {
        val canScrollDown = mScrollView.canScrollVertically(1)
        if (mBottomShadow != null) {
            mBottomShadow!!.visibility =
                if (canScrollDown) View.VISIBLE else View.GONE
        }
    }

    override fun setupBottomButton(button: Button) {
        button.setOnClickListener {
            mUnifiedLoginTracker.trackClick(UnifiedLoginTracker.Click.CONTINUE)
            updateAccountOrContinue()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        dispatcher.dispatch(AccountActionBuilder.newFetchAccountAction())

        mDisplayName = requireArguments().getString(ARG_DISPLAY_NAME)
        mEmailAddress = StringUtils.notNullStr(requireArguments().getString(ARG_EMAIL_ADDRESS))
        mPhotoUrl = StringUtils.notNullStr(requireArguments().getString(ARG_PHOTO_URL))
        mUsername = requireArguments().getString(ARG_USERNAME)
        mIsEmailSignup = requireArguments().getBoolean(ARG_IS_EMAIL_SIGNUP)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState == null) {
            // Start loading reader tags so they will be available asap
            ReaderUpdateServiceStarter.startService(
                WordPress.getContext(),
                EnumSet.of(UpdateTask.TAGS)
            )

            mUnifiedLoginTracker.track(step = UnifiedLoginTracker.Step.SUCCESS)
            if (mIsEmailSignup) {
                AnalyticsTracker.track(Stat.SIGNUP_EMAIL_EPILOGUE_VIEWED)

                // Start progress and wait for account to be fetched before populating views when
                // email does not exist in account store.
                if (TextUtils.isEmpty(mAccountStore.account.email)) {
                    startProgress(false)
                } else {
                    // Skip progress and populate views when email does exist in account store.
                    populateViews()
                }
            } else {
                AnalyticsTracker.track(Stat.SIGNUP_SOCIAL_EPILOGUE_VIEWED)
                mAccount.accessToken?.let { accessToken ->
                    DownloadAvatarAndUploadGravatarThread(
                        mPhotoUrl,
                        mEmailAddress,
                        accessToken
                    ).start()
                    mImageManager.loadIntoCircle(
                        mHeaderAvatar,
                        ImageType.AVATAR_WITHOUT_BACKGROUND,
                        (mPhotoUrl)
                    )
                }
            }
        } else {
            mDialog = parentFragmentManager
                .findFragmentByTag(FullScreenDialogFragment.TAG) as FullScreenDialogFragment?

            if (mDialog != null) {
                mDialog!!.setOnConfirmListener(this)
                mDialog!!.setOnDismissListener(this)
            }

            mDisplayName = savedInstanceState.getString(KEY_DISPLAY_NAME)
            mUsername = savedInstanceState.getString(KEY_USERNAME)
            mIsAvatarAdded = savedInstanceState.getBoolean(KEY_IS_AVATAR_ADDED)

            if (mIsEmailSignup) {
                mPhotoUrl = StringUtils.notNullStr(savedInstanceState.getString(KEY_PHOTO_URL))
                mEmailAddress = StringUtils.notNullStr(savedInstanceState.getString(KEY_EMAIL_ADDRESS))
                mHeaderEmailAddress.text = mEmailAddress
                mHeaderAvatarAdd.visibility = if (mIsAvatarAdded) View.GONE else View.VISIBLE
            }
            mImageManager.loadIntoCircle(
                mHeaderAvatar,
                ImageType.AVATAR_WITHOUT_BACKGROUND,
                mPhotoUrl
            )

            mIsUpdatingDisplayName = savedInstanceState.getBoolean(KEY_IS_UPDATING_DISPLAY_NAME)
            mIsUpdatingPassword = savedInstanceState.getBoolean(KEY_IS_UPDATING_PASSWORD)
            mHasUpdatedPassword = savedInstanceState.getBoolean(KEY_HAS_UPDATED_PASSWORD)
            mHasMadeUpdates = savedInstanceState.getBoolean(KEY_HAS_MADE_UPDATES)
        }
    }

    @Suppress("deprecation", "NestedBlockDepth", "LongMethod")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (isAdded) {
            when (resultCode) {
                Activity.RESULT_OK -> when (requestCode) {
                    RequestCodes.PHOTO_PICKER -> if (data != null) {
                        val mediaUriStringsArray =
                            data.getStringArrayExtra(MediaPickerConstants.EXTRA_MEDIA_URIS)

                        if (mediaUriStringsArray != null && mediaUriStringsArray.size > 0) {
                            val source =
                                PhotoPickerMediaSource.fromString(
                                    data.getStringExtra(MediaPickerConstants.EXTRA_MEDIA_SOURCE)
                                )
                            val stat =
                                if (source == PhotoPickerMediaSource.ANDROID_CAMERA) {
                                    Stat.SIGNUP_EMAIL_EPILOGUE_GRAVATAR_SHOT_NEW
                                } else {
                                    Stat.SIGNUP_EMAIL_EPILOGUE_GRAVATAR_GALLERY_PICKED
                                }
                            AnalyticsTracker.track(stat)
                            val imageUri = Uri.parse(mediaUriStringsArray[0])

                            if (imageUri != null) {
                                val wasSuccess = WPMediaUtils.fetchMediaAndDoNext(
                                    activity, imageUri
                                ) { uri -> startCropActivity(uri) }

                                if (!wasSuccess) {
                                    AppLog.e(
                                        AppLog.T.UTILS,
                                        "Can't download picked or captured image"
                                    )
                                }
                            } else {
                                AppLog.e(AppLog.T.UTILS, "Can't parse media string")
                            }
                        } else {
                            AppLog.e(AppLog.T.UTILS, "Can't resolve picked or captured image")
                        }
                    }

                    UCrop.REQUEST_CROP -> {
                        AnalyticsTracker.track(Stat.SIGNUP_EMAIL_EPILOGUE_GRAVATAR_CROPPED)
                        WPMediaUtils.fetchMediaAndDoNext(
                            activity, UCrop.getOutput((data)!!)
                        ) { uri ->
                            startGravatarUpload(
                                MediaUtils.getRealPathFromURI(
                                    activity, uri
                                )
                            )
                        }
                    }
                }

                UCrop.RESULT_ERROR -> {
                    AppLog.e(
                        AppLog.T.NUX, "Image cropping failed", UCrop.getError(
                            (data)!!
                        )
                    )
                    ToastUtils.showToast(
                        activity,
                        R.string.error_cropping_image,
                        ToastUtils.Duration.SHORT
                    )
                }
            }
        }
    }

    @Suppress("TooGenericExceptionThrown")
    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is SignupEpilogueListener) {
            mSignupEpilogueListener = context
        } else {
            throw RuntimeException("$context must implement SignupEpilogueListener")
        }
    }

    override fun onConfirm(result: Bundle?) {
        if (result != null) {
            mUsername = result.getString(BaseUsernameChangerFullScreenDialogFragment.RESULT_USERNAME)
            mEditTextUsername.setText(mUsername)
        }
    }

    override fun onDismiss() {
        val props: MutableMap<String, String?> = HashMap()
        props[SOURCE] = SOURCE_SIGNUP_EPILOGUE
        AnalyticsTracker.track(Stat.CHANGE_USERNAME_DISMISSED, props)
        mDialog = null
    }

    override fun onShown() {
        val props: MutableMap<String, String?> = HashMap()
        props[SOURCE] = SOURCE_SIGNUP_EPILOGUE
        AnalyticsTracker.track(Stat.CHANGE_USERNAME_DISPLAYED, props)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PHOTO_URL, mPhotoUrl)
        outState.putString(KEY_DISPLAY_NAME, mDisplayName)
        outState.putString(KEY_EMAIL_ADDRESS, mEmailAddress)
        outState.putString(KEY_USERNAME, mUsername)
        outState.putBoolean(KEY_IS_AVATAR_ADDED, mIsAvatarAdded)
        outState.putBoolean(KEY_IS_UPDATING_DISPLAY_NAME, mIsUpdatingDisplayName)
        outState.putBoolean(KEY_IS_UPDATING_PASSWORD, mIsUpdatingPassword)
        outState.putBoolean(KEY_HAS_UPDATED_PASSWORD, mHasUpdatedPassword)
        outState.putBoolean(KEY_HAS_MADE_UPDATES, mHasMadeUpdates)
    }

    @Suppress("EmptyFunctionBlock")
    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
    }

    @Suppress("EmptyFunctionBlock")
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
    }

    @Suppress("EmptyFunctionBlock")
    override fun afterTextChanged(s: Editable) {
    }

    @Suppress("EmptyFunctionBlock")
    override fun onHelp() {
    }

    override fun onLoginFinished() {
        endProgress()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun onAccountChanged(event: OnAccountChanged) {
        if (event.isError) {
            if (mIsUpdatingDisplayName) {
                mIsUpdatingDisplayName = false
                AnalyticsTracker.track(
                    if (mIsEmailSignup
                    ) Stat.SIGNUP_EMAIL_EPILOGUE_UPDATE_DISPLAY_NAME_FAILED
                    else Stat.SIGNUP_SOCIAL_EPILOGUE_UPDATE_DISPLAY_NAME_FAILED
                )
            } else if (mIsUpdatingPassword) {
                mIsUpdatingPassword = false
            }

            AppLog.e(
                AppLog.T.API, ("SignupEpilogueFragment.onAccountChanged: "
                        + event.error.type + " - " + event.error.message)
            )
            endProgress()

            if (isPasswordInErrorMessage(event.error.message)) {
                showErrorDialogWithCloseButton(event.error.message)
            } else {
                showErrorDialog(getString(R.string.signup_epilogue_error_generic))
            }
            // Wait to populate epilogue for email interface until account is fetched and email address
            // is available since flow is coming from magic link with no instance argument values.
        } else if (mIsEmailSignup && (event.causeOfChange == AccountAction.FETCH_ACCOUNT
                    ) && !TextUtils.isEmpty(mAccountStore.account.email)
        ) {
            endProgress()
            populateViews()
        } else if (event.causeOfChange == AccountAction.PUSH_SETTINGS) {
            mHasMadeUpdates = true

            if (mIsUpdatingDisplayName) {
                mIsUpdatingDisplayName = false
                AnalyticsTracker.track(
                    if (mIsEmailSignup
                    ) Stat.SIGNUP_EMAIL_EPILOGUE_UPDATE_DISPLAY_NAME_SUCCEEDED
                    else Stat.SIGNUP_SOCIAL_EPILOGUE_UPDATE_DISPLAY_NAME_SUCCEEDED
                )
            } else if (mIsUpdatingPassword) {
                mIsUpdatingPassword = false
                mHasUpdatedPassword = true
            }

            updateAccountOrContinue()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUsernameChanged(event: OnUsernameChanged) {
        if (event.isError) {
            AnalyticsTracker.track(
                if (mIsEmailSignup
                ) Stat.SIGNUP_EMAIL_EPILOGUE_UPDATE_USERNAME_FAILED
                else Stat.SIGNUP_SOCIAL_EPILOGUE_UPDATE_USERNAME_FAILED
            )
            AppLog.e(
                AppLog.T.API, ("SignupEpilogueFragment.onUsernameChanged: "
                        + event.error.type + " - " + event.error.message)
            )
            endProgress()
            showErrorDialog(getString(R.string.signup_epilogue_error_generic))
        } else {
            mHasMadeUpdates = true
            AnalyticsTracker.track(
                if (mIsEmailSignup
                ) Stat.SIGNUP_EMAIL_EPILOGUE_UPDATE_USERNAME_SUCCEEDED
                else Stat.SIGNUP_SOCIAL_EPILOGUE_UPDATE_USERNAME_SUCCEEDED
            )
            updateAccountOrContinue()
        }
    }

    private fun changedDisplayName(): Boolean {
        return !TextUtils.equals(mAccount.account.displayName, mDisplayName)
    }

    private fun changedPassword(): Boolean {
        return !TextUtils.isEmpty(mInputPassword.editText.text.toString())
    }

    private fun changedUsername(): Boolean {
        return !TextUtils.equals(mAccount.account.userName, mUsername)
    }

    private fun isPasswordInErrorMessage(message: String): Boolean {
        val lowercaseMessage = message.lowercase(Locale.getDefault())
        val lowercasePassword = getString(R.string.password).lowercase(Locale.getDefault())
        return lowercaseMessage.contains(lowercasePassword)
    }

    private fun launchDialog() {
        AnalyticsTracker.track(
            if (mIsEmailSignup
            ) Stat.SIGNUP_EMAIL_EPILOGUE_USERNAME_TAPPED
            else Stat.SIGNUP_SOCIAL_EPILOGUE_USERNAME_TAPPED
        )

        val bundle: Bundle = BaseUsernameChangerFullScreenDialogFragment.newBundle(
            mEditTextDisplayName.text.toString(), mEditTextUsername.text.toString()
        )

        mDialog = FullScreenDialogFragment.Builder(requireContext())
            .setTitle(R.string.username_changer_title)
            .setAction(R.string.username_changer_action)
            .setToolbarTheme(org.wordpress.android.login.R.style.ThemeOverlay_LoginFlow_Toolbar)
            .setOnConfirmListener(this)
            .setOnDismissListener(this)
            .setOnShownListener(this)
            .setContent(UsernameChangerFullScreenDialogFragment::class.java, bundle)
            .build()

        mDialog?.show(requireActivity().supportFragmentManager, FullScreenDialogFragment.TAG)
    }

    private fun loadAvatar(avatarUrl: String, injectFilePath: String) {
        val newAvatarUploaded = injectFilePath.isNotEmpty()
        if (newAvatarUploaded) {
            // Remove specific URL entry from bitmap cache. Update it via injected request cache.
            getBitmapCache().removeSimilar(avatarUrl)
            // Changing the signature invalidates Glide's cache
            mAppPrefsWrapper.avatarVersion += 1
        }

        val bitmap = getBitmapCache()[avatarUrl]
        // Avatar's API doesn't synchronously update the image at avatarUrl. There is a replication lag
        // (cca 5s), before the old avatar is replaced with the new avatar. Therefore we need to use this workaround,
        // which temporary saves the new image into a local bitmap cache.
        if (bitmap != null) {
            mImageManager.load((mHeaderAvatar), bitmap)
        } else {
            mImageManager.loadIntoCircle(
                mHeaderAvatar,
                ImageType.AVATAR_WITHOUT_BACKGROUND,
                if (newAvatarUploaded) {
                    injectFilePath
                } else {
                    avatarUrl
                },
                object : ImageManager.RequestListener<Drawable> {
                    override fun onLoadFailed(e: Exception?, model: Any?) {
                        AppLog.e(
                            AppLog.T.NUX,
                            "Uploading image to Gravatar succeeded, but setting image view failed"
                        )
                        showErrorDialogWithCloseButton(getString(R.string.signup_epilogue_error_avatar_view))
                    }

                    @Suppress("NAME_SHADOWING")
                    override fun onResourceReady(resource: Drawable, model: Any?) {
                        if (newAvatarUploaded && resource is BitmapDrawable) {
                            var bitmap = resource.bitmap
                            // create a copy since the original bitmap may by automatically recycled
                            bitmap = bitmap.copy(bitmap.config, true)
                            getBitmapCache().put((avatarUrl), bitmap)
                        }
                    }
                },
                mAppPrefsWrapper.avatarVersion
            )
        }
    }

    private fun populateViews() {
        mEmailAddress = mAccountStore.account.email
        mDisplayName = mSignupUtils.createDisplayNameFromEmail(mEmailAddress)
        mUsername = if (!TextUtils.isEmpty(mAccountStore.account.userName)
        ) mAccountStore.account.userName else mSignupUtils.createUsernameFromEmail(mEmailAddress)
        mHeaderDisplayName.text = mDisplayName
        mHeaderEmailAddress.text = mEmailAddress
        mEditTextDisplayName.setText(mDisplayName)
        mEditTextUsername.setText(mUsername)
        // Set fragment arguments to know if account should be updated when values change.
        val args = Bundle()
        args.putString(ARG_DISPLAY_NAME, mDisplayName)
        args.putString(ARG_EMAIL_ADDRESS, mEmailAddress)
        args.putString(ARG_PHOTO_URL, mPhotoUrl)
        args.putString(ARG_USERNAME, mUsername)
        args.putBoolean(ARG_IS_EMAIL_SIGNUP, mIsEmailSignup)
        arguments = args
    }

    private fun showErrorDialog(message: String?) {
        val dialogListener: DialogInterface.OnClickListener =
            DialogInterface.OnClickListener { _, which ->
                when (which) {
                    DialogInterface.BUTTON_NEGATIVE -> undoChanges()
                    DialogInterface.BUTTON_POSITIVE -> updateAccountOrContinue()
                }
            }

        val dialog = MaterialAlertDialogBuilder(requireActivity())
            .setMessage(message)
            .setNeutralButton(R.string.login_error_button, dialogListener)
            .setNegativeButton(R.string.signup_epilogue_error_button_negative, dialogListener)
            .setPositiveButton(R.string.signup_epilogue_error_button_positive, dialogListener)
            .create()
        dialog.show()
    }

    private fun showErrorDialogWithCloseButton(message: String?) {
        val dialog = MaterialAlertDialogBuilder(requireActivity())
            .setMessage(message)
            .setPositiveButton(R.string.login_error_button, null)
            .create()
        dialog.show()
    }

    private fun startCropActivity(uri: Uri?) {
        val baseContext: Context? = activity

        if (baseContext != null) {
            val context: Context = ContextThemeWrapper(baseContext, R.style.WordPress_NoActionBar)

            val options = UCrop.Options()
            options.setShowCropGrid(false)
            options.setStatusBarColor(
                context.getColorFromAttribute(
                    android.R.attr.statusBarColor
                )
            )
            options.setToolbarColor(context.getColorFromAttribute(R.attr.wpColorAppBar))
            options.setToolbarWidgetColor(
                context.getColorFromAttribute(
                    com.google.android.material.R.attr.colorOnSurface
                )
            )
            options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.NONE, UCropActivity.NONE)
            options.setHideBottomControls(true)

            UCrop.of((uri)!!, Uri.fromFile(File(context.cacheDir, "cropped.jpg")))
                .withAspectRatio(1f, 1f)
                .withOptions(options)
                .start(context, this)
        }
    }

    private fun startGravatarUpload(filePath: String) {
        if (!TextUtils.isEmpty(filePath)) {
            val file = File(filePath)
            if (file.exists()) {
                mAccountStore.accessToken?.let { accessToken ->
                    startProgress(false)
                    lifecycleScope.launch {
                        val result = mAvatarService.uploadCatching(
                            file,
                            accessToken
                        )
                        when (result) {
                            is GravatarResult.Success -> {
                                endProgress()
                                AnalyticsTracker.track(Stat.ME_GRAVATAR_UPLOADED)
                                mPhotoUrl = WPAvatarUtils.rewriteAvatarUrl(
                                    mAccount.account.avatarUrl,
                                    resources.getDimensionPixelSize(R.dimen.avatar_sz_large)
                                )
                                loadAvatar(mPhotoUrl, filePath)
                                mHeaderAvatarAdd.visibility = View.GONE
                                mIsAvatarAdded = true
                            }

                            is GravatarResult.Failure -> {
                                endProgress()
                                showErrorDialogWithCloseButton(getString(R.string.signup_epilogue_error_avatar))
                                val properties: MutableMap<String, Any?> = HashMap()
                                properties["error_type"] = result.error
                                AnalyticsTracker.track(Stat.ME_GRAVATAR_UPLOAD_EXCEPTION, properties)
                                AppLog.e(AppLog.T.NUX, "Uploading image to Gravatar failed")
                            }
                        }
                    }
                }
            } else {
                ToastUtils.showToast(
                    activity,
                    R.string.error_locating_image,
                    ToastUtils.Duration.SHORT
                )
            }
        } else {
            ToastUtils.showToast(activity, R.string.error_locating_image, ToastUtils.Duration.SHORT)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun undoChanges() {
        mDisplayName = if (!TextUtils.isEmpty(mAccountStore.account.displayName)
        ) mAccountStore.account.displayName else requireArguments().getString(ARG_DISPLAY_NAME)
        mEditTextDisplayName.setText(mDisplayName)
        mUsername = if (!TextUtils.isEmpty(mAccountStore.account.userName)
        ) mAccountStore.account.userName else requireArguments().getString(ARG_USERNAME)
        mEditTextUsername.setText(mUsername)
        mInputPassword.editText.setText("")
        updateAccountOrContinue()
    }

    private fun updateAccountOrContinue() {
        if (changedUsername()) {
            startProgressIfNeeded()
            updateUsername()
        } else if (changedDisplayName()) {
            startProgressIfNeeded()
            mIsUpdatingDisplayName = true
            updateDisplayName()
        } else if (changedPassword() && !mHasUpdatedPassword) {
            startProgressIfNeeded()
            mIsUpdatingPassword = true
            updatePassword()
        } else if (mSignupEpilogueListener != null) {
            if (!mHasMadeUpdates) {
                AnalyticsTracker.track(
                    if (mIsEmailSignup
                    ) Stat.SIGNUP_EMAIL_EPILOGUE_UNCHANGED
                    else Stat.SIGNUP_SOCIAL_EPILOGUE_UNCHANGED
                )
            }
            endProgressIfNeeded()
            mSignupEpilogueListener!!.onContinue()
        }
    }

    private fun updateDisplayName() {
        val payload = PushAccountSettingsPayload()
        payload.params = HashMap()
        payload.params["display_name"] = mDisplayName
        dispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload))
    }

    private fun updatePassword() {
        val payload = PushAccountSettingsPayload()
        payload.params = HashMap()
        payload.params["password"] = mInputPassword.editText.text.toString()
        dispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload))
    }

    private fun updateUsername() {
        mUsername?.let {
            val payload = PushUsernamePayload(it, AccountUsernameActionType.KEEP_OLD_SITE_AND_ADDRESS)
            dispatcher.dispatch(AccountActionBuilder.newPushUsernameAction(payload))
        }
    }

    private inner class DownloadAvatarAndUploadGravatarThread(
        private val mUrl: String,
        private val mEmail: String,
        private val mToken: String
    ) : Thread() {
        override fun run() {
            @Suppress("TooGenericExceptionCaught")
            try {
                val uri = MediaUtils.downloadExternalMedia(context, Uri.parse(mUrl))
                val file = File(URI(uri.toString()))
                lifecycleScope.launch {
                    when (val result = mAvatarService.uploadCatching(file, mToken)) {
                        is GravatarResult.Success -> {
                            AppLog.i(
                                AppLog.T.NUX,
                                "Google avatar download and Gravatar upload succeeded."
                            )
                            AnalyticsTracker.track(Stat.ME_GRAVATAR_UPLOADED)
                        }

                        is GravatarResult.Failure -> {
                            AppLog.i(
                                AppLog.T.NUX,
                                "Google avatar download and Gravatar upload failed."
                            )
                            val properties: MutableMap<String, Any?> = HashMap()
                            properties["error_type"] = result.error
                            AnalyticsTracker.track(Stat.ME_GRAVATAR_UPLOAD_EXCEPTION, properties)
                        }
                    }
                }
            } catch (exception: NullPointerException) {
                AppLog.e(
                    AppLog.T.NUX, ("Google avatar download and Gravatar upload failed - "
                            + exception.toString() + " - " + exception.message)
                )
            } catch (exception: URISyntaxException) {
                AppLog.e(
                    AppLog.T.NUX, ("Google avatar download and Gravatar upload failed - "
                            + exception.toString() + " - " + exception.message)
                )
            }
        }
    }

    companion object {
        private const val ARG_DISPLAY_NAME = "ARG_DISPLAY_NAME"
        private const val ARG_EMAIL_ADDRESS = "ARG_EMAIL_ADDRESS"
        private const val ARG_IS_EMAIL_SIGNUP = "ARG_IS_EMAIL_SIGNUP"
        private const val ARG_PHOTO_URL = "ARG_PHOTO_URL"
        private const val ARG_USERNAME = "ARG_USERNAME"
        private const val KEY_DISPLAY_NAME = "KEY_DISPLAY_NAME"
        private const val KEY_EMAIL_ADDRESS = "KEY_EMAIL_ADDRESS"
        private const val KEY_IS_AVATAR_ADDED = "KEY_IS_AVATAR_ADDED"
        private const val KEY_PHOTO_URL = "KEY_PHOTO_URL"
        private const val KEY_USERNAME = "KEY_USERNAME"
        private const val KEY_IS_UPDATING_DISPLAY_NAME = "KEY_IS_UPDATING_DISPLAY_NAME"
        private const val KEY_IS_UPDATING_PASSWORD = "KEY_IS_UPDATING_PASSWORD"
        private const val KEY_HAS_UPDATED_PASSWORD = "KEY_HAS_UPDATED_PASSWORD"
        private const val KEY_HAS_MADE_UPDATES = "KEY_HAS_MADE_UPDATES"

        private const val SOURCE = "source"
        private const val SOURCE_SIGNUP_EPILOGUE = "signup_epilogue"

        const val TAG: String = "signup_epilogue_fragment_tag"

        fun newInstance(
            displayName: String?, emailAddress: String?,
            photoUrl: String?, username: String?,
            isEmailSignup: Boolean
        ): SignupEpilogueFragment {
            val signupEpilogueFragment = SignupEpilogueFragment()
            val args = Bundle()
            args.putString(ARG_DISPLAY_NAME, displayName)
            args.putString(ARG_EMAIL_ADDRESS, emailAddress)
            args.putString(ARG_PHOTO_URL, photoUrl)
            args.putString(ARG_USERNAME, username)
            args.putBoolean(ARG_IS_EMAIL_SIGNUP, isEmailSignup)
            signupEpilogueFragment.arguments = args
            return signupEpilogueFragment
        }
    }
}
