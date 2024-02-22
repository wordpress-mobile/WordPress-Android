package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Note;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SqlUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationsTable {
    private static final String NOTIFICATIONS_TABLE = "tbl_notifications";

    private static SQLiteDatabase getDb() {
        return WordPress.wpDB.getDatabase();
    }

    public static final int NOTES_TO_RETRIEVE = 200;

    private static final Pattern STAT_ATTR_PATTERN = Pattern.compile(
            "\"type\":\"stat\"",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private static final Pattern REWIND_DOWNLOAD_READY_ATTR_PATTERN = Pattern.compile(
            "\"type\":\"rewind_download_ready\"",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final String REWIND_DOWNLOAD_READY_ATTR_SUBSTR = "\"type\":\"rewind_download_ready\"";

    public static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + NOTIFICATIONS_TABLE + " ("
                   + "id INTEGER PRIMARY KEY DEFAULT 0,"
                   + "note_id TEXT,"
                   + "type TEXT,"
                   + "raw_note_data TEXT,"
                   + "timestamp INTEGER,"
                   + " UNIQUE (note_id) ON CONFLICT REPLACE"
                   + ")");
    }

    private static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + NOTIFICATIONS_TABLE);
    }

    public static ArrayList<Note> getLatestNotes() {
        return getLatestNotes(NOTES_TO_RETRIEVE);
    }

    public static ArrayList<Note> getLatestNotes(int limit) {
        Cursor cursor = getDb().query(NOTIFICATIONS_TABLE, new String[]{"note_id", "raw_note_data"},
                                      null, null, null, null, "timestamp DESC", "" + limit);
        ArrayList<Note> notes = new ArrayList<Note>();
        while (cursor.moveToNext()) {
            String noteId = cursor.getString(0);
            String rawNoteData = cursor.getString(1);
            try {
                Note note = new Note(noteId, new JSONObject(rawNoteData));
                notes.add(note);
            } catch (JSONException e) {
                AppLog.e(AppLog.T.DB, "Can't parse notification with noteId:" + noteId + ", exception:" + e);
            }
        }
        cursor.close();
        return notes;
    }

    private static boolean putNote(Note note, boolean checkBeforeInsert) {
        String rawNote = prepareNote(note.getId(), note.getJson().toString());

        ContentValues values = new ContentValues();
        values.put("type", note.getRawType());
        values.put("timestamp", note.getTimestamp());
        values.put("raw_note_data", rawNote);

        long result;
        if (checkBeforeInsert && isNoteAvailable(note.getId())) {
            // Update
            String[] args = {note.getId()};
            result = getDb().update(
                    NOTIFICATIONS_TABLE,
                    values,
                    "note_id=?",
                    args);
            return result == 1;
        } else {
            // insert
            values.put("note_id", note.getId());
            result = getDb().insertWithOnConflict(NOTIFICATIONS_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            if (result == -1) {
                AppLog.e(AppLog.T.DB, "An error occurred while saving the note into the DB - note_id:" + note.getId());
            }
            return result != -1;
        }
    }

    /***
     * PrepareNote is used as a stop gap for handling rewind_download_ready notifications. As of this comment,
     * rewind download ready notifications have a deep link to stats and until the API changes, we are going to
     * swap "type""type":"stat" for "type""type":"rewind_download_ready" so that the generated link sends the
     * user to the correct location in the app. The source remains the same if this is not a rewind_download_ready note.
     * @param noteId
     * @param noteSrc
     * @return
     */
    private static String prepareNote(String noteId, String noteSrc) {
        final Matcher typeMatcher = REWIND_DOWNLOAD_READY_ATTR_PATTERN.matcher(noteSrc);
        if (typeMatcher.find()) {
            AppLog.d(AppLog.T.DB, "Substituting " + REWIND_DOWNLOAD_READY_ATTR_SUBSTR + " in NoteID: " + noteId);
            final Matcher matcher = STAT_ATTR_PATTERN.matcher(noteSrc);
            noteSrc = matcher.replaceAll(REWIND_DOWNLOAD_READY_ATTR_SUBSTR);
        }
        return noteSrc;
    }

    public static void saveNotes(@NonNull List<Note> notes, boolean clearBeforeSaving) {
        getDb().beginTransaction();
        try {
            if (clearBeforeSaving) {
                clearNotes();
            }

            for (Note note : notes) {
                // No need to check if the row already exists if we've just dropped the table.
                putNote(note, !clearBeforeSaving);
            }

            getDb().setTransactionSuccessful();
        } finally {
            getDb().endTransaction();
        }
    }

    public static boolean saveNote(@NonNull Note note) {
        getDb().beginTransaction();
        boolean saved = false;
        try {
            saved = putNote(note, true);
            getDb().setTransactionSuccessful();
        } finally {
            getDb().endTransaction();
        }
        return saved;
    }

    private static boolean isNoteAvailable(String noteID) {
        if (TextUtils.isEmpty(noteID)) {
            AppLog.e(AppLog.T.DB, "Asking for a note with null Id. Really?" + noteID);
            return false;
        }

        String[] args = {noteID};
        return SqlUtils.boolForQuery(getDb(),
                                     "SELECT 1 FROM " + NOTIFICATIONS_TABLE + " WHERE note_id=?1",
                                     args);
    }

    @Nullable
    public static Note getNoteById(@Nullable String noteID) {
        if (TextUtils.isEmpty(noteID)) {
            AppLog.e(AppLog.T.DB, "Asking for a note with null Id. Really?" + noteID);
            return null;
        }
        Cursor cursor =
                getDb().query(NOTIFICATIONS_TABLE, new String[]{"raw_note_data"}, "note_id=" + noteID, null, null, null,
                              null);
        try {
            if (cursor.moveToFirst()) {
                JSONObject jsonNote = new JSONObject(cursor.getString(0));
                return new Note(noteID, jsonNote);
            } else {
                AppLog.v(AppLog.T.DB, "No Note found in the DB with this id: " + noteID);
                return null;
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.DB, "Can't parse JSON Note: " + e);
            return null;
        } catch (CursorIndexOutOfBoundsException e) {
            AppLog.e(AppLog.T.DB, "An error with the cursor has occurred", e);
            return null;
        } finally {
            cursor.close();
        }
    }

    public static boolean deleteNoteById(String noteID) {
        if (TextUtils.isEmpty(noteID)) {
            AppLog.e(AppLog.T.DB, "Asking to delete a note with null Id. Really?" + noteID);
            return false;
        }
        getDb().beginTransaction();
        try {
            String[] args = {noteID};
            int result = getDb().delete(NOTIFICATIONS_TABLE, "note_id=?", args);
            getDb().setTransactionSuccessful();
            return result != 0;
        } finally {
            getDb().endTransaction();
        }
    }

    private static void clearNotes() {
        getDb().delete(NOTIFICATIONS_TABLE, null, null);
    }

    /*
     * drop & recreate notifications table
     */
    public static void reset() {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            dropTables(db);
            createTables(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
