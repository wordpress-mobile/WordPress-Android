package org.wordpress.android.ui.newstats.mostviewed

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.newstats.StatsColors
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.ui.newstats.util.formatStatValue
import java.util.Locale
import kotlin.math.abs

private const val EXTRA_DATA_SOURCE = "extra_data_source"
private const val EXTRA_ITEMS = "extra_items"
private const val EXTRA_TOTAL_VIEWS = "extra_total_views"
private const val EXTRA_TOTAL_VIEWS_CHANGE = "extra_total_views_change"
private const val EXTRA_TOTAL_VIEWS_CHANGE_PERCENT = "extra_total_views_change_percent"
private const val EXTRA_DATE_RANGE = "extra_date_range"

@AndroidEntryPoint
class MostViewedDetailActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dataSource = intent.extras?.getSerializableCompat<MostViewedDataSource>(EXTRA_DATA_SOURCE)
            ?: MostViewedDataSource.POSTS_AND_PAGES
        @Suppress("UNCHECKED_CAST")
        val items = intent.extras?.getSerializableCompat<ArrayList<MostViewedDetailItem>>(EXTRA_ITEMS)
            ?: arrayListOf()
        val totalViews = intent.getLongExtra(EXTRA_TOTAL_VIEWS, 0L)
        val totalViewsChange = intent.getLongExtra(EXTRA_TOTAL_VIEWS_CHANGE, 0L)
        val totalViewsChangePercent = intent.getDoubleExtra(EXTRA_TOTAL_VIEWS_CHANGE_PERCENT, 0.0)
        val dateRange = intent.getStringExtra(EXTRA_DATE_RANGE) ?: ""

        setContent {
            AppThemeM3 {
                MostViewedDetailScreen(
                    dataSource = dataSource,
                    items = items,
                    totalViews = totalViews,
                    totalViewsChange = totalViewsChange,
                    totalViewsChangePercent = totalViewsChangePercent,
                    dateRange = dateRange,
                    onBackPressed = onBackPressedDispatcher::onBackPressed
                )
            }
        }
    }

    companion object {
        @Suppress("LongParameterList")
        fun start(
            context: Context,
            dataSource: MostViewedDataSource,
            items: List<MostViewedDetailItem>,
            totalViews: Long,
            totalViewsChange: Long,
            totalViewsChangePercent: Double,
            dateRange: String
        ) {
            val intent = Intent(context, MostViewedDetailActivity::class.java).apply {
                putExtra(EXTRA_DATA_SOURCE, dataSource)
                putExtra(EXTRA_ITEMS, ArrayList(items))
                putExtra(EXTRA_TOTAL_VIEWS, totalViews)
                putExtra(EXTRA_TOTAL_VIEWS_CHANGE, totalViewsChange)
                putExtra(EXTRA_TOTAL_VIEWS_CHANGE_PERCENT, totalViewsChangePercent)
                putExtra(EXTRA_DATE_RANGE, dateRange)
            }
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MostViewedDetailScreen(
    dataSource: MostViewedDataSource,
    items: List<MostViewedDetailItem>,
    totalViews: Long,
    totalViewsChange: Long,
    totalViewsChangePercent: Double,
    dateRange: String,
    onBackPressed: () -> Unit
) {
    val title = stringResource(dataSource.labelResId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
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
                Spacer(modifier = Modifier.height(8.dp))
                SummaryCard(
                    totalViews = totalViews,
                    totalViewsChange = totalViewsChange,
                    totalViewsChangePercent = totalViewsChangePercent,
                    dateRange = dateRange
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                ColumnHeaders(itemCount = items.size)
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(items) { index, item ->
                DetailItemRow(
                    position = index + 1,
                    item = item,
                    maxViews = items.firstOrNull()?.views ?: 1L
                )
                if (index < items.lastIndex) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SummaryCard(
    totalViews: Long,
    totalViewsChange: Long,
    totalViewsChangePercent: Double,
    dateRange: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.stats_views),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = dateRange,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatStatValue(totalViews),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                TotalViewsChangeIndicator(
                    change = totalViewsChange,
                    changePercent = totalViewsChangePercent
                )
            }
        }
    }
}

@Composable
private fun TotalViewsChangeIndicator(
    change: Long,
    changePercent: Double
) {
    if (change == 0L) return

    val isPositive = change > 0
    val sign = if (isPositive) "+" else "-"
    val color = if (isPositive) StatsColors.ChangeBadgePositive else StatsColors.ChangeBadgeNegative
    val arrowIcon = if (isPositive) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = arrowIcon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Text(
            text = "$sign${formatStatValue(abs(change))} (${
                String.format(Locale.getDefault(), "%.1f%%", abs(changePercent))
            })",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun ColumnHeaders(itemCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.stats_most_viewed_top_n, itemCount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.stats_views),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetailItemRow(
    position: Int,
    item: MostViewedDetailItem,
    maxViews: Long
) {
    val percentage = if (maxViews > 0) item.views.toFloat() / maxViews.toFloat() else 0f
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(8.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = percentage)
                .fillMaxHeight()
                .background(barColor)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = position.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp)
            )

            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatStatValue(item.views),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ChangeIndicator(change = item.change)
            }
        }
    }
}

@Composable
private fun ChangeIndicator(change: MostViewedChange) {
    val (text, color) = when (change) {
        is MostViewedChange.Positive -> Pair(
            "+${formatStatValue(change.value)} (${
                String.format(Locale.getDefault(), "%.1f%%", change.percentage)
            })",
            StatsColors.ChangeBadgePositive
        )
        is MostViewedChange.Negative -> Pair(
            "-${formatStatValue(change.value)} (${
                String.format(Locale.getDefault(), "%.1f%%", change.percentage)
            })",
            StatsColors.ChangeBadgeNegative
        )
        is MostViewedChange.NoChange -> Pair(
            "+0 (0%)",
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        is MostViewedChange.NotAvailable -> return
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}

@Preview(showBackground = true)
@Composable
private fun MostViewedDetailScreenPreview() {
    AppThemeM3 {
        MostViewedDetailScreen(
            dataSource = MostViewedDataSource.POSTS_AND_PAGES,
            items = listOf(
                MostViewedDetailItem(1, "Welcome to Automattic", 998,
                    MostViewedChange.Positive(41, 4.3)),
                MostViewedDetailItem(2, "Travel Guidelines", 111,
                    MostViewedChange.Positive(22, 24.7)),
                MostViewedDetailItem(3, "LibreChat", 93,
                    MostViewedChange.Positive(21, 29.2)),
                MostViewedDetailItem(4, "Getting Started with Claude Code: A Comprehensive Tutorial", 91,
                    MostViewedChange.Positive(47, 106.8)),
                MostViewedDetailItem(5, "AI Tools & Resource Hub", 72,
                    MostViewedChange.Positive(31, 75.6))
            ),
            totalViews = 5400,
            totalViewsChange = 69,
            totalViewsChangePercent = 1.3,
            dateRange = "21-27 Jan",
            onBackPressed = {}
        )
    }
}
