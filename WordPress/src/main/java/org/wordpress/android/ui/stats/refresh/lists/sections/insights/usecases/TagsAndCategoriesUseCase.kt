package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import android.view.View
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.TagsModel
import org.wordpress.android.fluxc.model.stats.TagsModel.TagModel
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TAGS_AND_CATEGORIES
import org.wordpress.android.fluxc.store.stats.insights.TagsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewTag
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewTagsAndCategoriesStats
import org.wordpress.android.ui.stats.refresh.lists.BLOCK_ITEM_COUNT
import org.wordpress.android.ui.stats.refresh.lists.VIEW_ALL_ITEM_COUNT
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Divider
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.TextStyle.LIGHT
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TagsAndCategoriesUseCase.TagsAndCategoriesUiState
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.ItemPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.stats.refresh.utils.getBarWidth
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

class TagsAndCategoriesUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val tagsStore: TagsStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val resourceProvider: ResourceProvider,
    private val statsUtils: StatsUtils,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val popupMenuHandler: ItemPopupMenuHandler,
    private val contentDescriptionHelper: ContentDescriptionHelper,
    private val useCaseMode: UseCaseMode
) : BaseStatsUseCase<TagsModel, TagsAndCategoriesUiState>(
    TAGS_AND_CATEGORIES,
    mainDispatcher,
    backgroundDispatcher,
    TagsAndCategoriesUiState(null)
) {
    private val itemsToLoad = if (useCaseMode == VIEW_ALL) VIEW_ALL_ITEM_COUNT else BLOCK_ITEM_COUNT

    override suspend fun fetchRemoteData(forced: Boolean): State<TagsModel> {
        val response = tagsStore.fetchTags(statsSiteProvider.siteModel, LimitMode.Top(itemsToLoad), forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null && model.tags.isNotEmpty() -> State.Data(model)
            else -> State.Empty()
        }
    }

    override suspend fun loadCachedData(): TagsModel? {
        return tagsStore.getTags(statsSiteProvider.siteModel, LimitMode.Top(itemsToLoad))
    }

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_insights_tags_and_categories))

    override fun buildEmptyItem(): List<BlockListItem> {
        return listOf(buildTitle(), Empty())
    }

    override fun buildUiModel(domainModel: TagsModel, uiState: TagsAndCategoriesUiState): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()

        if (useCaseMode == BLOCK) {
            items.add(buildTitle())
        }

        if (domainModel.tags.isEmpty()) {
            items.add(Empty())
        } else {
            val header = Header(
                R.string.stats_tags_and_categories_title_label,
                R.string.stats_tags_and_categories_views_label
            )
            items.add(
                header
            )
            val tagsList = mutableListOf<BlockListItem>()
            val maxViews = domainModel.tags.maxByOrNull { it.views }?.views ?: 0
            domainModel.tags.forEachIndexed { index, tag ->
                when {
                    tag.items.size == 1 -> {
                        tagsList.add(mapTag(tag, index, domainModel.tags.size, maxViews, header))
                    }
                    else -> {
                        val isExpanded = areTagsEqual(tag, uiState.expandedTag)
                        tagsList.add(ExpandableItem(
                            mapCategory(tag, index, domainModel.tags.size, maxViews, header),
                            isExpanded
                        ) { changedExpandedState ->
                            onUiState(uiState.copy(expandedTag = if (changedExpandedState) tag else null))
                        })
                        if (isExpanded) {
                            tagsList.addAll(tag.items.map { subTag -> mapItem(subTag, header) })
                            tagsList.add(Divider)
                        }
                    }
                }
            }

            items.addAll(tagsList)
            if (useCaseMode == BLOCK && domainModel.hasMore) {
                items.add(
                    Link(
                        text = R.string.stats_insights_view_more,
                        navigateAction = ListItemInteraction.create(this::onLinkClick)
                    )
                )
            }
        }
        return items
    }

    private fun buildTitle() = Title(R.string.stats_insights_tags_and_categories, menuAction = this::onMenuClick)

    private fun areTagsEqual(tagA: TagModel, tagB: TagModel?): Boolean {
        return tagA.items == tagB?.items && tagA.views == tagB.views
    }

    private fun mapTag(tag: TagModel, index: Int, listSize: Int, maxViews: Long, header: Header): ListItemWithIcon {
        val item = tag.items.first()
        return ListItemWithIcon(
            icon = getIcon(item.type),
            text = item.name,
            value = statsUtils.toFormattedString(tag.views),
            barWidth = getBarWidth(tag.views, maxViews),
            showDivider = index < listSize - 1,
            navigationAction = ListItemInteraction.create(item.link, this::onTagClick),
            contentDescription = contentDescriptionHelper.buildContentDescription(
                header,
                item.name,
                tag.views
            )
        )
    }

    private fun mapCategory(
        tag: TagModel,
        index: Int,
        listSize: Int,
        maxViews: Long,
        header: Header
    ): ListItemWithIcon {
        val text = tag.items.foldIndexed("") { itemIndex, acc, item ->
            when (itemIndex) {
                0 -> item.name
                else -> resourceProvider.getString(R.string.stats_category_folded_name, acc, item.name)
            }
        }
        return ListItemWithIcon(
            icon = R.drawable.ic_folder_multiple_white_24dp,
            text = text,
            value = statsUtils.toFormattedString(tag.views),
            barWidth = getBarWidth(tag.views, maxViews),
            showDivider = index < listSize - 1,
            contentDescription = contentDescriptionHelper.buildContentDescription(
                header,
                text,
                tag.views
            )
        )
    }

    private fun mapItem(item: TagModel.Item, header: Header): ListItemWithIcon {
        return ListItemWithIcon(
            icon = getIcon(item.type),
            textStyle = LIGHT,
            text = item.name,
            showDivider = false,
            navigationAction = ListItemInteraction.create(item.link, this::onTagClick),
            contentDescription = contentDescriptionHelper.buildContentDescription(
                header.startLabel,
                item.name
            )
        )
    }

    private fun getIcon(type: String) =
        if (type == "tag") R.drawable.ic_tag_white_24dp else R.drawable.ic_folder_white_24dp

    private fun onLinkClick() {
        analyticsTracker.track(AnalyticsTracker.Stat.STATS_TAGS_AND_CATEGORIES_VIEW_MORE_TAPPED)
        navigateTo(ViewTagsAndCategoriesStats)
    }

    private fun onTagClick(link: String) {
        analyticsTracker.track(AnalyticsTracker.Stat.STATS_TAGS_AND_CATEGORIES_VIEW_TAG_TAPPED)
        navigateTo(ViewTag(link))
    }

    private fun onMenuClick(view: View) {
        popupMenuHandler.onMenuClick(view, type)
    }

    data class TagsAndCategoriesUiState(val expandedTag: TagModel? = null)

    class TagsAndCategoriesUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val tagsStore: TagsStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val resourceProvider: ResourceProvider,
        private val statsUtils: StatsUtils,
        private val analyticsTracker: AnalyticsTrackerWrapper,
        private val contentDescriptionHelper: ContentDescriptionHelper,
        private val popupMenuHandler: ItemPopupMenuHandler
    ) : InsightUseCaseFactory {
        override fun build(useCaseMode: UseCaseMode) =
            TagsAndCategoriesUseCase(
                mainDispatcher,
                backgroundDispatcher,
                tagsStore,
                statsSiteProvider,
                resourceProvider,
                statsUtils,
                analyticsTracker,
                popupMenuHandler,
                contentDescriptionHelper,
                useCaseMode
            )
    }
}
