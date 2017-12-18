package org.wordpress.android.ui.accounts.signup;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.accounts.login.LoginBaseFormFragment;
import org.wordpress.android.ui.accounts.login.LoginListener;
import org.wordpress.android.widgets.WPLoginInputRow;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPTextView;

public class SignupEpilogueSocialFragment extends LoginBaseFormFragment<LoginListener> {
    private String mDisplayName;
    private String mEmailAddress;
    private String mPhotoUrl;
    private String mUsername;

    private static final String ARG_DISPLAY_NAME = "ARG_DISPLAY_NAME";
    private static final String ARG_EMAIL_ADDRESS = "ARG_EMAIL_ADDRESS";
    private static final String ARG_PHOTO_URL = "ARG_PHOTO_URL";
    private static final String ARG_USERNAME = "ARG_USERNAME";
    private static final String KEY_DISPLAY_NAME = "KEY_DISPLAY_NAME";
    private static final String KEY_USERNAME = "KEY_USERNAME";

    public static final String TAG = "signup_epilogue_fragment_tag";

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
        return R.string.signup_creating_account;
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
        WPNetworkImageView avatar = (WPNetworkImageView) rootView.findViewById(R.id.avatar);
        avatar.setImageResource(R.drawable.ic_gridicons_user_circle_100dp);
        final WPTextView headerDisplayName = (WPTextView) rootView.findViewById(R.id.signup_epilogue_header_display);
        headerDisplayName.setText(mDisplayName);
        WPTextView headerEmailAddress = (WPTextView) rootView.findViewById(R.id.signup_epilogue_header_username);
        headerEmailAddress.setText(mEmailAddress);
        WPNetworkImageView headerAvatar = (WPNetworkImageView) rootView.findViewById(R.id.avatar);
        headerAvatar.setImageUrl(mPhotoUrl, WPNetworkImageView.ImageType.AVATAR);
        WPLoginInputRow inputDisplayName = (WPLoginInputRow) rootView.findViewById(R.id.signup_epilogue_input_display);
        inputDisplayName.getEditText().setText(mDisplayName);
        inputDisplayName.getEditText().addTextChangedListener(new TextWatcher() {
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
        WPLoginInputRow inputUsername = (WPLoginInputRow) rootView.findViewById(R.id.signup_epilogue_input_username);
        inputUsername.getEditText().setText(mUsername);
        inputUsername.getEditText().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Launch username changer.
            }
        });
        inputUsername.getEditText().setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    // TODO: Launch username changer.
                }
            }
        });
        inputUsername.getEditText().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                // Consume keyboard events except for Enter (i.e. click/tap) and Tab (i.e. focus/navigation).
                // The onKey method returns true if the listener has consumed the event and false otherwise
                // allowing hardware keyboard users to tap and navigate, but not input text as expected.
                return !(keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_TAB);
            }
        });
    }

    @Override
    protected void setupBottomButtons(Button secondaryButton, Button primaryButton) {
        primaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startProgress();
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
        } else {
            // Overwrite original display name and username if they have changed.
            mDisplayName = savedInstanceState.getString(KEY_DISPLAY_NAME);
            mUsername = savedInstanceState.getString(KEY_USERNAME);
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
}
