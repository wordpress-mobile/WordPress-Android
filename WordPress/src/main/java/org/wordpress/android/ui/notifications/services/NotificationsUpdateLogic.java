package org.wordpress.android.ui.notifications.services;

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
import org.wordpress.android.ui.notifications.utils.NotificationsActions;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.greenrobot.event.EventBus;

public class NotificationsUpdateLogic {
    private ServiceCompletionListener mCompletionListener;
    private Object mListenerCompanion;

    private boolean mRunning = false;
    private String mNoteId;
    private boolean mIsStartedByTappingOnNotification = false;
    private String mLocale;

    public NotificationsUpdateLogic(String locale, ServiceCompletionListener listener) {
        mLocale = locale;
        mCompletionListener = listener;
    }

    public void performRefresh(String noteId, boolean isStartedByTappingOnNotification, Object companion) {
        if (mRunning) {
            return;
        }
        mListenerCompanion = companion;
        mRunning = true;
        mNoteId = noteId;
        mIsStartedByTappingOnNotification = isStartedByTappingOnNotification;
        Map<String, String> params = new HashMap<>();
        params.put("number", "200");
        params.put("num_note_items", "20");
        params.put("fields", RestClientUtils.NOTIFICATION_FIELDS);
        if (!TextUtils.isEmpty(mLocale)) {
            params.put("locale", mLocale.toLowerCase(Locale.ENGLISH));
        }
        RestListener listener = new RestListener();
        WordPress.getRestClientUtilsV1_1().getNotifications(params, listener, listener);
    }

    private class RestListener implements RestRequest.Listener, RestRequest.ErrorListener {
        @Override
        public void onResponse(final JSONObject response) {
            List<Note> notes;
            if (response == null) {
                // Not sure this could ever happen, but make sure we're catching all response types
                AppLog.w(AppLog.T.NOTIFS, "Success, but did not receive any notes");
                EventBus.getDefault().post(
                        new NotificationEvents.NotificationsRefreshCompleted(new ArrayList<Note>(0)));
            } else {
                try {
                    notes = NotificationsActions.parseNotes(response);
                    // if we have a note id, we were started from NotificationsDetailActivity.
                    // That means we need to re-set the *read* flag on this note.
                    if (mIsStartedByTappingOnNotification && mNoteId != null) {
                        setNoteRead(mNoteId, notes);
                    }
                    NotificationsTable.saveNotes(notes, true);
                    EventBus.getDefault().post(new NotificationEvents.NotificationsRefreshCompleted(notes));
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.NOTIFS, "Success, but can't parse the response", e);
                    EventBus.getDefault().post(new NotificationEvents.NotificationsRefreshError());
                }
            }
            completed();
        }

        @Override
        public void onErrorResponse(final VolleyError volleyError) {
            logVolleyErrorDetails(volleyError);
            EventBus.getDefault().post(new NotificationEvents.NotificationsRefreshError(volleyError));
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
        mRunning = false;
        mCompletionListener.onCompleted(mListenerCompanion);
    }

    interface ServiceCompletionListener {
        void onCompleted(Object companion);
    }
}
