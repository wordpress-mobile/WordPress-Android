package org.wordpress.android.ui.notifications.utils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class NotificationsActions {
    public static void refreshNotifications(final RestRequest.Listener listener,
                                            final RestRequest.ErrorListener errorListener) {
        WordPress.getRestClientUtilsV1_1().getNotifications(new RestRequest.Listener() {
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
            WordPress.getRestClientUtilsV1_1().markNoteAsRead(note.getId(), note.getUnreadCount(), new RestRequest.Listener() {
                @Override
                public void onResponse(JSONObject response) {
                    note.setUnreadCount("0");
                    NotificationsTable.putNote(note);
                }
            }, new RestRequest.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    AppLog.e(AppLog.T.NOTIFS, "Could not mark note as read via API.");
                }
            });

            EventBus.getDefault().post(new NotificationEvents.NotificationsChanged());
        }
    }
}
