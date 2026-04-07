package org.wordpress.android.ui.newstats.utm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
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
import org.wordpress.android.ui.newstats.components.StatsItemName
import org.wordpress.android.ui.newstats.components.StatsListHeader
import org.wordpress.android.ui.newstats.components.StatsListRowContainer
import org.wordpress.android.ui.newstats.components.StatsSummaryCard
import org.wordpress.android.ui.newstats.util.formatStatValue
import org.wordpress.android.util.extensions.getParcelableArrayListCompat

private const val EXTRA_ITEMS = "extra_items"
private const val EXTRA_TOTAL_VIEWS = "extra_total_views"
private const val EXTRA_TOTAL_VIEWS_CHANGE =
    "extra_total_views_change"
private const val EXTRA_TOTAL_VIEWS_CHANGE_PERCENT =
    "extra_total_views_change_percent"
private const val EXTRA_DATE_RANGE = "extra_date_range"
private const val EXTRA_CATEGORY_LABEL_RES_ID =
    "extra_category_label_res_id"

@AndroidEntryPoint
class UtmDetailActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val items = intent.extras
            ?.getParcelableArrayListCompat<UtmDetailItem>(
                EXTRA_ITEMS
            ) ?: arrayListOf()
        val totalViews = intent.getLongExtra(
            EXTRA_TOTAL_VIEWS, 0L
        )
        val totalViewsChange = intent.getLongExtra(
            EXTRA_TOTAL_VIEWS_CHANGE, 0L
        )
        val totalViewsChangePercent =
            intent.getDoubleExtra(
                EXTRA_TOTAL_VIEWS_CHANGE_PERCENT, 0.0
            )
        val dateRange = intent.getStringExtra(
            EXTRA_DATE_RANGE
        ) ?: ""
        val categoryLabelResId = intent.getIntExtra(
            EXTRA_CATEGORY_LABEL_RES_ID,
            R.string.stats_utm_title
        )
        val maxViewsForBar =
            items.firstOrNull()?.views ?: 0L

        setContent {
            AppThemeM3 {
                UtmDetailScreen(
                    items = items,
                    maxViewsForBar = maxViewsForBar,
                    totalViews = totalViews,
                    totalViewsChange = totalViewsChange,
                    totalViewsChangePercent =
                        totalViewsChangePercent,
                    dateRange = dateRange,
                    categoryLabelResId =
                        categoryLabelResId,
                    onBackPressed =
                        onBackPressedDispatcher::onBackPressed
                )
            }
        }
    }

    companion object {
        @Suppress("LongParameterList")
        fun start(
            context: Context,
            detailData: UtmDetailData
        ) {
            val parcelableItems = ArrayList(
                detailData.items.map { item ->
                    UtmDetailItem(
                        title = item.title,
                        views = item.views,
                        topPosts = item.topPosts.map {
                            UtmDetailPostItem(
                                it.title, it.views
                            )
                        }
                    )
                }
            )
            val intent = Intent(
                context, UtmDetailActivity::class.java
            ).apply {
                putExtra(
                    EXTRA_ITEMS, parcelableItems
                )
                putExtra(
                    EXTRA_TOTAL_VIEWS,
                    detailData.totalViews
                )
                putExtra(
                    EXTRA_TOTAL_VIEWS_CHANGE,
                    detailData.totalViewsChange
                )
                putExtra(
                    EXTRA_TOTAL_VIEWS_CHANGE_PERCENT,
                    detailData.totalViewsChangePercent
                )
                putExtra(
                    EXTRA_DATE_RANGE,
                    detailData.dateRange
                )
                putExtra(
                    EXTRA_CATEGORY_LABEL_RES_ID,
                    detailData.selectedCategory
                        .labelResId
                )
            }
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UtmDetailScreen(
    items: List<UtmDetailItem>,
    maxViewsForBar: Long,
    totalViews: Long,
    totalViewsChange: Long,
    totalViewsChangePercent: Double,
    dateRange: String,
    @StringRes categoryLabelResId: Int,
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
                Spacer(
                    modifier = Modifier.height(8.dp)
                )
                StatsSummaryCard(
                    totalViews = totalViews,
                    dateRange = dateRange,
                    totalViewsChange =
                        totalViewsChange,
                    totalViewsChangePercent =
                        totalViewsChangePercent
                )
                Spacer(
                    modifier = Modifier.height(16.dp)
                )
            }

            item {
                StatsListHeader(
                    leftHeaderResId =
                        categoryLabelResId
                )
                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            itemsIndexed(items) { index, item ->
                val percentage =
                    if (maxViewsForBar > 0) {
                        item.views.toFloat() /
                            maxViewsForBar.toFloat()
                    } else {
                        0f
                    }
                DetailUtmRow(
                    position = index + 1,
                    item = item,
                    percentage = percentage
                )
                if (index < items.lastIndex) {
                    Spacer(
                        modifier = Modifier.height(4.dp)
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

@Composable
private fun DetailUtmRow(
    position: Int,
    item: UtmDetailItem,
    percentage: Float
) {
    var expanded by remember { mutableStateOf(false) }
    val hasTopPosts = item.topPosts.isNotEmpty()

    Column {
        StatsListRowContainer(percentage = percentage) {
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
    post: UtmDetailPostItem
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
