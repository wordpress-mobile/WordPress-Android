@file:Suppress("DEPRECATION")

package org.wordpress.android

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.Application
import android.app.AsyncNotedAppOp
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.SyncNotedAppOp
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.IntentFilter
import android.content.res.Configuration
import android.database.SQLException
import android.database.sqlite.SQLiteException
import android.net.ConnectivityManager
import android.net.http.HttpResponseCache
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.SystemClock
import android.text.TextUtils
import android.util.Log
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager
import androidx.work.WorkManager
import com.android.volley.RequestQueue
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.iid.FirebaseInstanceId
import com.wordpress.rest.RestClient
import kotlinx.coroutines.CoroutineScope
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.Tracker
import org.wordpress.android.datasets.NotificationsTable
import org.wordpress.android.datasets.ReaderDatabase
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.ListActionBuilder
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.generated.ThemeActionBuilder
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.site.PrivateAtomicCookie
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.ListStore.RemoveExpiredListsPayload
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.fluxc.tools.FluxCImageLoader
import org.wordpress.android.fluxc.utils.ErrorUtils.OnUnexpectedError
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.networking.ConnectionChangeReceiver
import org.wordpress.android.networking.OAuthAuthenticator
import org.wordpress.android.networking.RestClientUtils
import org.wordpress.android.push.GCMRegistrationScheduler
import org.wordpress.android.support.ZendeskHelper
import org.wordpress.android.ui.ActivityId
import org.wordpress.android.ui.debug.cookies.DebugCookieManager
import org.wordpress.android.ui.deeplinks.DeepLinkOpenWebLinksWithJetpackHelper
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalWidgetHelper
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter
import org.wordpress.android.ui.notifications.utils.NotificationsUtils
import org.wordpress.android.ui.posts.editor.ImageEditorFileUtils
import org.wordpress.android.ui.posts.editor.ImageEditorInitializer
import org.wordpress.android.ui.posts.editor.ImageEditorTracker
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater.StatsWidgetUpdaters
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.uploads.UploadStarter
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.AppLog.T.MAIN
import org.wordpress.android.util.AppThemeUtils
import org.wordpress.android.util.BitmapLruCache
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.EncryptedLogging
import org.wordpress.android.util.FluxCUtils
import org.wordpress.android.util.LocaleManager
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.PackageUtils
import org.wordpress.android.util.ProfilingUtils
import org.wordpress.android.util.QuickStartUtils
import org.wordpress.android.util.RateLimitedTask
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.VolleyUtils
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.util.config.AppConfig
import org.wordpress.android.util.config.OpenWebLinksWithJetpackFlowFeatureConfig
import org.wordpress.android.util.enqueuePeriodicUploadWorkRequestForAllSites
import org.wordpress.android.util.experiments.ExPlat
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.widgets.AppReviewManager
import org.wordpress.android.workers.WordPressWorkersFactory
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.net.CookieManager
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AppInitializer @Inject constructor(
    wellSqlInitializer: WellSqlInitializer,
    private val application: Application
) : DefaultLifecycleObserver {
    @Inject
    lateinit var userAgent: UserAgent

    @Inject
    lateinit var dispatcher: Dispatcher

    @Inject
    lateinit var accountStore: AccountStore

    @Inject
    lateinit var siteStore: SiteStore

    @Inject
    lateinit var mediaStore: MediaStore

    @Inject
    lateinit var zendeskHelper: ZendeskHelper

    @Inject
    lateinit var uploadStarter: UploadStarter

    @Inject
    lateinit var statsWidgetUpdaters: StatsWidgetUpdaters

    @Inject
    lateinit var statsStore: StatsStore

    @Inject
    lateinit var systemNotificationsTracker: SystemNotificationsTracker

    @Inject
    lateinit var readerTracker: ReaderTracker

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var privateAtomicCookie: PrivateAtomicCookie

    @Inject
    lateinit var imageEditorTracker: ImageEditorTracker

    @Inject
    lateinit var crashLogging: CrashLogging

    @Inject
    lateinit var encryptedLogging: EncryptedLogging

    @Inject
    lateinit var appConfig: AppConfig

    @Inject
    lateinit var imageEditorFileUtils: ImageEditorFileUtils

    @Inject
    lateinit var exPlat: ExPlat

    @Inject
    lateinit var wordPressWorkerFactory: WordPressWorkersFactory

    @Inject
    lateinit var gcmRegistrationScheduler: GCMRegistrationScheduler

    @Inject
    lateinit var cookieManager: CookieManager

    @Inject
    lateinit var buildConfig: BuildConfigWrapper

    private lateinit var debugCookieManager: DebugCookieManager

    @Inject
    @Named(APPLICATION_SCOPE)
    lateinit var appScope: CoroutineScope

    @Inject
    lateinit var selectedSiteRepository: SelectedSiteRepository

    // For development and production `AnalyticsTrackerNosara`, for testing a mocked `Tracker` will be injected.
    @Inject
    lateinit var tracker: Tracker

    @Inject
    @Named("custom-ssl")
    lateinit var requestQueue: RequestQueue

    @Inject
    lateinit var imageLoader: FluxCImageLoader

    @Inject
    lateinit var oAuthAuthenticator: OAuthAuthenticator

    // For jetpack focus
    @Inject
    lateinit var openWebLinksWithJetpackFlowFeatureConfig: OpenWebLinksWithJetpackFlowFeatureConfig

    @Inject
    lateinit var openWebLinksWithJetpackHelper: DeepLinkOpenWebLinksWithJetpackHelper

    @Inject
    lateinit var jetpackFeatureRemovalWidgetHelper: JetpackFeatureRemovalWidgetHelper

    @Inject
    lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper

    private lateinit var applicationLifecycleMonitor: ApplicationLifecycleMonitor

    @Suppress("DEPRECATION")
    private lateinit var credentialsClient: GoogleApiClient

    private var startDate: Long

    /**
     * Update site list in a background task. (WPCOM site list, and eventually self hosted multisites)
     */
    var updateSiteList = object : RateLimitedTask(SECONDS_BETWEEN_BLOGLIST_UPDATE) {
        override fun run(): Boolean {
            if (accountStore.hasAccessToken()) {
                dispatcher.dispatch(SiteActionBuilder.newFetchSitesAction(SiteUtils.getFetchSitesPayload()))
                dispatcher.dispatch(AccountActionBuilder.newFetchSubscriptionsAction())
            }
            return true
        }
    }

    /**
     * Update site information in a background task.
     */
    var updateSelectedSite = object : RateLimitedTask(SECONDS_BETWEEN_SITE_UPDATE) {
        override fun run(): Boolean {
            val selectedSiteLocalId = selectedSiteRepository.getSelectedSiteLocalId(true)
            val site = siteStore.getSiteByLocalId(selectedSiteLocalId)
            site?.let {
                dispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(site))
                // Reload editor details from the remote backend
                if (!AppPrefs.isDefaultAppWideEditorPreferenceSet()) {
                    // Check if the migration from app-wide to per-site setting has already happened - v12.9->13.0
                    dispatcher.dispatch(SiteActionBuilder.newFetchSiteEditorsAction(site))
                }
            }
            return true
        }
    }

    init {
        context = application
        startDate = SystemClock.elapsedRealtime()

        if (!initialized) {
            // This call needs be made before accessing any methods in android.webkit package
            setWebViewDataDirectorySuffixOnAndroidP()
        }

        wellSqlInitializer.init()
    }

    fun init() {
        crashLogging.initialize()
        dispatcher.register(this)
        appConfig.init(appScope)
        // Upload any encrypted logs that were queued but not yet uploaded
        encryptedLogging.start()

        // Init static fields from dagger injected singletons, for legacy Actions and Utilities
        WordPress.requestQueue = requestQueue
        WordPress.imageLoader = imageLoader
        sOAuthAuthenticator = oAuthAuthenticator

        ProfilingUtils.start("App Startup")

        enableLogRecording()
        AppLog.i(T.UTILS, "AppInitializer.init")

        WordPress.versionName = PackageUtils.getVersionName(application)
        initWpDb()
        context?.let { enableHttpResponseCache(it) }

        AppReviewManager.init(application)

        if (!initialized) {
            // EventBus setup
            EventBus.TAG = "WordPress-EVENT"
            EventBus.builder()
                .logNoSubscriberMessages(false)
                .sendNoSubscriberEvent(false)
                .throwSubscriberException(true)
                .installDefaultEventBus()
        }

        RestClientUtils.setUserAgent(userAgent.toString())

        if (!initialized) {
            zendeskHelper.setupZendesk(
                application,
                BuildConfig.ZENDESK_DOMAIN,
                BuildConfig.ZENDESK_APP_ID,
                BuildConfig.ZENDESK_OAUTH_CLIENT_ID
            )
        }

        val memoryAndConfigChangeMonitor = MemoryAndConfigChangeMonitor()
        application.registerComponentCallbacks(memoryAndConfigChangeMonitor)

        // initialize our ApplicationLifecycleMonitor, which is the App's LifecycleObserver implementation
        applicationLifecycleMonitor = ApplicationLifecycleMonitor()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Make the UploadStarter observe the app process so it can auto-start uploads
        uploadStarter.activateAutoUploading(ProcessLifecycleOwner.get() as ProcessLifecycleOwner)

        initAnalytics(SystemClock.elapsedRealtime() - startDate)

        updateNotificationSettings()
        // Allows vector drawable from resources (in selectors for instance) on Android < 21 (can cause issues with
        // memory usage and the use of Configuration). More information: http://bit.ly/2H1KTQo
        // Note: if removed, this will cause crashes on Android < 21
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        AppThemeUtils.setAppTheme(application)

        // verify media is sanitized
        sanitizeMediaUploadStateForSite()

        // remove expired lists
        dispatcher.dispatch(ListActionBuilder.newRemoveExpiredListsAction(RemoveExpiredListsPayload()))

        // setup the Credentials Client so we can clean it up on wpcom logout
        setupCredentialsClient()

        if (!initialized) {
            initWorkManager()
        }

        // Enqueue our periodic upload work request. The UploadWorkRequest will be called even if the app is closed.
        // It will upload local draft or published posts with local changes to the server.
        enqueuePeriodicUploadWorkRequestForAllSites()

        systemNotificationsTracker.checkSystemNotificationsState()
        ImageEditorInitializer.init(imageManager, imageEditorTracker, imageEditorFileUtils, appScope)

        exPlat.forceRefresh()

        initDebugCookieManager()

        if (!initialized && BuildConfig.DEBUG && Build.VERSION.SDK_INT >= VERSION_CODES.R) {
            initAppOpsManager()
        }

        AppLog.i(T.UTILS, "AppInitializer.userAgentString: $userAgent")

        initialized = true
    }

    private fun initDebugCookieManager() {
        if (buildConfig.isDebugSettingsEnabled()) {
            debugCookieManager = DebugCookieManager(application, cookieManager, buildConfig)
            debugCookieManager.sync()
        }
    }

    /**
     * Data access auditing
     * @link https://developer.android.com/guide/topics/data/audit-access
     */
    @RequiresApi(VERSION_CODES.R)
    private fun initAppOpsManager() {
        val appOpsManager = context?.getSystemService(AppOpsManager::class.java) as AppOpsManager

        val appOpsCallback = object : AppOpsManager.OnOpNotedCallback() {
            private fun logPrivateDataAccess(opCode: String, trace: String) {
                AppLog.i(T.MAIN, "Private data accessed. Operation: $opCode\nStack Trace:\n$trace")
            }

            override fun onNoted(syncNotedAppOp: SyncNotedAppOp) {
                logPrivateDataAccess(syncNotedAppOp.op, Throwable("Stack Trace: ").stackTrace.toString())
            }

            override fun onSelfNoted(syncNotedAppOp: SyncNotedAppOp) {
                logPrivateDataAccess(syncNotedAppOp.op, Throwable("Stack Trace: ").stackTrace.toString())
            }

            override fun onAsyncNoted(asyncNotedAppOp: AsyncNotedAppOp) {
                logPrivateDataAccess(asyncNotedAppOp.op, asyncNotedAppOp.message)
            }
        }

        appOpsManager.setOnOpNotedCallback(context?.mainExecutor, appOpsCallback)
    }

    private fun initWorkManager() {
        val configBuilder = androidx.work.Configuration.Builder().setWorkerFactory(wordPressWorkerFactory)
        if (BuildConfig.DEBUG) {
            configBuilder.setMinimumLoggingLevel(Log.DEBUG)
        }
        configBuilder.setJobSchedulerJobIdRange(WORK_MANAGER_ID_RANGE_MIN, WORK_MANAGER_ID_RANGE_MAX)
        WorkManager.initialize(application, configBuilder.build())
    }

    @Suppress("TooGenericExceptionCaught")
    private fun enableLogRecording() {
        AppLog.enableRecording(true)
        try {
            AppLog.enableLogFilePersistence(application.baseContext, MAX_LOG_COUNT)
        } catch (e: Exception) {
            AppLog.enableRecording(false)
            AppLog.e(T.UTILS, "Error enabling log file persistence", e)
        }
    }

    private fun sanitizeMediaUploadStateForSite() {
        val selectedSiteLocalId: Int = selectedSiteRepository.getSelectedSiteLocalId(true)
        val site = siteStore.getSiteByLocalId(selectedSiteLocalId)
        site?.let {
            Thread {
                UploadService.sanitizeMediaUploadStateForSite(mediaStore, dispatcher, site)
            }.start()
        }
    }

    @Suppress("DEPRECATION")
    private fun setupCredentialsClient() {
        credentialsClient = GoogleApiClient.Builder(application)
            .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(bundle: Bundle?) {
                    // Do nothing
                }

                override fun onConnectionSuspended(i: Int) {
                    // Do nothing
                }
            })
            .addApi(Auth.CREDENTIALS_API)
            .build()
        credentialsClient.connect()
    }

    private fun createNotificationChannelsOnSdk26(
        normal: Boolean = true,
        important: Boolean = true,
        reminder: Boolean = true,
        transient: Boolean = true,
        weeklyRoundup: Boolean = true
    ) {
        // create Notification channels introduced in Android Oreo
        if (Build.VERSION.SDK_INT >= VERSION_CODES.O) {
            val notificationManager = application.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            if (normal) {
                // Create the NORMAL channel (used for likes, comments, replies, etc.)
                val normalChannel = NotificationChannel(
                    application.getString(R.string.notification_channel_normal_id),
                    application.getString(R.string.notification_channel_general_title),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this

                notificationManager.createNotificationChannel(normalChannel)
            }
            if (important) {
                // Create the IMPORTANT channel (used for 2fa auth, for example)
                val importantChannel = NotificationChannel(
                    application.getString(R.string.notification_channel_important_id),
                    application.getString(R.string.notification_channel_important_title),
                    NotificationManager.IMPORTANCE_HIGH
                )
                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                notificationManager.createNotificationChannel(importantChannel)
            }
            if (reminder) {
                // Create the REMINDER channel (used for various reminders, like Quick Start, etc.)
                val reminderChannel = NotificationChannel(
                    application.getString(R.string.notification_channel_reminder_id),
                    application.getString(R.string.notification_channel_reminder_title),
                    NotificationManager.IMPORTANCE_LOW
                )
                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                notificationManager.createNotificationChannel(reminderChannel)
            }
            if (transient) {
                // Create the TRANSIENT channel (used for short-lived notifications such as processing a Like/Approve,
                // or media upload)
                val transientChannel = NotificationChannel(
                    application.getString(R.string.notification_channel_transient_id),
                    application.getString(R.string.notification_channel_transient_title),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                transientChannel.setSound(null, null)
                transientChannel.enableVibration(false)
                transientChannel.enableLights(false)
                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                notificationManager.createNotificationChannel(transientChannel)
            }
            if (weeklyRoundup) {
                // Create the WEEKLY ROUNDUP channel (used for weekly roundup notification containing weekly stats)
                val weeklyRoundupChannel = NotificationChannel(
                    application.getString(R.string.notification_channel_weekly_roundup_id),
                    application.getString(R.string.notification_channel_weekly_roundup_title),
                    NotificationManager.IMPORTANCE_LOW
                )
                // Register the channel with the system; you can't change the importance or other notification behaviors
                // after this
                notificationManager.createNotificationChannel(weeklyRoundupChannel)
            }
        }
    }

    private fun initAnalytics(elapsedTimeOnCreate: Long) {
        AnalyticsTracker.registerTracker(tracker)
        AnalyticsTracker.init(context)
        AnalyticsUtils.refreshMetadata(accountStore, siteStore)

        // Track app upgrade and install
        val versionCode = PackageUtils.getVersionCode(context)
        val oldVersionCode = AppPrefs.getLastAppVersionCode()
        if (oldVersionCode == 0) {
            // Track application installed if there isn't old version code
            AnalyticsTracker.track(Stat.APPLICATION_INSTALLED)
        }
        if (oldVersionCode != 0 && oldVersionCode < versionCode) {
            val properties: MutableMap<String, Long?> = HashMap(1)
            properties["elapsed_time_on_create"] = elapsedTimeOnCreate
            // app upgraded
            AnalyticsTracker.track(Stat.APPLICATION_UPGRADED, properties)
        }
        AppPrefs.setLastAppVersionCode(versionCode)
    }

    /**
     * Application.onCreate is called before any activity, service, or receiver - it can be called while the app
     * is in background by a sticky service or a receiver, so we don't want Application.onCreate to make network request
     * or other heavy tasks.
     *
     *
     * This deferredInit method is called when a user starts an activity for the first time, ie. when he sees a
     * screen for the first time. This allows us to have heavy calls on first activity startup instead of app startup.
     */
    fun deferredInit() {
        AppLog.i(T.UTILS, "Deferred Initialisation")

        // Refresh account informations
        if (accountStore.hasAccessToken()) {
            dispatcher.dispatch(AccountActionBuilder.newFetchAccountAction())
            dispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction())
            NotificationsUpdateServiceStarter.startService(context)
        }
    }

    private fun initWpDb() {
        if (!createAndVerifyWpDb()) {
            AppLog.e(T.DB, "Invalid database, sign out user and delete database")
            // Force DB deletion
            WordPressDB.deleteDatabase(application)
            WordPress.wpDB = WordPressDB(application)
        }
    }

    private fun createAndVerifyWpDb(): Boolean {
        return try {
            WordPress.wpDB = WordPressDB(application)
            true
        } catch (e: SQLiteException) {
            AppLog.e(T.DB, e)
            false
        } catch (e: SQLException) {
            AppLog.e(T.DB, e)
            false
        }
    }

    /**
     * Sign out from wpcom account.
     * Note: This method must not be called on UI Thread.
     */
    fun wordPressComSignOut() {
        // Keep the analytics tracking at the beginning, before the account data is actual removed.
        AnalyticsTracker.track(Stat.ACCOUNT_LOGOUT)

        removeWpComUserRelatedData(application.applicationContext)

        if (credentialsClient.isConnected) {
            Auth.CredentialsApi.disableAutoSignIn(credentialsClient)
        }

        // Once fully logged out refresh the metadata so the user information doesn't persist for logged out events
        AnalyticsUtils.refreshMetadata(accountStore, siteStore)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (!FluxCUtils.isSignedInWPComOrHasWPOrgSite(accountStore, siteStore)) {
            appConfig.refresh(appScope)
            flushHttpCache()

            // Analytics resets
            AnalyticsTracker.endSession(false)
            AnalyticsTracker.clearAllData()
        }

        if (!event.isError && accountStore.hasAccessToken()) {
            // previously we reset the reader database on logout but this meant losing saved posts
            // so now we only reset it when the user id changes
            if (event.causeOfChange == AccountAction.FETCH_ACCOUNT) {
                val thisUserId = accountStore.account.userId
                val lastUserId = AppPrefs.getLastUsedUserId()
                if (thisUserId != lastUserId) {
                    AppPrefs.setLastUsedUserId(thisUserId)
                    AppLog.i(T.READER, "User changed, resetting reader db")
                    ReaderDatabase.reset(false)
                }
            } else if (event.causeOfChange == AccountAction.FETCH_SETTINGS) {
                val prefs = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext())
                val hasUserOptedOut = !prefs.getBoolean(application.getString(R.string.pref_key_send_usage), true)
                AnalyticsTracker.setHasUserOptedOut(hasUserOptedOut)
                // When local and remote prefs are different, force opt out to TRUE
                if (hasUserOptedOut != accountStore.account.tracksOptOut) {
                    AnalyticsUtils.updateAnalyticsPreference(WordPress.getContext(), dispatcher, accountStore, true)
                }
            }
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAuthenticationChanged(event: OnAuthenticationChanged) {
        appConfig.refresh(appScope)

        if (accountStore.hasAccessToken()) {
            // Make sure the Push Notification token is sent to our servers after a successful login
            gcmRegistrationScheduler.scheduleRegistration()

            // Force a refresh if user has logged in. This can be removed once we start using an anonymous ID.
            exPlat.forceRefresh()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUnexpectedError(event: OnUnexpectedError) {
        AppLog.d(T.API, "Receiving OnUnexpectedError event, message: " + event.exception.message)
    }

    @Suppress("DEPRECATION")
    private fun removeWpComUserRelatedData(context: Context) {
        // cancel all Volley requests - do this before unregistering push since that uses a Volley request
        VolleyUtils.cancelAllRequests(requestQueue)

        NotificationsUtils.unregisterDevicePushNotifications(context)
        zendeskHelper.reset()
        try {
            FirebaseInstanceId.getInstance().deleteInstanceId()
        } catch (e: IOException) {
            AppLog.e(T.NOTIFS, "Could not delete GCM Token", e)
        } catch (e: IllegalArgumentException) {
            AppLog.e(T.NOTIFS, "Could not delete GCM Token", e)
        }

        // reset default account
        dispatcher.dispatch(AccountActionBuilder.newSignOutAction())
        // delete site-associated themes (keep WP.com themes cached)
        for (site in siteStore.sites) {
            dispatcher.dispatch(ThemeActionBuilder.newRemoveSiteThemesAction(site))
        }

        // delete wpcom and jetpack sites
        dispatcher.dispatch(SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction())

        // remove all lists
        dispatcher.dispatch(ListActionBuilder.newRemoveAllListsAction())
        // remove all posts
        dispatcher.dispatch(PostActionBuilder.newRemoveAllPostsAction())

        // reset all user prefs
        AppPrefs.reset()

        // reset the reader database, but retain bookmarked posts
        ReaderDatabase.reset(true)

        // Reset Stats Data
        statsStore.deleteAllData()
        statsWidgetUpdaters.update(context)

        // Reset Notifications Data
        NotificationsTable.reset()

        // clear App config data
        appConfig.clear()

        // Cancel QuickStart reminders
        QuickStartUtils.cancelQuickStartReminder(context)

        // Remove private Atomic cookie
        privateAtomicCookie.clearCookie()

        // Clear cached assignments if user has logged out. This can be removed once we start using an anonymous ID.
        exPlat.clear()
    }

    /*
     * Since Android P:
     * "Apps can no longer share a single WebView data directory across processes.
     * If your app has more than one process using WebView, CookieManager, or any other API in the android.webkit
     * package, your app will crash when the second process calls a WebView method."
     *
     * (see https://developer.android.com/about/versions/pie/android-9.0-migration)
     *
     * Also here: https://developer.android.com/about/versions/pie/android-9.0-changes-28#web-data-dirs
     *
     * "If your app must use instances of WebView in more than one process, you must assign a unique data directory
     * suffix for each process, using the WebView.setDataDirectorySuffix() method, before using a given instance of
     * WebView in that process."
     *
     * While we don't explicitly use a different process other than the default, making the directory suffix be the
     * actual process name will ensure there's one directory per process, should the Application's onCreate() method be
     * called from a different process any time.
     *
    */
    private fun setWebViewDataDirectorySuffixOnAndroidP() {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.P) {
            val procName = Application.getProcessName()
            if (!TextUtils.isEmpty(procName)) {
                WebView.setDataDirectorySuffix(procName)
            }
        }
    }

    /*
     * enable caching for HttpUrlConnection
     * http://developer.android.com/training/efficient-downloads/redundant_redundant.html
     */
    @Suppress("SwallowedException")
    private fun enableHttpResponseCache(context: Context) {
        try {
            val httpCacheDir = File(context.cacheDir, "http")
            HttpResponseCache.install(httpCacheDir, HTTP_CACHE_SIZE)
        } catch (e: IOException) {
            AppLog.w(T.UTILS, "Failed to enable http response cache")
        }
    }

    private fun enableDeepLinkingComponentsIfNeeded() {
        if (openWebLinksWithJetpackFlowFeatureConfig.isEnabled()) {
            if (!AppPrefs.getIsOpenWebLinksWithJetpack()) {
                openWebLinksWithJetpackHelper.enableDeepLinks()
            }
        } else {
            openWebLinksWithJetpackHelper.enableDeepLinks()
        }
    }

    private fun disableWidgetReceiversIfNeeded() {
        jetpackFeatureRemovalWidgetHelper.disableWidgetReceiversIfNeeded()
    }

    private fun flushHttpCache() {
        val cache = HttpResponseCache.getInstalled()
        cache?.flush()
    }

    override fun onStart(owner: LifecycleOwner) {
        applicationLifecycleMonitor.onAppComesFromBackground()
        appConfig.refresh(appScope)
    }

    override fun onStop(owner: LifecycleOwner) {
        applicationLifecycleMonitor.onAppGoesToBackground()
    }

    inner class ApplicationLifecycleMonitor {
        private var lastPingDate: Date? = null
        private var applicationOpenedDate: Date? = null
        private var connectionReceiverRegistered = false
        var firstActivityResumed = true

        private val isPushNotificationPingNeeded: Boolean
            get() {
                if (lastPingDate == null) {
                    // first startup
                    return false
                }
                val now = Date()
                if (DateTimeUtils.secondsBetween(now, lastPingDate) >= DEFAULT_TIMEOUT) {
                    lastPingDate = now
                    return true
                }
                return false
            }

        /**
         * Check if user has valid credentials, and at least 2 minutes have passed since the last ping, then try to
         * update the PN token.
         */
        private fun updatePushNotificationTokenIfNotLimited() {
            // Sync Push Notifications settings
            if (isPushNotificationPingNeeded && accountStore.hasAccessToken()) {
                // Register for Cloud messaging
                gcmRegistrationScheduler.scheduleRegistration()
            }
        }

        fun onAppGoesToBackground() {
            AppLog.i(T.UTILS, "App goes to background")
            if (WordPress.appIsInTheBackground) {
                return
            }
            WordPress.appIsInTheBackground = true
            val lastActivityString = AppPrefs.getLastActivityStr()
            val lastActivity = ActivityId.getActivityIdFromName(lastActivityString)
            val properties: MutableMap<String, Any?> = HashMap()
            properties["last_visible_screen"] = lastActivity.toString()
            if (applicationOpenedDate != null) {
                val now = Date()
                properties["time_in_app"] = DateTimeUtils.secondsBetween(now, applicationOpenedDate)
                applicationOpenedDate = null
            }
            properties.putAll(readerTracker.getAnalyticsData())

            readerTracker.onAppGoesToBackground()

            // Ensure that the deeplinking activity is are re-enabled if needed
            enableDeepLinkingComponentsIfNeeded()

            AnalyticsTracker.track(Stat.APPLICATION_CLOSED, properties)
            AnalyticsTracker.endSession(false)
            // Methods onAppComesFromBackground and onAppGoesToBackground are only workarounds to track when the app
            // goes to or comes from background. The workarounds are not 100% reliable, so avoid unregistering the
            // receiver twice.
            if (connectionReceiverRegistered) {
                connectionReceiverRegistered = false
                try {
                    application.unregisterReceiver(ConnectionChangeReceiver.getInstance())
                    AppLog.d(MAIN, "ConnectionChangeReceiver successfully unregistered")
                } catch (e: IllegalArgumentException) {
                    AppLog.e(MAIN, "ConnectionChangeReceiver was already unregistered")
                }
            }

            // Disable the widgets if needed
            disableWidgetReceiversIfNeeded()
        }

        /**
         * This method is called when:
         * 1. the app starts (but it's not opened by a service or a broadcast receiver, i.e. an activity is resumed)
         * 2. the app was in background and is now foreground
         */
        @Suppress("DEPRECATION")
        fun onAppComesFromBackground() {
            readerTracker.setupTrackers()
            AppLog.i(T.UTILS, "App comes from background")
            if (!WordPress.appIsInTheBackground) {
                return
            }
            WordPress.appIsInTheBackground = false

            // https://developer.android.com/reference/android/net/ConnectivityManager.html
            // Apps targeting Android 7.0 (API level 24) and higher do not receive this broadcast if the broadcast
            // receiver is declared in their manifest. Apps will still receive broadcasts if BroadcastReceiver is
            // registered with Context.registerReceiver() and that context is still valid.
            if (!connectionReceiverRegistered) {
                connectionReceiverRegistered = true
                application.registerReceiver(
                    ConnectionChangeReceiver.getInstance(),
                    IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
                )
            }
            AnalyticsUtils.refreshMetadata(accountStore, siteStore)
            applicationOpenedDate = Date()
            // This stat is part of a funnel that provides critical information. Before making ANY modification to this
            // stat please refer to: p4qSXL-35X-p2
            AnalyticsTracker.track(Stat.APPLICATION_OPENED)
            if (NetworkUtils.isNetworkAvailable(context)) {
                // Refresh account informations and Notifications
                if (accountStore.hasAccessToken()) {
                    NotificationsUpdateServiceStarter.startService(context)
                }

                // verify media is sanitized
                sanitizeMediaUploadStateForSite()

                // Rate limited PN Token Update
                updatePushNotificationTokenIfNotLimited()

                // Rate limited WPCom blog list update
                updateSiteList.runIfNotLimited()

                // Rate limited Site information and options update
                updateSelectedSite.runIfNotLimited()
            }

            // Let's migrate the old editor preference if available in AppPrefs to the remote backend
            SiteUtils.migrateAppWideMobileEditorPreferenceToRemote(accountStore, siteStore, dispatcher)
            if (!firstActivityResumed) {
                // Since we're force refreshing on app startup, we don't need to try refreshing again when starting
                // our first Activity.
                exPlat.refreshIfNeeded()
            }
            if (firstActivityResumed) {
                deferredInit()
            }
            firstActivityResumed = false

            // Disable the widgets if needed
            disableWidgetReceiversIfNeeded()
        }
    }

    /**
     * Uses ComponentCallbacks2 is used for memory-related event handling and configuration changes
     */
    private inner class MemoryAndConfigChangeMonitor : ComponentCallbacks2 {
        override fun onConfigurationChanged(newConfig: Configuration) {
            // Reapply locale on configuration change
            LocaleManager.setLocale(context)
        }

        override fun onLowMemory() {
            // Do nothing
        }

        override fun onTrimMemory(level: Int) {
            var evictBitmaps = false
            when (level) {
                ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
                ComponentCallbacks2.TRIM_MEMORY_MODERATE,
                ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
                ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
                ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> evictBitmaps = true
                ComponentCallbacks2.TRIM_MEMORY_BACKGROUND, ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {}
                else -> {}
            }
            if (evictBitmaps && bitmapCache != null) {
                // getBitmapCache
                WordPress.getBitmapCache().evictAll()
            }
        }
    }

    private fun updateNotificationSettings() {
        if (!jetpackFeatureRemovalPhaseHelper.shouldShowNotifications()) {
            NotificationsUtils.cancelAllNotifications(application)
            // Only create the transient notification channel to handle upload notifications
            createNotificationChannelsOnSdk26(
                normal = false,
                important = false,
                reminder = false,
                transient = true,
                weeklyRoundup = false,
            )
        } else {
            createNotificationChannelsOnSdk26()
        }
    }

    companion object {
        private const val SECONDS_BETWEEN_SITE_UPDATE = 60 * 60 // 1 hour
        private const val SECONDS_BETWEEN_BLOGLIST_UPDATE = 15 * 60 // 15 minutes
        private const val MAX_LOG_COUNT = 5
        private const val HTTP_CACHE_SIZE: Long = 5 * 1024 * 1024 // 5 MB
        private const val KILOBYTES_IN_BYTES = 1024
        private const val MEMORY_CACHE_RATIO = 0.25 // Use 1/4th of the available memory for memory cache.
        private const val DEFAULT_TIMEOUT = 2 * 60 // 2 minutes

        // Use service ids near the int max to avoid collisions with existing JobService ids
        // The minimum range size is 1000, but we can easily give 10000.
        private const val WORK_MANAGER_ID_RANGE_MAX = Int.MAX_VALUE
        private const val WORK_MANAGER_ID_RANGE_MIN = WORK_MANAGER_ID_RANGE_MAX - 10000

        @SuppressLint("StaticFieldLeak")
        var context: Context? = null
            private set

        // This is for UI testing. AppInitializer is being created more than once for only UI tests. initialized
        // prevents some static functions from being initialized twice and exceptions.
        private var initialized = false

        private var bitmapCache: BitmapLruCache? = null
        private var sOAuthAuthenticator: OAuthAuthenticator? = null

        val restClientUtils: RestClientUtils by lazy {
            RestClientUtils(
                context,
                WordPress.requestQueue,
                sOAuthAuthenticator,
                null
            )
        }

        val restClientUtilsV1_1: RestClientUtils by lazy {
            RestClientUtils(
                context,
                WordPress.requestQueue,
                sOAuthAuthenticator,
                null,
                RestClient.REST_CLIENT_VERSIONS.V1_1
            )
        }

        val restClientUtilsV1_2: RestClientUtils by lazy {
            RestClientUtils(
                context,
                WordPress.requestQueue,
                sOAuthAuthenticator,
                null,
                RestClient.REST_CLIENT_VERSIONS.V1_2
            )
        }

        val restClientUtilsV1_3: RestClientUtils by lazy {
            RestClientUtils(
                context,
                WordPress.requestQueue,
                sOAuthAuthenticator,
                null,
                RestClient.REST_CLIENT_VERSIONS.V1_3
            )
        }

        val restClientUtilsV2: RestClientUtils by lazy {
            RestClientUtils(
                context,
                WordPress.requestQueue,
                sOAuthAuthenticator,
                null,
                RestClient.REST_CLIENT_VERSIONS.V2
            )
        }

        val restClientUtilsV0: RestClientUtils by lazy {
            RestClientUtils(
                context,
                WordPress.requestQueue,
                sOAuthAuthenticator,
                null,
                RestClient.REST_CLIENT_VERSIONS.V0
            )
        }

        fun getBitmapCache(): BitmapLruCache {
            if (bitmapCache == null) {
                // The cache size will be measured in kilobytes rather than number of items.
                // See http://developer.android.com/training/displaying-bitmaps/cache-bitmap.html
                val maxMemory = (Runtime.getRuntime().maxMemory() / KILOBYTES_IN_BYTES).toInt()
                val cacheSize = (maxMemory * MEMORY_CACHE_RATIO).toInt()
                bitmapCache = BitmapLruCache(cacheSize)
            }
            return bitmapCache as BitmapLruCache
        }

        /**
         * Update locale of the static context when language is changed.
         *
         * When calling this method the application context **must** be already initialized.
         * This is already the case in `Activity`, `Fragment` or `View`.
         *
         * When called from other places (E.g. a `TestRule`) we should provide it in the [appContext] parameter.
         */
        fun updateContextLocale(appContext: Context? = null) {
            val context = appContext ?: run {
                check(context != null) { "Context must be initialized before calling updateContextLocale" }
                return@run context
            }
            this.context = LocaleManager.setLocale(context)
        }
    }
}
