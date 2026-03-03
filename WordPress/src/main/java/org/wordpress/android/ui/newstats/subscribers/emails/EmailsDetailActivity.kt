package org.wordpress.android.ui.newstats.subscribers.emails

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.newstats.util.formatEmailStat

@AndroidEntryPoint
class EmailsDetailActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppThemeM3 {
                EmailsDetailScreen(
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
                EmailsDetailActivity::class.java
            )
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmailsDetailScreen(
    viewModel: EmailsDetailViewModel = viewModel(),
    onBackPressed: () -> Unit
) {
    val items by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasError by viewModel.hasError.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    val title = stringResource(
        R.string.stats_subscribers_emails
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
                onRetry = { viewModel.loadData() }
            )
        } else {
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
                    DetailEmailColumnHeaders()
                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )
                }

                itemsIndexed(items) { index, item ->
                    DetailEmailRow(item = item)
                    if (index < items.lastIndex) {
                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
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
}

@Composable
private fun DetailEmailColumnHeaders() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(
                R.string.stats_emails_latest_header
            ),
            style = MaterialTheme
                .typography.labelMedium,
            color = MaterialTheme
                .colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(
                R.string.stats_emails_opens_header
            ),
            style = MaterialTheme
                .typography.labelMedium,
            color = MaterialTheme
                .colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(56.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(
                R.string.stats_emails_clicks_header
            ),
            style = MaterialTheme
                .typography.labelMedium,
            color = MaterialTheme
                .colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(56.dp)
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
    HorizontalDivider(
        color = MaterialTheme
            .colorScheme.outlineVariant
    )
}

@Composable
private fun DetailEmailRow(item: EmailListItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme
                .colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatEmailStat(item.opens),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (item.opens == 0L) {
                MaterialTheme
                    .colorScheme.onSurfaceVariant
            } else {
                MaterialTheme
                    .colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.width(56.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = formatEmailStat(item.clicks),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (item.clicks == 0L) {
                MaterialTheme
                    .colorScheme.onSurfaceVariant
            } else {
                MaterialTheme
                    .colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.width(56.dp)
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
