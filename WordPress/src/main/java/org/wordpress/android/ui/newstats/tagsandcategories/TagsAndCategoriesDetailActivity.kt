package org.wordpress.android.ui.newstats.tagsandcategories

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.newstats.util.formatStatValue
import org.wordpress.android.util.extensions
    .getParcelableArrayListCompat

private const val EXTRA_TAG_GROUPS = "extra_tag_groups"
private val CardCornerRadius = 10.dp
private const val BAR_BACKGROUND_ALPHA = 0.08f
private const val VERTICAL_LINE_ALPHA = 0.3f

@AndroidEntryPoint
class TagsAndCategoriesDetailActivity :
    BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val items = intent.extras
            ?.getParcelableArrayListCompat<TagGroupUiItem>(
                EXTRA_TAG_GROUPS
            ) ?: arrayListOf()

        setContent {
            AppThemeM3 {
                TagsAndCategoriesDetailScreen(
                    items = items,
                    onBackPressed = onBackPressedDispatcher
                        ::onBackPressed
                )
            }
        }
    }

    companion object {
        fun start(
            context: Context,
            items: List<TagGroupUiItem>
        ) {
            val intent = Intent(
                context,
                TagsAndCategoriesDetailActivity::class
                    .java
            ).apply {
                putExtra(
                    EXTRA_TAG_GROUPS,
                    ArrayList(items)
                )
            }
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagsAndCategoriesDetailScreen(
    items: List<TagGroupUiItem>,
    onBackPressed: () -> Unit
) {
    val maxViews =
        items.firstOrNull()?.views ?: 1L

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
        val expandedGroups = remember {
            mutableStateMapOf<Int, Boolean>()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {
            item {
                DetailColumnHeaders()
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
                val isExpandable =
                    item.tags.size > 1
                val isExpanded =
                    expandedGroups[index] == true

                DetailTagGroupRow(
                    item = item,
                    percentage = percentage,
                    position = index + 1,
                    isExpandable = isExpandable,
                    isExpanded = isExpanded,
                    onClick = if (isExpandable) {
                        {
                            expandedGroups[index] =
                                !isExpanded
                        }
                    } else {
                        null
                    }
                )
                if (isExpandable) {
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        ExpandedTagsSection(
                            tags = item.tags
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
}

@Composable
private fun DetailColumnHeaders() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(
                R.string
                    .stats_insights_tags_and_categories
            ),
            style = MaterialTheme.typography
                .labelMedium,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant
        )
        Text(
            text = stringResource(
                R.string.stats_views
            ),
            style = MaterialTheme.typography
                .labelMedium,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun DetailTagGroupRow(
    item: TagGroupUiItem,
    percentage: Float,
    position: Int,
    isExpandable: Boolean,
    isExpanded: Boolean,
    onClick: (() -> Unit)?
) {
    val barColor = MaterialTheme.colorScheme.primary
        .copy(alpha = BAR_BACKGROUND_ALPHA)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = percentage)
                .fillMaxHeight()
                .background(barColor)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 12.dp,
                    horizontal = 16.dp
                ),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text = "$position",
                    style = MaterialTheme.typography
                        .bodyMedium,
                    color = MaterialTheme.colorScheme
                        .onSurfaceVariant,
                    modifier = Modifier.width(28.dp)
                )
                TagTypeIcon(
                    displayType = item.displayType
                )
                Spacer(
                    modifier = Modifier.width(8.dp)
                )
                if (isExpandable) {
                    Icon(
                        imageVector =
                            if (isExpanded) {
                                Icons.Default
                                    .KeyboardArrowUp
                            } else {
                                Icons.Default
                                    .KeyboardArrowDown
                            },
                        contentDescription = null,
                        modifier =
                            Modifier.size(16.dp),
                        tint = MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                    )
                    Spacer(
                        modifier =
                            Modifier.width(4.dp)
                    )
                }
                Text(
                    text = item.name,
                    style = MaterialTheme.typography
                        .bodyLarge,
                    color = MaterialTheme.colorScheme
                        .onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = formatStatValue(item.views),
                style = MaterialTheme.typography
                    .bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme
                    .onSurface
            )
        }
    }
}

@Composable
private fun ExpandedTagsSection(
    tags: List<TagUiItem>
) {
    val lineColor = MaterialTheme.colorScheme.primary
        .copy(alpha = VERTICAL_LINE_ALPHA)

    Column(
        modifier = Modifier.padding(
            start = 52.dp,
            top = 4.dp,
            bottom = 4.dp
        )
    ) {
        tags.forEachIndexed { index, tag ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(24.dp)
                        .background(lineColor)
                )
                Spacer(
                    modifier = Modifier.width(12.dp)
                )
                TagTypeIcon(
                    displayType =
                        TagGroupDisplayType
                            .fromTags(listOf(tag))
                )
                Spacer(
                    modifier = Modifier.width(8.dp)
                )
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography
                        .bodyMedium,
                    color = MaterialTheme.colorScheme
                        .onSurface
                )
            }
            if (index < tags.lastIndex) {
                Spacer(
                    modifier = Modifier.height(2.dp)
                )
            }
        }
    }
}

@Composable
private fun TagTypeIcon(
    displayType: TagGroupDisplayType
) {
    Icon(
        imageVector = when (displayType) {
            TagGroupDisplayType.CATEGORY ->
                Icons.Outlined.Folder
            TagGroupDisplayType.TAG,
            TagGroupDisplayType.MIXED ->
                Icons.Outlined.Sell
        },
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme
            .onSurfaceVariant
    )
}

@Preview(showBackground = true)
@Composable
private fun TagsAndCategoriesDetailPreview() {
    AppThemeM3 {
        TagsAndCategoriesDetailScreen(
            items = listOf(
                TagGroupUiItem(
                    name = "Uncategorized",
                    tags = listOf(
                        TagUiItem(
                            name = "Uncategorized",
                            tagType = "category"
                        )
                    ),
                    views = 83,
                    displayType =
                        TagGroupDisplayType.CATEGORY
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
            onBackPressed = {}
        )
    }
}
