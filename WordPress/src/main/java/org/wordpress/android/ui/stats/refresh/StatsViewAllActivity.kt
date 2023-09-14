package org.wordpress.android.ui.stats.refresh

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.databinding.StatsViewAllActivityBinding
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.stats.StatsViewType
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider.SelectedDate
import android.R as AndroidR

@AndroidEntryPoint
class StatsViewAllActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = StatsViewAllActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == AndroidR.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        @JvmStatic
        fun startForGranularStats(
            context: Context,
            statsType: StatsViewType,
            granularity: StatsGranularity,
            selectedDate: SelectedDate,
            localSiteId: Int
        ) {
            start(context, statsType, granularity, selectedDate, localSiteId = localSiteId)
        }

        @JvmStatic
        fun startForInsights(context: Context, statsType: StatsViewType, localSiteId: Int) {
            start(context, statsType, localSiteId = localSiteId)
        }

        @JvmStatic
        fun startForTabbedInsightsStats(
            context: Context,
            statsType: StatsViewType,
            selectedTab: Int,
            localSiteId: Int
        ) {
            start(context, statsType, selectedTab = selectedTab, localSiteId = localSiteId)
        }

        private fun start(
            context: Context,
            statsType: StatsViewType,
            granularity: StatsGranularity? = null,
            selectedDate: SelectedDate? = null,
            selectedTab: Int? = null,
            localSiteId: Int
        ) {
            val intent = Intent(context, StatsViewAllActivity::class.java)
            intent.putExtra(StatsViewAllFragment.ARGS_VIEW_TYPE, statsType)
            selectedTab?.let {
                intent.putExtra(StatsViewAllFragment.SELECTED_TAB_KEY, selectedTab)
            }
            granularity?.let {
                intent.putExtra(StatsViewAllFragment.ARGS_TIMEFRAME, granularity)
            }
            selectedDate?.let {
                intent.putExtra(StatsViewAllFragment.ARGS_SELECTED_DATE, selectedDate)
            }
            intent.putExtra(WordPress.LOCAL_SITE_ID, localSiteId)
            AnalyticsTracker.track(Stat.STATS_VIEW_ALL_ACCESSED)
            context.startActivity(intent)
        }
    }
}
