package org.wordpress.android.ui.accounts.login;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
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
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

public class Login2FaFragment extends Fragment implements TextWatcher {
    private static final String KEY_IN_PROGRESS_MESSAGE_ID = "KEY_IN_PROGRESS_MESSAGE_ID";

    private static final String ARG_EMAIL_ADDRESS = "ARG_EMAIL_ADDRESS";
    private static final String ARG_PASSWORD = "ARG_PASSWORD";

    public static final String TAG = "login_2fa_fragment_tag";

    private TextInputLayout m2FaEditTextLayout;
    private EditText m2FaEditText;
    private Button mNextButton;
    private ProgressDialog mProgressDialog;

    private LoginListener mLoginListener;

    private @StringRes int mInProgressMessageId;

    private String mEmailAddress;
    private String mPassword;

    @Inject Dispatcher mDispatcher;

    public static Login2FaFragment newInstance(String emailAddress, String password) {
        Login2FaFragment fragment = new Login2FaFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL_ADDRESS, emailAddress);
        args.putString(ARG_PASSWORD, password);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        mEmailAddress = getArguments().getString(ARG_EMAIL_ADDRESS);
        mPassword = getArguments().getString(ARG_PASSWORD);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.login_2fa_screen, container, false);

        m2FaEditText = (EditText) rootView.findViewById(R.id.login_2fa);
        m2FaEditText.addTextChangedListener(this);
        m2FaEditTextLayout = (TextInputLayout) rootView.findViewById(R.id.login_2fa_layout);
        m2FaEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null
                        && event.getAction() == KeyEvent.ACTION_UP
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    next();
                }

                // always consume the event so the focus stays in the EditText
                return true;
            }
        });

        mNextButton = (Button) rootView.findViewById(R.id.login_2fa_next_button);
        mNextButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                next();
            }
        });

        rootView.findViewById(R.id.login_text_otp).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAdded()) {
                    doAuthAction(R.string.requesting_otp, "", true);
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

        if (savedInstanceState == null) {
            EditTextUtils.showSoftInput(m2FaEditText);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mInProgressMessageId = savedInstanceState.getInt(KEY_IN_PROGRESS_MESSAGE_ID, 0);

            if (mInProgressMessageId != 0) {
                showProgressDialog(mInProgressMessageId);
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

        outState.putInt(KEY_IN_PROGRESS_MESSAGE_ID, mInProgressMessageId);
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

    protected void next() {
        doAuthAction(R.string.logging_in, m2FaEditText.getText().toString(), false);
    }

    private void doAuthAction(@StringRes int messageId, String twoStepCode, boolean shouldSendTwoStepSMS) {
        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        showProgressDialog(messageId);

        AccountStore.AuthenticatePayload payload = new AccountStore.AuthenticatePayload(mEmailAddress, mPassword);
        payload.twoStepCode = twoStepCode;
        payload.shouldSendTwoStepSms = shouldSendTwoStepSMS;
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
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
        show2FaError(null);
    }

    private void updateNextButton() {
        mNextButton.setEnabled(m2FaEditText.getText().length() > 0);
    }

    private void show2FaError(String message) {
        m2FaEditTextLayout.setError(message);
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

    private void showProgressDialog(@StringRes int messageId) {
        mNextButton.setEnabled(false);
        mProgressDialog =
                ProgressDialog.show(getActivity(), "", getActivity().getString(messageId), true, true,
                        new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                if (mInProgressMessageId != 0) {
                                    endProgress();
                                }
                            }
                        });
        mInProgressMessageId = 0;
    }

    private void endProgress() {
        mInProgressMessageId = 0;

        if (mProgressDialog != null) {
            mProgressDialog.cancel();
            mProgressDialog = null;
        }

        updateNextButton();
    }

    private void handleAuthError(AccountStore.AuthenticationErrorType error, String errorMessage) {
        switch (error) {
            case INVALID_OTP:
                show2FaError(errorMessage);
                break;
            case NEEDS_2FA:
                // we get this error when requesting a verification code sent via SMS so, just ignore it.
                break;
            case INVALID_REQUEST:
                // TODO: FluxC: could be specific?
            default:
                AppLog.e(T.NUX, "Server response: " + errorMessage);

                ToastUtils.showToast(getActivity(),
                        errorMessage == null ? getString(R.string.error_generic) : errorMessage);
                break;
        }
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        endProgress();

        if (event.isError()) {
            AppLog.e(T.API, "onAuthenticationChanged has error: " + event.error.type + " - " + event.error.message);

            if (isAdded()) {
                handleAuthError(event.error.type, event.error.message);
            }

            return;
        }

        AppLog.i(T.NUX, "onAuthenticationChanged: " + event.toString());

        if (mLoginListener != null) {
            mLoginListener.loggedInViaPassword();
        }
    }

}
