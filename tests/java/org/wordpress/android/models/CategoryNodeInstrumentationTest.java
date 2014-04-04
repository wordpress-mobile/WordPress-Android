package org.wordpress.android.models;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;

import org.wordpress.android.TestUtils;

public class CategoryNodeInstrumentationTest extends InstrumentationTestCase {
    protected Context testContext;
    protected Context targetContext;

    @Override
    protected void setUp() {
        // Run tests in an isolated context
        targetContext = new RenamingDelegatingContext(getInstrumentation().getTargetContext(), "test_");
        testContext = getInstrumentation().getContext();
    }

    public void testLoadDB_MalformedCategoryParentId() {
        SQLiteDatabase db = TestUtils.loadDBFromDump(targetContext, testContext,
                "malformed_category_parent_id.sql");

        // This line failed before #36 was solved
        CategoryNode node = CategoryNode.createCategoryTreeFromDB(1);
    }

    public void tearDown() throws Exception {
        targetContext = null;
        testContext = null;
        super.tearDown();
    }
}
