package org.wordpress.android.ui.accounts.signup;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.Cache;
import com.android.volley.Request;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
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
import org.wordpress.android.ui.media.MediaBrowserType;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource;
import org.wordpress.android.ui.photopicker.PhotoPickerFragment;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPNetworkImageView.ImageType;
import org.wordpress.android.widgets.WPTextView;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TreeMap;

import javax.inject.Inject;

public class SignupEpilogueFragment extends LoginBaseFormFragment<SignupEpilogueListener>
        implements OnConfirmListener, OnDismissListener {
    private EditText mEditTextDisplayName;
    private EditText mEditTextUsername;
    private FullScreenDialogFragment mDialog;
    private SignupEpilogueListener mSignupEpilogueListener;

    protected ImageButton mHeaderAvatarAdd;
    protected String mDisplayName;
    protected String mEmailAddress;
    protected String mPhotoUrl;
    protected String mUsername;
    protected WPLoginInputRow mInputPassword;
    protected WPNetworkImageView mHeaderAvatar;
    protected WPTextView mHeaderDisplayName;
    protected WPTextView mHeaderEmailAddress;
    protected boolean mIsAvatarAdded;
    protected boolean mIsEmailSignup;

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

    public static final String TAG = "signup_epilogue_fragment_tag";

    @Inject protected AccountStore mAccount;
    @Inject protected Dispatcher mDispatcher;

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
        return 0;  // no content layout; entire view is inflated in createMainView
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
        final RelativeLayout headerAvatarLayout = rootView.findViewById(R.id.signup_epilogue_header_avatar_layout);
        headerAvatarLayout.setEnabled(mIsEmailSignup);
        headerAvatarLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
        mHeaderAvatarAdd = rootView.findViewById(R.id.signup_epilogue_header_avatar_add);
        mHeaderAvatarAdd.setVisibility(mIsEmailSignup ? View.VISIBLE : View.GONE);
        mHeaderAvatar = rootView.findViewById(R.id.signup_epilogue_header_avatar);
        mHeaderDisplayName = rootView.findViewById(R.id.signup_epilogue_header_display);
        mHeaderDisplayName.setText(mDisplayName);
        mHeaderEmailAddress = rootView.findViewById(R.id.signup_epilogue_header_email);
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
    }

    @Override
    protected void setupBottomButtons(Button secondaryButton, Button primaryButton) {
        primaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateAccountOrContinue();
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        mDisplayName = getArguments().getString(ARG_DISPLAY_NAME);
        mEmailAddress = getArguments().getString(ARG_EMAIL_ADDRESS);
        mPhotoUrl = getArguments().getString(ARG_PHOTO_URL);
        mUsername = getArguments().getString(ARG_USERNAME);
        mIsEmailSignup = getArguments().getBoolean(ARG_IS_EMAIL_SIGNUP);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            if (mIsEmailSignup) {
                AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_EMAIL_EPILOGUE_VIEWED);
                startProgress();
            } else {
                AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_SOCIAL_EPILOGUE_VIEWED);
                new DownloadAvatarAndUploadGravatarThread(mPhotoUrl, mEmailAddress, mAccount.getAccessToken()).start();
                mHeaderAvatar.setImageUrl(mPhotoUrl, WPNetworkImageView.ImageType.AVATAR);
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
                mPhotoUrl = savedInstanceState.getString(KEY_PHOTO_URL);
                mEmailAddress = savedInstanceState.getString(KEY_EMAIL_ADDRESS);
                mHeaderEmailAddress.setText(mEmailAddress);
                mHeaderAvatarAdd.setVisibility(mIsAvatarAdded ? View.GONE : View.VISIBLE);
            }

            mHeaderAvatar.setImageUrl(mPhotoUrl, WPNetworkImageView.ImageType.AVATAR);
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
                                String mediaUriString = data.getStringExtra(PhotoPickerActivity.EXTRA_MEDIA_URI);

                                if (mediaUriString != null) {
                                PhotoPickerMediaSource source = PhotoPickerMediaSource.fromString(
                                        data.getStringExtra(PhotoPickerActivity.EXTRA_MEDIA_SOURCE));
                                AnalyticsTracker.Stat stat =
                                        source == PhotoPickerActivity.PhotoPickerMediaSource.ANDROID_CAMERA
                                                ? AnalyticsTracker.Stat.SIGNUP_EMAIL_EPILOGUE_GRAVATAR_SHOT_NEW
                                                : AnalyticsTracker.Stat.SIGNUP_EMAIL_EPILOGUE_GRAVATAR_GALLERY_PICKED;
                                AnalyticsTracker.track(stat);
                                    Uri imageUri = Uri.parse(mediaUriString);

                                    if (imageUri != null) {
                                        boolean wasSuccess = WPMediaUtils.fetchMediaAndDoNext(getActivity(), imageUri,
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
                                            startGravatarUpload(MediaUtils.getRealPathFromURI(getActivity(), uri));
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
            AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_SOCIAL_EPILOGUE_UPDATE_DISPLAY_NAME_FAILED);
            AppLog.e(T.API, "SignupEpilogueFragment.onAccountChanged: " +
                    event.error.type + " - " + event.error.message);
            endProgress();
            showErrorDialog(getString(R.string.signup_epilogue_error_generic));
        // Wait to populate epilogue for email interface until account is fetched and email address
        // is available since flow is coming from magic link with no instance argument values.
        } else if (event.causeOfChange == AccountAction.FETCH_ACCOUNT
                && !TextUtils.isEmpty(mAccountStore.getAccount().getEmail())) {
            endProgress();
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
        } else if (changedUsername()) {
            startProgress();
            PushUsernamePayload payload = new PushUsernamePayload(mUsername,
                    AccountUsernameActionType.KEEP_OLD_SITE_AND_ADDRESS);
            mDispatcher.dispatch(AccountActionBuilder.newPushUsernameAction(payload));
        } else if (event.causeOfChange == AccountAction.PUSH_SETTINGS && mSignupEpilogueListener != null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_SOCIAL_EPILOGUE_UPDATE_DISPLAY_NAME_SUCCEEDED);
            mSignupEpilogueListener.onContinue();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUsernameChanged(OnUsernameChanged event) {
        if (event.isError()) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_SOCIAL_EPILOGUE_UPDATE_USERNAME_FAILED);
            AppLog.e(T.API, "SignupEpilogueFragment.onUsernameChanged: " +
                    event.error.type + " - " + event.error.message);
            endProgress();
            showErrorDialog(getString(R.string.signup_epilogue_error_generic));
        } else if (mSignupEpilogueListener != null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_SOCIAL_EPILOGUE_UPDATE_USERNAME_SUCCEEDED);
            mSignupEpilogueListener.onContinue();
        }
    }

    protected boolean changedDisplayName() {
        return !TextUtils.equals(getArguments().getString(ARG_DISPLAY_NAME), mDisplayName);
    }

    protected boolean changedPassword() {
        return !TextUtils.isEmpty(mInputPassword.getEditText().getText().toString());
    }

    protected boolean changedUsername() {
        return !TextUtils.equals(getArguments().getString(ARG_USERNAME), mUsername);
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
            String capitalized = s.substring(0, 1).toUpperCase() + s.substring(1);
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
        return mEmailAddress.split("@")[0].replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    private void injectCache(File file, String avatarUrl) throws IOException {
        final SimpleDateFormat sdf = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.US);
        final long currentTimeMs = System.currentTimeMillis();
        final Date currentTime = new Date(currentTimeMs);
        final long fiveMinutesLaterMs = currentTimeMs + 5 * 60 * 1000;
        final Date fiveMinutesLater = new Date(fiveMinutesLaterMs);

        Cache.Entry entry = new Cache.Entry();

        entry.data = new byte[(int) file.length()];
        DataInputStream dis = new DataInputStream(new FileInputStream(file));
        dis.readFully(entry.data);
        dis.close();

        entry.etag = null;
        entry.softTtl = fiveMinutesLaterMs;
        entry.ttl = fiveMinutesLaterMs;
        entry.serverDate = currentTimeMs;
        entry.lastModified = currentTimeMs;

        entry.responseHeaders = new TreeMap<>();
        entry.responseHeaders.put("Accept-Ranges", "bytes");
        entry.responseHeaders.put("Access-Control-Allow-Origin", "*");
        entry.responseHeaders.put("Cache-Control", "max-age=300");
        entry.responseHeaders.put("Content-Disposition", "inline; filename=\""
                + mAccountStore.getAccount().getAvatarUrl() + ".jpeg\"");
        entry.responseHeaders.put("Content-Length", String.valueOf(file.length()));
        entry.responseHeaders.put("Content-Type", "image/jpeg");
        entry.responseHeaders.put("Date", sdf.format(currentTime));
        entry.responseHeaders.put("Expires", sdf.format(fiveMinutesLater));
        entry.responseHeaders.put("Last-Modified", sdf.format(currentTime));
        entry.responseHeaders.put("Link", "<" + avatarUrl + ">; rel=\"canonical\"");
        entry.responseHeaders.put("Server", "injected cache");
        entry.responseHeaders.put("Source-Age", "0");
        entry.responseHeaders.put("X-Android-Received-Millis", String.valueOf(currentTimeMs));
        entry.responseHeaders.put("X-Android-Response-Source", "NETWORK 200");
        entry.responseHeaders.put("X-Android-Selected-Protocol", "http/1.1");
        entry.responseHeaders.put("X-Android-Sent-Millis", String.valueOf(currentTimeMs));

        WordPress.sRequestQueue.getCache().put(Request.Method.GET + ":" + avatarUrl, entry);
    }

    protected void launchDialog() {
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

    protected void loadAvatar(String avatarUrl, String injectFilePath) {
        if (injectFilePath != null && !injectFilePath.isEmpty()) {
            // Remove specific URL entry from bitmap cache.  Update it via injected request cache.
            WordPress.getBitmapCache().removeSimilar(avatarUrl);

            try {
                // Inject request cache with new image. Gravatar backend (plus CDNs) can't be
                // trusted to update the image quick enough.
                injectCache(new File(injectFilePath), avatarUrl);
            } catch (IOException exception) {
                AppLog.e(T.NUX, "Gravatar image could not be injected into request cache - " +
                        exception.toString() + " - " + exception.getMessage());
                showErrorDialogAvatar();
            }

            mHeaderAvatar.resetImage();
            mHeaderAvatar.removeCurrentUrlFromSkiplist();
        }

        mHeaderAvatar.setImageUrl(avatarUrl, ImageType.AVATAR, new WPNetworkImageView.ImageLoadListener() {
            @Override
            public void onError() {
                showErrorDialogAvatar();
            }

            @Override
            public void onLoaded() {
            }
        });
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
                    // DialogInterface.BUTTON_NEUTRAL is intentionally ignored.  Just dismiss dialog.
                }
            }
        };

        AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.LoginTheme))
                .setMessage(message)
                .setNeutralButton(R.string.login_error_button, dialogListener)
                .setNegativeButton(R.string.signup_epilogue_error_button_negative, dialogListener)
                .setPositiveButton(R.string.signup_epilogue_error_button_positive, dialogListener)
                .create();
        dialog.show();
    }

    protected void showErrorDialogAvatar() {
        AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.LoginTheme))
                .setMessage(R.string.signup_epilogue_error_avatar)
                .setPositiveButton(R.string.login_error_button, null)
                .create();
        dialog.show();
    }

    protected void startCropActivity(Uri uri) {
        final Context context = getActivity();

        if (context != null) {
            UCrop.Options options = new UCrop.Options();
            options.setShowCropGrid(false);
            options.setStatusBarColor(ContextCompat.getColor(context, R.color.status_bar_tint));
            options.setToolbarColor(ContextCompat.getColor(context, R.color.color_primary));
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
                startProgress();

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
                                showErrorDialogAvatar();
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
        mDisplayName = getArguments().getString(ARG_DISPLAY_NAME);
        mEditTextDisplayName.setText(mDisplayName);
        mUsername = getArguments().getString(ARG_USERNAME);
        mEditTextUsername.setText(mUsername);
        mInputPassword.getEditText().setText("");
        updateAccountOrContinue();
    }

    protected void updateAccountOrContinue() {
        if (changedDisplayName()) {
            startProgress();
            PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
            payload.params = new HashMap<>();
            payload.params.put("display_name", mDisplayName);

            if (changedPassword()) {
                payload.params.put("password", mInputPassword.getEditText().getText().toString());
            }

            mDispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload));
        } else if (changedPassword()) {
            startProgress();
            PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
            payload.params = new HashMap<>();
            payload.params.put("password", mInputPassword.getEditText().getText().toString());
            mDispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload));
        } else if (changedUsername()) {
            startProgress();
            PushUsernamePayload payload = new PushUsernamePayload(mUsername,
                    AccountUsernameActionType.KEEP_OLD_SITE_AND_ADDRESS);
            mDispatcher.dispatch(AccountActionBuilder.newPushUsernameAction(payload));
        } else if (mSignupEpilogueListener != null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_SOCIAL_EPILOGUE_UNCHANGED);
            mSignupEpilogueListener.onContinue();
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
            } catch (URISyntaxException exception) {
                AppLog.e(T.NUX, "Google avatar download and Gravatar upload failed - " +
                        exception.toString() + " - " + exception.getMessage());
            }
        }
    }
}
