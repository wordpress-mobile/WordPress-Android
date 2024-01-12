package org.wordpress.android.stats

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.push.NotificationType
import org.wordpress.android.push.NotificationsProcessingService.ARG_NOTIFICATION_TYPE
import org.wordpress.android.ui.domains.management.M3Theme
import org.wordpress.android.ui.stats.StatsTimeframe

class StatsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            M3Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StatsScreen(
                        onBackTapped = {},
                        onStatsSettingsTapped = {},
                    )
                }
            }
        }
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
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    M3Theme {
        StatsScreen(
            onBackTapped = {},
            onStatsSettingsTapped = {},
        )
    }
}
