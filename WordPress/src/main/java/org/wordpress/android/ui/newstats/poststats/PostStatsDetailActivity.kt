package org.wordpress.android.ui.newstats.poststats

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.newstats.components.StatsBarChart
import org.wordpress.android.ui.newstats.components.StatsChangeIndicator
import org.wordpress.android.ui.newstats.components.StatsLabeledValue
import org.wordpress.android.ui.newstats.components.StatsViewChange
import org.wordpress.android.ui.newstats.datasource.PostViewsChange
import org.wordpress.android.ui.newstats.datasource.PostViewsData
import org.wordpress.android.ui.newstats.datasource.PostViewsWeek
import org.wordpress.android.ui.newstats.util.ShimmerBox
import org.wordpress.android.ui.newstats.util.formatStatValue
import org.wordpress.android.ui.newstats.util.formatStatsDate
import org.wordpress.android.ui.newstats.util.formatStatsDateTime

private val ScreenPadding = 16.dp
private val ChartHeight = 120.dp
private const val LOADING_SHIMMER_ITEM_COUNT = 6

@AndroidEntryPoint
class PostStatsDetailActivity : BaseAppCompatActivity() {
    private val viewModel: PostStatsDetailViewModel
        by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val postId = intent.getLongExtra(ARG_POST_ID, 0L)
        // The view model survives recreation, so only the first creation kicks off the fetch --
        // otherwise every rotation or theme change refetches the post's whole view history.
        if (savedInstanceState == null) {
            viewModel.loadData(postId)
        }

        setContent {
            AppThemeM3 {
                val uiState by viewModel.uiState
                    .collectAsState()
                PostStatsDetailScreen(
                    uiState = uiState,
                    onBackPressed =
                        onBackPressedDispatcher
                            ::onBackPressed,
                    onRetry = { viewModel.loadData(postId) }
                )
            }
        }
    }

    companion object {
        private const val ARG_POST_ID = "post_id"

        fun start(context: Context, postId: Long) {
            context.startActivity(
                Intent(
                    context,
                    PostStatsDetailActivity::class.java
                ).putExtra(ARG_POST_ID, postId)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostStatsDetailScreen(
    uiState: PostStatsDetailUiState,
    onBackPressed: () -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            R.string.stats_post_detail_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons
                                .AutoMirrored
                                .Filled.ArrowBack,
                            contentDescription =
                                stringResource(
                                    R.string.back
                                )
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is PostStatsDetailUiState.Loading ->
                    LoadingContent()
                is PostStatsDetailUiState.Error ->
                    ErrorContent(uiState.message, onRetry)
                is PostStatsDetailUiState.Loaded ->
                    LoadedContent(uiState.data)
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(LOADING_SHIMMER_ITEM_COUNT) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(ScreenPadding),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.retry))
        }
    }
}

@Composable
private fun LoadedContent(data: PostViewsData) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(ScreenPadding)
    ) {
        item { PostHeader(data) }
        item { Spacer(modifier = Modifier.height(24.dp)) }

        if (data.recentDailyViews.any { it > 0L }) {
            item {
                SectionTitle(R.string.stats_views)
                StatsBarChart(
                    values = data.recentDailyViews,
                    height = ChartHeight
                )
                Spacer(
                    modifier = Modifier.height(24.dp)
                )
            }
        }

        statsSection(
            R.string.stats_detail_recent_weeks,
            data.weeks
        ) { WeekRow(it) }

        statsSection(
            R.string.stats_detail_months_and_years,
            data.years
        ) {
            DetailRow(
                label = it.year,
                value = formatStatValue(it.total)
            )
        }

        statsSection(
            R.string.stats_detail_average_views_per_day,
            data.averages
        ) {
            DetailRow(
                label = it.year,
                value = formatStatValue(it.overall)
            )
        }
    }
}

/**
 * A titled list section, skipped entirely when [items] is empty.
 */
private fun <T> LazyListScope.statsSection(
    @StringRes titleResId: Int,
    items: List<T>,
    row: @Composable (T) -> Unit
) {
    if (items.isEmpty()) return
    item { SectionTitle(titleResId) }
    items(items.size) { index -> row(items[index]) }
    item { Spacer(modifier = Modifier.height(24.dp)) }
}

@Composable
private fun PostHeader(data: PostViewsData) {
    Column {
        Text(
            text = data.postTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatStatsDateTime(data.postDate),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(24.dp)
        ) {
            StatsLabeledValue(
                labelResId = R.string.stats_views,
                value = data.totalViews
            )
            StatsLabeledValue(
                labelResId = R.string.stats_likes,
                value = data.likeCount
            )
            StatsLabeledValue(
                labelResId = R.string.stats_comments,
                value = data.commentCount
            )
        }
    }
}

@Composable
private fun SectionTitle(titleResId: Int) {
    Text(
        text = stringResource(titleResId),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun WeekRow(week: PostViewsWeek) {
    DetailRow(
        label = weekLabel(week),
        value = formatStatValue(week.total)
    ) {
        when (val change = week.change) {
            is PostViewsChange.Infinite -> Text(
                text = INFINITY,
                style = MaterialTheme.typography
                    .labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            is PostViewsChange.Percentage ->
                StatsChangeIndicator(
                    change = change.value.toViewChange()
                )
            is PostViewsChange.None -> Unit
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        trailing()
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

private fun weekLabel(week: PostViewsWeek): String {
    val start = formatStatsDate(week.startDay)
    val end = formatStatsDate(week.endDay)
    return if (start == end) start else "$start - $end"
}

// StatsChangeIndicator renders only the percentage, so the delta the sealed class also carries is
// not something this screen has -- a week's change arrives as a percentage, not a view count.
private fun Double.toViewChange(): StatsViewChange = when {
    this > 0 -> StatsViewChange.Positive(0L, this)
    this < 0 -> StatsViewChange.Negative(0L, -this)
    else -> StatsViewChange.NoChange
}

private const val INFINITY = "∞"
