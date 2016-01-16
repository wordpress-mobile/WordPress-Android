package org.wordpress.android.models;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;
import android.test.mock.MockCursor;

import org.wordpress.android.TestUtils;

import static org.wordpress.android.models.MenuLocationModel.*;

public class MenuLocationModelTest extends InstrumentationTestCase {
    protected Context mTestContext;
    protected Context mTargetContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Context targetContext = getInstrumentation().getTargetContext();
        mTargetContext = new RenamingDelegatingContext(targetContext, CONTEXT_RENAME_PREFIX);
        mTestContext = getInstrumentation().getContext();
        TestUtils.loadDBFromDump(mTargetContext, mTestContext, DB_FILE_NAME);
    }

    public void testEqualsWithNull() {
        //noinspection ObjectEqualsNull
        assertFalse(getTestLocation().equals(null));
    }

    public void testEqualsWithSameLocation() {
        assertTrue(getTestLocation().equals(getTestLocation()));
    }

    public void testEqualsWithDifferentLocation() {
        final MenuLocationModel staticLocation = getTestLocation();
        MenuLocationModel testLocation = getTestLocation();
        testLocation.locationId = -1;
        assertFalse(testLocation.equals(staticLocation));
        testLocation.locationId = staticLocation.locationId;
        testLocation.name = null;
        assertFalse(testLocation.equals(staticLocation));
        testLocation.name = staticLocation.name;
        testLocation.details = null;
        assertFalse(testLocation.equals(staticLocation));
        testLocation.details = staticLocation.details;
        testLocation.defaultState = null;
        assertFalse(testLocation.equals(staticLocation));
        testLocation.defaultState = staticLocation.defaultState;
        testLocation.menuId = -1;
        assertFalse(testLocation.equals(staticLocation));
    }

    public void testSerialize() {
        MenuLocationModel testLocation = getTestLocation();
        ContentValues values = testLocation.serializeToDatabase();
        assertEquals(TEST_ID, values.getAsLong(ID_COLUMN_NAME).longValue());
        assertEquals(TEST_NAME, values.getAsString(NAME_COLUMN_NAME));
        assertEquals(TEST_DETAILS, values.getAsString(DETAILS_COLUMN_NAME));
        assertEquals(TEST_DEFAULT_STATE, values.getAsString(DEFAULT_STATE_COLUMN_NAME));
        assertEquals(TEST_MENU, values.getAsLong(MENU_COLUMN_NAME).longValue());
    }

    public void testDeserialize() {
        MenuLocationModel testLocation = new MenuLocationModel();
        Cursor testCursor = new TestCursor();
        testLocation.deserializeFromDatabase(testCursor);
        testCursor.close();
        assertEquals(TEST_ID, testLocation.locationId);
        assertEquals(TEST_NAME, testLocation.name);
        assertEquals(TEST_DETAILS, testLocation.details);
        assertEquals(TEST_DEFAULT_STATE, testLocation.defaultState);
        assertEquals(TEST_MENU, testLocation.menuId);
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
                case 4:
                    return TEST_MENU;
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
                    return TEST_DEFAULT_STATE;
                default:
                    return "";
            }
        }

        @Override
        public int getColumnIndex(String columnName) {
            switch (columnName) {
                case ID_COLUMN_NAME:
                    return 0;
                case NAME_COLUMN_NAME:
                    return 1;
                case DETAILS_COLUMN_NAME:
                    return 2;
                case DEFAULT_STATE_COLUMN_NAME:
                    return 3;
                case MENU_COLUMN_NAME:
                    return 4;
                default:
                    return -1;
            }
        }
    }

    private MenuLocationModel getTestLocation() {
        MenuLocationModel testLocation = new MenuLocationModel();
        testLocation.locationId = TEST_ID;
        testLocation.name = TEST_NAME;
        testLocation.details = TEST_DETAILS;
        testLocation.defaultState = TEST_DEFAULT_STATE;
        testLocation.menuId = TEST_MENU;
        return testLocation;
    }

    private static final String CONTEXT_RENAME_PREFIX = "test_";
    private static final String DB_FILE_NAME          = "taliwutt-blogs-sample.sql";

    private static final long   TEST_ID               = 1;
    private static final String TEST_NAME             = "MenuLocationTestName";
    private static final String TEST_DETAILS          = "MenuLocationTestDetails";
    private static final String TEST_DEFAULT_STATE    = "MenuLocationTestDefaultState";
    private static final long   TEST_MENU             = Long.MAX_VALUE;
}
