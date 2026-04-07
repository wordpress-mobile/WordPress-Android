package org.wordpress.android.ui.newstats.utm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.components.StatsCardEmptyContent
import org.wordpress.android.ui.newstats.components.StatsCardErrorContent
import org.wordpress.android.ui.newstats.components.StatsItemName
import org.wordpress.android.ui.newstats.components.StatsListHeader
import org.wordpress.android.ui.newstats.components.StatsListRowContainer
import org.wordpress.android.ui.newstats.components.StatsSummaryCard
import org.wordpress.android.ui.newstats.util.ShimmerBox
import org.wordpress.android.ui.newstats.util.formatStatValue

@AndroidEntryPoint
class UtmDetailActivity : BaseAppCompatActivity() {
    private val viewModel: UtmDetailViewModel
        by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadData()

        setContent {
            AppThemeM3 {
                val uiState by viewModel.uiState
                    .collectAsState()
                UtmDetailScreen(
                    uiState = uiState,
                    onRetry = viewModel::retry,
                    onBackPressed =
                        onBackPressedDispatcher
                            ::onBackPressed
                )
            }
        }
    }

    companion object {
        fun start(
            context: Context,
            category: UtmCategory,
            period: StatsPeriod
        ) {
            val intent = Intent(
                context,
                UtmDetailActivity::class.java
            ).apply {
                putExtra(
                    UtmDetailViewModel
                        .EXTRA_CATEGORY_NAME,
                    category.name
                )
                putExtra(
                    UtmDetailViewModel
                        .EXTRA_PERIOD_TYPE,
                    period.toTypeString()
                )
                if (period is StatsPeriod.Custom) {
                    putExtra(
                        UtmDetailViewModel
                            .EXTRA_CUSTOM_START_DATE,
                        period.startDate.toEpochDay()
                    )
                    putExtra(
                        UtmDetailViewModel
                            .EXTRA_CUSTOM_END_DATE,
                        period.endDate.toEpochDay()
                    )
                }
            }
            context.startActivity(intent)
        }
    }
}

private const val LOADING_ITEM_COUNT = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UtmDetailScreen(
    uiState: UtmDetailUiState,
    onRetry: () -> Unit,
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
            is UtmDetailUiState.Loading ->
                DetailLoadingContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .padding(horizontal = 16.dp)
                )
            is UtmDetailUiState.Error ->
                StatsCardErrorContent(
                    titleResId =
                        R.string.stats_utm_title,
                    errorMessageResId =
                        uiState.messageResId,
                    onRetry = onRetry,
                    onRemoveCard = {},
                    cardPosition = null,
                    onMoveUp = null,
                    onMoveToTop = null,
                    onMoveDown = null,
                    onMoveToBottom = null
                )
            is UtmDetailUiState.Loaded ->
                DetailLoadedContent(
                    state = uiState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .padding(horizontal = 16.dp)
                )
        }
    }
}

@Composable
private fun DetailLoadingContent(
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        item {
            Spacer(
                modifier = Modifier.height(16.dp)
            )
        }
        items(LOADING_ITEM_COUNT) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                )
                Spacer(
                    modifier = Modifier.width(12.dp)
                )
                ShimmerBox(
                    modifier = Modifier
                        .width(50.dp)
                        .height(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailLoadedContent(
    state: UtmDetailUiState.Loaded,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        item {
            Spacer(
                modifier = Modifier.height(8.dp)
            )
            StatsSummaryCard(
                totalViews = state.totalViews,
                dateRange = state.dateRange,
                totalViewsChange =
                    state.totalViewsChange,
                totalViewsChangePercent =
                    state.totalViewsChangePercent
            )
            Spacer(
                modifier = Modifier.height(16.dp)
            )
        }

        if (state.items.isEmpty()) {
            item { StatsCardEmptyContent() }
        } else {
            item {
                StatsListHeader(
                    leftHeaderResId =
                        state.categoryLabelResId
                )
                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            itemsIndexed(
                state.items
            ) { index, item ->
                val percentage =
                    if (state.maxViewsForBar > 0) {
                        item.views.toFloat() /
                            state.maxViewsForBar
                                .toFloat()
                    } else {
                        0f
                    }
                DetailUtmRow(
                    position = index + 1,
                    item = item,
                    percentage = percentage
                )
                if (index < state.items.lastIndex) {
                    Spacer(
                        modifier = Modifier
                            .height(4.dp)
                    )
                }
            }
        }

        item {
            Spacer(
                modifier = Modifier.height(16.dp)
            )
        }
    }
}

@Composable
private fun DetailUtmRow(
    position: Int,
    item: UtmUiItem,
    percentage: Float
) {
    var expanded by remember {
        mutableStateOf(false)
    }
    val hasTopPosts = item.topPosts.isNotEmpty()

    Column {
        StatsListRowContainer(
            percentage = percentage
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (hasTopPosts) {
                            Modifier.clickable {
                                expanded = !expanded
                            }
                        } else {
                            Modifier
                        }
                    )
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
                if (hasTopPosts) {
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Default
                                .KeyboardArrowUp
                        } else {
                            Icons.Default
                                .KeyboardArrowDown
                        },
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp),
                        tint = MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                    )
                }
                Spacer(
                    modifier = Modifier.width(4.dp)
                )
                Text(
                    text = formatStatValue(
                        item.views
                    ),
                    style = MaterialTheme.typography
                        .bodyMedium,
                    color = MaterialTheme.colorScheme
                        .onSurface
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(
                    start = 24.dp
                )
            ) {
                item.topPosts.forEach { post ->
                    DetailUtmPostRow(post = post)
                }
            }
        }
    }
}

@Composable
private fun DetailUtmPostRow(
    post: UtmPostUiItem
) {
    StatsListRowContainer(percentage = 0f) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 8.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text = post.title,
                style = MaterialTheme.typography
                    .bodySmall,
                color = MaterialTheme.colorScheme
                    .onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(
                modifier = Modifier.width(8.dp)
            )
            Text(
                text = formatStatValue(post.views),
                style = MaterialTheme.typography
                    .bodySmall,
                color = MaterialTheme.colorScheme
                    .onSurface
            )
        }
    }
}
