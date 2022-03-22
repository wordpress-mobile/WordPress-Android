package org.wordpress.android

import androidx.multidex.MultiDexApplication
import org.wordpress.android.AppInitializer.StoryNotificationTrackerProvider
import org.wordpress.android.modules.AppComponent

/**
 * An abstract class to be extended by {@link WordPress} for real application and WordPressTest for UI test application.
 * Containing public static variables and methods to be accessed by other classes.
 */
abstract class BaseWordPress : MultiDexApplication() {
    val storyNotificationTrackerProvider: StoryNotificationTrackerProvider
        get() = initializer().storyNotificationTrackerProvider

    protected lateinit var appComponent: AppComponent

    abstract fun initializer(): AppInitializer

    fun wordPressComSignOut() {
        initializer().wordPressComSignOut()
    }
}
