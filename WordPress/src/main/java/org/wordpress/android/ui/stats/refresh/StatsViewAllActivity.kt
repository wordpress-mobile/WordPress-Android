package org.wordpress.android.ui.stats.refresh

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.toolbar.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.ui.stats.StatsAbstractFragment
import org.wordpress.android.ui.stats.StatsViewType

class StatsViewAllActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.stats_view_all_activity)

        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
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
        @JvmStatic
        fun startForGranularStats(context: Context, statsType: StatsViewType, granularity: StatsGranularity) {
            start(context, statsType, granularity)
        }

        @JvmStatic
        fun startForInsights(context: Context, statsType: StatsViewType) {
            start(context, statsType)
        }

        @JvmStatic
        fun startForTabbedInsightsStats(
            context: Context,
            statsType: StatsViewType,
            selectedTab: Int,
            localSiteId: Int?
        ) {
            start(context, statsType, selectedTab = selectedTab, localSiteId = localSiteId)
        }

        private fun start(
            context: Context,
            statsType: StatsViewType,
            granularity: StatsGranularity? = null,
            selectedTab: Int? = null,
            localSiteId: Int? = null
        ) {
            val intent = Intent(context, StatsViewAllActivity::class.java)
            intent.putExtra(StatsAbstractFragment.ARGS_VIEW_TYPE, statsType)
            selectedTab?.let {
                intent.putExtra(StatsViewAllFragment.SELECTED_TAB_KEY, selectedTab)
            }
            granularity?.let {
                intent.putExtra(StatsAbstractFragment.ARGS_TIMEFRAME, granularity)
            }
            localSiteId?.let {
                intent.putExtra(WordPress.LOCAL_SITE_ID, localSiteId)
            }
            AnalyticsTracker.track(Stat.STATS_VIEW_ALL_ACCESSED)
            context.startActivity(intent)
        }
    }
}
