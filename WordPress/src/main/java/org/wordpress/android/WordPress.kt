package org.wordpress.android

import android.app.Activity
import android.app.Application
import android.text.TextUtils
import androidx.multidex.MultiDexApplication
import com.android.volley.RequestQueue
import dagger.hilt.EntryPoints
import org.wordpress.android.AppInitializer.StoryNotificationTrackerProvider
import org.wordpress.android.fluxc.tools.FluxCImageLoader
import org.wordpress.android.modules.AppComponent
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.NUX

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
        fun updateContextLocale() {
            AppInitializer.updateContextLocale()
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

        /**
         * Gets a field from the project's BuildConfig using reflection. This is useful when flavors are used at the
         * project level to set custom fields.
         * based on: https://code.google.com/p/android/issues/detail?id=52962#c38
         *
         * @param application Used to find the correct file
         * @param fieldName The name of the field-to-access
         * @return The value of the field, or `null` if the field is not found.
         */
        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        fun getBuildConfigValue(application: Application, fieldName: String?): Any? {
            return try {
                val packageName = application.javaClass.getPackage().name
                val clazz = Class.forName("$packageName.BuildConfig")
                val field = clazz.getField(fieldName)
                field[null]
            } catch (e: LinkageError) {
                null
            } catch (e: ExceptionInInitializerError) {
                null
            } catch (e: ClassNotFoundException) {
                null
            } catch (e: NoSuchFieldException) {
                null
            } catch (e: NullPointerException) {
                null
            }
        }

        /**
         * Gets a field from the project's BuildConfig using reflection. This is useful when flavors are used at the
         * project level to set custom fields.
         * based on: https://code.google.com/p/android/issues/detail?id=52962#c38
         *
         * @param activity Used to get the Application instance
         * @param configValueName The name of the field-to-access
         * @return The string value of the field, or empty string if the field is not found.
         */
        fun getBuildConfigString(activity: Activity, configValueName: String): String? {
            return if (!BuildConfig.DEBUG) {
                ""
            } else {
                val value = getBuildConfigValue(activity.application, configValueName) as String?
                if (!TextUtils.isEmpty(value)) {
                    AppLog.d(NUX, "Auto-filled from build config: $configValueName")
                    value
                } else {
                    ""
                }
            }
        }
    }
}
