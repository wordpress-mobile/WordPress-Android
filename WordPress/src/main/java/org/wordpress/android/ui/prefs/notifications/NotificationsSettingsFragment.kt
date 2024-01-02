package org.wordpress.android.ui.prefs.notifications

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.wordpress.rest.RestRequest
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.WordPress.Companion.getRestClientUtilsV1_1
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.databinding.JetpackBadgeFooterBinding
import org.wordpress.android.datasets.ReaderBlogTable
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload.SubscriptionAction
import org.wordpress.android.fluxc.store.AccountStore.OnSubscriptionUpdated
import org.wordpress.android.fluxc.store.AccountStore.OnSubscriptionsChanged
import org.wordpress.android.fluxc.store.AccountStore.SubscriptionType
import org.wordpress.android.fluxc.store.AccountStore.UpdateSubscriptionPayload
import org.wordpress.android.fluxc.store.AccountStore.UpdateSubscriptionPayload.SubscriptionFrequency
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.models.NotificationsSettings
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.WPLaunchActivity
import org.wordpress.android.ui.bloggingreminders.BloggingReminderUtils.observeBottomSheet
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsSettingsStatusChanged
import org.wordpress.android.ui.notifications.utils.NotificationsUtils
import org.wordpress.android.ui.prefs.notifications.FollowedBlogsProvider.PreferenceModel
import org.wordpress.android.ui.prefs.notifications.NotificationsSettingsDialogPreference.BloggingRemindersProvider
import org.wordpress.android.ui.prefs.notifications.NotificationsSettingsDialogPreference.OnNotificationsSettingsChangedListener
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.WPActivityUtils
import org.wordpress.android.util.WPPermissionUtils
import org.wordpress.android.util.extensions.getColorStateListFromAttribute
import java.util.Collections
import javax.inject.Inject

class NotificationsSettingsFragment : PreferenceFragment(), OnSharedPreferenceChangeListener {
    private var mNotificationsSettings: NotificationsSettings? = null
    private var mSearchView: SearchView? = null
    private var mSearchMenuItem: MenuItem? = null
    private var mSearchMenuItemCollapsed = true
    private var mDeviceId: String? = null
    private var mNotificationUpdatedSite: String? = null
    private var mPreviousEmailPostsFrequency: String? = null
    private var mRestoredQuery: String? = null
    private var mUpdateSubscriptionFrequencyPayload: UpdateSubscriptionPayload? = null
    private var mNotificationsEnabled = false
    private var mPreviousEmailComments = false
    private var mPreviousEmailPosts = false
    private var mPreviousNotifyPosts = false
    private var mUpdateEmailPostsFirst = false
    private var mSiteCount = 0
    private var mSubscriptionCount = 0
    private val mTypePreferenceCategories: MutableList<PreferenceCategory> = ArrayList()
    private var mBlogsCategory: PreferenceCategory? = null
    private var mFollowedBlogsCategory: PreferenceCategory? = null

    @JvmField
    @Inject
    var mAccountStore: AccountStore? = null

    @JvmField
    @Inject
    var mSiteStore: SiteStore? = null

    @JvmField
    @Inject
    var mDispatcher: Dispatcher? = null

    @JvmField
    @Inject
    var mFollowedBlogsProvider: FollowedBlogsProvider? = null

    @JvmField
    @Inject
    var mBuildConfigWrapper: BuildConfigWrapper? = null

    @JvmField
    @Inject
    var mViewModelFactory: ViewModelProvider.Factory? = null

    @JvmField
    @Inject
    var mJetpackBrandingUtils: JetpackBrandingUtils? = null

