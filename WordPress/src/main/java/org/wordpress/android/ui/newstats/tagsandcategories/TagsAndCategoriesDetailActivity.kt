package org.wordpress.android.ui.newstats.tagsandcategories

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.newstats.components.StatsListHeader
import org.wordpress.android.ui.newstats.util.ShimmerBox

private const val DETAIL_EXPANDED_START_PADDING = 52
private const val LOADING_SHIMMER_ITEM_COUNT = 10

@AndroidEntryPoint
class TagsAndCategoriesDetailActivity :
    BaseAppCompatActivity() {
    private val viewModel:
        TagsAndCategoriesDetailViewModel
            by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.loadData()

        setContent {
            AppThemeM3 {
                val uiState by viewModel.uiState
                    .collectAsState()
                TagsAndCategoriesDetailScreen(
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
                TagsAndCategoriesDetailActivity::class
                    .java
            )
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagsAndCategoriesDetailScreen(
    uiState: TagsAndCategoriesCardUiState,
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
                                .stats_insights_tags_and_categories
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
            is TagsAndCategoriesCardUiState.Loading ->
                DetailLoadingContent(
                    modifier = Modifier
                        .padding(contentPadding)
                )
            is TagsAndCategoriesCardUiState.NoData ->
                DetailEmptyContent(
                    modifier = Modifier
                        .padding(contentPadding)
                )
            is TagsAndCategoriesCardUiState.Loaded ->
                DetailLoadedContent(
                    items = uiState.items,
                    maxViews = uiState.maxViewsForBar,
                    modifier = Modifier
                        .padding(contentPadding)
                )
            is TagsAndCategoriesCardUiState.Error ->
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
            .padding(top = 16.dp)
    ) {
        StatsListHeader(
            leftHeaderResId =
                R.string
                    .stats_insights_tags_and_categories,
            rightHeaderResId =
                R.string.stats_views
        )
        Spacer(modifier = Modifier.height(8.dp))
        repeat(LOADING_SHIMMER_ITEM_COUNT) {
            Spacer(modifier = Modifier.height(12.dp))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            )
        }
    }
}

@Composable
private fun DetailEmptyContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(
                R.string
                    .stats_insights_tags_empty
            ),
            style = MaterialTheme.typography
                .bodyMedium,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DetailLoadedContent(
    items: List<TagGroupUiItem>,
    maxViews: Long,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(
                    R.string
                        .stats_insights_tags_empty
                ),
                style = MaterialTheme.typography
                    .bodyMedium,
                color = MaterialTheme.colorScheme
                    .onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val expandedGroups = remember(items) {
        mutableStateMapOf<Int, Boolean>()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement =
            Arrangement.spacedBy(4.dp)
    ) {
        item {
            StatsListHeader(
                leftHeaderResId =
                    R.string
                        .stats_insights_tags_and_categories,
                rightHeaderResId =
                    R.string.stats_views
            )
            Spacer(
                modifier = Modifier.height(8.dp)
            )
        }
        itemsIndexed(items) { index, item ->
            val percentage =
                if (maxViews > 0) {
                    item.views.toFloat() /
                        maxViews.toFloat()
                } else {
                    0f
                }
            val isExpanded =
                expandedGroups[index] == true

            TagGroupRow(
                item = item,
                percentage = percentage,
                position = index + 1,
                isExpandable = item.isExpandable,
                isExpanded = isExpanded,
                onClick = if (item.isExpandable) {
                    {
                        expandedGroups[index] =
                            !isExpanded
                    }
                } else {
                    null
                }
            )
            if (item.isExpandable) {
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    ExpandedTagsSection(
                        tags = item.tags,
                        startPadding =
                            DETAIL_EXPANDED_START_PADDING
                                .dp
                    )
                }
            }
        }
        item {
            Spacer(
                modifier = Modifier.height(8.dp)
            )
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
        verticalArrangement =
            Arrangement.Center
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
                text = stringResource(
                    R.string.retry
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TagsAndCategoriesDetailPreview() {
    AppThemeM3 {
        TagsAndCategoriesDetailScreen(
            uiState =
                TagsAndCategoriesCardUiState.Loaded(
                    items = listOf(
                        TagGroupUiItem(
                            name = "Uncategorized",
                            tags = listOf(
                                TagUiItem(
                                    name =
                                        "Uncategorized",
                                    tagType = "category"
                                )
                            ),
                            views = 83,
                            displayType =
                                TagGroupDisplayType
                                    .CATEGORY
                        ),
                        TagGroupUiItem(
                            name = "snaps",
                            tags = listOf(
                                TagUiItem(
                                    name = "snaps",
                                    tagType = "tag"
                                )
                            ),
                            views = 15,
                            displayType =
                                TagGroupDisplayType.TAG
                        )
                    ),
                    maxViewsForBar = 83
                ),
            onBackPressed = {},
            onRetry = {}
        )
    }
}
