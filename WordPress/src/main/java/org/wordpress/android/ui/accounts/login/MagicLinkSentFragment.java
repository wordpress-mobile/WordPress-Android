package org.wordpress.android.ui.accounts.login;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.accounts.AbstractFragment;
import org.wordpress.android.ui.accounts.HelpActivity;
import org.wordpress.android.ui.accounts.JetpackCallbacks;
import org.wordpress.android.ui.notifications.services.NotificationsUpdateService;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.HelpshiftHelper;
import org.wordpress.android.util.ToastUtils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class MagicLinkSentFragment extends AbstractFragment {
    public static final String TAG = "magic_link_sent_fragment_tag";

    public interface OnMagicLinkSentInteraction {
        void onEnterPasswordRequested();
        void onMagicLinkFlowSucceeded();
    }

    private String mToken = "";

    private OnMagicLinkSentInteraction mListener;
    private JetpackCallbacks mJetpackCallbacks;

    protected @Inject SiteStore mSiteStore;
    protected @Inject AccountStore mAccountStore;
    protected @Inject Dispatcher mDispatcher;

    protected boolean mSitesFetched = false;
    protected boolean mAccountSettingsFetched = false;
    protected boolean mAccountFetched = false;

    public MagicLinkSentFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMagicLinkSentInteraction) {
            mListener = (OnMagicLinkSentInteraction) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnMagicLinkSentInteraction");
        }
        if (context instanceof JetpackCallbacks) {
            mJetpackCallbacks = (JetpackCallbacks) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement JetpackCallbacks");
        }
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

    @Override
    public void onResume() {
        super.onResume();

        if (!mToken.isEmpty()) {
            attemptLoginWithMagicLink();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.magic_link_sent_fragment, container, false);

        TextView enterPasswordView = (TextView) view.findViewById(R.id.password_layout);
        enterPasswordView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onEnterPasswordRequested();
                }
            }
        });

        TextView openEmailView = (TextView) view.findViewById(R.id.open_email_button);
        openEmailView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEmailClient();
            }
        });
        
        initInfoButtons(view);

        return view;
    }

    private void initInfoButtons(View rootView) {
        View.OnClickListener infoButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), HelpActivity.class);
                intent.putExtra(HelpshiftHelper.ORIGIN_KEY, HelpshiftHelper.chooseHelpshiftLoginTag
                        (mJetpackCallbacks.isJetpackAuth(), true));
                startActivity(intent);
            }
        };
        ImageView infoButton = (ImageView) rootView.findViewById(R.id.info_button);
        infoButton.setOnClickListener(infoButtonListener);
    }

    private void openEmailClient() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_EMAIL);
        getActivity().startActivity(intent);
    }

    public void setToken(String token) {
        mToken = token;
    }

    public void attemptLoginWithMagicLink() {
        // Save Token to the AccountStore, this will a onAuthenticationChanged (that will pull account setting and
        // sites)
        AccountStore.UpdateTokenPayload payload = new AccountStore.UpdateTokenPayload(mToken);
        mDispatcher.dispatch(AccountActionBuilder.newUpdateAccessTokenAction(payload));
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(AccountStore.OnAccountChanged event) {
        if (event.isError()) {
            AppLog.e(AppLog.T.API, "onAccountChanged has error: " + event.error.type + " - " + event.error.message);
            showAccountError(event.error.type, event.error.message);
            return;
        }

        AppLog.i(AppLog.T.NUX, "onAccountChanged: " + event.toString());

        // Success
        mAccountSettingsFetched |= event.causeOfChange == AccountAction.FETCH_SETTINGS;
        mAccountFetched |= event.causeOfChange == AccountAction.FETCH_ACCOUNT;

        // Finish activity if sites have been fetched
        if (mSitesFetched && mAccountSettingsFetched && mAccountFetched) {
            // move on the the main activity
            mListener.onMagicLinkFlowSucceeded();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(AccountStore.OnAuthenticationChanged event) {
        if (event.isError()) {
            AppLog.e(AppLog.T.API, "onAuthenticationChanged has error: " + event.error.type + " - " + event.error.message);
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_FAILED, event.getClass().getSimpleName(), event.error.type.toString(), event.error.message);

            showAuthError(event.error.type, event.error.message);
            endProgress();
            return;
        }

        AppLog.i(AppLog.T.NUX, "onAuthenticationChanged: " + event.toString());

        fetchAccountSettingsAndSites();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(SiteStore.OnSiteChanged event) {
        AppLog.i(AppLog.T.NUX, "onSiteChanged: " + event.toString());

        if (event.isError()) {
            AppLog.e(AppLog.T.API, "onSiteChanged has error: " + event.error.type + " - " + event.error.toString());
            if (!isAdded()) {
                return;
            }
            if (event.error.type == SiteStore.SiteErrorType.DUPLICATE_SITE) {
                if (event.rowsAffected == 0) {
                    // If there is a duplicate site and not any site has been added, show an error and
                    // stop the sign in process
                    ToastUtils.showToast(getContext(), R.string.cannot_add_duplicate_site);
                    return;
                } else {
                    // If there is a duplicate site, notify the user something could be wrong,
                    // but continue the sign in process
                    ToastUtils.showToast(getContext(), R.string.duplicate_site_detected);
                }
            } else {
                return;
            }
        }

        // Login Successful
        trackAnalyticsSignIn();
        mSitesFetched = true;

        // Finish activity if account settings have been fetched or if it's a wporg site
        if ((mAccountSettingsFetched && mAccountFetched)) {
            mListener.onMagicLinkFlowSucceeded();
        }
    }

    private void showAccountError(AccountStore.AccountErrorType error, String errorMessage) {
        switch (error) {
            case ACCOUNT_FETCH_ERROR:
                showError(R.string.error_fetch_my_profile);
                break;
            case SETTINGS_FETCH_ERROR:
                showError(R.string.error_fetch_account_settings);
                break;
            case SETTINGS_POST_ERROR:
                showError(R.string.error_post_account_settings);
                break;
            case GENERIC_ERROR:
            default:
                showError(errorMessage);
                break;
        }
    }

    private void showAuthError(AccountStore.AuthenticationErrorType error, String errorMessage) {
        switch (error) {
            case INCORRECT_USERNAME_OR_PASSWORD:
            case NOT_AUTHENTICATED: // NOT_AUTHENTICATED is the generic error from XMLRPC response on first call.
//                handleInvalidUsernameOrPassword(R.string.username_or_password_incorrect);
                break;
            case INVALID_OTP:
//                showTwoStepCodeError(R.string.invalid_verification_code);
                break;
            case NEEDS_2FA:
//                setTwoStepAuthVisibility(true);
//                mTwoStepEditText.setText(getAuthCodeFromClipboard());
                break;
            case INVALID_REQUEST:
                // TODO: FluxC: could be specific?
            default:
                // For all other kind of error, show a dialog with API Response error message
//                AppLog.e(AppLog.T.NUX, "Server response: " + errorMessage);
//                showGenericErrorDialog(errorMessage);
                break;
        }
    }

    private void fetchAccountSettingsAndSites() {
        if (mAccountStore.hasAccessToken()) {
            // Fetch user infos
            mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
            mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
            // Fetch sites
            mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());
            // Start Notification service
            NotificationsUpdateService.startService(getActivity().getApplicationContext());
        }
    }

    @Override
    protected void onDoneAction() {
        // do nothing
    }

    @Override
    protected boolean isUserDataValid() {
        return true;
    }

    private void trackAnalyticsSignIn() {
        AnalyticsUtils.refreshMetadata(mAccountStore, mSiteStore);
        Map<String, Boolean> properties = new HashMap<>();
        properties.put("dotcom_user", true);
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNED_IN, properties);
    }
}
