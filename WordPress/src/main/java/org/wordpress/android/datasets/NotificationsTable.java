package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Note;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.List;

public class NotificationsTable {
    private static final String NOTIFICATIONS_TABLE = "tbl_notifications";

    private static SQLiteDatabase getDb() {
        return WordPress.wpDB.getDatabase();
    }

    public static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + NOTIFICATIONS_TABLE + " ("
                + "id                       INTEGER PRIMARY KEY DEFAULT 0,"
                + "note_id                  TEXT,"
                + "type                     TEXT,"
                + "raw_note_data            TEXT,"
                + "timestamp                INTEGER)");
    }

    private static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + NOTIFICATIONS_TABLE);
    }

    public static ArrayList<Note> getLatestNotes() {
        return getLatestNotes(20);
    }

    public static ArrayList<Note> getLatestNotes(int limit) {
        Cursor cursor = getDb().query(NOTIFICATIONS_TABLE, new String[] {"note_id", "raw_note_data"},
                null, null, null, null, "timestamp DESC", "" + limit);
        ArrayList<Note> notes = new ArrayList<Note>();
        while (cursor.moveToNext()) {
            String note_id = cursor.getString(0);
            String raw_note_data = cursor.getString(1);
            try {
                Note note = new Note(new JSONObject(raw_note_data));
                notes.add(note);
            } catch (JSONException e) {
                AppLog.e(AppLog.T.DB, "Can't parse notification with note_id:" + note_id + ", exception:" + e);
            }
        }
        cursor.close();
        return notes;
    }

    public static void putNote(Note note) {
        ContentValues values = new ContentValues();
        values.put("type", note.getType());
        values.put("timestamp", note.getTimestamp());
        values.put("raw_note_data", note.getJSON().toString()); // easiest way to store schema-less data
        values.put("note_id", note.getId());

        getDb().insertWithOnConflict(NOTIFICATIONS_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static void saveNotes(List<Note> notes, boolean clearBeforeSaving) {
        getDb().beginTransaction();
        try {
            if (clearBeforeSaving) {
                clearNotes();
            }

            for (Note note: notes) {
                putNote(note);
            }

            getDb().setTransactionSuccessful();
        } finally {
            getDb().endTransaction();
        }
    }

    public static Note getNoteById(String id) {
        Cursor cursor = getDb().query(NOTIFICATIONS_TABLE, new String[] {"raw_note_data"},  "note_id=" + id, null, null, null, null);
        cursor.moveToFirst();

        try {
            JSONObject jsonNote = new JSONObject(cursor.getString(0));
            return new Note(jsonNote);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.DB, "Can't parse JSON Note: " + e);
            return null;
        } catch (CursorIndexOutOfBoundsException e) {
            AppLog.v(AppLog.T.DB, "No Note with this id: " + e);
            return null;
        } finally {
            cursor.close();
        }
    }

    protected static void clearNotes() {
        getDb().delete(NOTIFICATIONS_TABLE, null, null);
    }
}
