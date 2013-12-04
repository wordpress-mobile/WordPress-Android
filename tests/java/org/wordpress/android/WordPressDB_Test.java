package org.wordpress;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;
import android.util.Log;
import org.wordpress.android.TestUtils;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.CategoryNode;

public class WordPressDB_Test extends InstrumentationTestCase {
    protected Context testContext;
    protected Context targetContext;

    @Override
    protected void setUp() {
        // Run tests in an isolated context
        targetContext = new RenamingDelegatingContext(getInstrumentation().getTargetContext(),
                "test_");
        testContext = getInstrumentation().getContext();
        SQLiteDatabase db = TestUtils.loadDBFromDump(targetContext, testContext,
                "string-escaping-test.sql");
    }

    // This test failed before fixing #387
    public void testEscaping() {
        SQLiteDatabase db = TestUtils.loadDBFromDump(targetContext, testContext,
                "string-escaping-test.sql");
        String blogName = "say: \"pouet\"";
        String blogURL = "pouet.com";
        String username = "pouet";
        String password = "";
        boolean blogExists = WordPress.wpDB.checkForExistingBlog(blogName, blogURL, username,
                password);
        assertTrue(blogExists);
        db.close();
    }
}
