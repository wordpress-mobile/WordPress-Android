package org.wordpress.android.ui.stats.refresh.lists.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.add
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.databinding.StatsDetailActivityBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.stats.refresh.lists.StatsListFragment
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.util.analytics.AnalyticsUtils

const val POST_ID = "POST_ID"
const val POST_TYPE = "POST_TYPE"
const val POST_TITLE = "POST_TITLE"
const val POST_URL = "POST_URL"

@AndroidEntryPoint
class StatsDetailActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = StatsDetailActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val listType = intent.extras?.get(StatsListFragment.LIST_TYPE)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                when (listType) {
                    StatsSection.DETAIL -> add<StatsDetailFragment>(R.id.fragment_container)
                    StatsSection.TOTAL_LIKES_DETAIL -> add<TotalLikesDetailFragment>(R.id.fragment_container)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        fun start(
            context: Context,
            site: SiteModel,
            postId: Long,
            postType: String,
            postTitle: String,
            postUrl: String?
        ) {
            val statsPostViewIntent = Intent(context, StatsDetailActivity::class.java)
            statsPostViewIntent.putExtra(WordPress.LOCAL_SITE_ID, site.id)
            statsPostViewIntent.putExtra(POST_ID, postId)
            statsPostViewIntent.putExtra(POST_TYPE, postType)
            statsPostViewIntent.putExtra(POST_TITLE, postTitle)
            statsPostViewIntent.putExtra(StatsListFragment.LIST_TYPE, StatsSection.DETAIL)
            if (postUrl != null) {
                statsPostViewIntent.putExtra(POST_URL, postUrl)
            }
            AnalyticsUtils.trackWithSiteId(
                    AnalyticsTracker.Stat.STATS_SINGLE_POST_ACCESSED,
                    site.siteId
            )
            context.startActivity(statsPostViewIntent)
        }

        @JvmStatic
        fun startForTotalLikesDetail(
            context: Context,
            site: SiteModel
        ) {
            val intent = Intent(context, StatsDetailActivity::class.java).apply {
                putExtra(WordPress.LOCAL_SITE_ID, site.id)
                putExtra(StatsListFragment.LIST_TYPE, StatsSection.TOTAL_LIKES_DETAIL)
            }
            // TODO: Add tracking here
            context.startActivity(intent)
        }
    }
}
