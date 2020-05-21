package org.wordpress.android.ui.stats.refresh

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.stats_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import javax.inject.Inject

class StatsActivity : LocaleAwareActivity() {
    @Inject lateinit var statsSiteProvider: StatsSiteProvider
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: StatsViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        setContentView(R.layout.stats_list_activity)

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

    override fun onNewIntent(intent: Intent?) {
        intent?.let {
            val siteId = intent.getIntExtra(WordPress.LOCAL_SITE_ID, -1)
            if (siteId > -1) {
                viewModel = ViewModelProviders.of(this, viewModelFactory).get(StatsViewModel::class.java)
                viewModel.start(intent, restart = true)
            }
        }
        super.onNewIntent(intent)
    }

    companion object {
        const val INITIAL_SELECTED_PERIOD_KEY = "INITIAL_SELECTED_PERIOD_KEY"
        const val ARG_LAUNCHED_FROM = "ARG_LAUNCHED_FROM"
        const val ARG_DESIRED_TIMEFRAME = "ARG_DESIRED_TIMEFRAME"
        const val ARG_LOCAL_TABLE_SITE_ID = "ARG_LOCAL_TABLE_SITE_ID"
        @JvmStatic
        fun start(context: Context, site: SiteModel) {
            context.startActivity(buildIntent(context, site))
        }

        fun start(context: Context, localSiteId: Int, statsTimeframe: StatsTimeframe, period: String?) {
            val intent = buildIntent(context, localSiteId, statsTimeframe, period)
            context.startActivity(intent)
        }

        @JvmStatic
        fun buildIntent(context: Context, site: SiteModel): Intent {
            return buildIntent(context, site.id)
        }

        private fun buildIntent(
            context: Context,
            localSiteId: Int,
            statsTimeframe: StatsTimeframe? = null,
            period: String? = null
        ): Intent {
            val intent = Intent(context, StatsActivity::class.java)
            intent.putExtra(WordPress.LOCAL_SITE_ID, localSiteId)
            statsTimeframe?.let { intent.putExtra(ARG_DESIRED_TIMEFRAME, statsTimeframe) }
            period?.let { intent.putExtra(INITIAL_SELECTED_PERIOD_KEY, period) }
            return intent
        }
    }

    enum class StatsLaunchedFrom {
        STATS_WIDGET,
        NOTIFICATIONS
    }
}
