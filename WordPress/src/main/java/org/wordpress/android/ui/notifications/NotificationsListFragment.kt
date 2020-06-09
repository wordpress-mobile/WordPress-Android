package org.wordpress.android.ui.notifications

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.Html
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.google.android.material.appbar.AppBarLayout.LayoutParams
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayout.Tab
import kotlinx.android.synthetic.main.notifications_list_fragment.*
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.R
import org.wordpress.android.R.dimen
import org.wordpress.android.R.layout
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.NOTIFICATION_TAPPED_SEGMENTED_CONTROL
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.JetpackConnectionSource.NOTIFICATIONS
import org.wordpress.android.ui.JetpackConnectionWebViewActivity
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsUnseenStatus
import org.wordpress.android.ui.notifications.adapters.NotesAdapter.FILTERS
import org.wordpress.android.ui.notifications.adapters.NotesAdapter.FILTERS.FILTER_ALL
import org.wordpress.android.ui.notifications.adapters.NotesAdapter.FILTERS.FILTER_COMMENT
import org.wordpress.android.ui.notifications.adapters.NotesAdapter.FILTERS.FILTER_FOLLOW
import org.wordpress.android.ui.notifications.adapters.NotesAdapter.FILTERS.FILTER_LIKE
import org.wordpress.android.ui.notifications.adapters.NotesAdapter.FILTERS.FILTER_UNREAD
import org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter
import org.wordpress.android.ui.stats.StatsConnectJetpackActivity
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.NOTIFS
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.WPUrlUtils
import org.wordpress.android.widgets.WPViewPager
import java.util.HashMap
import javax.inject.Inject

class NotificationsListFragment : Fragment() {
    private var mShouldRefreshNotifications = false
    private var mLastTabPosition = 0

