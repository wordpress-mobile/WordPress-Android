@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.PopupWindow
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
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
import org.wordpress.android.analytics.AnalyticsTracker.Stat.NOTIFICATIONS_MARK_ALL_READ_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.NOTIFICATION_MENU_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.NOTIFICATION_TAPPED_SEGMENTED_CONTROL
import org.wordpress.android.databinding.NotificationsListFragmentBinding
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.JetpackConnectionSource.NOTIFICATIONS
import org.wordpress.android.ui.JetpackConnectionWebViewActivity
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureFullScreenOverlayFragment
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureOverlayScreenType
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener
import org.wordpress.android.ui.main.WPMainNavigationView.PageType
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsUnseenStatus
import org.wordpress.android.ui.notifications.NotificationsListFragment.Companion.TabPosition.All
import org.wordpress.android.ui.notifications.NotificationsListFragmentPage.Companion.KEY_TAB_POSITION
import org.wordpress.android.ui.notifications.adapters.Filter
import org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter
import org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter.IS_TAPPED_ON_NOTIFICATION
import org.wordpress.android.ui.stats.StatsConnectJetpackActivity
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.PermissionUtils
import org.wordpress.android.util.WPPermissionUtils
import org.wordpress.android.util.WPPermissionUtils.NOTIFICATIONS_PERMISSION_REQUEST_CODE
import org.wordpress.android.util.WPUrlUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.extensions.setLiftOnScrollTargetViewIdAndRequestLayout
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

