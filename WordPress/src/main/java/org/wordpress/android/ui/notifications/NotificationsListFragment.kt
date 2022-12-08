@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.notifications

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.MarginPageTransformer
import com.google.android.material.appbar.AppBarLayout.LayoutParams
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayout.Tab
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.NOTIFICATIONS_SELECTED_FILTER
import org.wordpress.android.analytics.AnalyticsTracker.Stat.NOTIFICATION_TAPPED_SEGMENTED_CONTROL
import org.wordpress.android.databinding.NotificationsListFragmentBinding
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.JetpackConnectionSource.NOTIFICATIONS
import org.wordpress.android.ui.JetpackConnectionWebViewActivity
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureFullScreenOverlayFragment
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureOverlayScreenType
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.main.WPMainNavigationView.PageType
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment
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
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.JetpackBrandingUtils.Screen
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.WPUrlUtils
import org.wordpress.android.util.extensions.setLiftOnScrollTargetViewIdAndRequestLayout
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

@AndroidEntryPoint
class NotificationsListFragment : Fragment(R.layout.notifications_list_fragment), ScrollableViewInitializedListener {
    @Inject lateinit var accountStore: AccountStore
    @Inject lateinit var jetpackBrandingUtils: JetpackBrandingUtils

    private val viewModel: NotificationsListViewModel by viewModels()

    private var shouldRefreshNotifications = false
    private var lastTabPosition = 0
    private var binding: NotificationsListFragmentBinding? = null

    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            binding?.setSelectedTab(savedInstanceState.getInt(KEY_LAST_TAB_POSITION, TAB_POSITION_ALL))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shouldRefreshNotifications = true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        binding = NotificationsListFragmentBinding.bind(view).apply {
            toolbarMain.setTitle(R.string.notifications_screen_title)
            (requireActivity() as AppCompatActivity).setSupportActionBar(toolbarMain)

            tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
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

                override fun onTabUnselected(tab: Tab) = Unit
                override fun onTabReselected(tab: Tab) = Unit
            })
            viewPager.adapter = NotificationsFragmentAdapter(this@NotificationsListFragment)
            buildTitles().let { titles ->
                TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    if (0 <= position && position < titles.size) {
                        tab.text =  titles[position]
                    }
                }.attach()
            }
            viewPager.setPageTransformer(
                MarginPageTransformer(resources.getDimensionPixelSize(R.dimen.margin_extra_large))
            )

            jetpackTermsAndConditions.text = HtmlCompat.fromHtml(
                    String.format(resources.getString(R.string.jetpack_connection_terms_and_conditions), "<u>", "</u>"),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            jetpackTermsAndConditions.setOnClickListener {
                WPWebViewActivity.openURL(requireContext(), WPUrlUtils.buildTermsOfServiceUrl(context))
            }
            jetpackFaq.setOnClickListener {
                WPWebViewActivity.openURL(requireContext(), StatsConnectJetpackActivity.FAQ_URL)
            }
        }

        viewModel.showJetpackPoweredBottomSheet.observeEvent(viewLifecycleOwner) {
            JetpackPoweredBottomSheetFragment
                    .newInstance(it, PageType.NOTIFS)
                    .show(childFragmentManager, JetpackPoweredBottomSheetFragment.TAG)
        }

        viewModel.showJetpackOverlay.observeEvent(viewLifecycleOwner) {
            if (savedInstanceState == null)
                JetpackFeatureFullScreenOverlayFragment
                        .newInstance(JetpackFeatureOverlayScreenType.NOTIFICATIONS)
                        .show(childFragmentManager, JetpackFeatureFullScreenOverlayFragment.TAG)
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

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().post(NotificationsUnseenStatus(false))
        binding?.apply {
            if (!accountStore.hasAccessToken()) {
                showConnectJetpackView()
                connectJetpack.visibility = View.VISIBLE
                tabLayout.visibility = View.GONE
                viewPager.visibility = View.GONE
            } else {
                connectJetpack.visibility = View.GONE
                tabLayout.visibility = View.VISIBLE
                viewPager.visibility = View.VISIBLE
                if (shouldRefreshNotifications) {
                    fetchNotesFromRemote()
                }
            }
            setSelectedTab(lastTabPosition)
        }
        viewModel.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_LAST_TAB_POSITION, lastTabPosition)
        super.onSaveInstanceState(outState)
    }

    private fun NotificationsListFragmentBinding.clearToolbarScrollFlags() {
        if (toolbarMain.layoutParams is LayoutParams) {
            val params = toolbarMain.layoutParams as LayoutParams
            params.scrollFlags = 0
        }
    }

    private fun fetchNotesFromRemote() {
        if (!isAdded || !NetworkUtils.isNetworkAvailable(activity)) {
            return
        }
        NotificationsUpdateServiceStarter.startService(activity)
    }

    private fun NotificationsListFragmentBinding.setSelectedTab(position: Int) {
        lastTabPosition = position
        tabLayout.getTabAt(lastTabPosition)?.select()
    }

    private fun NotificationsListFragmentBinding.showConnectJetpackView() {
        clearToolbarScrollFlags()
        jetpackSetup.setOnClickListener {
            val selectedSite = (requireActivity() as? WPMainActivity)?.selectedSite
            JetpackConnectionWebViewActivity.startJetpackConnectionFlow(activity, NOTIFICATIONS, selectedSite, false)
        }
    }

    private class NotificationsFragmentAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int {
            return TAB_COUNT
        }

        override fun createFragment(position: Int): Fragment {
            return NotificationsListFragmentPage.newInstance(position)
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

        @JvmStatic
        @Suppress("LongParameterList")
        fun openNoteForReply(
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

    override fun onScrollableViewInitialized(containerId: Int) {
        binding?.appBar?.setLiftOnScrollTargetViewIdAndRequestLayout(containerId)
        if (jetpackBrandingUtils.shouldShowJetpackBranding()) {
            binding?.root?.post {
                // post is used to create a minimal delay here. containerId changes just before
                // onScrollableViewInitialized is called, and findViewById can't find the new id before the delay.
                val jetpackBannerView = binding?.jetpackBanner?.root ?: return@post
                val scrollableView = binding?.root?.findViewById<View>(containerId) as? RecyclerView ?: return@post
                jetpackBrandingUtils.showJetpackBannerIfScrolledToTop(jetpackBannerView, scrollableView)
                jetpackBrandingUtils.initJetpackBannerAnimation(jetpackBannerView, scrollableView)

                if (jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) {
                    jetpackBannerView.setOnClickListener {
                        jetpackBrandingUtils.trackBannerTapped(Screen.NOTIFICATIONS)
                        JetpackPoweredBottomSheetFragment
                                .newInstance()
                                .show(childFragmentManager, JetpackPoweredBottomSheetFragment.TAG)
                    }
                }
            }
        }
    }
}
