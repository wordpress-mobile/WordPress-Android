package org.wordpress.android.ui.newstats.mostviewed

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityNavigator
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.newstats.StatsCardType
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.components.StatsSummaryCard
import org.wordpress.android.util.extensions.getParcelableArrayListCompat
import org.wordpress.android.util.extensions.getSerializableCompat
import javax.inject.Inject

private const val EXTRA_CARD_TYPE = "extra_card_type"
private const val EXTRA_ITEMS = "extra_items"
private const val EXTRA_TOTAL_VIEWS = "extra_total_views"
private const val EXTRA_TOTAL_VIEWS_CHANGE = "extra_total_views_change"
private const val EXTRA_TOTAL_VIEWS_CHANGE_PERCENT = "extra_total_views_change_percent"
private const val EXTRA_DATE_RANGE = "extra_date_range"
private const val EXTRA_VALUE_HEADER_RES_ID = "extra_value_header_res_id"

// Self-fetch mode (referrers): the detail screen re-fetches its own unbounded data for this period
// instead of receiving the item list through the Intent. When these are absent the screen renders
// the items passed via EXTRA_ITEMS (posts, clicks, search terms, etc.).
private const val EXTRA_PERIOD_TYPE = "extra_period_type"
private const val EXTRA_PERIOD_CUSTOM_START = "extra_period_custom_start"
private const val EXTRA_PERIOD_CUSTOM_END = "extra_period_custom_end"
private const val NO_EPOCH_DAY = -1L

@AndroidEntryPoint
class MostViewedDetailActivity : BaseAppCompatActivity() {
    @Inject lateinit var activityNavigator: ActivityNavigator

    private val viewModel: MostViewedDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cardType = intent.extras?.getSerializableCompat<StatsCardType>(EXTRA_CARD_TYPE)
            ?: StatsCardType.MOST_VIEWED_POSTS_AND_PAGES
        val valueHeaderResId = intent.getIntExtra(EXTRA_VALUE_HEADER_RES_ID, R.string.stats_views)
        val selfFetchPeriod = readSelfFetchPeriod()

        if (selfFetchPeriod != null) {
            viewModel.loadReferrers(selfFetchPeriod)
        }

