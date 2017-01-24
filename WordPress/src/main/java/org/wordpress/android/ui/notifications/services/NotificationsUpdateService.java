package org.wordpress.android.ui.notifications.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.models.Note;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.notifications.utils.NotificationsActions;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;


public class NotificationsUpdateService extends Service {

    public static final String IS_TAPPED_ON_NOTIFICATION = "is-tapped-on-notification";

    private boolean running = false;
    private String mNoteId;
    private boolean isStartedByTappingOnNotification = false;

    public static void startService(Context context) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, NotificationsUpdateService.class);
        context.startService(intent);
    }

    public static void startService(Context context, String noteId) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, NotificationsUpdateService.class);
        intent.putExtra(NotificationsListFragment.NOTE_ID_EXTRA, noteId);
        intent.putExtra(IS_TAPPED_ON_NOTIFICATION, true);
        context.startService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(AppLog.T.NOTIFS, "notifications update service > created");
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.NOTIFS, "notifications update service > destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            mNoteId = intent.getStringExtra(NotificationsListFragment.NOTE_ID_EXTRA);
            isStartedByTappingOnNotification = intent.getBooleanExtra(IS_TAPPED_ON_NOTIFICATION, false);
            performRefresh();
        }
        return START_NOT_STICKY;
    }

    private void performRefresh() {
        if (running) {
            return;
        }
        running = true;
        Map<String, String> params = new HashMap<>();
        params.put("number", "200");
        params.put("num_note_items", "20");
        params.put("fields", RestClientUtils.NOTIFICATION_FIELDS);
        RestListener listener = new RestListener();
        WordPress.getRestClientUtilsV1_1().getNotifications(params, listener, listener);
    }

    private class RestListener implements RestRequest.Listener, RestRequest.ErrorListener {
        @Override
        public void onResponse(final JSONObject response) {
            List<Note> notes;
            if (response == null) {
                //Not sure this could ever happen, but make sure we're catching all response types
                AppLog.w(AppLog.T.NOTIFS, "Success, but did not receive any notes");
                EventBus.getDefault().post(
                        new NotificationEvents.NotificationsRefreshCompleted(
                                new ArrayList<Note>(0)
                        )
                );
            } else {
                try {
                    notes = NotificationsActions.parseNotes(response);
                    // if we have a note id, we were started from NotificationsDetailActivity.
                    // That means we need to re-set the *read* flag on this note.
                    if (isStartedByTappingOnNotification && mNoteId != null) {
                        setNoteRead(mNoteId, notes);
                    }
                    NotificationsTable.saveNotes(notes, true);
                    EventBus.getDefault().post(
                            new NotificationEvents.NotificationsRefreshCompleted(notes)
                    );
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.NOTIFS, "Success, but can't parse the response", e);
                    EventBus.getDefault().post(
                            new NotificationEvents.NotificationsRefreshError()
                    );
                }
            }
            completed();
        }

        @Override
        public void onErrorResponse(final VolleyError volleyError) {
            logVolleyErrorDetails(volleyError);
            EventBus.getDefault().post(
                    new NotificationEvents.NotificationsRefreshError(volleyError)
            );
            completed();
        }
    }

    private void setNoteRead(String noteId, List<Note> notes) {
        int notePos = NotificationsUtils.findNoteInNoteArray(notes, noteId);
        if (notePos != -1) {
            notes.get(notePos).setRead();
        }
    }

    private static void logVolleyErrorDetails(final VolleyError volleyError) {
        if (volleyError == null) {
            AppLog.e(AppLog.T.NOTIFS, "Tried to log a VolleyError, but the error obj was null!");
            return;
        }
        if (volleyError.networkResponse != null) {
            NetworkResponse networkResponse = volleyError.networkResponse;
            AppLog.e(AppLog.T.NOTIFS, "Network status code: " + networkResponse.statusCode);
            if (networkResponse.data != null) {
                AppLog.e(AppLog.T.NOTIFS, "Network data: " + new String(networkResponse.data));
            }
        }
        AppLog.e(AppLog.T.NOTIFS, "Volley Error Message: " + volleyError.getMessage(), volleyError);
    }

    private void completed() {
        AppLog.i(AppLog.T.NOTIFS, "notifications update service > completed");
        running = false;
        stopSelf();
    }
}
