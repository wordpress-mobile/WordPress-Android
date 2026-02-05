package org.wordpress.android.ui.newstats.topauthors

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
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
import coil.compose.AsyncImage
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.newstats.components.StatsChangeIndicator
import org.wordpress.android.ui.newstats.components.StatsSummaryCard
import org.wordpress.android.ui.newstats.components.StatsViewChange
import org.wordpress.android.ui.newstats.util.formatStatValue
import org.wordpress.android.util.extensions.getParcelableArrayListCompat

private const val EXTRA_AUTHORS = "extra_authors"
private const val EXTRA_TOTAL_VIEWS = "extra_total_views"
private const val EXTRA_TOTAL_VIEWS_CHANGE = "extra_total_views_change"
private const val EXTRA_TOTAL_VIEWS_CHANGE_PERCENT = "extra_total_views_change_percent"
private const val EXTRA_DATE_RANGE = "extra_date_range"

@AndroidEntryPoint
class TopAuthorsDetailActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authors = intent.extras
            ?.getParcelableArrayListCompat<TopAuthorUiItem>(EXTRA_AUTHORS)
            ?: arrayListOf()
        val totalViews = intent.getLongExtra(EXTRA_TOTAL_VIEWS, 0L)
        val totalViewsChange = intent.getLongExtra(EXTRA_TOTAL_VIEWS_CHANGE, 0L)
        val totalViewsChangePercent = intent.getDoubleExtra(EXTRA_TOTAL_VIEWS_CHANGE_PERCENT, 0.0)
        val dateRange = intent.getStringExtra(EXTRA_DATE_RANGE) ?: ""
        // Calculate maxViewsForBar once (list is sorted by views descending)
        val maxViewsForBar = authors.firstOrNull()?.views ?: 1L

        setContent {
            AppThemeM3 {
                TopAuthorsDetailScreen(
                    authors = authors,
                    maxViewsForBar = maxViewsForBar,
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
            authors: List<TopAuthorUiItem>,
            totalViews: Long,
            totalViewsChange: Long,
            totalViewsChangePercent: Double,
            dateRange: String
        ) {
            val intent = Intent(context, TopAuthorsDetailActivity::class.java).apply {
                putExtra(EXTRA_AUTHORS, ArrayList(authors))
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
private fun TopAuthorsDetailScreen(
    authors: List<TopAuthorUiItem>,
    maxViewsForBar: Long,
    totalViews: Long,
    totalViewsChange: Long,
    totalViewsChangePercent: Double,
    dateRange: String,
    onBackPressed: () -> Unit
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Summary card
                StatsSummaryCard(
                    totalViews = totalViews,
                    dateRange = dateRange,
                    totalViewsChange = totalViewsChange,
                    totalViewsChangePercent = totalViewsChangePercent
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.stats_authors_author_header),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.stats_countries_views_header),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(authors) { index, author ->
                val percentage = if (maxViewsForBar > 0) {
                    author.views.toFloat() / maxViewsForBar.toFloat()
                } else 0f
                DetailAuthorRow(
                    position = index + 1,
                    author = author,
                    percentage = percentage
                )
                if (index < authors.lastIndex) {
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
private fun DetailAuthorRow(
    position: Int,
    author: TopAuthorUiItem,
    percentage: Float
) {
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Background bar representing the percentage
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
            // Position number
            Text(
                text = position.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp)
            )

            // Avatar (circular)
            if (author.avatarUrl != null) {
                AsyncImage(
                    model = author.avatarUrl,
                    contentDescription = author.name,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            // Author name
            Text(
                text = author.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Views count and change
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatStatValue(author.views),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                StatsChangeIndicator(change = author.change)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TopAuthorsDetailScreenPreview() {
    AppThemeM3 {
        TopAuthorsDetailScreen(
            authors = listOf(
                TopAuthorUiItem("John Doe", null, 3464, StatsViewChange.Positive(124, 3.7)),
                TopAuthorUiItem("Jane Smith", null, 556, StatsViewChange.Positive(45, 8.8)),
                TopAuthorUiItem("Bob Johnson", null, 522, StatsViewChange.Negative(12, 2.2)),
                TopAuthorUiItem("Alice Brown", null, 485, StatsViewChange.Positive(33, 7.3)),
                TopAuthorUiItem("Charlie Wilson", null, 412, StatsViewChange.NoChange),
                TopAuthorUiItem("Diana Miller", null, 387, StatsViewChange.Negative(8, 2.0)),
                TopAuthorUiItem("Edward Davis", null, 298, StatsViewChange.Positive(21, 7.6)),
                TopAuthorUiItem("Fiona Garcia", null, 245, StatsViewChange.Positive(15, 6.5)),
                TopAuthorUiItem("George Martinez", null, 201, StatsViewChange.Negative(5, 2.4)),
                TopAuthorUiItem("Hannah Anderson", null, 156, StatsViewChange.Positive(12, 8.3))
            ),
            maxViewsForBar = 3464,
            totalViews = 6726,
            totalViewsChange = 225,
            totalViewsChangePercent = 3.5,
            dateRange = "Last 7 days",
            onBackPressed = {}
        )
    }
}
