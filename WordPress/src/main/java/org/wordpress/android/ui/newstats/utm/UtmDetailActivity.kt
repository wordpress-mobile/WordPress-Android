package org.wordpress.android.ui.newstats.utm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.newstats.components.StatsChangeIndicator
import org.wordpress.android.ui.newstats.components.StatsItemName
import org.wordpress.android.ui.newstats.components.StatsListHeader
import org.wordpress.android.ui.newstats.components.StatsListRowContainer
import org.wordpress.android.ui.newstats.components.StatsSummaryCard
import org.wordpress.android.ui.newstats.components.StatsViewChange
import org.wordpress.android.ui.newstats.util.formatStatValue
import org.wordpress.android.util.extensions.getParcelableArrayListCompat

private const val EXTRA_ITEMS = "extra_items"
private const val EXTRA_TOTAL_VIEWS = "extra_total_views"
private const val EXTRA_TOTAL_VIEWS_CHANGE =
    "extra_total_views_change"
private const val EXTRA_TOTAL_VIEWS_CHANGE_PERCENT =
    "extra_total_views_change_percent"
private const val EXTRA_DATE_RANGE = "extra_date_range"

@AndroidEntryPoint
class UtmDetailActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val items = intent.extras
            ?.getParcelableArrayListCompat<UtmDetailItem>(
                EXTRA_ITEMS
            ) ?: arrayListOf()
        val totalViews = intent.getLongExtra(
            EXTRA_TOTAL_VIEWS, 0L
        )
        val totalViewsChange = intent.getLongExtra(
            EXTRA_TOTAL_VIEWS_CHANGE, 0L
        )
        val totalViewsChangePercent =
            intent.getDoubleExtra(
                EXTRA_TOTAL_VIEWS_CHANGE_PERCENT, 0.0
            )
        val dateRange = intent.getStringExtra(
            EXTRA_DATE_RANGE
        ) ?: ""
        val maxViewsForBar =
            items.firstOrNull()?.views ?: 0L

        setContent {
            AppThemeM3 {
                UtmDetailScreen(
                    items = items,
                    maxViewsForBar = maxViewsForBar,
                    totalViews = totalViews,
                    totalViewsChange = totalViewsChange,
                    totalViewsChangePercent =
                        totalViewsChangePercent,
                    dateRange = dateRange,
                    onBackPressed =
                        onBackPressedDispatcher::onBackPressed
                )
            }
        }
    }

    companion object {
        @Suppress("LongParameterList")
        fun start(
            context: Context,
            detailData: UtmDetailData
        ) {
            val parcelableItems = ArrayList(
                detailData.items.map { item ->
                    val change = item.change
                    UtmDetailItem(
                        title = item.title,
                        views = item.views,
                        changeValue = when (change) {
                            is StatsViewChange.Positive ->
                                change.value
                            is StatsViewChange.Negative ->
                                change.value
                            is StatsViewChange.NoChange ->
                                0L
                        },
                        changePercent = when (change) {
                            is StatsViewChange.Positive ->
                                change.percentage
                            is StatsViewChange.Negative ->
                                change.percentage
                            is StatsViewChange.NoChange ->
                                0.0
                        },
                        isPositive = change is
                            StatsViewChange.Positive,
                        topPosts = item.topPosts.map {
                            UtmDetailPostItem(
                                it.title, it.views
                            )
                        }
                    )
                }
            )
            val intent = Intent(
                context, UtmDetailActivity::class.java
            ).apply {
                putExtra(
                    EXTRA_ITEMS, parcelableItems
                )
                putExtra(
                    EXTRA_TOTAL_VIEWS,
                    detailData.totalViews
                )
                putExtra(
                    EXTRA_TOTAL_VIEWS_CHANGE,
                    detailData.totalViewsChange
                )
                putExtra(
                    EXTRA_TOTAL_VIEWS_CHANGE_PERCENT,
                    detailData.totalViewsChangePercent
                )
                putExtra(
                    EXTRA_DATE_RANGE,
                    detailData.dateRange
                )
            }
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UtmDetailScreen(
    items: List<UtmDetailItem>,
    maxViewsForBar: Long,
    totalViews: Long,
    totalViewsChange: Long,
    totalViewsChangePercent: Double,
    dateRange: String,
    onBackPressed: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            R.string.stats_utm_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.AutoMirrored.Filled
                                .ArrowBack,
                            contentDescription =
                                stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(
                    modifier = Modifier.height(8.dp)
                )
                StatsSummaryCard(
                    totalViews = totalViews,
                    dateRange = dateRange,
                    totalViewsChange =
                        totalViewsChange,
                    totalViewsChangePercent =
                        totalViewsChangePercent
                )
                Spacer(
                    modifier = Modifier.height(16.dp)
                )
            }

            item {
                StatsListHeader(
                    leftHeaderResId =
                        R.string.stats_utm_title
                )
                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            itemsIndexed(items) { index, item ->
                val percentage =
                    if (maxViewsForBar > 0) {
                        item.views.toFloat() /
                            maxViewsForBar.toFloat()
                    } else {
                        0f
                    }
                DetailUtmRow(
                    position = index + 1,
                    item = item,
                    percentage = percentage
                )
                if (index < items.lastIndex) {
                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailUtmRow(
    position: Int,
    item: UtmDetailItem,
    percentage: Float
) {
    val change = when {
        item.changeValue > 0 && item.isPositive ->
            StatsViewChange.Positive(
                item.changeValue, item.changePercent
            )
        item.changeValue > 0 && !item.isPositive ->
            StatsViewChange.Negative(
                item.changeValue, item.changePercent
            )
        else -> StatsViewChange.NoChange
    }
    StatsListRowContainer(percentage = percentage) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 10.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text = "$position",
                style = MaterialTheme.typography
                    .bodySmall,
                color = MaterialTheme.colorScheme
                    .onSurfaceVariant,
                modifier = Modifier.width(24.dp)
            )
            StatsItemName(
                name = item.title,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatStatValue(item.views),
                style = MaterialTheme.typography
                    .bodyMedium,
                color = MaterialTheme.colorScheme
                    .onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            StatsChangeIndicator(change = change)
        }
    }
}
