package org.wordpress.android.database

import android.content.Context
import android.test.InstrumentationTestCase
import android.test.RenamingDelegatingContext
import org.wordpress.android.WordPressDB

class WordPressDBTest : InstrumentationTestCase() {
    private lateinit var mTestContext: Context
    private lateinit var mTargetContext: Context

    override fun setUp() {
        // Run tests in an isolated context
        mTargetContext = RenamingDelegatingContext(instrumentation.targetContext, "test_")
        mTestContext = instrumentation.context
    }

    fun testDatabaseCreation() {
        // This should run all migrations, and fail only if an exception is raised.
        // For instance, when the SQL statement is invalid
        WordPressDB(mTargetContext)
    }
}
