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
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.NOTIFICATIONS_SELECTED_FILTER
import org.wordpress.android.analytics.AnalyticsTracker.Stat.NOTIFICATION_TAPPED_SEGMENTED_CONTROL
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
import org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter.IS_TAPPED_ON_NOTIFICATION
import org.wordpress.android.ui.stats.StatsConnectJetpackActivity
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.NOTIFS
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.WPUrlUtils
import java.util.HashMap
import javax.inject.Inject

class NotificationsListFragment : Fragment() {
    private var shouldRefreshNotifications = false
    private var lastTabPosition = 0

    @Inject lateinit var accountStore: AccountStore

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            setSelectedTab(savedInstanceState.getInt(KEY_LAST_TAB_POSITION, TAB_POSITION_ALL))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        shouldRefreshNotifications = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.notifications_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        toolbar_main.setTitle(R.string.notifications_screen_title)
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar_main)

        tab_layout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: Tab) {
                val properties: MutableMap<String, String?> = HashMap(1)
                when (tab.position) {
                    TAB_POSITION_ALL -> properties[NOTIFICATIONS_SELECTED_FILTER] = FILTER_ALL.toString()
                    TAB_POSITION_COMMENT -> properties[NOTIFICATIONS_SELECTED_FILTER] = FILTER_COMMENT.toString()
                    TAB_POSITION_FOLLOW -> properties[NOTIFICATIONS_SELECTED_FILTER] = FILTER_FOLLOW.toString()
                    TAB_POSITION_LIKE -> properties[NOTIFICATIONS_SELECTED_FILTER] = FILTER_LIKE.toString()
                    TAB_POSITION_UNREAD -> properties[NOTIFICATIONS_SELECTED_FILTER] = FILTER_UNREAD.toString()
                    else -> properties[NOTIFICATIONS_SELECTED_FILTER] = FILTER_ALL.toString()
                }
                AnalyticsTracker.track(NOTIFICATION_TAPPED_SEGMENTED_CONTROL, properties)
                lastTabPosition = tab.position
            }

            override fun onTabUnselected(tab: Tab) {}
            override fun onTabReselected(tab: Tab) {}
        })
        view_pager.adapter = NotificationsFragmentAdapter(childFragmentManager, buildTitles())
        view_pager.pageMargin = resources.getDimensionPixelSize(R.dimen.margin_extra_large)
        tab_layout.setupWithViewPager(view_pager)

        jetpack_terms_and_conditions.text = Html.fromHtml(
                String.format(resources.getString(R.string.jetpack_connection_terms_and_conditions), "<u>", "</u>")
        )
        jetpack_terms_and_conditions.setOnClickListener {
            WPWebViewActivity.openURL(requireContext(), WPUrlUtils.buildTermsOfServiceUrl(context))
        }
        jetpack_faq.setOnClickListener {
            WPWebViewActivity.openURL(requireContext(), StatsConnectJetpackActivity.FAQ_URL)
        }
    }

    private fun buildTitles(): List<String> {
        val result: ArrayList<String> = ArrayList(TAB_COUNT)
        result.add(TAB_POSITION_ALL, getString(R.string.notifications_tab_title_all))
        result.add(TAB_POSITION_UNREAD, getString(R.string.notifications_tab_title_unread_notifications))
        result.add(TAB_POSITION_COMMENT, getString(R.string.notifications_tab_title_comments))
        result.add(TAB_POSITION_FOLLOW, getString(R.string.notifications_tab_title_follows))
        result.add(TAB_POSITION_LIKE, getString(R.string.notifications_tab_title_likes))
        return result
    }

    override fun onPause() {
        super.onPause()
        shouldRefreshNotifications = true
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().post(NotificationsUnseenStatus(false))
        if (!accountStore.hasAccessToken()) {
            showConnectJetpackView()
            connect_jetpack.visibility = View.VISIBLE
            tab_layout.visibility = View.GONE
            view_pager.visibility = View.GONE
        } else {
            connect_jetpack.visibility = View.GONE
            tab_layout.visibility = View.VISIBLE
            view_pager.visibility = View.VISIBLE
            if (shouldRefreshNotifications) {
                fetchNotesFromRemote()
            }
        }
        setSelectedTab(lastTabPosition)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_LAST_TAB_POSITION, lastTabPosition)
        super.onSaveInstanceState(outState)
    }

    private fun clearToolbarScrollFlags() {
        if (toolbar_main.layoutParams is LayoutParams) {
            val params = toolbar_main.layoutParams as LayoutParams
            params.scrollFlags = 0
        }
    }

    private fun fetchNotesFromRemote() {
        if (!isAdded || !NetworkUtils.isNetworkAvailable(activity)) {
            return
        }
        NotificationsUpdateServiceStarter.startService(activity)
    }

    private fun setSelectedTab(position: Int) {
        lastTabPosition = position
        tab_layout.getTabAt(lastTabPosition)?.select()
    }

    private fun showConnectJetpackView() {
        clearToolbarScrollFlags()
        jetpack_setup.setOnClickListener {
            val siteModel = (requireActivity() as? WPMainActivity)?.selectedSite
            JetpackConnectionWebViewActivity.startJetpackConnectionFlow(activity, NOTIFICATIONS, siteModel, false)
        }
    }

    private class NotificationsFragmentAdapter internal constructor(
        fragmentManager: FragmentManager,
        private val titles: List<String>
    ) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getCount(): Int {
            return TAB_COUNT
        }

        override fun getItem(position: Int): Fragment {
            return NotificationsListFragmentPage.newInstance(position)
        }

        override fun getPageTitle(position: Int): CharSequence? {
            if (titles.size > position && position >= 0) {
                return titles[position]
            }
            return super.getPageTitle(position)
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
        notificationSettings.isVisible = accountStore.hasAccessToken()
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
        private const val TAB_COUNT = 5
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
            activity: Activity?,
            noteId: String?,
            shouldShowKeyboard: Boolean,
            replyText: String?,
            filter: FILTERS?,
            isTappedFromPushNotification: Boolean
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
            detailIntent.putExtra(IS_TAPPED_ON_NOTIFICATION, isTappedFromPushNotification)
            openNoteForReplyWithParams(detailIntent, activity)
        }

        private fun openNoteForReplyWithParams(detailIntent: Intent, activity: Activity) {
            activity.startActivityForResult(detailIntent, RequestCodes.NOTE_DETAIL)
        }
    }
}
