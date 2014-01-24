package org.wordpress.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.gson.internal.StringMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.HashMap;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

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
                if (TextUtils.isEmpty(line)) continue;
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

    public static void clearDefaultSharedPreferences(Context targetContext) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(targetContext);
        Editor editor = settings.edit();
        editor.clear();
        editor.commit();
    }

    public static void dropDB(Context targetContext) {
        targetContext.deleteDatabase(DATABASE_NAME);
    }

    public static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static HashMap stringMapToHashMap(StringMap<?> stringMap) {
        HashMap<String, Object> res = new HashMap<String, Object>();
        for (String key : stringMap.keySet()) {
            Object value = stringMap.get(key);
            if (StringMap.class.isInstance(value)) {
                HashMap newValue = stringMapToHashMap((StringMap<?>) value);
                res.put(key, newValue);
            } else {
                res.put(key, value);
            }
        }
        return res;
    }
}
