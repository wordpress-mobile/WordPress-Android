package org.wordpress.android.login;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class SignupMagicLinkFragment extends Fragment {
    private static final String ARG_EMAIL_ADDRESS = "ARG_EMAIL_ADDRESS";

    public static final String TAG = "signup_magic_link_fragment_tag";

    private Button mOpenMailButton;
    private ProgressDialog mProgressDialog;

    @Inject protected Dispatcher mDispatcher;
    @Inject protected LoginAnalyticsListener mAnalyticsListener;
    protected LoginListener mLoginListener;
    protected String mEmail;
    protected boolean mInProgress;

    public static SignupMagicLinkFragment newInstance(String email) {
        SignupMagicLinkFragment fragment = new SignupMagicLinkFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL_ADDRESS, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mEmail = getArguments().getString(ARG_EMAIL_ADDRESS);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.signup_magic_link, container, false);

        mOpenMailButton = layout.findViewById(R.id.signup_magic_link_button);
        mOpenMailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mLoginListener != null) {
                    mLoginListener.openEmailClient();
                }
            }
        });

        if (savedInstanceState == null) {
            sendMagicLinkEmail();
        }

        return layout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            mAnalyticsListener.trackMagicLinkOpenEmailClientViewed();
        }
    }

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);

        if (context instanceof LoginListener) {
            mLoginListener = (LoginListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement LoginListener");
        }

        mDispatcher.register(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoginListener = null;
        mDispatcher.unregister(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_login, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.help) {
            if (mLoginListener != null) {
                mLoginListener.helpSignupMagicLinkScreen(mEmail);
            }

            return true;
        }

        return false;
    }

    protected void startProgress(String message) {
        mOpenMailButton.setEnabled(false);

        mProgressDialog = ProgressDialog.show(getActivity(), "", message, true, true,
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (mInProgress) {
                            endProgress();
                        }
                    }
                });

        mInProgress = true;
    }

    protected void endProgress() {
        mInProgress = false;

        if (mProgressDialog != null) {
            mProgressDialog.cancel();
        }

        mProgressDialog = null;
        mOpenMailButton.setEnabled(true);
    }

    protected void sendMagicLinkEmail() {
        if (NetworkUtils.checkConnection(getActivity())) {
            startProgress(getString(R.string.signup_magic_link_progress));
            AccountStore.AuthEmailPayload authEmailPayload = new AccountStore.AuthEmailPayload(mEmail, true);
            mDispatcher.dispatch(AuthenticationActionBuilder.newSendAuthEmailAction(authEmailPayload));
        }
    }

    protected void showErrorDialog(String message) {
        DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        sendMagicLinkEmail();
                        break;
                    // DialogInterface.BUTTON_NEGATIVE is intentionally ignored. Just dismiss dialog.
                }
            }
        };

        AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.LoginTheme))
                .setMessage(message)
                .setNegativeButton(R.string.signup_magic_link_error_button_negative, dialogListener)
                .setPositiveButton(R.string.signup_magic_link_error_button_positive, dialogListener)
                .create();
        dialog.show();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthEmailSent(AccountStore.OnAuthEmailSent event) {
        if (mInProgress) {
            endProgress();

            if (event.isError()) {
                mAnalyticsListener.trackSignupMagicLinkFailed();
                AppLog.e(T.API, "OnAuthEmailSent error: " + event.error.type + " - " + event.error.message);
                showErrorDialog(getString(R.string.signup_magic_link_error));
            } else {
                mAnalyticsListener.trackSignupMagicLinkSent();
            }
        }
    }
}
