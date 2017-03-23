package org.wordpress.android.ui.accounts.login;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.OnAvailabilityChecked;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.accounts.AbstractFragment;
import org.wordpress.android.ui.accounts.SignInDialogFragment;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.emailchecker2.EmailChecker;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

public class LoginEmailFragment extends AbstractFragment implements TextWatcher {
    public static final String TAG = "login_email_fragment_tag";
    public static final int MAX_EMAIL_LENGTH = 100;

    protected EditText mEmailEditText;

    protected boolean mEmailAutoCorrected;
    protected String mEmail;

    protected Button mNextButton;
    protected View mUsernamePasswordButton;

    protected @Inject SiteStore mSiteStore;
    protected @Inject AccountStore mAccountStore;
    protected @Inject Dispatcher mDispatcher;

    public interface OnMagicLinkEmailInteraction {
        void onMagicLinkEmailCheckSuccess(String email);
        void onLoginViaUsernamePassword();
    }
    private OnMagicLinkEmailInteraction mListener;

    public String getEmail() {
        return mEmail;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        AnalyticsTracker.track(Stat.LOGIN_ACCESSED);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.login_email_screen, container, false);

        mEmailEditText = (EditText) rootView.findViewById(R.id.login_email);
        mEmailEditText.addTextChangedListener(this);
        mNextButton = (Button) rootView.findViewById(R.id.login_email_next_button);
        mNextButton.setOnClickListener(mNextClickListener);
        mUsernamePasswordButton = rootView.findViewById(R.id.login_email_username_password);

        mEmailEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    autocorrectEmail();
                }
            }
        });

        autofillFromBuildConfig();

        mEmailEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((didPressNextKey(actionId, event) || didPressEnterKey(actionId, event))) {
                    next();
                    return true;
                } else {
                    return false;
                }
            }
        });

        mUsernamePasswordButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onLoginViaUsernamePassword();
            }
        });

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMagicLinkEmailInteraction) {
            mListener = (OnMagicLinkEmailInteraction) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnMagicLinkEmailInteraction");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    /*
     * autofill the username and password from BuildConfig/gradle.properties (developer feature,
     * only enabled for DEBUG releases)
     */
    private void autofillFromBuildConfig() {
        if (!BuildConfig.DEBUG) return;

        String email = (String) WordPress.getBuildConfigValue(getActivity().getApplication(),
                "DEBUG_DOTCOM_LOGIN_EMAIL");
        if (!TextUtils.isEmpty(email)) {
            mEmailEditText.setText(email);
            AppLog.d(T.NUX, "Autofilled email from build config");
        }
    }

    private void autocorrectEmail() {
        if (mEmailAutoCorrected) {
            return;
        }
        final String email = EditTextUtils.getText(mEmailEditText).trim();
        // Check if the username looks like an email address
        final Pattern emailRegExPattern = Patterns.EMAIL_ADDRESS;
        Matcher matcher = emailRegExPattern.matcher(email);
        if (!matcher.find()) {
            return;
        }
        // It looks like an email address, then try to correct it
        String suggest = EmailChecker.suggestDomainCorrection(email);
        if (suggest.compareTo(email) != 0) {
            mEmailAutoCorrected = true;
            mEmailEditText.setText(suggest);
            mEmailEditText.setSelection(suggest.length());
        }
    }

    protected void onDoneAction() {
        next();
    }

    private boolean checkNetworkConnectivity() {
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            SignInDialogFragment nuxAlert;
            nuxAlert = SignInDialogFragment.newInstance(getString(R.string.no_network_title),
                    getString(R.string.no_network_message),
                    R.drawable.ic_notice_white_64dp,
                    getString(R.string.cancel));
            ft.add(nuxAlert, "alert");
            ft.commitAllowingStateLoss();
            return false;
        }
        return true;
    }

    protected void next() {
        if (!checkNetworkConnectivity()) {
            return;
        }

        if (isUsernameEmail()) {
            startProgress(getActivity().getString(R.string.checking_email));
            mDispatcher.dispatch(AccountActionBuilder.newIsAvailableEmailAction(mEmail));
        } else {
            showEmailError(R.string.email_invalid);
        }
    }

    private boolean isUsernameEmail() {
        mEmail = EditTextUtils.getText(mEmailEditText).trim();
        Pattern emailRegExPattern = Patterns.EMAIL_ADDRESS;
        Matcher matcher = emailRegExPattern.matcher(mEmail);

        return matcher.find() && mEmail.length() <= MAX_EMAIL_LENGTH;
    }

    private final OnClickListener mNextClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            next();
        }
    };

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (fieldsFilled()) {
            mNextButton.setEnabled(true);
        } else {
            mNextButton.setEnabled(false);
        }
        mEmailEditText.setError(null);
    }

    private boolean fieldsFilled() {
        return EditTextUtils.getText(mEmailEditText).trim().length() > 0;
    }

    protected boolean isUserDataValid() {
        final String email = EditTextUtils.getText(mEmailEditText).trim();
        boolean retValue = true;

        if (TextUtils.isEmpty(email)) {
            mEmailEditText.setError(getString(R.string.required_field));
            mEmailEditText.requestFocus();
            retValue = false;
        }

        return retValue;
    }

    private void showEmailError(int messageId) {
        mEmailEditText.setError(getString(messageId));
        mEmailEditText.requestFocus();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Autofill username / password if string fields are set (only useful after an error in sign up).
        // This can't be done in onCreateView
        if (mEmail != null) {
            mEmailEditText.setText(mEmail);
        }
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mDispatcher.unregister(this);
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAvailabilityChecked(OnAvailabilityChecked event) {
        if (event.isError()) {
            AppLog.e(T.API, "OnAvailabilityChecked has error: " + event.error.type + " - " + event.error.message);
        }

        switch(event.type) {
            case EMAIL:
                handleEmailAvailabilityEvent(event);
                break;
            default:
                // TODO: we're not expecting any other availability check so, we should never have reached this line
                break;
        }
    }

    /**
     * Handler for an email availability event. If a user enters an email address for their
     * username an API checks to see if it belongs to a wpcom account.  If it exists the magic links
     * flow is followed. Otherwise the self-hosted sign in form is shown.
     * @param event
     */
    private void handleEmailAvailabilityEvent(OnAvailabilityChecked event) {
        if (!event.isAvailable) {
            // TODO: Email address exists in WordPress.com so, goto magic link offer screen
            // Email address exists in WordPress.com
            if (mListener != null) {
                mListener.onMagicLinkEmailCheckSuccess(mEmail);
            }
        } else {
            showEmailError(R.string.email_not_found);
        }
    }
}
