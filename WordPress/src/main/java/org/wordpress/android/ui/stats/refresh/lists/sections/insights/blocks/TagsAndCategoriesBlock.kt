package org.wordpress.android.ui.stats.refresh.lists.sections.insights.blocks

import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.TagsModel
import org.wordpress.android.fluxc.model.stats.TagsModel.TagModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.TAGS_AND_CATEGORIES
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Item
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.StatsListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsBlock
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewTag
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewTagsAndCategoriesStats
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

private const val PAGE_SIZE = 6

class TagsAndCategoriesBlock
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val insightsStore: InsightsStore,
    private val resourceProvider: ResourceProvider
) : BaseStatsBlock(TAGS_AND_CATEGORIES, mainDispatcher) {
    override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean): StatsListItem? {
        val response = insightsStore.fetchTags(site, PAGE_SIZE, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> createFailedItem(
                    string.stats_view_tags_and_categories,
                    error.message ?: error.type.name
            )
            else -> model?.let { loadTagsAndCategories(site, model) }
        }
    }

    override suspend fun loadCachedData(site: SiteModel): StatsListItem? {
        val model = insightsStore.getTags(site, PAGE_SIZE)
        return model?.let { loadTagsAndCategories(site, model) }
    }

    private fun loadTagsAndCategories(
        site: SiteModel,
        model: TagsModel
    ): StatsListItem {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_view_tags_and_categories))
        if (model.tags.isEmpty()) {
            items.add(Empty)
        } else {
            items.addAll(model.tags.take(PAGE_SIZE).mapIndexed { index, tag ->
                when {
                    tag.items.size == 1 -> mapTag(tag, index, model.tags.size)
                    else -> ExpandableItem(
                            mapCategory(tag, index, model.tags.size),
                            tag.items.map { subTag -> mapItem(subTag) }
                    )
                }
            })
            if (model.hasMore) {
                items.add(Link(text = R.string.stats_insights_view_more) {
                    navigateTo(ViewTagsAndCategoriesStats(site.siteId))
                })
            }
        }
        return createDataItem(items)
    }

    private fun mapTag(tag: TagsModel.TagModel, index: Int, listSize: Int): Item {
        val item = tag.items.first()
        return Item(
                icon = getIcon(item.type),
                text = item.name,
                value = tag.views.toFormattedString(),
                showDivider = index < listSize - 1,
                clickAction = { clickTag(item.link) }
        )
    }

    private fun mapCategory(tag: TagsModel.TagModel, index: Int, listSize: Int): Item {
        val text = tag.items.foldIndexed("") { itemIndex, acc, item ->
            when (itemIndex) {
                0 -> item.name
                else -> resourceProvider.getString(R.string.stats_category_folded_name, acc, item.name)
            }
        }
        return Item(
                icon = R.drawable.ic_folder_multiple_grey_dark_24dp,
                text = text,
                value = tag.views.toFormattedString(),
                showDivider = index < listSize - 1
        )
    }

    private fun mapItem(item: TagModel.Item): Item {
        return Item(
                icon = getIcon(item.type),
                text = item.name,
                showDivider = false,
                clickAction = { clickTag(item.link) }
        )
    }

    private fun getIcon(type: String) =
            if (type == "tag") drawable.ic_tag_grey_dark_24dp else drawable.ic_folder_grey_dark_24dp

    private fun clickTag(link: String) {
        navigateTo(ViewTag(link))
    }
}
