package org.wordpress.android.util.analytics.service;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.HashMap;
import java.util.Map;

import static org.wordpress.android.util.analytics.service.InstallationReferrerServiceStarter.ARG_REFERRER;

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

        // if a referrer string has been passed already (coming from com.android.vending.INSTALL_REFERRER receiver),
        // just send it to tracks
        if (extras != null && extras.containsKey(ARG_REFERRER)) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("install_referrer", extras.getString(ARG_REFERRER));
            AnalyticsTracker.track(AnalyticsTracker.Stat.INSTALLATION_REFERRER_OBTAINED, properties);
            // mark referrer as obtained now
            AppPrefs.setInstallationReferrerObtained(true);
            stopService();
            return;
        }


        // if not, try to obtain it from Play Store app connection through the Install Referrer API Library if possible
        mReferrerClient = InstallReferrerClient.newBuilder(mContext).build();
        final InstallReferrerStateListener installReferrerStateListener = new InstallReferrerStateListener() {
            @Override
            public void onInstallReferrerSetupFinished(int responseCode) {
                switch (responseCode) {
                    case InstallReferrerResponse.OK:
                        // Connection established
                        try {
                            AppLog.i(T.UTILS, "installation referrer connected");
                            ReferrerDetails response = mReferrerClient.getInstallReferrer();
                            // mark referrer as obtained so we don't try fetching it again
                            // Citing here:
                            //  Caution: The install referrer information will be available for 90 days and won't
                            //  change unless the application is reinstalled. To avoid unnecessary API calls in your
                            //  app, you should invoke the API only once during the first execution after install.
                            // read more: https://developer.android.com/google/play/installreferrer/library
                            AppPrefs.setInstallationReferrerObtained(true);

                            // handle and send information to Tracks here
                            Map<String, Object> properties = new HashMap<>();
                            properties.put("install_referrer", response.getInstallReferrer());
                            properties.put("install_referrer_timestamp_begin",
                                    response.getInstallBeginTimestampSeconds());
                            properties.put("install_referrer_timestamp_click",
                                    response.getReferrerClickTimestampSeconds());
                            AnalyticsTracker.track(AnalyticsTracker.Stat.INSTALLATION_REFERRER_OBTAINED, properties);
                            mReferrerClient.endConnection();
                        } catch (RemoteException | IllegalStateException e) {
                            e.printStackTrace();
                            AppLog.e(T.UTILS,
                                    "installation referrer: " + e.getClass().getSimpleName() + " occurred", e);
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
                        // same as above, this error can be retried
                        // just log but DO NOT mark AppPrefs.setInstallationReferrerObtained(true);,
                        // we can obtain it also from the old com.android.vending.INSTALL_REFERRER intent
                        // if this is retried but the error persists
                        AppLog.i(T.UTILS, "installation referrer: service unavailable");
                        break;
                    case InstallReferrerResponse.DEVELOPER_ERROR:
                        break;
                    case InstallReferrerResponse.SERVICE_DISCONNECTED:
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
        };
        try {
            mReferrerClient.startConnection(installReferrerStateListener);
        } catch (RuntimeException e) {
            AppLog.e(T.UTILS, "installation referrer start connection failed!", e);
            AnalyticsTracker.track(Stat.INSTALLATION_REFERRER_FAILED);

            // just bail if we were not able to connect to the installation referrer service
            stopService();
        }
    }

    private void stopService() {
        mCompletionListener.onCompleted(mListenerCompanion);
    }

    interface ServiceCompletionListener {
        void onCompleted(Object companion);
    }
}