    @JvmField @Inject var mAccountStore: AccountStore? = null
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            setSelectedTab(
                    savedInstanceState.getInt(
                            KEY_LAST_TAB_POSITION,
                            TAB_POSITION_ALL
                    )
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        mShouldRefreshNotifications = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(layout.notifications_list_fragment, container, false)
        setHasOptionsMenu(true)
        toolbar_main.setTitle(string.notifications_screen_title)
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar_main)
        tab_layout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: Tab) {
                val properties: MutableMap<String, String?> = HashMap(
                        1
                )
                when (tab.position) {
                    TAB_POSITION_ALL -> properties[AnalyticsTracker.NOTIFICATIONS_SELECTED_FILTER] = FILTER_ALL.toString()
                    TAB_POSITION_COMMENT -> properties[AnalyticsTracker.NOTIFICATIONS_SELECTED_FILTER] = FILTER_COMMENT.toString()
                    TAB_POSITION_FOLLOW -> properties[AnalyticsTracker.NOTIFICATIONS_SELECTED_FILTER] = FILTER_FOLLOW.toString()
                    TAB_POSITION_LIKE -> properties[AnalyticsTracker.NOTIFICATIONS_SELECTED_FILTER] = FILTER_LIKE.toString()
                    TAB_POSITION_UNREAD -> properties[AnalyticsTracker.NOTIFICATIONS_SELECTED_FILTER] = FILTER_UNREAD.toString()
                    else -> properties[AnalyticsTracker.NOTIFICATIONS_SELECTED_FILTER] = FILTER_ALL.toString()
                }
                AnalyticsTracker.track(NOTIFICATION_TAPPED_SEGMENTED_CONTROL, properties)
                mLastTabPosition = tab.position
            }

            override fun onTabUnselected(tab: Tab) {}
            override fun onTabReselected(tab: Tab) {}
        })
        val viewPager: WPViewPager = view.findViewById(R.id.view_pager)
        viewPager.adapter = NotificationsFragmentAdapter(childFragmentManager)
        viewPager.pageMargin = resources.getDimensionPixelSize(dimen.margin_extra_large)
        tab_layout.setupWithViewPager(viewPager)
        val jetpackTermsAndConditions = view.findViewById<TextView>(R.id.jetpack_terms_and_conditions)
        jetpackTermsAndConditions.text = Html.fromHtml(
                String.format(
                        resources.getString(string.jetpack_connection_terms_and_conditions),
                        "<u>",
                        "</u>"
                )
        )
        jetpackTermsAndConditions.setOnClickListener {
            WPWebViewActivity.openURL(
                    requireContext(),
                    WPUrlUtils.buildTermsOfServiceUrl(context)
            )
        }
        val jetpackFaq = view.findViewById<Button>(R.id.jetpack_faq)
        jetpackFaq.setOnClickListener {
            WPWebViewActivity.openURL(
                    requireContext(),
                    StatsConnectJetpackActivity.FAQ_URL
            )
        }
        return view
    }

    override fun onPause() {
        super.onPause()
        mShouldRefreshNotifications = true
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().post(NotificationsUnseenStatus(false))
        if (!mAccountStore!!.hasAccessToken()) {
            showConnectJetpackView()
            tab_layout.visibility = View.GONE
        } else {
            if (mShouldRefreshNotifications) {
                fetchNotesFromRemote()
            }
        }
        setSelectedTab(mLastTabPosition)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_LAST_TAB_POSITION, mLastTabPosition)
        super.onSaveInstanceState(outState)
    }

    private fun clearToolbarScrollFlags() {
        if (toolbar_main != null && toolbar_main.layoutParams is LayoutParams) {
            val params = toolbar_main.layoutParams as LayoutParams
            params.scrollFlags = 0
        }
    }

    private fun fetchNotesFromRemote() {
        if (!isAdded) {
            return
        }
        if (!NetworkUtils.isNetworkAvailable(activity)) {
            return
        }
        NotificationsUpdateServiceStarter.startService(activity)
    }

    val selectedSite: SiteModel?
        get() {
            if (activity is WPMainActivity) {
                val mainActivity = activity as WPMainActivity?
                return mainActivity!!.selectedSite
            }
            return null
        }

    private fun setSelectedTab(position: Int) {
        mLastTabPosition = position
        if (tab_layout != null) {
            val tab = tab_layout.getTabAt(mLastTabPosition)
            tab?.select()
        }
    }

    private fun showConnectJetpackView() {
        if (isAdded && connect_jetpack != null) {
            connect_jetpack.visibility = View.VISIBLE
            tab_layout.visibility = View.GONE
            clearToolbarScrollFlags()
            val setupButton = connect_jetpack.findViewById<Button>(R.id.jetpack_setup)
            setupButton.setOnClickListener {
                val siteModel = selectedSite
                JetpackConnectionWebViewActivity
                        .startJetpackConnectionFlow(
                                activity,
                                NOTIFICATIONS,
                                siteModel,
                                false
                        )
            }
        }
    }

    private inner class NotificationsFragmentAdapter internal constructor(fragmentManager: FragmentManager?) :
            FragmentPagerAdapter(fragmentManager!!) {
        override fun getCount(): Int {
            return TAB_COUNT
        }

        override fun getItem(position: Int): Fragment {
            return NotificationsListFragmentPage.newInstance(position)
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                TAB_POSITION_ALL -> getString(string.notifications_tab_title_all)
                TAB_POSITION_COMMENT -> getString(string.notifications_tab_title_comments)
                TAB_POSITION_FOLLOW -> getString(string.notifications_tab_title_follows)
                TAB_POSITION_LIKE -> getString(string.notifications_tab_title_likes)
                TAB_POSITION_UNREAD -> getString(string.notifications_tab_title_unread_notifications)
                else -> super.getPageTitle(position)
            }
        }

        override fun restoreState(state: Parcelable?, loader: ClassLoader?) {
            try {
                super.restoreState(state, loader)
            } catch (exception: IllegalStateException) {
                AppLog.e(NOTIFS, exception)
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val notificationSettings = menu.findItem(R.id.notifications_settings)
        notificationSettings.isVisible = mAccountStore!!.hasAccessToken()
        super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.notifications_list_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.notifications_settings) {
            ActivityLauncher.viewNotificationsSettings(activity)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val NOTE_ID_EXTRA = "noteId"
        const val NOTE_INSTANT_REPLY_EXTRA = "instantReply"
        const val NOTE_PREFILLED_REPLY_EXTRA = "prefilledReplyText"
        const val NOTE_MODERATE_ID_EXTRA = "moderateNoteId"
        const val NOTE_MODERATE_STATUS_EXTRA = "moderateNoteStatus"
        const val NOTE_CURRENT_LIST_FILTER_EXTRA = "currentFilter"
        protected const val TAB_COUNT = 5
        const val TAB_POSITION_ALL = 0
        const val TAB_POSITION_UNREAD = 1
        const val TAB_POSITION_COMMENT = 2
        const val TAB_POSITION_FOLLOW = 3
        const val TAB_POSITION_LIKE = 4
        private const val KEY_LAST_TAB_POSITION = "lastTabPosition"
        fun newInstance(): NotificationsListFragment {
            return NotificationsListFragment()
        }

        private fun getOpenNoteIntent(activity: Activity, noteId: String): Intent {
            val detailIntent = Intent(activity, NotificationsDetailActivity::class.java)
            detailIntent.putExtra(NOTE_ID_EXTRA, noteId)
            return detailIntent
        }

        @JvmStatic fun openNoteForReply(
            activity: Activity?, noteId: String?, shouldShowKeyboard: Boolean, replyText: String?,
            filter: FILTERS?, isTappedFromPushNotification: Boolean
        ) {
            if (noteId == null || activity == null) {
                return
            }
            if (activity.isFinishing) {
                return
            }
            val detailIntent = getOpenNoteIntent(activity, noteId)
            detailIntent.putExtra(NOTE_INSTANT_REPLY_EXTRA, shouldShowKeyboard)
            if (!TextUtils.isEmpty(replyText)) {
                detailIntent.putExtra(NOTE_PREFILLED_REPLY_EXTRA, replyText)
            }
            detailIntent.putExtra(NOTE_CURRENT_LIST_FILTER_EXTRA, filter)
            detailIntent.putExtra(
                    NotificationsUpdateServiceStarter.IS_TAPPED_ON_NOTIFICATION,
                    isTappedFromPushNotification
            )
            openNoteForReplyWithParams(detailIntent, activity)
        }

        private fun openNoteForReplyWithParams(detailIntent: Intent, activity: Activity) {
            activity.startActivityForResult(detailIntent, RequestCodes.NOTE_DETAIL)
        }
    }
}
