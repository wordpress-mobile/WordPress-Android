package org.wordpress.android.ui.accounts.signup;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPTextView;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import javax.inject.Inject;

public class SignupEpilogueSocialFragment extends LoginBaseFormFragment<SignupEpilogueListener> {
    private String mEmailAddress;
    private String mPhotoUrl;

    protected SignupEpilogueListener mSignupEpilogueListener;
    protected String mDisplayName;
    protected String mUsername;
    protected WPLoginInputRow mInputDisplayName;
    protected WPLoginInputRow mInputUsername;

    private static final String ARG_DISPLAY_NAME = "ARG_DISPLAY_NAME";
    private static final String ARG_EMAIL_ADDRESS = "ARG_EMAIL_ADDRESS";
    private static final String ARG_PHOTO_URL = "ARG_PHOTO_URL";
    private static final String ARG_USERNAME = "ARG_USERNAME";
    private static final String KEY_DISPLAY_NAME = "KEY_DISPLAY_NAME";
    private static final String KEY_USERNAME = "KEY_USERNAME";

    public static final String TAG = "signup_epilogue_fragment_tag";

    @Inject
    protected AccountStore mAccount;
    @Inject
    protected Dispatcher mDispatcher;

    public static SignupEpilogueSocialFragment newInstance(String displayName, String emailAddress,
                                                           String photoUrl, String username) {
        SignupEpilogueSocialFragment fragment = new SignupEpilogueSocialFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DISPLAY_NAME, displayName);
        args.putString(ARG_EMAIL_ADDRESS, emailAddress);
        args.putString(ARG_PHOTO_URL, photoUrl);
        args.putString(ARG_USERNAME, username);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected @LayoutRes int getContentLayout() {
        return 0;
    }

    @Override
    protected @LayoutRes int getProgressBarText() {
        return R.string.signup_updating_account;
    }

    @Override
    protected void setupLabel(@NonNull TextView label) {
    }

    @Override
    protected ViewGroup createMainView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return (ViewGroup) inflater.inflate(R.layout.signup_epilogue, container, false);
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        final WPNetworkImageView headerAvatar = (WPNetworkImageView) rootView.findViewById(R.id.signup_epilogue_header_avatar);
        headerAvatar.setImageUrl(mPhotoUrl, WPNetworkImageView.ImageType.AVATAR);
        final WPTextView headerDisplayName = (WPTextView) rootView.findViewById(R.id.signup_epilogue_header_display);
        headerDisplayName.setText(mDisplayName);
        final WPTextView headerEmailAddress = (WPTextView) rootView.findViewById(R.id.signup_epilogue_header_username);
        headerEmailAddress.setText(mEmailAddress);
        mInputDisplayName = (WPLoginInputRow) rootView.findViewById(R.id.signup_epilogue_input_display);
        mInputDisplayName.getEditText().setText(mDisplayName);
        mInputDisplayName.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mDisplayName = s.toString();
                headerDisplayName.setText(mDisplayName);
            }
        });
        mInputUsername = (WPLoginInputRow) rootView.findViewById(R.id.signup_epilogue_input_username);
        mInputUsername.getEditText().setText(mUsername);
        mInputUsername.getEditText().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Launch username changer.
            }
        });
        mInputUsername.getEditText().setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    // TODO: Launch username changer.
                }
            }
        });
        mInputUsername.getEditText().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                // Consume keyboard events except for Enter (i.e. click/tap) and Tab (i.e. focus/navigation).
                // The onKey method returns true if the listener has consumed the event and false otherwise
                // allowing hardware keyboard users to tap and navigate, but not input text as expected.
                return !(keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_TAB);
            }
        });

        // Set focus on static text field to avoid showing keyboard on start.
        headerEmailAddress.requestFocus();
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
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_SOCIAL_EPILOGUE_VIEWED);
            new DownloadAvatarAndUploadGravatarTask().execute(mPhotoUrl, mEmailAddress, mAccount.getAccessToken());
        } else {
            // Overwrite original display name and username if they have changed.
            mDisplayName = savedInstanceState.getString(KEY_DISPLAY_NAME);
            mUsername = savedInstanceState.getString(KEY_USERNAME);
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_DISPLAY_NAME, mDisplayName);
        outState.putString(KEY_USERNAME, mUsername);
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
            AppLog.e(AppLog.T.API, "SignupEpilogueSocialFragment.onAccountChanged: " +
                    event.error.type + " - " + event.error.message);
            endProgress();
            showErrorDialog(getString(R.string.signup_epilogue_error_generic));
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
            AppLog.e(AppLog.T.API, "SignupEpilogueSocialFragment.onUsernameChanged: " +
                    event.error.type + " - " + event.error.message);
            endProgress();
            showErrorDialog(getString(R.string.signup_epilogue_error_generic));
        } else if (mSignupEpilogueListener != null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_SOCIAL_EPILOGUE_UPDATE_USERNAME_SUCCEEDED);
            mSignupEpilogueListener.onContinue();
        }
    }

    protected boolean changedDisplayName() {
        return !StringUtils.equals(getArguments().getString(ARG_DISPLAY_NAME), mDisplayName);
    }

    protected boolean changedUsername() {
        return !StringUtils.equals(getArguments().getString(ARG_USERNAME), mUsername);
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

    protected void undoChanges() {
        mDisplayName = getArguments().getString(ARG_DISPLAY_NAME);
        mInputDisplayName.getEditText().setText(mDisplayName);
        mUsername = getArguments().getString(ARG_USERNAME);
        mInputUsername.getEditText().setText(mUsername);
        updateAccountOrContinue();
    }

    protected void updateAccountOrContinue() {
        if (changedDisplayName()) {
            startProgress();
            PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
            payload.params = new HashMap<>();
            payload.params.put("display_name", mDisplayName);
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

    private class DownloadAvatarAndUploadGravatarTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            if (params.length != 3) {
                return null;
            }

            String url = params[0];
            String email = params[1];
            String token = params[2];

            try {
                Uri uri = MediaUtils.downloadExternalMedia(getContext(), Uri.parse(url));
                File file = new File(new URI(uri.toString()));
                GravatarApi.uploadGravatar(file, email, token,
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

            return null;
        }
    }
}
