package org.wordpress.android.login;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.util.AppLog;

import javax.inject.Inject;

import static android.app.Activity.RESULT_OK;

public class GoogleFragment extends Fragment implements ConnectionCallbacks, OnConnectionFailedListener {
    private boolean mIsResolvingError;
    private boolean mShouldResolveError;

    private static final String STATE_RESOLVING_ERROR = "STATE_RESOLVING_ERROR";
    private static final int REQUEST_CONNECT = 1000;

    protected GoogleApiClient mGoogleApiClient;
    protected GoogleListener mGoogleListener;
    protected LoginListener mLoginListener;
    protected String mDisplayName;
    protected String mGoogleEmail;
    protected String mIdToken;
    protected String mPhotoUrl;

    protected static final String SERVICE_TYPE_GOOGLE = "google";

    @Inject protected Dispatcher mDispatcher;
    @Inject protected SiteStore mSiteStore;

    @Inject protected LoginAnalyticsListener mAnalyticsListener;

    public interface GoogleListener {
        void onGoogleEmailSelected(String email);
        void onGoogleLoginFinished();
        void onGoogleSignupFinished(String name, String email, String photoUrl, String username);
        void onGoogleSignupError(String msg);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restore state of error resolving.
        mIsResolvingError = savedInstanceState != null && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

        // Configure sign-in to request user's ID, basic profile, email address, and ID token.
        // ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode(getString(R.string.default_web_client_id))
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestProfile()
                .requestEmail()
                .build();

        // Build Google API client with access to sign-in API and options specified above.
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity().getApplicationContext())
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if (!mIsResolvingError) {
            connectGoogleClient();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mIsResolvingError);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mLoginListener = (LoginListener) context;

        try {
            mGoogleListener = (GoogleListener) context;
        } catch (ClassCastException exception) {
            throw new ClassCastException(context.toString() + " must implement GoogleListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        disconnectGoogleClient();
    }

    @Override public void onResume() {
        super.onResume();
        // Show account dialog when Google API onConnected callback returns before fragment is attached.
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected() && !mIsResolvingError && !mShouldResolveError) {
            showAccountDialog();
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
    public void onConnected(Bundle bundle) {
        // Indicates account was selected, that account has granted any required permissions, and a
        // connection to Google Play services has been established.
        if (mShouldResolveError) {
            mShouldResolveError = false;

            if (isAdded()) {
                showAccountDialog();
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Could not connect to Google Play Services.  The user needs to select an account, grant
        // permissions or resolve an error in order to sign in.  Refer to the documentation for
        // ConnectionResult to see possible error codes.
        if (!mIsResolvingError && mShouldResolveError) {
            if (connectionResult.hasResolution()) {
                try {
                    mIsResolvingError = true;
                    connectionResult.startResolutionForResult(getActivity(), REQUEST_CONNECT);
                } catch (IntentSender.SendIntentException exception) {
                    mIsResolvingError = false;
                    mGoogleApiClient.connect();
                }
            } else {
                mIsResolvingError = false;
                AppLog.e(AppLog.T.NUX, GoogleApiAvailability.getInstance().getErrorString(
                        connectionResult.getErrorCode()));
                showError(getString(R.string.login_error_generic));
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Connection to Google Play services was lost.  GoogleApiClient will automatically attempt
        // to re-connect.  Any UI elements depending on connection to Google APIs should be hidden
        // or disabled until onConnected is called again.
        Log.w(LoginGoogleFragment.class.getSimpleName(), "onConnectionSuspended: " + i);
    }

    public void connectGoogleClient() {
        if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
            mShouldResolveError = true;
            mGoogleApiClient.connect();
        } else {
            showAccountDialog();
        }
    }

    protected void disconnectGoogleClient() {
        if (mGoogleApiClient.isConnected()) {
            Auth.GoogleSignInApi.signOut(mGoogleApiClient);
            mGoogleApiClient.disconnect();
        }
    }

    protected void showAccountDialog() {
        // Do nothing here.  This should be overridden by inheriting class.
    }

    protected void finishSignUp() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
        }
    }

    protected void showError(String message) {
        finishSignUp();
        mGoogleListener.onGoogleSignupError(message);
    }

    @Override
    public void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);

        switch (request) {
            case REQUEST_CONNECT:
                if (result != RESULT_OK) {
                    mShouldResolveError = false;
                }

                if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                } else {
                    showAccountDialog();
                }

                mIsResolvingError = false;
                break;
        }
    }
}