        setContent {
            AppThemeM3 {
                val uiState = if (selfFetchPeriod != null) {
                    viewModel.uiState.collectAsState().value
                } else {
                    remember { intent.toPassedItemsState() }
                }
                MostViewedDetailScreen(
                    cardType = cardType,
                    uiState = uiState,
                    valueHeaderResId = valueHeaderResId,
                    onBackPressed = onBackPressedDispatcher::onBackPressed,
                    onRetry = { viewModel.retry() },
                    onChildClick = { url -> activityNavigator.openInCustomTab(this, url) }
                )
            }
        }
    }

    /**
     * Reads the period for self-fetch mode, or null when the screen should render the items passed
     * via the Intent (posts, clicks, search terms, etc.).
     */
    private fun readSelfFetchPeriod(): StatsPeriod? {
        val periodType = intent.getStringExtra(EXTRA_PERIOD_TYPE) ?: return null
        val customStart = intent.getLongExtra(EXTRA_PERIOD_CUSTOM_START, NO_EPOCH_DAY)
        val customEnd = intent.getLongExtra(EXTRA_PERIOD_CUSTOM_END, NO_EPOCH_DAY)
        return StatsPeriod.fromTypeString(
            type = periodType,
            customStartEpochDay = customStart.takeIf { it != NO_EPOCH_DAY },
            customEndEpochDay = customEnd.takeIf { it != NO_EPOCH_DAY }
        )
    }

    private fun Intent.toPassedItemsState(): MostViewedDetailUiState.Loaded {
        val items = extras?.getParcelableArrayListCompat<MostViewedDetailItem>(EXTRA_ITEMS)
            ?: arrayListOf()
        return MostViewedDetailUiState.Loaded(
            items = items,
            // Calculate maxViewsForBar once (list is sorted by views descending)
            maxViewsForBar = items.firstOrNull()?.views ?: 1L,
            totalViews = getLongExtra(EXTRA_TOTAL_VIEWS, 0L),
            totalViewsChange = getLongExtra(EXTRA_TOTAL_VIEWS_CHANGE, 0L),
            totalViewsChangePercent = getDoubleExtra(EXTRA_TOTAL_VIEWS_CHANGE_PERCENT, 0.0),
            dateRange = getStringExtra(EXTRA_DATE_RANGE) ?: ""
        )
    }

    companion object {
        @Suppress("LongParameterList")
        fun start(
            context: Context,
            cardType: StatsCardType,
            items: List<MostViewedDetailItem>,
            totalViews: Long,
            totalViewsChange: Long,
            totalViewsChangePercent: Double,
            dateRange: String,
            valueHeaderResId: Int = R.string.stats_views
        ) {
            val intent = Intent(
                context, MostViewedDetailActivity::class.java
            ).apply {
                putExtra(EXTRA_CARD_TYPE, cardType)
                putExtra(EXTRA_ITEMS, ArrayList(items))
                putExtra(EXTRA_TOTAL_VIEWS, totalViews)
                putExtra(EXTRA_TOTAL_VIEWS_CHANGE, totalViewsChange)
                putExtra(
                    EXTRA_TOTAL_VIEWS_CHANGE_PERCENT,
                    totalViewsChangePercent
                )
                putExtra(EXTRA_DATE_RANGE, dateRange)
                putExtra(EXTRA_VALUE_HEADER_RES_ID, valueHeaderResId)
            }
            context.startActivity(intent)
        }

        /**
         * Launches the referrers detail screen in self-fetch mode: only the [period] is passed and
         * the screen re-fetches the full referrer list itself (see [MostViewedDetailViewModel]).
         */
        fun startReferrers(context: Context, period: StatsPeriod) {
            val intent = Intent(context, MostViewedDetailActivity::class.java).apply {
                putExtra(EXTRA_CARD_TYPE, StatsCardType.MOST_VIEWED_REFERRERS)
                putExtra(EXTRA_PERIOD_TYPE, period.toTypeString())
                if (period is StatsPeriod.Custom) {
                    putExtra(EXTRA_PERIOD_CUSTOM_START, period.startDate.toEpochDay())
                    putExtra(EXTRA_PERIOD_CUSTOM_END, period.endDate.toEpochDay())
                }
            }
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MostViewedDetailScreen(
    cardType: StatsCardType,
    uiState: MostViewedDetailUiState,
    valueHeaderResId: Int = R.string.stats_views,
    onBackPressed: () -> Unit,
    onRetry: () -> Unit = {},
    onChildClick: (String) -> Unit = {}
) {
    val title = stringResource(cardType.displayNameResId)

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
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
        when (uiState) {
            is MostViewedDetailUiState.Loading -> DetailLoadingContent(contentModifier)
            is MostViewedDetailUiState.Error -> DetailErrorContent(uiState.message, onRetry, contentModifier)
            is MostViewedDetailUiState.Loaded -> DetailLoadedContent(
                state = uiState,
                valueHeaderResId = valueHeaderResId,
                onChildClick = onChildClick,
                modifier = contentModifier
            )
        }
    }
}

@Composable
private fun DetailLoadedContent(
    state: MostViewedDetailUiState.Loaded,
    valueHeaderResId: Int,
    onChildClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            StatsSummaryCard(
                totalViews = state.totalViews,
                dateRange = state.dateRange,
                totalViewsChange = state.totalViewsChange,
                totalViewsChangePercent = state.totalViewsChangePercent
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            ColumnHeaders(
                itemCount = state.items.size,
                valueHeaderResId = valueHeaderResId
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Key by index rather than item.id: referrer ids are derived from name.hashCode(),
        // which is not guaranteed unique (empty/duplicate names collide) and would crash the
        // LazyColumn. The list is a static snapshot, so the index is a stable, unique key.
        itemsIndexed(state.items, key = { index, _ -> index }) { index, item ->
            val percentage = if (state.maxViewsForBar > 0) {
                item.views.toFloat() / state.maxViewsForBar.toFloat()
            } else 0f
            MostViewedExpandableRow(
                title = item.title,
                views = item.views,
                change = item.change,
                children = item.children,
                percentage = percentage,
                onChildClick = onChildClick,
                position = index + 1,
                childStartPadding = 32.dp
            )
            if (index < state.items.lastIndex) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailLoadingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun DetailErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.retry))
        }
    }
}

@Composable
private fun ColumnHeaders(
    itemCount: Int,
    valueHeaderResId: Int = R.string.stats_views
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(
                R.string.stats_most_viewed_top_n, itemCount
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(valueHeaderResId),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MostViewedDetailScreenPreview() {
    AppThemeM3 {
        MostViewedDetailScreen(
            cardType = StatsCardType.MOST_VIEWED_POSTS_AND_PAGES,
            uiState = MostViewedDetailUiState.Loaded(
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
                maxViewsForBar = 998,
                totalViews = 5400,
                totalViewsChange = 69,
                totalViewsChangePercent = 1.3,
                dateRange = "21-27 Jan"
            ),
            onBackPressed = {}
        )
    }
}
