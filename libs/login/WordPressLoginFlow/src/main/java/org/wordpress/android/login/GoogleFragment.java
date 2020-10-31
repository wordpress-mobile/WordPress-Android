package org.wordpress.android.login;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

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
import org.wordpress.android.util.AppLog.T;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import static android.app.Activity.RESULT_OK;

public abstract class GoogleFragment extends Fragment implements ConnectionCallbacks, OnConnectionFailedListener {
    private static final String STATE_SHOULD_RESOLVE_ERROR = "STATE_SHOULD_RESOLVE_ERROR";
    private static final String STATE_FINISHED = "STATE_FINISHED";
    private static final String STATE_DISPLAY_NAME = "STATE_DISPLAY_NAME";
    private static final String STATE_GOOGLE_EMAIL = "STATE_GOOGLE_EMAIL";
    private static final String STATE_GOOGLE_TOKEN_ID = "STATE_GOOGLE_TOKEN_ID";
    private static final String STATE_GOOGLE_PHOTO_URL = "STATE_GOOGLE_PHOTO_URL";
    private boolean mIsResolvingError;
    private boolean mShouldResolveError;
    /**
     * This flag is used to store the information the finishFlow was called when the fragment wasn't attached to an
     * activity (for example an EventBus event was received during ongoing configuration change).
     */
    private boolean mFinished;

    private static final String STATE_RESOLVING_ERROR = "STATE_RESOLVING_ERROR";
    private static final int REQUEST_CONNECT = 1000;

    protected GoogleApiClient mGoogleApiClient;
    protected GoogleListener mGoogleListener;
    protected LoginListener mLoginListener;
    protected String mDisplayName;
    protected String mGoogleEmail;
    protected String mIdToken;
    protected String mPhotoUrl;

    protected ProgressDialog mProgressDialog;

    public static final String SERVICE_TYPE_GOOGLE = "google";

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
        mDispatcher.register(this);
        if (savedInstanceState != null) {
            mIsResolvingError = savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
            mShouldResolveError = savedInstanceState.getBoolean(STATE_SHOULD_RESOLVE_ERROR, false);
            mFinished = savedInstanceState.getBoolean(STATE_FINISHED, false);
            mDisplayName = savedInstanceState.getString(STATE_DISPLAY_NAME);
            mGoogleEmail = savedInstanceState.getString(STATE_GOOGLE_EMAIL);
            mIdToken = savedInstanceState.getString(STATE_GOOGLE_TOKEN_ID);
            mPhotoUrl = savedInstanceState.getString(STATE_GOOGLE_PHOTO_URL);
        }

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
        outState.putBoolean(STATE_SHOULD_RESOLVE_ERROR, mShouldResolveError);
        outState.putBoolean(STATE_FINISHED, mFinished);
        outState.putString(STATE_DISPLAY_NAME, mDisplayName);
        outState.putString(STATE_GOOGLE_EMAIL, mGoogleEmail);
        outState.putString(STATE_GOOGLE_TOKEN_ID, mIdToken);
        outState.putString(STATE_GOOGLE_PHOTO_URL, mPhotoUrl);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mProgressDialog = ProgressDialog.show(getActivity(), null, getProgressDialogText(), true, false, null);
        mLoginListener = (LoginListener) context;

        try {
            mGoogleListener = (GoogleListener) context;
        } catch (ClassCastException exception) {
            throw new ClassCastException(context.toString() + " must implement GoogleListener");
        }
        if (mFinished) {
            finishFlow();
        }
    }

    @Override
    public void onDetach() {
        dismissProgressDialog();
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        disconnectGoogleClient();
        AppLog.d(T.MAIN, "GOOGLE SIGNUP/LOGIN: disconnecting google client");
        mDispatcher.unregister(this);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Show account dialog when Google API onConnected callback returns before fragment is attached.
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected() && !mIsResolvingError && !mShouldResolveError) {
            startFlow();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        // Indicates account was selected, that account has granted any required permissions, and a
        // connection to Google Play services has been established.
        if (mShouldResolveError) {
            mShouldResolveError = false;

            // if the fragment is not attached to an activity, the process is started in the onResume
            if (isAdded()) {
                startFlow();
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
            startFlow();
        }
    }

    protected void disconnectGoogleClient() {
        if (mGoogleApiClient.isConnected()) {
            Auth.GoogleSignInApi.signOut(mGoogleApiClient);
            mGoogleApiClient.disconnect();
        }
    }

    protected abstract String getProgressDialogText();

    protected abstract void startFlow();

    protected void finishFlow() {
        /* This flag might get lost when the finishFlow is called after the fragment's
         onSaveInstanceState was called - however it's a very rare case, since the fragment is retained across
         config changes.  */
        mFinished = true;
        if (getActivity() != null) {
            AppLog.d(T.MAIN, "GOOGLE SIGNUP/LOGIN: finishing signup/login");
            getActivity().getSupportFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
        }
    }

    protected void showError(String message) {
        finishFlow();
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
                    startFlow();
                }

                mIsResolvingError = false;
                break;
        }
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    // Remove scale from photo URL path string. Current URL matches /s96-c, which returns a 96 x 96
    // pixel image. Removing /s96-c from the string returns a 512 x 512 pixel image. Using regular
    // expressions may help if the photo URL scale value in the returned path changes.
    protected String removeScaleFromGooglePhotoUrl(String photoUrl) {
        Pattern pattern = Pattern.compile("(/s[0-9]+-c)");
        Matcher matcher = pattern.matcher(photoUrl);
        return matcher.find() ? photoUrl.replace(matcher.group(1), "") : photoUrl;
    }
}
