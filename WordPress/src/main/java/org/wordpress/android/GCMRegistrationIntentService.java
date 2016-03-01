package org.wordpress.android;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.HelpshiftHelper;

import java.util.UUID;

public class GCMRegistrationIntentService extends IntentService {

    public GCMRegistrationIntentService() {
        super("GCMRegistrationIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            InstanceID instanceID = InstanceID.getInstance(this);
            String gcmId = BuildConfig.GCM_ID;
            if (TextUtils.isEmpty(gcmId)) {
                AppLog.e(T.NOTIFS, "GCM_ID must be configured in gradle.properties");
                return;
            }
            String token = instanceID.getToken(gcmId, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            sendRegistrationToken(token);
        } catch (Exception e) {
            // SecurityException can happen on some devices without Google services (these devices probably strip
            // the AndroidManifest.xml and remove unsupported permissions).
            AppLog.e(T.NOTIFS, "Google Play Services unavailable: ", e);
        }
    }

    public void sendRegistrationToken(String gcmToken) {
        if (!TextUtils.isEmpty(gcmToken)) {
            AppLog.i(T.NOTIFS, "Sending GCM token to our remote services: " + gcmToken);
            // Register to WordPress.com notifications
            if (AccountHelper.isSignedInWordPressDotCom()) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                // Get or create UUID for WP.com notes api
                String uuid = preferences.getString(NotificationsUtils.WPCOM_PUSH_DEVICE_UUID, null);
                if (uuid == null) {
                    uuid = UUID.randomUUID().toString();
                    preferences.edit().putString(NotificationsUtils.WPCOM_PUSH_DEVICE_UUID, uuid).apply();
                }
                preferences.edit().putString(NotificationsUtils.WPCOM_PUSH_DEVICE_TOKEN, gcmToken).apply();
                NotificationsUtils.registerDeviceForPushNotifications(this, gcmToken);
            }

            // Register to other kind of notifications
            HelpshiftHelper.getInstance().registerDeviceToken(this, gcmToken);
            AnalyticsTracker.registerPushNotificationToken(gcmToken);
        } else {
            AppLog.w(T.NOTIFS, "Empty GCM token, can't register the id on remote services");
            PreferenceManager.getDefaultSharedPreferences(this).edit().remove(NotificationsUtils.WPCOM_PUSH_DEVICE_TOKEN).apply();
        }
    }
}
