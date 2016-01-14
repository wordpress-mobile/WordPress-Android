package org.wordpress.android.models;

import android.content.ContentValues;
import android.content.Context;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;
import android.test.mock.MockCursor;

import org.wordpress.android.TestUtils;

import java.util.ArrayList;
import java.util.List;

public class MenuModelTest extends InstrumentationTestCase {
    private static final String CONTEXT_RENAME_PREFIX = "test_";
    private static final String DB_FILE_NAME = "taliwutt-blogs-sample.sql";

    private static final long TEST_ID = Long.MAX_VALUE;
    private static final String TEST_NAME = "MenuModelTestName";
    private static final String TEST_DETAILS = "MenuModelTestDetails";
    private static final String TEST_LOCATIONS = "TESTLOC0,TESTLOC1,TESTLOC2,TESTLOC3,TESTLOC4";
    private static final String TEST_ITEMS = "0,1,2,3,4";

    protected Context mTestContext;
    protected Context mTargetContext;

    @Override
    protected void setUp() throws Exception {
        Context targetContext = getInstrumentation().getTargetContext();
        mTargetContext = new RenamingDelegatingContext(targetContext, CONTEXT_RENAME_PREFIX);
        mTestContext = getInstrumentation().getContext();
        TestUtils.loadDBFromDump(mTargetContext, mTestContext, DB_FILE_NAME);
        super.setUp();
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
        MenuModel testMenu = MenuModel.fromDatabase(new TestCursor());
        assertEquals(TEST_ID, testMenu.menuId);
        assertEquals(TEST_NAME, testMenu.name);
        assertEquals(TEST_DETAILS, testMenu.details);
        assertEquals(TEST_LOCATIONS, testMenu.serializeMenuLocations());
        assertEquals(TEST_ITEMS, testMenu.serializeMenuItems());
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

    private class TestCursor extends MockCursor {
        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public boolean moveToFirst() {
            return true;
        }

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
        testModel.locations = getTestLocations();
        testModel.menuItems = getTestItems();
        return testModel;
    }

    private List<MenuLocationModel> getTestLocations() {
        List<MenuLocationModel> locations = new ArrayList<>();
        for (String name : TEST_LOCATIONS.split(",")) {
            locations.add(MenuLocationModel.fromName(name));
        }
        return locations;
    }

    private List<MenuItemModel> getTestItems() {
        List<MenuItemModel> items = new ArrayList<>();
        for (String id : TEST_ITEMS.split(",")) {
            items.add(MenuItemModel.fromItemId(Long.valueOf(id)));
        }
        return items;
    }
}
