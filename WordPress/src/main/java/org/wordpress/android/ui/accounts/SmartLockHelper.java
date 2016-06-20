package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.content.IntentSender;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.lang.ref.WeakReference;

public class SmartLockHelper {
    private GoogleApiClient mCredentialsClient;
    private WeakReference<FragmentActivity> mActivity;

    public interface Callback {
        void onCredentialRetrieved(Credential credential);
    }

    public SmartLockHelper(@NonNull FragmentActivity activity) {
        if (activity instanceof OnConnectionFailedListener && activity instanceof ConnectionCallbacks) {
            mActivity = new WeakReference<>(activity);
        } else {
            throw new RuntimeException("SmartLockHelper constructor needs an activity that " +
                    "implements OnConnectionFailedListener and ConnectionCallbacks");
        }
    }

    private FragmentActivity getActivityAndCheckAvailability() {
        FragmentActivity activity = mActivity.get();
        if (activity == null) {
            return null;
        }
        int status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity);
        if (status == ConnectionResult.SUCCESS) {
            return activity;
        }
        return null;
    }

    public void initSmartLockForPasswords() {
        FragmentActivity activity = getActivityAndCheckAvailability();
        if (activity == null) {
            return;
        }
        mCredentialsClient = new GoogleApiClient.Builder(activity)
                .addConnectionCallbacks((ConnectionCallbacks) activity)
                .enableAutoManage(activity, (OnConnectionFailedListener) activity)
                .addApi(Auth.CREDENTIALS_API)
                .build();
    }

    public void smartLockAutoFill(@NonNull final Callback callback) {
        Activity activity = getActivityAndCheckAvailability();
        if (activity == null || mCredentialsClient == null || !mCredentialsClient.isConnected()) {
            return;
        }
        CredentialRequest credentialRequest = new CredentialRequest.Builder()
                .setPasswordLoginSupported(true)
                .build();
        Auth.CredentialsApi.request(mCredentialsClient, credentialRequest).setResultCallback(
                new ResultCallback<CredentialRequestResult>() {
                    @Override
                    public void onResult(@NonNull CredentialRequestResult result) {
                        Status status = result.getStatus();
                        if (status.isSuccess()) {
                            Credential credential = result.getCredential();
                            callback.onCredentialRetrieved(credential);
                        } else {
                            if (status.getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED) {
                                try {
                                    Activity activity = getActivityAndCheckAvailability();
                                    if (activity == null) {
                                        return;
                                    }
                                    // Prompt the user to choose a saved credential
                                    status.startResolutionForResult(activity, SignInActivity.SMART_LOCK_READ);
                                } catch (IntentSender.SendIntentException e) {
                                    AppLog.d(T.NUX, "SmartLock: Failed to send resolution for credential request");
                                }
                            } else {
                                // The user must create an account or sign in manually.
                                AppLog.d(T.NUX, "SmartLock: Unsuccessful credential request.");
                            }
                        }
                    }
                });
    }


    public void saveCredentialsInSmartLock(@NonNull final String username, @NonNull final String password,
                                           @NonNull final String displayName, @NonNull final Uri profilePicture) {
        Activity activity = getActivityAndCheckAvailability();
        if (activity == null || mCredentialsClient == null || !mCredentialsClient.isConnected()) {
            return;
        }
        Credential credential = new Credential.Builder(username).setPassword(password)
                .setName(displayName).setProfilePictureUri(profilePicture).build();
        Auth.CredentialsApi.save(mCredentialsClient, credential).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (!status.isSuccess() && status.hasResolution()) {
                            try {
                                Activity activity = getActivityAndCheckAvailability();
                                if (activity == null) {
                                    return;
                                }
                                // This prompt the user to resolve the save request
                                status.startResolutionForResult(activity, SignInActivity.SMART_LOCK_SAVE);
                            } catch (IntentSender.SendIntentException e) {
                                // Could not resolve the request
                            }
                        }
                    }
                });
    }

    public void deleteCredentialsInSmartLock(@NonNull final String username, @NonNull final String password) {
        Activity activity = getActivityAndCheckAvailability();
        if (activity == null || mCredentialsClient == null || !mCredentialsClient.isConnected()) {
            return;
        }

        Credential credential = new Credential.Builder(username).setPassword(password).build();
        Auth.CredentialsApi.delete(mCredentialsClient, credential).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        AppLog.i(T.NUX, status.isSuccess() ? "SmartLock: credentials deleted for username: " + username
                                : "SmartLock: Credentials not deleted for username: " + username );
                    }
                });
    }
}
