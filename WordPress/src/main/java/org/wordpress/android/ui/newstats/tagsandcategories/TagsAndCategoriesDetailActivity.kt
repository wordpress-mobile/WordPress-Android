package org.wordpress.android.ui.newstats.tagsandcategories

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.newstats.components.StatsListHeader

private const val DETAIL_EXPANDED_START_PADDING = 52

@AndroidEntryPoint
class TagsAndCategoriesDetailActivity :
    BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val items = detailItems ?: emptyList()

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
        private var detailItems: List<TagGroupUiItem>? =
            null

        fun start(
            context: Context,
            items: List<TagGroupUiItem>
        ) {
            detailItems = items
            val intent = Intent(
                context,
                TagsAndCategoriesDetailActivity::class
                    .java
            )
            context.startActivity(intent)
        }

        fun clearData() {
            detailItems = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            clearData()
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
                val isExpandable =
                    item.tags.size > 1
                val isExpanded =
                    expandedGroups[index] == true

                TagGroupRow(
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
