package org.wordpress.android.ui.accounts.login;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore.OnAvailabilityChecked;
import org.wordpress.android.ui.accounts.LoginMode;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginEmailFragment extends LoginBaseFormFragment implements TextWatcher {
    private static final String KEY_REQUESTED_EMAIL = "KEY_REQUESTED_EMAIL";

    public static final String TAG = "login_email_fragment_tag";
    public static final int MAX_EMAIL_LENGTH = 100;

    private TextInputLayout mEmailEditTextLayout;
    private EditText mEmailEditText;

    private String mRequestedEmail;

    @Override
    protected @LayoutRes int getContentLayout() {
        return R.layout.login_email_screen;
    }

    @Override
    protected @LayoutRes int getProgressBarText() {
        return R.string.checking_email;
    }

    @Override
    protected void setupLabel(TextView label) {
        if (mLoginListener.getLoginMode() == LoginMode.JETPACK_STATS) {
            label.setText(R.string.stats_sign_in_jetpack_different_com_account);
        } else {
            label.setText(R.string.enter_email_wordpress_com);
        }
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        mEmailEditTextLayout = (TextInputLayout) rootView.findViewById(R.id.input_layout);
        mEmailEditTextLayout.setHint(getString(R.string.email_address));

        ImageView icon = (ImageView) rootView.findViewById(R.id.icon);
        icon.setContentDescription(getString(R.string.login_email_image));
        icon.setImageResource(R.drawable.ic_user_grey_24dp);

        mEmailEditText = (EditText) rootView.findViewById(R.id.input);
        mEmailEditText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        mEmailEditText.addTextChangedListener(this);

        autoFillFromBuildConfig();

        mEmailEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null
                        && event.getAction() == KeyEvent.ACTION_UP
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    next(getCleanedEmail());
                }

                // always consume the event so the focus stays in the EditText
                return true;
            }
        });
    }

    @Override
    protected void setupBottomButtons(Button secondaryButton, Button primaryButton) {
        secondaryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginListener != null) {
                    if (mLoginListener.getLoginMode() == LoginMode.JETPACK_STATS) {
                        mLoginListener.loginViaWpcomUsernameInstead();
                    } else {
                        mLoginListener.loginViaSiteAddress();
                    }
                }
            }
        });

        switch (mLoginListener.getLoginMode()) {
            case FULL:
                // all features enabled and with typical values
                secondaryButton.setText(R.string.enter_site_address_instead);
                break;
            case JETPACK_STATS:
                secondaryButton.setText(R.string.enter_username_instead);
                break;
            case WPCOM_LOGIN_DEEPLINK:
                secondaryButton.setVisibility(View.GONE);
                break;
        }

        primaryButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                next(getCleanedEmail());
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mRequestedEmail = savedInstanceState.getString(KEY_REQUESTED_EMAIL);
        }
    }

    @Override
    protected EditText getEditTextToFocusOnStart() {
        return mEmailEditText;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_REQUESTED_EMAIL, mRequestedEmail);
    }

    /*
     * auto-fill the username and password from BuildConfig/gradle.properties (developer feature,
     * only enabled for DEBUG releases)
     */
    private void autoFillFromBuildConfig() {
        if (!BuildConfig.DEBUG) return;

        String email = (String) WordPress.getBuildConfigValue(getActivity().getApplication(),
                "DEBUG_DOTCOM_LOGIN_EMAIL");
        if (!TextUtils.isEmpty(email)) {
            mEmailEditText.setText(email);
            AppLog.d(T.NUX, "Auto-filled email from build config");
        }
    }

    protected void next(String email) {
        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        if (isValidEmail(email)) {
            showProgressDialog();
            mRequestedEmail = email;
            mDispatcher.dispatch(AccountActionBuilder.newIsAvailableEmailAction(email));
        } else {
            showEmailError(R.string.email_invalid);
        }
    }

    private String getCleanedEmail() {
        return EditTextUtils.getText(mEmailEditText).trim();
    }

    private boolean isValidEmail(String email) {
        Pattern emailRegExPattern = Patterns.EMAIL_ADDRESS;
        Matcher matcher = emailRegExPattern.matcher(email);

        return matcher.find() && email.length() <= MAX_EMAIL_LENGTH;
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        mEmailEditTextLayout.setError(null);
    }

    private void showEmailError(int messageId) {
        mEmailEditTextLayout.setError(getString(messageId));
    }

    @Override
    protected void endProgress() {
        super.endProgress();
        mRequestedEmail = null;
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAvailabilityChecked(OnAvailabilityChecked event) {
        if (mRequestedEmail == null || !mRequestedEmail.equalsIgnoreCase(event.value)) {
            // bail if user canceled or a different email request is outstanding
            return;
        }

        if (isInProgress()) {
            endProgress();
        }

        if (event.isError()) {
            // report the error but don't bail yet.
            AppLog.e(T.API, "OnAvailabilityChecked has error: " + event.error.type + " - " + event.error.message);
            showEmailError(R.string.login_error_while_checking_email);
            return;
        }

        switch (event.type) {
            case EMAIL:
                if (event.isAvailable) {
                    // email address is available on wpcom, so apparently the user can't login with that one.
                    showEmailError(R.string.email_not_registered_wpcom);
                } else if (mLoginListener != null) {
                    EditTextUtils.hideSoftInput(mEmailEditText);
                    mLoginListener.gotWpcomEmail(event.value);
                }
                break;
            default:
                AppLog.e(T.API, "OnAvailabilityChecked unhandled event type: " + event.error.type);
                break;
        }
    }
}
