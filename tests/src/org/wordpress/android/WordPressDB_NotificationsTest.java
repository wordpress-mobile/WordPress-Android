package org.wordpress.android.models;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;
import android.util.Log;
import org.json.JSONObject;
import org.wordpress.android.TestUtils;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.CategoryNode;

public class WordPressDB_NotificationsTest extends InstrumentationTestCase {
    protected Context testContext;
    protected Context targetContext;

    @Override
    protected void setUp() {
        // Run tests in an isolated context
        targetContext = new RenamingDelegatingContext(getInstrumentation().getTargetContext(), "test_");
        testContext = getInstrumentation().getContext();
    }

    // This test reproduces #134 (crash when not fixed)
    public void testAddNote_issue134() {
        SQLiteDatabase db = TestUtils.loadDBFromDump(targetContext, testContext, "empty_tables.sql");
        WordPressDB wpdb = new WordPressDB(targetContext);

        Bundle b = new Bundle();
        b.putString("title", "Hey");
        b.putString("msg", "Hoy");
        b.putString("type", "c");
        b.putString("icon", "");
        b.putString("noticon", "");
        b.putString("msg", "");
        b.putString("note_id", ""); // empty string note_id makes addNote() crash
        Note note = new Note(b);
        wpdb.addNote(note, true);

        db.close();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }
}
