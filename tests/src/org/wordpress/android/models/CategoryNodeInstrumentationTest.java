package org.wordpress.android.models;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;
import android.util.Log;
import org.wordpress.android.TestUtils;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.CategoryNode;

public class CategoryNodeInstrumentationTest extends InstrumentationTestCase {
    protected Context testContext;
    protected Context targetContext;

    @Override
    protected void setUp() {
        // Run tests in an isolated context
        targetContext = new RenamingDelegatingContext(getInstrumentation().getTargetContext(), "test_");
        testContext = getInstrumentation().getContext();
    }

    public void testLoadDB() {
        SQLiteDatabase db = TestUtils.loadDBFromDump(targetContext, testContext,
                "one_category.sql");
        CategoryNode node = CategoryNode.createCategoryTreeFromDB(1);
        // At least 1 category exists in test db: malformed_category_parent_id.sql
        assertTrue((node.getChildren().size() != 0));
        db.close();
    }

    public void testLoadDB_MalformedCategoryParentId() {
        SQLiteDatabase db = TestUtils.loadDBFromDump(targetContext, testContext,
                "malformed_category_parent_id.sql");

        // This line failed before #36 was solved
        CategoryNode node = CategoryNode.createCategoryTreeFromDB(1);
        db.close();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }
}
