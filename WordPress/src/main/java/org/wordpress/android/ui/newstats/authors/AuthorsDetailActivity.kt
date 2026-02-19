package org.wordpress.android.ui.newstats.authors

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.newstats.components.StatsDetailListItem
import org.wordpress.android.ui.newstats.components.StatsListHeader
import org.wordpress.android.ui.newstats.components.StatsSummaryCard
import org.wordpress.android.ui.newstats.components.StatsViewChange
import org.wordpress.android.util.extensions.getParcelableArrayListCompat

private const val EXTRA_AUTHORS = "extra_authors"
private const val EXTRA_TOTAL_VIEWS = "extra_total_views"
private const val EXTRA_TOTAL_VIEWS_CHANGE = "extra_total_views_change"
private const val EXTRA_TOTAL_VIEWS_CHANGE_PERCENT = "extra_total_views_change_percent"
private const val EXTRA_DATE_RANGE = "extra_date_range"

@AndroidEntryPoint
class AuthorsDetailActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authors = intent.extras
            ?.getParcelableArrayListCompat<AuthorUiItem>(EXTRA_AUTHORS)
            ?: arrayListOf()
        val totalViews = intent.getLongExtra(EXTRA_TOTAL_VIEWS, 0L)
        val totalViewsChange = intent.getLongExtra(EXTRA_TOTAL_VIEWS_CHANGE, 0L)
        val totalViewsChangePercent = intent.getDoubleExtra(EXTRA_TOTAL_VIEWS_CHANGE_PERCENT, 0.0)
        val dateRange = intent.getStringExtra(EXTRA_DATE_RANGE) ?: ""
        // Calculate maxViewsForBar once (list is sorted by views descending)
        val maxViewsForBar = authors.firstOrNull()?.views ?: 0L

        setContent {
            AppThemeM3 {
                AuthorsDetailScreen(
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
            authors: List<AuthorUiItem>,
            totalViews: Long,
            totalViewsChange: Long,
            totalViewsChangePercent: Double,
            dateRange: String
        ) {
            val intent = Intent(context, AuthorsDetailActivity::class.java).apply {
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
private fun AuthorsDetailScreen(
    authors: List<AuthorUiItem>,
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
                StatsSummaryCard(
                    totalViews = totalViews,
                    dateRange = dateRange,
                    totalViewsChange = totalViewsChange,
                    totalViewsChangePercent = totalViewsChangePercent
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                StatsListHeader(leftHeaderResId = R.string.stats_authors_author_header)
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
            authors = listOf(
                AuthorUiItem("John Doe", null, 3464, StatsViewChange.Positive(124, 3.7)),
                AuthorUiItem("Jane Smith", null, 556, StatsViewChange.Positive(45, 8.8)),
                AuthorUiItem("Bob Johnson", null, 522, StatsViewChange.Negative(12, 2.2)),
                AuthorUiItem("Alice Brown", null, 485, StatsViewChange.Positive(33, 7.3)),
                AuthorUiItem("Charlie Wilson", null, 412, StatsViewChange.NoChange),
                AuthorUiItem("Diana Miller", null, 387, StatsViewChange.Negative(8, 2.0)),
                AuthorUiItem("Edward Davis", null, 298, StatsViewChange.Positive(21, 7.6)),
                AuthorUiItem("Fiona Garcia", null, 245, StatsViewChange.Positive(15, 6.5)),
                AuthorUiItem("George Martinez", null, 201, StatsViewChange.Negative(5, 2.4)),
                AuthorUiItem("Hannah Anderson", null, 156, StatsViewChange.Positive(12, 8.3))
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
