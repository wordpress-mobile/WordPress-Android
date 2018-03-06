package org.wordpress.android.database;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;

public class WordPressDBTest extends InstrumentationTestCase {
    protected Context mTestContext;
    protected Context mTargetContext;

    @Override
    protected void setUp() {
        // Run tests in an isolated context
        mTargetContext = new RenamingDelegatingContext(getInstrumentation().getTargetContext(), "test_");
        mTestContext = getInstrumentation().getContext();
    }
}
