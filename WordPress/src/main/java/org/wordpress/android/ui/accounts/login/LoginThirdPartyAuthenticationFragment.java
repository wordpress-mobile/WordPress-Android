package org.wordpress.android.ui.accounts.login;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;

import org.wordpress.android.R;
import org.wordpress.android.util.AppLog;

import static android.app.Activity.RESULT_OK;

public class LoginThirdPartyAuthenticationFragment extends Fragment
        implements ActivityCompat.OnRequestPermissionsResultCallback,
                   GoogleApiClient.ConnectionCallbacks,
                   GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private LoginListener mLoginListener;
    private boolean isResolvingError;
    private boolean isShowingDialog;
    private boolean shouldResolveError;

    private static final String STATE_RESOLVING_ERROR = "STATE_RESOLVING_ERROR";
    private static final String STATE_SHOWING_DIALOG = "STATE_SHOWING_DIALOG";
    private static final int REQUEST_CONNECT = 1000;
    private static final int REQUEST_LOGIN = 1001;
    private static final int REQUEST_PERMISSIONS_GET_ACCOUNTS = 9000;

    public static final String TAG = "login_third_party_authentication_fragment_tag";

    @Override
    public void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);

        switch (request) {
            case REQUEST_CONNECT:
                if (result != RESULT_OK) {
                    shouldResolveError = false;
                }

                if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                } else {
                    showAccountDialog();
                }

                isResolvingError = false;
                break;
            case REQUEST_LOGIN:
                if (result == RESULT_OK) {
                    isShowingDialog = false;
                    GoogleSignInResult signInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

                    if (signInResult.isSuccess()) {
                        try {
                            GoogleSignInAccount account = signInResult.getSignInAccount();
                            String token = account.getIdToken();
                            // TODO: Validate token with server.
                        } catch (NullPointerException exception) {
                            AppLog.e(AppLog.T.NUX, "Cannot get ID token from Google sign-in account.", exception);
                            // TODO: Show error screen.
                        }
                    }
                }

                break;
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
    public void onConnected(Bundle bundle) {
        // Indicates account was selected, that account has granted any required permissions, and a
        // connection to Google Play services has been established.
        if (shouldResolveError) {
            shouldResolveError = false;
            showAccountDialog();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Could not connect to Google Play Services.  The user needs to select an account, grant
        // permissions or resolve an error in order to sign in.  Refer to the documentation for
        // ConnectionResult to see possible error codes.
        if (!isResolvingError && shouldResolveError) {
            if (connectionResult.hasResolution()) {
                try {
                    isResolvingError = true;
                    connectionResult.startResolutionForResult(getActivity(), REQUEST_CONNECT);
                } catch (IntentSender.SendIntentException exception) {
                    isResolvingError = false;
                    mGoogleApiClient.connect();
                }
            } else {
                isResolvingError = false;
                AppLog.e(AppLog.T.NUX, GoogleApiAvailability.getInstance().getErrorString(connectionResult.getErrorCode()));
                // TODO: Show error screen.
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Connection to Google Play services was lost.  GoogleApiClient will automatically attempt
        // to re-connect.  Any UI elements depending on connection to Google APIs should be hidden
        // or disabled until onConnected is called again.
        Log.w(LoginThirdPartyAuthenticationFragment.class.getSimpleName(), "onConnectionSuspended: " + i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // Restore state of error resolving and account choosing.
        if (savedInstanceState != null) {
            isResolvingError = savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
            isShowingDialog = savedInstanceState.getBoolean(STATE_SHOWING_DIALOG, false);
        }

        // Configure sign-in to request user's ID, basic profile, email address, and ID token.
        // ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // Build Google API client with access to sign-in API and options specified above.
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // Request account permission and start login process.
        if (!isShowingDialog) {
            requestAccountsPermission();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_login, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.login_third_party_authentication, container, false);

        layout.findViewById(R.id.login_google_try_again).setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings("PrivateMemberAccessBetweenOuterAndInnerClass")
            @Override
            public void onClick(View view) {
                if (!isResolvingError) {
                    requestAccountsPermission();
                }
            }
        });

        return layout;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoginListener = null;
        disconnectGoogleClient();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help:
                if (mLoginListener != null) {
                    // TODO: Pass email from Google login to help method.
                    mLoginListener.helpThirdParty("");
                }

                return true;
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int request, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(request, permissions, grantResults);

        switch (request) {
            case REQUEST_PERMISSIONS_GET_ACCOUNTS:
                isShowingDialog = false;

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    connectGoogleClient();
                }

                break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, isResolvingError);
        outState.putBoolean(STATE_SHOWING_DIALOG, isShowingDialog);
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

    public void connectGoogleClient() {
        if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
            shouldResolveError = true;
            mGoogleApiClient.connect();
        } else {
            showAccountDialog();
        }
    }

    private void disconnectGoogleClient() {
        if (mGoogleApiClient.isConnected()) {
            Auth.GoogleSignInApi.signOut(mGoogleApiClient);
            mGoogleApiClient.disconnect();
        }
    }

    private void requestAccountsPermission() {
        // Request accounts permission to get emails for Google login.
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            // Show explanation for permission then permission request dialog.
            if (shouldShowRequestPermissionRationale(Manifest.permission.GET_ACCOUNTS)) {
                // Show permission request dialog.
                DialogInterface.OnDismissListener dialogDismissListener = new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.GET_ACCOUNTS}, REQUEST_PERMISSIONS_GET_ACCOUNTS);
                    }
                };

                // Show explanation for permission.
                new AlertDialog.Builder(getContext())
                        .setTitle(getResources().getString(R.string.dialog_permissions_accounts_title))
                        .setMessage(getResources().getString(R.string.dialog_permissions_accounts_message))
                        .setPositiveButton(R.string.dialog_permissions_accounts_positive, null)
                        .setOnDismissListener(dialogDismissListener)
                        .show();
            // Show permission request dialog.
            } else {
                isShowingDialog = true;
                requestPermissions(new String[]{Manifest.permission.GET_ACCOUNTS}, REQUEST_PERMISSIONS_GET_ACCOUNTS);
            }
        // Connect Google API client.
        } else {
            connectGoogleClient();
        }
    }

    private void showAccountDialog() {
        isShowingDialog = true;
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, REQUEST_LOGIN);
    }
}
