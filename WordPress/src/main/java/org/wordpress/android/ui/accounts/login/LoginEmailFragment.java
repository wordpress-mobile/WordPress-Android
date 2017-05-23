package org.wordpress.android.ui.accounts.login;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.accounts.login.LoginEmailAvailabilityFragment.Events.CheckUpdate;
import org.wordpress.android.ui.accounts.login.nav.LoginNav;
import org.wordpress.android.ui.accounts.login.nav.LoginStateGetter;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.emailchecker2.EmailChecker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;

public class LoginEmailFragment extends Fragment implements TextWatcher {
    public static final String TAG = "login_email_fragment_tag";
    public static final int MAX_EMAIL_LENGTH = 100;

    private TextInputLayout mEmailEditTextLayout;
    private EditText mEmailEditText;
    private Button mNextButton;
    private ProgressDialog mProgressDialog;

    private LoginNav.InputEmail mLoginNavInputEmail;
    private boolean mEmailAutoCorrected;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            // reset the email availability checker the first time we enter the screen
            LoginEmailAvailabilityFragment.clear();
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.login_email_screen, container, false);

        mEmailEditText = (EditText) rootView.findViewById(R.id.login_email);

        mEmailEditTextLayout = (TextInputLayout) rootView.findViewById(R.id.login_email_layout);

        mNextButton = (Button) rootView.findViewById(R.id.login_email_next_button);
        mNextButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    next(getCleanedEmail());
                }
        });

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

        rootView.findViewById(R.id.login_email_username_password).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginNavInputEmail != null) {
                    mLoginNavInputEmail.loginViaUsernamePassword();
                }
            }
        });

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof LoginStateGetter.FsmGetter) {
            mLoginNavInputEmail = ((LoginStateGetter.FsmGetter) context).getLoginStateGetter().getLoginNavInputEmail();
        } else {
            throw new RuntimeException(context.toString() + " must implement LoginStateGetter.FsmGetter");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoginNavInputEmail = null;

        // dismiss the progress dialog if open to avoid leaking the Activity reference
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }

        // stop listening to events if not attached to the Activity anymore
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);

        LoginEmailAvailabilityFragment.askUpdate();

        // enable the email input change listener last, after the EditBox has been set up (i.e. re-populated on rotate).
        //  The listener clears the state of the email checker so, need to avoid clearing it on rotate.
        mEmailEditText.addTextChangedListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_login, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.help) {
            mLoginNavInputEmail.help();
            return true;
        }

        return false;
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

    private void autoCorrectEmail() {
        if (mEmailAutoCorrected) {
            return;
        }
        final String email = getCleanedEmail();
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

    protected void next(String email) {
        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        autoCorrectEmail();

        if (isValidEmail(email)) {
            showEmailError(null);
            LoginEmailAvailabilityFragment.newCheckRequest(email);
        } else {
            showEmailError(getString(R.string.email_invalid));
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
        LoginEmailAvailabilityFragment.clear();
    }

    private void updateNextButton() {
        mNextButton.setEnabled(getCleanedEmail().length() > 0);
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(getActivity(), "", getString(R.string.checking_email), true, true,
                    new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    LoginEmailAvailabilityFragment.clear();
                }
            });
            mProgressDialog.show();
        } else {
            mProgressDialog.show();
        }
    }

    public void dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    private void clearResultWithMessage(String message) {
        updateNextButton();
        dismissProgressDialog();
        showEmailError(message);
    }

    private void showEmailError(String message) {
        mEmailEditTextLayout.setError(message);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CheckUpdate event) {
        switch (event.emailCheckState) {
            case IDLE:
                clearResultWithMessage(null);
                break;
            case IN_PROGRESS:
                mNextButton.setEnabled(false);
                showProgressDialog();
                break;
            case AVAILABLE_ON_WPCOM:
                // email address is available on wpcom so, the user can't login with that email
                clearResultWithMessage(getString(R.string.email_not_registered_wpcom));
                break;
            case UNAVAILABLE_ON_WPCOM:
                // reset the email checker since all went well
                LoginEmailAvailabilityFragment.clear();

                // email address is not available on wpcom, which means user does exist so, move on to next screen
                mLoginNavInputEmail.gotEmail(event.emailAddress);
                break;
            case ERROR:
                clearResultWithMessage(getString(R.string.login_error_while_checking_email));
                break;
        }
    }
}
