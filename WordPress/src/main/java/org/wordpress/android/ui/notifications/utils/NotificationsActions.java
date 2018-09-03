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
    // Get the latest note from the local DB and send its timestamp to the server.
    // The server will discard the value if we've already seen a most recent note elsewhere or on this device.
    public static void updateNotesSeenTimestamp() {
        ArrayList<Note> latestNotes = NotificationsTable.getLatestNotes(1);
        if (latestNotes.size() == 0) {
            return;
        }
        updateSeenTimestamp(latestNotes.get(0));
    }

    // If `note.getTimestamp()` is not the most recent seen note, the server will discard the value.
    public static void updateSeenTimestamp(Note note) {
        WordPress.getRestClientUtilsV1_1().markNotificationsSeen(
                String.valueOf(note.getTimestamp()),
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Assuming that we've marked the most recent notification as seen. (Beware, seen != read).
                        EventBus.getDefault().post(new NotificationEvents.NotificationsUnseenStatus(false));
                    }
                },
                new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(AppLog.T.NOTIFS, "Could not mark notifications/seen' value via API.", error);
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
            WordPress.getRestClientUtilsV1_1().decrementUnreadCount(note.getId(), "9999", new RestRequest.Listener() {
                @Override
                public void onResponse(JSONObject response) {
                    note.setRead();
                    NotificationsTable.saveNote(note);
                }
            }, new RestRequest.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    AppLog.e(AppLog.T.NOTIFS, "Could not mark note as read via API.");
                }
            });
        }
    }

    public static void downloadNoteAndUpdateDB(final String noteID, final RestRequest.Listener respoListener,
                                               final RestRequest.ErrorListener errorListener) {
        WordPress.getRestClientUtilsV1_1().getNotification(
                noteID,
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (response == null) {
                            // Not sure this could ever happen, but make sure we're catching all response types
                            AppLog.w(AppLog.T.NOTIFS, "Success, but did not receive any notes");
                        }
                        try {
                            List<Note> notes = NotificationsActions.parseNotes(response);
                            if (notes.size() > 0) {
                                NotificationsTable.saveNote(notes.get(0));
                                EventBus.getDefault()
                                        .post(new NotificationEvents.NotificationsChanged(notes.get(0).isUnread()));
                            } else {
                                AppLog.e(AppLog.T.NOTIFS, "Success, but no note!!!???");
                            }
                        } catch (JSONException e) {
                            AppLog.e(AppLog.T.NOTIFS, "Success, but can't parse the response for the note_id " + noteID,
                                     e);
                        }
                        if (respoListener != null) {
                            respoListener.onResponse(response);
                        }
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(AppLog.T.NOTIFS, "Error retrieving note with ID " + noteID, error);
                        if (errorListener != null) {
                            errorListener.onErrorResponse(error);
                        }
                    }
                });
    }
}
