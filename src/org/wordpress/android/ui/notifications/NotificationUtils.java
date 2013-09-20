package org.wordpress.android.ui.notifications;

import android.util.Log;
import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Note;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.InflaterInputStream;

public class NotificationUtils {
    public static void refreshNotifications(final RestRequest.Listener listener,
                                            final RestRequest.ErrorListener errorListener) {
        WordPress.restClient.getNotifications(
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (listener != null) {
                            listener.onResponse(response);
                        }
                    }
                },
                new RestRequest.ErrorListener() {
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
        notes = new ArrayList<Note>(notesJSON.length());
        for (int i = 0; i < notesJSON.length(); i++) {
            Note n = new Note(notesJSON.getJSONObject(i));
            notes.add(n);
        }
        return notes;
    }

    public static String unzipString(byte[] zbytes) {
        String unzipped = null;
        try {
            // Add extra byte to array when Inflater is set to true
            byte[] input = new byte[zbytes.length + 1];
            System.arraycopy(zbytes, 0, input, 0, zbytes.length);
            input[zbytes.length] = 0;
            ByteArrayInputStream bin = new ByteArrayInputStream(input);
            InflaterInputStream in = new InflaterInputStream(bin);
            ByteArrayOutputStream bout = new ByteArrayOutputStream(512);
            int b;
            while ((b = in.read()) != -1) {
                bout.write(b);
            }
            bout.close();
            unzipped = bout.toString();
        } catch (IOException io) {
            Log.e(WordPress.TAG, "Unzipping failed: " + io);
            return null;
        }
        return unzipped;
    }
}
