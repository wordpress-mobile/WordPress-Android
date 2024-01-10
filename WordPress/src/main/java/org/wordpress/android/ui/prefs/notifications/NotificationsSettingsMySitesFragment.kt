package org.wordpress.android.ui.prefs.notifications

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.NotificationsSettings
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.SiteUtils
import javax.inject.Inject

class NotificationsSettingsMySitesFragment: ChildNotificationSettingsFragment(), NotificationsMySitesSettingsFragment {
    companion object {
        const val ARG_IS_FOLLOWED = "ARG_IS_FOLLOWED"
    }
    override var mNotificationUpdatedSite: String? = null
    override var mPreviousEmailPostsFrequency: String? = null
    override var mUpdateSubscriptionFrequencyPayload: AccountStore.UpdateSubscriptionPayload? = null
    override var mPreviousEmailComments: Boolean = false
    override var mPreviousEmailPosts: Boolean = false
    override var mPreviousNotifyPosts: Boolean = false
    override var mUpdateEmailPostsFirst: Boolean = false

    @Inject
    override lateinit var mDispatcher: Dispatcher

    @Inject
    lateinit var mSiteStore: SiteStore

    @Inject
    lateinit var mFollowedBlogsProvider: FollowedBlogsProvider

    private lateinit var rootCategory: PreferenceCategory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)

        val isFollowed: Boolean
        requireArguments().apply {
            isFollowed = getBoolean(ARG_IS_FOLLOWED)
        }
        rootCategory = PreferenceCategory(requireContext())
        rootCategory.setTitle(
            if (isFollowed)
                R.string.notification_settings_category_followed_sites
            else
                R.string.notification_settings_category_your_sites
        )
        preferenceScreen?.addPreference(rootCategory)
        if (isFollowed) {
            configureFollowedBlogsSettings(rootCategory)
        } else {
            configureBlogsSettings(rootCategory)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.notification_settings_my_sites, rootKey)
    }

    override fun onStart() {
        super.onStart()
        mDispatcher.register(this)
    }

    override fun onStop() {
        super.onStop()
        mDispatcher.unregister(this)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSubscriptionsChanged(event: AccountStore.OnSubscriptionsChanged) {
        if (event.isError) {
            AppLog.e(AppLog.T.API, "NotificationsSettingsFragment.onSubscriptionsChanged: " + event.error.message)
        } else {
            configureFollowedBlogsSettings(rootCategory)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSubscriptionUpdated(event: AccountStore.OnSubscriptionUpdated) {
        if (event.isError) {
            AppLog.e(AppLog.T.API, "NotificationsSettingsFragment.onSubscriptionUpdated: " + event.error.message)
        } else if (event.type == AccountStore.SubscriptionType.EMAIL_POST && mUpdateEmailPostsFirst) {
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

    @Suppress("DEPRECATION")
    private fun configureFollowedBlogsSettings(blogsCategory: PreferenceCategory?) {
        if (!isAdded || blogsCategory == null) {
            return
        }
        val models: List<FollowedBlogsProvider.PreferenceModel> =
            mFollowedBlogsProvider.getAllFollowedBlogs(null)
                .sortedWith { (title): FollowedBlogsProvider.PreferenceModel,
                              (otherTitle): FollowedBlogsProvider.PreferenceModel ->
                title.compareTo(
                    otherTitle,
                    ignoreCase = true
                )
            }
        blogsCategory.removeAll()

        val context: Context? = activity
        for ((title, summary, blogId, clickHandler) in models) {
            if (context == null) {
                return
            }
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
                            putBoolean(
                                NotificationSettingsFollowedDialog.ARG_NOTIFICATION_POSTS,
                                mPreviousNotifyPosts
                            )
                            putBoolean(
                                NotificationSettingsFollowedDialog.ARG_EMAIL_POSTS,
                                mPreviousEmailPosts
                            )
                            putString(
                                NotificationSettingsFollowedDialog.ARG_EMAIL_POSTS_FREQUENCY,
                                mPreviousEmailPostsFrequency
                            )
                            putBoolean(
                                NotificationSettingsFollowedDialog.ARG_EMAIL_COMMENTS,
                                mPreviousEmailComments
                            )
                        }
                        dialog.arguments = args
                        dialog.setTargetFragment(
                            this@NotificationsSettingsMySitesFragment,
                            RequestCodes.NOTIFICATION_SETTINGS
                        )
                        dialog.show(parentFragmentManager, NotificationSettingsFollowedDialog.TAG)
                        true
                    }
            } else {
                prefScreen.isEnabled = false
            }
            blogsCategory.addPreference(prefScreen)
        }
    }

    private fun configureBlogsSettings(blogsCategory: PreferenceCategory?) {
        if (!isAdded || blogsCategory == null) {
            return
        }
        val sites: List<SiteModel> = mSiteStore.sitesAccessedViaWPComRest
            .sortedWith { o1, o2 ->
                SiteUtils.getSiteNameOrHomeURL(o1)
                    .compareTo(SiteUtils.getSiteNameOrHomeURL(o2), ignoreCase = true)
            }
        blogsCategory.removeAll()

        val context: Context? = activity
        for (site in sites) {
            if (context == null) {
                return
            }
            val prefScreen = preferenceManager.createPreferenceScreen(context)
            prefScreen.title = SiteUtils.getSiteNameOrHomeURL(site)
            prefScreen.summary = SiteUtils.getHomeURLOrHostName(site)
            prefScreen.extras.apply {
                putLong(NotificationsSettingsTypesFragment.ARG_BLOG_ID, site.siteId)
                putInt(
                    NotificationsSettingsTypesFragment.ARG_NOTIFICATION_CHANNEL,
                    NotificationsSettings.Channel.BLOGS.ordinal
                )
            }
            prefScreen.fragment = NotificationsSettingsTypesFragment::class.qualifiedName
            blogsCategory.addPreference(prefScreen)
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RequestCodes.NOTIFICATION_SETTINGS) {
            this.onMySiteSettingsChanged(data)
        }
    }
}
