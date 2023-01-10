package org.wordpress.android

import android.content.Context
import androidx.multidex.MultiDexApplication
import com.android.volley.RequestQueue
import dagger.hilt.EntryPoints
import kotlinx.coroutines.MainScope
import org.wordpress.android.AppInitializer.StoryNotificationTrackerProvider
import org.wordpress.android.fluxc.tools.FluxCImageLoader
import org.wordpress.android.modules.AppComponent

/**
 * An abstract class to be extended by {@link WordPressApp} for real application and WordPressTest for UI test
 * application. Containing public static variables and methods to be accessed by other classes.
 */
abstract class WordPress : MultiDexApplication() {
    val storyNotificationTrackerProvider: StoryNotificationTrackerProvider
        get() = initializer().storyNotificationTrackerProvider

    abstract fun initializer(): AppInitializer

    fun component(): AppComponent = EntryPoints.get(this, AppComponent::class.java)

    fun wordPressComSignOut() {
        initializer().wordPressComSignOut()
    }

    @Suppress("TooManyFunctions")
    companion object {
        const val SITE = "SITE"
        const val LOCAL_SITE_ID = "LOCAL_SITE_ID"
        const val REMOTE_SITE_ID = "REMOTE_SITE_ID"
        const val USER_AGENT_APPNAME = "wp-android"

        lateinit var versionName: String
        lateinit var wpDB: WordPressDB
        var appIsInTheBackground = true

        @JvmField
        var requestQueue: RequestQueue? = null

        @JvmField
        var imageLoader: FluxCImageLoader? = null

        @JvmStatic
        fun getBitmapCache() = AppInitializer.getBitmapCache()

        @JvmStatic
        fun getContext() = AppInitializer.context!!

        @JvmStatic
        @JvmOverloads
        fun updateContextLocale(appContext: Context? = null) {
            AppInitializer.updateContextLocale(appContext)
        }

        @JvmStatic
        fun getRestClientUtils() = AppInitializer.restClientUtils

        @Suppress("FunctionNaming")
        @JvmStatic
        fun getRestClientUtilsV1_1() = AppInitializer.restClientUtilsV1_1

        @Suppress("FunctionNaming")
        @JvmStatic
        fun getRestClientUtilsV1_2() = AppInitializer.restClientUtilsV1_2

        @Suppress("FunctionNaming")
        @JvmStatic
        fun getRestClientUtilsV1_3() = AppInitializer.restClientUtilsV1_3

        fun getRestClientUtilsV2() = AppInitializer.restClientUtilsV2

        fun getRestClientUtilsV0() = AppInitializer.restClientUtilsV0

        @JvmStatic
        fun getDefaultUserAgent() = AppInitializer.defaultUserAgent

        @JvmStatic
        fun getUserAgent() = AppInitializer.userAgent
    }
}
