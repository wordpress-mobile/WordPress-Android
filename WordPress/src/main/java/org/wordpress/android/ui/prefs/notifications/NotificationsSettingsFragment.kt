package org.wordpress.android.ui.prefs.notifications

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.preference.DialogPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
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
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnSubscriptionUpdated
import org.wordpress.android.fluxc.store.AccountStore.OnSubscriptionsChanged
import org.wordpress.android.fluxc.store.AccountStore.SubscriptionType
import org.wordpress.android.fluxc.store.AccountStore.UpdateSubscriptionPayload
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.models.NotificationsSettings
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.WPLaunchActivity
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsSettingsStatusChanged
import org.wordpress.android.ui.notifications.utils.NotificationsUtils
import org.wordpress.android.ui.prefs.notifications.FollowedBlogsProvider.PreferenceModel
import org.wordpress.android.ui.prefs.notifications.NotificationsSettingsDialogPreference.OnNotificationsSettingsChangedListener
import org.wordpress.android.ui.prefs.notifications.NotificationsSettingsMySitesFragment.Companion.ARG_IS_FOLLOWED
import org.wordpress.android.ui.prefs.notifications.NotificationsSettingsTypesFragment.Companion.ARG_BLOG_ID
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.WPPermissionUtils
import javax.inject.Inject

