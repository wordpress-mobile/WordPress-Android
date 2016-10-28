package org.wordpress.android.ui.notifications.utils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.models.Note;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;

public class NotificationsActions {

    public static void updateSeenNotes() {
        ArrayList<Note> latestNotes = NotificationsTable.getLatestNotes(1);
        if (latestNotes.size() == 0) return;
        WordPress.getRestClientUtilsV1_1().markNotificationsSeen(
                String.valueOf(latestNotes.get(0).getTimestamp()),
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Assuming that we've marked the most recent notification as seen. (Beware, seen != read).
                        EventBus.getDefault().post(new NotificationEvents.NotificationsUnseenStatus(false));
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(AppLog.T.NOTIFS, "Could not mark notifications/seen' value via API.", error);
                    }
                });
    }

    public static void getNotifications(final RestRequest.Listener listener,
                                        final RestRequest.ErrorListener errorListener) {

        Map<String, String> params = new HashMap<>();
        params.put("number", "200");
        params.put("num_note_items", "20");
        params.put("fields", RestClientUtils.NOTIFICATION_FIELDS);
        WordPress.getRestClientUtilsV1_1().getNotifications(params, new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (listener != null) {
                            listener.onResponse(response);
                        }
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (errorListener != null) {
                            errorListener.onErrorResponse(error);
                        }
                    }
                }
        );
    }

    public static List<Note> parseNotes(JSONObject response) throws JSONException {
        List<Note> notes;
        JSONArray notesJSON = response.getJSONArray("notes");
        notes = new ArrayList<>(notesJSON.length());
        for (int i = 0; i < notesJSON.length(); i++) {
            Note n = new Note(notesJSON.getJSONObject(i));
            notes.add(n);
        }
        return notes;
    }

    public static void markNoteAsRead(final Note note) {
        if (note == null) {
            return;
        }

        // mark the note as read if it's unread
        if (note.isUnread()) {
            WordPress.getRestClientUtilsV1_1().decrementUnreadCount(note.getId(), "1", new RestRequest.Listener() {
                @Override
                public void onResponse(JSONObject response) {
                    note.setRead();
                    NotificationsTable.putNote(note);
                    EventBus.getDefault().post(new NotificationEvents.NotificationsChanged());
                }
            }, new RestRequest.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    AppLog.e(AppLog.T.NOTIFS, "Could not mark note as read via API.");
                }
            });

        }
    }
}
