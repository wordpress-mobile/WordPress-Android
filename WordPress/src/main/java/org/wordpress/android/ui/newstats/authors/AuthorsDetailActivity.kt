package org.wordpress.android.ui.newstats.authors

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.components.StatsDetailListItem
import org.wordpress.android.ui.newstats.components.StatsListHeader
import org.wordpress.android.ui.newstats.components.StatsSummaryCard
import org.wordpress.android.ui.newstats.components.StatsViewChange

private const val EXTRA_PERIOD_TYPE = "extra_period_type"
private const val EXTRA_PERIOD_CUSTOM_START = "extra_period_custom_start"
private const val EXTRA_PERIOD_CUSTOM_END = "extra_period_custom_end"
private const val NO_EPOCH_DAY = -1L

@AndroidEntryPoint
class AuthorsDetailActivity : BaseAppCompatActivity() {
    private val viewModel: AuthorsDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.load(readPeriod())

        setContent {
            AppThemeM3 {
                val uiState by viewModel.uiState.collectAsState()
                AuthorsDetailScreen(
                    uiState = uiState,
                    onBackPressed = onBackPressedDispatcher::onBackPressed,
                    onRetry = { viewModel.retry() },
                    onOpenWpAdmin = {
                        viewModel.getAdminUrl()?.let { ActivityLauncher.openUrlExternal(this, it) }
                    }
                )
            }
        }
    }

    private fun readPeriod(): StatsPeriod {
        val periodType = intent.getStringExtra(EXTRA_PERIOD_TYPE) ?: return StatsPeriod.Last7Days
        val customStart = intent.getLongExtra(EXTRA_PERIOD_CUSTOM_START, NO_EPOCH_DAY)
        val customEnd = intent.getLongExtra(EXTRA_PERIOD_CUSTOM_END, NO_EPOCH_DAY)
        return StatsPeriod.fromTypeString(
            type = periodType,
            customStartEpochDay = customStart.takeIf { it != NO_EPOCH_DAY },
            customEndEpochDay = customEnd.takeIf { it != NO_EPOCH_DAY }
        )
    }

    companion object {
        /**
         * Launches the authors detail screen, which self-fetches the full (unbounded) authors list
         * for [period] rather than receiving it through the Intent (see [AuthorsDetailViewModel]).
         */
        fun startSelfFetch(context: Context, period: StatsPeriod) {
            val intent = Intent(context, AuthorsDetailActivity::class.java).apply {
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
private fun AuthorsDetailScreen(
    uiState: AuthorsDetailUiState,
    onBackPressed: () -> Unit,
    onRetry: () -> Unit = {},
    onOpenWpAdmin: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.stats_authors_title)) },
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
            is AuthorsDetailUiState.Loading -> AuthorsLoadingContent(contentModifier)
            is AuthorsDetailUiState.Error -> AuthorsErrorContent(
                message = uiState.message,
                onRetry = onRetry,
                onOpenWpAdmin = if (uiState.isAuthError) onOpenWpAdmin else null,
                modifier = contentModifier
            )
            is AuthorsDetailUiState.Loaded -> AuthorsLoadedContent(uiState, contentModifier)
        }
    }
}

@Composable
private fun AuthorsLoadedContent(
    state: AuthorsDetailUiState.Loaded,
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
            StatsListHeader(leftHeaderResId = R.string.stats_authors_author_header)
            Spacer(modifier = Modifier.height(8.dp))
        }

        itemsIndexed(state.authors) { index, author ->
            val percentage = if (state.maxViewsForBar > 0) {
                author.views.toFloat() / state.maxViewsForBar.toFloat()
            } else 0f
            DetailAuthorRow(
                position = index + 1,
                author = author,
                percentage = percentage
            )
            if (index < state.authors.lastIndex) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AuthorsLoadingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AuthorsErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenWpAdmin: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (onOpenWpAdmin != null) {
            Button(onClick = onOpenWpAdmin) {
                Text(text = stringResource(R.string.my_site_btn_wp_admin))
            }
        } else {
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun DetailAuthorRow(
    position: Int,
    author: AuthorUiItem,
    percentage: Float
) {
    StatsDetailListItem(
        position = position,
        percentage = percentage,
        name = author.name,
        views = author.views,
        change = author.change,
        icon = { AuthorAvatar(avatarUrl = author.avatarUrl, name = author.name) }
    )
}

@Preview(showBackground = true)
@Composable
private fun AuthorsDetailScreenPreview() {
    AppThemeM3 {
        AuthorsDetailScreen(
            uiState = AuthorsDetailUiState.Loaded(
                authors = listOf(
                    AuthorUiItem("John Doe", null, 3464, StatsViewChange.Positive(124, 3.7)),
                    AuthorUiItem("Jane Smith", null, 556, StatsViewChange.Positive(45, 8.8)),
                    AuthorUiItem("Bob Johnson", null, 522, StatsViewChange.Negative(12, 2.2)),
                    AuthorUiItem("Alice Brown", null, 485, StatsViewChange.Positive(33, 7.3)),
                    AuthorUiItem("Charlie Wilson", null, 412, StatsViewChange.NoChange)
                ),
                maxViewsForBar = 3464,
                totalViews = 6726,
                totalViewsChange = 225,
                totalViewsChangePercent = 3.5,
                dateRange = "Last 7 days"
            ),
            onBackPressed = {}
        )
    }
}
