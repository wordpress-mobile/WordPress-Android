package org.wordpress.android.ui.newstats.yearinreview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.newstats.util.ProvideShimmerBrush
import org.wordpress.android.ui.newstats.util.ShimmerBox
import org.wordpress.android.ui.newstats.util.formatStatValue

private val CardCornerRadius = 10.dp
private const val LOADING_SHIMMER_ITEM_COUNT = 3

@AndroidEntryPoint
class YearInReviewDetailActivity :
    BaseAppCompatActivity() {
    private val viewModel:
        YearInReviewDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.loadData()

        setContent {
            AppThemeM3 {
                val uiState by viewModel.uiState
                    .collectAsState()
                YearInReviewDetailScreen(
                    uiState = uiState,
                    onBackPressed =
                        onBackPressedDispatcher
                            ::onBackPressed,
                    onRetry = { viewModel.loadData() }
                )
            }
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(
                context,
                YearInReviewDetailActivity::class.java
            )
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearInReviewDetailScreen(
    uiState: YearInReviewDetailUiState,
    onBackPressed: () -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            R.string
                                .stats_insights_year_in_review
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackPressed
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled
                                .ArrowBack,
                            contentDescription =
                                stringResource(
                                    R.string.back
                                )
                        )
                    }
                }
            )
        }
    ) { contentPadding ->
        when (uiState) {
            is YearInReviewDetailUiState.Loading ->
                ProvideShimmerBrush {
                    DetailLoadingContent(
                        modifier = Modifier
                            .padding(contentPadding)
                    )
                }
            is YearInReviewDetailUiState.Loaded ->
                DetailLoadedContent(
                    years = uiState.years,
                    modifier = Modifier
                        .padding(contentPadding)
                )
            is YearInReviewDetailUiState.Error ->
                DetailErrorContent(
                    message = uiState.message,
                    onRetry = onRetry,
                    modifier = Modifier
                        .padding(contentPadding)
                )
        }
    }
}

@Composable
private fun DetailLoadingContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
        repeat(LOADING_SHIMMER_ITEM_COUNT) {
            LoadingYearSection()
        }
    }
}

@Composable
private fun LoadingYearSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardCornerRadius))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme
                    .outlineVariant,
                shape = RoundedCornerShape(
                    CardCornerRadius
                )
            )
            .background(
                MaterialTheme.colorScheme.surface
            )
            .padding(16.dp)
    ) {
        ShimmerBox(
            modifier = Modifier
                .width(140.dp)
                .height(24.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        repeat(4) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DetailLoadedContent(
    years: List<YearSummary>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
        items(years) { year ->
            YearDetailSection(year)
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DetailErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography
                .bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(
                text = stringResource(R.string.retry)
            )
        }
    }
}

@Composable
private fun YearDetailSection(year: YearSummary) {
    val borderColor =
        MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardCornerRadius))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(
                    CardCornerRadius
                )
            )
            .background(
                MaterialTheme.colorScheme.surface
            )
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(
                R.string
                    .stats_insights_year_in_review_title,
                year.year
            ),
            style = MaterialTheme.typography
                .titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        StatRow(
            labelRes =
                R.string.stats_insights_total_posts,
            value = formatStatValue(year.totalPosts)
        )
        StatDivider()
        StatRow(
            labelRes =
                R.string.stats_insights_total_comments,
            value = formatStatValue(year.totalComments)
        )
        StatDivider()
        StatRow(
            labelRes = R.string
                .stats_insights_avg_comments_per_post,
            value = formatStatValue(year.avgComments)
        )
        StatDivider()
        StatRow(
            labelRes =
                R.string.stats_insights_total_likes,
            value = formatStatValue(year.totalLikes)
        )
        StatDivider()
        StatRow(
            labelRes = R.string
                .stats_insights_avg_likes_per_post,
            value = formatStatValue(year.avgLikes)
        )
        StatDivider()
        StatRow(
            labelRes =
                R.string.stats_insights_total_words,
            value = formatStatValue(year.totalWords)
        )
        StatDivider()
        StatRow(
            labelRes = R.string
                .stats_insights_avg_words_per_post,
            value = formatStatValue(year.avgWords)
        )
    }
}

@Composable
private fun StatRow(
    @StringRes labelRes: Int,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme
            .outlineVariant.copy(alpha = 0.5f)
    )
}

@Preview(showBackground = true)
@Composable
private fun YearInReviewDetailScreenPreview() {
    AppThemeM3 {
        YearInReviewDetailScreen(
            uiState = YearInReviewDetailUiState.Loaded(
                years = listOf(
                    YearSummary(
                        year = "2025",
                        totalPosts = 42,
                        totalWords = 15000,
                        avgWords = 357.1,
                        totalLikes = 230,
                        avgLikes = 5.5,
                        totalComments = 85,
                        avgComments = 2.0
                    ),
                    YearSummary(
                        year = "2024",
                        totalPosts = 38,
                        totalWords = 12500,
                        avgWords = 328.9,
                        totalLikes = 180,
                        avgLikes = 4.7,
                        totalComments = 60,
                        avgComments = 1.6
                    )
                )
            ),
            onBackPressed = {},
            onRetry = {}
        )
    }
}
