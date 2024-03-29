package org.wordpress.android.rules

import android.util.Log
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

const val TAG = "RetryTestRule"

/**
 * Custom rule used to retry running a test if a problem occurs.
 */
class RetryTestRule : TestRule {
    override fun apply(base: Statement?, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                // We only retry functions that are annotated with @Retry.
                val retry = description?.getAnnotation(Retry::class.java)
                if (retry != null) {
                    var lastThrown: Throwable? = null
                    for (i in 0..retry.numberOfTimes) {
                        try {
                            base?.evaluate()
                            return
                        } catch (t: Throwable) {
                            Log.e(TAG, "Test failed to run due to problem on run $i", t)
                            lastThrown = t
                        }
                    }
                    Log.e(TAG, "Could not pass test.")
                    if (lastThrown != null) {
                        throw lastThrown
                    }
                }
                // If test function does not have @Retry, run as normal.
                base?.evaluate()
            }
        }
    }
}
