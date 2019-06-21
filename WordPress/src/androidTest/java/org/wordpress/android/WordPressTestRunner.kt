package org.wordpress.android

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Custom AndroidJUnitRunner that replaces the original application with [WordPressTest].
 */
class WordPressTestRunner : AndroidJUnitRunner() {
    override fun newApplication(classLoader: ClassLoader, className: String, context: Context): Application {
        return super.newApplication(classLoader, WordPressTest::class.java.name, context)
    }
}
