package org.wordpress.android;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;

import static junit.framework.Assert.*;

public class TestUtils {
    private static String DATABASE_NAME = "wordpress";

    public static SQLiteDatabase loadDBFromDump(Context targetContext, Context testContext, String filename) {
        WordPress.wpDB = new WordPressDB(targetContext);

        Field dbField;
        try {
            dbField = WordPressDB.class.getDeclaredField("db");
            dbField.setAccessible(true);
            SQLiteDatabase db = (SQLiteDatabase) dbField.get(WordPress.wpDB);
            assertNotNull(db);

            // delete and recreate DB
            targetContext.deleteDatabase(DATABASE_NAME);
            targetContext.openOrCreateDatabase(DATABASE_NAME, 0, null);

            // Load file
            InputStream is = testContext.getAssets().open(filename);
            InputStreamReader inputStreamReader = new InputStreamReader(is);
            BufferedReader f = new BufferedReader(inputStreamReader);
            for (String line = f.readLine(); line != null; line = f.readLine()) {
                try {
                    db.execSQL(line);
                } catch (android.database.sqlite.SQLiteException e ) {
                    // ignore import errors
                }
            }
            f.close();
            return db;
        } catch (NoSuchFieldException e) {
            assertTrue(e.toString(), false);
        } catch (IllegalAccessException e) {
            assertTrue(e.toString(), false);
        } catch (IOException e) {
            assertTrue(e.toString(), false);
        }
        return null;
    }
}
