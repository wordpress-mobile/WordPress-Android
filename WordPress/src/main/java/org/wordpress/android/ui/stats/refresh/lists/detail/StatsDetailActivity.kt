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
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.databinding.StatsDetailActivityBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.stats.StatsViewType
import org.wordpress.android.ui.stats.refresh.StatsViewAllFragment
import org.wordpress.android.ui.stats.refresh.lists.StatsListFragment
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider.SelectedDate
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
                    StatsSection.INSIGHT_DETAIL,
                    StatsSection.TOTAL_LIKES_DETAIL,
                    StatsSection.TOTAL_COMMENTS_DETAIL,
                    StatsSection.TOTAL_FOLLOWERS_DETAIL -> add<InsightsDetailFragment>(R.id.fragment_container)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        @Suppress("LongParameterList")
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
                Stat.STATS_SINGLE_POST_ACCESSED,
                site.siteId
            )
            context.startActivity(statsPostViewIntent)
        }

        @JvmStatic
        @Suppress("LongParameterList")
        fun startForInsightsDetail(
            context: Context,
            statsSection: StatsSection,
            statsViewType: StatsViewType,
            granularity: StatsGranularity?,
            selectedDate: SelectedDate?,
            localSiteId: Int
        ) {
            val intent = Intent(context, StatsDetailActivity::class.java).apply {
                putExtra(WordPress.LOCAL_SITE_ID, localSiteId)
                putExtra(StatsListFragment.LIST_TYPE, statsSection)
                putExtra(StatsViewAllFragment.ARGS_VIEW_TYPE, statsViewType)
                granularity?.let {
                    putExtra(StatsViewAllFragment.ARGS_TIMEFRAME, granularity)
                }
                selectedDate?.let {
                    putExtra(StatsViewAllFragment.ARGS_SELECTED_DATE, selectedDate)
                }
            }
            context.startActivity(intent)
        }
    }
}
