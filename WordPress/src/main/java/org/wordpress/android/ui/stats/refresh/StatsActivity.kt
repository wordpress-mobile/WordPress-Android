package org.wordpress.android.ui.stats.refresh

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.StatsListActivityBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.push.NotificationType
import org.wordpress.android.push.NotificationsProcessingService.ARG_NOTIFICATION_TYPE
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.JetpackBrandingUtils
import javax.inject.Inject

@AndroidEntryPoint
class StatsActivity : LocaleAwareActivity() {
    @Inject lateinit var statsSiteProvider: StatsSiteProvider
    @Inject lateinit var jetpackBrandingUtils: JetpackBrandingUtils
    private val viewModel: StatsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(StatsListActivityBinding.inflate(layoutInflater).root)
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
                viewModel.start(intent, restart = true)
            }
        }
        super.onNewIntent(intent)
    }

    companion object {
        const val INITIAL_SELECTED_PERIOD_KEY = "INITIAL_SELECTED_PERIOD_KEY"
        const val ARG_LAUNCHED_FROM = "ARG_LAUNCHED_FROM"
        const val ARG_DESIRED_TIMEFRAME = "ARG_DESIRED_TIMEFRAME"

        @JvmStatic
        @JvmOverloads
        fun start(
            context: Context,
            site: SiteModel,
            statsTimeframe: StatsTimeframe? = null,
            period: String? = null
        ) = context.startActivity(buildIntent(context, site, statsTimeframe, period))

        @JvmStatic
        @JvmOverloads
        fun buildIntent(
            context: Context,
            site: SiteModel,
            statsTimeframe: StatsTimeframe? = null,
            period: String? = null,
            notificationType: NotificationType? = null
        ) = Intent(context, StatsActivity::class.java).apply {
            putExtra(WordPress.LOCAL_SITE_ID, site.id)
            statsTimeframe?.let { putExtra(ARG_DESIRED_TIMEFRAME, it) }
            period?.let { putExtra(INITIAL_SELECTED_PERIOD_KEY, it) }
            notificationType?.let { putExtra(ARG_NOTIFICATION_TYPE, it) }
        }
    }

    enum class StatsLaunchedFrom {
        STATS_WIDGET,
        NOTIFICATIONS
    }
}
