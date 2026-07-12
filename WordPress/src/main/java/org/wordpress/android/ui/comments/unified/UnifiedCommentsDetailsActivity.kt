package org.wordpress.android.ui.comments.unified

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.databinding.UnifiedCommentsDetailsActivityBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.commentsrs.CommentBrowsingSession
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.util.analytics.AnalyticsUtils.AnalyticsCommentActionSource
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import javax.inject.Inject

@AndroidEntryPoint
class UnifiedCommentsDetailsActivity : BaseAppCompatActivity() {
    @Inject lateinit var browsingSession: CommentBrowsingSession

    private var lastSelectedPosition = -1
    private var skipNextPageTrack = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The comment is loaded from the network (wordpress-rs), so bail out with a toast rather
        // than showing an empty screen when there's no connection. Only on first launch: a
        // recreation (rotation, process-death restore) may already have the comment on screen.
        if (savedInstanceState == null && !NetworkUtils.isNetworkAvailable(this)) {
            ToastUtils.showToast(this, R.string.no_network_message)
            finish()
            return
        }

        val binding = UnifiedCommentsDetailsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.setupActionBar()

        val site = requireNotNull(intent.getSerializableExtraCompat<SiteModel>(WordPress.SITE))
        val remoteCommentId = intent.getLongExtra(KEY_REMOTE_COMMENT_ID, 0)

        setupPager(binding.commentsPager, site, remoteCommentId, isFirstCreate = savedInstanceState == null)
    }

    private fun setupPager(pager: ViewPager2, site: SiteModel, initialCommentId: Long, isFirstCreate: Boolean) {
        // Page over the list's comments when opened from the list (a seeded session); otherwise a
        // single non-swipeable page — opened without a session, or restored after process death.
        val sessionIds = browsingSession.commentIds.value
        val useSession = browsingSession.isActive && initialCommentId in sessionIds
        val initialIds = if (useSession) sessionIds else listOf(initialCommentId)

        val adapter = CommentPagerAdapter(this, site).also { it.submit(initialIds) }
        pager.adapter = adapter

        if (isFirstCreate) {
            val startIndex = initialIds.indexOf(initialCommentId).coerceAtLeast(0)
            pager.setCurrentItem(startIndex, false)
            lastSelectedPosition = startIndex
            trackCommentViewed(site) // initial view; onPageSelected covers each later swipe
            maybeLoadMore(startIndex, adapter)
        } else {
            // ViewPager2 restores the user's page itself; don't re-track that restore as a view.
            skipNextPageTrack = true
        }

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                maybeLoadMore(position, adapter)
                if (skipNextPageTrack) {
                    skipNextPageTrack = false
                    lastSelectedPosition = position
                    return
                }
                if (position == lastSelectedPosition) return
                lastSelectedPosition = position
                trackCommentViewed(site)
            }
        })

        if (useSession) {
            // Appended ids (from load-more) grow the pager without recreating existing pages.
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    browsingSession.commentIds.collect { adapter.submit(it) }
                }
            }
        }
    }

    private fun maybeLoadMore(position: Int, adapter: CommentPagerAdapter) {
        if (!browsingSession.isActive || !browsingSession.canLoadMore) return
        if (position >= adapter.itemCount - LOAD_MORE_THRESHOLD) {
            lifecycleScope.launch { browsingSession.loadMore() }
        }
    }

    private fun trackCommentViewed(site: SiteModel) {
        // Site-comments host only; the notifications host tracks its own COMMENT_VIEWED.
        AnalyticsUtils.trackCommentActionWithSiteDetails(
            Stat.COMMENT_VIEWED, AnalyticsCommentActionSource.SITE_COMMENTS, site
        )
    }

    private fun UnifiedCommentsDetailsActivityBinding.setupActionBar() {
        setSupportActionBar(toolbarMain)
        supportActionBar?.let {
            it.setTitle(R.string.comment)
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        // Keep the session across config-change recreation; drop it only when leaving the screen so
        // a later single-comment open (e.g. from a notification) doesn't inherit a stale list.
        if (isFinishing) browsingSession.clear()
        super.onDestroy()
    }

    private class CommentPagerAdapter(
        activity: FragmentActivity,
        private val site: SiteModel
    ) : FragmentStateAdapter(activity) {
        private var ids: List<Long> = emptyList()

        /** Replaces the id list; only appends are expected (load-more), so notify the inserted range. */
        fun submit(newIds: List<Long>) {
            val old = ids
            if (newIds == old) return
            ids = newIds
            if (newIds.size > old.size && newIds.subList(0, old.size) == old) {
                notifyItemRangeInserted(old.size, newIds.size - old.size)
            } else {
                @Suppress("NotifyDataSetChanged")
                notifyDataSetChanged()
            }
        }

        override fun getItemCount(): Int = ids.size
        override fun createFragment(position: Int): Fragment =
            UnifiedCommentDetailsFragment.newInstance(site, ids[position])
        override fun getItemId(position: Int): Long = ids[position]
        override fun containsItem(itemId: Long): Boolean = ids.contains(itemId)
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context, site: SiteModel, remoteCommentId: Long): Intent =
            Intent(context, UnifiedCommentsDetailsActivity::class.java).apply {
                putExtra(WordPress.SITE, site)
                putExtra(KEY_REMOTE_COMMENT_ID, remoteCommentId)
            }

        private const val KEY_REMOTE_COMMENT_ID = "key_remote_comment_id"

        // Prefetch the next page when within this many pages of the end.
        private const val LOAD_MORE_THRESHOLD = 2
    }
}
