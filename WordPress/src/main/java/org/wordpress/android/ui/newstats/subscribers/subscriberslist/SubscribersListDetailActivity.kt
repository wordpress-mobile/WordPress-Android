package org.wordpress.android.ui.newstats.subscribers.subscriberslist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity

private const val LOAD_MORE_THRESHOLD = 5

@AndroidEntryPoint
class SubscribersListDetailActivity :
    BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppThemeM3 {
                SubscribersListDetailScreen(
                    onBackPressed =
                        onBackPressedDispatcher::onBackPressed
                )
            }
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(
                context,
                SubscribersListDetailActivity::class.java
            )
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscribersListDetailScreen(
    viewModel: SubscribersListDetailViewModel =
        viewModel(),
    onBackPressed: () -> Unit
) {
    val items by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by
        viewModel.isLoadingMore.collectAsState()
    val canLoadMore by
        viewModel.canLoadMore.collectAsState()
    val hasError by viewModel.hasError.collectAsState()

    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.loadInitialPage()
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible =
                listState.layoutInfo.visibleItemsInfo
                    .lastOrNull()?.index ?: 0
            val totalItems =
                listState.layoutInfo.totalItemsCount
            canLoadMore && !isLoading &&
                !isLoadingMore && totalItems > 0 &&
                lastVisible >= totalItems -
                LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    val title = stringResource(
        R.string.stats_subscribers_list
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(
                        onClick = onBackPressed
                    ) {
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (hasError) {
            ErrorContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                onRetry = {
                    viewModel.loadInitialPage()
                }
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )
                    DetailColumnHeaders(
                        itemCount = items.size
                    )
                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )
                }

                itemsIndexed(items) { index, item ->
                    DetailSubscriberRow(item = item)
                    if (index < items.lastIndex) {
                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )
                    }
                }

                if (isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment =
                                Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(24.dp),
                                strokeWidth = 2.dp
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
    }
}

@Composable
private fun DetailColumnHeaders(itemCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(
                R.string.stats_most_viewed_top_n,
                itemCount
            ),
            style = MaterialTheme
                .typography.labelMedium,
            color = MaterialTheme
                .colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(
                R.string.stats_subscribers_since_header
            ),
            style = MaterialTheme
                .typography.labelMedium,
            color = MaterialTheme
                .colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetailSubscriberRow(
    item: SubscriberListItem
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme
                .colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.formattedDate,
            style = MaterialTheme
                .typography.bodySmall,
            color = MaterialTheme
                .colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(
                    R.string.stats_error_api
                ),
                style = MaterialTheme
                    .typography.bodyMedium,
                color = MaterialTheme
                    .colorScheme.onSurfaceVariant
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
}
