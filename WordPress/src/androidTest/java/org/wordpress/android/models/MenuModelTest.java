package org.wordpress.android.models;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;
import android.test.mock.MockCursor;

import org.wordpress.android.TestUtils;
import org.wordpress.android.util.DatabaseUtils;

public class MenuModelTest extends InstrumentationTestCase {
    protected Context mTestContext;
    protected Context mTargetContext;
    protected SQLiteDatabase mDb;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Context targetContext = getInstrumentation().getTargetContext();
        mTargetContext = new RenamingDelegatingContext(targetContext, CONTEXT_RENAME_PREFIX);
        mTestContext = getInstrumentation().getContext();
        mDb = TestUtils.loadDBFromDump(mTargetContext, mTestContext, DB_FILE_NAME);
    }

    public void testEqualsWithNull() {
        //noinspection ObjectEqualsNull
        assertFalse(getTestMenu().equals(null));
    }

    public void testEqualsWithSameMenu() {
        assertTrue(getTestMenu().equals(getTestMenu()));
    }

    public void testEqualsWithDifferentMenu() {
        MenuModel staticMenu = getTestMenu();
        MenuModel testMenu = getTestMenu();
        testMenu.name = null;
        assertFalse(testMenu.equals(staticMenu));
        testMenu.name = staticMenu.name;
        testMenu.menuId = -1;
        assertFalse(testMenu.equals(staticMenu));
        testMenu.menuId = staticMenu.menuId;
        testMenu.details = null;
        assertFalse(testMenu.equals(staticMenu));
        testMenu.details = staticMenu.details;
        testMenu.locations = null;
        assertFalse(testMenu.equals(staticMenu));
        testMenu.locations = staticMenu.locations;
        testMenu.menuItems = null;
        assertFalse(testMenu.equals(staticMenu));
    }

    public void testSerialize() {
        MenuModel testMenu = getTestMenu();
        ContentValues values = testMenu.serializeToDatabase();
        assertEquals(TEST_ID, values.getAsLong(MenuModel.ID_COLUMN_NAME).longValue());
        assertEquals(TEST_NAME, values.getAsString(MenuModel.NAME_COLUMN_NAME));
        assertEquals(TEST_DETAILS, values.getAsString(MenuModel.DETAILS_COLUMN_NAME));
        assertEquals(TEST_LOCATIONS, values.getAsString(MenuModel.LOCATIONS_COLUMN_NAME));
        assertEquals(TEST_ITEMS, values.getAsString(MenuModel.ITEMS_COLUMN_NAME));
    }

    public void testDeserialize() {
        Cursor testCursor = new TestCursor();
        MenuModel testMenu = new MenuModel();
        testMenu.deserializeFromDatabase(testCursor);
        testCursor.close();
        assertEquals(TEST_ID, testMenu.menuId);
        assertEquals(TEST_NAME, testMenu.name);
        assertEquals(TEST_DETAILS, testMenu.details);
        assertEquals(TEST_LOCATIONS, DatabaseUtils.separatedStringList(testMenu.locations, ","));
        assertEquals(TEST_ITEMS, DatabaseUtils.separatedStringList(testMenu.menuItems, ","));
    }

    private class TestCursor extends MockCursor {
        @Override public int getCount() { return 1; }

        @Override public boolean moveToFirst() { return true; }

        @Override public boolean isBeforeFirst() { return false; }

        @Override public boolean isAfterLast() { return false; }

        @Override public void close() {}

        @Override
        public long getLong(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return TEST_ID;
                default:
                    return -1;
            }
        }

        @Override
        public String getString(int columnIndex) {
            switch (columnIndex) {
                case 1:
                    return TEST_NAME;
                case 2:
                    return TEST_DETAILS;
                case 3:
                    return TEST_LOCATIONS;
                case 4:
                    return TEST_ITEMS;
                default:
                    return "";
            }
        }

        @Override
        public int getColumnIndex(String columnName) {
            switch (columnName) {
                case MenuModel.ID_COLUMN_NAME:
                    return 0;
                case MenuModel.NAME_COLUMN_NAME:
                    return 1;
                case MenuModel.DETAILS_COLUMN_NAME:
                    return 2;
                case MenuModel.LOCATIONS_COLUMN_NAME:
                    return 3;
                case MenuModel.ITEMS_COLUMN_NAME:
                    return 4;
                default:
                    return -1;
            }
        }
    }

    private MenuModel getTestMenu() {
        MenuModel testModel = new MenuModel();
        testModel.menuId = TEST_ID;
        testModel.name = TEST_NAME;
        testModel.details = TEST_DETAILS;
        testModel.setLocationsFromStringList(TEST_LOCATIONS);
        testModel.setItemsFromStringList(TEST_ITEMS);
        return testModel;
    }

    private static final String CONTEXT_RENAME_PREFIX = "test_";
    private static final String DB_FILE_NAME = "taliwutt-blogs-sample.sql";

    private static final long TEST_ID = Long.MAX_VALUE;
    private static final String TEST_NAME = "MenuModelTestName";
    private static final String TEST_DETAILS = "MenuModelTestDetails";
    private static final String TEST_LOCATIONS = "0,1,2,3,4";
    private static final String TEST_ITEMS = "0,1,2,3,4";
}
