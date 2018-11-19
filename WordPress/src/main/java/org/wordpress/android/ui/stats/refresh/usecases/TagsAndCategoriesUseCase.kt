package org.wordpress.android.ui.stats.refresh.usecases

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
import org.wordpress.android.ui.stats.refresh.BlockListItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.Item
import org.wordpress.android.ui.stats.refresh.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.InsightsItem
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewTag
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewTagsAndCategoriesStats
import org.wordpress.android.ui.stats.refresh.toFormattedString
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

class TagsAndCategoriesUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val insightsStore: InsightsStore,
    private val resourceProvider: ResourceProvider
) : BaseInsightsUseCase(TAGS_AND_CATEGORIES, mainDispatcher) {
    override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean): InsightsItem? {
        val response = insightsStore.fetchTags(site, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> failedItem(
                    string.stats_view_tags_and_categories,
                    error.message ?: error.type.name
            )
            else -> model?.let { loadTagsAndCategories(site, model) }
        }
    }

    override suspend fun loadCachedData(site: SiteModel): InsightsItem? {
        val model = insightsStore.getTags(site)
        return model?.let { loadTagsAndCategories(site, model) }
    }

    private fun loadTagsAndCategories(
        site: SiteModel,
        model: TagsModel?
    ): InsightsItem {
        val items = mutableListOf<BlockListItem>()
        if (model == null) {
            items.add(Empty)
        } else {
            items.add(Title(R.string.stats_view_tags_and_categories))
            if (model.tags.isEmpty()) {
                items.add(Empty)
            } else {
                items.addAll(model.tags.mapIndexed { index, tag ->
                    when {
                        tag.items.size == 1 -> mapTag(tag, index, model.tags.size)
                        else -> ExpandableItem(
                                mapCategory(tag, index, model.tags.size),
                                tag.items.map { subTag -> mapItem(subTag) }
                        )
                    }
                })
                items.add(Link(text = R.string.stats_insights_view_more) {
                    navigateTo(ViewTagsAndCategoriesStats(site.siteId))
                })
            }
        }
        return dataItem(items)
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
