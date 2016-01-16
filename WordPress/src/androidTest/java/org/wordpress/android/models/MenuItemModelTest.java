package org.wordpress.android.models;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;
import android.test.mock.MockCursor;

import org.wordpress.android.TestUtils;

import static org.wordpress.android.models.MenuItemModel.*;

public class MenuItemModelTest extends InstrumentationTestCase {
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
        assertFalse((new MenuItemModel()).equals(null));
    }

    public void testEqualsWithSameItem() {
        assertTrue(getTestItem().equals(getTestItem()));
    }

    public void testEqualsWithDifferentItem() {
        final MenuItemModel staticItem = getTestItem();
        MenuItemModel testItem = getTestItem();
        testItem.itemId = INVALID_ID;
        assertFalse(testItem.equals(staticItem));
        testItem.itemId = staticItem.itemId;
        testItem.menuId = INVALID_ID;
        assertFalse(testItem.equals(staticItem));
        testItem.menuId = staticItem.menuId;
        testItem.parentId = INVALID_ID;
        assertFalse(testItem.equals(staticItem));
        testItem.parentId = staticItem.parentId;
        testItem.contentId = INVALID_ID;
        assertFalse(testItem.equals(staticItem));
        testItem.contentId = staticItem.contentId;
        testItem.url = "";
        assertFalse(testItem.equals(staticItem));
        testItem.url = staticItem.url;
        testItem.name = "";
        assertFalse(testItem.equals(staticItem));
        testItem.name = staticItem.name;
        testItem.details = "";
        assertFalse(testItem.equals(staticItem));
        testItem.details = staticItem.details;
        testItem.linkTarget = "";
        assertFalse(testItem.equals(staticItem));
        testItem.linkTarget = staticItem.linkTarget;
        testItem.linkTitle = "";
        assertFalse(testItem.equals(staticItem));
        testItem.linkTitle = staticItem.linkTitle;
        testItem.type = "";
        assertFalse(testItem.equals(staticItem));
        testItem.type = staticItem.type;
        testItem.typeFamily = "";
        assertFalse(testItem.equals(staticItem));
        testItem.typeFamily = staticItem.typeFamily;
        testItem.typeLabel = "";
        assertFalse(testItem.equals(staticItem));
        testItem.typeLabel = staticItem.typeLabel;
        testItem.children = null;
        assertFalse(testItem.equals(staticItem));
        testItem.children = staticItem.children;
    }

    public void testDeserializeFromCursor() {
        Cursor testCursor = new TestCursor();
        MenuItemModel testItem = new MenuItemModel();
        testItem.deserializeFromDatabase(testCursor);
        testCursor.close();
        assertTrue(testItem.equals(getTestItem()));
    }

    public void testSerialize() {
        MenuItemModel testItem = getTestItem();
        ContentValues values = testItem.serializeToDatabase();
        assertEquals(TEST_ID, values.getAsLong(ID_COLUMN_NAME).longValue());
        assertEquals(TEST_MENU, values.getAsLong(MENU_ID_COLUMN_NAME).longValue());
        assertEquals(TEST_PARENT, values.getAsLong(PARENT_ID_COLUMN_NAME).longValue());
        assertEquals(TEST_CONTENT, values.getAsLong(CONTENT_ID_COLUMN_NAME).longValue());
        assertEquals(TEST_URL, values.getAsString(URL_COLUMN_NAME));
        assertEquals(TEST_NAME, values.getAsString(NAME_COLUMN_NAME));
        assertEquals(TEST_DETAILS, values.getAsString(DETAILS_COLUMN_NAME));
        assertEquals(TEST_TARGET, values.getAsString(LINK_TARGET_COLUMN_NAME));
        assertEquals(TEST_TITLE, values.getAsString(LINK_TITLE_COLUMN_NAME));
        assertEquals(TEST_TYPE, values.getAsString(TYPE_COLUMN_NAME));
        assertEquals(TEST_TYPE_FAMILY, values.getAsString(TYPE_FAMILY_COLUMN_NAME));
        assertEquals(TEST_TYPE_LABEL, values.getAsString(TYPE_LABEL_COLUMN_NAME));
        assertEquals(TEST_CHILDREN, values.getAsString(CHILDREN_COLUMN_NAME));
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
                case 1:
                    return TEST_MENU;
                case 2:
                    return TEST_PARENT;
                case 3:
                    return TEST_CONTENT;
                default:
                    return -1;
            }
        }

        @Override
        public String getString(int columnIndex) {
            switch (columnIndex) {
                case 4:
                    return TEST_URL;
                case 5:
                    return TEST_NAME;
                case 6:
                    return TEST_DETAILS;
                case 7:
                    return TEST_TARGET;
                case 8:
                    return TEST_TITLE;
                case 9:
                    return TEST_TYPE;
                case 10:
                    return TEST_TYPE_FAMILY;
                case 11:
                    return TEST_TYPE_LABEL;
                case 12:
                    return TEST_CHILDREN;
                default:
                    return "";
            }
        }

        @Override
        public int getColumnIndex(String columnName) {
            switch (columnName) {
                case ID_COLUMN_NAME:
                    return 0;
                case MENU_ID_COLUMN_NAME:
                    return 1;
                case PARENT_ID_COLUMN_NAME:
                    return 2;
                case CONTENT_ID_COLUMN_NAME:
                    return 3;
                case URL_COLUMN_NAME:
                    return 4;
                case NAME_COLUMN_NAME:
                    return 5;
                case DETAILS_COLUMN_NAME:
                    return 6;
                case LINK_TARGET_COLUMN_NAME:
                    return 7;
                case LINK_TITLE_COLUMN_NAME:
                    return 8;
                case TYPE_COLUMN_NAME:
                    return 9;
                case TYPE_FAMILY_COLUMN_NAME:
                    return 10;
                case TYPE_LABEL_COLUMN_NAME:
                    return 11;
                case CHILDREN_COLUMN_NAME:
                    return 12;
                default:
                    return -1;
            }
        }
    }

    private MenuItemModel getTestItem() {
        MenuItemModel testItem = new MenuItemModel();
        // can't assume TestCursor.deserializeFromDatabase(testCursor) works, so we set manually
        testItem.itemId = TEST_ID;
        testItem.menuId = TEST_MENU;
        testItem.parentId = TEST_PARENT;
        testItem.contentId = TEST_CONTENT;
        testItem.url = TEST_URL;
        testItem.name = TEST_NAME;
        testItem.details = TEST_DETAILS;
        testItem.linkTarget = TEST_TARGET;
        testItem.linkTitle = TEST_TITLE;
        testItem.type = TEST_TYPE;
        testItem.typeFamily = TEST_TYPE_FAMILY;
        testItem.typeLabel = TEST_TYPE_LABEL;
        testItem.setChildrenFromStringList(TEST_CHILDREN);
        return testItem;
    }

    private static final String CONTEXT_RENAME_PREFIX = "test_";
    private static final String DB_FILE_NAME          = "taliwutt-blogs-sample.sql";

    private static final long INVALID_ID = -1;
    private static final long TEST_ID = 123;
    private static final long TEST_MENU = 456;
    private static final long TEST_PARENT = 101;
    private static final long TEST_CONTENT = 789;
    private static final String TEST_URL = "MenuItemModelTest_url";
    private static final String TEST_NAME = "MenuItemModelTest_name";
    private static final String TEST_DETAILS = "MenuItemModelTest_details";
    private static final String TEST_TARGET = "MenuItemModelTest_linkTarget";
    private static final String TEST_TITLE = "MenuItemModelTest_linkTitle";
    private static final String TEST_TYPE = "MenuItemModelTest_public String type";
    private static final String TEST_TYPE_FAMILY = "MenuItemModelTest_typeFamily";
    private static final String TEST_TYPE_LABEL = "MenuItemModelTest_typeLabel";
    private static final String TEST_CHILDREN = "0,1,2,3,4,5,6,7,8,9";
}
