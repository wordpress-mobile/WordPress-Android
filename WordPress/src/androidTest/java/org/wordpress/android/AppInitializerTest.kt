package org.wordpress.android

import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertFalse
import org.junit.Test
import org.wordpress.android.AppInitializer.ApplicationLifecycleMonitor
import org.wordpress.android.support.BaseTest

/**
 * This tests if [ProcessLifecycleOwner] observer works at the app startup.
 *
 * [WordPress.appIsInTheBackground] is set to false when [ApplicationLifecycleMonitor.onAppComesFromBackground] is
 * called.
 */
@HiltAndroidTest
class AppInitializerTest : BaseTest() {
    @Test
    fun verifyOnAppComesFromBackgroundCalled() {
        Thread.sleep(200)
        assertFalse(WordPress.appIsInTheBackground)
    }
}
