package org.wordpress.android.database;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;

import org.wordpress.android.TestUtils;
import org.wordpress.android.datasets.MenusTable;
import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.models.MenuLocationModel;
import org.wordpress.android.models.MenuModel;

public class MenusTableTest extends InstrumentationTestCase {
    private static final String CONTEXT_RENAME_PREFIX = "test_";
    private static final String DB_FILE_NAME = "taliwutt-blogs-sample.sql";

    private static final long TEST_ID = 1;
    private static final String TEST_NAME = "MenusTableTestName";
    private static final String TEST_DETAILS = "MenusTableTestDetails";

    protected Context mTargetContext;
    protected Context mTestContext;

    @Override
    protected void setUp() throws Exception {
        Context targetContext = getInstrumentation().getTargetContext();
        mTargetContext = new RenamingDelegatingContext(targetContext, CONTEXT_RENAME_PREFIX);
        mTestContext = getInstrumentation().getContext();
        TestUtils.clearApplicationState(mTargetContext);
        TestUtils.resetEventBus();
        TestUtils.loadDBFromDump(mTargetContext, mTestContext, DB_FILE_NAME);
    }

    public void testUniqueTableNames() {
        assertFalse(MenuModel.MENUS_TABLE_NAME == MenuItemModel.MENU_ITEMS_TABLE_NAME);
        assertFalse(MenuModel.MENUS_TABLE_NAME == MenuLocationModel.MENU_LOCATIONS_TABLE_NAME);
        assertFalse(MenuItemModel.MENU_ITEMS_TABLE_NAME == MenuLocationModel.MENU_LOCATIONS_TABLE_NAME);
    }

    public void testInvalidMenu() {
        assertNull(MenusTable.getMenuForMenuId(null));
    }

    public void testInvalidMenuItem() {
        assertNull(MenusTable.getMenuItemForId(-1));
    }

    public void testSaveLoadMenuItem() {
        MenuItemModel testItem = getTestMenuItem();
        assertTrue(MenusTable.saveMenuItem(testItem));
        MenuItemModel savedItem = MenusTable.getMenuItemForId(testItem.itemId);
        assertNotNull(savedItem);
        assertEquals(testItem.itemId, savedItem.itemId);
        assertEquals(testItem.contentId, savedItem.contentId);
        assertEquals(testItem.details, savedItem.details);
        assertEquals(testItem.linkTarget, savedItem.linkTarget);
        assertEquals(testItem.linkTitle, savedItem.linkTitle);
        assertEquals(testItem.name, savedItem.name);
        assertEquals(testItem.type, savedItem.type);
        assertEquals(testItem.typeFamily, savedItem.typeFamily);
        assertEquals(testItem.typeLabel, savedItem.typeLabel);
        assertEquals(testItem.url, savedItem.url);
        assertEquals(testItem.menuId, TEST_ID);
        assertEquals(testItem.parentId, TEST_ID);
    }

    private MenuItemModel getTestMenuItem() {
        MenuItemModel testItem = new MenuItemModel();
        testItem.itemId = 0;
        testItem.contentId = 0;
        testItem.details = TEST_DETAILS;
        testItem.linkTarget = "";
        testItem.linkTitle = "";
        testItem.name = TEST_NAME;
        testItem.type = "";
        testItem.typeFamily = "";
        testItem.typeLabel = "";
        testItem.url = "";
        testItem.menuId = TEST_ID;
        testItem.parentId = TEST_ID;
        testItem.setChildrenFromStringList("111,112,113");
        return testItem;
    }
}
