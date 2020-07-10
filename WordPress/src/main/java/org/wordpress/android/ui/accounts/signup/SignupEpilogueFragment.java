package org.wordpress.android.ui.accounts.signup;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;
import androidx.core.widget.NestedScrollView.OnScrollChangeListener;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AccountUsernameActionType;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnUsernameChanged;
import org.wordpress.android.fluxc.store.AccountStore.PushAccountSettingsPayload;
import org.wordpress.android.fluxc.store.AccountStore.PushUsernamePayload;
import org.wordpress.android.login.LoginBaseFormFragment;
import org.wordpress.android.login.widgets.WPLoginInputRow;
import org.wordpress.android.networking.GravatarApi;
import org.wordpress.android.ui.FullScreenDialogFragment;
import org.wordpress.android.ui.FullScreenDialogFragment.OnConfirmListener;
import org.wordpress.android.ui.FullScreenDialogFragment.OnDismissListener;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Click;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Step;
import org.wordpress.android.ui.media.MediaBrowserType;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource;
import org.wordpress.android.ui.photopicker.PhotoPickerFragment;
import org.wordpress.android.ui.prefs.AppPrefsWrapper;
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic;
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ContextExtensionsKt;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ViewUtilsKt;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageManager.RequestListener;
import org.wordpress.android.util.image.ImageType;
import org.wordpress.android.widgets.WPTextView;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;

import javax.inject.Inject;