    @JvmField
    @Inject
    var mUiHelpers: UiHelpers? = null
    private var mBloggingRemindersViewModel: BloggingRemindersViewModel? = null
    private val mBloggingRemindersSummariesBySiteId: MutableMap<Long?, UiString?> = HashMap()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity.application as WordPress).component().inject(this)
        addPreferencesFromResource(R.xml.notifications_settings)
        setHasOptionsMenu(true)
        removeSightAndSoundsForAPI26()
        removeFollowedBlogsPreferenceForIfDisabled()

        // Bump Analytics
        if (savedInstanceState == null) {
            AnalyticsTracker.track(Stat.NOTIFICATION_SETTINGS_LIST_OPENED)
        }
    }

    private fun removeSightAndSoundsForAPI26() {
        // on API26 we removed the Sight & Sounds category altogether, as it can always be
        // overriden by the user in the Device settings, and the settings here
        // wouldn't either reflect nor have any effect anyway.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val preferenceScreen =
                findPreference(activity.getString(R.string.wp_pref_notifications_root)) as PreferenceScreen
            val categorySightsAndSounds = preferenceScreen
                .findPreference(activity.getString(R.string.pref_notification_sights_sounds)) as PreferenceCategory
            preferenceScreen.removePreference(categorySightsAndSounds)
        }
    }

    private fun removeFollowedBlogsPreferenceForIfDisabled() {
        if (!mBuildConfigWrapper!!.isFollowedSitesSettingsEnabled) {
            val preferenceScreen =
                findPreference(activity.getString(R.string.wp_pref_notifications_root)) as PreferenceScreen
            val categoryFollowedBlogs = preferenceScreen
                .findPreference(activity.getString(R.string.pref_notification_blogs_followed)) as PreferenceCategory
            preferenceScreen.removePreference(categoryFollowedBlogs)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val isLoggedIn = mAccountStore!!.hasAccessToken()
        if (!isLoggedIn) {
            // Not logged in users can start Notification Settings from App info > Notifications menu.
            // If there isn't a logged in user, just show the entry screen.
            val intent = Intent(context, WPLaunchActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            activity.finish()
            return
        }
        val settings = PreferenceManager.getDefaultSharedPreferences(activity)
        mDeviceId = settings.getString(NotificationsUtils.WPCOM_PUSH_DEVICE_SERVER_ID, "")
        if (hasNotificationsSettings()) {
            loadNotificationsAndUpdateUI(true)
        }
        if (savedInstanceState != null && savedInstanceState.containsKey(
                NotificationsSettingsFragment.Companion.KEY_SEARCH_QUERY
            )
        ) {
            mRestoredQuery =
                savedInstanceState.getString(NotificationsSettingsFragment.Companion.KEY_SEARCH_QUERY)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val lv = view.findViewById<View>(android.R.id.list) as ListView
        if (lv != null) {
            ViewCompat.setNestedScrollingEnabled(lv, true)
            addJetpackBadgeAsFooterIfEnabled(lv)
        }
        initBloggingReminders()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle) {
        super.onViewStateRestored(savedInstanceState)
        val otherBlogsScreen = findPreference(
            getString(R.string.pref_notification_other_blogs)
        ) as PreferenceScreen
        addToolbarToDialog(otherBlogsScreen)
    }

    private fun addJetpackBadgeAsFooterIfEnabled(listView: ListView) {
        if (mJetpackBrandingUtils!!.shouldShowJetpackBranding()) {
            val screen: JetpackPoweredScreen =
                JetpackPoweredScreen.WithDynamicText.NOTIFICATIONS_SETTINGS
            val context = context
            val inflater = LayoutInflater.from(context)
            val binding = JetpackBadgeFooterBinding.inflate(inflater)
            binding.footerJetpackBadge.jetpackPoweredBadge.text = mUiHelpers!!.getTextOfUiString(
                context,
                mJetpackBrandingUtils!!.getBrandingTextForScreen(screen)
            )
            if (mJetpackBrandingUtils!!.shouldShowJetpackPoweredBottomSheet()) {
                binding.footerJetpackBadge.jetpackPoweredBadge.setOnClickListener { v: View? ->
                    mJetpackBrandingUtils!!.trackBadgeTapped(screen)
                    JetpackPoweredBottomSheetFragment().show(
                        (activity as AppCompatActivity).supportFragmentManager,
                        JetpackPoweredBottomSheetFragment.TAG
                    )
                }
            }
            listView.addFooterView(binding.root, null, false)
        }
    }

    override fun onStart() {
        super.onStart()
        mDispatcher!!.register(this)
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onResume() {
        super.onResume()
        mNotificationsEnabled = NotificationsUtils.isNotificationsEnabled(activity)
        refreshSettings()
    }

    override fun onStop() {
        super.onStop()
        mDispatcher!!.unregister(this)
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.notifications_settings, menu)
        mSearchMenuItem = menu.findItem(R.id.menu_notifications_settings_search)
        mSearchView = mSearchMenuItem?.getActionView() as SearchView?
        mSearchView!!.queryHint = getString(R.string.search_sites)
        mBlogsCategory = findPreference(
            getString(R.string.pref_notification_blogs)
        ) as PreferenceCategory
        mFollowedBlogsCategory = findPreference(
            getString(R.string.pref_notification_blogs_followed)
        ) as PreferenceCategory
        mSearchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                configureBlogsSettings(mBlogsCategory, true)
                configureFollowedBlogsSettings(mFollowedBlogsCategory, true)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                // we need to perform this check because when the search menu item is collapsed
                // a new queryTExtChange event is triggered with an empty value "", and we only
                // would want to take care of it when the user actively opened/cleared the search term
                configureBlogsSettings(mBlogsCategory, !mSearchMenuItemCollapsed)
                configureFollowedBlogsSettings(mFollowedBlogsCategory, !mSearchMenuItemCollapsed)
                return true
            }
        })
        mSearchMenuItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                mSearchMenuItemCollapsed = false
                configureBlogsSettings(mBlogsCategory, true)
                configureFollowedBlogsSettings(mFollowedBlogsCategory, true)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                mSearchMenuItemCollapsed = true
                configureBlogsSettings(mBlogsCategory, false)
                configureFollowedBlogsSettings(mFollowedBlogsCategory, false)
                return true
            }
        })
        updateSearchMenuVisibility()

        // Check for a restored search query (if device was rotated, etc)
        if (!TextUtils.isEmpty(mRestoredQuery)) {
            mSearchMenuItem?.expandActionView()
            mSearchView!!.setQuery(mRestoredQuery, true)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (mSearchView != null && !TextUtils.isEmpty(mSearchView!!.query)) {
            outState.putString(
                NotificationsSettingsFragment.Companion.KEY_SEARCH_QUERY,
                mSearchView!!.query.toString()
            )
        }
        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null && requestCode == RequestCodes.NOTIFICATION_SETTINGS) {
            val notifyPosts = data.getBooleanExtra(
                NotificationSettingsFollowedDialog.KEY_NOTIFICATION_POSTS,
                false
            )
            val emailPosts =
                data.getBooleanExtra(NotificationSettingsFollowedDialog.KEY_EMAIL_POSTS, false)
            val emailPostsFrequency =
                data.getStringExtra(NotificationSettingsFollowedDialog.KEY_EMAIL_POSTS_FREQUENCY)
            val emailComments =
                data.getBooleanExtra(NotificationSettingsFollowedDialog.KEY_EMAIL_COMMENTS, false)
            if (notifyPosts != mPreviousNotifyPosts) {
                ReaderBlogTable.setNotificationsEnabledByBlogId(
                    mNotificationUpdatedSite!!.toLong(),
                    notifyPosts
                )
                val payload: AddOrDeleteSubscriptionPayload
                payload = if (notifyPosts) {
                    AnalyticsTracker.track(Stat.FOLLOWED_BLOG_NOTIFICATIONS_SETTINGS_ON)
                    AddOrDeleteSubscriptionPayload(
                        mNotificationUpdatedSite!!,
                        SubscriptionAction.NEW
                    )
                } else {
                    AnalyticsTracker.track(Stat.FOLLOWED_BLOG_NOTIFICATIONS_SETTINGS_OFF)
                    AddOrDeleteSubscriptionPayload(
                        mNotificationUpdatedSite!!,
                        SubscriptionAction.DELETE
                    )
                }
                mDispatcher!!.dispatch(
                    AccountActionBuilder.newUpdateSubscriptionNotificationPostAction(
                        payload
                    )
                )
            }
            if (emailPosts != mPreviousEmailPosts) {
                val payload: AddOrDeleteSubscriptionPayload
                payload = if (emailPosts) {
                    AnalyticsTracker.track(Stat.FOLLOWED_BLOG_NOTIFICATIONS_SETTINGS_EMAIL_ON)
                    AddOrDeleteSubscriptionPayload(
                        mNotificationUpdatedSite!!,
                        SubscriptionAction.NEW
                    )
                } else {
                    AnalyticsTracker.track(Stat.FOLLOWED_BLOG_NOTIFICATIONS_SETTINGS_EMAIL_OFF)
                    AddOrDeleteSubscriptionPayload(
                        mNotificationUpdatedSite!!,
                        SubscriptionAction.DELETE
                    )
                }
                mDispatcher!!.dispatch(
                    AccountActionBuilder.newUpdateSubscriptionEmailPostAction(
                        payload
                    )
                )
            }
            if (emailPostsFrequency != null && !emailPostsFrequency.equals(
                    mPreviousEmailPostsFrequency,
                    ignoreCase = true
                )
            ) {
                val subscriptionFrequency = getSubscriptionFrequencyFromString(emailPostsFrequency)
                mUpdateSubscriptionFrequencyPayload = UpdateSubscriptionPayload(
                    mNotificationUpdatedSite!!,
                    subscriptionFrequency
                )
                /*
                 * The email post frequency update will be overridden by the email post update if the email post
                 * frequency callback returns first.  Thus, the updates must be dispatched sequentially when the
                 * email post update is switched from disabled to enabled.
                 */if (emailPosts != mPreviousEmailPosts && emailPosts) {
                    mUpdateEmailPostsFirst = true
                } else {
                    mDispatcher!!.dispatch(
                        AccountActionBuilder.newUpdateSubscriptionEmailPostFrequencyAction(
                            mUpdateSubscriptionFrequencyPayload
                        )
                    )
                }
            }
            if (emailComments != mPreviousEmailComments) {
                val payload: AddOrDeleteSubscriptionPayload
                payload = if (emailComments) {
                    AnalyticsTracker.track(Stat.FOLLOWED_BLOG_NOTIFICATIONS_SETTINGS_COMMENTS_ON)
                    AddOrDeleteSubscriptionPayload(
                        mNotificationUpdatedSite!!,
                        SubscriptionAction.NEW
                    )
                } else {
                    AnalyticsTracker.track(Stat.FOLLOWED_BLOG_NOTIFICATIONS_SETTINGS_COMMENTS_OFF)
                    AddOrDeleteSubscriptionPayload(
                        mNotificationUpdatedSite!!,
                        SubscriptionAction.DELETE
                    )
                }
                mDispatcher!!.dispatch(
                    AccountActionBuilder.newUpdateSubscriptionEmailCommentAction(
                        payload
                    )
                )
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSubscriptionsChanged(event: OnSubscriptionsChanged) {
        if (event.isError) {
            AppLog.e(
                AppLog.T.API,
                "NotificationsSettingsFragment.onSubscriptionsChanged: " + event.error.message
            )
        } else {
            configureFollowedBlogsSettings(mFollowedBlogsCategory, !mSearchMenuItemCollapsed)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSubscriptionUpdated(event: OnSubscriptionUpdated) {
        if (event.isError) {
            AppLog.e(
                AppLog.T.API,
                "NotificationsSettingsFragment.onSubscriptionUpdated: " + event.error.message
            )
        } else if (event.type == SubscriptionType.EMAIL_POST && mUpdateEmailPostsFirst) {
            mUpdateEmailPostsFirst = false
            mDispatcher!!.dispatch(
                AccountActionBuilder.newUpdateSubscriptionEmailPostFrequencyAction(
                    mUpdateSubscriptionFrequencyPayload
                )
            )
        } else {
            mDispatcher!!.dispatch(AccountActionBuilder.newFetchSubscriptionsAction())
        }
    }

    private fun getSubscriptionFrequencyFromString(s: String): SubscriptionFrequency {
        return if (s.equals(SubscriptionFrequency.DAILY.toString(), ignoreCase = true)) {
            AnalyticsTracker.track(Stat.FOLLOWED_BLOG_NOTIFICATIONS_SETTINGS_EMAIL_DAILY)
            SubscriptionFrequency.DAILY
        } else if (s.equals(SubscriptionFrequency.WEEKLY.toString(), ignoreCase = true)) {
            AnalyticsTracker.track(Stat.FOLLOWED_BLOG_NOTIFICATIONS_SETTINGS_EMAIL_WEEKLY)
            SubscriptionFrequency.WEEKLY
        } else {
            AnalyticsTracker.track(Stat.FOLLOWED_BLOG_NOTIFICATIONS_SETTINGS_EMAIL_INSTANTLY)
            SubscriptionFrequency.INSTANTLY
        }
    }

    private fun refreshSettings() {
        if (!hasNotificationsSettings()) {
            EventBus.getDefault()
                .post(NotificationsSettingsStatusChanged(getString(R.string.loading)))
        }
        if (hasNotificationsSettings()) {
            updateUIForNotificationsEnabledState()
        }
        if (!mAccountStore!!.hasAccessToken()) {
            return
        }
        NotificationsUtils.getPushNotificationSettings(activity, RestRequest.Listener { response ->
            AppLog.d(AppLog.T.NOTIFS, "Get settings action succeeded")
            if (!isAdded) {
                return@Listener
            }
            val settingsExisted = hasNotificationsSettings()
            if (!settingsExisted) {
                EventBus.getDefault().post(NotificationsSettingsStatusChanged(null))
            }
            val settings = PreferenceManager.getDefaultSharedPreferences(activity)
            val editor = settings.edit()
            editor.putString(
                NotificationsUtils.WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS,
                response.toString()
            )
            editor.apply()
            loadNotificationsAndUpdateUI(!settingsExisted)
            updateUIForNotificationsEnabledState()
        }, RestRequest.ErrorListener { error ->
            if (!isAdded) {
                return@ErrorListener
            }
            AppLog.e(AppLog.T.NOTIFS, "Get settings action failed", error)
            if (!hasNotificationsSettings()) {
                EventBus.getDefault().post(
                    NotificationsSettingsStatusChanged(
                        getString(R.string.error_loading_notifications)
                    )
                )
            }
        })
    }

    private fun loadNotificationsAndUpdateUI(shouldUpdateUI: Boolean) {
        val settingsJson: JSONObject
        settingsJson = try {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
                activity
            )
            JSONObject(
                sharedPreferences.getString(
                    NotificationsUtils.WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS,
                    ""
                )
            )
        } catch (e: JSONException) {
            AppLog.e(AppLog.T.NOTIFS, "Could not parse notifications settings JSON")
            return
        }
        if (mNotificationsSettings == null) {
            mNotificationsSettings = NotificationsSettings(settingsJson)
        } else {
            mNotificationsSettings!!.updateJson(settingsJson)
        }
        if (shouldUpdateUI) {
            if (mBlogsCategory == null) {
                mBlogsCategory = findPreference(
                    getString(R.string.pref_notification_blogs)
                ) as PreferenceCategory
            }
            if (mFollowedBlogsCategory == null) {
                mFollowedBlogsCategory = findPreference(
                    getString(R.string.pref_notification_blogs_followed)
                ) as PreferenceCategory
            }
            configureBlogsSettings(mBlogsCategory, false)
            configureFollowedBlogsSettings(mFollowedBlogsCategory, false)
            configureOtherSettings()
            configureWPComSettings()
        }
    }

    private fun hasNotificationsSettings(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
            activity
        )
        return sharedPreferences.contains(NotificationsUtils.WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS)
    }

    // Updates the UI for preference screens based on if notifications are enabled or not
    private fun updateUIForNotificationsEnabledState() {
        if (mTypePreferenceCategories.size == 0) {
            return
        }
        for (category in mTypePreferenceCategories) {
            if (mNotificationsEnabled && category.preferenceCount > NotificationsSettingsFragment.Companion.TYPE_COUNT) {
                category.removePreference(category.getPreference(NotificationsSettingsFragment.Companion.TYPE_COUNT))
            } else if (!mNotificationsEnabled && category.preferenceCount == NotificationsSettingsFragment.Companion.TYPE_COUNT) {
                val disabledMessage = Preference(activity)
                category.addPreference(disabledMessage)
            }
            if (category.preferenceCount >= NotificationsSettingsFragment.Companion.TYPE_COUNT
                && category.getPreference(NotificationsSettingsFragment.Companion.TYPE_COUNT - 1) != null
            ) {
                category.getPreference(NotificationsSettingsFragment.Companion.TYPE_COUNT - 1).isEnabled =
                    mNotificationsEnabled
            }
            if (category.preferenceCount > NotificationsSettingsFragment.Companion.TYPE_COUNT
                && category.getPreference(NotificationsSettingsFragment.Companion.TYPE_COUNT) != null
            ) {
                updateDisabledMessagePreference(category.getPreference(NotificationsSettingsFragment.Companion.TYPE_COUNT))
            }
        }
    }

    private fun updateDisabledMessagePreference(disabledMessagePreference: Preference) {
        disabledMessagePreference.setSummary(disabledMessageResId)
        disabledMessagePreference.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { preference: Preference? ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shouldRequestRuntimePermission()) {
                    // Request runtime permission.
                    requestPermissions(
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        WPPermissionUtils.NOTIFICATIONS_PERMISSION_REQUEST_CODE
                    )
                } else {
                    // Navigate to app settings.
                    WPPermissionUtils.showNotificationsSettings(context)
                }
                true
            }
    }

    private fun shouldRequestRuntimePermission(): Boolean {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !WPPermissionUtils.isPermissionAlwaysDenied(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ))
    }

    @get:StringRes
    private val disabledMessageResId: Int
        private get() = if (shouldRequestRuntimePermission()) {
            R.string.notifications_disabled_permission_dialog
        } else {
            R.string.notifications_disabled
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        WPPermissionUtils.setPermissionListAsked(
            activity,
            requestCode,
            permissions,
            grantResults,
            false
        )
    }

    private fun configureBlogsSettings(blogsCategory: PreferenceCategory?, showAll: Boolean) {
        if (!isAdded) {
            return
        }
        val sites: List<SiteModel>
        var trimmedQuery = ""
        if (mSearchView != null && !TextUtils.isEmpty(mSearchView!!.query)) {
            trimmedQuery = mSearchView!!.query.toString().trim { it <= ' ' }
            sites = mSiteStore!!.getSitesAccessedViaWPComRestByNameOrUrlMatching(trimmedQuery)
        } else {
            sites = mSiteStore!!.sitesAccessedViaWPComRest
        }
        mSiteCount = sites.size
        if (mSiteCount > 0) {
            Collections.sort(sites) { o1, o2 ->
                SiteUtils.getSiteNameOrHomeURL(o1).compareTo(
                    SiteUtils.getSiteNameOrHomeURL(o2), ignoreCase = true
                )
            }
        }
        val context: Context? = activity
        blogsCategory!!.removeAll()
        val maxSitesToShow: Int =
            if (showAll) NotificationsSettingsFragment.Companion.NO_MAXIMUM else NotificationsSettingsFragment.Companion.MAX_SITES_TO_SHOW_ON_FIRST_SCREEN
        var count = 0
        for (site in sites) {
            if (context == null) {
                return
            }
            count++
            if (maxSitesToShow != NotificationsSettingsFragment.Companion.NO_MAXIMUM && count > maxSitesToShow) {
                break
            }
            val prefScreen = preferenceManager.createPreferenceScreen(context)
            prefScreen.title = SiteUtils.getSiteNameOrHomeURL(site)
            prefScreen.summary = SiteUtils.getHomeURLOrHostName(site)
            addPreferencesForPreferenceScreen(
                prefScreen,
                NotificationsSettings.Channel.BLOGS,
                site.siteId
            )
            blogsCategory.addPreference(prefScreen)
        }

        // Add a message in a preference if there are no matching search results
        if (mSiteCount == 0 && !TextUtils.isEmpty(trimmedQuery)) {
            val searchResultsPref = Preference(context)
            searchResultsPref.summary =
                String.format(getString(R.string.notifications_no_search_results), trimmedQuery)
            blogsCategory.addPreference(searchResultsPref)
        }
        if (mSiteCount > maxSitesToShow && !showAll) {
            // append a "view all" option
            appendViewAllSitesOption(context, getString(R.string.pref_notification_blogs), false)
        }
        updateSearchMenuVisibility()
    }

    private fun configureFollowedBlogsSettings(
        blogsCategory: PreferenceCategory?,
        showAll: Boolean
    ) {
        if (!isAdded || blogsCategory == null) {
            return
        }
        val models: List<PreferenceModel>
        var query = ""
        if (mSearchView != null && !TextUtils.isEmpty(mSearchView!!.query)) {
            query = mSearchView!!.query.toString().trim { it <= ' ' }
            models = mFollowedBlogsProvider!!.getAllFollowedBlogs(query)
        } else {
            models = mFollowedBlogsProvider!!.getAllFollowedBlogs(null)
        }
        val context: Context? = activity
        blogsCategory.removeAll()
        val maxSitesToShow: Int =
            if (showAll) NotificationsSettingsFragment.Companion.NO_MAXIMUM else NotificationsSettingsFragment.Companion.MAX_SITES_TO_SHOW_ON_FIRST_SCREEN
        mSubscriptionCount = 0
        if (models.size > 0) {
            Collections.sort(models) { (title): PreferenceModel, (title1): PreferenceModel ->
                title.compareTo(
                    title1, ignoreCase = true
                )
            }
        }
        for ((title, summary, blogId, clickHandler) in models) {
            if (context == null) {
                return
            }
            mSubscriptionCount++
            if (!showAll && mSubscriptionCount > maxSitesToShow) {
                break
            }
            val prefScreen = preferenceManager.createPreferenceScreen(context)
            prefScreen.title = title
            prefScreen.summary = summary
            if (clickHandler != null) {
                prefScreen.onPreferenceClickListener =
                    Preference.OnPreferenceClickListener { preference: Preference? ->
                        mNotificationUpdatedSite = blogId
                        mPreviousNotifyPosts = clickHandler.shouldNotifyPosts
                        mPreviousEmailPosts = clickHandler.shouldEmailPosts
                        mPreviousEmailPostsFrequency = clickHandler.emailPostFrequency
                        mPreviousEmailComments = clickHandler.shouldEmailComments
                        val dialog = NotificationSettingsFollowedDialog()
                        val args = Bundle()
                        args.putBoolean(
                            NotificationSettingsFollowedDialog.ARG_NOTIFICATION_POSTS,
                            mPreviousNotifyPosts
                        )
                        args.putBoolean(
                            NotificationSettingsFollowedDialog.ARG_EMAIL_POSTS,
                            mPreviousEmailPosts
                        )
                        args.putString(
                            NotificationSettingsFollowedDialog.ARG_EMAIL_POSTS_FREQUENCY,
                            mPreviousEmailPostsFrequency
                        )
                        args.putBoolean(
                            NotificationSettingsFollowedDialog.ARG_EMAIL_COMMENTS,
                            mPreviousEmailComments
                        )
                        dialog.arguments = args
                        dialog.setTargetFragment(
                            this@NotificationsSettingsFragment,
                            RequestCodes.NOTIFICATION_SETTINGS
                        )
                        dialog.show(childFragmentManager, NotificationSettingsFollowedDialog.TAG)
                        true
                    }
            } else {
                prefScreen.isEnabled = false
            }
            blogsCategory.addPreference(prefScreen)
        }

        // Add message if there are no matching search results.
        if (mSubscriptionCount == 0 && !TextUtils.isEmpty(query)) {
            val searchResultsPref = Preference(context)
            searchResultsPref.summary =
                String.format(getString(R.string.notifications_no_search_results), query)
            blogsCategory.addPreference(searchResultsPref)
        }

        // Add view all entry when more sites than maximum to show.
        if (!showAll && mSubscriptionCount > maxSitesToShow) {
            appendViewAllSitesOption(
                context,
                getString(R.string.pref_notification_blogs_followed),
                true
            )
        }
        updateSearchMenuVisibility()
    }

    private fun appendViewAllSitesOption(
        context: Context?,
        preference: String,
        isFollowed: Boolean
    ) {
        val blogsCategory = findPreference(preference) as PreferenceCategory
        val prefScreen = preferenceManager.createPreferenceScreen(context)
        prefScreen.setTitle(if (isFollowed) R.string.notification_settings_item_your_sites_all_followed_sites else R.string.notification_settings_item_your_sites_all_your_sites)
        addSitesForViewAllSitesScreen(prefScreen, isFollowed)
        blogsCategory.addPreference(prefScreen)
    }

    private fun updateSearchMenuVisibility() {
        // Show the search menu item in the toolbar if we have enough sites
        if (mSearchMenuItem != null) {
            mSearchMenuItem!!.isVisible =
                (mSiteCount > NotificationsSettingsFragment.Companion.SITE_SEARCH_VISIBILITY_COUNT
                        || mSubscriptionCount > NotificationsSettingsFragment.Companion.SITE_SEARCH_VISIBILITY_COUNT)
        }
    }

    private fun configureOtherSettings() {
        val otherBlogsScreen = findPreference(
            getString(R.string.pref_notification_other_blogs)
        ) as PreferenceScreen
        addPreferencesForPreferenceScreen(otherBlogsScreen, NotificationsSettings.Channel.OTHER, 0)
    }

    private fun configureWPComSettings() {
        val otherPreferenceCategory = findPreference(
            getString(R.string.pref_notification_other_category)
        ) as PreferenceCategory
        val devicePreference = NotificationsSettingsDialogPreference(
            activity,
            null,
            NotificationsSettings.Channel.WPCOM,
            NotificationsSettings.Type.DEVICE,
            0,
            mNotificationsSettings,
            mOnSettingsChangedListener
        )
        devicePreference.setTitle(R.string.notification_settings_item_other_account_emails)
        devicePreference.setDialogTitle(R.string.notification_settings_item_other_account_emails)
        devicePreference.setSummary(R.string.notification_settings_item_other_account_emails_summary)
        otherPreferenceCategory.addPreference(devicePreference)
    }

    private fun addPreferencesForPreferenceScreen(
        preferenceScreen: PreferenceScreen,
        channel: NotificationsSettings.Channel,
        blogId: Long
    ) {
        val context = activity ?: return
        val rootCategory = PreferenceCategory(context)
        rootCategory.setTitle(R.string.notification_types)
        preferenceScreen.addPreference(rootCategory)
        val timelinePreference = NotificationsSettingsDialogPreference(
            context,
            null,
            channel,
            NotificationsSettings.Type.TIMELINE,
            blogId,
            mNotificationsSettings,
            mOnSettingsChangedListener
        )
        setPreferenceIcon(timelinePreference, R.drawable.ic_bell_white_24dp)
        timelinePreference.setTitle(R.string.notifications_tab)
        timelinePreference.setDialogTitle(R.string.notifications_tab)
        timelinePreference.setSummary(R.string.notifications_tab_summary)
        rootCategory.addPreference(timelinePreference)
        val emailPreference = NotificationsSettingsDialogPreference(
            context,
            null,
            channel,
            NotificationsSettings.Type.EMAIL,
            blogId,
            mNotificationsSettings,
            mOnSettingsChangedListener
        )
        setPreferenceIcon(emailPreference, R.drawable.ic_mail_white_24dp)
        emailPreference.setTitle(R.string.email)
        emailPreference.setDialogTitle(R.string.email)
        emailPreference.setSummary(R.string.notifications_email_summary)
        rootCategory.addPreference(emailPreference)
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        val deviceID = settings.getString(NotificationsUtils.WPCOM_PUSH_DEVICE_SERVER_ID, null)
        if (!TextUtils.isEmpty(deviceID)) {
            val devicePreference = NotificationsSettingsDialogPreference(
                context,
                null,
                channel,
                NotificationsSettings.Type.DEVICE,
                blogId,
                mNotificationsSettings,
                mOnSettingsChangedListener,
                mBloggingRemindersProvider
            )
            setPreferenceIcon(devicePreference, R.drawable.ic_phone_white_24dp)
            devicePreference.setTitle(R.string.app_notifications)
            devicePreference.setDialogTitle(R.string.app_notifications)
            devicePreference.setSummary(R.string.notifications_push_summary)
            devicePreference.isEnabled = mNotificationsEnabled
            rootCategory.addPreference(devicePreference)
        }
        mTypePreferenceCategories.add(rootCategory)
    }

    private fun setPreferenceIcon(
        preference: NotificationsSettingsDialogPreference,
        @DrawableRes drawableRes: Int
    ) {
        preference.setIcon(drawableRes)
        preference.icon.setTintMode(PorterDuff.Mode.SRC_IN)
        preference.icon.setTintList(preference.context.getColorStateListFromAttribute(R.attr.wpColorOnSurfaceMedium))
    }

    private fun addSitesForViewAllSitesScreen(
        preferenceScreen: PreferenceScreen,
        isFollowed: Boolean
    ) {
        val context = activity ?: return
        val rootCategory = PreferenceCategory(context)
        rootCategory.setTitle(if (isFollowed) R.string.notification_settings_category_followed_sites else R.string.notification_settings_category_your_sites)
        preferenceScreen.addPreference(rootCategory)
        if (isFollowed) {
            configureFollowedBlogsSettings(rootCategory, true)
        } else {
            configureBlogsSettings(rootCategory, true)
        }
    }

    private val mOnSettingsChangedListener =
        OnNotificationsSettingsChangedListener { channel, type, blogId, newValues ->
            if (!isAdded) {
                return@OnNotificationsSettingsChangedListener
            }

            // Construct a new settings JSONObject to send back to WP.com
            val settingsObject = JSONObject()
            when (channel) {
                NotificationsSettings.Channel.BLOGS -> try {
                    val blogObject = JSONObject()
                    blogObject.put(NotificationsSettings.KEY_BLOG_ID, blogId)
                    val blogsArray = JSONArray()
                    if (type == NotificationsSettings.Type.DEVICE) {
                        newValues.put(NotificationsSettings.KEY_DEVICE_ID, mDeviceId!!.toLong())
                        val devicesArray = JSONArray()
                        devicesArray.put(newValues)
                        blogObject.put(NotificationsSettings.KEY_DEVICES, devicesArray)
                        blogsArray.put(blogObject)
                    } else {
                        blogObject.put(type.toString(), newValues)
                        blogsArray.put(blogObject)
                    }
                    settingsObject.put(NotificationsSettings.KEY_BLOGS, blogsArray)
                } catch (e: JSONException) {
                    AppLog.e(AppLog.T.NOTIFS, "Could not build notification settings object")
                }

                NotificationsSettings.Channel.OTHER -> try {
                    val otherObject = JSONObject()
                    if (type == NotificationsSettings.Type.DEVICE) {
                        newValues.put(NotificationsSettings.KEY_DEVICE_ID, mDeviceId!!.toLong())
                        val devicesArray = JSONArray()
                        devicesArray.put(newValues)
                        otherObject.put(NotificationsSettings.KEY_DEVICES, devicesArray)
                    } else {
                        otherObject.put(type.toString(), newValues)
                    }
                    settingsObject.put(NotificationsSettings.KEY_OTHER, otherObject)
                } catch (e: JSONException) {
                    AppLog.e(AppLog.T.NOTIFS, "Could not build notification settings object")
                }

                NotificationsSettings.Channel.WPCOM -> try {
                    settingsObject.put(NotificationsSettings.KEY_WPCOM, newValues)
                } catch (e: JSONException) {
                    AppLog.e(AppLog.T.NOTIFS, "Could not build notification settings object")
                }
            }
            if (settingsObject.length() > 0) {
                getRestClientUtilsV1_1()
                    .post("/me/notifications/settings", settingsObject, null, null, null)
            }
        }

    override fun onPreferenceTreeClick(
        preferenceScreen: PreferenceScreen,
        preference: Preference
    ): Boolean {
        super.onPreferenceTreeClick(preferenceScreen, preference)
        if (preference is PreferenceScreen) {
            addToolbarToDialog(preference)
            AnalyticsTracker.track(Stat.NOTIFICATION_SETTINGS_STREAMS_OPENED)
        } else {
            AnalyticsTracker.track(Stat.NOTIFICATION_SETTINGS_DETAILS_OPENED)
        }
        return false
    }

    private fun addToolbarToDialog(preference: Preference) {
        val prefDialog = (preference as PreferenceScreen).dialog
        if (prefDialog != null) {
            val title = preference.getTitle().toString()
            WPActivityUtils.addToolbarToDialog(this, prefDialog, title)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (key == getString(R.string.pref_key_notification_pending_drafts)) {
            if (activity != null) {
                val prefs = PreferenceManager.getDefaultSharedPreferences(
                    activity
                )
                val shouldNotifyOfPendingDrafts =
                    prefs.getBoolean("wp_pref_notification_pending_drafts", true)
                if (shouldNotifyOfPendingDrafts) {
                    AnalyticsTracker.track(Stat.NOTIFICATION_PENDING_DRAFTS_SETTINGS_ENABLED)
                } else {
                    AnalyticsTracker.track(Stat.NOTIFICATION_PENDING_DRAFTS_SETTINGS_DISABLED)
                }
            }
        } else if (key == getString(R.string.wp_pref_custom_notification_sound)) {
            val defaultPath =
                getString(R.string.notification_settings_item_sights_and_sounds_choose_sound_default)
            val value = sharedPreferences.getString(key, defaultPath)
            if (value!!.trim { it <= ' ' }.lowercase().startsWith("file://")) {
                // sound path begins with 'file://` which will lead to FileUriExposedException when used. Revert to
                //  default and let the user know.
                AppLog.w(
                    AppLog.T.NOTIFS,
                    "Notification sound starts with unacceptable scheme: $value"
                )
                val context = WordPress.getContext()
                if (context != null) {
                    // let the user know we won't be using the selected sound
                    ToastUtils.showToast(
                        context,
                        R.string.notification_sound_has_invalid_path,
                        ToastUtils.Duration.LONG
                    )
                }
            }
        }
    }

    private val appCompatActivity: AppCompatActivity?
        private get() {
            val activity = activity
            return if (activity is AppCompatActivity) {
                activity
            } else null
        }
    private val mBloggingRemindersProvider: BloggingRemindersProvider =
        object : BloggingRemindersProvider {
            override fun getSummary(blogId: Long): String? {
                val uiString = mBloggingRemindersSummariesBySiteId[blogId]
                return if (uiString != null) mUiHelpers!!.getTextOfUiString(context, uiString)
                    .toString() else null
            }

            override fun onClick(blogId: Long) {
                mBloggingRemindersViewModel!!.onNotificationSettingsItemClicked(blogId)
            }
        }

    private fun initBloggingReminders() {
        if (!isAdded) {
            return
        }
        val appCompatActivity = appCompatActivity
        if (appCompatActivity != null) {
            mBloggingRemindersViewModel = ViewModelProvider(appCompatActivity, mViewModelFactory!!)
                .get(BloggingRemindersViewModel::class.java)
            observeBottomSheet(
                mBloggingRemindersViewModel!!.isBottomSheetShowing,
                appCompatActivity,
                NotificationsSettingsFragment.Companion.BLOGGING_REMINDERS_BOTTOM_SHEET_TAG
            ) { appCompatActivity.supportFragmentManager }
            mBloggingRemindersViewModel!!.notificationsSettingsUiState
                .observe(appCompatActivity) { map ->
                    mBloggingRemindersSummariesBySiteId.putAll(map)
                }
        }
    }

    companion object {
        private const val KEY_SEARCH_QUERY = "search_query"
        private const val SITE_SEARCH_VISIBILITY_COUNT = 15

        // The number of notification types we support (e.g. timeline, email, mobile)
        private const val TYPE_COUNT = 3
        private const val NO_MAXIMUM = -1
        private const val MAX_SITES_TO_SHOW_ON_FIRST_SCREEN = 3
        private const val BLOGGING_REMINDERS_BOTTOM_SHEET_TAG = "blogging-reminders-dialog-tag"
    }
}
