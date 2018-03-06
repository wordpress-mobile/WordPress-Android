package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Note;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SqlUtils;

import java.util.ArrayList;
import java.util.List;

public class NotificationsTable {
    private static final String NOTIFICATIONS_TABLE = "tbl_notifications";

    private static SQLiteDatabase getDb() {
        return WordPress.wpDB.getDatabase();
    }

    public static final int NOTES_TO_RETRIEVE = 200;

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
        ContentValues values = new ContentValues();
        values.put("type", note.getType());
        values.put("timestamp", note.getTimestamp());
        values.put("raw_note_data", note.getJSON().toString());

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

    public static void saveNotes(List<Note> notes, boolean clearBeforeSaving) {
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

    public static boolean saveNote(Note note) {
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

    public static Note getNoteById(String noteID) {
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
