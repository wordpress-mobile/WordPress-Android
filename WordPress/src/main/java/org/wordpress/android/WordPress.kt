package org.wordpress.android

import android.app.Application
import android.content.Context
import android.database.SQLException
import android.os.Build
import android.util.Log
import androidx.work.Configuration
import coil.decode.VideoFrameDecoder
import com.android.volley.RequestQueue
import dagger.hilt.EntryPoints
import okhttp3.OkHttpClient
import org.wordpress.android.fluxc.tools.FluxCImageLoader
import org.wordpress.android.modules.AppComponent
import org.wordpress.android.ui.main.EdgeToEdgeActivityLifecycleCallbacks
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

/**
 * An abstract class to be extended by {@link WordPressApp} for real application and WordPressTest for UI test
 * application. Containing public static variables and methods to be accessed by other classes.
 */
abstract class WordPress : Application(), coil.ImageLoaderFactory, Configuration.Provider {
    abstract fun initializer(): AppInitializer

    /**
     * WorkManager is initialized on demand (the default initializer is removed in the manifest) so that its
     * Room database is opened by whichever thread first needs it, off the main thread on normal process starts.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(component().wordPressWorkersFactory())
            .setJobSchedulerJobIdRange(WORK_MANAGER_ID_RANGE_MIN, WORK_MANAGER_ID_RANGE_MAX)
            .apply { if (BuildConfig.DEBUG) setMinimumLoggingLevel(Log.DEBUG) }
            .build()

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            registerActivityLifecycleCallbacks(
                EdgeToEdgeActivityLifecycleCallbacks()
            )
        }
    }

    fun component(): AppComponent = EntryPoints.get(this, AppComponent::class.java)

    fun wordPressComSignOut() {
        initializer().wordPressComSignOut()
    }

    /**
     * This returns a singleton Coil ImageLoader that's accessed with context.imageLoader
     */
    override fun newImageLoader(): coil.ImageLoader {
        val authUtils = component().authenticationUtils()
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val authHeaders = authUtils.getAuthHeaders(request.url.toString())
                val newRequest = request.newBuilder().apply {
                    authHeaders.forEach { (name, value) -> header(name, value) }
                }.build()
                chain.proceed(newRequest)
            }
            .build()
        return coil.ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }

    @Suppress("TooManyFunctions")
    companion object {
        const val SITE = "SITE"
        const val LOCAL_SITE_ID = "LOCAL_SITE_ID"
        const val REMOTE_SITE_ID = "REMOTE_SITE_ID"
        const val USER_AGENT_APPNAME = "wp-android"

        // Use service ids near the int max to avoid collisions with existing JobService ids
        // The minimum range size is 1000, but we can easily give 10000.
        private const val WORK_MANAGER_ID_RANGE_MAX = Int.MAX_VALUE
        private const val WORK_MANAGER_ID_RANGE_MIN = WORK_MANAGER_ID_RANGE_MAX - 10000

        lateinit var versionName: String
        var appIsInTheBackground = true

        /**
         * The legacy app database. Opened lazily (and warmed up off the main thread by [AppInitializer]) so
         * that background process starts don't run the SQLite open + migrations on the main thread.
         */
        @JvmStatic
        val wpDB: WordPressDB by lazy { openWpDb(getContext()) }

        private fun openWpDb(context: Context): WordPressDB {
            return try {
                WordPressDB(context)
            } catch (e: SQLException) {
                AppLog.e(T.DB, "Invalid database, deleting and recreating it", e)
                WordPressDB.deleteDatabase(context)
                WordPressDB(context)
            }
        }

        @JvmField
        var requestQueue: RequestQueue? = null

        @JvmField
        var imageLoader: FluxCImageLoader? = null

        @JvmStatic
        fun getBitmapCache() = AppInitializer.getBitmapCache()

        @JvmStatic
        fun getContext() = AppInitializer.context!!

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
    }
}
