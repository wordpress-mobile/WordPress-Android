package org.wordpress.android.util.analytics.service;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;

import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;


/**
 * Background service to retrieve installation referrer information.
 */

public class InstallationReferrerServiceLogic {
    private ServiceCompletionListener mCompletionListener;
    private Object mListenerCompanion;
    private InstallReferrerClient mReferrerClient;
    private Context mContext;

    public InstallationReferrerServiceLogic(Context context, ServiceCompletionListener completionListener) {
        mCompletionListener = completionListener;
        mContext = context;
    }

    public void onDestroy() {
        AppLog.i(T.UTILS, "installation referrer service destroyed");
    }

    public void performTask(Bundle extras, Object companion) {
        mListenerCompanion = companion;
        mReferrerClient = InstallReferrerClient.newBuilder(mContext).build();
        mReferrerClient.startConnection(new InstallReferrerStateListener() {
            @Override
            public void onInstallReferrerSetupFinished(int responseCode) {
                switch (responseCode) {
                    case InstallReferrerResponse.OK:
                        // Connection established
                        try {
                            AppLog.i(T.UTILS, "installation referrer connected");
                            ReferrerDetails response = mReferrerClient.getInstallReferrer();
                            if (!AppPrefs.isInstallationReferrerObtained()) {
                                AppPrefs.setInstallationReferrerObtained(true);
                                // TODO handle and send information to Tracks here
                                // handleReferrer(response);
                            }
                            mReferrerClient.endConnection();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        break;
                    case InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                        // API not available on the current Play Store app
                        // just log but DO NOT mark AppPrefs.setInstallationReferrerObtained(true);,
                        // because the user could update to a version of Google PLay that supports it
                        // and we can obtain it also from the old com.android.vending.INSTALL_REFERRER intent
                        AppLog.i(T.UTILS, "installation referrer: feature not supported");
                        break;
                    case InstallReferrerResponse.SERVICE_UNAVAILABLE:
                        // Connection could not be established
                        // same as above, this is a retriable error
                        // just log but DO NOT mark AppPrefs.setInstallationReferrerObtained(true);,
                        // and we can obtain it also from the old com.android.vending.INSTALL_REFERRER intent
                        // if this is retried but the error persists
                        AppLog.i(T.UTILS, "installation referrer: service unavailable");
                        break;
                }

                stopService();
            }

            @Override
            public void onInstallReferrerServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                stopService();
            }
        });
    }

    private void stopService() {
        mCompletionListener.onCompleted(mListenerCompanion);
    }

    interface ServiceCompletionListener {
        void onCompleted(Object companion);
    }
}
