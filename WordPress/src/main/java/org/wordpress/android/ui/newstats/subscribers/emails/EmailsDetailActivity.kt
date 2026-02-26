package org.wordpress.android.ui.newstats.subscribers.emails

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.newstats.util.formatStatValue
import org.wordpress.android.util.extensions.getParcelableArrayListCompat

private const val EXTRA_ITEMS = "extra_items"

@AndroidEntryPoint
class EmailsDetailActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val items = intent.extras
            ?.getParcelableArrayListCompat<EmailListItem>(
                EXTRA_ITEMS
            ) ?: arrayListOf()

        setContent {
            AppThemeM3 {
                EmailsDetailScreen(
                    items = items,
                    onBackPressed =
                        onBackPressedDispatcher::onBackPressed
                )
            }
        }
    }

    companion object {
        fun start(
            context: Context,
            items: List<EmailListItem>
        ) {
            val intent = Intent(
                context,
                EmailsDetailActivity::class.java
            ).apply {
                putExtra(
                    EXTRA_ITEMS, ArrayList(items)
                )
            }
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmailsDetailScreen(
    items: List<EmailListItem>,
    onBackPressed: () -> Unit
) {
    val title = stringResource(
        R.string.stats_subscribers_emails
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
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
                Spacer(modifier = Modifier.height(8.dp))
                DetailEmailColumnHeaders(
                    itemCount = items.size
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(items) { index, item ->
                DetailEmailRow(item = item)
                if (index < items.lastIndex) {
                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DetailEmailColumnHeaders(itemCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(
                R.string.stats_most_viewed_top_n,
                itemCount
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
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = stringResource(
                R.string.stats_emails_clicks_header
            ),
            style = MaterialTheme
                .typography.labelMedium,
            color = MaterialTheme
                .colorScheme.onSurfaceVariant,
            modifier = Modifier.width(60.dp)
        )
    }
}

@Composable
private fun DetailEmailRow(item: EmailListItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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
        Text(
            text = formatStatValue(item.opens),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme
                .colorScheme.onSurface,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = formatStatValue(item.clicks),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme
                .colorScheme.onSurface,
            modifier = Modifier.width(60.dp)
        )
    }
}
