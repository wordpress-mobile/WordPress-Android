package org.wordpress.android.push;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.support.ZendeskHelper;
import org.wordpress.android.ui.notifications.SystemNotificationsTracker;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.Map;

import javax.inject.Inject;

import static org.wordpress.android.push.GCMMessageHandler.PUSH_TYPE_ZENDESK;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class GCMMessageService extends FirebaseMessagingService {
    public static final String EXTRA_VOICE_OR_INLINE_REPLY = "extra_voice_or_inline_reply";

    public static final String PUSH_ARG_NOTE_ID = "note_id";
    public static final String PUSH_ARG_NOTE_FULL_DATA = "note_full_data";
    private static final String PUSH_ARG_ZENDESK_REQUEST_ID = "zendesk_sdk_request_id";

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject ZendeskHelper mZendeskHelper;
    @Inject SystemNotificationsTracker mSystemNotificationsTracker;
    @Inject GCMMessageHandler mGCMMessageHandler;

    private void synchronizedHandleDefaultPush(@NonNull Map<String, String> data) {
        // ACTIVE_NOTIFICATIONS_MAP being static, we can't just synchronize the method
        mSystemNotificationsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_RECEIVED_PROCESSING_START);
        synchronized (GCMMessageService.class) {
            mGCMMessageHandler.handleDefaultPush(
                    this, convertMapToBundle(data), mAccountStore.getAccount().getUserId());
        }
        mSystemNotificationsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_RECEIVED_PROCESSING_END);
    }

    // convert FCM RemoteMessage's Map into legacy GCM Bundle to keep code changes to a minimum
    private Bundle convertMapToBundle(@NonNull Map<String, String> data) {
        Bundle bundle = new Bundle();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }
        return bundle;
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        Map data = message.getData();
        AppLog.v(T.NOTIFS, "Received Message");

        if (data == null) {
            AppLog.v(T.NOTIFS, "No notification message content received. Aborting.");
            return;
        }

        if (!mAccountStore.hasAccessToken()) {
            return;
        }

        if (PUSH_TYPE_ZENDESK.equals(String.valueOf(data.get("type")))) {
            String zendeskRequestId = String.valueOf(data.get(PUSH_ARG_ZENDESK_REQUEST_ID));

            // Try to refresh the Zendesk request page if it's currently being displayed; otherwise show a notification
            if (!mZendeskHelper.refreshRequest(this, zendeskRequestId)) {
                mGCMMessageHandler.handleZendeskNotification(this);
            }
        }

        synchronizedHandleDefaultPush(data);
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is also called
     * when the InstanceID token is initially generated, so this is where
     * you retrieve the token.
     */

    @Override public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        GCMRegistrationIntentService.enqueueWork(this,
                new Intent(this, GCMRegistrationIntentService.class));
    }
}
