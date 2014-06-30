package org.wordpress.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.gson.internal.StringMap;

import org.wordpress.android.util.DateTimeUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

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

    public static void clearApplicationState(Context context) {
        WordPress.currentBlog = null;
        if (WordPress.getContext() != null) {
            WordPress.signOut(context);
        }
        TestUtils.clearDefaultSharedPreferences(context);
        TestUtils.dropDB(context);
    }

    public static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static HashMap<String, Object> stringMapToHashMap(StringMap<?> stringMap) {
        HashMap<String, Object> res = new HashMap<String, Object>();
        for (String key : stringMap.keySet()) {
            Object value = stringMap.get(key);
            if (StringMap.class.isInstance(value)) {
                HashMap<String, Object> newValue = stringMapToHashMap((StringMap<?>) value);
                res.put(key, newValue);
            } else {
                res.put(key, value);
            }
        }
        return res;
    }

    public static Date gsonStringToJavaDate(final String strDate) {
        try {
            SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.ENGLISH);
            return df.parse(strDate);
        } catch (ParseException e) {
            return null;
        }
    }

    public static Date parseStringToDate(String value) {
        // try do parseit as a Date
        Date newValue = DateTimeUtils.iso8601ToJavaDate(value);
        if (newValue != null) {
            return newValue;
        }
        newValue = gsonStringToJavaDate(value);
        if (newValue != null) {
            return newValue;
        }
        return null;
    }

    public static Object castIt(Object value) {
        if (value instanceof HashMap) {
            return injectDateInHashMap((HashMap<String, Object>) value);
        } else if (value instanceof String) {
            Date newValue = parseStringToDate((String) value);
            if (newValue != null) {
                return newValue;
            } else {
                return value;
            }
        } else if (value instanceof Double) {
            return (int) Math.round((Double) value);
        } else if (value instanceof Object[]) {
            return injectDateInArray((Object[]) value);
        } else if (value instanceof StringMap) {
            return injectDateInHashMap(stringMapToHashMap((StringMap) value));
        }
        return value;
    }

    public static Object[] injectDateInArray(Object[] array) {
        HashSet<Object> res = new HashSet<Object>();
        for (Object value : array) {
            res.add(castIt(value));
        }
        return res.toArray();
    }

    public static HashMap<String, Object> injectDateInHashMap(HashMap<String, Object> hashMap) {
        HashMap<String, Object> res = new HashMap<String, Object>();
        for (String key : hashMap.keySet()) {
            Object value = hashMap.get(key);
            res.put(key, castIt(value));
        }
        return res;
    }
}