public class SignupEpilogueFragment extends LoginBaseFormFragment<SignupEpilogueListener>
        implements OnConfirmListener, OnDismissListener {
    private EditText mEditTextDisplayName;
    private EditText mEditTextUsername;
    private FullScreenDialogFragment mDialog;
    private SignupEpilogueListener mSignupEpilogueListener;

    protected ImageView mHeaderAvatarAdd;
    protected String mDisplayName;
    protected String mEmailAddress;
    protected String mPhotoUrl;
    protected String mUsername;
    protected WPLoginInputRow mInputPassword;
    protected ImageView mHeaderAvatar;
    protected WPTextView mHeaderDisplayName;
    protected WPTextView mHeaderEmailAddress;
    protected View mBottomShadow;
    protected NestedScrollView mScrollView;
    protected boolean mIsAvatarAdded;
    protected boolean mIsEmailSignup;

    private boolean mIsUpdatingDisplayName = false;
    private boolean mIsUpdatingPassword = false;
    private boolean mHasUpdatedPassword = false;
    private boolean mHasMadeUpdates = false;

    private static final String ARG_DISPLAY_NAME = "ARG_DISPLAY_NAME";
    private static final String ARG_EMAIL_ADDRESS = "ARG_EMAIL_ADDRESS";
    private static final String ARG_IS_EMAIL_SIGNUP = "ARG_IS_EMAIL_SIGNUP";
    private static final String ARG_PHOTO_URL = "ARG_PHOTO_URL";
    private static final String ARG_USERNAME = "ARG_USERNAME";
    private static final String KEY_DISPLAY_NAME = "KEY_DISPLAY_NAME";
    private static final String KEY_EMAIL_ADDRESS = "KEY_EMAIL_ADDRESS";
    private static final String KEY_IS_AVATAR_ADDED = "KEY_IS_AVATAR_ADDED";
    private static final String KEY_PHOTO_URL = "KEY_PHOTO_URL";
    private static final String KEY_USERNAME = "KEY_USERNAME";
    private static final String KEY_IS_UPDATING_DISPLAY_NAME = "KEY_IS_UPDATING_DISPLAY_NAME";
    private static final String KEY_IS_UPDATING_PASSWORD = "KEY_IS_UPDATING_PASSWORD";
    private static final String KEY_HAS_UPDATED_PASSWORD = "KEY_HAS_UPDATED_PASSWORD";
    private static final String KEY_HAS_MADE_UPDATES = "KEY_HAS_MADE_UPDATES";

    public static final String TAG = "signup_epilogue_fragment_tag";

    @Inject protected AccountStore mAccount;
    @Inject protected Dispatcher mDispatcher;
    @Inject protected ImageManager mImageManager;
    @Inject protected AppPrefsWrapper mAppPrefsWrapper;
    @Inject protected UnifiedLoginTracker mUnifiedLoginTracker;

    public static SignupEpilogueFragment newInstance(String displayName, String emailAddress,
                                                     String photoUrl, String username,
                                                     boolean isEmailSignup) {
        SignupEpilogueFragment signupEpilogueFragment = new SignupEpilogueFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DISPLAY_NAME, displayName);
        args.putString(ARG_EMAIL_ADDRESS, emailAddress);
        args.putString(ARG_PHOTO_URL, photoUrl);
        args.putString(ARG_USERNAME, username);
        args.putBoolean(ARG_IS_EMAIL_SIGNUP, isEmailSignup);
        signupEpilogueFragment.setArguments(args);
        return signupEpilogueFragment;
    }

    @Override
    protected @LayoutRes int getContentLayout() {
        return 0; // no content layout; entire view is inflated in createMainView
    }

    @Override
    protected @LayoutRes int getProgressBarText() {
        return R.string.signup_updating_account;
    }

    @Override
    protected void setupLabel(@NonNull TextView label) {
        // no label in this screen
    }

    @Override
    protected ViewGroup createMainView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return (ViewGroup) inflater.inflate(R.layout.signup_epilogue, container, false);
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        final FrameLayout headerAvatarLayout = rootView.findViewById(R.id.login_epilogue_header_avatar_layout);
        headerAvatarLayout.setEnabled(mIsEmailSignup);
        headerAvatarLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mUnifiedLoginTracker.trackClick(Click.SELECT_AVATAR);
                Intent intent = new Intent(getActivity(), PhotoPickerActivity.class);
                intent.putExtra(PhotoPickerFragment.ARG_BROWSER_TYPE, MediaBrowserType.GRAVATAR_IMAGE_PICKER);
                startActivityForResult(intent, RequestCodes.PHOTO_PICKER);
            }
        });
        headerAvatarLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ToastUtils.showToast(getActivity(), getString(R.string.content_description_add_avatar),
                                     ToastUtils.Duration.SHORT);
                return true;
            }
        });
        ViewUtilsKt.redirectContextClickToLongPressListener(headerAvatarLayout);
        mHeaderAvatarAdd = rootView.findViewById(R.id.login_epilogue_header_avatar_add);
        mHeaderAvatarAdd.setVisibility(mIsEmailSignup ? View.VISIBLE : View.GONE);
        mHeaderAvatar = rootView.findViewById(R.id.login_epilogue_header_avatar);
        mHeaderDisplayName = rootView.findViewById(R.id.login_epilogue_header_title);
        mHeaderDisplayName.setText(mDisplayName);
        mHeaderEmailAddress = rootView.findViewById(R.id.login_epilogue_header_subtitle);
        mHeaderEmailAddress.setText(mEmailAddress);
        WPLoginInputRow inputDisplayName = rootView.findViewById(R.id.signup_epilogue_input_display);
        mEditTextDisplayName = inputDisplayName.getEditText();
        mEditTextDisplayName.setText(mDisplayName);
        mEditTextDisplayName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mDisplayName = s.toString();
                mHeaderDisplayName.setText(mDisplayName);
            }
        });
        WPLoginInputRow inputUsername = rootView.findViewById(R.id.signup_epilogue_input_username);
        mEditTextUsername = inputUsername.getEditText();
        mEditTextUsername.setText(mUsername);
        mEditTextUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mUnifiedLoginTracker.trackClick(Click.EDIT_USERNAME);
                launchDialog();
            }
        });
        mEditTextUsername.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    launchDialog();
                }
            }
        });
        mEditTextUsername.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                // Consume keyboard events except for Enter (i.e. click/tap) and Tab (i.e. focus/navigation).
                // The onKey method returns true if the listener has consumed the event and false otherwise
                // allowing hardware keyboard users to tap and navigate, but not input text as expected.
                // This allows the username changer to launch using the keyboard.
                return !(keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_TAB);
            }
        });
        mInputPassword = rootView.findViewById(R.id.signup_epilogue_input_password);
        mInputPassword.setVisibility(mIsEmailSignup ? View.VISIBLE : View.GONE);
        final WPTextView passwordDetail = rootView.findViewById(R.id.signup_epilogue_input_password_detail);
        passwordDetail.setVisibility(mIsEmailSignup ? View.VISIBLE : View.GONE);

        // Set focus on static text field to avoid showing keyboard on start.
        mHeaderEmailAddress.requestFocus();

        mBottomShadow = rootView.findViewById(R.id.bottom_shadow);
        mScrollView = rootView.findViewById(R.id.scroll_view);
        mScrollView.setOnScrollChangeListener(
                (OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> showBottomShadowIfNeeded());
        // We must use onGlobalLayout here otherwise canScrollVertically will always return false
        mScrollView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                mScrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                showBottomShadowIfNeeded();
            }
        });
    }

    private void showBottomShadowIfNeeded() {
        if (mScrollView != null) {
            final boolean canScrollDown = mScrollView.canScrollVertically(1);
            if (mBottomShadow != null) {
                mBottomShadow.setVisibility(canScrollDown ? View.VISIBLE : View.GONE);
            }
        }
    }

    @Override
    protected void setupBottomButtons(Button secondaryButton, Button primaryButton) {
        primaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mUnifiedLoginTracker.trackClick(Click.CONTINUE);
                updateAccountOrContinue();
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());

        mDisplayName = getArguments().getString(ARG_DISPLAY_NAME);
        mEmailAddress = getArguments().getString(ARG_EMAIL_ADDRESS);
        mPhotoUrl = StringUtils.notNullStr(getArguments().getString(ARG_PHOTO_URL));
        mUsername = getArguments().getString(ARG_USERNAME);
        mIsEmailSignup = getArguments().getBoolean(ARG_IS_EMAIL_SIGNUP);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            // Start loading reader tags so they will be available asap
            ReaderUpdateServiceStarter.startService(WordPress.getContext(),
                    EnumSet.of(ReaderUpdateLogic.UpdateTask.TAGS));

            mUnifiedLoginTracker.track(Step.SUCCESS);
            if (mIsEmailSignup) {
                AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_EMAIL_EPILOGUE_VIEWED);

                // Start progress and wait for account to be fetched before populating views when
                // email does not exist in account store.
                if (TextUtils.isEmpty(mAccountStore.getAccount().getEmail())) {
                    startProgress(false);
                } else {
                    // Skip progress and populate views when email does exist in account store.
                    populateViews();
                }
            } else {
                AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_SOCIAL_EPILOGUE_VIEWED);
                new DownloadAvatarAndUploadGravatarThread(mPhotoUrl, mEmailAddress, mAccount.getAccessToken()).start();
                mImageManager.loadIntoCircle(mHeaderAvatar, ImageType.AVATAR_WITHOUT_BACKGROUND, mPhotoUrl);
            }
        } else {
            mDialog = (FullScreenDialogFragment) getFragmentManager().findFragmentByTag(FullScreenDialogFragment.TAG);

            if (mDialog != null) {
                mDialog.setOnConfirmListener(this);
                mDialog.setOnDismissListener(this);
            }

            mDisplayName = savedInstanceState.getString(KEY_DISPLAY_NAME);
            mUsername = savedInstanceState.getString(KEY_USERNAME);
            mIsAvatarAdded = savedInstanceState.getBoolean(KEY_IS_AVATAR_ADDED);

            if (mIsEmailSignup) {
                mPhotoUrl = StringUtils.notNullStr(savedInstanceState.getString(KEY_PHOTO_URL));
                mEmailAddress = savedInstanceState.getString(KEY_EMAIL_ADDRESS);
                mHeaderEmailAddress.setText(mEmailAddress);
                mHeaderAvatarAdd.setVisibility(mIsAvatarAdded ? View.GONE : View.VISIBLE);
            }
            mImageManager.loadIntoCircle(mHeaderAvatar, ImageType.AVATAR_WITHOUT_BACKGROUND, mPhotoUrl);

            mIsUpdatingDisplayName = savedInstanceState.getBoolean(KEY_IS_UPDATING_DISPLAY_NAME);
            mIsUpdatingPassword = savedInstanceState.getBoolean(KEY_IS_UPDATING_PASSWORD);
            mHasUpdatedPassword = savedInstanceState.getBoolean(KEY_HAS_UPDATED_PASSWORD);
            mHasMadeUpdates = savedInstanceState.getBoolean(KEY_HAS_MADE_UPDATES);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (isAdded()) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    switch (requestCode) {
                        case RequestCodes.PHOTO_PICKER:
                            if (data != null) {
                                String[] mediaUriStringsArray =
                                        data.getStringArrayExtra(PhotoPickerActivity.EXTRA_MEDIA_URIS);

                                if (mediaUriStringsArray != null && mediaUriStringsArray.length > 0) {
                                    PhotoPickerMediaSource source = PhotoPickerMediaSource.fromString(
                                            data.getStringExtra(PhotoPickerActivity.EXTRA_MEDIA_SOURCE));
                                    AnalyticsTracker.Stat stat =
                                            source == PhotoPickerActivity.PhotoPickerMediaSource.ANDROID_CAMERA
                                                ? AnalyticsTracker.Stat.SIGNUP_EMAIL_EPILOGUE_GRAVATAR_SHOT_NEW
                                                : AnalyticsTracker.Stat.SIGNUP_EMAIL_EPILOGUE_GRAVATAR_GALLERY_PICKED;
                                    AnalyticsTracker.track(stat);
                                    Uri imageUri = Uri.parse(mediaUriStringsArray[0]);

                                    if (imageUri != null) {
                                        boolean wasSuccess = WPMediaUtils.fetchMediaAndDoNext(
                                                getActivity(), imageUri,
                                                new WPMediaUtils.MediaFetchDoNext() {
                                                    @Override
                                                    public void doNext(Uri uri) {
                                                        startCropActivity(uri);
                                                    }
                                                });

                                        if (!wasSuccess) {
                                            AppLog.e(T.UTILS, "Can't download picked or captured image");
                                        }
                                    } else {
                                        AppLog.e(T.UTILS, "Can't parse media string");
                                    }
                                } else {
                                    AppLog.e(T.UTILS, "Can't resolve picked or captured image");
                                }
                            }

                            break;
                        case UCrop.REQUEST_CROP:
                            AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_EMAIL_EPILOGUE_GRAVATAR_CROPPED);
                            WPMediaUtils.fetchMediaAndDoNext(getActivity(), UCrop.getOutput(data),
                                                             new WPMediaUtils.MediaFetchDoNext() {
                                                                 @Override
                                                                 public void doNext(Uri uri) {
                                                                     startGravatarUpload(MediaUtils.getRealPathFromURI(
                                                                             getActivity(), uri));
                                                                 }
                                                             });

                            break;
                    }

                    break;
                case UCrop.RESULT_ERROR:
                    AppLog.e(T.NUX, "Image cropping failed", UCrop.getError(data));
                    ToastUtils.showToast(getActivity(), R.string.error_cropping_image, ToastUtils.Duration.SHORT);
                    break;
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof SignupEpilogueListener) {
            mSignupEpilogueListener = (SignupEpilogueListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement SignupEpilogueListener");
        }
    }

    @Override
    public void onConfirm(@Nullable Bundle result) {
        if (result != null) {
            mUsername = result.getString(UsernameChangerFullScreenDialogFragment.RESULT_USERNAME);
            mEditTextUsername.setText(mUsername);
        }
    }

    @Override
    public void onDismiss() {
        mDialog = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_PHOTO_URL, mPhotoUrl);
        outState.putString(KEY_DISPLAY_NAME, mDisplayName);
        outState.putString(KEY_EMAIL_ADDRESS, mEmailAddress);
        outState.putString(KEY_USERNAME, mUsername);
        outState.putBoolean(KEY_IS_AVATAR_ADDED, mIsAvatarAdded);
        outState.putBoolean(KEY_IS_UPDATING_DISPLAY_NAME, mIsUpdatingDisplayName);
        outState.putBoolean(KEY_IS_UPDATING_PASSWORD, mIsUpdatingPassword);
        outState.putBoolean(KEY_HAS_UPDATED_PASSWORD, mHasUpdatedPassword);
        outState.putBoolean(KEY_HAS_MADE_UPDATES, mHasMadeUpdates);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    protected void onHelp() {
    }

    @Override
    protected void onLoginFinished() {
        endProgress();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(OnAccountChanged event) {
        if (event.isError()) {
            if (mIsUpdatingDisplayName) {
                mIsUpdatingDisplayName = false;
                AnalyticsTracker.track(mIsEmailSignup
                        ? AnalyticsTracker.Stat.SIGNUP_EMAIL_EPILOGUE_UPDATE_DISPLAY_NAME_FAILED
                        : AnalyticsTracker.Stat.SIGNUP_SOCIAL_EPILOGUE_UPDATE_DISPLAY_NAME_FAILED);
            } else if (mIsUpdatingPassword) {
                mIsUpdatingPassword = false;
            }

            AppLog.e(T.API, "SignupEpilogueFragment.onAccountChanged: "
                            + event.error.type + " - " + event.error.message);
            endProgress();

            if (isPasswordInErrorMessage(event.error.message)) {
                showErrorDialogWithCloseButton(event.error.message);
            } else {
                showErrorDialog(getString(R.string.signup_epilogue_error_generic));
            }
        // Wait to populate epilogue for email interface until account is fetched and email address
        // is available since flow is coming from magic link with no instance argument values.
        } else if (mIsEmailSignup && event.causeOfChange == AccountAction.FETCH_ACCOUNT
                   && !TextUtils.isEmpty(mAccountStore.getAccount().getEmail())) {
            endProgress();
            populateViews();
        } else if (event.causeOfChange == AccountAction.PUSH_SETTINGS) {
            mHasMadeUpdates = true;

            if (mIsUpdatingDisplayName) {
                mIsUpdatingDisplayName = false;
                AnalyticsTracker.track(mIsEmailSignup
                        ? AnalyticsTracker.Stat.SIGNUP_EMAIL_EPILOGUE_UPDATE_DISPLAY_NAME_SUCCEEDED
                        : AnalyticsTracker.Stat.SIGNUP_SOCIAL_EPILOGUE_UPDATE_DISPLAY_NAME_SUCCEEDED);
            } else if (mIsUpdatingPassword) {
                mIsUpdatingPassword = false;
                mHasUpdatedPassword = true;
            }

            updateAccountOrContinue();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUsernameChanged(OnUsernameChanged event) {
        if (event.isError()) {
            AnalyticsTracker.track(mIsEmailSignup
                    ? AnalyticsTracker.Stat.SIGNUP_EMAIL_EPILOGUE_UPDATE_USERNAME_FAILED
                    : AnalyticsTracker.Stat.SIGNUP_SOCIAL_EPILOGUE_UPDATE_USERNAME_FAILED);
            AppLog.e(T.API, "SignupEpilogueFragment.onUsernameChanged: "
                            + event.error.type + " - " + event.error.message);
            endProgress();
            showErrorDialog(getString(R.string.signup_epilogue_error_generic));
        } else {
            mHasMadeUpdates = true;
            AnalyticsTracker.track(mIsEmailSignup
                    ? AnalyticsTracker.Stat.SIGNUP_EMAIL_EPILOGUE_UPDATE_USERNAME_SUCCEEDED
                    : AnalyticsTracker.Stat.SIGNUP_SOCIAL_EPILOGUE_UPDATE_USERNAME_SUCCEEDED);
            updateAccountOrContinue();
        }
    }

    protected boolean changedDisplayName() {
        return !TextUtils.equals(mAccount.getAccount().getDisplayName(), mDisplayName);
    }

    protected boolean changedPassword() {
        return !TextUtils.isEmpty(mInputPassword.getEditText().getText().toString());
    }

    protected boolean changedUsername() {
        return !TextUtils.equals(mAccount.getAccount().getUserName(), mUsername);
    }

    /**
     * Create a display name from the email address by taking everything before the "@" symbol,
     * removing all non-letters and non-periods, replacing periods with spaces, and capitalizing
     * the first letter of each word.
     *
     * @return {@link String} to be the display name
     */
    private String createDisplayNameFromEmail() {
        String username = mEmailAddress.split("@")[0].replaceAll("[^A-Za-z/.]", "");
        String[] array = username.split("\\.");
        StringBuilder builder = new StringBuilder();

        for (String s : array) {
            String capitalized = s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
            builder.append(capitalized.concat(" "));
        }

        return builder.toString().trim();
    }

    /**
     * Create a username from the email address by taking everything before the "@" symbol and
     * removing all non-alphanumeric characters.
     *
     * @return {@link String} to be the username
     */
    private String createUsernameFromEmail() {
        return mEmailAddress.split("@")[0].replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private boolean isPasswordInErrorMessage(String message) {
        String lowercaseMessage = message.toLowerCase(Locale.getDefault());
        String lowercasePassword = getString(R.string.password).toLowerCase(Locale.getDefault());
        return lowercaseMessage.contains(lowercasePassword);
    }

    protected void launchDialog() {
        AnalyticsTracker.track(mIsEmailSignup
                ? AnalyticsTracker.Stat.SIGNUP_EMAIL_EPILOGUE_USERNAME_TAPPED
                : AnalyticsTracker.Stat.SIGNUP_SOCIAL_EPILOGUE_USERNAME_TAPPED);

        final Bundle bundle = UsernameChangerFullScreenDialogFragment.newBundle(
                mEditTextDisplayName.getText().toString(), mEditTextUsername.getText().toString());

        mDialog = new FullScreenDialogFragment.Builder(getContext())
                .setTitle(R.string.username_changer_title)
                .setAction(R.string.username_changer_action)
                .setOnConfirmListener(this)
                .setOnDismissListener(this)
                .setContent(UsernameChangerFullScreenDialogFragment.class, bundle)
                .build();

        mDialog.show(getActivity().getSupportFragmentManager(), FullScreenDialogFragment.TAG);
    }

    protected void loadAvatar(final String avatarUrl, String injectFilePath) {
        final boolean newAvatarUploaded = injectFilePath != null && !injectFilePath.isEmpty();
        if (newAvatarUploaded) {
            // Remove specific URL entry from bitmap cache. Update it via injected request cache.
            WordPress.getBitmapCache().removeSimilar(avatarUrl);
            // Changing the signature invalidates Glide's cache
            mAppPrefsWrapper.setAvatarVersion(mAppPrefsWrapper.getAvatarVersion() + 1);
        }

        Bitmap bitmap = WordPress.getBitmapCache().get(avatarUrl);
        // Avatar's API doesn't synchronously update the image at avatarUrl. There is a replication lag
        // (cca 5s), before the old avatar is replaced with the new avatar. Therefore we need to use this workaround,
        // which temporary saves the new image into a local bitmap cache.
        if (bitmap != null) {
            mImageManager.load(mHeaderAvatar, bitmap);
        } else {
            mImageManager.loadIntoCircle(mHeaderAvatar, ImageType.AVATAR_WITHOUT_BACKGROUND,
                    newAvatarUploaded ? injectFilePath : avatarUrl, new RequestListener<Drawable>() {
                        @Override
                        public void onLoadFailed(@Nullable Exception e, @Nullable Object model) {
                            AppLog.e(T.NUX, "Uploading image to Gravatar succeeded, but setting image view failed");
                            showErrorDialogWithCloseButton(getString(R.string.signup_epilogue_error_avatar_view));
                        }

                        @Override
                        public void onResourceReady(@NotNull Drawable resource, @Nullable Object model) {
                            if (newAvatarUploaded && resource instanceof BitmapDrawable) {
                                Bitmap bitmap = ((BitmapDrawable) resource).getBitmap();
                                // create a copy since the original bitmap may by automatically recycled
                                bitmap = bitmap.copy(bitmap.getConfig(), true);
                                WordPress.getBitmapCache().put(avatarUrl, bitmap);
                            }
                        }
                    }, mAppPrefsWrapper.getAvatarVersion());
        }
    }

    private void populateViews() {
        mEmailAddress = mAccountStore.getAccount().getEmail();
        mDisplayName = createDisplayNameFromEmail();
        mUsername = !TextUtils.isEmpty(mAccountStore.getAccount().getUserName())
                ? mAccountStore.getAccount().getUserName() : createUsernameFromEmail();
        mHeaderDisplayName.setText(mDisplayName);
        mHeaderEmailAddress.setText(mEmailAddress);
        mEditTextDisplayName.setText(mDisplayName);
        mEditTextUsername.setText(mUsername);
        // Set fragment arguments to know if account should be updated when values change.
        Bundle args = new Bundle();
        args.putString(ARG_DISPLAY_NAME, mDisplayName);
        args.putString(ARG_EMAIL_ADDRESS, mEmailAddress);
        args.putString(ARG_PHOTO_URL, mPhotoUrl);
        args.putString(ARG_USERNAME, mUsername);
        args.putBoolean(ARG_IS_EMAIL_SIGNUP, mIsEmailSignup);
        setArguments(args);
    }

    protected void showErrorDialog(String message) {
        DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_NEGATIVE:
                        undoChanges();
                        break;
                    case DialogInterface.BUTTON_POSITIVE:
                        updateAccountOrContinue();
                        break;
                    // DialogInterface.BUTTON_NEUTRAL is intentionally ignored. Just dismiss dialog.
                }
            }
        };

        AlertDialog dialog = new MaterialAlertDialogBuilder(getActivity())
                .setMessage(message)
                .setNeutralButton(R.string.login_error_button, dialogListener)
                .setNegativeButton(R.string.signup_epilogue_error_button_negative, dialogListener)
                .setPositiveButton(R.string.signup_epilogue_error_button_positive, dialogListener)
                .create();
        dialog.show();
    }

    protected void showErrorDialogWithCloseButton(String message) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(getActivity())
                .setMessage(message)
                .setPositiveButton(R.string.login_error_button, null)
                .create();
        dialog.show();
    }

    protected void startCropActivity(Uri uri) {
        final Context context = getActivity();

        if (context != null) {
            UCrop.Options options = new UCrop.Options();
            options.setShowCropGrid(false);
            options.setStatusBarColor(ContextExtensionsKt.getColorFromAttribute(
                    context, android.R.attr.statusBarColor
            ));
            options.setToolbarColor(ContextExtensionsKt.getColorFromAttribute(context, R.attr.wpColorAppBar));
            options.setToolbarWidgetColor(ContextExtensionsKt.getColorFromAttribute(
                    context, R.attr.colorOnPrimarySurface
            ));
            options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.NONE, UCropActivity.NONE);
            options.setHideBottomControls(true);

            UCrop.of(uri, Uri.fromFile(new File(context.getCacheDir(), "cropped.jpg")))
                 .withAspectRatio(1, 1)
                 .withOptions(options)
                 .start(getActivity(), this);
        }
    }

    protected void startGravatarUpload(final String filePath) {
        if (!TextUtils.isEmpty(filePath)) {
            final File file = new File(filePath);

            if (file.exists()) {
                startProgress(false);

                GravatarApi.uploadGravatar(file, mAccountStore.getAccount().getEmail(), mAccountStore.getAccessToken(),
                        new GravatarApi.GravatarUploadListener() {
                            @Override
                            public void onSuccess() {
                                endProgress();
                                mPhotoUrl = GravatarUtils.fixGravatarUrl(mAccount.getAccount().getAvatarUrl(),
                                        getResources().getDimensionPixelSize(R.dimen.avatar_sz_large));
                                loadAvatar(mPhotoUrl, filePath);
                                mHeaderAvatarAdd.setVisibility(View.GONE);
                                mIsAvatarAdded = true;
                            }

                            @Override
                            public void onError() {
                                endProgress();
                                showErrorDialogWithCloseButton(getString(R.string.signup_epilogue_error_avatar));
                                AppLog.e(T.NUX, "Uploading image to Gravatar failed");
                            }
                        });
            } else {
                ToastUtils.showToast(getActivity(), R.string.error_locating_image, ToastUtils.Duration.SHORT);
            }
        } else {
            ToastUtils.showToast(getActivity(), R.string.error_locating_image, ToastUtils.Duration.SHORT);
        }
    }

    protected void undoChanges() {
        mDisplayName = !TextUtils.isEmpty(mAccountStore.getAccount().getDisplayName())
                ? mAccountStore.getAccount().getDisplayName() : getArguments().getString(ARG_DISPLAY_NAME);
        mEditTextDisplayName.setText(mDisplayName);
        mUsername = !TextUtils.isEmpty(mAccountStore.getAccount().getUserName())
                ? mAccountStore.getAccount().getUserName() : getArguments().getString(ARG_USERNAME);
        mEditTextUsername.setText(mUsername);
        mInputPassword.getEditText().setText("");
        updateAccountOrContinue();
    }

    protected void updateAccountOrContinue() {
        if (changedUsername()) {
            startProgressIfNeeded();
            updateUsername();
        } else if (changedDisplayName()) {
            startProgressIfNeeded();
            mIsUpdatingDisplayName = true;
            updateDisplayName();
        } else if (changedPassword() && !mHasUpdatedPassword) {
            startProgressIfNeeded();
            mIsUpdatingPassword = true;
            updatePassword();
        } else if (mSignupEpilogueListener != null) {
            if (!mHasMadeUpdates) {
                AnalyticsTracker.track(mIsEmailSignup
                        ? AnalyticsTracker.Stat.SIGNUP_EMAIL_EPILOGUE_UNCHANGED
                        : AnalyticsTracker.Stat.SIGNUP_SOCIAL_EPILOGUE_UNCHANGED);
            }
            endProgressIfNeeded();
            mSignupEpilogueListener.onContinue();
        }
    }

    private void updateDisplayName() {
        PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
        payload.params = new HashMap<>();
        payload.params.put("display_name", mDisplayName);
        mDispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload));
    }

    private void updatePassword() {
        PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
        payload.params = new HashMap<>();
        payload.params.put("password", mInputPassword.getEditText().getText().toString());
        mDispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload));
    }

    private void updateUsername() {
        PushUsernamePayload payload = new PushUsernamePayload(
                mUsername, AccountUsernameActionType.KEEP_OLD_SITE_AND_ADDRESS);
        mDispatcher.dispatch(AccountActionBuilder.newPushUsernameAction(payload));
    }

    private void startProgressIfNeeded() {
        if (!isInProgress()) {
            startProgress(false);
        }
    }

    private void endProgressIfNeeded() {
        if (isInProgress()) {
            endProgress();
        }
    }

    private class DownloadAvatarAndUploadGravatarThread extends Thread {
        private String mEmail;
        private String mToken;
        private String mUrl;

        DownloadAvatarAndUploadGravatarThread(String url, String email, String token) {
            mUrl = url;
            mEmail = email;
            mToken = token;
        }

        @Override
        public void run() {
            try {
                Uri uri = MediaUtils.downloadExternalMedia(getContext(), Uri.parse(mUrl));
                File file = new File(new URI(uri.toString()));
                GravatarApi.uploadGravatar(file, mEmail, mToken,
                        new GravatarApi.GravatarUploadListener() {
                            @Override
                            public void onSuccess() {
                                AppLog.i(T.NUX, "Google avatar download and Gravatar upload succeeded.");
                            }

                            @Override
                            public void onError() {
                                AppLog.i(T.NUX, "Google avatar download and Gravatar upload failed.");
                            }
                        });
            } catch (NullPointerException | URISyntaxException exception) {
                AppLog.e(T.NUX, "Google avatar download and Gravatar upload failed - "
                                + exception.toString() + " - " + exception.getMessage());
            }
        }
    }
}