class NotificationsSettingsFragment : PreferenceFragmentCompat(), NotificationsMySitesSettingsFragment,
    OnSharedPreferenceChangeListener {
    private var mNotificationsSettings: NotificationsSettings? = null
    private var mSearchView: SearchView? = null
    private var mSearchMenuItem: MenuItem? = null
    private var mSearchMenuItemCollapsed = true
    private var mDeviceId: String? = null
    private var mRestoredQuery: String? = null
    private var mNotificationsEnabled = false
    override var mNotificationUpdatedSite: String? = null
    override var mPreviousEmailPostsFrequency: String? = null
    override var mUpdateSubscriptionFrequencyPayload: UpdateSubscriptionPayload? = null
    override var mPreviousEmailComments = false
    override var mPreviousEmailPosts = false
    override var mPreviousNotifyPosts = false
    override var mUpdateEmailPostsFirst = false
    private var mSiteCount = 0
    private var mSubscriptionCount = 0
    private val mTypePreferenceCategories: MutableList<PreferenceCategory> = ArrayList()
    private var mBlogsCategory: PreferenceCategory? = null
    private var mFollowedBlogsCategory: PreferenceCategory? = null

    @Inject
    lateinit var mAccountStore: AccountStore

    @Inject
    lateinit var mSiteStore: SiteStore

    @Inject
    override lateinit var mDispatcher: Dispatcher

    @Inject
    lateinit var mFollowedBlogsProvider: FollowedBlogsProvider

    @Inject
    lateinit var mBuildConfigWrapper: BuildConfigWrapper

    @Inject
    lateinit var mJetpackBrandingUtils: JetpackBrandingUtils

    @Inject
    lateinit var mUiHelpers: UiHelpers

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        addPreferencesFromResource(R.xml.notifications_settings)
        setHasOptionsMenu(true)
        removeSightAndSoundsForAPI26()
        removeFollowedBlogsPreferenceForIfDisabled()

        // Bump Analytics
        if (savedInstanceState == null) {
            AnalyticsTracker.track(Stat.NOTIFICATION_SETTINGS_LIST_OPENED)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) = Unit

    @Suppress("DEPRECATION")
    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is NotificationsSettingsDialogPreferenceX) {
            if (parentFragmentManager.findFragmentByTag(NotificationsSettingsDialogFragment.TAG) != null) {
                return
            }
            with(preference) {
                NotificationsSettingsDialogFragment(
                    channel = channel,
                    type = type,
                    blogId = blogId,
                    settings = settings,
                    onNotificationsSettingsChangedListener = listener,
                    bloggingRemindersProvider = bloggingRemindersProvider,
                    title = context.getString(dialogTitleRes)
                ).apply {
                    setTargetFragment(
                        this@NotificationsSettingsFragment,
                        RequestCodes.NOTIFICATION_SETTINGS
                    )
                }.show(
                    parentFragmentManager,
                    NotificationsSettingsDialogFragment.TAG
                )
            }
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun removeSightAndSoundsForAPI26() {
        // on API26 we removed the Sight & Sounds category altogether, as it can always be
        // overridden by the user in the Device settings, and the settings here
        // wouldn't either reflect nor have any effect anyway.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val (preferenceScreen, categorySightsAndSounds) =
                getPreferenceScreenAndCategory(R.string.pref_notification_sights_sounds)

            if (categorySightsAndSounds != null) {
                preferenceScreen?.removePreference(categorySightsAndSounds)
            }
        }
    }

    private fun removeFollowedBlogsPreferenceForIfDisabled() {
        if (!mBuildConfigWrapper.isFollowedSitesSettingsEnabled) {
            val (preferenceScreen, categoryFollowedBlogs) =
                getPreferenceScreenAndCategory(R.string.pref_notification_blogs_followed)

            if (categoryFollowedBlogs != null) {
                preferenceScreen?.removePreference(categoryFollowedBlogs)
            }
        }
    }

    private fun getPreferenceScreenAndCategory(pref: Int): Pair<PreferenceScreen?, PreferenceCategory?> {
        val preferenceScreen =
            findPreference(requireActivity().getString(R.string.wp_pref_notifications_root)) as PreferenceScreen?
        val requiredPreference = preferenceScreen
            ?.findPreference(requireActivity().getString(pref)) as PreferenceCategory?
        return Pair(preferenceScreen, requiredPreference)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val isLoggedIn = mAccountStore.hasAccessToken()
        if (!isLoggedIn) {
            // Not logged in users can start Notification Settings from App info > Notifications menu.
            // If there isn't a logged in user, just show the entry screen.
            val intent = Intent(context, WPLaunchActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            requireActivity().finish()
            return
        }
        val settings = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        mDeviceId = settings.getString(NotificationsUtils.WPCOM_PUSH_DEVICE_SERVER_ID, "")
        if (hasNotificationsSettings()) {
            loadNotificationsAndUpdateUI(true)
        }
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SEARCH_QUERY)) {
            mRestoredQuery = savedInstanceState.getString(KEY_SEARCH_QUERY)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val lv = view.findViewById<View>(R.id.list) as ListView?
        if (lv != null) {
            ViewCompat.setNestedScrollingEnabled(lv, true)
            addJetpackBadgeAsFooterIfEnabled(lv)
        }
    }

    private fun addJetpackBadgeAsFooterIfEnabled(listView: ListView) {
        if (mJetpackBrandingUtils.shouldShowJetpackBranding()) {
            val screen: JetpackPoweredScreen = JetpackPoweredScreen.WithDynamicText.NOTIFICATIONS_SETTINGS
            val inflater = LayoutInflater.from(context)
            val binding: JetpackBadgeFooterBinding = JetpackBadgeFooterBinding.inflate(inflater)
            binding.footerJetpackBadge.jetpackPoweredBadge.text = mUiHelpers.getTextOfUiString(
                requireContext(),
                mJetpackBrandingUtils.getBrandingTextForScreen(screen)
            )
            if (mJetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) {
                binding.footerJetpackBadge.jetpackPoweredBadge.setOnClickListener {
                    mJetpackBrandingUtils.trackBadgeTapped(screen)
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
        mDispatcher.register(this)
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onResume() {
        super.onResume()
        mNotificationsEnabled = NotificationsUtils.isNotificationsEnabled(activity)
        setToolbarTitle()
        refreshSettings()
    }

    override fun onStop() {
        super.onStop()
        mDispatcher.unregister(this)
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.notifications_settings, menu)
        mSearchMenuItem = menu.findItem(R.id.menu_notifications_settings_search)
        mSearchView = mSearchMenuItem?.actionView as SearchView?
        mSearchView?.queryHint = getString(R.string.search_sites)
        mBlogsCategory = findPreference(
            getString(R.string.pref_notification_blogs)
        ) as PreferenceCategory?
        mFollowedBlogsCategory = findPreference(
            getString(R.string.pref_notification_blogs_followed)
        ) as PreferenceCategory?
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
            mSearchView?.setQuery(mRestoredQuery, true)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (mSearchView != null && !TextUtils.isEmpty(mSearchView!!.query)) {
            outState.putString(KEY_SEARCH_QUERY, mSearchView!!.query.toString())
        }
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RequestCodes.NOTIFICATION_SETTINGS) {
            this.onMySiteSettingsChanged(data)
        } else if (requestCode == RequestCodes.NOTIFICATION_SETTINGS_ALERT_RINGTONE && data != null) {
            val ringtone: Uri? = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            val settings = PreferenceManager.getDefaultSharedPreferences(requireContext())
            settings.edit {
                putString(
                    getString(R.string.wp_pref_custom_notification_sound),
                    (ringtone ?: "").toString()
                )
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSubscriptionsChanged(event: OnSubscriptionsChanged) {
        if (event.isError) {
            AppLog.e(AppLog.T.API, "NotificationsSettingsFragment.onSubscriptionsChanged: " + event.error.message)
        } else {
            configureFollowedBlogsSettings(mFollowedBlogsCategory, !mSearchMenuItemCollapsed)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSubscriptionUpdated(event: OnSubscriptionUpdated) {
        if (event.isError) {
            AppLog.e(AppLog.T.API, "NotificationsSettingsFragment.onSubscriptionUpdated: " + event.error.message)
        } else if (event.type == SubscriptionType.EMAIL_POST && mUpdateEmailPostsFirst) {
            mUpdateEmailPostsFirst = false
            mDispatcher.dispatch(
                AccountActionBuilder.newUpdateSubscriptionEmailPostFrequencyAction(
                    mUpdateSubscriptionFrequencyPayload
                )
            )
        } else {
            mDispatcher.dispatch(AccountActionBuilder.newFetchSubscriptionsAction())
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
        if (!mAccountStore.hasAccessToken()) {
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
            val settings = PreferenceManager.getDefaultSharedPreferences(
                requireActivity()
            )
            val editor = settings.edit()
            editor.putString(NotificationsUtils.WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS, response.toString())
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
        val settingsJson: JSONObject = try {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
                requireActivity()
            )
            JSONObject(
                sharedPreferences.getString(NotificationsUtils.WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS, "")!!
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
                ) as PreferenceCategory?
            }
            if (mFollowedBlogsCategory == null) {
                mFollowedBlogsCategory = findPreference(
                    getString(R.string.pref_notification_blogs_followed)
                ) as PreferenceCategory?
            }
            configureBlogsSettings(mBlogsCategory, false)
            configureFollowedBlogsSettings(mFollowedBlogsCategory, false)
            configureOtherSettings()
            configureWPComSettings()
        }
    }

    private fun hasNotificationsSettings(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
            requireActivity()
        )
        return sharedPreferences.contains(NotificationsUtils.WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS)
    }

    // Updates the UI for preference screens based on if notifications are enabled or not
    private fun updateUIForNotificationsEnabledState() {
        if (mTypePreferenceCategories.size == 0) {
            return
        }
        for (category in mTypePreferenceCategories) {
            if (mNotificationsEnabled && category.preferenceCount > TYPE_COUNT) {
                category.removePreference(category.getPreference(TYPE_COUNT))
            } else if (!mNotificationsEnabled && category.preferenceCount == TYPE_COUNT) {
                val disabledMessage = Preference(requireActivity())
                category.addPreference(disabledMessage)
            }
            if (category.preferenceCount >= TYPE_COUNT
            ) {
                category.getPreference(TYPE_COUNT - 1).isEnabled =
                    mNotificationsEnabled
            }
            if (category.preferenceCount > TYPE_COUNT
            ) {
                updateDisabledMessagePreference(category.getPreference(TYPE_COUNT))
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun updateDisabledMessagePreference(disabledMessagePreference: Preference) {
        disabledMessagePreference.setSummary(disabledMessageResId)
        disabledMessagePreference.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shouldRequestRuntimePermission()) {
                    // Request runtime permission.
                    requestPermissions(
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        WPPermissionUtils.NOTIFICATIONS_PERMISSION_REQUEST_CODE
                    )
                } else {
                    // Navigate to app settings.
                    WPPermissionUtils.showNotificationsSettings(requireContext())
                }
                true
            }
    }

    private fun shouldRequestRuntimePermission(): Boolean {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !WPPermissionUtils.isPermissionAlwaysDenied(requireActivity(), Manifest.permission.POST_NOTIFICATIONS))
    }

    @get:StringRes
    private val disabledMessageResId: Int
        get() = if (shouldRequestRuntimePermission()) {
            R.string.notifications_disabled_permission_dialog
        } else {
            R.string.notifications_disabled
        }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        WPPermissionUtils.setPermissionListAsked(requireActivity(), requestCode, permissions, grantResults, false)
    }

    private fun configureBlogsSettings(blogsCategory: PreferenceCategory?, showAll: Boolean) {
        if (!isAdded) {
            return
        }
        val sites: List<SiteModel>
        var trimmedQuery = ""
        if (mSearchView != null && !TextUtils.isEmpty(mSearchView!!.query)) {
            trimmedQuery = mSearchView!!.query.toString().trim { it <= ' ' }
            sites = mSiteStore.getSitesAccessedViaWPComRestByNameOrUrlMatching(trimmedQuery)
        } else {
            sites = mSiteStore.sitesAccessedViaWPComRest
        }
        mSiteCount = sites.size
        if (mSiteCount > 0) {
            sites.sortedWith { o1, o2 ->
                SiteUtils.getSiteNameOrHomeURL(o1)
                    .compareTo(SiteUtils.getSiteNameOrHomeURL(o2), ignoreCase = true)
            }
        }
        blogsCategory!!.removeAll()
        val maxSitesToShow = if (showAll) NO_MAXIMUM else MAX_SITES_TO_SHOW_ON_FIRST_SCREEN

        setBlogsPreferenceScreen(sites, maxSitesToShow, blogsCategory)

        // Add a message in a preference if there are no matching search results
        if (mSiteCount == 0 && !TextUtils.isEmpty(trimmedQuery)) {
            val searchResultsPref = Preference(requireContext())
            searchResultsPref.summary =
                String.format(getString(R.string.notifications_no_search_results), trimmedQuery)
            blogsCategory.addPreference(searchResultsPref)
        }
        if (mSiteCount > maxSitesToShow && !showAll) {
            // append a "view all" option
            appendViewAllSitesOption(getString(R.string.pref_notification_blogs), false)
        }
        updateSearchMenuVisibility()
    }

    private fun setBlogsPreferenceScreen(sites: List<SiteModel>, maxSitesToShow: Int,
                                         blogsCategory: PreferenceCategory) {
        val context: Context? = activity
        var count = 0
        for (site in sites) {
            if (context == null) {
                return
            }
            count++
            if (maxSitesToShow != NO_MAXIMUM && count > maxSitesToShow) {
                break
            }
            val prefScreen = preferenceManager.createPreferenceScreen(context)
            prefScreen.title = SiteUtils.getSiteNameOrHomeURL(site)
            prefScreen.summary = SiteUtils.getHomeURLOrHostName(site)
            prefScreen.extras.apply {
                putLong(ARG_BLOG_ID, site.siteId)
                putInt(
                    NotificationsSettingsTypesFragment.ARG_NOTIFICATION_CHANNEL,
                    NotificationsSettings.Channel.BLOGS.ordinal
                )
            }
            prefScreen.fragment = NotificationsSettingsTypesFragment::class.qualifiedName
            blogsCategory.addPreference(prefScreen)
        }
    }

    private fun configureFollowedBlogsSettings(blogsCategory: PreferenceCategory?, showAll: Boolean) {
        if (!isAdded || blogsCategory == null)
            return

        var models: List<PreferenceModel>
        var query = ""
        if (mSearchView != null && !TextUtils.isEmpty(mSearchView!!.query)) {
            query = mSearchView!!.query.toString().trim { it <= ' ' }
            models = mFollowedBlogsProvider.getAllFollowedBlogs(query)
        } else {
            models = mFollowedBlogsProvider.getAllFollowedBlogs(null)
        }
        blogsCategory.removeAll()

        val maxSitesToShow = if (showAll) NO_MAXIMUM else MAX_SITES_TO_SHOW_ON_FIRST_SCREEN
        mSubscriptionCount = 0

        models = models.sortedWith { (title): PreferenceModel, (otherTitle): PreferenceModel ->
            title.compareTo(otherTitle, ignoreCase = true)
        }

        setFollowedBlogsPreferenceScreen(models, maxSitesToShow, showAll, blogsCategory)

        // Add message if there are no matching search results.
        if (mSubscriptionCount == 0 && !TextUtils.isEmpty(query)) {
            val searchResultsPref = Preference(requireContext())
            searchResultsPref.summary = String.format(getString(R.string.notifications_no_search_results), query)
            blogsCategory.addPreference(searchResultsPref)
        }

        // Add view all entry when more sites than maximum to show.
        if (!showAll && mSubscriptionCount > maxSitesToShow) {
            appendViewAllSitesOption(getString(R.string.pref_notification_blogs_followed), true)
        }
        updateSearchMenuVisibility()
    }

    @Suppress("DEPRECATION")
    private fun setFollowedBlogsPreferenceScreen(models: List<PreferenceModel>, maxSitesToShow: Int, showAll: Boolean,
                                                 blogsCategory: PreferenceCategory) {
        val context: Context? = activity
        for ((title, summary, blogId, clickHandler) in models) {
            if (context == null)
                return
            mSubscriptionCount++
            if (!showAll && mSubscriptionCount > maxSitesToShow)
                break
            val prefScreen = preferenceManager.createPreferenceScreen(context)
            prefScreen.title = title
            prefScreen.summary = summary
            if (clickHandler != null) {
                prefScreen.onPreferenceClickListener =
                    Preference.OnPreferenceClickListener {
                        mNotificationUpdatedSite = blogId
                        mPreviousNotifyPosts = clickHandler.shouldNotifyPosts
                        mPreviousEmailPosts = clickHandler.shouldEmailPosts
                        mPreviousEmailPostsFrequency = clickHandler.emailPostFrequency
                        mPreviousEmailComments = clickHandler.shouldEmailComments
                        val dialog = NotificationSettingsFollowedDialog()
                        val args = Bundle().apply {
                            putBoolean(NotificationSettingsFollowedDialog.ARG_NOTIFICATION_POSTS, mPreviousNotifyPosts)
                            putBoolean(NotificationSettingsFollowedDialog.ARG_EMAIL_POSTS, mPreviousEmailPosts)
                            putString(NotificationSettingsFollowedDialog.ARG_EMAIL_POSTS_FREQUENCY,
                                mPreviousEmailPostsFrequency)
                            putBoolean(NotificationSettingsFollowedDialog.ARG_EMAIL_COMMENTS, mPreviousEmailComments)
                        }
                        dialog.arguments = args
                        dialog.setTargetFragment(this@NotificationsSettingsFragment,
                            RequestCodes.NOTIFICATION_SETTINGS)
                        dialog.show(parentFragmentManager, NotificationSettingsFollowedDialog.TAG)
                        true
                    }
            } else {
                prefScreen.isEnabled = false
            }
            blogsCategory.addPreference(prefScreen)
        }
    }

    private fun appendViewAllSitesOption(preference: String, isFollowed: Boolean) {
        val blogsCategory = findPreference(preference) as PreferenceCategory?
        val prefScreen = preferenceManager.createPreferenceScreen(requireContext())
        prefScreen.fragment = NotificationsSettingsMySitesFragment::class.qualifiedName
        prefScreen.setTitle(
            if (isFollowed)
                R.string.notification_settings_item_your_sites_all_followed_sites
            else
                R.string.notification_settings_item_your_sites_all_your_sites
        )
        prefScreen.extras.apply {
            putBoolean(ARG_IS_FOLLOWED, isFollowed)
        }
        addSitesForViewAllSitesScreen(prefScreen, isFollowed)
        blogsCategory?.addPreference(prefScreen)
    }

    private fun updateSearchMenuVisibility() {
        // Show the search menu item in the toolbar if we have enough sites
        if (mSearchMenuItem != null) {
            mSearchMenuItem!!.isVisible = (mSiteCount > SITE_SEARCH_VISIBILITY_COUNT
                    || mSubscriptionCount > SITE_SEARCH_VISIBILITY_COUNT)
        }
    }

    private fun configureOtherSettings() {
        val otherBlogsScreen = findPreference(
            getString(R.string.pref_notification_other_blogs)
        ) as PreferenceScreen?
        otherBlogsScreen?.let {
            it.extras.apply {
                putLong(ARG_BLOG_ID, 0)
                putInt(
                    NotificationsSettingsTypesFragment.ARG_NOTIFICATION_CHANNEL,
                    NotificationsSettings.Channel.OTHER.ordinal
                )
            }
            it.fragment = NotificationsSettingsTypesFragment::class.qualifiedName
        }
    }

    private fun configureWPComSettings() {
        val otherPreferenceCategory = findPreference(
            getString(R.string.pref_notification_other_category)
        ) as PreferenceCategory?

        // Remove previously configured preference.
        val previouslyConfiguredPreference = otherPreferenceCategory?.findPreference(
            getString(R.string.notification_settings_item_other_account_emails)
        ) as DialogPreference?
        if (previouslyConfiguredPreference != null) {
            otherPreferenceCategory?.removePreference(previouslyConfiguredPreference)
        }

        // Add the preference back with updated details
        val devicePreference = NotificationsSettingsDialogPreferenceX(
            context = requireContext(),
            attrs = null,
            channel = NotificationsSettings.Channel.WPCOM,
            type = NotificationsSettings.Type.DEVICE,
            blogId = 0,
            settings = mNotificationsSettings!!,
            listener = mOnSettingsChangedListener,
            dialogTitleRes = R.string.notification_settings_item_other_account_emails
        ).apply {
            setTitle(R.string.notification_settings_item_other_account_emails)
            key = getString(R.string.notification_settings_item_other_account_emails)
            setSummary(R.string.notification_settings_item_other_account_emails_summary)
        }

        otherPreferenceCategory?.addPreference(devicePreference)
    }

    private fun addSitesForViewAllSitesScreen(preferenceScreen: PreferenceScreen, isFollowed: Boolean) {
        val context = activity ?: return
        val rootCategory = PreferenceCategory(context)
        rootCategory.setTitle(
            if (isFollowed)
                R.string.notification_settings_category_followed_sites
            else
                R.string.notification_settings_category_your_sites
        )
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
            when (channel!!) {
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

    @Suppress("DEPRECATION")
    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference is PreferenceScreen) {
            AnalyticsTracker.track(Stat.NOTIFICATION_SETTINGS_STREAMS_OPENED)
        } else {
            AnalyticsTracker.track(Stat.NOTIFICATION_SETTINGS_DETAILS_OPENED)
        }

        /* Since ringtone preference has been removed in androidx.preference, we use a workaround as
         * recommended here: https://issuetracker.google.com/issues/37057453#comment3 */
        val notificationSoundPreferenceKey = getString(R.string.wp_pref_custom_notification_sound)
        return if (preference.key?.equals(notificationSoundPreferenceKey) == true) {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
            val settings = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val existingValue: String? = settings.getString(notificationSoundPreferenceKey, null)
            if (existingValue != null) {
                if (existingValue.isEmpty()) {
                    // Select "Silent"
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, null as Uri?)
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingValue))
                }
            } else {
                // No ringtone has been selected, set to the default
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
            }
            startActivityForResult(intent, RequestCodes.NOTIFICATION_SETTINGS_ALERT_RINGTONE)
            true
        } else {
            super.onPreferenceTreeClick(preference)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (key == getString(R.string.pref_key_notification_pending_drafts)) {
            activity?.let {
                val prefs = PreferenceManager.getDefaultSharedPreferences(it)
                val shouldNotifyOfPendingDrafts = prefs.getBoolean("wp_pref_notification_pending_drafts", true)
                if (shouldNotifyOfPendingDrafts) {
                    AnalyticsTracker.track(Stat.NOTIFICATION_PENDING_DRAFTS_SETTINGS_ENABLED)
                } else {
                    AnalyticsTracker.track(Stat.NOTIFICATION_PENDING_DRAFTS_SETTINGS_DISABLED)
                }
            }
        } else if (key == getString(R.string.wp_pref_custom_notification_sound)) {
            val defaultPath = getString(R.string.notification_settings_item_sights_and_sounds_choose_sound_default)
            val value = sharedPreferences.getString(key, defaultPath)
            if (value!!.trim { it <= ' ' }.lowercase().startsWith("file://")) {
                // sound path begins with 'file://` which will lead to FileUriExposedException when used. Revert to
                //  default and let the user know.
                AppLog.w(
                    AppLog.T.NOTIFS,
                    "Notification sound starts with unacceptable scheme: $value"
                )
                val context = WordPress.getContext()
                ToastUtils.showToast(
                    context,
                    R.string.notification_sound_has_invalid_path,
                    ToastUtils.Duration.LONG
                )
            }
        }
    }

    private fun setToolbarTitle() {
        with(requireActivity() as AppCompatActivity) {
            val titleView = findViewById<TextView>(R.id.toolbar_title)
            titleView.text = getString(R.string.notification_settings)
        }
    }

    companion object {
        const val TAG = "NOTIFICATION_SETTINGS_FRAGMENT_TAG"
        private const val KEY_SEARCH_QUERY = "search_query"
        private const val SITE_SEARCH_VISIBILITY_COUNT = 15

        // The number of notification types we support (e.g. timeline, email, mobile)
        private const val TYPE_COUNT = 3
        private const val NO_MAXIMUM = -1
        private const val MAX_SITES_TO_SHOW_ON_FIRST_SCREEN = 3
    }
}