@AndroidEntryPoint
class NotificationsListFragment : Fragment(R.layout.notifications_list_fragment), ScrollableViewInitializedListener,
    OnScrollToTopListener {
    @Inject
    lateinit var accountStore: AccountStore

    @Inject
    lateinit var jetpackBrandingUtils: JetpackBrandingUtils

    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private val viewModel: NotificationsListViewModel by viewModels()

    private var lastTabPosition = 0
    private var binding: NotificationsListFragmentBinding? = null

    private var containerId: Int? = null

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            binding?.setSelectedTab(savedInstanceState.getInt(KEY_LAST_TAB_POSITION, All.ordinal))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        binding = NotificationsListFragmentBinding.bind(view).apply {
            toolbarMain.setTitle(R.string.notifications_screen_title)
            (requireActivity() as AppCompatActivity).setSupportActionBar(toolbarMain)

            tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
                override fun onTabSelected(tab: Tab) {
                    val tabPosition = TabPosition.values().getOrNull(tab.position) ?: All
                    AnalyticsTracker.track(
                        NOTIFICATION_TAPPED_SEGMENTED_CONTROL, hashMapOf(
                            NOTIFICATIONS_SELECTED_FILTER to tabPosition.filter.toString()
                        )
                    )
                    lastTabPosition = tab.position
                }

                override fun onTabUnselected(tab: Tab) = Unit
                override fun onTabReselected(tab: Tab) = Unit
            })
            viewPager.adapter = NotificationsFragmentAdapter(this@NotificationsListFragment)
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = TabPosition.values().getOrNull(position)?.let { getString(it.titleRes) } ?: ""
            }.attach()
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
                fetchRemoteNotes()
            }
            setSelectedTab(lastTabPosition)
            setNotificationPermissionWarning()
        }
        viewModel.onResume()
    }

    private fun fetchRemoteNotes() {
        if (!isAdded) {
            return
        }
        NotificationsUpdateServiceStarter.startService(activity)
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

    private fun NotificationsListFragmentBinding.setSelectedTab(position: Int) {
        lastTabPosition = position
        tabLayout.getTabAt(lastTabPosition)?.select()
    }

    private fun NotificationsListFragmentBinding.setNotificationPermissionWarning() {
        val hasPermission = PermissionUtils.checkNotificationsPermission(activity)
        if (hasPermission) {
            // If the permissions is granted, we should reset the state of the warning. Because the permission may be
            // disabled later, then we should be able to show the warning again to inform the user.
            viewModel.resetNotificationsPermissionWarningDismissState()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasPermission ||
            viewModel.isNotificationsPermissionsWarningDismissed
        ) {
            // If the user dismissed the warning, don't show it again.
            notificationPermissionWarning.isVisible = false
        } else {
            notificationPermissionWarning.isVisible = true
            notificationPermissionWarning.setOnClickListener {
                val isAlwaysDenied = WPPermissionUtils.isPermissionAlwaysDenied(
                    requireActivity(),
                    Manifest.permission.POST_NOTIFICATIONS
                )
                if (isAlwaysDenied) {
                    NotificationsPermissionBottomSheetFragment().show(
                        parentFragmentManager,
                        NotificationsPermissionBottomSheetFragment.TAG
                    )
                } else {
                    requestPermissions(
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATIONS_PERMISSION_REQUEST_CODE
                    )
                }
            }
            permissionDismissButton.setOnClickListener {
                notificationPermissionWarning.isVisible = false
                viewModel.onNotificationsPermissionWarningDismissed()
            }
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        WPPermissionUtils.setPermissionListAsked(
            requireActivity(),
            requestCode,
            permissions,
            grantResults,
            false
        )
        viewModel.resetNotificationsPermissionWarningDismissState()
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
            return TabPosition.values().size
        }

        override fun createFragment(position: Int): Fragment {
            return NotificationsListFragmentPage.newInstance(position)
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onPrepareOptionsMenu(menu: Menu) {
        val notificationActions = menu.findItem(R.id.notifications_actions)
        notificationActions.isVisible = accountStore.hasAccessToken()
        notificationActions.actionView?.setOnClickListener {
            analyticsTrackerWrapper.track(NOTIFICATION_MENU_TAPPED)
            showNotificationActionsPopup(it)
        }
        super.onPrepareOptionsMenu(menu)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.notifications_list_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    /**
     * For displaying the popup of notifications settings
     */
    @SuppressLint("InflateParams")
    private fun showNotificationActionsPopup(anchorView: View) {
        val popupWindow = PopupWindow(requireContext(), null, R.style.WordPress)
        popupWindow.isOutsideTouchable = true
        popupWindow.elevation = resources.getDimension(R.dimen.popup_over_toolbar_elevation)
        popupWindow.contentView = LayoutInflater.from(requireContext())
            .inflate(R.layout.notification_actions, null).apply {
                findViewById<View>(R.id.text_mark_all_as_read).setOnClickListener {
                    markAllAsRead()
                    popupWindow.dismiss()
                }
                findViewById<View>(R.id.text_settings).setOnClickListener {
                    ActivityLauncher.viewNotificationsSettings(activity)
                    popupWindow.dismiss()
                }
            }
        popupWindow.showAsDropDown(anchorView)
    }

    /**
     * For marking the status of every notification as read
     */
    private fun markAllAsRead() {
        analyticsTrackerWrapper.track(NOTIFICATIONS_MARK_ALL_READ_TAPPED)
        (childFragmentManager.fragments.firstOrNull {
            // use -1 to make sure that the (null == null) will not happen
            (it.arguments?.getInt(KEY_TAB_POSITION) ?: -1) == binding?.viewPager?.currentItem
        } as? NotificationsListFragmentPage)?.markAllNotesAsRead()
    }

    companion object {
        const val NOTE_ID_EXTRA = "noteId"
        const val NOTE_INSTANT_REPLY_EXTRA = "instantReply"
        const val NOTE_PREFILLED_REPLY_EXTRA = "prefilledReplyText"
        const val NOTE_MODERATE_ID_EXTRA = "moderateNoteId"
        const val NOTE_MODERATE_STATUS_EXTRA = "moderateNoteStatus"
        const val NOTE_CURRENT_LIST_FILTER_EXTRA = "currentFilter"

        enum class TabPosition(@StringRes val titleRes: Int, val filter: Filter) {
            All(R.string.notifications_tab_title_all, Filter.ALL),
            Unread(R.string.notifications_tab_title_unread_notifications, Filter.UNREAD),
            Comment(R.string.notifications_tab_title_comments, Filter.COMMENT),
            Subscribers(R.string.notifications_tab_title_subscribers, Filter.FOLLOW),
            Like(R.string.notifications_tab_title_likes, Filter.LIKE);
        }

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
            filter: Filter?,
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
        this.containerId = containerId
        binding?.appBar?.setLiftOnScrollTargetViewIdAndRequestLayout(containerId)
        if (jetpackBrandingUtils.shouldShowJetpackBranding()) {
            val screen = JetpackPoweredScreen.WithDynamicText.NOTIFICATIONS
            binding?.root?.post {
                // post is used to create a minimal delay here. containerId changes just before
                // onScrollableViewInitialized is called, and findViewById can't find the new id before the delay.
                val jetpackBannerView = binding?.jetpackBanner?.root ?: return@post
                val scrollableView = getRecyclerViewById() ?: return@post
                jetpackBrandingUtils.showJetpackBannerIfScrolledToTop(jetpackBannerView, scrollableView)
                jetpackBrandingUtils.initJetpackBannerAnimation(jetpackBannerView, scrollableView)
                binding?.jetpackBanner?.jetpackBannerText?.text = uiHelpers.getTextOfUiString(
                    requireContext(),
                    jetpackBrandingUtils.getBrandingTextForScreen(screen)
                )

                if (jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) {
                    jetpackBannerView.setOnClickListener {
                        jetpackBrandingUtils.trackBannerTapped(screen)
                        JetpackPoweredBottomSheetFragment
                            .newInstance()
                            .show(childFragmentManager, JetpackPoweredBottomSheetFragment.TAG)
                    }
                }
            }
        }
    }

    private fun getRecyclerViewById() =
        containerId?.let {
            binding?.root?.findViewById<View>(it) as? RecyclerView
        }

    override fun onScrollToTop() {
        if (isAdded) {
            getRecyclerViewById()?.smoothScrollToPosition(0)
            binding?.appBar?.setExpanded(true, true)
        }
    }
}
