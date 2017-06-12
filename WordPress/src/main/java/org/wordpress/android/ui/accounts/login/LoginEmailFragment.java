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

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore.OnAvailabilityChecked;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

public class LoginEmailFragment extends Fragment implements TextWatcher {
    private static final String KEY_IN_PROGRESS = "KEY_IN_PROGRESS";
    private static final String KEY_REQUESTED_EMAIL = "KEY_REQUESTED_EMAIL";

    public static final String TAG = "login_email_fragment_tag";
    public static final int MAX_EMAIL_LENGTH = 100;

    private TextInputLayout mEmailEditTextLayout;
    private EditText mEmailEditText;
    private Button mNextButton;
    private ProgressDialog mProgressDialog;

    private LoginListener mLoginListener;

    private boolean mInProgress;
    private String mRequestedEmail;

    @Inject Dispatcher mDispatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.login_email_screen, container, false);

        mEmailEditText = (EditText) rootView.findViewById(R.id.login_email);
        mEmailEditText.addTextChangedListener(this);

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

        rootView.findViewById(R.id.login_site_address).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginListener != null) {
                    mLoginListener.loginViaSiteAddress();
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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mInProgress = savedInstanceState.getBoolean(KEY_IN_PROGRESS);
            mRequestedEmail = savedInstanceState.getString(KEY_REQUESTED_EMAIL);

            if (mInProgress) {
                showEmailCheckProgressDialog();
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof LoginListener) {
            mLoginListener = (LoginListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement LoginListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoginListener = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_IN_PROGRESS, mInProgress);
        outState.putString(KEY_REQUESTED_EMAIL, mRequestedEmail);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_login, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.help) {
            mLoginListener.help();
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


    protected void next(String email) {
        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        if (isValidEmail(email)) {
            showEmailCheckProgressDialog();
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
        updateNextButton();
        mEmailEditTextLayout.setError(null);
    }

    private void updateNextButton() {
        mNextButton.setEnabled(getCleanedEmail().length() > 0);
    }

    private void showEmailError(int messageId) {
        mEmailEditTextLayout.setError(getString(messageId));
    }

    @Override
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mDispatcher.unregister(this);
    }

    private void showEmailCheckProgressDialog() {
        mNextButton.setEnabled(false);
        mProgressDialog =
                ProgressDialog.show(getActivity(), "", getActivity().getString(R.string.checking_email), true, true,
                        new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                if (mInProgress) {
                                    endProgress();
                                }
                            }
                        });
        mInProgress = true;
    }

    private void endProgress() {
        mInProgress = false;

        if (mProgressDialog != null) {
            mProgressDialog.cancel();
            mProgressDialog = null;
        }

        mRequestedEmail = null;

        updateNextButton();
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAvailabilityChecked(OnAvailabilityChecked event) {
        if (mRequestedEmail == null || !mRequestedEmail.equalsIgnoreCase(event.value)) {
            // bail if user canceled or a different email request is outstanding
            return;
        }

        if (mInProgress) {
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
                    mLoginListener.showMagicLinkRequestScreen(event.value);
                }
                break;
            default:
                AppLog.e(T.API, "OnAvailabilityChecked unhandled event type: " + event.error.type);
                break;
        }
    }
}
