package org.wordpress.android

import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.wordpress.android.support.BaseTest

@HiltAndroidTest
class AppInitializerTest : BaseTest() {
    // This tests if ProcessLifecycleOwner observer works at the app startup.
    @Test
    fun verifyOnAppComesFromBackgroundCalled() {
        // WordPress.appIsInTheBackground is set to false when OnAppComesFromBackground() is called.
        assert(!WordPress.appIsInTheBackground)
    }
}
